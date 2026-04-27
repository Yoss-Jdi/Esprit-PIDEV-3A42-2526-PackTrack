package com.gestioncolis.controllers;

import com.gestioncolis.utils.PasswordUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.enums.Role;
import com.gestioncolis.services.UtilisateursServices;
import com.gestioncolis.utils.PhotoManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * Controller de UsersFormView.fxml.
 * Gère ajout et modification d'un utilisateur avec photo de profil.
 *
 * Flux photo :
 *   1. L'utilisateur choisit un fichier → prévisualisation immédiate via ImageView
 *   2. Au Save → PhotoManager.copyToProject() copie l'image dans resources/images/profiles/
 *   3. Le chemin RELATIF (ex: images/profiles/uuid.jpg) est enregistré en base
 *   4. En mode Edit, la photo existante est chargée via PhotoManager.loadImage()
 */
public class UsersFormController implements Initializable,
        DashboardController.UserAware {

    // ─── FXML ─────────────────────────────────────────────────────────────────
    @FXML private Label          formTitle;
    @FXML private Label          formSubtitle;
    @FXML private Button         btnBack;
    @FXML private Button         btnSave;

    @FXML private TextField      fPrenom;
    @FXML private TextField      fNom;
    @FXML private TextField      fEmail;
    @FXML private TextField      fTel;
    @FXML private ComboBox<Role> fRole;
    @FXML private PasswordField  fPassword;
    @FXML private PasswordField  fConfirm;

    // Photo
    @FXML private Button         photoBtn;
    @FXML private Label          photoLabel;
    @FXML private StackPane      photoPreviewCircle;
    @FXML private ImageView      photoPreviewImg;
    @FXML private Label          photoPlaceholderIcon;

    @FXML private Label passLabel;
    @FXML private Label confirmLabel;

    // Erreurs
    @FXML private Label errPrenom;
    @FXML private Label errNom;
    @FXML private Label errEmail;
    @FXML private Label errTel;
    @FXML private Label errRole;
    @FXML private Label errPass;
    @FXML private Label errConfirm;
    @FXML private Label globalError;

    // ─── State ────────────────────────────────────────────────────────────────
    private final UtilisateursServices service = new UtilisateursServices();
    private Utilisateurs currentUser;
    private Utilisateurs editTarget;

    /** Fichier source sélectionné dans le FileChooser (avant copie) */
    private File   newPhotoFile      = null;
    /** Chemin relatif existant en base (mode Edit, conservé si pas de nouvelle photo) */
    private String existingPhotoPath = null;

    private Runnable onSaveCallback;

    // ═════════════════════════════════════════════════════════════════════════
    // INITIALIZE
    // ═════════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        fRole.getItems().addAll(Role.values());
        applyCircleClip(photoPreviewImg, 28);
    }

    @Override
    public void setCurrentUser(Utilisateurs user) {
        this.currentUser = user;
    }

    /**
     * À appeler depuis UsersController avant de charger cette vue.
     * @param existing  null = mode Ajout, non-null = mode Modification
     * @param onSave    callback exécuté après une sauvegarde réussie
     */
    public void initForm(Utilisateurs existing, Runnable onSave) {
        this.editTarget     = existing;
        this.onSaveCallback = onSave;
        boolean isEdit = (existing != null);

        if (isEdit) {
            formTitle.setText("Modifier l'utilisateur");
            formSubtitle.setText("Modifiez les informations de "
                    + existing.getPrenom() + " " + existing.getNom());
            btnSave.setText("Enregistrer les modifications");
            passLabel.setText("Nouveau mot de passe (laisser vide = inchangé)");
            confirmLabel.setText("Confirmer le nouveau mot de passe");

            fPrenom.setText(existing.getPrenom());
            fNom   .setText(existing.getNom());
            fEmail .setText(existing.getEmail());
            fTel   .setText(existing.getTelephone() != null ? existing.getTelephone() : "");
            fRole  .setValue(existing.getRole());

            // IMPORTANT : Garder le mot de passe haché existant en mémoire
            // On ne modifie le mot de passe que si un nouveau est saisi

            // ── Charger la photo existante ────────────────────────────────
            existingPhotoPath = existing.getPhoto();
            if (existingPhotoPath != null && !existingPhotoPath.isBlank()) {
                Image img = PhotoManager.loadImage(existingPhotoPath);
                if (img != null) {
                    showPhotoPreview(img, existingPhotoPath);
                } else {
                    photoLabel.setText("⚠ Photo introuvable sur le disque");
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PHOTO PICKER
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void handlePhotoPick() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images",
                        "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        File file = fc.showOpenDialog(photoBtn.getScene().getWindow());
        if (file != null) {
            newPhotoFile = file;
            try {
                Image img = new Image(file.toURI().toString(), 56, 56, false, true);
                if (!img.isError()) showPhotoPreview(img, file.getName());
            } catch (Exception e) {
                System.err.println("⚠️ Prévisualisation impossible : " + e.getMessage());
                photoLabel.setText(file.getName());
            }
        }
    }

    private void showPhotoPreview(Image img, String labelText) {
        photoPreviewImg.setImage(img);
        photoPreviewImg.setVisible(true);
        photoPreviewImg.setManaged(true);
        photoPlaceholderIcon.setVisible(false);
        photoPlaceholderIcon.setManaged(false);
        String name = labelText.contains("/") || labelText.contains("\\")
                ? labelText.substring(Math.max(labelText.lastIndexOf('/'), labelText.lastIndexOf('\\')) + 1)
                : labelText;
        photoLabel.setText(name.length() > 38 ? name.substring(0, 35) + "…" : name);
        photoBtn.setText("🔄  Changer la photo");
    }

    private void applyCircleClip(ImageView iv, double radius) {
        Circle clip = new Circle(radius, radius, radius);
        iv.setClip(clip);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SAVE
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleSave() {
        clearErrors();
        boolean isEdit = (editTarget != null);
        boolean ok     = true;

        String prenom  = fPrenom.getText().trim();
        String nom     = fNom.getText().trim();
        String email   = fEmail.getText().trim();
        String tel     = fTel.getText().trim();
        String pass    = fPassword.getText();
        String confirm = fConfirm.getText();
        Role   role    = fRole.getValue();

        if (prenom.length() < 2)
        { showErr(errPrenom, fPrenom, "Au moins 2 caractères."); ok = false; }
        if (nom.length() < 2)
        { showErr(errNom, fNom, "Au moins 2 caractères."); ok = false; }
        if (!email.matches("^[\\w.%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$"))
        { showErr(errEmail, fEmail, "Adresse email invalide."); ok = false; }
        if (!tel.isEmpty() && !tel.matches("^[+]?[0-9 \\-]{8,15}$"))
        { showErr(errTel, fTel, "Numéro invalide."); ok = false; }
        if (role == null)
        { showErrLabel(errRole, "Veuillez sélectionner un rôle."); ok = false; }
        if (!isEdit) {
            if (pass.length() < 6)
            { showErr(errPass, fPassword, "Minimum 6 caractères."); ok = false; }
            else if (!pass.equals(confirm))
            { showErr(errConfirm, fConfirm, "Les mots de passe ne correspondent pas."); ok = false; }
        } else if (!pass.isEmpty()) {
            if (pass.length() < 6)
            { showErr(errPass, fPassword, "Minimum 6 caractères."); ok = false; }
            else if (!pass.equals(confirm))
            { showErr(errConfirm, fConfirm, "Les mots de passe ne correspondent pas."); ok = false; }
        }
        if (!ok) return;

        // ── Copier la photo si un nouveau fichier a été choisi ──────────────
        String finalPhotoPath = existingPhotoPath;
        if (newPhotoFile != null) {
            try {
                finalPhotoPath = PhotoManager.copyToProject(newPhotoFile);
                System.out.println("📸 Photo enregistrée : " + finalPhotoPath);
            } catch (IOException e) {
                System.err.println("⚠️ Copie photo échouée : " + e.getMessage());
            }
        }

        // ── Construire l'entité ─────────────────────────────────────────────
        Utilisateurs u = isEdit ? editTarget : new Utilisateurs();
        u.setPrenom(prenom);
        u.setNom(nom);
        u.setEmail(email);
        u.setTelephone(tel.isEmpty() ? null : tel);
        u.setRole(role);
        u.setPhoto(finalPhotoPath);

        // GESTION DU MOT DE PASSE AVEC HACHAGE
        if (!isEdit) {
            // Nouvel utilisateur - HACHER LE MOT DE PASSE
            String hashedPassword = PasswordUtil.hashPassword(pass);
            u.setMotDePasse(hashedPassword);
        } else if (!pass.isEmpty()) {
            // Modification avec nouveau mot de passe - HACHER LE NOUVEAU MOT DE PASSE
            String hashedPassword = PasswordUtil.hashPassword(pass);
            u.setMotDePasse(hashedPassword);
        }
        // Si modification sans nouveau mot de passe, on garde le hachage existant (déjà dans editTarget)

        if (!isEdit) u.setCreatedAt(LocalDateTime.now());

        // ── Persistance ────────────────────────────────────────────────────
        try {
            if (isEdit) service.modifier(u);
            else        service.ajouter(u);
            if (onSaveCallback != null) onSaveCallback.run();
            loadView("/views/UsersView.fxml");
        } catch (SQLException ex) {
            String msg = (ex.getMessage() != null && ex.getMessage().contains("Duplicate"))
                    ? "Cet email est déjà utilisé par un autre compte."
                    : "Erreur base de données : " + ex.getMessage();
            showGlobalErr(msg);
        }
    }

    @FXML
    private void handleBack() {
        loadView("/views/UsersView.fxml");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═════════════════════════════════════════════════════════════════════════
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof DashboardController.UserAware)
                ((DashboardController.UserAware) ctrl).setCurrentUser(currentUser);
            StackPane contentArea =
                    (StackPane) btnBack.getScene().getRoot().lookup("#contentArea");
            if (contentArea != null) fadeSwap(contentArea, view);
        } catch (IOException e) {
            System.err.println("❌ Navigation error: " + e.getMessage());
        }
    }

    private void fadeSwap(StackPane area, Node newNode) {
        if (area.getChildren().isEmpty()) { area.getChildren().add(newNode); return; }
        Node old = area.getChildren().get(0);
        javafx.animation.FadeTransition fo =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(150), old);
        fo.setToValue(0);
        fo.setOnFinished(e -> {
            area.getChildren().setAll(newNode);
            newNode.setOpacity(0);
            javafx.animation.FadeTransition fi =
                    new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), newNode);
            fi.setFromValue(0); fi.setToValue(1); fi.play();
        });
        fo.play();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════════
    private void showErr(Label lbl, Control field, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
        if (!field.getStyleClass().contains("fv-input-error"))
            field.getStyleClass().add("fv-input-error");
    }
    private void showErrLabel(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }
    private void showGlobalErr(String msg) {
        globalError.setText("❌  " + msg);
        globalError.setVisible(true); globalError.setManaged(true);
    }
    private void clearErrors() {
        for (Label l : Arrays.asList(errPrenom, errNom, errEmail, errTel,
                errRole, errPass, errConfirm, globalError))
        { l.setVisible(false); l.setManaged(false); }
        for (Control c : Arrays.asList(fPrenom, fNom, fEmail, fTel, fPassword, fConfirm))
            c.getStyleClass().removeAll("fv-input-error");
    }
}