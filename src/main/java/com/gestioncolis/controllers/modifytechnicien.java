package com.gestioncolis.controllers;

import com.gestioncolis.SceneNavigator;
import com.gestioncolis.entities.technicien;
import com.gestioncolis.entities.vehicule;
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
import java.util.Set;
import java.util.stream.Collectors;

public class modifytechnicien {
    private final Servicetechnicien servicetechnicien;
    private final SceneNavigator sceneNavigator;
    private technicien technicienAModifier;

    @FXML
    private Label formTitleLabel;
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

    public modifytechnicien(Servicetechnicien servicetechnicien, SceneNavigator sceneNavigator) {
        this.servicetechnicien = servicetechnicien;
        this.sceneNavigator = sceneNavigator;
    }

    @FXML
    private void initialize() {
        configurerListeVehicules();
        configurerValidationVisuelle();
        afficherMessageInfo("Selectionnez un technicien depuis la liste pour le modifier. L'email doit etre valide.");
        mettreAJourFormulaire();
    }

    public void setTechnicien(technicien technicien) {
        this.technicienAModifier = technicien;
        mettreAJourFormulaire();
    }

    @FXML
    private void handleUpdate() {
        if (technicienAModifier == null) {
            afficherErreurValidation("Aucun technicien n'a ete selectionne.");
            return;
        }

        try {
            reinitialiserStylesValidation();
            technicien technicienModifie = new technicien(
                    technicienAModifier.getId(),
                    lireTexte(nomField),
                    lireTexte(prenomField),
                    lireTexte(specialiteField),
                    lireTexte(telephoneField),
                    lireTexte(emailField),
                    new ArrayList<>(vehiculesListView.getSelectionModel().getSelectedItems())
            );

            boolean modificationReussie = servicetechnicien.modifierTechnicien(technicienModifie);
            if (!modificationReussie) {
                afficherErreurValidation("La modification a echoue.");
                return;
            }

            sceneNavigator.showDisplayTechnicienView("Technicien modifie : " + technicienModifie.getNomComplet());
        } catch (IllegalArgumentException exception) {
            afficherErreurValidation(exception.getMessage());
        } catch (IOException exception) {
            afficherErreur("Impossible de revenir vers la liste des techniciens.", exception);
        }
    }

    @FXML
    private void handleBack() {
        try {
            sceneNavigator.showDisplayTechnicienView("Modification annulee.");
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
        try {
            sceneNavigator.showDisplayTechnicienView("Retour a la liste des techniciens.");
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

    private void mettreAJourFormulaire() {
        if (technicienAModifier == null || nomField == null) {
            return;
        }

        formTitleLabel.setText("Modifier le technicien #" + technicienAModifier.getId());
        nomField.setText(technicienAModifier.getNom());
        prenomField.setText(technicienAModifier.getPrenom());
        specialiteField.setText(technicienAModifier.getSpecialite());
        telephoneField.setText(technicienAModifier.getTelephone());
        emailField.setText(technicienAModifier.getEmail());

        vehiculesListView.getSelectionModel().clearSelection();
        Set<Integer> vehiculesSelectionnes = technicienAModifier.getVehicules().stream()
                .map(vehicule::getId)
                .collect(Collectors.toSet());

        for (vehicule vehiculeDisponible : vehiculesListView.getItems()) {
            if (vehiculesSelectionnes.contains(vehiculeDisponible.getId())) {
                vehiculesListView.getSelectionModel().select(vehiculeDisponible);
            }
        }

        afficherMessageInfo("Mettez a jour les informations et la liste des vehicules.");
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
