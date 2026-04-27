package com.example.rayen.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OllamaService {
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String DEFAULT_MODEL = "llama3";
    private static final Pattern JSON_STRING_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private final HttpClient httpClient;
    private final URI endpoint;
    private final String model;

    public OllamaService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
        this.endpoint = URI.create(lireConfiguration("ollama.url", "OLLAMA_URL", DEFAULT_OLLAMA_URL));
        this.model = lireConfiguration("ollama.model", "OLLAMA_MODEL", DEFAULT_MODEL);
    }

    public String ask(String prompt) {
        String promptNormalise = prompt == null ? "" : prompt.trim();
        if (promptNormalise.isEmpty()) {
            throw new IllegalArgumentException("Le message a envoyer a Ollama est vide.");
        }

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(construirePayload(promptNormalise), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de contacter Ollama sur " + endpoint
                    + ". Verifiez que le serveur Ollama est lance.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La requete vers Ollama a ete interrompue.", exception);
        }

        String body = response.body() == null ? "" : response.body();
        if (response.statusCode() >= 400) {
            String erreur = extraireValeurJson(body, "error");
            if (erreur.isBlank()) {
                erreur = "Reponse HTTP " + response.statusCode() + " recue depuis Ollama.";
            }
            throw new IllegalStateException(erreur);
        }

        String reponse = extraireValeurJson(body, "response");
        if (reponse.isBlank()) {
            throw new IllegalStateException("Ollama a repondu, mais aucun texte exploitable n'a ete trouve.");
        }

        return reponse.trim();
    }

    public String getModel() {
        return model;
    }

    private String construirePayload(String prompt) {
        return """
                {
                  "model": "%s",
                  "prompt": "%s",
                  "stream": false,
                  "options": {
                    "temperature": 0.3
                  }
                }
                """.formatted(echapperJson(model), echapperJson(prompt));
    }

    private String extraireValeurJson(String body, String cle) {
        Pattern pattern = Pattern.compile(String.format(JSON_STRING_PATTERN.pattern(), Pattern.quote(cle)));
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            return "";
        }

        return desechapperJson(matcher.group(1));
    }

    private String echapperJson(String valeur) {
        return valeur
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String desechapperJson(String valeur) {
        StringBuilder resultat = new StringBuilder();

        for (int index = 0; index < valeur.length(); index++) {
            char caractere = valeur.charAt(index);
            if (caractere != '\\' || index == valeur.length() - 1) {
                resultat.append(caractere);
                continue;
            }

            char suivant = valeur.charAt(++index);
            switch (suivant) {
                case '"' -> resultat.append('"');
                case '\\' -> resultat.append('\\');
                case '/' -> resultat.append('/');
                case 'b' -> resultat.append('\b');
                case 'f' -> resultat.append('\f');
                case 'n' -> resultat.append('\n');
                case 'r' -> resultat.append('\r');
                case 't' -> resultat.append('\t');
                case 'u' -> {
                    if (index + 4 >= valeur.length()) {
                        resultat.append("\\u");
                        break;
                    }

                    String codeHexadecimal = valeur.substring(index + 1, index + 5);
                    try {
                        resultat.append((char) Integer.parseInt(codeHexadecimal, 16));
                        index += 4;
                    } catch (NumberFormatException exception) {
                        resultat.append("\\u").append(codeHexadecimal);
                        index += 4;
                    }
                }
                default -> resultat.append(suivant);
            }
        }

        return resultat.toString();
    }

    private String lireConfiguration(String cleSysteme, String cleEnvironnement, String valeurParDefaut) {
        String valeurSysteme = System.getProperty(cleSysteme);
        if (valeurSysteme != null && !valeurSysteme.isBlank()) {
            return valeurSysteme.trim();
        }

        String valeurEnvironnement = System.getenv(cleEnvironnement);
        if (valeurEnvironnement != null && !valeurEnvironnement.isBlank()) {
            return valeurEnvironnement.trim();
        }

        return valeurParDefaut;
    }
}
