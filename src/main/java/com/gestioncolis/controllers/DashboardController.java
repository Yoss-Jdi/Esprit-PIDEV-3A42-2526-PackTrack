package com.gestioncolis.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.utils.SessionManager;
import com.gestioncolis.services.Servicevehicule;
import com.gestioncolis.services.Servicetechnicien;
import com.gestioncolis.SceneNavigator;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // ─── Sidebar nav buttons ───────────────────────────────────────────────────
    @FXML private Button btnDashboard;
    @FXML private Button btnUtilisateurs;
    @FXML private Button btnLivraisons;
    @FXML private Button btnColis;
    @FXML private Button btnReclamations;
    @FXML private Button btnEntreprises;
    @FXML private Button btnFactures;
    @FXML private Button btnAjoutFacture;
    @FXML private Button btnRecompenses;
    @FXML private Button btnAjoutRecompense;
    @FXML private Button btnStatsFactures;
    @FXML private Button btnStatsRecompenses;
    @FXML private Button btnParametres;

    // ─── Flotte ───────────────────────────────────────────────────────────────
    @FXML private Button btnVehicules;
    @FXML private Button btnTechniciens;

    // ─── Sidebar user info ─────────────────────────────────────────────────────
    @FXML private Label  sidebarUserName;
    @FXML private Label  avatarInitials;
    @FXML private Label  topbarUserInfo;

    // ─── Topbar ───────────────────────────────────────────────────────────────
    @FXML private Label    topbarTitle;
    @FXML private Text     topbarBreadcrumb;

    // ─── Content area ─────────────────────────────────────────────────────────
    @FXML private StackPane contentArea;

    // ─── State ────────────────────────────────────────────────────────────────
    private Utilisateurs currentUser;
    private Button       activeBtn;

    // ─── Services pour la flotte ──────────────────────────────────────────────
    private Servicevehicule servicevehicule;
    private Servicetechnicien servicetechnicien;
    private SceneNavigator sceneNavigator;

    // ═════════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        activeBtn = btnDashboard;

        // Initialiser les services pour la flotte
        try {
            servicevehicule = new Servicevehicule();
            servicetechnicien = new Servicetechnicien(servicevehicule);
        } catch (Exception e) {
            System.err.println("Erreur d'initialisation des services flotte: " + e.getMessage());
        }

        loadDefaultContent();
    }

    /**
     * Appelé par AuthController juste après l'ouverture du dashboard.
     */
    public void setCurrentUser(Utilisateurs user) {
        this.currentUser = user;
        String fullName = user.getPrenom() + " " + user.getNom();
        sidebarUserName.setText(fullName);
        topbarUserInfo.setText(user.getEmail());

        String initials = "";
        if (user.getPrenom() != null && !user.getPrenom().isEmpty())
            initials += user.getPrenom().charAt(0);
        if (user.getNom() != null && !user.getNom().isEmpty())
            initials += user.getNom().charAt(0);
        avatarInitials.setText(initials.toUpperCase());

        if (!contentArea.getChildren().isEmpty()) {
            Node node = contentArea.getChildren().get(0);
            if (node.getUserData() instanceof UserAware ua) {
                ua.setCurrentUser(user);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // NAVIGATION HANDLERS
    // ═════════════════════════════════════════════════════════════════════════
    @FXML private void handleNavDashboard()    { navigate(btnDashboard,    "Tableau de bord", "Admin › Accueil",       "/views/DashboardHomeView.fxml"); }
    @FXML private void handleNavUtilisateurs() { navigate(btnUtilisateurs, "Utilisateurs",    "Admin › Utilisateurs",  "/views/UsersView.fxml"); }
    @FXML private void handleNavLivraisons()   { navigate(btnLivraisons,   "Livraisons",      "Admin › Livraisons",    "/fxml/listeLivraisons.fxml"); }
    @FXML private void handleNavColis()        { navigate(btnColis,        "Colis",           "Admin › Colis",         "/fxml/listeColis.fxml"); }
    @FXML private void handleNavReclamations() { navigate(btnReclamations, "Réclamations",    "Admin › Réclamations",  null); }
    @FXML private void handleNavEntreprises()  { navigate(btnEntreprises,  "Entreprises",     "Admin › Entreprises",   null); }

    // ── Flotte ────────────────────────────────────────────────────────────────
    @FXML private void handleNavVehicules() {
        navigate(btnVehicules, "Véhicules", "Admin › Flotte › Véhicules", "/com/example/rayen/display.fxml");
    }

    @FXML private void handleNavTechniciens() {
        navigate(btnTechniciens, "Techniciens", "Admin › Flotte › Techniciens", "/com/example/rayen/displaytechnicien.fxml");
    }

    // ── Chatbot (depuis la topbar) ─────────────────────────────────────────────
    @FXML private void handleOpenChatbot() {
        try {
            loadView("/com/example/rayen/chatbot.fxml");
        } catch (Exception e) {
            System.err.println("Chatbot indisponible : " + e.getMessage());
        }
    }

    // ── Factures ──────────────────────────────────────────────────────────────
    @FXML private void handleNavAjoutFacture()    { navigate(btnAjoutFacture,    "Ajouter Facture",    "Admin › Factures › Ajouter",    "/com/gestioncolis/facture-view.fxml"); }
    @FXML private void handleNavFactures()        { navigate(btnFactures,        "Liste Factures",     "Admin › Factures › Liste",      "/com/gestioncolis/facture-table-view.fxml"); }

    // ── Récompenses ───────────────────────────────────────────────────────────
    @FXML private void handleNavAjoutRecompense() { navigate(btnAjoutRecompense, "Ajouter Récompense", "Admin › Récompenses › Ajouter", "/com/gestioncolis/recompense-view.fxml"); }
    @FXML private void handleNavRecompenses()     { navigate(btnRecompenses,     "Liste Récompenses",  "Admin › Récompenses › Liste",   "/com/gestioncolis/recompense-table-view.fxml"); }

    // ── Statistiques ──────────────────────────────────────────────────────────
    @FXML private void handleNavStatsFactures()    { navigate(btnStatsFactures,    "Stats Factures",     "Admin › Stats › Factures",      "/com/gestioncolis/facture-stats.fxml"); }
    @FXML private void handleNavStatsRecompenses() { navigate(btnStatsRecompenses, "Stats Récompenses",  "Admin › Stats › Récompenses",   "/com/gestioncolis/recompense-stats.fxml"); }

    @FXML private void handleNavParametres()       { navigate(btnParametres,       "Paramètres",         "Admin › Paramètres",            null); }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.getInstance().deconnecter();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AuthView.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            Stage stage = (Stage) contentArea.getScene().getWindow();

            stage.setTitle("TrackPack — Authentification");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setWidth(1100);
            stage.setHeight(700);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.centerOnScreen();

        } catch (IOException e) {
            System.err.println("❌ Erreur logout : " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CORE NAVIGATION
    // ═════════════════════════════════════════════════════════════════════════
    private void navigate(Button btn, String title, String breadcrumb, String fxmlPath) {
        topbarTitle.setText(title);
        topbarBreadcrumb.setText(breadcrumb);

        if (activeBtn != null) activeBtn.getStyleClass().remove("nav-btn-active");
        btn.getStyleClass().add("nav-btn-active");
        activeBtn = btn;

        if (fxmlPath != null) loadView(fxmlPath);
        else                  loadPlaceholder(title);
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            // ===== MODIFICATION IMPORTANTE: Utiliser une ControllerFactory =====
            loader.setControllerFactory(this::createController);

            Node view = loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof UserAware ua) ua.setCurrentUser(currentUser);
            view.setUserData(ctrl instanceof UserAware ? ctrl : null);
            fadeSwap(view);
        } catch (IOException e) {
            e.printStackTrace();
            loadPlaceholder("Erreur de chargement: " + e.getMessage());
        }
    }

    /**
     * Factory pour créer les contrôleurs avec leurs dépendances
     */
    private Object createController(Class<?> type) {
        try {
            // Contrôleurs de la flotte (vehicules/techniciens)
            if (type == displayContoller.class) {
                return new displayContoller(servicevehicule, createDummySceneNavigator());
            }
            if (type == displaytechnicien.class) {
                return new displaytechnicien(servicetechnicien, createDummySceneNavigator());
            }
            if (type == addContoller.class) {
                return new addContoller(servicevehicule, createDummySceneNavigator());
            }
            if (type == addtechnicien.class) {
                return new addtechnicien(servicetechnicien, createDummySceneNavigator());
            }
            if (type == modifyContoller.class) {
                return new modifyContoller(servicevehicule, createDummySceneNavigator());
            }
            if (type == modifytechnicien.class) {
                return new modifytechnicien(servicetechnicien, createDummySceneNavigator());
            }
            if (type == stat.class) {
                return new stat(servicetechnicien, createDummySceneNavigator());
            }

            // Constructeur par défaut pour les autres contrôleurs
            return type.getDeclaredConstructor().newInstance();

        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Impossible d'instancier le contrôleur " + type.getName(), e);
        }
    }

    /**
     * Créer un SceneNavigator simplifié pour les vues de flotte intégrées au dashboard
     */
    private SceneNavigator createDummySceneNavigator() {
        // Retourner un SceneNavigator qui redirige vers le dashboard au lieu de changer de scène
        return new SceneNavigator(null, servicevehicule, servicetechnicien, null) {
            @Override
            public void showDisplayView(String message) {
                navigate(btnVehicules, "Véhicules", "Admin › Flotte › Véhicules", "/com/example/rayen/display.fxml");
            }

            @Override
            public void showDisplayTechnicienView(String message) {
                navigate(btnTechniciens, "Techniciens", "Admin › Flotte › Techniciens", "/com/example/rayen/displaytechnicien.fxml");
            }

            @Override
            public void showAddView() {
                loadView("/com/example/rayen/add.fxml");
            }

            @Override
            public void showAddTechnicienView() {
                loadView("/com/example/rayen/addtechnicien.fxml");
            }

            @Override
            public void showModifyView(com.gestioncolis.entities.vehicule v) {
                loadView("/com/example/rayen/modify.fxml");
            }

            @Override
            public void showModifyTechnicienView(com.gestioncolis.entities.technicien t) {
                loadView("/com/example/rayen/modifytechnicien.fxml");
            }

            @Override
            public void showStatView() {
                loadView("/com/example/rayen/stat.fxml");
            }

            @Override
            public void showChatbotWindow() {
                loadView("/com/example/rayen/chatbot.fxml");
            }
        };
    }

    private void loadDefaultContent() {
        loadView("/views/DashboardHomeView.fxml");
    }

    private void loadPlaceholder(String moduleName) {
        javafx.scene.layout.VBox ph = new javafx.scene.layout.VBox();
        ph.setAlignment(javafx.geometry.Pos.CENTER);
        ph.setSpacing(12);
        javafx.scene.text.Text icon = new javafx.scene.text.Text("🚧");
        icon.setStyle("-fx-font-size: 48px;");
        javafx.scene.text.Text msg = new javafx.scene.text.Text(
                "Module « " + moduleName + " » — Bientôt disponible");
        msg.getStyleClass().add("placeholder-text");
        ph.getChildren().addAll(icon, msg);
        fadeSwap(ph);
    }

    private void fadeSwap(Node newNode) {
        if (contentArea.getChildren().isEmpty()) {
            contentArea.getChildren().add(newNode);
            newNode.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(300), newNode);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
            return;
        }
        Node old = contentArea.getChildren().get(0);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), old);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            contentArea.getChildren().setAll(newNode);
            newNode.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(280), newNode);
            fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.play();
        });
        fadeOut.play();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MARKER INTERFACE
    // ═════════════════════════════════════════════════════════════════════════
    public interface UserAware {
        void setCurrentUser(Utilisateurs user);
    }
}