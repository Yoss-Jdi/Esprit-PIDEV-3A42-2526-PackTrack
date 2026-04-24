"""
generate_dataset_duree.py
─────────────────────────
Génère un dataset synthétique de durées de livraison en Tunisie.

Features :
  distance_km  – distance routière réelle
  poids_kg     – poids du colis (influence les temps de chargement/déchargement)
  heure        – heure de départ (0-23)
  jour_semaine – 0=Lundi … 6=Dimanche

Target :
  duree_minutes – durée estimée en minutes (minimum 5)

Logique de simulation :
  1. Vitesse de base selon la distance (ville/route nationale/autoroute).
  2. Coefficient de congestion selon heure + jour.
  3. Temps de manutention lié au poids.
  4. Bruit gaussien ±8 %.
"""

import math
import random
import csv
import os

SEED = 42
random.seed(SEED)

OUTPUT = os.path.join(os.path.dirname(__file__), "data", "dataset_duree.csv")


# ── Vitesse de base ───────────────────────────────────────────────────────────
def vitesse_base_kmh(distance_km):
    """
    Vitesse moyenne estimée selon la distance :
      < 5 km   → urbain dense              : 18–25 km/h
      5-20 km  → périurbain / rocade       : 30–45 km/h
      20-80 km → voie rapide / route natio : 65–85 km/h
      > 80 km  → autoroute                 : 90–110 km/h
    """
    if distance_km < 5:
        return random.uniform(18, 25)
    elif distance_km < 20:
        return random.uniform(30, 45)
    elif distance_km < 80:
        return random.uniform(65, 85)
    else:
        return random.uniform(90, 110)


# ── Coefficient de congestion ─────────────────────────────────────────────────
def coeff_congestion(heure, jour_semaine):
    """
    Multiplie le temps de trajet selon l'heure et le jour.
    Tunisie : pic matin 7h-9h, pic soir 17h-19h, calme vendredi après-midi.
    Valeurs > 1.0 = plus lent (trafic dense).
    """
    # Vendredi (4) et Samedi (5) = week-end tunisien
    week_end = jour_semaine in (4, 5)

    if week_end:
        # Trafic léger sauf le matin du vendredi (sortie mosquée ~12h)
        if 11 <= heure <= 13:
            return random.uniform(1.15, 1.30)
        return random.uniform(0.85, 1.05)
    else:
        # Jour ouvrable
        if 7 <= heure <= 9:       # pointe matin
            return random.uniform(1.40, 1.75)
        elif 12 <= heure <= 14:   # pause déjeuner
            return random.uniform(1.15, 1.35)
        elif 17 <= heure <= 19:   # pointe soir
            return random.uniform(1.45, 1.80)
        elif 22 <= heure or heure <= 5:  # nuit
            return random.uniform(0.70, 0.85)
        else:
            return random.uniform(0.95, 1.15)


# ── Temps de manutention ──────────────────────────────────────────────────────
def temps_manutention_minutes(poids_kg):
    """
    Temps de chargement + déchargement selon le poids.
      < 5 kg   : 2–4 min
      5-20 kg  : 4–8 min
      20-50 kg : 8–14 min
      > 50 kg  : 14–22 min
    """
    if poids_kg < 5:
        return random.uniform(2, 4)
    elif poids_kg < 20:
        return random.uniform(4, 8)
    elif poids_kg < 50:
        return random.uniform(8, 14)
    else:
        return random.uniform(14, 22)


def generer_ligne():
    # Distance : distribution réaliste (beaucoup de courts trajets)
    # 60 % trajets courts-moyens, 40 % longs
    if random.random() < 0.60:
        distance_km = random.uniform(0.5, 40)
    else:
        distance_km = random.uniform(40, 500)

    poids_kg     = round(random.uniform(0.1, 200), 2)
    heure        = random.randint(0, 23)
    jour_semaine = random.randint(0, 6)

    vitesse = vitesse_base_kmh(distance_km)
    coeff   = coeff_congestion(heure, jour_semaine)

    # Temps de route en minutes
    temps_route = (distance_km / vitesse) * 60 * coeff
    # Temps de manutention
    manutention = temps_manutention_minutes(poids_kg)
    # Total brut
    duree = temps_route + manutention
    # Bruit gaussien ±8 %
    duree *= random.gauss(1.0, 0.08)
    # Minimum 5 minutes
    duree = max(5.0, duree)
    duree = round(duree, 2)

    return {
        "distance_km":  round(distance_km, 3),
        "poids_kg":     poids_kg,
        "heure":        heure,
        "jour_semaine": jour_semaine,
        "duree_minutes": duree,
    }


def generer_dataset(n=7000):
    return [generer_ligne() for _ in range(n)]


if __name__ == "__main__":
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    rows = generer_dataset(7000)

    with open(OUTPUT, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=rows[0].keys())
        writer.writeheader()
        writer.writerows(rows)

    print(f"✅  Dataset durée généré : {len(rows)} lignes → {OUTPUT}")
    print(f"   Exemple : {rows[0]}")
    durees = [r["duree_minutes"] for r in rows]
    print(f"   Min={min(durees):.1f} min  Max={max(durees):.1f} min  Moy={sum(durees)/len(durees):.1f} min")