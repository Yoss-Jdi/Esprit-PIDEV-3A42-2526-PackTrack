package com.example.rayen.controllers;

import com.example.rayen.SceneNavigator;
import com.example.rayen.entities.vehicule;
import com.example.rayen.services.EmailService;
import com.example.rayen.services.Servicevehicule;
import com.example.rayen.utils.ValidationUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class addContoller {
    private final Servicevehicule servicevehicule;
    private final SceneNavigator sceneNavigator;

    @FXML
    private TextField matriculeField;
    @FXML
    private TextField marqueField;
    @FXML
    private TextField modeleField;
    @FXML
    private TextField couleurField;
    @FXML
    private TextField prixLocationField;
    @FXML
    private TextField notificationEmailField;
    @FXML
    private CheckBox disponibleCheckBox;
    @FXML
    private Label feedbackLabel;

    public addContoller(Servicevehicule servicevehicule, SceneNavigator sceneNavigator) {
        this.servicevehicule = servicevehicule;
        this.sceneNavigator = sceneNavigator;
    }

    @FXML
    private void initialize() {
        disponibleCheckBox.setSelected(true);
        configurerValidationVisuelle();
        afficherMessageInfo("Remplissez le formulaire pour ajouter un vehicule. Format matricule : 123TN4567.");
    }

    @FXML
    private void handleSave() {
        try {
            reinitialiserStylesValidation();
            vehicule nouveauVehicule = lireVehiculeDepuisFormulaire();
            String emailNotification = lireEmailNotification();
            vehicule vehiculeAjoute = servicevehicule.ajouterVehicule(nouveauVehicule);
            sceneNavigator.showDisplayView(construireMessageAjout(vehiculeAjoute, emailNotification));
        } catch (IllegalArgumentException exception) {
            afficherErreurValidation(exception.getMessage());
        } catch (IllegalStateException exception) {
            afficherErreur("Impossible d'ajouter le vehicule.", exception);
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir la liste des vehicules.", exception);
        }
    }

    @FXML
    private void handleBack() {
        try {
            sceneNavigator.showDisplayView("Retour a la liste des vehicules.");
        } catch (IOException exception) {
            afficherErreur("Impossible de revenir vers la liste.", exception);
        }
    }

    @FXML
    private void handleGoVehicules() {
        handleBack();
    }

    @FXML
    private void handleGoTechniciens() {
        try {
            sceneNavigator.showDisplayTechnicienView("Gestion des techniciens et affectation des vehicules.");
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir la liste des techniciens.", exception);
        }
    }

    @FXML
    private void handleOpenChatbot() {
        try {
            sceneNavigator.showChatbotWindow();
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir le chatbot Ollama.", exception);
        }
    }

    private vehicule lireVehiculeDepuisFormulaire() {
        double prixLocation;
        try {
            prixLocation = Double.parseDouble(prixLocationField.getText().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Le prix de location doit etre un nombre valide.");
        }

        return new vehicule(
                lireTexte(matriculeField),
                lireTexte(marqueField),
                lireTexte(modeleField),
                lireTexte(couleurField),
                prixLocation,
                disponibleCheckBox.isSelected()
        );
    }

    private String lireEmailNotification() {
        return ValidationUtils.normaliserEmailOptionnel(notificationEmailField.getText());
    }

    private String construireMessageAjout(vehicule vehiculeAjoute, String emailNotification) {
        String message = "Vehicule ajoute : " + vehiculeAjoute.getMatricule();
        if (emailNotification.isBlank()) {
            return message + ". Aucun email envoye.";
        }

        try {
            EmailService.sendVehiculeCreationEmail(emailNotification, vehiculeAjoute);
            return message + ". Email envoye a " + emailNotification + ".";
        } catch (RuntimeException exception) {
            return message + ". Email non envoye : " + exception.getMessage();
        }
    }

    private String lireTexte(TextField champ) {
        return champ.getText() == null ? "" : champ.getText().trim();
    }

    private void configurerValidationVisuelle() {
        configurerChamp(matriculeField);
        configurerChamp(marqueField);
        configurerChamp(modeleField);
        configurerChamp(couleurField);
        configurerChamp(prixLocationField);
        configurerChamp(notificationEmailField);
    }

    private void configurerChamp(TextField champ) {
        champ.textProperty().addListener((observable, ancienneValeur, nouvelleValeur) -> {
            champ.getStyleClass().remove("field-input-error");
            feedbackLabel.getStyleClass().remove("feedback-error");
            if (!feedbackLabel.getStyleClass().contains("feedback-info")) {
                feedbackLabel.getStyleClass().add("feedback-info");
            }
        });
    }

    private void reinitialiserStylesValidation() {
        matriculeField.getStyleClass().remove("field-input-error");
        marqueField.getStyleClass().remove("field-input-error");
        modeleField.getStyleClass().remove("field-input-error");
        couleurField.getStyleClass().remove("field-input-error");
        prixLocationField.getStyleClass().remove("field-input-error");
        notificationEmailField.getStyleClass().remove("field-input-error");
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-info");
    }

    private void afficherMessageInfo(String message) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-info");
        feedbackLabel.getStyleClass().add("feedback-info");
    }

    private void afficherErreurValidation(String message) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-info");
        feedbackLabel.getStyleClass().add("feedback-error");

        String messageNormalise = message.toLowerCase();
        if (messageNormalise.contains("matricule")) {
            ajouterStyleErreur(matriculeField);
        }
        if (messageNormalise.contains("email")) {
            ajouterStyleErreur(notificationEmailField);
        }
        if (messageNormalise.contains("prix")) {
            ajouterStyleErreur(prixLocationField);
        }
        if (messageNormalise.contains("marque")) {
            ajouterStyleErreur(marqueField);
        }
        if (messageNormalise.contains("modele")) {
            ajouterStyleErreur(modeleField);
        }
        if (messageNormalise.contains("couleur")) {
            ajouterStyleErreur(couleurField);
        }
    }

    private void ajouterStyleErreur(TextField champ) {
        if (!champ.getStyleClass().contains("field-input-error")) {
            champ.getStyleClass().add("field-input-error");
        }
    }

    private void afficherErreur(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
