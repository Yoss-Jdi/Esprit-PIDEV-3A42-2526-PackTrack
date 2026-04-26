package com.gestioncolis.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.enums.Role;
import com.gestioncolis.services.UtilisateursServices;
import com.gestioncolis.utils.PasswordResetDialog;
import com.gestioncolis.utils.PhotoManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class AuthController implements Initializable {

    // ─── Left panel ───────────────────────────────────────────────────────────
    @FXML private StackPane  leftPanel;
    @FXML private Text       panelTitle;
    @FXML private Text       panelSubtitle;
    @FXML private Text       switchHint;
    @FXML private Button     switchBtn;

    @FXML private HBox cardContainer;

    // ─── Forms ────────────────────────────────────────────────────────────────
    @FXML private ScrollPane loginPane;
    @FXML private ScrollPane signupPane;

    // ─── Login fields ─────────────────────────────────────────────────────────
    @FXML private TextField     loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private Label         loginEmailErr;
    @FXML private Label         loginPassErr;
    @FXML private Label         loginGlobalErr;

    // ─── Signup fields ────────────────────────────────────────────────────────
    @FXML private TextField      signupNom;
    @FXML private TextField      signupPrenom;
    @FXML private TextField      signupEmail;
    @FXML private TextField      signupTel;
    @FXML private ComboBox<Role> signupRole;
    @FXML private Button         photoPickerBtn;
    @FXML private Label          photoNameLabel;
    @FXML private PasswordField  signupPassword;
    @FXML private PasswordField  signupConfirm;
    @FXML private CheckBox       cguCheck;

    // ─── Photo preview ───────────────────────────────────────────────────────
    @FXML private StackPane  photoPreviewCircle;
    @FXML private ImageView  photoPreviewImg;
    @FXML private Label      photoPlaceholderIcon;

    // ─── Errors ───────────────────────────────────────────────────────────────
    @FXML private Label nomErr;
    @FXML private Label prenomErr;
    @FXML private Label emailErr;
    @FXML private Label telErr;
    @FXML private Label roleErr;
    @FXML private Label passErr;
    @FXML private Label confirmErr;
    @FXML private Label cguErr;
    @FXML private Label signupGlobalErr;

    // ─── Password strength ────────────────────────────────────────────────────
    @FXML private Rectangle s1, s2, s3, s4;

    // ─── BG Circles (Blobs) ───────────────────────────────────────────────────
    @FXML private Circle blob1, blob2, blob3, blob4, blob5;

    // ─── State ────────────────────────────────────────────────────────────────
    private boolean isLoginMode       = true;
    private File    selectedPhotoFile = null;
    private String  savedPhotoPath    = null;
    private UtilisateursServices service;

    private static final List<Role> PUBLIC_ROLES = Arrays.asList(
            Role.CLIENT, Role.ENTREPRISE, Role.LIVREUR, Role.TECHNICIEN
    );

    // ═════════════════════════════════════════════════════════════════════════
    // INITIALIZE
    // ═════════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            service = new UtilisateursServices();
        } catch (Exception e) {
            System.err.println("⚠️ Service init error: " + e.getMessage());
        }

        signupRole.getItems().addAll(PUBLIC_ROLES);
        signupPassword.textProperty().addListener((obs, old, val) -> updateStrength(val));

        signupNom.focusedProperty()   .addListener((o, was, now) -> { if (!now) validateNom(); });
        signupPrenom.focusedProperty().addListener((o, was, now) -> { if (!now) validatePrenom(); });
        signupEmail.focusedProperty() .addListener((o, was, now) -> { if (!now) validateEmail(); });
        signupTel.focusedProperty()   .addListener((o, was, now) -> { if (!now) validateTel(); });
        loginEmail.focusedProperty()  .addListener((o, was, now) -> { if (!now) validateLoginEmail(); });

        applyCircleClip(photoPreviewImg, 32);

        playStartupAnimation();
        startBlobAnimations();
        animateBlobs();

        Hyperlink forgotLink = (Hyperlink) loginPane.lookup(".forgot-link");
        if (forgotLink != null) {
            forgotLink.setOnAction(e -> handleForgotPassword());
        }
    }

    private void applyCircleClip(ImageView iv, double radius) {
        Circle clip = new Circle(radius, radius, radius);
        iv.setClip(clip);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PHOTO FILE PICKER
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void handlePhotoPick() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif","*.webp"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        Stage stage = (Stage) photoPickerBtn.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedPhotoFile = file;
            savedPhotoPath    = null;

            try {
                Image img = new Image(file.toURI().toString(), 64, 64, false, true);
                photoPreviewImg.setImage(img);
                photoPreviewImg.setVisible(true);
                photoPreviewImg.setManaged(true);
                photoPlaceholderIcon.setVisible(false);
                photoPlaceholderIcon.setManaged(false);

                // Animation d'apparition
                ScaleTransition st = new ScaleTransition(Duration.millis(200), photoPreviewImg);
                st.setFromX(0.5);
                st.setFromY(0.5);
                st.setToX(1);
                st.setToY(1);
                st.play();
            } catch (Exception e) {
                System.err.println("⚠️ Prévisualisation impossible : " + e.getMessage());
            }

            String name = file.getName();
            photoNameLabel.setText(name.length() > 42 ? name.substring(0, 42) + "…" : name);
            photoPickerBtn.setText("✅  " + (name.length() > 30 ? name.substring(0, 30) + "…" : name));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SWITCH LOGIN ↔ SIGNUP avec animations fluides améliorées
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleSwitch() {
        if (isLoginMode) {
            // Transition vers Signup
            animateExit(loginPane, -480, () -> {
                animateEnter(signupPane, 480, true);
                updateLeftPanelWithAnimation("Créer un compte", "Vous avez déjà un compte ?",
                        "Se connecter", "Créez votre profil et\ncommencez l'aventure TrackPack.");
            });
            isLoginMode = false;
        } else {
            // Transition vers Login
            animateExit(signupPane, 480, () -> {
                animateEnter(loginPane, -480, false);
                updateLeftPanelWithAnimation("Bienvenue !", "Pas encore de compte ?",
                        "S'inscrire", "Connectez-vous pour accéder\nà votre espace TrackPack.");
            });
            isLoginMode = true;
        }
    }

    private void animateExit(ScrollPane pane, double toX, Runnable onFinished) {
        // Animation de sortie élégante
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), pane);
        tt.setToX(toX);
        tt.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition ft = new FadeTransition(Duration.millis(300), pane);
        ft.setToValue(0);

        ScaleTransition st = new ScaleTransition(Duration.millis(300), pane);
        st.setToX(0.95);
        st.setToY(0.95);

        ParallelTransition pt = new ParallelTransition(tt, ft, st);
        pt.setOnFinished(e -> {
            pane.setVisible(false);
            pane.setTranslateX(0);
            pane.setScaleX(1);
            pane.setScaleY(1);
            if (onFinished != null) onFinished.run();
        });
        pt.play();
    }

    private void animateEnter(ScrollPane pane, double fromX, boolean fromRight) {
        double translateFrom = fromRight ? 480 : -480;
        pane.setTranslateX(translateFrom);
        pane.setOpacity(0);
        pane.setScaleX(0.95);
        pane.setScaleY(0.95);
        pane.setVisible(true);

        TranslateTransition tt = new TranslateTransition(Duration.millis(500), pane);
        tt.setFromX(translateFrom);
        tt.setToX(0);
        tt.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));

        FadeTransition ft = new FadeTransition(Duration.millis(400), pane);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(50));

        ScaleTransition st = new ScaleTransition(Duration.millis(400), pane);
        st.setFromX(0.95);
        st.setFromY(0.95);
        st.setToX(1);
        st.setToY(1);

        ParallelTransition pt = new ParallelTransition(tt, ft, st);
        pt.play();
    }

    private void updateLeftPanelWithAnimation(String title, String hint, String btn, String subtitle) {
        // Animation de fondu pour le texte du panneau gauche
        FadeTransition out = new FadeTransition(Duration.millis(200), leftPanel);
        out.setToValue(0);
        out.setOnFinished(e -> {
            panelTitle.setText(title);
            panelSubtitle.setText(subtitle);
            switchHint.setText(hint);
            switchBtn.setText(btn);

            TranslateTransition slide = new TranslateTransition(Duration.millis(300), leftPanel);
            slide.setFromX(-10);
            slide.setToX(0);

            FadeTransition in = new FadeTransition(Duration.millis(300), leftPanel);
            in.setFromValue(0);
            in.setToValue(1);

            ParallelTransition pt = new ParallelTransition(slide, in);
            pt.play();
        });
        out.play();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HANDLE LOGIN
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleLogin() {
        clearLoginErrors();
        boolean valid = validateLoginEmail();
        String pass = loginPassword.getText();
        if (pass.isEmpty()) { showError(loginPassErr, loginPassword, "Le mot de passe est requis."); valid = false; }
        if (!valid) return;

        // Animation de loading sur le bouton
        Button loginBtn = (Button) loginPane.lookup("#loginBtn");
        String originalText = loginBtn.getText();
        loginBtn.setText("⏳ Connexion...");
        loginBtn.setDisable(true);

        try {
            String email = loginEmail.getText().trim();
            Utilisateurs found = service.afficher().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email)
                            && u.getMotDePasse().equals(pass))
                    .findFirst().orElse(null);

            if (found == null) {
                showGlobalError(loginGlobalErr, "Email ou mot de passe incorrect.");
                shakeField(loginEmail);
                shakeField(loginPassword);
                loginBtn.setText(originalText);
                loginBtn.setDisable(false);
            } else {
                // Vérifier le rôle et rediriger vers le dashboard approprié
                if (found.getRole() == Role.ADMIN) {
                    // Animation de transition vers dashboard admin
                    animateLoginSuccess(() -> redirectToAdminDashboard(found));
                } else {
                    // Rediriger vers le dashboard utilisateur normal
                    animateLoginSuccess(() -> redirectToUserDashboard(found));
                }
            }
        } catch (SQLException ex) {
            showGlobalError(loginGlobalErr, "Erreur serveur : " + ex.getMessage());
            loginBtn.setText(originalText);
            loginBtn.setDisable(false);
        }
    }

    @FXML
    private void handleFaceLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/FaceLoginView.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);  // Même taille que AuthView
            Stage stage = (Stage) cardContainer.getScene().getWindow();

            // Appliquer les mêmes propriétés que AuthView
            stage.setTitle("TrackPack — Reconnaissance Faciale");
            stage.setScene(scene);
            stage.setResizable(true);      // Permettre le redimensionnement
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.setWidth(1100);
            stage.setHeight(700);
            stage.centerOnScreen();

        } catch (IOException e) {
            System.err.println("❌ Erreur ouverture FaceLogin : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void animateLoginSuccess(Runnable onSuccess) {
        // Animation de succès avant redirection
        FadeTransition ft = new FadeTransition(Duration.millis(300), cardContainer);
        ft.setToValue(0.5);
        ft.setOnFinished(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), cardContainer);
            st.setToX(0.95);
            st.setToY(0.95);
            st.setOnFinished(f -> onSuccess.run());
            st.play();
        });
        ft.play();
    }

    private void redirectToDashboard(Utilisateurs user) {
        try {
            FXMLLoader loader;
            if (user.getRole() == Role.ADMIN) {
                loader = new FXMLLoader(getClass().getResource("/views/DashboardLayout.fxml"));
            } else {
                loader = new FXMLLoader(getClass().getResource("/views/UserHomeView.fxml"));
            }

            Scene scene = new Scene(loader.load(), 1280, 760);

            if (user.getRole() == Role.ADMIN) {
                DashboardController dashCtrl = loader.getController();
                dashCtrl.setCurrentUser(user);
            } else {
                // Charger le CSS pour l'interface utilisateur
                String cssUrl = getClass().getResource("/css/user-home.css").toExternalForm();
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl);
                }

                UserHomeController userCtrl = loader.getController();
                userCtrl.setCurrentUser(user);
            }

            Stage stage = (Stage) loginEmail.getScene().getWindow();

            FadeTransition ft = new FadeTransition(Duration.millis(400), stage.getScene().getRoot());
            ft.setToValue(0);
            ft.setOnFinished(e -> {
                stage.setTitle("TrackPack — " + (user.getRole() == Role.ADMIN ? "Dashboard Admin" : "Espace " + user.getRole().name()));
                stage.setScene(scene);
                stage.setResizable(true);
                stage.setMinWidth(1024);
                stage.setMinHeight(700);
                stage.centerOnScreen();

                FadeTransition ftIn = new FadeTransition(Duration.millis(400), stage.getScene().getRoot());
                ftIn.setFromValue(0);
                ftIn.setToValue(1);
                ftIn.play();
            });
            ft.play();
        } catch (IOException e) {
            showGlobalError(loginGlobalErr, "Erreur chargement dashboard : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HANDLE SIGNUP
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleSignup() {
        clearSignupErrors();
        boolean valid = true;
        if (!validateNom())     valid = false;
        if (!validatePrenom())  valid = false;
        if (!validateEmail())   valid = false;
        if (!validateTel())     valid = false;
        if (!validateRole())    valid = false;
        if (!validatePass())    valid = false;
        if (!validateConfirm()) valid = false;
        if (!validateCgu())     valid = false;
        if (!valid) return;

        // Animation de loading
        Button signupBtn = (Button) signupPane.lookup("#signupBtn");
        String originalText = signupBtn.getText();
        signupBtn.setText("✨ Création en cours...");
        signupBtn.setDisable(true);

        String photoPath = null;
        if (selectedPhotoFile != null) {
            try {
                photoPath = PhotoManager.copyToProject(selectedPhotoFile);
                System.out.println("📸 Photo copiée : " + photoPath);
            } catch (IOException e) {
                System.err.println("⚠️ Copie photo échouée : " + e.getMessage());
            }
        }

        Utilisateurs u = new Utilisateurs(
                signupEmail.getText().trim(), signupPassword.getText(),
                signupNom.getText().trim(), signupPrenom.getText().trim(),
                signupTel.getText().trim().isEmpty() ? null : signupTel.getText().trim(),
                signupRole.getValue(), photoPath, LocalDateTime.now()
        );

        try {
            service.ajouter(u);
            resetPhotoPreview();

            // Animation de succès
            ScaleTransition st = new ScaleTransition(Duration.millis(200), signupBtn);
            st.setToX(1.05);
            st.setToY(1.05);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.setOnFinished(e -> {
                handleSwitch();
                loginEmail.setText(u.getEmail());
                showGlobalSuccess(loginGlobalErr, "✅ Compte créé avec succès ! Vous pouvez vous connecter.");
                signupBtn.setText(originalText);
                signupBtn.setDisable(false);
            });
            st.play();
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("Duplicate"))
                showError(emailErr, signupEmail, "Cet email est déjà utilisé.");
            else
                showGlobalError(signupGlobalErr, "Erreur serveur : " + ex.getMessage());
            signupBtn.setText(originalText);
            signupBtn.setDisable(false);
        }
    }

    @FXML
    private void handleForgotPassword() {
        Stage stage = (Stage) loginEmail.getScene().getWindow();
        PasswordResetDialog.show(stage);
    }

    private void resetPhotoPreview() {
        selectedPhotoFile = null;
        savedPhotoPath    = null;
        photoPreviewImg.setImage(null);
        photoPreviewImg.setVisible(false);
        photoPreviewImg.setManaged(false);
        photoPlaceholderIcon.setVisible(true);
        photoPlaceholderIcon.setManaged(true);
        photoPickerBtn.setText("📁  Choisir une photo");
        photoNameLabel.setText("Aucun fichier sélectionné");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // VALIDATIONS (inchangées)
    // ═════════════════════════════════════════════════════════════════════════
    private boolean validateLoginEmail() {
        String v = loginEmail.getText().trim();
        if (v.isEmpty()) { showError(loginEmailErr, loginEmail, "L'email est requis."); return false; }
        if (!v.matches("^[\\w.%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError(loginEmailErr, loginEmail, "Format d'email invalide."); return false;
        }
        clearFieldError(loginEmailErr, loginEmail); return true;
    }

    private boolean validateNom() {
        String v = signupNom.getText().trim();
        if (v.isEmpty()) { showError(nomErr, signupNom, "Le nom est requis."); return false; }
        if (v.length() < 2) { showError(nomErr, signupNom, "Minimum 2 caractères."); return false; }
        if (!v.matches("[a-zA-ZÀ-ÿ\\s\\-']+")) { showError(nomErr, signupNom, "Caractères invalides."); return false; }
        clearFieldError(nomErr, signupNom); return true;
    }

    private boolean validatePrenom() {
        String v = signupPrenom.getText().trim();
        if (v.isEmpty()) { showError(prenomErr, signupPrenom, "Le prénom est requis."); return false; }
        if (v.length() < 2) { showError(prenomErr, signupPrenom, "Minimum 2 caractères."); return false; }
        clearFieldError(prenomErr, signupPrenom); return true;
    }

    private boolean validateEmail() {
        String v = signupEmail.getText().trim();
        if (v.isEmpty()) { showError(emailErr, signupEmail, "L'email est requis."); return false; }
        if (!v.matches("^[\\w.%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError(emailErr, signupEmail, "Format d'email invalide."); return false;
        }
        clearFieldError(emailErr, signupEmail); return true;
    }

    private boolean validateTel() {
        String v = signupTel.getText().trim();
        if (v.isEmpty()) { clearFieldError(telErr, signupTel); return true; }
        if (!v.matches("^[+]?[0-9\\s\\-]{8,15}$")) {
            showError(telErr, signupTel, "Numéro de téléphone invalide."); return false;
        }
        clearFieldError(telErr, signupTel); return true;
    }

    private boolean validateRole() {
        if (signupRole.getValue() == null) { showError(roleErr, null, "Veuillez sélectionner un rôle."); return false; }
        roleErr.setVisible(false); roleErr.setManaged(false); return true;
    }

    private boolean validatePass() {
        String v = signupPassword.getText();
        if (v.isEmpty()) { showError(passErr, signupPassword, "Le mot de passe est requis."); return false; }
        if (v.length() < 6) { showError(passErr, signupPassword, "Minimum 6 caractères."); return false; }
        clearFieldError(passErr, signupPassword); return true;
    }

    private boolean validateConfirm() {
        if (!signupConfirm.getText().equals(signupPassword.getText())) {
            showError(confirmErr, signupConfirm, "Les mots de passe ne correspondent pas."); return false;
        }
        clearFieldError(confirmErr, signupConfirm); return true;
    }

    private boolean validateCgu() {
        if (!cguCheck.isSelected()) {
            cguErr.setText("Vous devez accepter les CGU."); cguErr.setVisible(true); cguErr.setManaged(true); return false;
        }
        cguErr.setVisible(false); cguErr.setManaged(false); return true;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PASSWORD STRENGTH
    // ═════════════════════════════════════════════════════════════════════════
    private void updateStrength(String pass) {
        int score = 0;
        if (pass.length() >= 6)  score++;
        if (pass.length() >= 10) score++;
        if (pass.matches(".*[A-Z].*") && pass.matches(".*[a-z].*")) score++;
        if (pass.matches(".*[0-9].*") || pass.matches(".*[^a-zA-Z0-9].*")) score++;
        Rectangle[] segs = {s1, s2, s3, s4};
        String[] on = {"seg-active-1","seg-active-2","seg-active-3","seg-active-4"};
        for (int i = 0; i < 4; i++) {
            final boolean active = i < score; final int idx = i;
            segs[i].getStyleClass().removeAll("seg-active-1","seg-active-2","seg-active-3","seg-active-4","seg-weak");
            segs[i].getStyleClass().add(active ? on[idx] : "seg-weak");
            FadeTransition ft = new FadeTransition(Duration.millis(200), segs[i]);
            ft.setToValue(active ? 1.0 : 0.25); ft.play();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // UI HELPERS
    // ═════════════════════════════════════════════════════════════════════════
    private void showError(Label lbl, Control field, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
        if (field != null) {
            field.getStyleClass().remove("valid-field");
            if (!field.getStyleClass().contains("error-field")) field.getStyleClass().add("error-field");
        }
    }

    private void clearFieldError(Label lbl, Control field) {
        lbl.setVisible(false); lbl.setManaged(false);
        if (field != null) {
            field.getStyleClass().remove("error-field");
            if (!field.getStyleClass().contains("valid-field")) field.getStyleClass().add("valid-field");
        }
    }

    private void showGlobalError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true); lbl.setManaged(true);

        // Animation d'erreur
        ScaleTransition st = new ScaleTransition(Duration.millis(300), lbl);
        st.setFromX(0.95);
        st.setFromY(0.95);
        st.setToX(1);
        st.setToY(1);
        st.play();

        FadeTransition ft = new FadeTransition(Duration.millis(300), lbl);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void showGlobalSuccess(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true); lbl.setManaged(true);
        lbl.getStyleClass().add("notif-success");

        FadeTransition ft = new FadeTransition(Duration.millis(300), lbl);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(400), lbl);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> lbl.getStyleClass().remove("notif-success"));
                fadeOut.play();
            });
        }).start();
    }

    private void clearLoginErrors() {
        loginEmailErr.setVisible(false); loginEmailErr.setManaged(false);
        loginPassErr.setVisible(false);  loginPassErr.setManaged(false);
        loginGlobalErr.setVisible(false); loginGlobalErr.setManaged(false);
        loginEmail.getStyleClass().removeAll("error-field","valid-field");
        loginPassword.getStyleClass().removeAll("error-field","valid-field");
    }

    private void clearSignupErrors() {
        for (Label l : new Label[]{nomErr,prenomErr,emailErr,telErr,roleErr,passErr,confirmErr,cguErr,signupGlobalErr})
        { l.setVisible(false); l.setManaged(false); }
        for (Control c : new Control[]{signupNom,signupPrenom,signupEmail,signupTel,signupPassword,signupConfirm})
        { c.getStyleClass().removeAll("error-field","valid-field"); }
    }

    private void shakeField(Control field) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(55), field);
        tt.setByX(8);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();

        // Animation de couleur rouge temporaire
        field.getStyleClass().add("error-field");
        new Thread(() -> {
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() ->
                    field.getStyleClass().remove("error-field"));
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ANIMATIONS DE DÉMARRAGE
    // ═════════════════════════════════════════════════════════════════════════
    private void playStartupAnimation() {
        if (cardContainer.getScene() == null) {
            cardContainer.sceneProperty().addListener((obs, old, scene) -> {
                if (scene != null) doStartupAnimation();
            });
        } else {
            doStartupAnimation();
        }
    }

    private void doStartupAnimation() {
        // Animation d'entrée du card container
        cardContainer.setOpacity(0);
        cardContainer.setTranslateY(30);

        FadeTransition ft = new FadeTransition(Duration.millis(800), cardContainer);
        ft.setFromValue(0);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(800), cardContainer);
        tt.setFromY(30);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.play();

        // Animation d'entrée des champs
        Timeline timeline = new Timeline();
        for (int i = 0; i < 8; i++) {
            KeyFrame kf = new KeyFrame(Duration.millis(50 + i * 50), e -> {});
            timeline.getKeyFrames().add(kf);
        }
        timeline.play();
    }

    private void startBlobAnimations() {
        // Animation des blobs en arrière-plan
        animateBlob(blob1, 20000, 1.2);
        animateBlob(blob2, 25000, 1.15);
        animateBlob(blob3, 18000, 1.25);
        animateBlob(blob4, 22000, 1.1);
        animateBlob(blob5, 15000, 1.3);
    }

    private void animateBlob(Circle blob, int durationMs, double scale) {
        ScaleTransition st = new ScaleTransition(Duration.millis(durationMs), blob);
        st.setFromX(1);
        st.setFromY(1);
        st.setToX(scale);
        st.setToY(scale);
        st.setCycleCount(Animation.INDEFINITE);
        st.setAutoReverse(true);
        st.play();

        TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), blob);
        tt.setFromX(0);
        tt.setFromY(0);
        tt.setToX(blob.getCenterX() * 0.05);
        tt.setToY(blob.getCenterY() * 0.03);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setAutoReverse(true);
        tt.play();
    }

    private void animateBlobs() {
        // Animation de pulsation pour les blobs
        SequentialTransition seq = new SequentialTransition();

        FadeTransition ft1 = new FadeTransition(Duration.millis(4000), blob1);
        ft1.setFromValue(0.08);
        ft1.setToValue(0.15);
        ft1.setAutoReverse(true);
        ft1.setCycleCount(Animation.INDEFINITE);

        FadeTransition ft2 = new FadeTransition(Duration.millis(5000), blob2);
        ft2.setFromValue(0.06);
        ft2.setToValue(0.12);
        ft2.setAutoReverse(true);
        ft2.setCycleCount(Animation.INDEFINITE);

        seq.getChildren().addAll(ft1, ft2);
        seq.play();
    }

    // Ajoutez cette méthode dans la classe AuthController
    private void setupResizeListener() {
        Stage stage = (Stage) cardContainer.getScene().getWindow();
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            double width = newVal.doubleValue();
            // Ajuster la position des blobs en fonction de la taille
            if (blob2 != null) {
                blob2.setCenterX(width - 100);
            }
            if (blob3 != null) {
                blob3.setCenterX(width - 80);
            }
        });

        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            double height = newVal.doubleValue();
            if (blob4 != null) {
                blob4.setCenterY(height - 80);
            }
        });
    }

    /**
     * Redirige vers le dashboard administrateur
     */
    private void redirectToAdminDashboard(Utilisateurs admin) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/DashboardLayout.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 720);

            DashboardController dashCtrl = loader.getController();
            dashCtrl.setCurrentUser(admin);

            Stage stage = (Stage) loginEmail.getScene().getWindow();

            // Animation de transition
            FadeTransition ft = new FadeTransition(Duration.millis(400), stage.getScene().getRoot());
            ft.setToValue(0);
            ft.setOnFinished(e -> {
                stage.setTitle("TrackPack — Dashboard Admin");
                stage.setScene(scene);
                stage.setResizable(true);
                stage.setMinWidth(900);
                stage.setMinHeight(600);
                stage.centerOnScreen();

                FadeTransition ftIn = new FadeTransition(Duration.millis(400), stage.getScene().getRoot());
                ftIn.setFromValue(0);
                ftIn.setToValue(1);
                ftIn.play();
            });
            ft.play();
        } catch (IOException e) {
            showGlobalError(loginGlobalErr, "Erreur chargement dashboard admin : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Redirige vers le dashboard utilisateur normal (CLIENT, ENTREPRISE, LIVREUR, TECHNICIEN)
     */
    private void redirectToUserDashboard(Utilisateurs user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/UserHomeView.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 760);

            // Ajouter le CSS
            String cssUrl = getClass().getResource("/css/user-home.css").toExternalForm();
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl);
            }

            UserHomeController userCtrl = loader.getController();
            userCtrl.setCurrentUser(user);

            Stage stage = (Stage) loginEmail.getScene().getWindow();

            FadeTransition ft = new FadeTransition(Duration.millis(400), stage.getScene().getRoot());
            ft.setToValue(0);
            ft.setOnFinished(e -> {
                stage.setTitle("TrackPack — Espace " + user.getRole().name());
                stage.setScene(scene);
                stage.setResizable(true);
                stage.setMinWidth(1024);
                stage.setMinHeight(700);
                stage.centerOnScreen();

                FadeTransition ftIn = new FadeTransition(Duration.millis(400), stage.getScene().getRoot());
                ftIn.setFromValue(0);
                ftIn.setToValue(1);
                ftIn.play();
            });
            ft.play();
        } catch (IOException e) {
            showGlobalError(loginGlobalErr, "Erreur chargement dashboard utilisateur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}