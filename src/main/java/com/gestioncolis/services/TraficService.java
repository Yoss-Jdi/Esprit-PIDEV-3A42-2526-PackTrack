package com.gestioncolis.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.gestioncolis.utils.EnvLoader;

/**
 * Service trafic utilisant TomTom Traffic Flow API.
 *
 * La clé API est lue (par ordre de priorité) depuis :
 *   1. Variable d'environnement système TOMTOM_API_KEY
 *   2. Fichier .env à la racine du projet
 *
 * Impact sur la livraison : trafic dense → temps de trajet augmenté.
 */
public class TraficService {

    private static final String API_KEY =
            EnvLoader.get("TOMTOM_API_KEY", "");

    private static final String BASE_URL = "https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json";

    /**
     * Analyse le trafic autour d'une position GPS.
     *
     * @return TraficResult avec niveau de congestion
     */
    public TraficResult analyserTrafic(double lat, double lon) {
        if (API_KEY.isBlank()) {
            System.out.println("[Trafic] ⚠ Clé API non configurée (TOMTOM_API_KEY absent du .env) — fallback trafic normal");
            return TraficResult.traficNormal();
        }

        try {
            String urlStr = String.format("%s?key=%s&point=%.5f,%.5f&unit=KMPH",
                    BASE_URL, API_KEY, lat, lon);

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                System.out.println("[Trafic] Erreur HTTP " + conn.getResponseCode());
                return TraficResult.traficNormal();
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }

            return parseTrafic(sb.toString());

        } catch (Exception e) {
            System.err.println("[Trafic] Erreur : " + e.getMessage());
            return TraficResult.traficNormal();
        }
    }

    private TraficResult parseTrafic(String json) {
        try {
            double vitesseActuelle = parseDouble(json, "currentSpeed");
            double vitesseLimite = parseDouble(json, "freeFlowSpeed");
            double confidence = parseDouble(json, "confidence");

            // Calcul de la congestion (0.0 = fluide, 1.0 = bloqué)
            double congestion = 0.0;
            if (vitesseLimite > 0) {
                congestion = 1.0 - (vitesseActuelle / vitesseLimite);
                congestion = Math.max(0.0, Math.min(1.0, congestion));
            }

            return new TraficResult(vitesseActuelle, vitesseLimite, congestion, confidence);

        } catch (Exception e) {
            System.err.println("[Trafic] Parse error : " + e.getMessage());
            return TraficResult.traficNormal();
        }
    }

    private double parseDouble(String json, String key) {
        try {
            String marker = "\"" + key + "\"";
            int idx = json.indexOf(marker);
            if (idx < 0) return 0.0;
            int colon = json.indexOf(':', idx);
            int end = json.length();
            for (int i = colon + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == ',' || c == '}') { end = i; break; }
            }
            String value = json.substring(colon + 1, end).trim();
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * DTO - Résultat trafic.
     */
    public static class TraficResult {
        private final double vitesseActuelle;  // km/h
        private final double vitesseLimite;    // km/h
        private final double congestion;       // 0.0-1.0
        private final double confidence;       // 0.0-1.0

        public TraficResult(double vitesseActuelle, double vitesseLimite,
                            double congestion, double confidence) {
            this.vitesseActuelle = vitesseActuelle;
            this.vitesseLimite = vitesseLimite;
            this.congestion = congestion;
            this.confidence = confidence;
        }

        public static TraficResult traficNormal() {
            return new TraficResult(50, 50, 0.0, 0.5);
        }

        /**
         * Coefficient multiplicateur de temps de trajet (1.0 = normal).
         * Congestion élevée augmente le temps.
         */
        public double coefficientImpact() {
            // congestion 0.0 → ×1.0 (fluide)
            // congestion 0.5 → ×1.5 (ralentissement)
            // congestion 1.0 → ×3.0 (bloqué)
            return 1.0 + (congestion * 2.0);
        }

        public String getNiveau() {
            if (congestion < 0.2) return "FLUIDE";
            if (congestion < 0.5) return "MODERE";
            if (congestion < 0.8) return "DENSE";
            return "BLOQUE";
        }

        public String getIcone() {
            if (congestion < 0.2) return "🟢";
            if (congestion < 0.5) return "🟡";
            if (congestion < 0.8) return "🟠";
            return "🔴";
        }

        // Getters
        public double getVitesseActuelle() { return vitesseActuelle; }
        public double getVitesseLimite() { return vitesseLimite; }
        public double getCongestion() { return congestion; }
        public double getConfidence() { return confidence; }

        @Override
        public String toString() {
            return String.format("%s Trafic %s | %.0f km/h (limite %.0f) | Impact ×%.2f",
                    getIcone(), getNiveau(), vitesseActuelle, vitesseLimite, coefficientImpact());
        }
    }
}