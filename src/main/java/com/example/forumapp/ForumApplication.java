package com.example.forumapp;

import com.example.forumapp.controllers.AdminDashboardController;
import com.example.forumapp.controllers.LoginController;
import com.example.forumapp.controllers.UserDashboardController;
import com.example.forumapp.services.AiService;
import com.example.forumapp.services.AuthService;
import com.example.forumapp.services.CommentService;
import com.example.forumapp.services.CommentsAnalysisService;
import com.example.forumapp.services.ConfigService;
import com.example.forumapp.services.ForumService;
import com.example.forumapp.services.ModerationService;
import com.example.forumapp.services.NotificationService;
import com.example.forumapp.services.PdfExportService;
import com.example.forumapp.services.PostService;
import com.example.forumapp.services.UserService;
import com.example.forumapp.utils.SceneManager;

import javafx.application.Application;
import javafx.stage.Stage;

public class ForumApplication extends Application {

    private AuthService authService;
    private ModerationService moderationService;
    private ForumService forumService;
    private PostService postService;
    private CommentService commentService;
    private UserService userService;
    private PdfExportService pdfExportService;
    private AiService aiService;
    private CommentsAnalysisService commentsAnalysisService;
    private NotificationService notificationService;
    private SceneManager sceneManager;

    @Override
    public void start(Stage stage) {
        // Initialisation du service de configuration requis par ModerationService
        ConfigService configService = new ConfigService();
        
        authService = new AuthService();
        
        // On passe configService au constructeur
        moderationService = new ModerationService(configService);
        
        notificationService = new NotificationService();
        
        forumService = new ForumService(authService);
        postService = new PostService(authService, moderationService);
        commentService = new CommentService(authService, moderationService, notificationService);
        userService = new UserService(authService, postService, commentService);
        
        pdfExportService = new PdfExportService();
        aiService = new AiService();
        commentsAnalysisService = new CommentsAnalysisService();

        sceneManager = new SceneManager(stage, this::createController);

        stage.setMinWidth(960);
        stage.setMinHeight(640);
        sceneManager.showLogin();
        stage.show();
    }

    private Object createController(Class<?> type) {
        if (type == LoginController.class) {
            return new LoginController(authService, sceneManager);
        }
        if (type == UserDashboardController.class) {
            return new UserDashboardController(authService, forumService, postService, commentService, pdfExportService, aiService, notificationService, sceneManager);
        }
        if (type == AdminDashboardController.class) {
            return new AdminDashboardController(authService, forumService, postService, commentService, userService, sceneManager, commentsAnalysisService);
        }
        throw new IllegalArgumentException("Controleur inconnu: " + type.getName());
    }
}