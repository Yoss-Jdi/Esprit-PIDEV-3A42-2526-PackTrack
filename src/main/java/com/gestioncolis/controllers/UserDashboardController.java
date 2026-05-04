package com.gestioncolis.controllers;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.gestioncolis.components.NotificationPanel;
import com.gestioncolis.components.NotificationToast;
import com.gestioncolis.entities.Comment;
import com.gestioncolis.entities.Forum;
import com.gestioncolis.entities.Notification;
import com.gestioncolis.entities.Post;
import com.gestioncolis.entities.User;
import com.gestioncolis.services.AiService;
import com.gestioncolis.services.AuthService;
import com.gestioncolis.services.CommentService;
import com.gestioncolis.services.ForumService;
import com.gestioncolis.services.GeminiChatbotService;
import com.gestioncolis.services.NotificationService;
import com.gestioncolis.services.PdfExportService;
import com.gestioncolis.services.PostService;
import com.gestioncolis.utils.AlertUtils;
import com.gestioncolis.utils.DateTimeUtils;
import com.gestioncolis.utils.PostImageStorage;
import com.gestioncolis.utils.SceneManager;

import com.gestioncolis.entities.UserRole;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class UserDashboardController {

    private final AuthService authService;
    private final ForumService forumService;
    private final PostService postService;
    private final CommentService commentService;
    private final PdfExportService pdfExportService;
    private final AiService aiService;
    private final NotificationService notificationService;
    private final SceneManager sceneManager;
    private final GeminiChatbotService chatbotService;

    private final ObservableList<Forum> forumItems = FXCollections.observableArrayList();
    private final Map<Long, Boolean> commentsExpandedState = new HashMap<>();

    private Forum selectedForum;
    private Post editingPost;
    private Comment editingComment;

    // Notification UI components
    private NotificationPanel notificationPanel;
    private NotificationToast notificationToast;
    private long lastKnownNotifCount = 0;
    private Timeline notificationPoller;

    @FXML
    private StackPane rootPane;
    @FXML
    private BorderPane mainLayout;
    @FXML
    private Label currentUserLabel;
    @FXML
    private Button adminButton;
    @FXML
    private Label bellBadge;
    @FXML
    private VBox notificationOverlayContainer;
    @FXML
    private TextField searchField;
    @FXML
    private javafx.scene.control.ComboBox<Forum> forumComboBox;
    @FXML
    private Button togglePostComposerButton;
    @FXML
    private VBox postComposerCard;
    @FXML
    private Label postFormTitleLabel;
    @FXML
    private TextField postTitleField;
    @FXML
    private TextArea postContentArea;
    @FXML
    private Button choosePostImageButton;
    @FXML
    private Button clearPostImageButton;
    @FXML
    private Label postImageNameLabel;
    @FXML
    private ImageView postImagePreview;
    @FXML
    private Button savePostButton;
    @FXML
    private Button resetPostButton;
    @FXML
    private Label postCountLabel;
    @FXML
    private javafx.scene.control.ListView<Post> postListView;
    @FXML
    private javafx.scene.control.ListView<Comment> commentListView;
    @FXML
    private TextArea commentContentArea;
    @FXML
    private Button saveCommentButton;
    @FXML
    private Button resetCommentButton;
    @FXML
    private Label selectedPostLabel;
    @FXML
    private Label forumTitleLabel;
    @FXML
    private Label forumDescriptionLabel;
    @FXML
    private Label commentFormTitleLabel;
    @FXML
    private VBox commentComposerCard;

    @FXML private VBox chatbotPanel;
    @FXML private javafx.scene.control.ScrollPane chatScrollPane;
    @FXML private VBox chatMessages;
    @FXML private TextField chatInput;

    private String pendingPostImagePath;

    public UserDashboardController(AuthService authService, ForumService forumService,
            PostService postService, CommentService commentService,
            PdfExportService pdfExportService, AiService aiService,
            NotificationService notificationService,
            SceneManager sceneManager) {
        this.authService = authService;
        this.forumService = forumService;
        this.postService = postService;
        this.commentService = commentService;
        this.pdfExportService = pdfExportService;
        this.aiService = aiService;
        this.notificationService = notificationService;
        this.sceneManager = sceneManager;
        this.chatbotService = new GeminiChatbotService(postService);
    }

    @FXML
    private void initialize() {
        User user = authService.requireUser();
        currentUserLabel.setText(user.getNom() + " \u2022 " + user.getRole());

        if (user.getRole() == UserRole.ADMIN) {
            adminButton.setVisible(true);
            adminButton.setManaged(true);
        }

        // Setup forum selector ComboBox
        forumComboBox.setItems(forumItems);
        forumComboBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            selectedForum = n;
            resetPostForm(false);
            loadAndDisplayPosts();
        });

        // Setup comment and post ListViews with cell factories
        setupPostListView();
        setupCommentListView();

        // Search field listener
        searchField.textProperty().addListener((obs, o, n) -> loadForums(true));

        setPostComposerEnabled(false);
        loadForums(true);

        // ===== NOTIFICATION SYSTEM SETUP =====
        User actor = authService.getCurrentUser();
        notificationPanel = new NotificationPanel(notificationService, actor.getId());
        notificationPanel.setOnBadgeUpdate(this::updateNotificationBadge);
        notificationPanel.setOnNotificationClick(this::handleNotificationClick);
        notificationOverlayContainer.getChildren().add(notificationPanel);

        // Setup toast overlay
        notificationToast = new NotificationToast();
        notificationToast.setOnNotificationClick(this::handleNotificationClick);
        if (rootPane != null) {
            rootPane.getChildren().add(notificationToast);
        }

        updateNotificationBadge();
        startNotificationPoller();
    }

    private void startNotificationPoller() {
        notificationPoller = new Timeline(new KeyFrame(Duration.seconds(30), e -> updateNotificationBadge()));
        notificationPoller.setCycleCount(Animation.INDEFINITE);
        notificationPoller.play();
    }

    private void updateNotificationBadge() {
        long unread = notificationService.countUnread(authService.getCurrentUser().getId());
        javafx.application.Platform.runLater(() -> {
            if (unread > 0) {
                bellBadge.setText(String.valueOf(unread));
                bellBadge.setVisible(true);
                if (unread > lastKnownNotifCount) {
                    Notification latest = notificationService.getLatestNotification(authService.getCurrentUser().getId());
                    if (latest != null && notificationToast != null) {
                        notificationToast.showToast(latest);
                    }
                }
            } else {
                bellBadge.setVisible(false);
            }
            lastKnownNotifCount = unread;
        });
    }

    @FXML
    private void toggleNotifications() {
        notificationPanel.toggle();
    }

    private void handleNotificationClick(long postId) {
        safeRun(() -> {
            Post target = postService.findById(postId, authService.getCurrentUser().getId()).orElse(null);
            if (target == null) return;

            // 1. Switch forum if needed
            if (selectedForum == null || selectedForum.getId() != target.getForumId()) {
                forumItems.stream()
                        .filter(f -> f.getId() == target.getForumId())
                        .findFirst()
                        .ifPresent(f -> forumComboBox.getSelectionModel().select(f));
            }

            // 2. Select the post in the list
            javafx.application.Platform.runLater(() -> {
                postListView.getItems().stream()
                        .filter(p -> p.getId() == target.getId())
                        .findFirst()
                        .ifPresent(p -> {
                            postListView.getSelectionModel().select(p);
                            postListView.scrollTo(p);
                        });
            });
        });
    }

    private void setupPostListView() {
        postListView.setCellFactory(param -> new ListCell<Post>() {
            @Override
            protected void updateItem(Post post, boolean empty) {
                super.updateItem(post, empty);
                if (empty || post == null) {
                    setText(null);
                    setGraphic(null);
                    setPrefHeight(0);
                } else {
                    VBox card = buildPostCard(post, authService.getCurrentUser());
                    setGraphic(card);
                    setPrefHeight(Region.USE_COMPUTED_SIZE);
                    setMaxHeight(Double.MAX_VALUE);
                }
            }
        });
    }

    private void setupCommentListView() {
        commentListView.setCellFactory(param -> new ListCell<Comment>() {
            @Override
            protected void updateItem(Comment comment, boolean empty) {
                super.updateItem(comment, empty);
                if (empty || comment == null) {
                    setText(null);
                    setGraphic(null);
                    setPrefHeight(0);
                } else {
                    VBox card = buildCommentCard(comment, authService.getCurrentUser());
                    setGraphic(card);
                    setPrefHeight(Region.USE_COMPUTED_SIZE);
                    setMaxHeight(Double.MAX_VALUE);
                }
            }
        });
    }


    @FXML
    private void handleSavePost() {
        safeRun(() -> {
            if (selectedForum == null) {
                AlertUtils.showError("Forum", "Sélectionne un forum d'abord.");
                return;
            }
            Post draft = new Post();
            if (editingPost != null) {
                draft.setId(editingPost.getId());
            }
            draft.setForumId(selectedForum.getId());
            draft.setTitle(postTitleField.getText());
            draft.setContent(postContentArea.getText());
            draft.setImagePath(pendingPostImagePath);
            Post saved = postService.savePost(draft);
            editingPost = saved;
            pendingPostImagePath = saved.getImagePath();
            resetPostForm(true);
            loadAndDisplayPosts();
        });
    }

    @FXML
    private void handleResetPost() {
        resetPostForm(false);
    }

    @FXML
    private void handleSaveComment() {
        safeRun(() -> {
            Post selectedPost = postListView.getSelectionModel().getSelectedItem();
            if (selectedPost == null) {
                AlertUtils.showError("Commentaire", "Sélectionne un post d'abord.");
                return;
            }
            if (commentContentArea.getText().trim().isEmpty()) {
                AlertUtils.showError("Commentaire", "Le commentaire ne peut pas être vide.");
                return;
            }
            Comment comment = new Comment();
            comment.setPostId(selectedPost.getId());
            comment.setContent(commentContentArea.getText());
            commentService.saveComment(comment);
            commentContentArea.clear();
            loadAndDisplayPosts();
        });
    }

    @FXML
    private void handleResetComment() {
        commentContentArea.clear();
    }

    @FXML
    private void handleGoToAdmin() {
        sceneManager.showAdminDashboard();
    }

    @FXML
    private void handleLogout() {
        // Stop notification polling
        if (notificationPoller != null) {
            notificationPoller.stop();
        }
        authService.logout();
        sceneManager.showLogin();
    }

    @FXML
    private void togglePostComposer() {
        boolean isVisible = postComposerCard.isVisible();
        postComposerCard.setVisible(!isVisible);
        postComposerCard.setManaged(!isVisible);
        if (togglePostComposerButton != null) {
            togglePostComposerButton.setText(!isVisible ? "✖ Fermer" : "➕ Nouveau Post");
        }
        if (!isVisible && selectedForum == null && !forumItems.isEmpty()) {
            forumComboBox.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void toggleChatbot() {
        boolean isVisible = chatbotPanel.isVisible();
        chatbotPanel.setVisible(!isVisible);
        chatbotPanel.setManaged(!isVisible);
        if (!isVisible && chatMessages.getChildren().isEmpty()) {
            addBotMessage("Bonjour ! Je suis le compagnon du forum (propulsé par Gemini). Je peux rechercher des posts dans la base de données ou répondre à vos questions générales !");
        }
    }

    @FXML
    private void handleSendMessage() {
        String message = chatInput.getText();
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        addUserMessage(message);
        chatInput.clear();

        Label loadingLabel = new Label("🤖 Gemini cherche...");
        loadingLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
        chatMessages.getChildren().add(loadingLabel);

        new Thread(() -> {
            String response = chatbotService.processUserMessage(message, authService.getCurrentUser().getId());
            javafx.application.Platform.runLater(() -> {
                chatMessages.getChildren().remove(loadingLabel);
                addBotMessage(response);
            });
        }).start();
    }

    private void addUserMessage(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: #d1ecf1; -fx-text-fill: #0c5460; -fx-padding: 8; -fx-background-radius: 8;");
        label.setWrapText(true);
        label.setMaxWidth(260);
        HBox hbox = new HBox(label);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        chatMessages.getChildren().add(hbox);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: #e2e3e5; -fx-text-fill: #383d41; -fx-padding: 8; -fx-background-radius: 8;");
        label.setWrapText(true);
        label.setMaxWidth(260);
        HBox hbox = new HBox(label);
        hbox.setAlignment(Pos.CENTER_LEFT);
        chatMessages.getChildren().add(hbox);
        scrollToBottom();
    }

    private void scrollToBottom() {
        chatMessages.heightProperty().addListener((observable, oldValue, newValue) -> {
            if (chatScrollPane != null) chatScrollPane.setVvalue(1.0);
        });
    }

    @FXML
    private void handleDownloadMyPosts() {
        safeRun(() -> {
            if (postListView.getItems().isEmpty()) {
                AlertUtils.showError("Export PDF", "Aucun post à télécharger.");
                return;
            }
            List<Post> posts = postService.getPostsByForum(selectedForum.getId(), "", authService.getCurrentUser().getId());
            if (!posts.isEmpty()) {
                String filePath = pdfExportService.exportForumReport(posts, "Mes Posts - " + selectedForum.getTitle());
                AlertUtils.showInfo("PDF généré", "Fichier sauvegardé :\n" + filePath);
            }
        });
    }

    @FXML
    private void handleChoosePostImage() {
        safeRun(() -> {
            File file = chooseImageFile();
            if (file == null) {
                return;
            }
            setPendingPostImagePath(PostImageStorage.importImage(file));
        });
    }

    @FXML
    private void handleClearPostImage() {
        setPendingPostImagePath(null);
    }

    // ===== FEED RENDERING =====
    private void loadAndDisplayPosts() {
        commentsExpandedState.clear();

        if (selectedForum == null) {
            setPostComposerEnabled(false);
            postCountLabel.setText("0 posts");
            postListView.setItems(FXCollections.observableArrayList());
            selectedPostLabel.setText("Sélectionnez un post");
            forumTitleLabel.setText("");
            forumDescriptionLabel.setText("");
            commentListView.setItems(FXCollections.observableArrayList());
            return;
        }

        setPostComposerEnabled(true);
        forumTitleLabel.setText(selectedForum.getTitle());
        forumDescriptionLabel.setText(selectedForum.getDescription());

        User user = authService.getCurrentUser();
        List<Post> posts = postService.getPostsByForum(selectedForum.getId(), searchField.getText(), user.getId());
        postCountLabel.setText(posts.size() + " post(s)");

        ObservableList<Post> postItems = FXCollections.observableArrayList(posts);
        postListView.setItems(postItems);

        // Set up selection listener for posts
        postListView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                selectedPostLabel.setText(n.getTitle());
                List<Comment> comments = commentService.getCommentsByPost(n.getId(), user.getId());
                ObservableList<Comment> commentItems = FXCollections.observableArrayList(comments);
                commentListView.setItems(commentItems);
            }
        });
    }

    private VBox buildPostCard(Post post, User user) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("surface-card", "post-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-padding: 12;");

        // Title
        Label title = new Label(post.getTitle());
        title.getStyleClass().add("card-title");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setWrapText(true);

        // Meta (author, date, likes)
        Label meta = new Label("Par " + post.getAuthorName() + " \u2022 "
                + DateTimeUtils.format(post.getUpdatedAt()) + " \u2022 " + post.getLikeCount() + " likes");
        meta.getStyleClass().add("meta-label");
        meta.setMaxWidth(Double.MAX_VALUE);
        meta.setWrapText(true);

        // Content body
        Label body = new Label(post.getContent());
        body.getStyleClass().add("card-body");
        body.setWrapText(true);
        body.setMaxWidth(Double.MAX_VALUE);

        // Image (if exists)
        ImageView imageView = buildPostImageView(post.getImagePath());

        // Action buttons
        HBox actions = buildPostActionBar(post, user);

        // Add components to card
        card.getChildren().addAll(title, meta);
        if (imageView != null) {
            card.getChildren().add(imageView);
        }
        card.getChildren().addAll(body, actions);

        // Comments section (collapsible/inline) - ALWAYS SHOW
        VBox commentsSection = buildCommentsSection(post, user);
        if (commentsSection != null) {
            card.getChildren().add(commentsSection);
        }

        return card;
    }

    private HBox buildPostActionBar(Post post, User user) {
        HBox actions = new HBox(8);
        actions.setStyle("-fx-padding: 4 0 0 0;");

        // Like button
        Button likeBtn = new Button(post.isLikedByCurrentUser() ? "\u2764 Retirer" : "\u2661 Liker");
        likeBtn.getStyleClass().add(post.isLikedByCurrentUser() ? "like-button-active" : "ghost-button-sm");
        likeBtn.setOnAction(e -> safeRun(() -> {
            postService.toggleLike(post.getId());
            loadAndDisplayPosts();
        }));

        // Like count badge
        Label likeBadge = new Label(post.getLikeCount() + " likes");
        likeBadge.getStyleClass().add("badge-pill");

        // Comments count
        List<Comment> comments = commentService.getCommentsByPost(post.getId(), user.getId());
        Button commentsBtn = new Button(comments.size() + " 💬");
        commentsBtn.getStyleClass().add("primary-button-sm");
        commentsBtn.setOnAction(e -> toggleComments(post.getId()));

        // AI Summary button
        Button aiBtn = new Button("✨ IA Résumé");
        aiBtn.getStyleClass().add("primary-button-sm");
        aiBtn.setOnAction(e -> {
            aiBtn.setText("⏳ IA...");
            aiBtn.setDisable(true);
            new Thread(() -> {
                String summary = aiService.summarizePost(post.getContent());
                javafx.application.Platform.runLater(() -> {
                    aiBtn.setText("✨ IA Résumé");
                    aiBtn.setDisable(false);
                    showAiSummaryDialog(post.getTitle(), summary);
                });
            }).start();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actions.getChildren().addAll(likeBtn, likeBadge, commentsBtn, aiBtn);

        // Edit/Delete buttons if user can manage
        if (postService.canManage(user, post)) {
            Button editBtn = new Button("Modifier");
            editBtn.getStyleClass().add("ghost-button-sm");
            editBtn.setOnAction(e -> populatePostForm(post));

            Button deleteBtn = new Button("Supprimer");
            deleteBtn.getStyleClass().add("danger-button-sm");
            deleteBtn.setOnAction(e -> safeRun(() -> {
                if (AlertUtils.confirm("Suppression", "Supprimer ce post ?")) {
                    postService.deletePost(post.getId());
                    loadAndDisplayPosts();
                }
            }));

            actions.getChildren().addAll(spacer, editBtn, deleteBtn);
        } else {
            actions.getChildren().add(spacer);
        }

        return actions;
    }

    private VBox buildCommentsSection(Post post, User user) {
        List<Comment> comments = commentService.getCommentsByPost(post.getId(), user.getId());

        VBox section = new VBox(8);
        section.setStyle("-fx-border-color: transparent transparent transparent #818cf8; -fx-border-width: 0 0 0 3; -fx-padding: 8 0 0 12; -fx-margin-top: 8;");
        section.getStyleClass().add("comments-section");

        // Expandable header - Show even if empty
        if (!comments.isEmpty()) {
            Button toggleBtn = new Button("▸ " + comments.size() + " commentaire(s)");
            toggleBtn.getStyleClass().add("ghost-button-sm");
            toggleBtn.setStyle("-fx-padding: 0;");

            VBox commentsContainer = new VBox(6);
            commentsContainer.setStyle("-fx-padding: 8 0 0 0;");
            commentsContainer.setVisible(false);
            commentsContainer.setManaged(false);

            // Build comment cards
            for (Comment comment : comments) {
                commentsContainer.getChildren().add(buildCommentCard(comment, user));
            }

            toggleBtn.setOnAction(e -> {
                boolean isExpanded = commentsExpandedState.getOrDefault(post.getId(), false);
                commentsExpandedState.put(post.getId(), !isExpanded);
                commentsContainer.setVisible(!isExpanded);
                commentsContainer.setManaged(!isExpanded);
                toggleBtn.setText((isExpanded ? "▸" : "▾") + " " + comments.size() + " commentaire(s)");
            });

            section.getChildren().add(toggleBtn);
            section.getChildren().add(commentsContainer);
        }

        // Comment composer - ALWAYS SHOW (allows users to comment on own posts)
        VBox commentComposer = buildCommentComposer(post, user);
        if (commentComposer != null) {
            section.getChildren().add(commentComposer);
        }

        return section;
    }

    private VBox buildCommentCard(Comment comment, User user) {
        VBox card = new VBox(4);
        card.getStyleClass().addAll("surface-card", "comment-card");
        card.setStyle("-fx-padding: 8; -fx-border-width: 0;");
        card.setMaxWidth(Double.MAX_VALUE);

        // Meta
        Label meta = new Label("Par " + comment.getAuthorName() + " \u2022 "
                + DateTimeUtils.format(comment.getUpdatedAt()));
        meta.getStyleClass().add("meta-label");
        meta.setStyle("-fx-font-size: 10;");

        // Body
        Label body = new Label(comment.getContent());
        body.getStyleClass().add("card-body");
        body.setWrapText(true);
        body.setMaxWidth(Double.MAX_VALUE);
        body.setStyle("-fx-font-size: 11;");

        // Like button
        Button likeBtn = new Button(comment.isLikedByCurrentUser() ? "❤ Retirer" : "🤍 Liker");
        likeBtn.getStyleClass().add(comment.isLikedByCurrentUser() ? "like-button-active" : "ghost-button-sm");
        likeBtn.setStyle("-fx-font-size: 10; -fx-padding: 3 8 3 8;");
        likeBtn.setOnAction(e -> safeRun(() -> {
            commentService.toggleLike(comment.getId());
            loadAndDisplayPosts();
        }));

        HBox actions = new HBox(8);
        actions.getChildren().add(likeBtn);

        if (commentService.canManage(user, comment)) {
            Button deleteBtn = new Button("Supprimer");
            deleteBtn.getStyleClass().add("danger-button-sm");
            deleteBtn.setStyle("-fx-font-size: 10; -fx-padding: 3 8 3 8;");
            deleteBtn.setOnAction(e -> safeRun(() -> {
                if (AlertUtils.confirm("Suppression", "Supprimer ce commentaire ?")) {
                    commentService.deleteComment(comment.getId());
                    loadAndDisplayPosts();
                }
            }));
            actions.getChildren().add(deleteBtn);
        }

        card.getChildren().addAll(meta, body, actions);
        return card;
    }

    private VBox buildCommentComposer(Post post, User user) {
        VBox composer = new VBox(6);
        composer.setStyle("-fx-padding: 8 0 0 0;");

        Label label = new Label("💭 Ajouter un commentaire");
        label.setStyle("-fx-font-size: 11; -fx-font-weight: bold;");

        TextArea commentArea = new TextArea();
        commentArea.setPrefRowCount(2);
        commentArea.setWrapText(true);
        commentArea.getStyleClass().add("compact-area");
        commentArea.setStyle("-fx-font-size: 11;");

        Button submitBtn = new Button("Commenter");
        submitBtn.getStyleClass().add("primary-button-sm");
        submitBtn.setStyle("-fx-font-size: 10; -fx-padding: 5 12 5 12;");
        submitBtn.setOnAction(e -> safeRun(() -> {
            if (commentArea.getText().trim().isEmpty()) {
                AlertUtils.showError("Commentaire", "Le commentaire ne peut pas être vide.");
                return;
            }
            Comment comment = new Comment();
            comment.setPostId(post.getId());
            comment.setContent(commentArea.getText());
            commentService.saveComment(comment);
            loadAndDisplayPosts();
        }));

        composer.getChildren().addAll(label, commentArea, submitBtn);
        return composer;
    }

    private void toggleComments(long postId) {
        boolean isExpanded = commentsExpandedState.getOrDefault(postId, false);
        commentsExpandedState.put(postId, !isExpanded);
        loadAndDisplayPosts();
    }

    // ===== FORM HANDLING =====
    private void populatePostForm(Post post) {
        clearPendingPostImageSelection(false);
        editingPost = post;
        postFormTitleLabel.setText("Modifier le post");
        postTitleField.setText(post.getTitle());
        postContentArea.setText(post.getContent());
        pendingPostImagePath = post.getImagePath();
        updatePostImagePreview();
        setPostComposerEnabled(true);
        
        postComposerCard.setVisible(true);
        postComposerCard.setManaged(true);
        if (togglePostComposerButton != null) {
            togglePostComposerButton.setText("✖ Fermer");
        }
    }

    private void resetPostForm(boolean keepCurrentImageFile) {
        clearPendingPostImageSelection(keepCurrentImageFile);
        editingPost = null;
        postFormTitleLabel.setText("✏️ Créer une nouvelle publication");
        postTitleField.clear();
        postContentArea.clear();
        setPostComposerEnabled(selectedForum != null);

        postComposerCard.setVisible(false);
        postComposerCard.setManaged(false);
        if (togglePostComposerButton != null) {
            togglePostComposerButton.setText("➕ Nouveau Post");
        }
    }

    private void loadForums(boolean preserveSelection) {
        long selectedId = preserveSelection && selectedForum != null ? selectedForum.getId() : -1;
        List<Forum> forums = forumService.getForums(searchField.getText());
        forumItems.setAll(forums);

        if (forums.isEmpty()) {
            selectedForum = null;
            postListView.setItems(FXCollections.observableArrayList());
            setPostComposerEnabled(false);
            return;
        }

        Forum toSelect = forums.stream().filter(f -> f.getId() == selectedId)
                .findFirst().orElse(forums.get(0));
        forumComboBox.getSelectionModel().select(toSelect);
    }

    // ===== UTILITIES =====
    private void setPostComposerEnabled(boolean enabled) {
        postComposerCard.setDisable(!enabled);
        savePostButton.setDisable(!enabled);
        resetPostButton.setDisable(!enabled);
        choosePostImageButton.setDisable(!enabled);
        updatePostImagePreview();
    }

    private void safeRun(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            AlertUtils.showError("Forum", e.getMessage());
        }
    }

    private File chooseImageFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));
        return chooser.showOpenDialog(postComposerCard.getScene().getWindow());
    }

    private void setPendingPostImagePath(String newPath) {
        String previousPath = pendingPostImagePath;
        String persistedPath = editingPost == null ? null : editingPost.getImagePath();
        if (previousPath != null && !Objects.equals(previousPath, newPath)
                && !Objects.equals(previousPath, persistedPath)) {
            PostImageStorage.deleteStoredImage(previousPath);
        }
        pendingPostImagePath = newPath;
        updatePostImagePreview();
    }

    private void clearPendingPostImageSelection(boolean keepCurrentImageFile) {
        if (!keepCurrentImageFile) {
            String persistedPath = editingPost == null ? null : editingPost.getImagePath();
            if (pendingPostImagePath != null
                    && !Objects.equals(pendingPostImagePath, persistedPath)) {
                PostImageStorage.deleteStoredImage(pendingPostImagePath);
            }
        }
        pendingPostImagePath = null;
        updatePostImagePreview();
    }

    private void updatePostImagePreview() {
        boolean hasSelection = pendingPostImagePath != null && !pendingPostImagePath.isBlank();
        String imageUri = PostImageStorage.toExternalForm(pendingPostImagePath);
        postImageNameLabel.setText(hasSelection
                ? PostImageStorage.getDisplayName(pendingPostImagePath) : "Aucune image");
        postImagePreview.setImage(imageUri == null ? null : new Image(imageUri, true));
        postImagePreview.setVisible(imageUri != null);
        postImagePreview.setManaged(imageUri != null);
        clearPostImageButton.setDisable(postComposerCard.isDisabled() || !hasSelection);
    }

    private ImageView buildPostImageView(String imagePath) {
        String imageUri = PostImageStorage.toExternalForm(imagePath);
        if (imageUri == null) {
            return null;
        }
        ImageView imageView = new ImageView(new Image(imageUri, true));
        imageView.setFitWidth(360);
        imageView.setFitHeight(220);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        return imageView;
    }

    private void showAiSummaryDialog(String title, String summary) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Résumé par IA");

        VBox container = new VBox(15);
        container.getStyleClass().add("ai-summary-dialog");
        container.setPadding(new javafx.geometry.Insets(20));
        container.setPrefWidth(450);

        Label headerLabel = new Label("✨ Résumé Magique");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4f46e5;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #6b7280;");
        titleLabel.setWrapText(true);

        TextArea summaryArea = new TextArea(summary);
        summaryArea.setEditable(false);
        summaryArea.setWrapText(true);
        summaryArea.setPrefHeight(150);
        summaryArea.getStyleClass().add("ai-summary-text-area");
        summaryArea.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 10; -fx-font-size: 14px; -fx-text-fill: #374151;");

        Button closeBtn = new Button("Génial !");
        closeBtn.getStyleClass().add("primary-button");
        closeBtn.setPrefWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());

        container.getChildren().addAll(headerLabel, titleLabel, summaryArea, closeBtn);

        Scene scene = new Scene(container);
        if (getClass().getResource("/com/example/forumapp/css/forum-theme.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/example/forumapp/css/forum-theme.css").toExternalForm());
        }

        dialog.setScene(scene);
        dialog.show();
    }
}
