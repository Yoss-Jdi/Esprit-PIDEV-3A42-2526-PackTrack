package com.gestioncolis;

import com.gestioncolis.controllers.AdminDashboarForumController;
import com.gestioncolis.controllers.AuthController;
import com.gestioncolis.controllers.LoginController;
import com.gestioncolis.controllers.UserDashboardController;
import com.gestioncolis.services.AiService;
import com.gestioncolis.services.AuthService;
import com.gestioncolis.services.CommentService;
import com.gestioncolis.services.CommentsAnalysisService;
import com.gestioncolis.services.ConfigService;
import com.gestioncolis.services.ForumService;
import com.gestioncolis.services.ModerationService;
import com.gestioncolis.services.NotificationService;
import com.gestioncolis.services.PdfExportService;
import com.gestioncolis.services.PostService;
import com.gestioncolis.services.UserService;
import com.gestioncolis.utils.SceneManager;
import com.gestioncolis.utils.SessionManager;

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
        if (type == AuthController.class) {
            return new AuthController();
        }
        if (type == LoginController.class) {
            return new LoginController(authService, sceneManager);
        }
        if (type == UserDashboardController.class) {
            syncForumAuthFromMainSession();
            return new UserDashboardController(authService, forumService, postService, commentService, pdfExportService, aiService, notificationService, sceneManager);
        }
        if (type == AdminDashboarForumController.class) {
            syncForumAuthFromMainSession();
            return new AdminDashboarForumController(authService, forumService, postService, commentService, userService, sceneManager, commentsAnalysisService);
        }
        throw new IllegalArgumentException("Controleur inconnu: " + type.getName());
    }

    private void syncForumAuthFromMainSession() {
        if (SessionManager.getInstance().isConnecte()) {
            authService.syncFromMainSession();
        } else {
            authService.logout();
        }
    }
}