package com.gestioncolis.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.gestioncolis.entities.Conversation;
import com.gestioncolis.entities.Message;
import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.services.ChatService;
import com.gestioncolis.utils.EmojiPicker;
import com.gestioncolis.utils.SessionManager;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatController implements Initializable {

    @FXML private Label chatStatusLabel;
    @FXML private TextField searchConversations;
    @FXML private VBox conversationsContainer;
    @FXML private ScrollPane conversationsList;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserRole;
    @FXML private VBox newChatPanel;
    @FXML private HBox chatActiveHeader;
    @FXML private Circle onlineStatus;
    @FXML private Label activeContactName;
    @FXML private Label activeContactStatus;
    @FXML private VBox messagesContainer;
    @FXML private ScrollPane messagesScroll;
    @FXML private TextField messageInput;
    @FXML private Label typingIndicator;
    @FXML private VBox welcomeChatBox;
    @FXML private TextField searchUserField;
    @FXML private VBox searchResultsContainer;

    private Utilisateurs currentUser;
    private Conversation currentConversation;
    private int currentContactId;
    private final ChatService chatService = new ChatService();
    private ScheduledExecutorService messagePoller;
    private Timeline typingTimeline;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    // Cache pour les éléments de conversation
    private final Map<Integer, HBox> conversationItemsCache = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupSearchListener();
        setupNewChatSearch();
        startMessagePoller();
    }

    public void setCurrentUser(Utilisateurs user) {
        this.currentUser = user;
        if (user != null) {
            sidebarUserName.setText(user.getPrenom() + " " + user.getNom());
            sidebarUserRole.setText(user.getRole().name());
            sidebarUserRole.getStyleClass().add("role-" + user.getRole().name().toLowerCase());
            loadConversations();
        }
    }

    private void loadConversations() {
        new Thread(() -> {
            try {
                List<Conversation> convs = chatService.getUserConversations(currentUser.getIdUtilisateur());
                Platform.runLater(() -> updateConversationsList(convs));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateConversationsList(List<Conversation> conversations) {
        // Mettre à jour ou créer les éléments sans effacer la liste
        Set<Integer> currentIds = new HashSet<>();

        for (Conversation conv : conversations) {
            currentIds.add(conv.getIdConv());
            HBox existingItem = conversationItemsCache.get(conv.getIdConv());

            if (existingItem != null) {
                // Mettre à jour l'élément existant
                updateConversationItem(existingItem, conv);
            } else {
                // Créer un nouvel élément
                HBox newItem = createConversationItem(conv);
                conversationItemsCache.put(conv.getIdConv(), newItem);
                conversationsContainer.getChildren().add(newItem);

                // Animation d'entrée pour le nouvel élément
                newItem.setOpacity(0);
                newItem.setTranslateX(-20);
                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.millis(150),
                                new KeyValue(newItem.opacityProperty(), 1),
                                new KeyValue(newItem.translateXProperty(), 0)
                        )
                );
                timeline.play();
            }
        }

        // Supprimer les éléments qui n'existent plus
        List<Integer> toRemove = new ArrayList<>();
        for (Map.Entry<Integer, HBox> entry : conversationItemsCache.entrySet()) {
            if (!currentIds.contains(entry.getKey())) {
                conversationsContainer.getChildren().remove(entry.getValue());
                toRemove.add(entry.getKey());
            }
        }
        for (Integer id : toRemove) {
            conversationItemsCache.remove(id);
        }

        // Re-trier les conversations par date du dernier message
        sortConversationsContainer();
    }

    private void sortConversationsContainer() {
        List<Node> children = new ArrayList<>(conversationsContainer.getChildren());
        children.sort((a, b) -> {
            if (!(a instanceof HBox) || !(b instanceof HBox)) return 0;

            Conversation convA = getConversationFromItem((HBox) a);
            Conversation convB = getConversationFromItem((HBox) b);

            if (convA == null && convB == null) return 0;
            if (convA == null) return 1;
            if (convB == null) return -1;

            if (convA.getLastMessageTime() == null && convB.getLastMessageTime() == null) return 0;
            if (convA.getLastMessageTime() == null) return 1;
            if (convB.getLastMessageTime() == null) return -1;

            return convB.getLastMessageTime().compareTo(convA.getLastMessageTime());
        });
        conversationsContainer.getChildren().setAll(children);
    }

    private Conversation getConversationFromItem(HBox item) {
        for (Map.Entry<Integer, HBox> entry : conversationItemsCache.entrySet()) {
            if (entry.getValue() == item) {
                // Créer une conversation temporaire pour le tri
                Conversation temp = new Conversation();
                // On ne peut pas récupérer directement, on utilise le cache
                return null;
            }
        }
        return null;
    }

    private void updateConversationItem(HBox item, Conversation conv) {
        // Mettre à jour l'avatar (garder la même couleur)
        // Mettre à jour le nom
        VBox info = (VBox) item.getChildren().get(1);
        Label nameLabel = (Label) info.getChildren().get(0);
        nameLabel.setText(conv.getContactName());

        // Mettre à jour le dernier message et l'heure
        HBox lastMsgRow = (HBox) info.getChildren().get(1);
        Label lastMsgLabel = (Label) lastMsgRow.getChildren().get(0);
        lastMsgLabel.setText(conv.getLastMessage() != null ? conv.getLastMessage() : "Nouvelle conversation");

        Label timeLabel = (Label) lastMsgRow.getChildren().get(1);
        String timeStr = conv.getLastMessageTime() != null ? conv.getLastMessageTime().format(TIME_FORMAT) : "";
        timeLabel.setText(timeStr);

        // Mettre à jour le badge non lu
        boolean hasBadge = item.getChildren().size() > 3;
        boolean hasDeleteBtn = item.getChildren().size() > (hasBadge ? 4 : 3);

        if (conv.getUnreadCount() > 0) {
            if (!hasBadge) {
                Label badge = new Label(String.valueOf(conv.getUnreadCount()));
                badge.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 16;");
                // Insérer avant le bouton supprimer
                if (hasDeleteBtn) {
                    item.getChildren().add(item.getChildren().size() - 1, badge);
                } else {
                    item.getChildren().add(badge);
                }
            } else {
                Label badge = (Label) item.getChildren().get(3);
                badge.setText(String.valueOf(conv.getUnreadCount()));
                badge.setVisible(true);
                badge.setManaged(true);
            }
        } else if (hasBadge) {
            Label badge = (Label) item.getChildren().get(3);
            badge.setVisible(false);
            badge.setManaged(false);
        }
    }

    private HBox createConversationItem(Conversation conv) {
        HBox item = new HBox();
        item.setUserData(conv.getIdConv());
        item.setAlignment(Pos.CENTER_LEFT);
        item.setSpacing(12);
        item.setPadding(new javafx.geometry.Insets(14, 16, 14, 16));
        item.getStyleClass().add("chat-conversation-item");
        item.setOnMouseClicked(e -> selectConversation(conv));

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setPrefSize(52, 52);
        avatar.setMaxSize(52, 52);
        avatar.setStyle("-fx-background-color: " + getAvatarColor(conv.getIdConv()) + "; -fx-background-radius: 26; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");

        FontIcon avatarIcon = new FontIcon("fas-user");
        avatarIcon.setIconSize(22);
        avatarIcon.setIconColor(Color.WHITE);
        avatar.getChildren().add(avatarIcon);

        // Infos contact
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(conv.getContactName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 15px;");

        HBox lastMsgRow = new HBox(8);
        Label lastMsgLabel = new Label(conv.getLastMessage() != null ? conv.getLastMessage() : "Nouvelle conversation");
        lastMsgLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        lastMsgLabel.setMaxWidth(180);

        String timeStr = conv.getLastMessageTime() != null ? conv.getLastMessageTime().format(TIME_FORMAT) : "";
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");
        lastMsgRow.getChildren().addAll(lastMsgLabel, timeLabel);

        info.getChildren().addAll(nameLabel, lastMsgRow);
        item.getChildren().add(avatar);
        item.getChildren().add(info);

        // Badge non lu
        if (conv.getUnreadCount() > 0) {
            Label badge = new Label(String.valueOf(conv.getUnreadCount()));
            badge.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 16;");
            item.getChildren().add(badge);
        }

        // Bouton supprimer
        Button deleteBtn = new Button();
        FontIcon deleteIcon = new FontIcon("fas-trash-alt");
        deleteIcon.setIconSize(14);
        deleteIcon.setIconColor(Color.web("#ef4444"));
        deleteBtn.setGraphic(deleteIcon);
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6;");
        deleteBtn.setOnAction(e -> {
            e.consume();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Supprimer la conversation");
            alert.setHeaderText(null);
            alert.setContentText("Voulez-vous vraiment supprimer cette conversation avec " + conv.getContactName() + " ?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        chatService.deleteConversation(conv.getIdConv());
                        Platform.runLater(() -> {
                            conversationItemsCache.remove(conv.getIdConv());
                            conversationsContainer.getChildren().remove(item);
                            if (currentConversation != null && currentConversation.getIdConv() == conv.getIdConv()) {
                                currentConversation = null;
                                chatActiveHeader.setVisible(false);
                                welcomeChatBox.setVisible(true);
                            }
                            loadConversations();
                        });
                    } catch (Exception ex) { ex.printStackTrace(); }
                }).start();
            }
        });
        item.getChildren().add(deleteBtn);

        return item;
    }

    private void selectConversation(Conversation conv) {
        this.currentConversation = conv;
        this.currentContactId = conv.getUser1Id() == currentUser.getIdUtilisateur() ? conv.getUser2Id() : conv.getUser1Id();

        chatActiveHeader.setVisible(true);
        chatActiveHeader.setManaged(true);
        welcomeChatBox.setVisible(false);
        welcomeChatBox.setManaged(false);
        newChatPanel.setVisible(false);
        newChatPanel.setManaged(false);

        activeContactName.setText(conv.getContactName());
        activeContactStatus.setText("En ligne");
        onlineStatus.setFill(Color.web("#22c55e"));

        loadMessages();
    }

    private void loadMessages() {
        new Thread(() -> {
            try {
                List<Message> messages = chatService.getConversationMessages(currentConversation.getIdConv(), currentUser.getIdUtilisateur());
                Platform.runLater(() -> updateMessagesList(messages));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void updateMessagesList(List<Message> messages) {
        messagesContainer.getChildren().clear();

        if (messages.isEmpty()) {
            Label emptyLabel = new Label("Aucun message. Envoyez un message pour commencer la conversation !");
            emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-padding: 30; -fx-alignment: center;");
            messagesContainer.getChildren().add(emptyLabel);
        } else {
            for (Message msg : messages) {
                boolean isOwn = msg.getSenderId() == currentUser.getIdUtilisateur();
                addMessageToUI(msg, isOwn);
            }
        }
        scrollToBottom();
    }

    private void addMessageToUI(Message msg, boolean isOwn) {
        HBox messageRow = new HBox();
        messageRow.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageRow.setPadding(new javafx.geometry.Insets(6, 20, 6, 20));
        messageRow.setUserData(msg.getIdMsg());

        VBox messageBubble = new VBox(6);
        messageBubble.setMaxWidth(400);
        messageBubble.getStyleClass().add(isOwn ? "chat-message-own" : "chat-message-other");

        Label messageLabel = new Label(msg.getContent());
        messageLabel.setWrapText(true);
        messageLabel.setStyle(isOwn ? "-fx-text-fill: white; -fx-font-size: 14px;" : "-fx-text-fill: #1e293b; -fx-font-size: 14px;");
        messageLabel.setMaxWidth(380);

        HBox footerBox = new HBox(10);
        footerBox.setAlignment(Pos.CENTER_RIGHT);

        Label timeLabel = new Label(msg.getCreatedAt() != null ? msg.getCreatedAt().format(TIME_FORMAT) : "");
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        footerBox.getChildren().add(timeLabel);

        if (isOwn) {
            Button editBtn = new Button();
            FontIcon editIcon = new FontIcon("fas-pen");
            editIcon.setIconSize(12);
            editIcon.setIconColor(Color.web("#94a3b8"));
            editBtn.setGraphic(editIcon);
            editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;");
            editBtn.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog(msg.getContent());
                dialog.setTitle("Modifier le message");
                dialog.setHeaderText(null);
                dialog.setContentText("Nouveau texte:");
                Optional<String> result = dialog.showAndWait();
                result.ifPresent(newContent -> {
                    new Thread(() -> {
                        try {
                            chatService.editMessage(msg.getIdMsg(), newContent);
                            Platform.runLater(() -> loadMessages());
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }).start();
                });
            });

            Button deleteBtn = new Button();
            FontIcon deleteIcon = new FontIcon("fas-trash-alt");
            deleteIcon.setIconSize(12);
            deleteIcon.setIconColor(Color.web("#ef4444"));
            deleteBtn.setGraphic(deleteIcon);
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;");
            deleteBtn.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Supprimer le message");
                alert.setHeaderText(null);
                alert.setContentText("Voulez-vous vraiment supprimer ce message ?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    new Thread(() -> {
                        try {
                            chatService.deleteMessage(msg.getIdMsg());
                            Platform.runLater(() -> loadMessages());
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }).start();
                }
            });
            footerBox.getChildren().addAll(editBtn, deleteBtn);
        }

        messageBubble.getChildren().addAll(messageLabel, footerBox);
        messageRow.getChildren().add(messageBubble);
        messagesContainer.getChildren().add(messageRow);
    }

    @FXML
    private void handleSendMessage() {
        String text = messageInput.getText().trim();
        if (text.isEmpty() || currentConversation == null) return;

        messageInput.clear();

        // Message temporaire pour affichage instantané
        Message tempMsg = new Message();
        tempMsg.setIdMsg(0);
        tempMsg.setConversationId(currentConversation.getIdConv());
        tempMsg.setSenderId(currentUser.getIdUtilisateur());
        tempMsg.setReceiverId(currentContactId);
        tempMsg.setContent(text);
        tempMsg.setRead(false);
        tempMsg.setCreatedAt(LocalDateTime.now());
        addMessageToUI(tempMsg, true);

        new Thread(() -> {
            try {
                Message msg = chatService.sendMessage(currentConversation.getIdConv(), currentUser.getIdUtilisateur(), currentContactId, text);
                if (msg != null) {
                    Platform.runLater(() -> loadConversations());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Erreur envoi message: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleOpenEmoji() {
        EmojiPicker.show(messageInput.getScene().getWindow(),
                messageInput.localToScreen(messageInput.getBoundsInLocal()).getMinX(),
                messageInput.localToScreen(messageInput.getBoundsInLocal()).getMinY() - 250,
                emoji -> {
                    String currentText = messageInput.getText();
                    messageInput.setText(currentText + emoji);
                    messageInput.requestFocus();
                });
    }

    @FXML
    private void handleDeleteConversation() {
        if (currentConversation != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Supprimer la conversation");
            alert.setHeaderText(null);
            alert.setContentText("Voulez-vous vraiment supprimer cette conversation ?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        chatService.deleteConversation(currentConversation.getIdConv());
                        Platform.runLater(() -> {
                            conversationItemsCache.remove(currentConversation.getIdConv());
                            currentConversation = null;
                            chatActiveHeader.setVisible(false);
                            welcomeChatBox.setVisible(true);
                            loadConversations();
                        });
                    } catch (Exception e) { e.printStackTrace(); }
                }).start();
            }
        }
    }

    private void startMessagePoller() {
        messagePoller = Executors.newSingleThreadScheduledExecutor();
        messagePoller.scheduleAtFixedRate(() -> {
            try {
                if (currentConversation != null) {
                    List<Message> messages = chatService.getConversationMessages(currentConversation.getIdConv(), currentUser.getIdUtilisateur());
                    Platform.runLater(() -> {
                        updateMessagesList(messages);
                        loadConversations();
                    });
                } else {
                    Platform.runLater(() -> loadConversations());
                }
            } catch (Exception e) {}
        }, 2, 2, TimeUnit.SECONDS);
    }

    @FXML
    private void handleNewChat() {
        newChatPanel.setVisible(true);
        newChatPanel.setManaged(true);
        chatActiveHeader.setVisible(false);
        chatActiveHeader.setManaged(false);
        welcomeChatBox.setVisible(false);
        welcomeChatBox.setManaged(false);
        messagesContainer.getChildren().clear();
        currentConversation = null;
    }

    @FXML
    private void handleBackToHome() {
        if (messagePoller != null) messagePoller.shutdown();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/UserHomeView.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 760);
            URL cssUrl = getClass().getResource("/css/user-home.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            UserHomeController userCtrl = loader.getController();
            userCtrl.setCurrentUser(currentUser);
            Stage stage = (Stage) sidebarUserName.getScene().getWindow();
            FadeTransition ft = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
            ft.setToValue(0);
            ft.setOnFinished(e -> {
                stage.setScene(scene);
                stage.setTitle("TrackPack — Espace " + currentUser.getRole().name());
                stage.setMinWidth(1024);
                stage.setMinHeight(700);
                FadeTransition ftIn = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
                ftIn.setFromValue(0);
                ftIn.setToValue(1);
                ftIn.play();
            });
            ft.play();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            if (messagesScroll != null) messagesScroll.setVvalue(1.0);
        });
    }

    private void setupSearchListener() {
        searchConversations.textProperty().addListener((obs, old, val) -> {
            String search = val.toLowerCase();
            for (Node node : conversationsContainer.getChildren()) {
                if (node instanceof HBox hbox && hbox.getChildren().size() > 1) {
                    Node infoNode = hbox.getChildren().get(1);
                    if (infoNode instanceof VBox vbox && vbox.getChildren().size() > 0) {
                        Label nameLabel = (Label) vbox.getChildren().get(0);
                        node.setVisible(nameLabel.getText().toLowerCase().contains(search));
                        node.setManaged(node.isVisible());
                    }
                }
            }
        });
    }

    private void setupNewChatSearch() {
        searchUserField.textProperty().addListener((obs, old, val) -> {
            if (val.length() >= 2) searchUsers(val);
            else searchResultsContainer.getChildren().clear();
        });
    }

    private void searchUsers(String query) {
        new Thread(() -> {
            try {
                List<Utilisateurs> users = chatService.searchUsers(query, currentUser.getIdUtilisateur());
                Platform.runLater(() -> updateSearchResults(users));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void updateSearchResults(List<Utilisateurs> users) {
        searchResultsContainer.getChildren().clear();
        users.forEach(user -> {
            HBox item = createSearchResultItem(user);
            searchResultsContainer.getChildren().add(item);
        });
    }

    private HBox createSearchResultItem(Utilisateurs user) {
        HBox item = new HBox();
        item.setAlignment(Pos.CENTER_LEFT);
        item.setSpacing(12);
        item.setPadding(new javafx.geometry.Insets(10, 16, 10, 16));
        item.setStyle("-fx-cursor: hand;");
        item.setOnMouseClicked(e -> startNewConversation(user));

        StackPane avatar = new StackPane();
        avatar.setPrefSize(44, 44);
        avatar.setStyle("-fx-background-color: " + getAvatarColor(user.getIdUtilisateur()) + "; -fx-background-radius: 22;");
        FontIcon avatarIcon = new FontIcon("fas-user");
        avatarIcon.setIconSize(18);
        avatarIcon.setIconColor(Color.WHITE);
        avatar.getChildren().add(avatarIcon);

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameLabel = new Label(user.getPrenom() + " " + user.getNom());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 14px;");
        Label roleLabel = new Label(user.getRole().name());
        roleLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        info.getChildren().addAll(nameLabel, roleLabel);

        Button startBtn = new Button("Message");
        startBtn.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 6 18; -fx-cursor: hand;");
        startBtn.setOnAction(e -> startNewConversation(user));

        item.getChildren().addAll(avatar, info, startBtn);
        return item;
    }

    private void startNewConversation(Utilisateurs contact) {
        new Thread(() -> {
            try {
                int convId = chatService.getOrCreateConversation(currentUser.getIdUtilisateur(), contact.getIdUtilisateur());
                Platform.runLater(() -> {
                    searchUserField.clear();
                    searchResultsContainer.getChildren().clear();
                    newChatPanel.setVisible(false);
                    newChatPanel.setManaged(false);
                    loadConversations();
                    Conversation newConv = new Conversation();
                    newConv.setIdConv(convId);
                    newConv.setUser1Id(currentUser.getIdUtilisateur());
                    newConv.setUser2Id(contact.getIdUtilisateur());
                    newConv.setContactName(contact.getPrenom() + " " + contact.getNom());
                    selectConversation(newConv);
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private String getAvatarColor(int index) {
        String[] colors = {"#6366f1", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#06b6d4"};
        return colors[Math.abs(index % colors.length)];
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML private void handleNewConversation() { handleNewChat(); }
    @FXML private void handleSearchMessages() { searchConversations.requestFocus(); }
    @FXML private void handleSettings() { System.out.println("Paramètres chat"); }
    @FXML private void handleMoreOptions() { System.out.println("Plus d'options"); }

    @FXML
    private void handleLogout() {
        if (messagePoller != null) messagePoller.shutdown();
        SessionManager.getInstance().deconnecter();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AuthView.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            Stage stage = (Stage) sidebarUserName.getScene().getWindow();
            stage.setTitle("TrackPack — Connexion");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.centerOnScreen();
        } catch (IOException e) { System.err.println("Erreur logout: " + e.getMessage()); }
    }
}