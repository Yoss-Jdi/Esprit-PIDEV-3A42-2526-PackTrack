import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

from flask import Flask, request, jsonify
import joblib
import numpy as np
import os

app = Flask(__name__)

MODEL_PATH = "model/model_ca.pkl"

if not os.path.exists(MODEL_PATH):
    raise FileNotFoundError(f"Modele introuvable : {MODEL_PATH}")

meta            = joblib.load(MODEL_PATH)
model           = meta["model"]
last_mois_index = meta["last_mois_index"]
last_annee      = meta["last_annee"]
last_mois       = meta["last_mois"]

print(f"[OK] Modele charge : {meta['model_name']} | MAE={meta['mae']} | R2={meta['r2']}")
print(f"   Donnees jusqu a : {last_mois:02d}/{last_annee}")


@app.route("/predict", methods=["POST"])
def predict():
    try:
        data  = request.get_json(force=True)
        mois  = int(data.get("mois",  0))
        annee = int(data.get("annee", 0))
        if not (1 <= mois <= 12):
            return jsonify({"error": "Mois invalide (1-12)"}), 400
        if annee < 2020:
            return jsonify({"error": "Annee invalide"}), 400
        delta      = (annee - last_annee) * 12 + (mois - last_mois)
        mois_index = last_mois_index + delta
        X          = np.array([[mois_index,
                                np.sin(2 * np.pi * mois / 12),
                                np.cos(2 * np.pi * mois / 12)]])
        ca = max(0, round(float(model.predict(X)[0]), 2))
        return jsonify({"mois": mois, "annee": annee, "ca_predit": ca,
                        "model": meta["model_name"], "mae": meta["mae"]})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/predict-range", methods=["POST"])
def predict_range():
    try:
        data    = request.get_json(force=True)
        nb_mois = int(data.get("nb_mois", 6))
        nb_mois = max(1, min(nb_mois, 24))

        mois_noms = ["Jan","Fev","Mar","Avr","Mai","Juin",
                     "Juil","Aout","Sep","Oct","Nov","Dec"]

        predictions = []
        annee_c = last_annee
        mois_c  = last_mois

        for i in range(1, nb_mois + 1):
            mois_c += 1
            if mois_c > 12:
                mois_c  = 1
                annee_c += 1
            mois_index = last_mois_index + i
            X = np.array([[mois_index,
                           np.sin(2 * np.pi * mois_c / 12),
                           np.cos(2 * np.pi * mois_c / 12)]])
            ca = max(0, round(float(model.predict(X)[0]), 2))
            predictions.append({
                "mois":      mois_c,
                "annee":     annee_c,
                "ca_predit": ca,
                "label":     f"{mois_noms[mois_c - 1]} {annee_c}"
            })

        return jsonify({"predictions": predictions, "mae": meta["mae"],
                        "model": meta["model_name"]})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/history", methods=["GET"])
def history():
    try:
        import mysql.connector
        DB_CONFIG = {
            "host":     "localhost",
            "port":     3306,
            "user":     "root",
            "password": "",
            "database": "gestioncolis"
        }
        cnx    = mysql.connector.connect(**DB_CONFIG)
        cursor = cnx.cursor()
        cursor.execute("""
            SELECT YEAR(dateEmission), MONTH(dateEmission), SUM(montantTTC)
            FROM factures
            WHERE statut != 'Annulee' AND dateEmission IS NOT NULL
            GROUP BY YEAR(dateEmission), MONTH(dateEmission)
            ORDER BY YEAR(dateEmission), MONTH(dateEmission)
        """)
        rows = cursor.fetchall()
        cursor.close()
        cnx.close()

        mois_noms = ["Jan","Fev","Mar","Avr","Mai","Juin",
                     "Juil","Aout","Sep","Oct","Nov","Dec"]
        historique = []
        for annee, mois, ca in rows:
            if annee and mois and ca:
                historique.append({
                    "mois":  int(mois),
                    "annee": int(annee),
                    "ca":    round(float(ca), 2),
                    "label": f"{mois_noms[int(mois)-1]} {int(annee)}"
                })
        return jsonify({"historique": historique})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "model": meta["model_name"],
                    "r2": meta["r2"], "mae": meta["mae"]})


if __name__ == "__main__":
    print("Demarrage Flask sur http://localhost:5000")
    app.run(host="0.0.0.0", port=5000, debug=False)
