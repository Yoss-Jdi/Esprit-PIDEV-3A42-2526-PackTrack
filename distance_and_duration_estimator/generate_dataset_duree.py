"""
generate_dataset_duree.py
──────────────────────────────────
Dataset de durées avec conditions météo et trafic simulées.

Nouvelles features :
  - conditions_meteo : 0=normal, 1=pluie, 2=neige, 3=orage
  - niveau_trafic    : 0.0-1.0 (congestion)
  - temperature      : -5 à 45°C
  - visibilite       : 100-10000 m
"""

import math
import random
import csv
import os

SEED = 42
random.seed(SEED)

OUTPUT = os.path.join(os.path.dirname(__file__), "data", "dataset_duree.csv")


def vitesse_base_kmh(distance_km):
    if distance_km < 5:
        return random.uniform(18, 25)
    elif distance_km < 20:
        return random.uniform(30, 45)
    elif distance_km < 80:
        return random.uniform(65, 85)
    else:
        return random.uniform(90, 110)


def coeff_congestion_heure(heure, jour_semaine):
    """Congestion liée à l'heure (indépendant du trafic API)"""
    week_end = jour_semaine in (4, 5)

    if week_end:
        if 11 <= heure <= 13:
            return random.uniform(1.15, 1.30)
        return random.uniform(0.85, 1.05)
    else:
        if 7 <= heure <= 9:
            return random.uniform(1.40, 1.75)
        elif 12 <= heure <= 14:
            return random.uniform(1.15, 1.35)
        elif 17 <= heure <= 19:
            return random.uniform(1.45, 1.80)
        elif 22 <= heure or heure <= 5:
            return random.uniform(0.70, 0.85)
        else:
            return random.uniform(0.95, 1.15)


def temps_manutention_minutes(poids_kg):
    if poids_kg < 5:
        return random.uniform(2, 4)
    elif poids_kg < 20:
        return random.uniform(4, 8)
    elif poids_kg < 50:
        return random.uniform(8, 14)
    else:
        return random.uniform(14, 22)


def generer_conditions_meteo():
    """
    Génère des conditions météo réalistes pour la Tunisie.
    Retourne : (conditions_meteo, temperature, visibilite, coeff_impact)
    """
    # Distribution réaliste : 70% normal, 20% pluie, 8% orage, 2% neige
    rand = random.random()

    if rand < 0.70:  # Normal
        conditions = 0
        temperature = random.uniform(15, 35)
        visibilite = random.randint(8000, 10000)
        coeff = 1.0
    elif rand < 0.90:  # Pluie
        conditions = 1
        temperature = random.uniform(10, 25)
        visibilite = random.randint(2000, 7000)
        coeff = random.uniform(1.10, 1.20)  # +10-20%
    elif rand < 0.98:  # Orage
        conditions = 3
        temperature = random.uniform(15, 30)
        visibilite = random.randint(1000, 5000)
        coeff = random.uniform(1.20, 1.35)  # +20-35%
    else:  # Neige (rare en Tunisie, surtout en altitude)
        conditions = 2
        temperature = random.uniform(-2, 8)
        visibilite = random.randint(500, 3000)
        coeff = random.uniform(1.35, 1.55)  # +35-55%

    return conditions, temperature, visibilite, coeff


def generer_niveau_trafic(heure, jour_semaine, distance_km):
    """
    Génère un niveau de trafic (0.0-1.0) basé sur l'heure et le type de route.
    0.0 = fluide, 1.0 = bloqué
    """
    week_end = jour_semaine in (4, 5)

    # Urbain = plus de trafic
    base_trafic = 0.3 if distance_km < 10 else 0.1

    if week_end:
        return random.uniform(0.0, base_trafic + 0.2)
    else:
        if 7 <= heure <= 9 or 17 <= heure <= 19:  # Rush hours
            return random.uniform(0.4, 0.9)
        elif 12 <= heure <= 14:
            return random.uniform(0.2, 0.5)
        elif 22 <= heure or heure <= 5:
            return random.uniform(0.0, 0.1)
        else:
            return random.uniform(0.1, 0.4)


def generer_ligne():
    # Features de base
    if random.random() < 0.60:
        distance_km = random.uniform(0.5, 40)
    else:
        distance_km = random.uniform(40, 500)

    poids_kg = round(random.uniform(0.1, 200), 2)
    heure = random.randint(0, 23)
    jour_semaine = random.randint(0, 6)

    # Nouvelles features : météo et trafic
    conditions_meteo, temperature, visibilite, coeff_meteo = generer_conditions_meteo()
    niveau_trafic = generer_niveau_trafic(heure, jour_semaine, distance_km)

    # Calcul durée avec tous les facteurs
    vitesse = vitesse_base_kmh(distance_km)
    coeff_heure = coeff_congestion_heure(heure, jour_semaine)
    coeff_trafic = 1.0 + (niveau_trafic * 2.0)  # trafic 0.5 → ×1.5

    temps_route = (distance_km / vitesse) * 60 * coeff_heure * coeff_meteo * coeff_trafic
    manutention = temps_manutention_minutes(poids_kg)

    duree = temps_route + manutention
    duree *= random.gauss(1.0, 0.05)  # Bruit ±5%
    duree = max(5.0, duree)
    duree = round(duree, 2)

    return {
        "distance_km": round(distance_km, 3),
        "poids_kg": poids_kg,
        "heure": heure,
        "jour_semaine": jour_semaine,
        "conditions_meteo": conditions_meteo,  # NOUVELLE
        "niveau_trafic": round(niveau_trafic, 3),  # NOUVELLE
        "temperature": round(temperature, 1),  # NOUVELLE
        "visibilite": visibilite,  # NOUVELLE
        "duree_minutes": duree,
    }


def generer_dataset(n=8000):
    return [generer_ligne() for _ in range(n)]


if __name__ == "__main__":
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    rows = generer_dataset(8000)

    with open(OUTPUT, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=rows[0].keys())
        writer.writeheader()
        writer.writerows(rows)

    print(f"✅  Dataset durée généré : {len(rows)} lignes → {OUTPUT}")
    print(f"   Exemple : {rows[0]}")
    durees = [r["duree_minutes"] for r in rows]
    print(f"   Min={min(durees):.1f} min  Max={max(durees):.1f} min  Moy={sum(durees)/len(durees):.1f} min")

    # Stats météo
    meteo_counts = {}
    for r in rows:
        m = r["conditions_meteo"]
        meteo_counts[m] = meteo_counts.get(m, 0) + 1

    meteo_labels = {0: "Normal", 1: "Pluie", 2: "Neige", 3: "Orage"}
    print("\n   Répartition météo :")
    for code, count in sorted(meteo_counts.items()):
        pct = (count / len(rows)) * 100
        print(f"     {meteo_labels[code]:<10} : {count:>5} ({pct:>5.1f}%)")