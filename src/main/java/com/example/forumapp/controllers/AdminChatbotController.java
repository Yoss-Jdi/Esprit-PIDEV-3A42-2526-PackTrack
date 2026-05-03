package com.example.forumapp.controllers;

import com.example.forumapp.services.ChatbotService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AdminChatbotController {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox chatContainer;
    @FXML private TextField inputField;
    @FXML private Button sendButton;

    private ChatbotService chatbotService;

    @FXML
    public void initialize() {
        // Auto-scroll to bottom when new messages are added
        chatContainer.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue(1.0);
        });
    }

    public void setChatbotService(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
        addBotMessage("Hello Admin. I am DB-Insight. You can ask me for stats, to find a user, or to delete a post by its ID.");
    }

    @FXML
    private void handleSend() {
        String input = inputField.getText();
        if (input == null || input.trim().isEmpty()) return;
        
        addUserMessage(input);
        inputField.clear();

        if (chatbotService != null) {
            // Process query in background thread if it takes time, but for now we'll do it synchronously
            // or in a Platform.runLater if we want UI updates
            String response = chatbotService.processQuery(input);
            addBotMessage(response);
        } else {
            addBotMessage("Error: ChatbotService is not initialized.");
        }
    }

    private void addUserMessage(String message) {
        Label label = new Label("Admin:\n" + message);
        label.setStyle("-fx-background-color: #d1ecf1; -fx-text-fill: #0c5460; -fx-padding: 10px; -fx-background-radius: 10px;");
        label.setWrapText(true);
        label.setMaxWidth(280);
        
        HBox hbox = new HBox(label);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        
        chatContainer.getChildren().add(hbox);
    }

    private void addBotMessage(String message) {
        Label label = new Label("🤖 DB-Insight:\n" + message);
        label.setStyle("-fx-background-color: #e2e3e5; -fx-text-fill: #383d41; -fx-padding: 10px; -fx-background-radius: 10px;");
        label.setWrapText(true);
        label.setMaxWidth(280);
        
        HBox hbox = new HBox(label);
        hbox.setAlignment(Pos.CENTER_LEFT);
        
        chatContainer.getChildren().add(hbox);
    }
}
