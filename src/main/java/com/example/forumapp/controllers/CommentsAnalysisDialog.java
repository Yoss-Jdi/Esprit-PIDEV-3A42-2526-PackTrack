package com.example.forumapp.controllers;

import com.example.forumapp.services.CommentsAnalysisService;
import com.example.forumapp.utils.AlertUtils;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CommentsAnalysisDialog {

    private Stage stage;
    private TextArea resultTextArea;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    private CommentsAnalysisService analysisService;

    public CommentsAnalysisDialog(CommentsAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * Affiche la dialogue d'analyse des commentaires.
     */
    public void show(long postId, String postTitle) {
        stage = new Stage();
        stage.setTitle("Analyse des Commentaires - " + postTitle);
        stage.setWidth(900);
        stage.setHeight(700);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setStyle("-fx-font-family: 'Segoe UI', Arial; -fx-font-size: 11;");

        // ========== HEADER ==========
        VBox header = createHeader(postTitle);
        root.setTop(header);

        // ========== CENTER - Résultat ==========
        resultTextArea = new TextArea();
        resultTextArea.setWrapText(true);
        resultTextArea.setEditable(false);
        resultTextArea.setPadding(new Insets(10));
        resultTextArea.setStyle(
                "-fx-control-inner-background: #f5f5f5;"
                + "-fx-font-family: 'Consolas', monospace;"
                + "-fx-font-size: 11;"
                + "-fx-text-fill: #333;"
        );
        resultTextArea.setText("En attente d'analyse...");

        ScrollPane scrollPane = new ScrollPane(resultTextArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-padding: 0; -fx-border-color: #ddd;");
        root.setCenter(scrollPane);

        // ========== FOOTER - Statut et Boutons ==========
        HBox footer = createFooter();
        root.setBottom(footer);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        // Lancer l'analyse en arrière-plan
        analyzeCommentsAsync(postId);
    }

    private VBox createHeader(String postTitle) {
        VBox vbox = new VBox(10);
        vbox.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 10 0;");

        Label titleLabel = new Label("📊 Analyse des Commentaires");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1976d2;");

        Label postLabel = new Label("Post: " + postTitle);
        postLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");

        vbox.getChildren().addAll(titleLabel, postLabel);
        return vbox;
    }

    private HBox createFooter() {
        HBox hbox = new HBox(10);
        hbox.setPadding(new Insets(10, 0, 0, 0));
        hbox.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");

        // Indicateur de chargement
        progressIndicator = new ProgressIndicator();
        progressIndicator.setStyle("-fx-padding: 0;");
        progressIndicator.setPrefSize(25, 25);

        // Label de statut
        statusLabel = new Label("Analyse en cours...");
        statusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");

        // Bouton Copier
        Button copyButton = new Button("📋 Copier");
        copyButton.setStyle("-fx-padding: 6 12; -fx-font-size: 11; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand;");
        copyButton.setOnAction(e -> copyToClipboard());

        // Bouton Fermer
        Button closeButton = new Button("✕ Fermer");
        closeButton.setStyle("-fx-padding: 6 12; -fx-font-size: 11; -fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
        closeButton.setOnAction(e -> stage.close());

        HBox.setHgrow(statusLabel, Priority.ALWAYS);
        hbox.getChildren().addAll(progressIndicator, statusLabel, copyButton, closeButton);

        return hbox;
    }

    /**
     * Lance l'analyse de manière asynchrone.
     */
    private void analyzeCommentsAsync(long postId) {
        Thread analysisThread = new Thread(() -> {
            try {
                System.out.println("[DIALOG] Démarrage de l'analyse pour post_id: " + postId);
                String result = analysisService.analyzePostComments(postId);

                // Mettre à jour l'UI sur le thread JavaFX
                javafx.application.Platform.runLater(() -> {
                    resultTextArea.setText(result);
                    statusLabel.setText("✅ Analyse complétée");
                    progressIndicator.setVisible(false);
                });

            } catch (Exception e) {
                System.out.println("[DIALOG] ❌ Erreur lors de l'analyse: " + e.getMessage());
                e.printStackTrace();

                // Afficher l'erreur dans l'UI
                javafx.application.Platform.runLater(() -> {
                    String errorMsg = "❌ Erreur lors de l'analyse:\n\n" + e.getMessage()
                            + "\n\nAssurez-vous que Ollama est en cours d'exécution sur localhost:11434";
                    resultTextArea.setText(errorMsg);
                    resultTextArea.setStyle(
                            "-fx-control-inner-background: #ffebee;"
                            + "-fx-font-family: 'Consolas', monospace;"
                            + "-fx-font-size: 11;"
                            + "-fx-text-fill: #c62828;"
                    );
                    statusLabel.setText("❌ Erreur: " + e.getMessage());
                    progressIndicator.setVisible(false);
                });
            }
        });

        analysisThread.setDaemon(true);
        analysisThread.start();
    }

    /**
     * Copie le contenu de la TextArea vers le presse-papiers.
     */
    private void copyToClipboard() {
        String text = resultTextArea.getText();
        if (text != null && !text.isEmpty()) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
            AlertUtils.showInfo("Succès", "Contenu copié dans le presse-papiers!");
        }
    }
}
