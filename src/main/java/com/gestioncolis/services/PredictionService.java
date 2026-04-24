package com.gestioncolis.services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Client HTTP Java pour le service ML Flask.
 *
 * ┌──────────────────────────────────────────────────────────┐
 * │  POST http://localhost:5001/predict-complet              │
 * │                                                          │
 * │  Payload  : adresse_depart, adresse_destination,        │
 * │             poids_kg, date_debut                        │
 * │                                                          │
 * │  Réponse  : distance_km, duree_minutes, duree_formatee  │
 * └──────────────────────────────────────────────────────────┘
 *
 * Aucune dépendance externe : JSON construit/parsé manuellement.
 * Fallback silencieux : renvoie toujours un {@link PredictionResult}.
 */
public class PredictionService {

    private static final String BASE_URL    = "http://localhost:5001";
    private static final String PREDICT_URL = BASE_URL + "/predict-complet";
    private static final String HEALTH_URL  = BASE_URL + "/health";
    private static final int    TIMEOUT_MS  = 15_000;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ─────────────────────────────────────────────────────────────────
    // API publique
    // ─────────────────────────────────────────────────────────────────

    /**
     * Prédit la distance (km) et la durée (minutes) pour une livraison.
     *
     * @param adresseDepart      adresse complète de départ
     * @param adresseDestination adresse complète de destination
     * @param poidsKg            poids du colis en kg
     * @param dateDebut          date/heure de prise en charge (maintenant si null)
     * @return {@link PredictionResult} — jamais null, même en cas d'erreur
     */
    public PredictionResult predire(String adresseDepart,
                                    String adresseDestination,
                                    double poidsKg,
                                    LocalDateTime dateDebut) {
        if (dateDebut == null) dateDebut = LocalDateTime.now();

        String payload = buildJson(adresseDepart, adresseDestination, poidsKg, dateDebut);

        try {
            String body = post(PREDICT_URL, payload);
            return parseResponse(body);
        } catch (IOException e) {
            System.err.println("[PredictionService] Réseau : " + e.getMessage());
            return new PredictionResult("Service ML indisponible (" + e.getMessage() + ")");
        } catch (Exception e) {
            System.err.println("[PredictionService] Inattendu : " + e.getMessage());
            return new PredictionResult("Erreur prédiction : " + e.getMessage());
        }
    }

    /**
     * Vérifie rapidement (3 s timeout) que le service Flask est accessible.
     */
    public boolean estDisponible() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(HEALTH_URL).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3_000);
            conn.setReadTimeout(3_000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // HTTP
    // ─────────────────────────────────────────────────────────────────

    private String post(String urlStr, String jsonBody) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept",       "application/json");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        conn.disconnect();

        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + " : " + sb);
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────
    // Construction JSON (sans bibliothèque externe)
    // ─────────────────────────────────────────────────────────────────

    private String buildJson(String adresseDepart, String adresseDestination,
                             double poidsKg, LocalDateTime dateDebut) {
        return "{"
                + "\"adresse_depart\":"       + jsonStr(adresseDepart)      + ","
                + "\"adresse_destination\":"  + jsonStr(adresseDestination) + ","
                + "\"poids_kg\":"             + poidsKg                     + ","
                + "\"date_debut\":"           + jsonStr(dateDebut.format(FMT))
                + "}";
    }

    private String jsonStr(String s) {
        if (s == null) return "null";
        return "\""
                + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                + "\"";
    }

    // ─────────────────────────────────────────────────────────────────
    // Parsing JSON minimal (sans bibliothèque externe)
    // Contrat de l'API Flask :
    //   { "distance_km": 12.4, "duree_minutes": 35,
    //     "duree_formatee": "35 min", ... }
    // ─────────────────────────────────────────────────────────────────

    private PredictionResult parseResponse(String json) {
        try {
            double distanceKm   = parseDouble(json, "distance_km");
            double dureeMinutes = parseDouble(json, "duree_minutes");
            String dureeFormat  = parseString(json, "duree_formatee");

            System.out.printf("[PredictionService] ✅ %.2f km | %.0f min | %s%n",
                    distanceKm, dureeMinutes, dureeFormat);

            return new PredictionResult(distanceKm, dureeMinutes, dureeFormat);

        } catch (Exception e) {
            System.err.println("[PredictionService] Parse error : " + e.getMessage()
                    + " | body=" + json);
            return new PredictionResult("Réponse ML illisible : " + e.getMessage());
        }
    }

    /** Extrait un double : "key": 12.4 */
    private double parseDouble(String json, String key) {
        return Double.parseDouble(extractToken(json, key).trim());
    }

    /** Extrait une String : "key": "valeur" */
    private String parseString(String json, String key) {
        String marker = "\"" + key + "\"";
        int idx = json.indexOf(marker);
        if (idx < 0) return "-";
        int start = json.indexOf('"', idx + marker.length() + 1);
        if (start < 0) return "-";
        int end = json.indexOf('"', start + 1);
        return (end < 0) ? "-" : json.substring(start + 1, end);
    }

    /** Extrait le token brut (nombre) associé à une clé. */
    private String extractToken(String json, String key) {
        String marker = "\"" + key + "\"";
        int idx = json.indexOf(marker);
        if (idx < 0) throw new IllegalArgumentException("Clé introuvable : " + key);
        int colon = json.indexOf(':', idx + marker.length());
        int end   = json.length();
        for (int i = colon + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == ',' || c == '}') { end = i; break; }
        }
        return json.substring(colon + 1, end).trim();
    }
}