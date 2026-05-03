package com.example.forumapp.components;

import com.example.forumapp.services.AiService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.concurrent.CompletableFuture;

public class AIResponseAssistant extends VBox {

    private final AiService aiService;
    private String currentPostContent;

    private TextArea responseArea;
    private Button generateButton;
    private ProgressIndicator loadingIndicator;

    public AIResponseAssistant() {
        this.aiService = new AiService();
        setupUI();
    }

    private void setupUI() {
        this.setSpacing(10);
        this.setPadding(new Insets(10));
        this.setStyle("-fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #f9fafb;");

        Label title = new Label("✨ Assistant de Réponse IA");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #374151;");

        Label description = new Label("Générez une réponse polie avec une touche de dialecte tunisien (darja) pour ce post.");
        description.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        description.setWrapText(true);

        generateButton = new Button("Générer la réponse");
        generateButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 5;");
        generateButton.setDisable(true); // Désactivé jusqu'à ce qu'un post soit défini

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(20, 20);
        loadingIndicator.setVisible(false);

        HBox buttonBox = new HBox(10, generateButton, loadingIndicator);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        responseArea = new TextArea();
        responseArea.setPromptText("La réponse générée par l'IA apparaîtra ici. Vous pouvez la modifier avant de l'envoyer.");
        responseArea.setWrapText(true);
        responseArea.setPrefRowCount(4);
        responseArea.setStyle("-fx-control-inner-background: #ffffff; -fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 13px;");

        generateButton.setOnAction(e -> generateResponse());

        this.getChildren().addAll(title, description, buttonBox, responseArea);
    }

    /**
     * Définit le contenu du post actuel pour lequel on souhaite générer une réponse.
     * @param postContent Le texte du post.
     */
    public void setPostContent(String postContent) {
        this.currentPostContent = postContent;
        this.generateButton.setDisable(postContent == null || postContent.trim().isEmpty());
    }

    /**
     * Retourne la réponse actuellement dans la zone de texte (générée ou modifiée).
     */
    public String getGeneratedResponse() {
        return responseArea.getText();
    }

    private void generateResponse() {
        if (currentPostContent == null || currentPostContent.trim().isEmpty()) {
            return;
        }

        generateButton.setDisable(true);
        loadingIndicator.setVisible(true);
        responseArea.setText("Analyse en cours, veuillez patienter...");

        CompletableFuture.supplyAsync(() -> aiService.generateAdminResponse(currentPostContent))
                .thenAccept(response -> Platform.runLater(() -> {
                    responseArea.setText(response);
                    generateButton.setDisable(false);
                    loadingIndicator.setVisible(false);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        responseArea.setText("Erreur lors de la génération : " + ex.getMessage());
                        generateButton.setDisable(false);
                        loadingIndicator.setVisible(false);
                    });
                    return null;
                });
    }
}
