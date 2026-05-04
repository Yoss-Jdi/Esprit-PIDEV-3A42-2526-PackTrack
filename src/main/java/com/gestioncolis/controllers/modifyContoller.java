package com.gestioncolis.controllers;

import com.gestioncolis.SceneNavigator;
import com.gestioncolis.entities.vehicule;
import com.gestioncolis.services.Servicevehicule;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class modifyContoller {
    private final Servicevehicule servicevehicule;
    private final SceneNavigator sceneNavigator;
    private vehicule vehiculeAModifier;

    @FXML
    private Label formTitleLabel;
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
    private CheckBox disponibleCheckBox;
    @FXML
    private Label feedbackLabel;

    public modifyContoller(Servicevehicule servicevehicule, SceneNavigator sceneNavigator) {
        this.servicevehicule = servicevehicule;
        this.sceneNavigator = sceneNavigator;
    }

    @FXML
    private void initialize() {
        disponibleCheckBox.setSelected(true);
        configurerValidationVisuelle();
        afficherMessageInfo("Selectionnez un vehicule depuis la liste pour le modifier. Format matricule : 123TN4567.");
        mettreAJourFormulaire();
    }

    public void setVehicule(vehicule vehicule) {
        this.vehiculeAModifier = vehicule;
        mettreAJourFormulaire();
    }

    @FXML
    private void handleUpdate() {
        if (vehiculeAModifier == null) {
            afficherErreurValidation("Aucun vehicule n'a ete selectionne.");
            return;
        }

        try {
            reinitialiserStylesValidation();
            vehicule vehiculeModifie = new vehicule(
                    vehiculeAModifier.getId(),
                    lireTexte(matriculeField),
                    lireTexte(marqueField),
                    lireTexte(modeleField),
                    lireTexte(couleurField),
                    lirePrixLocation(),
                    disponibleCheckBox.isSelected()
            );

            boolean modificationReussie = servicevehicule.modifierVehicule(vehiculeModifie);
            if (!modificationReussie) {
                afficherErreurValidation("La modification a echoue.");
                return;
            }

            sceneNavigator.showDisplayView("Vehicule modifie : " + vehiculeModifie.getMatricule());
        } catch (IllegalArgumentException exception) {
            afficherErreurValidation(exception.getMessage());
        } catch (IOException exception) {
            afficherErreur("Impossible de revenir vers la liste des vehicules.", exception);
        }
    }

    @FXML
    private void handleBack() {
        try {
            sceneNavigator.showDisplayView("Modification annulee.");
        } catch (IOException exception) {
            afficherErreur("Impossible de revenir vers la liste.", exception);
        }
    }

    @FXML
    private void handleGoVehicules() {
        try {
            sceneNavigator.showDisplayView("Retour a la liste des vehicules.");
        } catch (IOException exception) {
            afficherErreur("Impossible de revenir vers la liste des vehicules.", exception);
        }
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

    private void mettreAJourFormulaire() {
        if (vehiculeAModifier == null || matriculeField == null) {
            return;
        }

        formTitleLabel.setText("Modifier le vehicule #" + vehiculeAModifier.getId());
        matriculeField.setText(vehiculeAModifier.getMatricule());
        marqueField.setText(vehiculeAModifier.getMarque());
        modeleField.setText(vehiculeAModifier.getModele());
        couleurField.setText(vehiculeAModifier.getCouleur());
        prixLocationField.setText(String.valueOf(vehiculeAModifier.getPrixLocation()));
        disponibleCheckBox.setSelected(vehiculeAModifier.isDisponible());
        afficherMessageInfo("Mettez a jour les champs puis cliquez sur enregistrer.");
    }

    private String lireTexte(TextField champ) {
        return champ.getText() == null ? "" : champ.getText().trim();
    }

    private double lirePrixLocation() {
        try {
            double prixLocation = Double.parseDouble(prixLocationField.getText().trim());
            return prixLocation;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Le prix de location doit etre un nombre valide.");
        }
    }

    private void configurerValidationVisuelle() {
        configurerChamp(matriculeField);
        configurerChamp(marqueField);
        configurerChamp(modeleField);
        configurerChamp(couleurField);
        configurerChamp(prixLocationField);
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
        if (messageNormalise.contains("marque")) {
            ajouterStyleErreur(marqueField);
        }
        if (messageNormalise.contains("modele")) {
            ajouterStyleErreur(modeleField);
        }
        if (messageNormalise.contains("couleur")) {
            ajouterStyleErreur(couleurField);
        }
        if (messageNormalise.contains("prix")) {
            ajouterStyleErreur(prixLocationField);
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
