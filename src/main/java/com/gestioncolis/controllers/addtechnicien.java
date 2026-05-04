package com.gestioncolis.controllers;

import com.gestioncolis.SceneNavigator;
import com.gestioncolis.entities.technicien;
import com.gestioncolis.entities.vehicule;
import com.gestioncolis.services.EmailService;
import com.gestioncolis.services.Servicetechnicien;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.ArrayList;

public class addtechnicien {
    private final Servicetechnicien servicetechnicien;
    private final SceneNavigator sceneNavigator;

    @FXML
    private TextField nomField;
    @FXML
    private TextField prenomField;
    @FXML
    private TextField specialiteField;
    @FXML
    private TextField telephoneField;
    @FXML
    private TextField emailField;
    @FXML
    private ListView<vehicule> vehiculesListView;
    @FXML
    private Label feedbackLabel;

    public addtechnicien(Servicetechnicien servicetechnicien, SceneNavigator sceneNavigator) {
        this.servicetechnicien = servicetechnicien;
        this.sceneNavigator = sceneNavigator;
    }

    @FXML
    private void initialize() {
        configurerListeVehicules();
        configurerValidationVisuelle();
        afficherMessageInfo("Remplissez le formulaire puis selectionnez les vehicules du technicien. L'email doit etre valide.");
    }

    @FXML
    private void handleSave() {
        try {
            reinitialiserStylesValidation();
            technicien nouveauTechnicien = new technicien(
                    lireTexte(nomField),
                    lireTexte(prenomField),
                    lireTexte(specialiteField),
                    lireTexte(telephoneField),
                    lireTexte(emailField),
                    new ArrayList<>(vehiculesListView.getSelectionModel().getSelectedItems())
            );

            technicien technicienAjoute = servicetechnicien.ajouterTechnicien(nouveauTechnicien);
            sceneNavigator.showDisplayTechnicienView(construireMessageAjout(technicienAjoute));
        } catch (IllegalArgumentException exception) {
            afficherErreurValidation(exception.getMessage());
        } catch (IllegalStateException exception) {
            afficherErreur("Impossible d'ajouter le technicien.", exception);
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir la liste des techniciens.", exception);
        }
    }

    @FXML
    private void handleBack() {
        try {
            sceneNavigator.showDisplayTechnicienView("Retour a la liste des techniciens.");
        } catch (IOException exception) {
            afficherErreur("Impossible de revenir vers la liste.", exception);
        }
    }

    @FXML
    private void handleGoVehicules() {
        try {
            sceneNavigator.showDisplayView("Retour a la liste des vehicules.");
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir la liste des vehicules.", exception);
        }
    }

    @FXML
    private void handleGoTechniciens() {
        handleBack();
    }

    @FXML
    private void handleOpenChatbot() {
        try {
            sceneNavigator.showChatbotWindow();
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir le chatbot Ollama.", exception);
        }
    }

    private void configurerListeVehicules() {
        vehiculesListView.setItems(servicetechnicien.afficherVehicules());
        vehiculesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        vehiculesListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(vehicule item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }

                String statutAffectation = item.getTechnicienId() == null
                        ? "Non assigne"
                        : "Affecte a " + item.getTechnicienAffichage();
                setText(item.getMatricule() + " - " + item.getMarque() + " " + item.getModele() + " | " + statutAffectation);
            }
        });
    }

    private String construireMessageAjout(technicien technicienAjoute) {
        String message = "Technicien ajoute : " + technicienAjoute.getNomComplet();

        try {
            EmailService.sendTechnicienCreationEmail(technicienAjoute);
            return message + ". Email envoye a " + technicienAjoute.getEmail() + ".";
        } catch (RuntimeException exception) {
            return message + ". Email non envoye : " + exception.getMessage();
        }
    }

    private String lireTexte(TextField champ) {
        return champ.getText() == null ? "" : champ.getText().trim();
    }

    private void configurerValidationVisuelle() {
        configurerChamp(nomField);
        configurerChamp(prenomField);
        configurerChamp(specialiteField);
        configurerChamp(telephoneField);
        configurerChamp(emailField);
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
        nomField.getStyleClass().remove("field-input-error");
        prenomField.getStyleClass().remove("field-input-error");
        specialiteField.getStyleClass().remove("field-input-error");
        telephoneField.getStyleClass().remove("field-input-error");
        emailField.getStyleClass().remove("field-input-error");
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
        if (messageNormalise.contains("nom")) {
            ajouterStyleErreur(nomField);
        }
        if (messageNormalise.contains("prenom")) {
            ajouterStyleErreur(prenomField);
        }
        if (messageNormalise.contains("specialite")) {
            ajouterStyleErreur(specialiteField);
        }
        if (messageNormalise.contains("telephone")) {
            ajouterStyleErreur(telephoneField);
        }
        if (messageNormalise.contains("email")) {
            ajouterStyleErreur(emailField);
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
