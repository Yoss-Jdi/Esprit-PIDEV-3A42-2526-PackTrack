package com.example.forumapp.controllers;

import com.example.forumapp.components.AIResponseAssistant;
import com.example.forumapp.entities.*;
import com.example.forumapp.services.*;
import com.example.forumapp.utils.AlertUtils;
import com.example.forumapp.utils.PostImageStorage;
import com.example.forumapp.utils.SceneManager;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class AdminDashboardController {

    private final AuthService authService;
    private final ForumService forumService;
    private final PostService postService;
    private final CommentService commentService;
    private final UserService userService;
    private final SceneManager sceneManager;
    private final CommentsAnalysisService commentsAnalysisService;

    private final ObservableList<Forum> forumItems = FXCollections.observableArrayList();
    private final ObservableList<Post> postItems = FXCollections.observableArrayList();
    private final ObservableList<User> userItems = FXCollections.observableArrayList();
    private final ObservableList<Activity> activityItems = FXCollections.observableArrayList();

    private Forum editingForum;
    private Post editingPost;
    private User editingUser;

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
    @FXML private TextField userSearchField;

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

    // User Management
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Number> userIdColumn;
    @FXML private TableColumn<User, String> userNameColumn;
    @FXML private TableColumn<User, String> userEmailColumn;
    @FXML private TableColumn<User, String> userRoleColumn;
    @FXML private Label userEditorTitleLabel;
    @FXML private TextField userNomField;
    @FXML private TextField userEmailField;
    @FXML private ComboBox<UserRole> userRoleComboBox;
    @FXML private PasswordField userPasswordField;

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

    public AdminDashboardController(AuthService authService, ForumService forumService,
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
        User user = authService.requireAdmin();
        currentUserLabel.setText(user.getNom() + " \u2022 ADMIN");

        setupTables();
        setupSearchFilters();

        resetForumForm();
        resetPostForm(false);
        resetUserForm();
        refreshAll();

        // Assistant IA
        aiAssistant = new AIResponseAssistant();
        aiAssistantContainer.getChildren().setAll(aiAssistant);
        
        userRoleComboBox.setItems(FXCollections.observableArrayList(UserRole.values()));
    }

    private void setupTables() {
        forumTable.setItems(forumItems);
        postTable.setItems(postItems);
        userTable.setItems(userItems);
        activityTable.setItems(activityItems);

        forumTitleColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTitle()));
        forumDescriptionColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getDescription()));
        forumPostCountColumn.setCellValueFactory(d -> new ReadOnlyLongWrapper(d.getValue().getPostCount()));

        postTitleColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTitle()));
        postForumColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getForumTitle()));
        postAuthorColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getAuthorName()));
        postCommentCountColumn.setCellValueFactory(d -> new ReadOnlyLongWrapper(d.getValue().getCommentCount()));
        postLikeColumn.setCellValueFactory(d -> new ReadOnlyLongWrapper(d.getValue().getLikeCount()));

        userIdColumn.setCellValueFactory(d -> new ReadOnlyLongWrapper(d.getValue().getId()));
        userNameColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getNom()));
        userEmailColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEmail()));
        userRoleColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getRole().name()));

        activityTypeColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().type));
        activityContentColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().content));
        activityAuthorColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().author));
        activityDateColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().date));

        forumTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> populateForumForm(b));
        postTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> populatePostForm(b));
        userTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> populateUserForm(b));
    }

    private void setupSearchFilters() {
        setupSearch(forumSearchField, forumTable, forumItems, (f, q) -> 
            f.getTitle().toLowerCase().contains(q) || f.getDescription().toLowerCase().contains(q));
            
        setupSearch(postSearchField, postTable, postItems, (p, q) -> 
            p.getTitle().toLowerCase().contains(q) || p.getContent().toLowerCase().contains(q) || p.getAuthorName().toLowerCase().contains(q));
            
        setupSearch(userSearchField, userTable, userItems, (u, q) -> 
            u.getNom().toLowerCase().contains(q) || u.getEmail().toLowerCase().contains(q));
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
    private void handleSaveUser() {
        safeRun(() -> {
            if (editingUser == null) return;
            User draft = new User();
            draft.setId(editingUser.getId());
            draft.setNom(editingUser.getNom());
            draft.setEmail(editingUser.getEmail());
            draft.setRole(userRoleComboBox.getValue());
            userService.saveUser(draft, userPasswordField.getText());
            AlertUtils.showInfo("Utilisateur", "Utilisateur mis à jour.");
            refreshAll();
            resetUserForm();
        });
    }

    @FXML
    private void handleDeleteUser() {
        safeRun(() -> {
            if (editingUser != null && AlertUtils.confirm("Suppression", "Supprimer définitivement cet utilisateur ?")) {
                userService.deleteUser(editingUser.getId());
                refreshAll();
                resetUserForm();
            }
        });
    }

    @FXML
    private void handleResetUser() {
        userTable.getSelectionModel().clearSelection();
        resetUserForm();
    }

    @FXML
    private void handleViewForum() {
        sceneManager.showUserDashboard();
    }

    @FXML
    private void handleLogout() {
        authService.logout();
        sceneManager.showLogin();
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
        userItems.setAll(userService.getAllUsers());

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

    private void populateUserForm(User u) {
        editingUser = u;
        if (u == null) { resetUserForm(); return; }
        userEditorTitleLabel.setText("Modifier l'utilisateur");
        userNomField.setText(u.getNom());
        userEmailField.setText(u.getEmail());
        userRoleComboBox.setValue(u.getRole());
        userPasswordField.clear();
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

    private void resetUserForm() {
        editingUser = null;
        userEditorTitleLabel.setText("Sélectionnez un utilisateur");
        userNomField.clear();
        userEmailField.clear();
        userRoleComboBox.getSelectionModel().selectFirst();
        userPasswordField.clear();
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

    public static class Activity {
        public String type, content, author, date;
        public Activity(String t, String c, String a, String d) { type=t; content=c; author=a; date=d; }
    }
}
