package com.gestioncolis.utils;

import com.gestioncolis.services.*;

/**
 * Singleton qui stocke les services du forum une fois créés.
 * Cela évite les complications avec les factories et permet aux contrôleurs
 * d'accéder facilement aux services partagés.
 */
public class ForumServiceHolder {
    private static final ForumServiceHolder INSTANCE = new ForumServiceHolder();

    private AuthService authService;
    private ForumService forumService;
    private PostService postService;
    private CommentService commentService;
    private PdfExportService pdfExportService;
    private AiService aiService;
    private NotificationService notificationService;
    private UserService userService;
    private ModerationService moderationService;
    private CommentsAnalysisService commentsAnalysisService;
    private SceneManager sceneManager;

    private ForumServiceHolder() {
    }

    public static ForumServiceHolder getInstance() {
        return INSTANCE;
    }

    // Setters pour initialiser tous les services en une seule fois
    public void initializeServices(
            AuthService authService,
            ForumService forumService,
            PostService postService,
            CommentService commentService,
            PdfExportService pdfExportService,
            AiService aiService,
            NotificationService notificationService,
            UserService userService,
            ModerationService moderationService,
            CommentsAnalysisService commentsAnalysisService,
            SceneManager sceneManager) {
        this.authService = authService;
        this.forumService = forumService;
        this.postService = postService;
        this.commentService = commentService;
        this.pdfExportService = pdfExportService;
        this.aiService = aiService;
        this.notificationService = notificationService;
        this.userService = userService;
        this.moderationService = moderationService;
        this.commentsAnalysisService = commentsAnalysisService;
        this.sceneManager = sceneManager;
    }

    // Getters
    public AuthService getAuthService() {
        return authService;
    }

    public ForumService getForumService() {
        return forumService;
    }

    public PostService getPostService() {
        return postService;
    }

    public CommentService getCommentService() {
        return commentService;
    }

    public PdfExportService getPdfExportService() {
        return pdfExportService;
    }

    public AiService getAiService() {
        return aiService;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public UserService getUserService() {
        return userService;
    }

    public ModerationService getModerationService() {
        return moderationService;
    }

    public CommentsAnalysisService getCommentsAnalysisService() {
        return commentsAnalysisService;
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }

    // Vérifier si les services sont initialisés
    public boolean isInitialized() {
        return authService != null;
    }

    // Reset (utile pour les tests)
    public void reset() {
        this.authService = null;
        this.forumService = null;
        this.postService = null;
        this.commentService = null;
        this.pdfExportService = null;
        this.aiService = null;
        this.notificationService = null;
        this.userService = null;
        this.moderationService = null;
        this.commentsAnalysisService = null;
        this.sceneManager = null;
    }
}

