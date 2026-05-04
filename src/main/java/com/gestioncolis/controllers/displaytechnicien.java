package com.gestioncolis.controllers;

import com.gestioncolis.SceneNavigator;
import com.gestioncolis.entities.technicien;
import com.gestioncolis.services.PdfGenerator;
import com.gestioncolis.services.Servicetechnicien;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.Optional;

public class displaytechnicien {
    private final Servicetechnicien servicetechnicien;
    private final SceneNavigator sceneNavigator;
    private String initialMessage;
    private FilteredList<technicien> techniciensFiltres;

    @FXML
    private TableView<technicien> technicienTable;
    @FXML
    private TableColumn<technicien, Number> idColumn;
    @FXML
    private TableColumn<technicien, String> nomCompletColumn;
    @FXML
    private TableColumn<technicien, String> specialiteColumn;
    @FXML
    private TableColumn<technicien, String> telephoneColumn;
    @FXML
    private TableColumn<technicien, String> emailColumn;
    @FXML
    private TableColumn<technicien, Number> nombreVehiculesColumn;
    @FXML
    private TableColumn<technicien, String> vehiculesColumn;
    @FXML
    private TextField searchField;
    @FXML
    private Label infoLabel;

    public displaytechnicien(Servicetechnicien servicetechnicien, SceneNavigator sceneNavigator) {
        this.servicetechnicien = servicetechnicien;
        this.sceneNavigator = sceneNavigator;
    }

    @FXML
    private void initialize() {
        configurerColonnes();
        configurerRecherche();
        infoLabel.setText(messageParDefaut());

        technicienTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, ancienTechnicien, nouveauTechnicien) -> mettreAJourMessage(nouveauTechnicien));
    }

    public void setInitialMessage(String initialMessage) {
        this.initialMessage = initialMessage;
        if (infoLabel != null && initialMessage != null && !initialMessage.isBlank()) {
            infoLabel.setText(initialMessage);
        }
    }

    @FXML
    private void handleAdd() {
        try {
            sceneNavigator.showAddTechnicienView();
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir le formulaire d'ajout.", exception);
        }
    }

    @FXML
    private void handleModify() {
        technicien technicienSelectionne = technicienTable.getSelectionModel().getSelectedItem();
        if (technicienSelectionne == null) {
            afficherAvertissement("Veuillez selectionner un technicien a modifier.");
            return;
        }

        try {
            sceneNavigator.showModifyTechnicienView(technicienSelectionne);
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir le formulaire de modification.", exception);
        }
    }

    @FXML
    private void handleDelete() {
        technicien technicienSelectionne = technicienTable.getSelectionModel().getSelectedItem();
        if (technicienSelectionne == null) {
            afficherAvertissement("Veuillez selectionner un technicien a supprimer.");
            return;
        }

        if (!confirmerSuppression(technicienSelectionne)) {
            return;
        }

        boolean suppressionReussie = servicetechnicien.supprimerTechnicien(technicienSelectionne.getId());
        if (suppressionReussie) {
            infoLabel.setText("Technicien supprime : " + technicienSelectionne.getNomComplet());
            technicienTable.refresh();
        } else {
            afficherAvertissement("La suppression a echoue.");
        }
    }

    @FXML
    private void handleRefresh() {
        technicienTable.refresh();
        if (searchField.getText() != null && !searchField.getText().isBlank()) {
            mettreAJourInfoRecherche();
        } else {
            infoLabel.setText("La liste des techniciens est a jour.");
        }
    }

    @FXML
    private void handleStats() {
        try {
            sceneNavigator.showStatView();
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir les statistiques des techniciens.", exception);
        }
    }

    @FXML
    private void handleGeneratePdf() {
        technicien technicienSelectionne = technicienTable.getSelectionModel().getSelectedItem();
        if (technicienSelectionne == null) {
            afficherAvertissement("Veuillez selectionner un technicien pour generer le PDF.");
            return;
        }

        try {
            String cheminPdf = PdfGenerator.createTechnicienPdf(technicienSelectionne);
            infoLabel.setText("PDF technicien genere : " + cheminPdf);
            afficherInformation("PDF genere avec succes.", "Fichier cree : " + cheminPdf);
        } catch (IOException exception) {
            afficherErreur("Impossible de generer le PDF du technicien.", exception);
        }
    }

    @FXML
    private void handleGoVehicules() {
        handleVehicules();
    }

    @FXML
    private void handleGoTechniciens() {
        infoLabel.setText("Vous etes deja dans la section techniciens.");
    }

    @FXML
    private void handleOpenChatbot() {
        try {
            sceneNavigator.showChatbotWindow();
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir le chatbot Ollama.", exception);
        }
    }

    @FXML
    private void handleVehicules() {
        try {
            sceneNavigator.showDisplayView("Retour a la liste des vehicules.");
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir la liste des vehicules.", exception);
        }
    }

    @FXML
    private void handleClearSearch() {
        searchField.clear();
        technicienTable.getSelectionModel().clearSelection();
        infoLabel.setText(messageParDefaut());
    }

    private void configurerColonnes() {
        idColumn.setCellValueFactory(cellData -> new ReadOnlyIntegerWrapper(cellData.getValue().getId()));
        nomCompletColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getNomComplet()));
        specialiteColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getSpecialite()));
        telephoneColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getTelephone()));
        emailColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getEmail()));
        nombreVehiculesColumn.setCellValueFactory(cellData -> new ReadOnlyIntegerWrapper(cellData.getValue().getNombreVehicules()));
        vehiculesColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getVehiculesResume()));
    }

    private void configurerRecherche() {
        techniciensFiltres = new FilteredList<>(servicetechnicien.afficherTechniciens(), technicien -> true);
        SortedList<technicien> techniciensTries = new SortedList<>(techniciensFiltres);
        techniciensTries.comparatorProperty().bind(technicienTable.comparatorProperty());
        technicienTable.setItems(techniciensTries);
        technicienTable.setPlaceholder(new Label("Aucun technicien disponible."));

        searchField.textProperty().addListener((observable, ancienneValeur, nouvelleValeur) -> {
            appliquerRecherche(nouvelleValeur);
            technicienTable.getSelectionModel().clearSelection();
            mettreAJourInfoRecherche();
        });
    }

    private void appliquerRecherche(String recherche) {
        String rechercheNormalisee = normaliser(recherche);
        techniciensFiltres.setPredicate(technicien -> {
            if (rechercheNormalisee.isBlank()) {
                return true;
            }

            return contient(String.valueOf(technicien.getId()), rechercheNormalisee)
                    || contient(technicien.getNom(), rechercheNormalisee)
                    || contient(technicien.getPrenom(), rechercheNormalisee)
                    || contient(technicien.getNomComplet(), rechercheNormalisee)
                    || contient(technicien.getSpecialite(), rechercheNormalisee)
                    || contient(technicien.getTelephone(), rechercheNormalisee)
                    || contient(technicien.getEmail(), rechercheNormalisee)
                    || contient(technicien.getVehiculesResume(), rechercheNormalisee)
                    || contient(String.valueOf(technicien.getNombreVehicules()), rechercheNormalisee);
        });

        technicienTable.setPlaceholder(new Label(
                rechercheNormalisee.isBlank()
                        ? "Aucun technicien disponible."
                        : "Aucun technicien ne correspond a votre recherche."
        ));
    }

    private void mettreAJourMessage(technicien technicienSelectionne) {
        if (technicienSelectionne == null) {
            mettreAJourInfoRecherche();
            return;
        }

        infoLabel.setText("Technicien selectionne : " + technicienSelectionne.getNomComplet()
                + " | Vehicules : " + technicienSelectionne.getVehiculesResume());
    }

    private void mettreAJourInfoRecherche() {
        String recherche = searchField.getText();
        if (recherche != null && !recherche.isBlank()) {
            infoLabel.setText("Recherche techniciens : " + techniciensFiltres.size() + " resultat(s).");
            return;
        }

        infoLabel.setText(initialMessage != null && !initialMessage.isBlank() ? initialMessage : messageParDefaut());
    }

    private boolean contient(String valeur, String rechercheNormalisee) {
        return normaliser(valeur).contains(rechercheNormalisee);
    }

    private String normaliser(String valeur) {
        return valeur == null ? "" : valeur.toLowerCase().trim();
    }

    private boolean confirmerSuppression(technicien technicienSelectionne) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le technicien " + technicienSelectionne.getNomComplet() + " ?");
        alert.setContentText("Les vehicules associes resteront dans la liste mais seront desaffectes.");

        Optional<ButtonType> resultat = alert.showAndWait();
        return resultat.isPresent() && resultat.get() == ButtonType.OK;
    }

    private String messageParDefaut() {
        return initialMessage != null && !initialMessage.isBlank()
                ? initialMessage
                : "Selectionnez un technicien pour le modifier ou le supprimer.";
    }

    private void afficherErreur(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }

    private void afficherAvertissement(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(message);
        alert.setContentText(null);
        alert.showAndWait();
    }

    private void afficherInformation(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(titre);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
