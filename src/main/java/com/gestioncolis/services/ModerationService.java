package com.gestioncolis.services;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModerationService {

    // Liste locale de mots inappropriés (français, anglais, dialecte)
    private static final java.util.List<String> BAD_WORDS = java.util.Arrays.asList(
            "merde", "putain", "connard", "salope", "fuck", "bitch", "asshole", "shit",
            "zab", "zbi", "namak", "KILL", "MORT", "kahba", "9ahba", "zebi", "zib", "nik", "mnayek", "nayek");

    private boolean containsBadWords(String text) {
        if (text == null || text.isBlank())
            return false;
        String lowerText = text.toLowerCase();
        for (String word : BAD_WORDS) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(lowerText).find()) {
                return true;
            }
        }
        return false;
    }

    // OPENAI: Clé API OpenAI et paramètres
    private final String apiKey;
    private final HttpClient client;
    private final ConfigService configService;
    private static final String OPENAI_MODERATION_ENDPOINT = "https://api.openai.com/v1/moderations";
    private static final int TIMEOUT_SECONDS = 10;

    public ModerationService(ConfigService configService) {
        this.configService = configService;
        // OPENAI: Charger la clé depuis forum.properties
        this.apiKey = loadApiKey();
        this.client = HttpClient.newHttpClient();
        System.out.println("[MODERATION-OPENAI] Clé chargée: "
                + (apiKey.isBlank() ? "VIDE !" : "OK (" + apiKey.substring(0, 8) + "...)"));
    }

    // OPENAI: Charger la clé depuis forum.properties avec la clé "openai.api.key"
    private String loadApiKey() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("forum.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                System.out.println("[MODERATION-OPENAI] forum.properties introuvable !");
            }
        } catch (Exception e) {
            System.out.println("[MODERATION-OPENAI] Erreur chargement clé: " + e.getMessage());
        }
        return props.getProperty("openai.api.key", "");
    }

    /**
     * OPENAI: Retourne true si le texte est signalé comme inapproprié par l'API
     * OpenAI. Retourne false si clé vide ou erreur réseau (fail-open).
     */
    public boolean isInappropriate(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        // Vérification locale d'abord
        if (containsBadWords(text)) {
            System.out.println("[MODERATION-LOCAL] Mot inapproprié détecté localement.");
            return true;
        }

        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("[MODERATION-OPENAI] Clé vide, modération ignorée.");
            return false;
        }

        try {
            String jsonResponse = callOpenAiModerationApi(text);
            System.out.println("[MODERATION-OPENAI] Réponse: " + jsonResponse);
            boolean flagged = parseFlaggedStatus(jsonResponse);
            System.out.println("[MODERATION-OPENAI] Contenu signalé: " + flagged);
            return flagged;
        } catch (Exception e) {
            System.out.println("[MODERATION-OPENAI] Erreur réseau: " + e.getMessage());
            // OPENAI: Fail-open - accepter en cas d'erreur
            return false;
        }
    }

    /**
     * OPENAI: Retourne un résumé lisible des violations détectées.
     */
    public String getViolationSummary(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // Vérification locale d'abord
        if (containsBadWords(text)) {
            return "utilisation de vocabulaire inapproprié ou vulgaire";
        }

        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }

        try {
            String jsonResponse = callOpenAiModerationApi(text);
            return buildViolationSummary(jsonResponse);
        } catch (Exception e) {
            System.out.println("[MODERATION-OPENAI] Erreur: " + e.getMessage());
            return "contenu inapproprié";
        }
    }

    // OPENAI: Appelle l'API de modération OpenAI
    private String callOpenAiModerationApi(String text) throws Exception {
        String safeText = escapeJsonString(text);

        // OPENAI: Construire le JSON pour OpenAI Moderations API
        String jsonBody = """
                {
                  "input": "%s"
                }
                """.formatted(safeText);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_MODERATION_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[MODERATION-OPENAI] Status HTTP: " + response.statusCode());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "OpenAI Moderation API status " + response.statusCode() + " : " + response.body());
        }

        return response.body();
    }

    // OPENAI: Parser le statut "flagged" depuis la réponse JSON
    private boolean parseFlaggedStatus(String json) {
        // OPENAI: Chercher "flagged":true ou "flagged":false dans la première entrée de
        // results
        Pattern pattern = Pattern.compile("\"flagged\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }
        return false;
    }

    // OPENAI: Construire un résumé lisible des catégories de violation
    private String buildViolationSummary(String json) {
        StringBuilder summary = new StringBuilder();

        // OPENAI: Traduire les catégories de violation OpenAI en français
        if (json.contains("\"hate\":true")) {
            summary.append("discours haineux, ");
        }
        if (json.contains("\"harassment\":true")) {
            summary.append("harcelement, ");
        }
        if (json.contains("\"violence\":true")) {
            summary.append("contenu violent, ");
        }
        if (json.contains("\"self-harm\":true")) {
            summary.append("automutilation, ");
        }
        if (json.contains("\"sexual\":true")) {
            summary.append("contenu sexuel, ");
        }

        if (summary.length() > 0) {
            return summary.substring(0, summary.length() - 2);
        }
        return "contenu inapproprié";
    }

    private String escapeJsonString(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
