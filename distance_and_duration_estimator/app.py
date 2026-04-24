"""
app.py
──────
Service ML Flask — prédit distance et durée d'une livraison.

Compatible avec les adresses produites par MapHelper (Leaflet + Nominatim) :
  • Format normal   : "Avenue Habib Bourguiba, Lafayette, Tunis, Tunisie"
  • Format fallback : "36.81900, 10.16580"  (si Nominatim échoue au clic)

Routes :
  GET  /health          → statut du service
  POST /predict-complet → prédiction distance + durée

Port : 5001
"""

import os
import re
import pickle
import math
import datetime
import requests
import numpy as np
from flask import Flask, request, jsonify

# ── Configuration ─────────────────────────────────────────────────────────────
BASE_DIR   = os.path.dirname(__file__)
MODEL_DIST = os.path.join(BASE_DIR, "models", "model_distance.pkl")
MODEL_DUR  = os.path.join(BASE_DIR, "models", "model_duree.pkl")

# Clé OpenRouteService — variable d'environnement en production
# export ORS_API_KEY="votre_clé"
ORS_API_KEY = os.environ.get("eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImY5NmQ3NWQ5NGNiODQ4MmNiMzI4NjQ4MDFhZDkwNDdiIiwiaCI6Im11cm11cjY0In0=", "")

app = Flask(__name__)

# ── Chargement des modèles au démarrage ───────────────────────────────────────
print("[app] Chargement des modèles ML…")
try:
    with open(MODEL_DIST, "rb") as f:
        model_distance = pickle.load(f)

    print(f"  ✅ Modèle distance : {MODEL_DIST}")
except FileNotFoundError:
    model_distance = None
    print(f"  ⚠  Modèle distance introuvable — fallback haversine actif")

try:
    with open(MODEL_DUR, "rb") as f:
        model_duree = pickle.load(f)
    print(f"  ✅ Modèle durée    : {MODEL_DUR}")
except FileNotFoundError:
    model_duree = None
    print(f"  ⚠  Modèle durée introuvable — fallback vitesse 40 km/h actif")


# ── Géocodage ─────────────────────────────────────────────────────────────────

# Regex pour détecter le format "lat, lon" produit par MapHelper en fallback
# Ex: "36.81900, 10.16580" ou "36.81900,10.16580"
_RE_COORDS = re.compile(
    r"^\s*(-?\d{1,3}(?:\.\d+)?)\s*,\s*(-?\d{1,3}(?:\.\d+)?)\s*$"
)


def extraire_coords_directes(adresse: str):
    """
    Si MapHelper n'a pas réussi le géocodage inversé, il envoie
    directement "lat, lon" (ex: "36.81900, 10.16580").
    On les extrait sans appel réseau.
    """
    m = _RE_COORDS.match(adresse)
    if m:
        lat, lon = float(m.group(1)), float(m.group(2))
        # Vérification plausibilité Tunisie (bbox approximative)
        if 30.0 <= lat <= 38.0 and 7.5 <= lon <= 12.0:
            print(f"  [geocode] Coords directes détectées : {lat}, {lon}")
            return lat, lon
    return None


def geocoder_nominatim(adresse: str):
    """
    Géocode via Nominatim (OpenStreetMap) — gratuit, sans clé.
    Fonctionne parfaitement avec les display_name produits par
    le géocodage inversé de MapHelper (clic sur la carte).
    Ex: "Rue de Carthage, Bab Bhar, Tunis, Gouvernorat de Tunis, 1000, Tunisie"
    """
    url = "https://nominatim.openstreetmap.org/search"
    params = {
        "q":               adresse,
        "format":          "json",
        "limit":           1,
        "accept-language": "fr",
        "countrycodes":    "tn",
    }
    headers = {"User-Agent": "PackTrackML/1.0"}
    try:
        r = requests.get(url, params=params, headers=headers, timeout=8)
        data = r.json()
        if data:
            lat = float(data[0]["lat"])
            lon = float(data[0]["lon"])
            print(f"  [Nominatim] ✅ → {lat:.5f}, {lon:.5f}")
            return lat, lon
    except Exception as e:
        print(f"  [Nominatim] Erreur : {e}")
    return None


def geocoder_ors(adresse: str):
    """
    Géocode via OpenRouteService — plus précis, nécessite une clé API.
    Activé uniquement si ORS_API_KEY est défini dans l'environnement.
    """
    if not ORS_API_KEY:
        return None
    url = "https://api.openrouteservice.org/geocode/search"
    params = {
        "api_key":          ORS_API_KEY,
        "text":             adresse,
        "size":             1,
        "boundary.country": "TN",
    }
    try:
        r = requests.get(url, params=params, timeout=8)
        data = r.json()
        feats = data.get("features", [])
        if feats:
            coords = feats[0]["geometry"]["coordinates"]  # [lon, lat]
            lat, lon = coords[1], coords[0]
            print(f"  [ORS] ✅ → {lat:.5f}, {lon:.5f}")
            return lat, lon
    except Exception as e:
        print(f"  [ORS] Erreur : {e}")
    return None


def geocoder(adresse: str):
    """
    Stratégie de géocodage à 3 niveaux, adaptée au format MapHelper :

    1. Coordonnées directes "lat, lon"  → 0 ms, aucun réseau
       (format produit par MapHelper quand Nominatim reverse échoue)

    2. ORS si ORS_API_KEY est défini    → plus précis pour adresses complexes

    3. Nominatim                        → toujours disponible, gratuit
       (traite très bien les display_name comme
        "Avenue Bourguiba, Tunis, Gouvernorat de Tunis, Tunisie")
    """
    coords = extraire_coords_directes(adresse)
    if coords:
        return coords

    coords = geocoder_ors(adresse)
    if coords:
        return coords

    return geocoder_nominatim(adresse)


# ── Utilitaires ───────────────────────────────────────────────────────────────

def formater_duree(minutes: float) -> str:
    m = int(round(minutes))
    if m < 60:
        return f"{m} min"
    h, rem = m // 60, m % 60
    return f"{h}h {rem:02d}min" if rem else f"{h}h"


def haversine(lat1, lon1, lat2, lon2) -> float:
    """Distance à vol d'oiseau en km — utilisé en fallback si modèle absent."""
    R = 6371.0
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlam = math.radians(lon2 - lon1)
    a = (math.sin(dphi / 2) ** 2
         + math.cos(phi1) * math.cos(phi2) * math.sin(dlam / 2) ** 2)
    return R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


# ── Routes ────────────────────────────────────────────────────────────────────

@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status":         "ok",
        "model_distance": model_distance is not None,
        "model_duree":    model_duree is not None,
        "ors_key":        bool(ORS_API_KEY),
    })


@app.route("/predict-complet", methods=["POST"])
def predict_complet():
    data = request.get_json(force=True)

    # Validation des champs requis
    required = ["adresse_depart", "adresse_destination", "poids_kg", "date_debut"]
    missing  = [k for k in required if k not in data]
    if missing:
        return jsonify({"error": f"Champs manquants : {missing}"}), 400

    adresse_dep  = str(data["adresse_depart"]).strip()
    adresse_dest = str(data["adresse_destination"]).strip()
    poids_kg     = float(data["poids_kg"])

    # Parsing date_debut — accepte ISO avec T ou espace
    try:
        date_str   = str(data["date_debut"]).replace("T", " ")[:19]
        date_debut = datetime.datetime.strptime(date_str, "%Y-%m-%d %H:%M:%S")
    except ValueError as e:
        return jsonify({"error": f"Format date_debut invalide : {e}"}), 400

    print(f"\n[predict] dep='{adresse_dep[:70]}'"
          f"\n          dest='{adresse_dest[:70]}'"
          f"\n          poids={poids_kg}kg | date={date_debut}")

    # Géocodage
    coords_dep = geocoder(adresse_dep)
    if not coords_dep:
        return jsonify({"error": f"Adresse départ introuvable : '{adresse_dep}'"}), 422

    coords_dest = geocoder(adresse_dest)
    if not coords_dest:
        return jsonify({"error": f"Adresse destination introuvable : '{adresse_dest}'"}), 422

    lat_dep,  lon_dep  = coords_dep
    lat_dest, lon_dest = coords_dest

    # Prédiction distance
    features_dist = np.array([[lat_dep, lon_dep, lat_dest, lon_dest]])
    if model_distance:
        distance_km = float(model_distance.predict(features_dist)[0])
    else:
        distance_km = haversine(lat_dep, lon_dep, lat_dest, lon_dest) * 1.30
        print("  ⚠  Fallback haversine ×1.30")

    distance_km = max(0.5, round(distance_km, 2))
    print(f"  Distance ML : {distance_km} km")

    # Prédiction durée
    heure        = date_debut.hour
    jour_semaine = date_debut.weekday()   # 0=Lundi … 6=Dimanche
    features_dur = np.array([[distance_km, poids_kg, heure, jour_semaine]])

    if model_duree:
        duree_minutes = float(model_duree.predict(features_dur)[0])
    else:
        duree_minutes = (distance_km / 40) * 60 + 5
        print("  ⚠  Fallback vitesse 40 km/h")

    duree_minutes = max(5.0, round(duree_minutes, 1))
    print(f"  Durée ML    : {duree_minutes} min → {formater_duree(duree_minutes)}")

    return jsonify({
        "distance_km":        distance_km,
        "duree_minutes":      duree_minutes,
        "duree_formatee":     formater_duree(duree_minutes),
        "coords_depart":      [lat_dep,  lon_dep],
        "coords_destination": [lat_dest, lon_dest],
    })


# ── Point d'entrée ────────────────────────────────────────────────────────────
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, debug=False)