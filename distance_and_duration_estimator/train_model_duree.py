"""
train_model_duree.py
─────────────────────────────
Entraîne un modèle de durée avec features météo et trafic.

Features (8 au total) :
  [0] distance_km
  [1] poids_kg
  [2] heure
  [3] jour_semaine
  [4] conditions_meteo  (0-3)
  [5] niveau_trafic     (0.0-1.0)
  [6] temperature       (°C)
  [7] visibilite        (m)
"""

import os
import pickle
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

BASE_DIR = os.path.dirname(__file__)
DATA_PATH = os.path.join(BASE_DIR, "data", "dataset_duree.csv")
MODEL_PATH = os.path.join(BASE_DIR, "models", "model_duree.pkl")

FEATURES = [
    "distance_km", "poids_kg", "heure", "jour_semaine",
    "conditions_meteo", "niveau_trafic", "temperature", "visibilite"
]
TARGET = "duree_minutes"


def charger_donnees():
    df = pd.read_csv(DATA_PATH)
    print(f"  Données chargées : {len(df)} lignes")
    print(f"  Colonnes : {list(df.columns)}")
    print(f"\n  Stats target :\n{df[TARGET].describe().to_string()}\n")

    # Stats météo
    print("  Répartition météo :")
    meteo_map = {0: "Normal", 1: "Pluie", 2: "Neige", 3: "Orage"}
    for code, label in meteo_map.items():
        count = (df["conditions_meteo"] == code).sum()
        pct = (count / len(df)) * 100
        print(f"    {label:<10} : {count:>5} ({pct:>5.1f}%)")

    print(f"\n  Trafic moyen : {df['niveau_trafic'].mean():.2f}")
    print(f"  Température moyenne : {df['temperature'].mean():.1f}°C\n")

    return df


def entrainer():
    df = charger_donnees()

    X = df[FEATURES].values
    y = df[TARGET].values

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.15, random_state=42
    )

    # GradientBoosting pour capturer les interactions complexes
    gb = GradientBoostingRegressor(
        n_estimators=300,
        max_depth=8,
        learning_rate=0.05,
        subsample=0.8,
        random_state=42,
    )

    model = Pipeline([
        ("scaler", StandardScaler()),
        ("gb", gb),
    ])

    print("  Entraînement GradientBoosting durée enrichi…")
    model.fit(X_train, y_train)

    # Évaluation
    y_pred = model.predict(X_test)
    y_pred = np.maximum(y_pred, 5.0)

    mae = mean_absolute_error(y_test, y_pred)
    r2 = r2_score(y_test, y_pred)

    cv_scores = cross_val_score(model, X_train, y_train,
                                cv=5, scoring="neg_mean_absolute_error")
    cv_mae = -cv_scores.mean()

    print(f"\n  ── Résultats modèle durée enrichi ──────────")
    print(f"  MAE  (test) : {mae:.2f} min")
    print(f"  R²   (test) : {r2:.4f}")
    print(f"  MAE  (CV-5) : {cv_mae:.2f} min")

    print(f"\n  Exemples de prédictions :")
    for i in range(min(8, len(X_test))):
        print(f"    Réel={y_test[i]:.1f} min  Prédit={y_pred[i]:.1f} min"
              f"  Δ={abs(y_test[i]-y_pred[i]):.1f} min")

    # Importance des features
    gb_model = model.named_steps["gb"]
    print(f"\n  Importance des features :")
    importances = sorted(zip(FEATURES, gb_model.feature_importances_),
                         key=lambda x: x[1], reverse=True)
    for feat, imp in importances:
        print(f"    {feat:<20} : {imp:.4f}")

    # Sauvegarde
    os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)
    with open(MODEL_PATH, "wb") as f:
        pickle.dump(model, f)

    print(f"\n  ✅ Modèle durée enrichi sauvegardé → {MODEL_PATH}")
    return model


if __name__ == "__main__":
    print("═" * 60)
    print("  ENTRAÎNEMENT — Modèle Durée Enrichi (Météo + Trafic)")
    print("═" * 60)
    entrainer()