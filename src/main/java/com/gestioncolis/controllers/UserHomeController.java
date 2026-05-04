package com.gestioncolis.controllers;

import com.gestioncolis.enums.Role;
import com.gestioncolis.utils.PasswordUtil;
import com.gestioncolis.utils.PhotoManager;
import com.gestioncolis.utils.SessionManager;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.services.UtilisateursServices;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

import javafx.geometry.Bounds;
import javafx.scene.layout.VBox;

public class UserHomeController implements Initializable {

    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label welcomeHeroLabel;

    @FXML private Label navAccueil;
    @FXML private Label navServices;
    @FXML private Label navEquipe;
    @FXML private Label navContact;

    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox accueilSection;
    @FXML private VBox servicesSection;
    @FXML private VBox equipeSection;
    @FXML private VBox contactSection;
    @FXML private VBox footerSection;

    // Contact form fields
    @FXML private TextField contactName;
    @FXML private TextField contactEmail;
    @FXML private TextArea contactMessage;

    // Developer images
    @FXML private ImageView dev1Image;
    @FXML private ImageView dev2Image;
    @FXML private ImageView dev3Image;
    @FXML private ImageView dev4Image;
    @FXML private ImageView dev5Image;

    @FXML private ImageView userAvatarImage;
    @FXML private StackPane userAvatarPlaceholder;

    // Éléments pour le menu déroulant
    @FXML private VBox userDropdownMenu;
    @FXML private HBox userSessionContainer;

    @FXML private Button btnGestionColis;

    private Utilisateurs currentUser;
    private boolean isDropdownVisible = false;
    private EventHandler<MouseEvent> closeDropdownFilter;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (navAccueil != null) {
            navAccueil.getStyleClass().add("nav-link-active");
        }
        loadDeveloperImages();

        // Créer le filtre pour fermer le dropdown
        closeDropdownFilter = event -> {
            if (isDropdownVisible && userDropdownMenu != null && userSessionContainer != null) {
                // Utiliser sceneToLocal pour une détection fiable quelle que soit la hiérarchie de nœuds
                Point2D clickInMenu = userDropdownMenu.sceneToLocal(event.getSceneX(), event.getSceneY());
                boolean inMenu = userDropdownMenu.isVisible() &&
                        userDropdownMenu.getBoundsInLocal().contains(clickInMenu);

                Point2D clickInSession = userSessionContainer.sceneToLocal(event.getSceneX(), event.getSceneY());
                boolean inSession = userSessionContainer.getBoundsInLocal().contains(clickInSession);

                // Ne fermer que si le clic est en dehors du menu ET de la session
                // Ne pas consommer l'événement si on est dans le menu (pour que les boutons fonctionnent)
                if (!inMenu && !inSession) {
                    hideDropdown();
                }
            }
        };
    }

    private void updateDropdownPosition() {
        if (userSessionContainer != null && userDropdownMenu != null) {
            Platform.runLater(() -> {
                try {
                    // Attendre que le layout soit calculé
                    userDropdownMenu.applyCss();
                    userDropdownMenu.layout();

                    // Obtenir la position de la session à l'écran
                    Point2D screenPoint = userSessionContainer.localToScreen(0, userSessionContainer.getHeight());
                    // Convertir en coordonnées de la scène
                    Point2D scenePoint = userDropdownMenu.getScene().getRoot().screenToLocal(screenPoint);

                    // Positionner le menu directement sous la session, aligné à droite
                    double menuX = scenePoint.getX() - userDropdownMenu.getPrefWidth() + userSessionContainer.getWidth() - 10;
                    double menuY = scenePoint.getY() + 5;

                    // S'assurer que le menu reste dans l'écran
                    double maxX = userDropdownMenu.getScene().getWidth() - userDropdownMenu.getPrefWidth() - 10;
                    menuX = Math.max(10, Math.min(menuX, maxX));

                    userDropdownMenu.setTranslateX(menuX);
                    userDropdownMenu.setTranslateY(menuY);
                } catch (Exception e) {
                    System.err.println("Erreur positionnement dropdown: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }

    @FXML
    private void toggleDropdown() {
        if (userDropdownMenu == null) {
            System.err.println("userDropdownMenu est null");
            return;
        }

        if (isDropdownVisible) {
            hideDropdown();
        } else {
            // Forcer le calcul des dimensions avant positionnement
            userDropdownMenu.setVisible(true);
            userDropdownMenu.setManaged(true);
            userDropdownMenu.applyCss();
            userDropdownMenu.layout();

            // Positionner le menu
            positionDropdown();

            showDropdown();
        }
    }

    private void positionDropdown() {
        if (userSessionContainer == null || userDropdownMenu == null) return;

        try {
            // Obtenir les coordonnées de la session dans la scène
            Bounds sessionBounds = userSessionContainer.localToScene(userSessionContainer.getBoundsInLocal());
            double sessionX = sessionBounds.getMinX();
            double sessionY = sessionBounds.getMinY();
            double sessionHeight = sessionBounds.getHeight();

            // Obtenir les dimensions du menu
            double menuWidth = userDropdownMenu.getPrefWidth();
            double menuHeight = userDropdownMenu.getPrefHeight();

            // Positionner sous la session, aligné à droite
            // L'alignement à droite signifie : le bord droit du menu aligné avec le bord droit de la session
            double menuX = sessionX + userSessionContainer.getWidth() - menuWidth;
            double menuY = sessionY + sessionHeight + 5;

            // Convertir en coordonnées du StackPane racine
            Point2D menuPoint = new Point2D(menuX, menuY);
            Point2D rootPoint = userDropdownMenu.getParent().sceneToLocal(
                    userDropdownMenu.getScene().getRoot().localToScene(menuPoint)
            );

            userDropdownMenu.setTranslateX(rootPoint.getX());
            userDropdownMenu.setTranslateY(rootPoint.getY());

            System.out.println("Dropdown positionné à: x=" + rootPoint.getX() + ", y=" + rootPoint.getY());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDropdown() {
        if (userDropdownMenu == null) return;

        // S'assurer que le menu est bien positionné
        positionDropdown();

        userDropdownMenu.setVisible(true);
        userDropdownMenu.setManaged(true);
        userDropdownMenu.toFront(); // Mettre au premier plan

        // Animation d'apparition
        userDropdownMenu.setOpacity(0);
        userDropdownMenu.setScaleY(0);
        userDropdownMenu.setScaleX(0.95);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(userDropdownMenu.opacityProperty(), 0),
                        new KeyValue(userDropdownMenu.scaleYProperty(), 0),
                        new KeyValue(userDropdownMenu.scaleXProperty(), 0.95)
                ),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(userDropdownMenu.opacityProperty(), 1),
                        new KeyValue(userDropdownMenu.scaleYProperty(), 1),
                        new KeyValue(userDropdownMenu.scaleXProperty(), 1)
                )
        );
        timeline.play();

        // Ajouter un filtre pour fermer le menu en cliquant ailleurs
        Scene scene = userSessionContainer.getScene();
        if (scene != null) {
            scene.addEventFilter(MouseEvent.MOUSE_PRESSED, closeDropdownFilter);
        }

        isDropdownVisible = true;
    }

    private void hideDropdown() {
        if (userDropdownMenu == null) return;

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(userDropdownMenu.opacityProperty(), 1),
                        new KeyValue(userDropdownMenu.scaleYProperty(), 1),
                        new KeyValue(userDropdownMenu.scaleXProperty(), 1)
                ),
                new KeyFrame(Duration.millis(150),
                        new KeyValue(userDropdownMenu.opacityProperty(), 0),
                        new KeyValue(userDropdownMenu.scaleYProperty(), 0),
                        new KeyValue(userDropdownMenu.scaleXProperty(), 0.95)
                )
        );
        timeline.setOnFinished(e -> {
            userDropdownMenu.setVisible(false);
            userDropdownMenu.setManaged(false);

            // Retirer le filtre
            Scene scene = userSessionContainer.getScene();
            if (scene != null && closeDropdownFilter != null) {
                scene.removeEventFilter(MouseEvent.MOUSE_PRESSED, closeDropdownFilter);
            }
        });
        timeline.play();

        isDropdownVisible = false;
    }

    @FXML
    private void handleEditProfile() {
        System.out.println("handleEditProfile appelé");
        hideDropdown();
        openEditProfileDialog();
    }

    @FXML
    private void handleLogout() {
        System.out.println("handleLogout appelé");
        hideDropdown();
        try {
            // ✅ DÉCONNECTION DE LA SESSION
            SessionManager.getInstance().deconnecter();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AuthView.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setTitle("TrackPack — Connexion");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.setWidth(1100);
            stage.setHeight(700);
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("Erreur logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openEditProfileDialog() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initOwner(userNameLabel.getScene().getWindow());

        VBox dialogContent = createEditProfileDialog(dialogStage);

        // Animation d'entrée
        dialogContent.setScaleX(0.9);
        dialogContent.setScaleY(0.9);
        dialogContent.setOpacity(0);

        Scene scene = new Scene(dialogContent);
        scene.setFill(Color.TRANSPARENT);
        URL cssUrl = getClass().getResource("/css/user-home.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        dialogStage.setScene(scene);

        Timeline showAnim = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(dialogContent.scaleXProperty(), 0.9),
                        new KeyValue(dialogContent.scaleYProperty(), 0.9),
                        new KeyValue(dialogContent.opacityProperty(), 0)
                ),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(dialogContent.scaleXProperty(), 1),
                        new KeyValue(dialogContent.scaleYProperty(), 1),
                        new KeyValue(dialogContent.opacityProperty(), 1)
                )
        );
        showAnim.play();

        dialogStage.showAndWait();
    }

    private VBox createEditProfileDialog(Stage dialogStage) {
        VBox container = new VBox(20);
        container.setStyle("-fx-background-color: white; -fx-background-radius: 28;");
        container.setPadding(new Insets(32, 36, 36, 36));
        container.setMaxWidth(500);
        container.setEffect(new DropShadow(25, Color.rgb(0, 0, 0, 0.15)));

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(56, 56);
        iconContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #6366f1, #4f46e5); -fx-background-radius: 28;");
        FontIcon editIcon = new FontIcon(FontAwesomeSolid.USER_EDIT);
        editIcon.setIconColor(Color.WHITE);
        editIcon.setIconSize(24);
        iconContainer.getChildren().add(editIcon);

        VBox titleBox = new VBox(4);
        Label title = new Label("Modifier mon profil");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label subtitle = new Label("Mettez à jour vos informations personnelles");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button();
        FontIcon closeIcon = new FontIcon(FontAwesomeSolid.TIMES);
        closeIcon.setIconColor(Color.web("#94a3b8"));
        closeIcon.setIconSize(16);
        closeBtn.setGraphic(closeIcon);
        closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 8;");
        closeBtn.setOnAction(e -> {
            animateDialogClose(container, dialogStage);
        });

        header.getChildren().addAll(iconContainer, titleBox, spacer, closeBtn);

        // Photo de profil
        VBox photoSection = createPhotoSection();

        // Champs du formulaire
        TextField nomField = new TextField(currentUser.getNom());
        nomField.setPromptText("Nom");
        nomField.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 14 18; -fx-font-size: 14px;");

        TextField prenomField = new TextField(currentUser.getPrenom());
        prenomField.setPromptText("Prénom");
        prenomField.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 14 18; -fx-font-size: 14px;");

        TextField emailField = new TextField(currentUser.getEmail());
        emailField.setPromptText("Email");
        emailField.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 14 18; -fx-font-size: 14px;");

        TextField telField = new TextField(currentUser.getTelephone() != null ? currentUser.getTelephone() : "");
        telField.setPromptText("Téléphone");
        telField.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 14 18; -fx-font-size: 14px;");

        // Champs mot de passe
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nouveau mot de passe (optionnel)");
        newPasswordField.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 14 18; -fx-font-size: 14px;");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmer le mot de passe");
        confirmPasswordField.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 14 18; -fx-font-size: 14px;");

        // Message d'erreur
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Boutons
        HBox buttonBox = new HBox(16);
        buttonBox.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #e2e8f0; -fx-border-width: 1.5; -fx-border-radius: 30; -fx-text-fill: #64748b; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 28; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> animateDialogClose(container, dialogStage));

        Button saveBtn = new Button("Enregistrer");
        saveBtn.setStyle("-fx-background-color: linear-gradient(to right, #6366f1, #4f46e5); -fx-background-radius: 30; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 32; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(99,102,241,0.4), 10, 0, 0, 3);");
        saveBtn.setOnAction(e -> {
            if (validateAndSave(nomField, prenomField, emailField, telField,
                    newPasswordField, confirmPasswordField, errorLabel)) {
                animateDialogClose(container, dialogStage);
            }
        });

        buttonBox.getChildren().addAll(cancelBtn, saveBtn);

        container.getChildren().addAll(
                header,
                photoSection,
                nomField,
                prenomField,
                emailField,
                telField,
                newPasswordField,
                confirmPasswordField,
                errorLabel,
                buttonBox
        );

        return container;
    }

    // Variable pour stocker la référence à la photo preview
    private ImageView photoPreview;

    private VBox createPhotoSection() {
        VBox photoSection = new VBox(12);
        photoSection.setAlignment(Pos.CENTER);
        photoSection.setStyle("-fx-padding: 10 0 10 0;");

        StackPane photoContainer = new StackPane();
        photoContainer.setPrefSize(100, 100);
        photoContainer.setStyle("-fx-background-color: #eef2ff; -fx-background-radius: 50; -fx-border-color: #c7d2fe; -fx-border-width: 2; -fx-border-radius: 50;");

        photoPreview = new ImageView();
        photoPreview.setFitWidth(94);
        photoPreview.setFitHeight(94);
        Circle clip = new Circle(47, 47, 47);
        photoPreview.setClip(clip);

        // Charger la photo actuelle
        if (currentUser.getPhoto() != null && !currentUser.getPhoto().isBlank()) {
            Image img = PhotoManager.loadImage(currentUser.getPhoto());
            if (img != null && !img.isError()) {
                photoPreview.setImage(img);
            }
        }

        Label placeholderIcon = new Label("📷");
        placeholderIcon.setStyle("-fx-font-size: 32px;");
        placeholderIcon.setVisible(photoPreview.getImage() == null);

        photoContainer.getChildren().addAll(photoPreview, placeholderIcon);

        Button changePhotoBtn = new Button("Changer la photo");
        changePhotoBtn.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 30; -fx-text-fill: #4f46e5; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 10 24; -fx-cursor: hand;");
        changePhotoBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            Stage stage = (Stage) photoContainer.getScene().getWindow();
            File file = fc.showOpenDialog(stage);
            if (file != null) {
                try {
                    Image img = new Image(file.toURI().toString(), 94, 94, true, true);
                    photoPreview.setImage(img);
                    placeholderIcon.setVisible(false);
                    photoPreview.setUserData(file);
                } catch (Exception ex) {
                    System.err.println("Erreur chargement image: " + ex.getMessage());
                }
            }
        });

        photoSection.getChildren().addAll(photoContainer, changePhotoBtn);
        return photoSection;
    }

    private boolean validateAndSave(TextField nomField, TextField prenomField, TextField emailField,
                                    TextField telField, PasswordField newPasswordField,
                                    PasswordField confirmPasswordField, Label errorLabel) {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String tel = telField.getText().trim();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (nom.isEmpty() || nom.length() < 2) {
            showError(errorLabel, "Le nom doit contenir au moins 2 caractères");
            return false;
        }
        if (prenom.isEmpty() || prenom.length() < 2) {
            showError(errorLabel, "Le prénom doit contenir au moins 2 caractères");
            return false;
        }
        if (!email.matches("^[\\w.%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError(errorLabel, "Format d'email invalide");
            return false;
        }
        if (!tel.isEmpty() && !tel.matches("^[+]?[0-9\\s\\-]{8,15}$")) {
            showError(errorLabel, "Format de téléphone invalide");
            return false;
        }
        if (!newPass.isEmpty() && newPass.length() < 6) {
            showError(errorLabel, "Le mot de passe doit contenir au moins 6 caractères");
            return false;
        }
        if (!newPass.isEmpty() && !newPass.equals(confirmPass)) {
            showError(errorLabel, "Les mots de passe ne correspondent pas");
            return false;
        }

        // Mettre à jour l'utilisateur
        currentUser.setNom(nom);
        currentUser.setPrenom(prenom);
        currentUser.setEmail(email);
        currentUser.setTelephone(tel.isEmpty() ? null : tel);

        // HACHER LE NOUVEAU MOT DE PASSE S'IL A ÉTÉ MODIFIÉ
        if (!newPass.isEmpty()) {
            String hashedPassword = PasswordUtil.hashPassword(newPass);
            currentUser.setMotDePasse(hashedPassword);  // ← Mot de passe haché
        }

        // Gérer la photo
        if (photoPreview != null && photoPreview.getUserData() instanceof File) {
            try {
                String photoPath = PhotoManager.copyToProject((File) photoPreview.getUserData());
                currentUser.setPhoto(photoPath);
            } catch (IOException ex) {
                System.err.println("Erreur copie photo: " + ex.getMessage());
            }
        }

        // Sauvegarder en base
        try {
            UtilisateursServices service = new UtilisateursServices();
            service.modifier(currentUser);

            // Mettre à jour l'affichage
            String fullName = currentUser.getPrenom() + " " + currentUser.getNom();
            if (fullName.length() > 20) {
                fullName = fullName.substring(0, 18) + "...";
            }
            userNameLabel.setText(fullName);
            userRoleLabel.setText(currentUser.getRole().name());
            welcomeHeroLabel.setText("Bienvenue, " + currentUser.getPrenom() + " !");
            loadUserPhoto();

            showSuccessNotification("Profil mis à jour avec succès !");
            return true;
        } catch (SQLException ex) {
            showError(errorLabel, "Erreur lors de la sauvegarde: " + ex.getMessage());
            return false;
        }
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> {
                    label.setVisible(false);
                    label.setManaged(false);
                })
        );
        timeline.play();
    }

    private void showSuccessNotification(String message) {
        Label notification = new Label(message);
        notification.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 12 24; -fx-background-radius: 30; -fx-effect: dropshadow(gaussian, rgba(34,197,94,0.3), 15, 0, 0, 3);");

        StackPane root = (StackPane) userNameLabel.getScene().getRoot();
        notification.setTranslateY(-50);
        root.getChildren().add(notification);

        Timeline showAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(notification.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(300), new KeyValue(notification.opacityProperty(), 1))
        );

        Timeline hideAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(notification.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(300), new KeyValue(notification.opacityProperty(), 0))
        );
        hideAnim.setOnFinished(e -> root.getChildren().remove(notification));

        showAnim.play();
        Timeline autoHide = new Timeline(
                new KeyFrame(Duration.seconds(2.5), e -> hideAnim.play())
        );
        autoHide.play();
    }

    private void animateDialogClose(VBox container, Stage dialogStage) {
        Timeline closeAnim = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(container.scaleXProperty(), 1),
                        new KeyValue(container.scaleYProperty(), 1),
                        new KeyValue(container.opacityProperty(), 1)
                ),
                new KeyFrame(Duration.millis(150),
                        new KeyValue(container.scaleXProperty(), 0.95),
                        new KeyValue(container.scaleYProperty(), 0.95),
                        new KeyValue(container.opacityProperty(), 0)
                )
        );
        closeAnim.setOnFinished(e -> dialogStage.close());
        closeAnim.play();
    }

    private void loadDeveloperImages() {
        loadImageFromResources(dev1Image, "/images/devs/yassine.jpg");
        loadImageFromResources(dev2Image, "/images/devs/rayen.jpg");
        loadImageFromResources(dev3Image, "/images/devs/mahdi.jpg");
        loadImageFromResources(dev4Image, "/images/devs/yosra.jpg");
        loadImageFromResources(dev5Image, "/images/devs/maisa.jpg");
        loadDefaultImageIfNeeded();
    }

    private void loadImageFromResources(ImageView imageView, String imagePath) {
        if (imageView == null) return;
        try {
            URL imageUrl = getClass().getResource(imagePath);
            if (imageUrl != null) {
                Image image = new Image(imageUrl.toExternalForm());
                if (!image.isError()) {
                    imageView.setImage(image);
                    return;
                }
            }
            System.err.println("Image non trouvée: " + imagePath);
        } catch (Exception e) {
            System.err.println("Erreur chargement image " + imagePath + ": " + e.getMessage());
        }
        setDefaultAvatar(imageView);
    }

    private void setDefaultAvatar(ImageView imageView) {
        try {
            URL defaultUrl = getClass().getResource("/images/default-avatar.png");
            if (defaultUrl != null) {
                imageView.setImage(new Image(defaultUrl.toExternalForm()));
            } else {
                imageView.setImage(null);
            }
        } catch (Exception e) {
            imageView.setImage(null);
        }
    }

    private void loadDefaultImageIfNeeded() {
        ImageView[] images = {dev1Image, dev2Image, dev3Image, dev4Image, dev5Image};
        for (ImageView img : images) {
            if (img != null && (img.getImage() == null || img.getImage().isError())) {
                setDefaultAvatar(img);
            }
        }
    }

    public void setCurrentUser(Utilisateurs user) {
        this.currentUser = user;
        if (user != null) {
            // Nom affiché
            String fullName = user.getPrenom() + " " + user.getNom();
            if (fullName.length() > 20) fullName = fullName.substring(0, 18) + "...";
            userNameLabel.setText(fullName);

            // ✅ FIX 2a — Rôle formaté lisiblement (pas le nom brut de l'enum)
            String roleDisplay = switch (user.getRole()) {
                case ENTREPRISE -> "Entreprise";
                case LIVREUR    -> "Livreur";
                case CLIENT     -> "Client";
                case ADMIN      -> "Administrateur";
                case TECHNICIEN -> "Technicien";
            };
            userRoleLabel.setText(roleDisplay);
            userRoleLabel.getStyleClass().removeIf(c -> c.startsWith("role-"));
            userRoleLabel.getStyleClass().add("role-" + user.getRole().name().toLowerCase());

            // Message d'accueil
            welcomeHeroLabel.setText("Bienvenue, " + user.getPrenom() + " !");

            // ✅ FIX 2b — Bouton service card adapté au rôle
            if (btnGestionColis != null) {
                if (user.getRole() == Role.LIVREUR) {
                    btnGestionColis.setText("Gestion des livraisons");
                } else {
                    btnGestionColis.setText("Gestion des colis");
                }
            }

            // Photo utilisateur
            loadUserPhoto();
        }
    }

    @FXML private void scrollToAccueil() {
        updateActiveNav(navAccueil);
        scrollToNode(accueilSection);
    }

    @FXML
    private void scrollToServices() {
        updateActiveNav(navServices);
        scrollToNode(servicesSection);
    }

    @FXML
    private void scrollToEquipe() {
        updateActiveNav(navEquipe);
        scrollToNode(equipeSection);
    }

    @FXML
    private void scrollToContact() {
        updateActiveNav(navContact);
        scrollToNode(contactSection);
    }

    private void updateActiveNav(Label activeLabel) {
        String activeStyle = "nav-link-active";
        if (navAccueil != null) navAccueil.getStyleClass().remove(activeStyle);
        if (navServices != null) navServices.getStyleClass().remove(activeStyle);
        if (navEquipe != null) navEquipe.getStyleClass().remove(activeStyle);
        if (navContact != null) navContact.getStyleClass().remove(activeStyle);
        if (activeLabel != null) activeLabel.getStyleClass().add(activeStyle);
    }

    private void scrollToNode(VBox node) {
        if (mainScrollPane == null || node == null) return;
        Platform.runLater(() -> {
            double nodeY = node.localToScene(0, 0).getY();
            double scrollPaneY = mainScrollPane.localToScene(0, 0).getY();
            double contentHeight = mainScrollPane.getContent().getBoundsInLocal().getHeight();
            double viewportHeight = mainScrollPane.getViewportBounds().getHeight();
            if (contentHeight <= viewportHeight) return;
            double targetVvalue = (nodeY - scrollPaneY) / (contentHeight - viewportHeight);
            targetVvalue = Math.max(0, Math.min(1, targetVvalue));
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(500), new KeyValue(mainScrollPane.vvalueProperty(), targetVvalue))
            );
            timeline.play();
        });
    }

    @FXML
    private void handleGestionColis() {
        if (currentUser == null) return;
        switch (currentUser.getRole()) {
            case LIVREUR -> naviguerVers("/fxml/listeLivraisons.fxml");
            default      -> naviguerVers("/fxml/listeColis.fxml");   // CLIENT, ENTREPRISE
        }
    }

    @FXML private void handleSuiviColis() { 
        showNotification("Suivi des colis - Prochainement disponible");
        System.out.println("Suivi des colis");
    }
    @FXML private void handleLivraisonExpress() { 
        showNotification("Livraison express - Prochainement disponible");
        System.out.println("Livraison express"); 
    }
    @FXML private void handleSupport() { 
        showNotification("Support - Prochainement disponible");
        System.out.println("Support"); 
    }
    @FXML private void handleAPI() { 
        showNotification("API Integration - Prochainement disponible");
        System.out.println("API Integration"); 
    }
    @FXML private void handleStatistiques() { 
        showNotification("Statistiques - Prochainement disponible");
        System.out.println("Statistiques"); 
    }

    @FXML
    private void handleNouveauColis() {
        if (currentUser == null) return;
        switch (currentUser.getRole()) {
            case ENTREPRISE -> naviguerVers("/fxml/listeColis.fxml");
            case LIVREUR    -> naviguerVers("/fxml/listeLivraisons.fxml");
            default         -> naviguerVers("/fxml/listeColis.fxml");   // CLIENT
        }
    }

    @FXML private void handleEnSavoirPlus() { 
        showNotification("Découvrez nos services en scrollant vers le bas");
        System.out.println("En savoir plus"); 
    }

    @FXML
    private void handleSendMessage() {
        String name = contactName.getText();
        String email = contactEmail.getText();
        String message = contactMessage.getText();
        System.out.println("Message de " + name + " (" + email + "): " + message);
        showNotification("Merci pour votre message! Nous vous répondrons bientôt.");
    }

    private void showNotification(String message) {
        Label notification = new Label(message);
        notification.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 12 24; -fx-background-radius: 30; -fx-effect: dropshadow(gaussian, rgba(99,102,241,0.3), 15, 0, 0, 3);");

        StackPane root = (StackPane) userNameLabel.getScene().getRoot();
        notification.setTranslateY(-50);
        notification.setTranslateX(0);
        root.getChildren().add(notification);

        Timeline showAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(notification.opacityProperty(), 0), new KeyValue(notification.translateYProperty(), -50)),
                new KeyFrame(Duration.millis(300), new KeyValue(notification.opacityProperty(), 1), new KeyValue(notification.translateYProperty(), 20))
        );

        Timeline hideAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(notification.opacityProperty(), 1), new KeyValue(notification.translateYProperty(), 20)),
                new KeyFrame(Duration.millis(300), new KeyValue(notification.opacityProperty(), 0), new KeyValue(notification.translateYProperty(), -50))
        );
        hideAnim.setOnFinished(e -> root.getChildren().remove(notification));

        showAnim.play();
        Timeline autoHide = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> hideAnim.play())
        );
        autoHide.play();
    }

    @FXML
    private void handleOpenChat() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ChatView.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 760);
            URL cssUrl = getClass().getResource("/css/user-home.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

            ChatController chatCtrl = loader.getController();
            chatCtrl.setCurrentUser(currentUser);

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            FadeTransition ft = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
            ft.setToValue(0);
            ft.setOnFinished(e -> {
                stage.setScene(scene);
                stage.setTitle("TrackPack — Messenger");
                FadeTransition ftIn = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
                ftIn.setFromValue(0);
                ftIn.setToValue(1);
                ftIn.play();
            });
            ft.play();
        } catch (IOException e) {
            System.err.println("Erreur ouverture chat: " + e.getMessage());
        }
    }

    private void applyCircleClip(ImageView imageView, double radius) {
        Circle clip = new Circle(radius, radius, radius);
        imageView.setClip(clip);
    }

    private void loadUserPhoto() {
        if (currentUser == null) return;
        String photoPath = currentUser.getPhoto();
        if (photoPath != null && !photoPath.isBlank()) {
            Image img = PhotoManager.loadImage(photoPath);
            if (img != null && !img.isError()) {
                userAvatarImage.setImage(img);
                userAvatarImage.setVisible(true);
                userAvatarImage.setManaged(true);
                userAvatarPlaceholder.setVisible(false);
                userAvatarPlaceholder.setManaged(false);
                applyCircleClip(userAvatarImage, 17);
                return;
            }
        }
        userAvatarImage.setVisible(false);
        userAvatarImage.setManaged(false);
        userAvatarPlaceholder.setVisible(true);
        userAvatarPlaceholder.setManaged(true);
        String initials = "";
        if (currentUser.getPrenom() != null && !currentUser.getPrenom().isEmpty())
            initials += currentUser.getPrenom().charAt(0);
        if (currentUser.getNom() != null && !currentUser.getNom().isEmpty())
            initials += currentUser.getNom().charAt(0);
        Label initialsLabel = new Label(initials.toUpperCase());
        initialsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        userAvatarPlaceholder.getChildren().clear();
        userAvatarPlaceholder.getChildren().add(initialsLabel);
    }
    private void naviguerVers(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent view = loader.load();

            // ✅ Passer l'utilisateur courant
            Object controller = loader.getController();
            if (controller instanceof DashboardController.UserAware ua) {
                ua.setCurrentUser(currentUser);
            }

            // ✅ FIX 3 — Créer la scène avec les bonnes dimensions
            //    et ajouter le CSS colis si présent
            javafx.scene.Scene scene = new javafx.scene.Scene(view, 1280, 760);
            java.net.URL cssColisCss = getClass().getResource("/css/colis.css");
            if (cssColisCss != null) scene.getStylesheets().add(cssColisCss.toExternalForm());

            Stage stage = (Stage) userNameLabel.getScene().getWindow();

            FadeTransition ft = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
            ft.setToValue(0);
            ft.setOnFinished(e -> {
                stage.setScene(scene);
                // ✅ Garder les contraintes de taille pour éviter le décalage
                stage.setMinWidth(1024);
                stage.setMinHeight(700);
                FadeTransition ftIn = new FadeTransition(Duration.millis(300), scene.getRoot());
                ftIn.setFromValue(0);
                ftIn.setToValue(1);
                ftIn.play();
            });
            ft.play();

        } catch (IOException e) {
            System.err.println("Erreur navigation → " + fxmlPath + " : " + e.getMessage());
            e.printStackTrace();
        }
    }
}