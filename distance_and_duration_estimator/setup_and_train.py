"""
setup_and_train.py
──────────────────
Script unique pour initialiser le service ML de zéro :
  1. Génère le dataset distance  (6 000 lignes)
  2. Génère le dataset durée     (7 000 lignes)
  3. Entraîne le modèle distance → models/model_distance.pkl
  4. Entraîne le modèle durée    → models/model_duree.pkl

Usage :
  python setup_and_train.py
"""

import sys
import os

sys.path.insert(0, os.path.dirname(__file__))

print("=" * 60)
print("  PackTrack ML — Initialisation du service")
print("=" * 60)

# ── Étape 1 : Datasets ────────────────────────────────────────────
print("\n▶  Étape 1/4 — Génération dataset distance…")
from generate_dataset_distance import generer_dataset as gen_dist, OUTPUT as OUT_DIST
import csv, os as _os

_os.makedirs(_os.path.dirname(OUT_DIST), exist_ok=True)
rows_dist = gen_dist(6000)
with open(OUT_DIST, "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=rows_dist[0].keys())
    writer.writeheader()
    writer.writerows(rows_dist)
print(f"  ✅ {len(rows_dist)} lignes → {OUT_DIST}")

print("\n▶  Étape 2/4 — Génération dataset durée…")
from generate_dataset_duree import generer_dataset as gen_dur, OUTPUT as OUT_DUR
rows_dur = gen_dur(7000)
with open(OUT_DUR, "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=rows_dur[0].keys())
    writer.writeheader()
    writer.writerows(rows_dur)
print(f"  ✅ {len(rows_dur)} lignes → {OUT_DUR}")

# ── Étape 2 : Entraînement ────────────────────────────────────────
print("\n▶  Étape 3/4 — Entraînement modèle distance…")
from train_model_distance import entrainer as train_dist
train_dist()

print("\n▶  Étape 4/4 — Entraînement modèle durée…")
from train_model_duree import entrainer as train_dur
train_dur()

print("\n" + "=" * 60)
print("  ✅ Tous les modèles sont prêts.")
print("  Lancez maintenant le service avec :")
print("     python app.py")
print("  Le service écoute sur http://localhost:5001")
print("=" * 60)