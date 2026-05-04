"""
train_model.py
--------------
Connexion MySQL → extraction CA mensuel → entraînement → sauvegarde model_ca.pkl
Lancer une seule fois : python train_model.py
"""

import mysql.connector
import pandas as pd
import numpy as np
from sklearn.linear_model import LinearRegression
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error, r2_score
import joblib
import os

# ─── CONFIG BASE DE DONNÉES ───────────────────────────────────────────────────
DB_CONFIG = {
    "host":     "localhost",
    "port":     3306,
    "user":     "root",
    "password": "0000",           # ← ton mot de passe MySQL
    "database": "TrackPackDB"
}

# ─── 1. EXTRACTION DES DONNÉES ────────────────────────────────────────────────
print("Connexion à la base de données...")
cnx = mysql.connector.connect(**DB_CONFIG)
cursor = cnx.cursor()

query = """
    SELECT
        YEAR(dateEmission)  AS annee,
        MONTH(dateEmission) AS mois,
        SUM(montantTTC)     AS ca_total
    FROM factures
    WHERE statut != 'Annulée'
      AND dateEmission IS NOT NULL
    GROUP BY YEAR(dateEmission), MONTH(dateEmission)
    ORDER BY annee, mois
"""

cursor.execute(query)
rows = cursor.fetchall()
cursor.close()
cnx.close()

# Construire DataFrame manuellement (évite le warning SQLAlchemy)
df = pd.DataFrame(rows, columns=["annee", "mois", "ca_total"])

# ─── 2. NETTOYAGE ─────────────────────────────────────────────────────────────
print(f"\nDonnées brutes ({len(df)} lignes) :")
print(df.to_string())

#  Supprimer les lignes avec NaN (dates nulles en BDD)
df = df.dropna(subset=["annee", "mois", "ca_total"])
df["annee"]    = df["annee"].astype(int)
df["mois"]     = df["mois"].astype(int)
df["ca_total"] = df["ca_total"].astype(float)
df = df.sort_values(["annee", "mois"]).reset_index(drop=True)

print(f"\nDonnées après nettoyage ({len(df)} lignes) :")
print(df.to_string())

if len(df) < 3:
    print("\n⚠ Pas assez de données réelles — utilisation de données simulées.")
    df = pd.DataFrame({
        "annee":    [2025]*12 + [2026]*4,
        "mois":     list(range(1, 13)) + [1, 2, 3, 4],
        "ca_total": [
            3200, 3500, 2900, 4100, 4400, 3800,
            5100, 4700, 4200, 5500, 6000, 7200,
            4100, 4500, 4900, 5200
        ]
    })

# ─── 3. FEATURE ENGINEERING ───────────────────────────────────────────────────
df["mois_index"] = range(1, len(df) + 1)
df["sin_mois"]   = np.sin(2 * np.pi * df["mois"] / 12)
df["cos_mois"]   = np.cos(2 * np.pi * df["mois"] / 12)

X = df[["mois_index", "sin_mois", "cos_mois"]].values
y = df["ca_total"].values

print(f"\nFeatures shape : {X.shape}")

# ─── 4. ENTRAÎNEMENT ──────────────────────────────────────────────────────────
# Avec peu de données → pas de split, on entraîne sur tout
rf = RandomForestRegressor(n_estimators=100, random_state=42)
lr = LinearRegression()

rf.fit(X, y)
lr.fit(X, y)

mae_rf = mean_absolute_error(y, rf.predict(X))
mae_lr = mean_absolute_error(y, lr.predict(X))

print(f"\nMAE Random Forest  : {mae_rf:.2f}")
print(f"MAE Linear Reg.    : {mae_lr:.2f}")

best_model = rf if mae_rf <= mae_lr else lr
best_name  = "RandomForest" if mae_rf <= mae_lr else "LinearRegression"
print(f"Meilleur modèle    : {best_name}")

r2 = r2_score(y, best_model.predict(X))
print(f"R²                 : {r2:.3f}")

# ─── 5. SAUVEGARDE ────────────────────────────────────────────────────────────
meta = {
    "model":           best_model,
    "last_mois_index": int(df["mois_index"].max()),
    "last_annee":      int(df["annee"].max()),
    "last_mois":       int(df["mois"].max()),
    "model_name":      best_name,
    "mae":             round(min(mae_rf, mae_lr), 2),
    "r2":              round(r2, 3)
}

os.makedirs("model", exist_ok=True)
joblib.dump(meta, "model/model_ca.pkl")
print("\n Modèle sauvegardé dans model/model_ca.pkl")

# ─── 6. TEST ──────────────────────────────────────────────────────────────────
next_mois_index = meta["last_mois_index"] + 1
next_mois       = (meta["last_mois"] % 12) + 1
X_next = np.array([[
    next_mois_index,
    np.sin(2 * np.pi * next_mois / 12),
    np.cos(2 * np.pi * next_mois / 12)
]])
prediction = best_model.predict(X_next)[0]
print(f"Test prédiction mois suivant (mois {next_mois:02d}) : {prediction:.2f} TND")