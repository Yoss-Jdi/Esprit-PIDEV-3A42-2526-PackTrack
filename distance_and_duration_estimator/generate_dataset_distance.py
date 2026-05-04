"""
generate_dataset_distance.py
────────────────────────────
Génère un dataset synthétique de distances routières en Tunisie.

Stratégie :
  1. On part de vraies coordonnées GPS de villes / quartiers tunisiens.
  2. On calcule la distance à vol d'oiseau (haversine).
  3. On applique un facteur de détour réaliste qui dépend du type de trajet
     (urbain intra-ville, périurbain, interurbain) + bruit gaussien.
  4. On s'assure d'un minimum de 0.5 km.

Features exportées :
  lat_depart, lon_depart, lat_destination, lon_destination → distance_km
"""

import math
import random
import csv
import os

SEED = 42
random.seed(SEED)

OUTPUT = os.path.join(os.path.dirname(__file__), "data", "dataset_distance.csv")

# ── Points GPS réels en Tunisie (lat, lon, nom) ──────────────────────────────
POINTS = [
    # Grand Tunis
    (36.8190, 10.1658, "Tunis Centre"),
    (36.8065, 10.1815, "Bab Bhar"),
    (36.8500, 10.1940, "La Marsa"),
    (36.8335, 10.2270, "Carthage"),
    (36.8830, 10.1550, "Ariana"),
    (36.8740, 10.1940, "La Soukra"),
    (36.8160, 10.2290, "Le Bardo"),
    (36.7880, 10.1740, "Manouba"),
    (36.8440, 10.0900, "Ettadhamen"),
    (36.7990, 10.2050, "Ben Arous"),
    (36.7580, 10.2260, "Rades"),
    (36.7340, 10.2530, "Hammam Lif"),
    (36.8760, 10.3380, "Soliman"),
    (36.9280, 10.2750, "Nabeul"),  # début Cap Bon
    (36.7280, 10.7380, "Hammamet"),
    (36.4060, 10.7020, "Sousse"),
    (35.6740, 10.8960, "Monastir"),
    (35.5030, 11.0420, "Mahdia"),
    (34.7400, 10.7600, "Sfax"),
    (33.8810, 10.0980, "Gabes"),
    (33.3190,  9.8820, "Medenine"),
    (32.9310, 10.4510, "Zarzis"),
    (33.1190,  9.0500, "Tataouine"),
    (34.3730,  8.8310, "Gafsa"),
    (34.9240,  8.1260, "Kasserine"),
    (35.6740,  9.1070, "Kairouan"),
    (36.1820,  9.5560, "Siliana"),
    (36.3580,  9.1830, "Le Kef"),
    (37.2740,  9.8650, "Bizerte"),
    (36.6630,  9.0870, "Beja"),
    (36.4740,  8.7840, "Jendouba"),
    (36.7980, 10.5890, "Zaghouan"),
    (36.5100, 10.5060, "Grombalia"),
    (36.5600, 10.6740, "Menzel Temime"),
    (36.6500, 10.7380, "Kelibia"),
    (36.0500, 10.5050, "Enfidha"),
    (35.9280, 10.5750, "Hergla"),
    (35.2820, 10.9500, "Ksour Essaf"),
    (34.4310, 10.0740, "El Djem"),
    (34.3480, 10.5520, "Chebba"),
    # Quartiers de Tunis pour trajets courts
    (36.8340, 10.1590, "Montplaisir"),
    (36.8120, 10.1800, "Lafayette"),
    (36.8010, 10.1720, "Bab Alioua"),
    (36.8220, 10.1560, "Cité Jardins"),
    (36.8300, 10.2100, "Mégrine"),
    (36.8600, 10.1400, "Ennasr"),
    (36.8900, 10.1900, "Ain Zaghouan"),
    (36.8680, 10.2200, "El Manar"),
    (36.8760, 10.1050, "Cité Olympique"),
    (36.8420, 10.1350, "Cité Ibn Sina"),
]


def haversine(lat1, lon1, lat2, lon2):
    """Distance à vol d'oiseau en km."""
    R = 6371.0
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlam = math.radians(lon2 - lon1)
    a = math.sin(dphi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(dlam / 2) ** 2
    return R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def facteur_route(dist_vol):
    """
    Facteur de détour réaliste selon la distance à vol d'oiseau :
      < 2 km   → urbain dense        : ×1.35 – 1.55 (rues étroites, sens uniques)
      2-15 km  → périurbain           : ×1.20 – 1.40
      15-80 km → interurbain (GP/RN) : ×1.10 – 1.25
      > 80 km  → autoroute / GP      : ×1.05 – 1.18
    """
    if dist_vol < 2:
        return random.uniform(1.35, 1.55)
    elif dist_vol < 15:
        return random.uniform(1.20, 1.40)
    elif dist_vol < 80:
        return random.uniform(1.10, 1.25)
    else:
        return random.uniform(1.05, 1.18)


def generer_paire():
    """Génère une paire (départ, destination) aléatoire."""
    p1, p2 = random.sample(POINTS, 2)
    return p1, p2


def generer_dataset(n=6000):
    rows = []
    for _ in range(n):
        p1, p2 = generer_paire()
        lat1, lon1 = p1[0], p1[1]
        lat2, lon2 = p2[0], p2[1]

        # Légère perturbation GPS (±0.003°  ≈ ±330 m) pour diversifier
        lat1 += random.gauss(0, 0.003)
        lon1 += random.gauss(0, 0.003)
        lat2 += random.gauss(0, 0.003)
        lon2 += random.gauss(0, 0.003)

        vol    = haversine(lat1, lon1, lat2, lon2)
        factor = facteur_route(vol)
        dist   = max(0.5, vol * factor)

        # Bruit résiduel ±3 %
        dist *= random.uniform(0.97, 1.03)
        dist  = round(dist, 3)

        rows.append({
            "lat_depart":      round(lat1, 6),
            "lon_depart":      round(lon1, 6),
            "lat_destination": round(lat2, 6),
            "lon_destination": round(lon2, 6),
            "distance_km":     dist,
        })
    return rows


if __name__ == "__main__":
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    rows = generer_dataset(6000)

    with open(OUTPUT, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=rows[0].keys())
        writer.writeheader()
        writer.writerows(rows)

    print(f"✅  Dataset distance généré : {len(rows)} lignes → {OUTPUT}")
    # Aperçu
    print(f"   Exemple : {rows[0]}")
    dists = [r["distance_km"] for r in rows]
    print(f"   Min={min(dists):.2f} km  Max={max(dists):.2f} km  Moy={sum(dists)/len(dists):.2f} km")