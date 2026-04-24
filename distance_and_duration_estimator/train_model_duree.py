"""
train_model_duree.py
────────────────────
Entraîne un RandomForestRegressor pour prédire la durée d'une livraison.

Features (ordre important, cohérent avec app.py) :
  [0] distance_km
  [1] poids_kg
  [2] heure
  [3] jour_semaine

Target : duree_minutes

Sauvegarde : models/model_duree.pkl
"""

import os
import pickle
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

BASE_DIR   = os.path.dirname(__file__)
DATA_PATH  = os.path.join(BASE_DIR, "data",   "dataset_duree.csv")
MODEL_PATH = os.path.join(BASE_DIR, "models", "model_duree.pkl")

FEATURES = ["distance_km", "poids_kg", "heure", "jour_semaine"]
TARGET   = "duree_minutes"


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
    # heure et jour_semaine sont des features cycliques / catégorielles
    # → RF gère bien ça sans encodage supplémentaire.
    rf = RandomForestRegressor(
        n_estimators=200,
        max_depth=20,          # profondeur limitée pour éviter l'overfitting
        min_samples_leaf=3,
        n_jobs=-1,
        random_state=42,
    )

    model = Pipeline([
        ("scaler", StandardScaler()),
        ("rf", rf),
    ])

    print("  Entraînement RandomForest durée…")
    model.fit(X_train, y_train)

    # ── Évaluation ────────────────────────────────────────────────────
    y_pred = model.predict(X_test)
    # Garde-fou : minimum 5 min (cohérent avec app.py)
    y_pred = np.maximum(y_pred, 5.0)

    mae = mean_absolute_error(y_test, y_pred)
    r2  = r2_score(y_test, y_pred)

    cv_scores = cross_val_score(model, X_train, y_train,
                                cv=5, scoring="neg_mean_absolute_error")
    cv_mae = -cv_scores.mean()

    print(f"\n  ── Résultats modèle durée ─────────────────────")
    print(f"  MAE  (test) : {mae:.2f} min")
    print(f"  R²   (test) : {r2:.4f}")
    print(f"  MAE  (CV-5) : {cv_mae:.2f} min")

    print(f"\n  Exemples de prédictions :")
    for i in range(min(5, len(X_test))):
        print(f"    Réel={y_test[i]:.1f} min  Prédit={y_pred[i]:.1f} min"
              f"  Δ={abs(y_test[i]-y_pred[i]):.1f} min")

    # Importance des features
    rf_model = model.named_steps["rf"]
    print(f"\n  Importance des features :")
    for feat, imp in zip(FEATURES, rf_model.feature_importances_):
        print(f"    {feat:<20} : {imp:.4f}")

    # ── Sauvegarde ────────────────────────────────────────────────────
    os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)
    with open(MODEL_PATH, "wb") as f:
        pickle.dump(model, f)

    print(f"\n  ✅ Modèle durée sauvegardé → {MODEL_PATH}")
    return model


if __name__ == "__main__":
    print("═" * 55)
    print("  ENTRAÎNEMENT — Modèle Durée")
    print("═" * 55)
    entrainer()