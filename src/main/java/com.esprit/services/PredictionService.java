package com.esprit.services;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PredictionService {

    private static final String BASE_URL = "http://localhost:5000";

    // ─── Prédiction 1 mois ────────────────────────────────────────────────────
    public double predireCA(int mois, int annee) {
        try {
            String body = "{\"mois\":" + mois + ",\"annee\":" + annee + "}";
            String json = post(BASE_URL + "/predict", body);
            if (json == null) return -1;
            return extraireDouble(json, "ca_predit");
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    // ─── Prédiction N mois (pour le graphique) ────────────────────────────────
    /**
     * Retourne une liste de double[] : [mois, annee, ca_predit]
     * pour les nbMois prochains mois.
     */
    public List<double[]> predirePlusieursMois(int nbMois) {
        List<double[]> result = new ArrayList<>();
        try {
            String body = "{\"nb_mois\":" + nbMois + "}";
            String json = post(BASE_URL + "/predict-range", body);
            if (json == null) return result;

            // Parse le tableau "predictions"
            // Format : {"mois":7,"annee":2026,"ca_predit":1073.4,"label":"Juil 2026"}
            String[] entries = json.split("\\{");
            for (String entry : entries) {
                if (!entry.contains("ca_predit")) continue;
                double mois     = extraireDouble(entry, "\"mois\"");
                double annee    = extraireDouble(entry, "\"annee\"");
                double caPredit = extraireDouble(entry, "ca_predit");
                String label    = extraireString(entry, "label");

                if (mois > 0 && caPredit >= 0) {
                    result.add(new double[]{mois, annee, caPredit});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Retourne les labels "Juil 2026" pour affichage sur l'axe X.
     */
    public List<String> labelsPlusieursMois(int nbMois) {
        List<String> labels = new ArrayList<>();
        try {
            String body = "{\"nb_mois\":" + nbMois + "}";
            String json = post(BASE_URL + "/predict-range", body);
            if (json == null) return labels;

            String[] entries = json.split("\\{");
            for (String entry : entries) {
                if (!entry.contains("label")) continue;
                String label = extraireString(entry, "label");
                if (label != null && !label.isEmpty()) {
                    labels.add(label);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return labels;
    }

    // ─── Historique depuis Flask ───────────────────────────────────────────────
    /**
     * Retourne les données historiques : liste de double[] [mois, annee, ca]
     */
    public List<double[]> getHistorique() {
        List<double[]> result = new ArrayList<>();
        try {
            String json = get(BASE_URL + "/history");
            if (json == null) return result;

            String[] entries = json.split("\\{");
            for (String entry : entries) {
                if (!entry.contains("\"ca\"")) continue;
                double mois  = extraireDouble(entry, "\"mois\"");
                double annee = extraireDouble(entry, "\"annee\"");
                double ca    = extraireDouble(entry, "\"ca\"");
                if (mois > 0 && ca >= 0) {
                    result.add(new double[]{mois, annee, ca});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<String> labelsHistorique() {
        List<String> labels = new ArrayList<>();
        try {
            String json = get(BASE_URL + "/history");
            if (json == null) return labels;
            String[] entries = json.split("\\{");
            for (String entry : entries) {
                if (!entry.contains("label")) continue;
                String label = extraireString(entry, "label");
                if (label != null && !label.isEmpty()) {
                    labels.add(label);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return labels;
    }

    // ─── MAE du modèle ────────────────────────────────────────────────────────
    public double getMAE() {
        try {
            String json = get(BASE_URL + "/health");
            if (json == null) return -1;
            return extraireDouble(json, "mae");
        } catch (Exception e) {
            return -1;
        }
    }

    // ─── HTTP helpers ─────────────────────────────────────────────────────────
    private String post(String urlStr, String body) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            InputStream is = (status == 200) ? conn.getInputStream() : conn.getErrorStream();
            return lire(is);
        } catch (java.net.ConnectException e) {
            System.err.println("Flask non démarré ! Lance : python app.py");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String get(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            return lire(conn.getInputStream());
        } catch (Exception e) {
            return null;
        }
    }

    private String lire(InputStream is) {
        if (is == null) return null;
        Scanner sc = new Scanner(is, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        while (sc.hasNextLine()) sb.append(sc.nextLine());
        sc.close();
        return sb.toString();
    }

    // ─── JSON parsers simples ─────────────────────────────────────────────────
    private double extraireDouble(String json, String cle) {
        int idx = json.indexOf("\"" + cle.replace("\"", "") + "\":");
        if (idx < 0) idx = json.indexOf(cle + ":");
        if (idx < 0) return -1;
        int start = json.indexOf(":", idx) + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
                || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        try { return Double.parseDouble(json.substring(start, end)); }
        catch (Exception e) { return -1; }
    }

    private String extraireString(String json, String cle) {
        String search = "\"" + cle + "\":\"";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        int start = idx + search.length();
        int end   = json.indexOf("\"", start);
        if (end < 0) return "";
        return json.substring(start, end);
    }
}