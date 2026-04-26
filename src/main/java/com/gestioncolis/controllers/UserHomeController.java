package com.gestioncolis.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.gestioncolis.entities.Utilisateurs;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

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

    private Utilisateurs currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialiser le style actif
        if (navAccueil != null) {
            navAccueil.getStyleClass().add("nav-link-active");
        }

        // Charger les images des développeurs
        loadDeveloperImages();
    }

    /**
     * Charge les images des développeurs depuis le dossier resources/images/devs/
     */
    private void loadDeveloperImages() {
        // Méthode 1: Depuis le dossier resources (recommandée)
        loadImageFromResources(dev1Image, "/images/devs/yassine.jpg");
        loadImageFromResources(dev2Image, "/images/devs/rayen.jpg");
        loadImageFromResources(dev3Image, "/images/devs/mahdi.jpg");
        loadImageFromResources(dev4Image, "/images/devs/yosra.jpg");
        loadImageFromResources(dev5Image, "/images/devs/maisa.jpg");

        // Si une image est introuvable, charger l'image par défaut
        loadDefaultImageIfNeeded();
    }

    /**
     * Charge une image depuis les ressources du projet
     */
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

        // Image par défaut en cas d'échec
        setDefaultAvatar(imageView);
    }

    /**
     * Définit l'avatar par défaut
     */
    private void setDefaultAvatar(ImageView imageView) {
        try {
            URL defaultUrl = getClass().getResource("/images/default-avatar.png");
            if (defaultUrl != null) {
                imageView.setImage(new Image(defaultUrl.toExternalForm()));
            } else {
                // Utiliser une icône FontIcon comme fallback
                imageView.setImage(null);
            }
        } catch (Exception e) {
            imageView.setImage(null);
        }
    }

    /**
     * Vérifie si toutes les images ont été chargées, sinon met l'image par défaut
     */
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
            String fullName = user.getPrenom() + " " + user.getNom();
            if (fullName.length() > 20) {
                fullName = fullName.substring(0, 18) + "...";
            }
            userNameLabel.setText(fullName);
            userRoleLabel.setText(user.getRole().name());
            welcomeHeroLabel.setText("Bienvenue, " + user.getPrenom() + " !");
            userRoleLabel.getStyleClass().add("role-" + user.getRole().name().toLowerCase());
        }
    }

    @FXML
    private void scrollToAccueil() {
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
        javafx.application.Platform.runLater(() -> {
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

    // Action handlers pour les services
    @FXML private void handleGestionColis() { System.out.println("Gestion des colis"); }
    @FXML private void handleSuiviColis() { System.out.println("Suivi des colis"); }
    @FXML private void handleLivraisonExpress() { System.out.println("Livraison express"); }
    @FXML private void handleSupport() { System.out.println("Support"); }
    @FXML private void handleAPI() { System.out.println("API Integration"); }
    @FXML private void handleStatistiques() { System.out.println("Statistiques"); }

    @FXML private void handleNouveauColis() { System.out.println("Nouveau colis"); }
    @FXML private void handleEnSavoirPlus() { System.out.println("En savoir plus"); }

    @FXML
    private void handleSendMessage() {
        String name = contactName.getText();
        String email = contactEmail.getText();
        String message = contactMessage.getText();
        System.out.println("Message de " + name + " (" + email + "): " + message);
        // TODO: Envoyer le message
    }

    @FXML
    private void handleLogout() {
        try {
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
        }
    }
}