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

import java.io.IOException;
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
    @FXML private Button btnParametres;

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

    // ═════════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        activeBtn = btnDashboard;
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

        // Inject user into home view if already loaded
        if (!contentArea.getChildren().isEmpty()) {
            Node node = contentArea.getChildren().get(0);
            // The controller is stored as user data
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
    @FXML private void handleNavLivraisons()   { navigate(btnLivraisons,   "Livraisons",      "Admin › Livraisons",    null); }
    @FXML private void handleNavColis()        { navigate(btnColis,        "Colis",           "Admin › Colis",         null); }
    @FXML private void handleNavReclamations() { navigate(btnReclamations, "Réclamations",    "Admin › Réclamations",  null); }
    @FXML private void handleNavEntreprises()  { navigate(btnEntreprises,  "Entreprises",     "Admin › Entreprises",   null); }
    @FXML private void handleNavParametres()   { navigate(btnParametres,   "Paramètres",      "Admin › Paramètres",    null); }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AuthView.fxml"));
            Scene scene = new Scene(loader.load(), 960, 640);
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setTitle("TrackPack — Connexion");
            stage.setScene(scene);
            stage.setResizable(false);
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
            Node view = loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof UserAware ua) ua.setCurrentUser(currentUser);
            // Store controller as user data so setCurrentUser can reach it later
            view.setUserData(ctrl instanceof UserAware ? ctrl : null);
            fadeSwap(view);
        } catch (IOException e) {
            e.printStackTrace();  // affiche la cause racine complète
            loadPlaceholder("Erreur de chargement");
        }
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
