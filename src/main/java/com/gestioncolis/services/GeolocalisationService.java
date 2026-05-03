package com.gestioncolis.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service de géolocalisation utilisant Nominatim (OpenStreetMap).
 * Fonctions :
 *   - Géocodage d'adresses → coordonnées GPS
 *   - Calcul de distance à vol d'oiseau (Haversine)
 */
public class GeolocalisationService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final Pattern COORDS_PATTERN =
            Pattern.compile("^\\s*(-?\\d{1,3}(?:\\.\\d+)?)\\s*,\\s*(-?\\d{1,3}(?:\\.\\d+)?)\\s*$");

    /**
     * Géocode une adresse vers des coordonnées [lat, lon].
     * Accepte :
     *   - Adresses textuelles : "Avenue Bourguiba, Tunis, Tunisie"
     *   - Coordonnées directes : "36.8190, 10.1658"
     *
     * @return [lat, lon] ou null si échec
     */
    public double[] geocoder(String adresse) {
        if (adresse == null || adresse.isBlank()) return null;

        // Tenter d'extraire des coordonnées directes
        Matcher m = COORDS_PATTERN.matcher(adresse.trim());
        if (m.matches()) {
            try {
                double lat = Double.parseDouble(m.group(1));
                double lon = Double.parseDouble(m.group(2));
                if (lat >= 30.0 && lat <= 38.0 && lon >= 7.5 && lon <= 12.0) {
                    return new double[]{lat, lon};
                }
            } catch (NumberFormatException ignored) {}
        }

        // Géocodage via Nominatim
        try {
            String encoded = URLEncoder.encode(adresse, StandardCharsets.UTF_8);
            String urlStr = NOMINATIM_URL + "?format=json&q=" + encoded
                    + "&accept-language=fr&limit=1&countrycodes=tn";

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "PackTrack/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }

            String json = sb.toString();
            if (json.startsWith("[{")) {
                double lat = parseDouble(json, "lat");
                double lon = parseDouble(json, "lon");
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            System.err.println("[Geolocalisation] Erreur géocodage : " + e.getMessage());
        }

        return null;
    }

    /**
     * Calcule la distance à vol d'oiseau entre deux points GPS (Haversine).
     * @return distance en kilomètres
     */
    public double calculerDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Rayon de la Terre en km

        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dphi = Math.toRadians(lat2 - lat1);
        double dlam = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dphi / 2) * Math.sin(dphi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(dlam / 2) * Math.sin(dlam / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Calcule la distance entre une localisation et une adresse.
     * @return distance en km, ou null si échec de géocodage
     */
    public Double calculerDistanceVersAdresse(double[] localisationDepart, String adresseArrivee) {
        if (localisationDepart == null || localisationDepart.length != 2) return null;

        double[] coordsArrivee = geocoder(adresseArrivee);
        if (coordsArrivee == null) return null;

        return calculerDistance(
                localisationDepart[0], localisationDepart[1],
                coordsArrivee[0], coordsArrivee[1]
        );
    }

    // ── Parsing JSON minimal ──────────────────────────────────────────
    private double parseDouble(String json, String key) {
        String marker = "\"" + key + "\"";
        int idx = json.indexOf(marker);
        if (idx < 0) throw new IllegalArgumentException("Clé introuvable : " + key);
        int colon = json.indexOf(':', idx);
        int start = colon + 1;
        int end = json.length();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == ',' || c == '}') { end = i; break; }
        }
        String value = json.substring(start, end).trim();
        value = value.replace("\"", "");
        return Double.parseDouble(value);
    }
}