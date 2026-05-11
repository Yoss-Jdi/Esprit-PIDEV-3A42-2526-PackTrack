package com.gestioncolis.controllers;

import com.gestioncolis.services.OllamaService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class chatbot {
    private static final int MAX_CONTEXT_MESSAGES = 10;
    private static final String SYSTEM_PROMPT = """
            You are Rayen Assistant for the JavaFX application Rayen Fleet Manager.
            Help users with vehicles, technicians, assignments, statistics, PDF export and email notifications.
            Give concise, practical answers.
            If the user asks about missing live data, say clearly that you do not have direct database access from the chat.
            Reply in the same language as the user's latest message when possible.
            """;

    private OllamaService ollamaService;
    private final List<ConversationTurn> historiqueConversation = new ArrayList<>();
    private boolean requeteEnCours;

    @FXML
    private Label assistantStatusLabel;
    @FXML
    private ScrollPane messagesScrollPane;
    @FXML
    private VBox messagesBox;
    @FXML
    private FlowPane quickActionsPane;
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;

    // ✅ Constructeur par défaut (obligation JavaFX FXML)
    public chatbot() {
        this(null);
    }

    public chatbot(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @FXML
    private void initialize() {
        // ✅ Initialiser OllamaService en lazy si nécessaire
        if (ollamaService == null) {
            try {
                ollamaService = new OllamaService();
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de l'initialisation d'OllamaService: " + e.getMessage());
                assistantStatusLabel.setText("Ollama indisponible");
                return;
            }
        }
        
        assistantStatusLabel.setText("Assistant local pret - Ollama / " + ollamaService.getModel());
        ajouterMessageAssistant("Bonjour, je suis Rayen Assistant.\nJe peux vous aider pour les vehicules, les techniciens, les statistiques, les PDF et les emails.");
        Platform.runLater(messageField::requestFocus);
    }

    @FXML
    private void handleSend() {
        if (requeteEnCours) {
            return;
        }

        String message = messageField.getText() == null ? "" : messageField.getText().trim();
        if (message.isBlank()) {
            return;
        }

        envoyerMessageUtilisateur(message);
    }

    @FXML
    private void handleQuickVehicule() {
        envoyerMessageUtilisateur("Explique-moi comment ajouter un vehicule dans Rayen Fleet Manager.");
    }

    @FXML
    private void handleQuickTechnicien() {
        envoyerMessageUtilisateur("Comment affecter un ou plusieurs vehicules a un technicien ?");
    }

    @FXML
    private void handleQuickStats() {
        envoyerMessageUtilisateur("Comment lire le graphique des statistiques des techniciens ?");
    }

    @FXML
    private void handleQuickDocuments() {
        envoyerMessageUtilisateur("Explique les notifications email et la generation PDF dans l'application.");
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) sendButton.getScene().getWindow();
        stage.close();
    }

    private void envoyerMessageUtilisateur(String message) {
        if (requeteEnCours) {
            return;
        }

        messageField.clear();
        historiqueConversation.add(new ConversationTurn("Utilisateur", message));
        ajouterMessageUtilisateur(message);

        HBox bulleAttente = ajouterMessage("Rayen Assistant est en train d'ecrire...", false, true);
        requeteEnCours = true;
        mettreAJourDisponibiliteSaisie(false);
        assistantStatusLabel.setText("Connexion a Ollama...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return ollamaService.ask(construirePromptConversation());
            }
        };

        task.setOnSucceeded(event -> {
            messagesBox.getChildren().remove(bulleAttente);

            String reponse = task.getValue() == null ? "" : task.getValue().trim();
            if (reponse.isBlank()) {
                reponse = "Je n'ai pas recu de reponse exploitable depuis Ollama.";
            }

            historiqueConversation.add(new ConversationTurn("Assistant", reponse));
            ajouterMessageAssistant(reponse);
            assistantStatusLabel.setText("Reponse generee avec Ollama / " + ollamaService.getModel());
            mettreAJourDisponibiliteSaisie(true);
        });

        task.setOnFailed(event -> {
            messagesBox.getChildren().remove(bulleAttente);

            Throwable erreur = task.getException();
            String messageErreur = erreur == null || erreur.getMessage() == null || erreur.getMessage().isBlank()
                    ? "Impossible de joindre Ollama. Verifiez que le serveur local est lance."
                    : erreur.getMessage();

            ajouterMessageAssistant("Je ne peux pas repondre pour le moment.\n" + messageErreur);
            assistantStatusLabel.setText("Ollama indisponible");
            mettreAJourDisponibiliteSaisie(true);
        });

        Thread thread = new Thread(task, "ollama-chatbot-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private String construirePromptConversation() {
        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT)
                .append("\nConversation recente :\n");

        int indexDebut = Math.max(0, historiqueConversation.size() - MAX_CONTEXT_MESSAGES);
        for (int index = indexDebut; index < historiqueConversation.size(); index++) {
            ConversationTurn tour = historiqueConversation.get(index);
            prompt.append(tour.role()).append(" : ").append(tour.content()).append('\n');
        }

        prompt.append("Assistant :");
        return prompt.toString();
    }

    private void mettreAJourDisponibiliteSaisie(boolean disponible) {
        requeteEnCours = !disponible;
        messageField.setDisable(!disponible);
        sendButton.setDisable(!disponible);
        quickActionsPane.setDisable(!disponible);
        if (disponible) {
            Platform.runLater(messageField::requestFocus);
        }
    }

    private void ajouterMessageAssistant(String contenu) {
        ajouterMessage(contenu, false, false);
    }

    private void ajouterMessageUtilisateur(String contenu) {
        ajouterMessage(contenu, true, false);
    }

    private HBox ajouterMessage(String contenu, boolean utilisateur, boolean attente) {
        Label messageLabel = new Label(contenu);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(220);
        messageLabel.getStyleClass().add("chat-message-text");

        StackPane bulle = new StackPane(messageLabel);
        bulle.getStyleClass().add(attente
                ? "chat-bubble-loading"
                : utilisateur ? "chat-bubble-user" : "chat-bubble-assistant");

        HBox ligne = new HBox(10);
        ligne.setAlignment(utilisateur ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Region espaceFlexible = new Region();
        HBox.setHgrow(espaceFlexible, Priority.ALWAYS);

        if (utilisateur) {
            ligne.getChildren().addAll(espaceFlexible, bulle);
        } else {
            ligne.getChildren().addAll(bulle, espaceFlexible);
        }

        messagesBox.getChildren().add(ligne);
        faireDefilerEnBas();
        return ligne;
    }

    private void faireDefilerEnBas() {
        Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
    }

    private record ConversationTurn(String role, String content) {
    }
}
