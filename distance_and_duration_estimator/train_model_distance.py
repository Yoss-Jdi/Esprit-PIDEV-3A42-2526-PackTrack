"""
train_model_distance.py
───────────────────────
Entraîne un RandomForestRegressor pour prédire la distance routière
à partir des coordonnées GPS (lat/lon départ + destination).

Features (ordre important, cohérent avec app.py) :
  [0] lat_depart
  [1] lon_depart
  [2] lat_destination
  [3] lon_destination

Target : distance_km

Sauvegarde : models/model_distance.pkl
"""

import os
import pickle
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline

BASE_DIR   = os.path.dirname(__file__)
DATA_PATH  = os.path.join(BASE_DIR, "data",   "dataset_distance.csv")
MODEL_PATH = os.path.join(BASE_DIR, "models", "model_distance.pkl")

FEATURES = ["lat_depart", "lon_depart", "lat_destination", "lon_destination"]
TARGET   = "distance_km"


def charger_donnees():
    df = pd.read_csv(DATA_PATH)
    print(f"  Données chargées : {len(df)} lignes")
    print(f"  Colonnes : {list(df.columns)}")
    print(f"  Stats target :\n{df[TARGET].describe().to_string()}\n")
    return df


def entrainer():
    df = charger_donnees()

    X = df[FEATURES].values
    y = df[TARGET].values

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.15, random_state=42
    )

    # ── Modèle : RandomForest ─────────────────────────────────────────
    # n_estimators=200, max_depth=None → bon compromis biais/variance
    # pour des données GPS avec relations non-linéaires.
    rf = RandomForestRegressor(
        n_estimators=200,
        max_depth=None,
        min_samples_leaf=2,
        n_jobs=-1,
        random_state=42,
    )

    # Pipeline avec scaler (utile si on change de modèle plus tard)
    model = Pipeline([
        ("scaler", StandardScaler()),
        ("rf", rf),
    ])

    print("  Entraînement RandomForest distance…")
    model.fit(X_train, y_train)

    # ── Évaluation ────────────────────────────────────────────────────
    y_pred = model.predict(X_test)
    mae  = mean_absolute_error(y_test, y_pred)
    r2   = r2_score(y_test, y_pred)

    # Cross-validation 5 folds sur les données d'entraînement
    cv_scores = cross_val_score(model, X_train, y_train,
                                cv=5, scoring="neg_mean_absolute_error")
    cv_mae = -cv_scores.mean()

    print(f"\n  ── Résultats modèle distance ──────────────────")
    print(f"  MAE  (test)  : {mae:.3f} km")
    print(f"  R²   (test)  : {r2:.4f}")
    print(f"  MAE  (CV-5)  : {cv_mae:.3f} km")

    # Quelques prédictions de contrôle
    print(f"\n  Exemples de prédictions :")
    for i in range(min(5, len(X_test))):
        print(f"    Réel={y_test[i]:.2f} km  Prédit={y_pred[i]:.2f} km"
              f"  Δ={abs(y_test[i]-y_pred[i]):.2f} km")

    # ── Sauvegarde ────────────────────────────────────────────────────
    os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)
    with open(MODEL_PATH, "wb") as f:
        pickle.dump(model, f)

    print(f"\n  ✅ Modèle distance sauvegardé → {MODEL_PATH}")
    return model


if __name__ == "__main__":
    print("═" * 55)
    print("  ENTRAÎNEMENT — Modèle Distance")
    print("═" * 55)
    entrainer()