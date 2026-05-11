package com.gestioncolis.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

public class AiService {

    private static final String[] MODELES = {
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash"
    };

    private final String apiKey;
    private final HttpClient httpClient;

    private static final String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    // ===================== CONSTRUCTOR =====================
    public AiService() {
        this.apiKey = loadApiKey();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        System.out.println("[AiService] Initialisé avec Gemini");
    }

    // ===================== API KEY =====================
    private String loadApiKey() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("forum.properties")) {

            if (is != null) {
                props.load(is);
            }

        } catch (Exception e) {
            System.out.println("[AiService] Erreur chargement clé: " + e.getMessage());
        }

        return props.getProperty("gemini.api.key", "");
    }

    // ======================================================
    // 1. DESCRIPTION RECOMPENSE (ton ancien code)
    // ======================================================
    public String genererDescriptionRecompense(String nom, int points) {

        String prompt =
                "Tu es un assistant pour TrackPack. "
                        + "Génère une description courte (2-3 phrases) pour : '"
                        + nom + "' coûtant " + points + " points.";

        for (String modele : MODELES) {
            String result = appelerAPI(modele, prompt);
            if (result != null) return result;
        }

        return "Quota API dépassé. Réessayez plus tard.";
    }

    // ======================================================
    // 2. RESUME POST (ton nouveau système JSON)
    // ======================================================
    public String summarizePost(String content) {
        if (content == null || content.isBlank()) {
            return "Contenu vide.";
        }

        String prompt =
                "Résume ce post en 2-3 phrases en français :\n" + content;

        try {
            return callGemini(prompt);
        } catch (Exception e) {
            return "Erreur résumé: " + e.getMessage();
        }
    }

    // ======================================================
    // 3. REPONSE ADMIN
    // ======================================================
    public String generateAdminResponse(String content) {
        if (content == null || content.isBlank()) {
            return "Contenu vide.";
        }

        String prompt =
                "Tu es un admin pro d’un forum de livraison en Tunisie. "
                        + "Réponds professionnellement :\n" + content;

        try {
            return callGemini(prompt);
        } catch (Exception e) {
            return "Erreur réponse: " + e.getMessage();
        }
    }

    // ======================================================
    // CALL GEMINI (VERSION CLEAN UNIQUE)
    // ======================================================
    private String callGemini(String prompt) throws Exception {

        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Clé Gemini non configurée");
        }

        String url = String.format(GEMINI_ENDPOINT, "gemini-2.5-flash")
                + "?key=" + apiKey;

        JSONObject body = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject contentObj = new JSONObject();
        JSONArray parts = new JSONArray();

        JSONObject textObj = new JSONObject();
        textObj.put("text", prompt);

        parts.put(textObj);
        contentObj.put("parts", parts);
        contents.put(contentObj);
        body.put("contents", contents);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API error: " + response.body());
        }

        return parse(response.body());
    }

    // ======================================================
    // PARSER JSON
    // ======================================================
    private String parse(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray candidates = root.getJSONArray("candidates");

            if (!candidates.isEmpty()) {
                JSONObject content = candidates.getJSONObject(0)
                        .getJSONObject("content");

                JSONArray parts = content.getJSONArray("parts");

                return parts.getJSONObject(0)
                        .getString("text")
                        .trim();
            }

            return "Pas de réponse.";
        } catch (Exception e) {
            return "Erreur parsing JSON.";
        }
    }

    // ======================================================
    // API AVEC FALLBACK MODELES (optionnel conservé)
    // ======================================================
    private String appelerAPI(String modele, String prompt) {
        try {
            String url = String.format(GEMINI_ENDPOINT, modele)
                    + "?key=" + apiKey;

            JSONObject body = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray parts = new JSONArray();

            JSONObject textObj = new JSONObject();
            textObj.put("text", prompt);

            parts.put(textObj);
            contentObj.put("parts", parts);
            contents.put(contentObj);
            body.put("contents", contents);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                return null; // fallback
            }

            if (response.statusCode() != 200) {
                return null;
            }

            return parse(response.body());

        } catch (Exception e) {
            return null;
        }
    }
}