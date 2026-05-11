package com.gestioncolis.controllers;
import com.gestioncolis.components.AIResponseAssistant;
import com.gestioncolis.utils.AlertUtils;
import com.gestioncolis.utils.PostImageStorage;
import com.gestioncolis.utils.SceneManager;
import com.gestioncolis.utils.SessionManager;
import com.gestioncolis.entities.*;
import com.gestioncolis.services.*;
import com.gestioncolis.enums.Role;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
public class AdminDashboarForumController {

    private AuthService authService;
    private ForumService forumService;
    private PostService postService;
    private CommentService commentService;
    private UserService userService;
    private SceneManager sceneManager;
    private CommentsAnalysisService commentsAnalysisService;

    private final ObservableList<Forum> forumItems = FXCollections.observableArrayList();
    private final ObservableList<Post> postItems = FXCollections.observableArrayList();
    private final ObservableList<Activity> activityItems = FXCollections.observableArrayList();

    private Forum editingForum;
    private Post editingPost;

    @FXML
    private Label currentUserLabel;
    @FXML
    private Label statsUsersValue;
    @FXML
    private Label statsForumsValue;
    @FXML
    private Label statsPostsValue;
    @FXML
    private Label statsCommentsValue;
    @FXML
    private Label statsLikesValue;

    @FXML private TextField forumSearchField;
    @FXML private TextField postSearchField;

    @FXML
    private TableView<Forum> forumTable;
    @FXML
    private TableColumn<Forum, String> forumTitleColumn;
    @FXML
    private TableColumn<Forum, String> forumDescriptionColumn;
    @FXML
    private TableColumn<Forum, Number> forumPostCountColumn;
    @FXML
    private Label forumEditorTitleLabel;
    @FXML
    private TextField forumTitleField;
    @FXML
    private TextArea forumDescriptionArea;

    @FXML
    private TableView<Post> postTable;
    @FXML
    private TableColumn<Post, String> postTitleColumn;
    @FXML
    private TableColumn<Post, String> postForumColumn;
    @FXML
    private TableColumn<Post, String> postAuthorColumn;
    @FXML
    private TableColumn<Post, Number> postLikeColumn;
    @FXML
    private TableColumn<Post, Number> postCommentCountColumn;
    @FXML
    private Label postEditorTitleLabel;
    @FXML
    private ComboBox<Forum> postForumComboBox;
    @FXML
    private TextField adminPostTitleField;
    @FXML
    private TextArea adminPostContentArea;
    @FXML
    private Button chooseAdminPostImageButton;
    @FXML
    private Button clearAdminPostImageButton;
    @FXML
    private Label adminPostImageNameLabel;
    @FXML
    private ImageView adminPostImagePreview;


    // Activity Table
    @FXML private TableView<Activity> activityTable;
    @FXML private TableColumn<Activity, String> activityTypeColumn;
    @FXML private TableColumn<Activity, String> activityContentColumn;
    @FXML private TableColumn<Activity, String> activityAuthorColumn;
    @FXML private TableColumn<Activity, String> activityDateColumn;

    private String pendingAdminPostImagePath;

    @FXML
    private VBox aiAssistantContainer;

    private AIResponseAssistant aiAssistant;

    // Constructeur par défaut (pour JavaFX FXML)
    public AdminDashboarForumController() {
        this(null, null, null, null, null, null, null);
    }

    public AdminDashboarForumController(AuthService authService, ForumService forumService,
                                    PostService postService, CommentService commentService,
                                    UserService userService, SceneManager sceneManager,
                                    CommentsAnalysisService commentsAnalysisService) {
        this.authService = authService;
        this.forumService = forumService;
        this.postService = postService;
        this.commentService = commentService;
        this.userService = userService;
        this.sceneManager = sceneManager;
        this.commentsAnalysisService = commentsAnalysisService;
    }

    // Méthode pour injecter les services après construction JavaFX
    public void setServices(AuthService authService, ForumService forumService,
                           PostService postService, CommentService commentService,
                           UserService userService, SceneManager sceneManager,
                           CommentsAnalysisService commentsAnalysisService) {
        this.authService = authService;
        this.forumService = forumService;
        this.postService = postService;
        this.commentService = commentService;
        this.userService = userService;
        this.sceneManager = sceneManager;
        this.commentsAnalysisService = commentsAnalysisService;
    }

    @FXML
    private void initialize() {
        // Si les services ne sont pas injectés via le constructeur, les obtenir du holder
        if (authService == null) {
            com.gestioncolis.utils.ForumServiceHolder holder = com.gestioncolis.utils.ForumServiceHolder.getInstance();
            
            if (!holder.isInitialized()) {
                throw new RuntimeException("Services du forum non disponibles. Vérifiez l'initialisation du holder ou de l'injection.");
            }

            // Injecter les services depuis le holder
            this.authService = holder.getAuthService();
            this.forumService = holder.getForumService();
            this.postService = holder.getPostService();
            this.commentService = holder.getCommentService();
            this.userService = holder.getUserService();
            this.sceneManager = holder.getSceneManager();
            this.commentsAnalysisService = holder.getCommentsAnalysisService();
        }

        User user = authService.requireAdmin();
        currentUserLabel.setText(user.getNom() + " \u2022 ADMIN");

        setupTables();
        setupSearchFilters();

        resetForumForm();
        resetPostForm(false);
        refreshAll();

        // Assistant IA
        aiAssistant = new AIResponseAssistant();
        aiAssistantContainer.getChildren().setAll(aiAssistant);
    }

    private void setupTables() {
        forumTable.setItems(forumItems);
        postTable.setItems(postItems);
        activityTable.setItems(activityItems);

        forumTitleColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTitle()));
        forumDescriptionColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getDescription()));
        forumPostCountColumn.setCellValueFactory(d -> new ReadOnlyLongWrapper(d.getValue().getPostCount()));

        postTitleColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTitle()));
        postForumColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getForumTitle()));
        postAuthorColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getAuthorName()));
        postCommentCountColumn.setCellValueFactory(d -> new ReadOnlyLongWrapper(d.getValue().getCommentCount()));
        postLikeColumn.setCellValueFactory(d -> new ReadOnlyLongWrapper(d.getValue().getLikeCount()));

        activityTypeColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().type));
        activityContentColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().content));
        activityAuthorColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().author));
        activityDateColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().date));

        forumTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> populateForumForm(b));
        postTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> populatePostForm(b));
    }

    private void setupSearchFilters() {
        setupSearch(forumSearchField, forumTable, forumItems, (f, q) ->
                f.getTitle().toLowerCase().contains(q) || f.getDescription().toLowerCase().contains(q));

        setupSearch(postSearchField, postTable, postItems, (p, q) ->
                p.getTitle().toLowerCase().contains(q) || p.getContent().toLowerCase().contains(q) || p.getAuthorName().toLowerCase().contains(q));
    }

    private <T> void setupSearch(TextField searchField, TableView<T> table, ObservableList<T> baseItems, java.util.function.BiPredicate<T, String> predicate) {
        javafx.collections.transformation.FilteredList<T> filteredData = new javafx.collections.transformation.FilteredList<>(baseItems, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String query = newValue.toLowerCase().trim();
                return predicate.test(item, query);
            });
        });
        table.setItems(filteredData);
    }

    @FXML
    private void handleSaveForum() {
        safeRun(() -> {
            Forum f = (editingForum != null) ? editingForum : new Forum();
            f.setTitle(forumTitleField.getText());
            f.setDescription(forumDescriptionArea.getText());
            Forum saved = forumService.saveForum(f);
            refreshAll();
            selectInTable(forumTable, forumItems, saved.getId(), Forum::getId);
        });
    }

    @FXML
    private void handleDeleteForum() {
        safeRun(() -> {
            if (editingForum != null && AlertUtils.confirm("Suppression", "Supprimer ce forum et tout son contenu ?")) {
                forumService.deleteForum(editingForum.getId());
                refreshAll();
                resetForumForm();
            }
        });
    }

    @FXML
    private void handleResetForum() {
        forumTable.getSelectionModel().clearSelection();
        resetForumForm();
    }

    @FXML
    private void handleSavePost() {
        safeRun(() -> {
            Post p = (editingPost != null) ? editingPost : new Post();
            Forum sf = postForumComboBox.getValue();
            p.setForumId(sf == null ? 0 : sf.getId());
            p.setTitle(adminPostTitleField.getText());
            p.setContent(adminPostContentArea.getText());
            p.setImagePath(pendingAdminPostImagePath);
            Post saved = postService.savePost(p);
            refreshAll();
            selectInTable(postTable, postItems, saved.getId(), Post::getId);
        });
    }

    @FXML
    private void handlePublishAiComment() {
        safeRun(() -> {
            if (editingPost == null) {
                throw new RuntimeException("Veuillez d'abord sélectionner un post.");
            }
            String response = aiAssistant.getGeneratedResponse();
            if (response == null || response.trim().isEmpty() || response.contains("Analyse en cours")) {
                throw new RuntimeException("Aucune réponse IA générée à publier.");
            }

            Comment c = new Comment();
            c.setPostId(editingPost.getId());
            c.setContent(response);

            commentService.saveComment(c);
            AlertUtils.showInfo("Succès", "Votre réponse a été publiée en tant que commentaire !");
            refreshAll();
        });
    }

    @FXML
    private void handleDeletePost() {
        safeRun(() -> {
            if (editingPost != null && AlertUtils.confirm("Suppression", "Supprimer ce post ?")) {
                postService.deletePost(editingPost.getId());
                refreshAll();
                resetPostForm(false);
            }
        });
    }

    @FXML
    private void handleResetPost() {
        postTable.getSelectionModel().clearSelection();
        resetPostForm(false);
    }


    @FXML
    private void handleViewForum() {
        sceneManager.showUserDashboard();
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().deconnecter();
        authService.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AuthView.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 1100, 700);
            javafx.stage.Stage stage = (javafx.stage.Stage) currentUserLabel.getScene().getWindow();

            stage.setTitle("TrackPack — Connexion");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setWidth(1100);
            stage.setHeight(700);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.centerOnScreen();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de retourner vers AuthView.", e);
        }
    }

    @FXML
    private void handleOpenChatbot() {
        safeRun(() -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/example/forumapp/view/admin-chatbot-view.fxml"));
                javafx.scene.Parent root = loader.load();
                AdminChatbotController controller = loader.getController();
                ChatbotService cs = new ChatbotService(postService, forumService, userService, commentService, new NotificationService(), authService);
                controller.setChatbotService(cs);
                javafx.stage.Stage s = new javafx.stage.Stage();
                s.setTitle("DB-Insight Admin Chatbot");
                s.setScene(new javafx.scene.Scene(root));
                s.show();
            } catch (Exception e) {
                AlertUtils.showError("Erreur", "Impossible d'ouvrir le Chatbot.");
            }
        });
    }

    @FXML
    private void handleChooseAdminPostImage() {
        safeRun(() -> {
            File f = chooseImageFile();
            if (f != null) setPendingAdminPostImagePath(PostImageStorage.importImage(f));
        });
    }

    @FXML
    private void handleClearAdminPostImage() {
        setPendingAdminPostImagePath(null);
    }

    @FXML
    private void handleAnalyzePostComments() {
        safeRun(() -> {
            if (editingPost == null) return;
            CommentsAnalysisDialog dialog = new CommentsAnalysisDialog(commentsAnalysisService);
            dialog.show(editingPost.getId(), editingPost.getTitle());
        });
    }

    private void refreshAll() {
        User actor = authService.getCurrentUser();
        statsUsersValue.setText(String.valueOf(userService.countAll()));
        statsForumsValue.setText(String.valueOf(forumService.countAll()));
        statsPostsValue.setText(String.valueOf(postService.countAll()));
        statsCommentsValue.setText(String.valueOf(commentService.countAll()));
        statsLikesValue.setText(String.valueOf(userService.countLikes()));

        List<Forum> forums = forumService.getForums("");
        forumItems.setAll(forums);
        postForumComboBox.setItems(FXCollections.observableArrayList(forums));

        List<Post> posts = postService.getAllPostsForAdmin(actor.getId());
        postItems.setAll(posts);

        activityItems.clear();
        posts.stream().limit(10).forEach(p -> activityItems.add(new Activity("POST", p.getTitle(), p.getAuthorName(), p.getCreatedAt().toString())));
        // Note: activity for comments can still be shown by fetching latest comments if needed, but for simplicity we show posts.
    }

    private void populateForumForm(Forum f) {
        editingForum = f;
        if (f == null) { resetForumForm(); return; }
        forumEditorTitleLabel.setText("Modifier le forum");
        forumTitleField.setText(f.getTitle());
        forumDescriptionArea.setText(f.getDescription());
    }

    private void populatePostForm(Post p) {
        editingPost = p;
        if (p == null) { resetPostForm(false); return; }
        postEditorTitleLabel.setText("Modifier le post");
        adminPostTitleField.setText(p.getTitle());
        adminPostContentArea.setText(p.getContent());
        pendingAdminPostImagePath = p.getImagePath();
        updateAdminPostImagePreview();
        postForumComboBox.getItems().stream().filter(f -> f.getId() == p.getForumId()).findFirst().ifPresent(postForumComboBox::setValue);
        if (aiAssistant != null) aiAssistant.setPostContent(p.getTitle() + "\n" + p.getContent());
    }


    private void resetForumForm() {
        editingForum = null;
        forumEditorTitleLabel.setText("Créer un forum");
        forumTitleField.clear();
        forumDescriptionArea.clear();
    }

    private void resetPostForm(boolean keepImage) {
        if (!keepImage) pendingAdminPostImagePath = null;
        editingPost = null;
        postEditorTitleLabel.setText("Créer un post");
        adminPostTitleField.clear();
        adminPostContentArea.clear();
        postForumComboBox.getSelectionModel().selectFirst();
        updateAdminPostImagePreview();
    }


    private <T> void selectInTable(TableView<T> table, ObservableList<T> items, long id, IdExtractor<T> ex) {
        items.stream().filter(i -> ex.getId(i) == id).findFirst().ifPresent(i -> table.getSelectionModel().select(i));
    }

    private interface IdExtractor<T> { long getId(T item); }

    private void safeRun(Runnable r) {
        try { r.run(); } catch (Exception e) { AlertUtils.showError("Admin", e.getMessage()); }
    }

    private String truncate(String v, int m) {
        return (v == null || v.length() <= m) ? v : v.substring(0, m - 3) + "...";
    }

    private File chooseImageFile() {
        FileChooser c = new FileChooser();
        c.setTitle("Choisir une image");
        c.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        return c.showOpenDialog(postTable.getScene().getWindow());
    }

    private void setPendingAdminPostImagePath(String path) {
        pendingAdminPostImagePath = path;
        updateAdminPostImagePreview();
    }

     private void updateAdminPostImagePreview() {
         boolean hasImg = pendingAdminPostImagePath != null && !pendingAdminPostImagePath.isBlank();
         String uri = PostImageStorage.toExternalForm(pendingAdminPostImagePath);
         adminPostImageNameLabel.setText(hasImg ? PostImageStorage.getDisplayName(pendingAdminPostImagePath) : "Aucune image");
         adminPostImagePreview.setImage(uri == null ? null : new Image(uri, true));
         adminPostImagePreview.setVisible(uri != null);
         adminPostImagePreview.setManaged(uri != null);
         clearAdminPostImageButton.setDisable(!hasImg);
     }

     /**
      * Retourner à la vue précédente (UserHomeView)
      */
     @FXML
     private void handleGoBack() {
         com.gestioncolis.utils.NavigationManager.getInstance().goBack();
     }

    public static class Activity {
        public String type, content, author, date;
        public Activity(String t, String c, String a, String d) { type=t; content=c; author=a; date=d; }
    }

}
