package com.example.forumapp.services;

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

    private final String apiKey;
    private final HttpClient httpClient;
    private static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    public AiService() {
        this.apiKey = loadApiKey();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        System.out.println("[AiService-GEMINI] Initialisé avec Gemini (gemini-2.5-flash)");
    }

    private String loadApiKey() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("forum.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            System.out.println("[AiService-GEMINI] Erreur chargement clé: " + e.getMessage());
        }
        return props.getProperty("gemini.api.key", "");
    }

    public String summarizePost(String content) {
        try {
            if (content == null || content.isBlank()) {
                return "Contenu vide.";
            }
            if (content.length() < 150) {
                return "Le post est deja court.";
            }

            String systemPrompt = "Tu es un assistant qui resume des posts de forum en 2-3 phrases maximum en francais.";
            String prompt = systemPrompt + "\n\nTexte à résumer :\n" + content;

            String response = callGeminiApi(prompt);
            System.out.println("[AiService-GEMINI] Résumé généré");
            return response;
        } catch (Exception e) {
            System.out.println("[AiService-GEMINI] Erreur résumé: " + e.getMessage());
            return "Erreur lors du résumé : " + e.getMessage();
        }
    }

    public String generateAdminResponse(String content) {
        try {
            if (content == null || content.isBlank()) {
                return "Contenu vide.";
            }

            String systemPrompt = "Tu es un administrateur professionnel d un forum de livraison de colis en Tunisie. Reponds de facon professionnelle et bienveillante en francais avec parfois une touche de dialecte tunisien.";
            String prompt = systemPrompt + "\n\nPost de l'utilisateur :\n" + content;

            String response = callGeminiApi(prompt);
            System.out.println("[AiService-GEMINI] Réponse générée");
            return response;
        } catch (Exception e) {
            System.out.println("[AiService-GEMINI] Erreur génération réponse: " + e.getMessage());
            return "Erreur lors de la génération de réponse : " + e.getMessage();
        }
    }

    private String callGeminiApi(String prompt) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Gemini API key non configurée dans forum.properties");
        }

        JSONObject partsObj = new JSONObject();
        partsObj.put("text", prompt);

        JSONArray partsArray = new JSONArray();
        partsArray.put(partsObj);

        JSONObject contentObj = new JSONObject();
        contentObj.put("parts", partsArray);

        JSONArray contentsArray = new JSONArray();
        contentsArray.put(contentObj);

        JSONObject requestBody = new JSONObject();
        requestBody.put("contents", contentsArray);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_ENDPOINT + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[AiService-GEMINI] Status HTTP: " + response.statusCode());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error " + response.statusCode() + " : " + response.body());
        }

        return parseGeminiResponse(response.body());
    }

    private String parseGeminiResponse(String jsonResponse) {
        try {
            JSONObject root = new JSONObject(jsonResponse);
            JSONArray candidates = root.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject content = firstCandidate.getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text").trim();
                }
            }
            return "Pas de réponse pertinente trouvée.";
        } catch (Exception e) {
            System.out.println("[AiService-GEMINI] Erreur parsing JSON: " + e.getMessage());
            return "Erreur lors de la lecture de la réponse de l'IA.";
        }
    }
}
