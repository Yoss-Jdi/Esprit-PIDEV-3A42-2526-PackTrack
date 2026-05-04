"""
Setup complet du système ML enrichi :
  1. Dataset distance (6000 lignes)
  2. Dataset durée enrichi météo+trafic (8000 lignes)
  3. Modèle distance
  4. Modèle durée enrichi

"""

import sys
import os

sys.path.insert(0, os.path.dirname(__file__))

print("=" * 70)
print("  PackTrack ML Enrichi — Initialisation complète")
print("=" * 70)

# Dataset distance (inchangé)
print("\n▶ Étape 1/4 — Génération dataset distance…")
from generate_dataset_distance import generer_dataset as gen_dist, OUTPUT as OUT_DIST
import csv

os.makedirs(os.path.dirname(OUT_DIST), exist_ok=True)
rows_dist = gen_dist(6000)
with open(OUT_DIST, "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=rows_dist[0].keys())
    writer.writeheader()
    writer.writerows(rows_dist)
print(f"  ✅ {len(rows_dist)} lignes → {OUT_DIST}")

# Dataset durée ENRICHI
print("\n▶ Étape 2/4 — Génération dataset durée enrichi (météo + trafic)…")
from generate_dataset_duree import generer_dataset as gen_dur, OUTPUT as OUT_DUR
rows_dur = gen_dur(8000)
with open(OUT_DUR, "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=rows_dur[0].keys())
    writer.writeheader()
    writer.writerows(rows_dur)
print(f"  ✅ {len(rows_dur)} lignes → {OUT_DUR}")

# Modèle distance
print("\n▶ Étape 3/4 — Entraînement modèle distance…")
from train_model_distance import entrainer as train_dist
train_dist()

# Modèle durée enrichi
print("\n▶ Étape 4/4 — Entraînement modèle durée enrichi…")
from train_model_duree import entrainer as train_dur
train_dur()

print("\n" + "=" * 70)
print("  ✅ Système ML enrichi prêt !")
print("\n  Configuration requise :")
print("\n  Lancement du service :")
print("     python app.py")
print("=" * 70)