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
from dotenv import load_dotenv


# ── Configuration ─────────────────────────────────────────────────────────────
BASE_DIR   = os.path.dirname(__file__)
MODEL_DIST = os.path.join(BASE_DIR, "models", "model_distance.pkl")
MODEL_DUR  = os.path.join(BASE_DIR, "models", "model_duree.pkl")

load_dotenv()
ORS_API_KEY = os.getenv("ORS_API_KEY")

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

    required = ["adresse_depart", "adresse_destination", "poids_kg", "date_debut"]
    missing = [k for k in required if k not in data]
    if missing:
        return jsonify({"error": f"Champs manquants : {missing}"}), 400

    adresse_dep = str(data["adresse_depart"]).strip()
    adresse_dest = str(data["adresse_destination"]).strip()
    poids_kg = float(data["poids_kg"])

    try:
        date_str = str(data["date_debut"]).replace("T", " ")[:19]
        date_debut = datetime.datetime.strptime(date_str, "%Y-%m-%d %H:%M:%S")
    except ValueError as e:
        return jsonify({"error": f"Format date_debut invalide : {e}"}), 400

    # Features contextuelles optionnelles
    conditions_meteo = data.get("conditions_meteo", 0)  # 0=normal par défaut
    niveau_trafic = data.get("niveau_trafic", 0.2)     # 0.2=modéré par défaut
    temperature = data.get("temperature", 20.0)
    visibilite = data.get("visibilite", 10000)

    print(f"\n[predict] dep='{adresse_dep[:70]}'"
          f"\n          dest='{adresse_dest[:70]}'"
          f"\n          poids={poids_kg}kg | date={date_debut}"
          f"\n          meteo={conditions_meteo} | trafic={niveau_trafic:.2f}")

    # Géocodage
    coords_dep = geocoder(adresse_dep)
    if not coords_dep:
        return jsonify({"error": f"Adresse départ introuvable : '{adresse_dep}'"}), 422

    coords_dest = geocoder(adresse_dest)
    if not coords_dest:
        return jsonify({"error": f"Adresse destination introuvable : '{adresse_dest}'"}), 422

    lat_dep, lon_dep = coords_dep
    lat_dest, lon_dest = coords_dest

    # Prédiction distance (inchangée)
    features_dist = np.array([[lat_dep, lon_dep, lat_dest, lon_dest]])
    if model_distance:
        distance_km = float(model_distance.predict(features_dist)[0])
    else:
        distance_km = haversine(lat_dep, lon_dep, lat_dest, lon_dest) * 1.30

    distance_km = max(0.5, round(distance_km, 2))
    print(f"  Distance ML : {distance_km} km")

    # Prédiction durée ENRICHIE avec 8 features
    heure = date_debut.hour
    jour_semaine = date_debut.weekday()

    features_dur = np.array([[
        distance_km, poids_kg, heure, jour_semaine,
        conditions_meteo, niveau_trafic, temperature, visibilite
    ]])

    if model_duree:
        duree_minutes = float(model_duree.predict(features_dur)[0])
    else:
        # Fallback avec facteurs météo/trafic
        vitesse_base = 40
        coeff_meteo = [1.0, 1.15, 1.40, 1.25][min(conditions_meteo, 3)]
        coeff_trafic = 1.0 + (niveau_trafic * 2.0)
        duree_minutes = ((distance_km / vitesse_base) * 60 * coeff_meteo * coeff_trafic) + 5

    duree_minutes = max(5.0, round(duree_minutes, 1))
    print(f"  Durée ML    : {duree_minutes} min → {formater_duree(duree_minutes)}")

    return jsonify({
        "distance_km": distance_km,
        "duree_minutes": duree_minutes,
        "duree_formatee": formater_duree(duree_minutes),
        "coords_depart": [lat_dep, lon_dep],
        "coords_destination": [lat_dest, lon_dest],
    })


# ── Point d'entrée ────────────────────────────────────────────────────────────
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, debug=False)