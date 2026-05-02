package com.gestioncolis.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.gestioncolis.utils.EnvLoader;
/**
 * Service météo utilisant OpenWeatherMap API.
 *
 * La clé API est lue (par ordre de priorité) depuis :
 *   1. Variable d'environnement système OPENWEATHER_API_KEY
 *   2. Fichier .env à la racine du projet
 *
 * Impact sur la livraison :
 *   - Pluie/neige → vitesse réduite
 *   - Température extrême → précautions supplémentaires
 *   - Visibilité faible → ralentissement
 */
public class MeteoService {

    private static final String API_KEY =
            EnvLoader.get("OPENWEATHER_API_KEY", "");

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    /**
     * Récupère les conditions météo pour une position GPS.
     *
     * @return MeteoResult avec conditions actuelles
     */
    public MeteoResult obtenirMeteo(double lat, double lon) {
        if (API_KEY.isBlank()) {
            System.out.println("[Meteo] ⚠ Clé API non configurée (OPENWEATHER_API_KEY absent du .env) — fallback conditions normales");
            return MeteoResult.conditionsNormales();
        }

        try {
            String urlStr = String.format("%s?lat=%.5f&lon=%.5f&appid=%s&units=metric&lang=fr",
                    BASE_URL, lat, lon, API_KEY);

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                System.out.println("[Meteo] Erreur HTTP " + conn.getResponseCode());
                return MeteoResult.conditionsNormales();
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }

            return parseMeteo(sb.toString());

        } catch (Exception e) {
            System.err.println("[Meteo] Erreur : " + e.getMessage());
            return MeteoResult.conditionsNormales();
        }
    }

    private MeteoResult parseMeteo(String json) {
        try {
            // Parse manuel du JSON (pas de dépendance externe)
            String description = parseString(json, "description");
            double temperature = parseDouble(json, "temp");
            double vitesseVent = parseDouble(json, "speed"); // m/s
            int visibilite = (int) parseDouble(json, "visibility"); // mètres

            // Détection des conditions météo
            String weatherMain = parseString(json, "main");
            boolean pluie = weatherMain.toLowerCase().contains("rain") ||
                    description.toLowerCase().contains("pluie");
            boolean neige = weatherMain.toLowerCase().contains("snow") ||
                    description.toLowerCase().contains("neige");
            boolean brouillard = weatherMain.toLowerCase().contains("fog") ||
                    weatherMain.toLowerCase().contains("mist") ||
                    visibilite < 1000;
            boolean orage = weatherMain.toLowerCase().contains("thunder") ||
                    description.toLowerCase().contains("orage");

            return new MeteoResult(
                    temperature, vitesseVent, visibilite,
                    pluie, neige, brouillard, orage, description
            );

        } catch (Exception e) {
            System.err.println("[Meteo] Parse error : " + e.getMessage());
            return MeteoResult.conditionsNormales();
        }
    }

    private String parseString(String json, String key) {
        String marker = "\"" + key + "\"";
        int idx = json.indexOf(marker);
        if (idx < 0) return "";
        int start = json.indexOf('"', idx + marker.length() + 1);
        if (start < 0) return "";
        int end = json.indexOf('"', start + 1);
        return (end < 0) ? "" : json.substring(start + 1, end);
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
     * DTO - Résultat météo.
     */
    public static class MeteoResult {
        private final double temperature;      // °C
        private final double vitesseVent;      // m/s
        private final int visibilite;          // mètres
        private final boolean pluie;
        private final boolean neige;
        private final boolean brouillard;
        private final boolean orage;
        private final String description;

        public MeteoResult(double temperature, double vitesseVent, int visibilite,
                           boolean pluie, boolean neige, boolean brouillard,
                           boolean orage, String description) {
            this.temperature = temperature;
            this.vitesseVent = vitesseVent;
            this.visibilite = visibilite;
            this.pluie = pluie;
            this.neige = neige;
            this.brouillard = brouillard;
            this.orage = orage;
            this.description = description;
        }

        public static MeteoResult conditionsNormales() {
            return new MeteoResult(20, 3, 10000, false, false, false, false, "Conditions normales");
        }

        /**
         * Coefficient multiplicateur de temps de trajet (1.0 = normal).
         * Conditions défavorables augmentent le temps.
         */
        public double coefficientImpact() {
            double coeff = 1.0;

            if (pluie) coeff *= 1.15;              // +15% sous la pluie
            if (neige) coeff *= 1.40;              // +40% sous la neige
            if (orage) coeff *= 1.25;              // +25% orage
            if (brouillard) coeff *= 1.30;         // +30% brouillard
            if (vitesseVent > 15) coeff *= 1.10;   // +10% vent fort
            if (temperature < 5) coeff *= 1.08;    // +8% froid intense
            if (temperature > 38) coeff *= 1.05;   // +5% chaleur extrême

            return coeff;
        }

        public String getSeverite() {
            if (orage || neige) return "SEVERE";
            if (pluie || brouillard) return "MODERATE";
            return "NORMAL";
        }

        public String getIcone() {
            if (orage) return "⛈️";
            if (neige) return "❄️";
            if (pluie) return "🌧️";
            if (brouillard) return "🌫️";
            if (temperature > 30) return "☀️";
            if (temperature < 10) return "🥶";
            return "☁️";
        }

        // Getters
        public double getTemperature() { return temperature; }
        public double getVitesseVent() { return vitesseVent; }
        public int getVisibilite() { return visibilite; }
        public boolean isPluie() { return pluie; }
        public boolean isNeige() { return neige; }
        public boolean isBrouillard() { return brouillard; }
        public boolean isOrage() { return orage; }
        public String getDescription() { return description; }

        @Override
        public String toString() {
            return String.format("%s %s | %.1f°C | Vent %.1f m/s | Impact ×%.2f",
                    getIcone(), description, temperature, vitesseVent, coefficientImpact());
        }
    }
}