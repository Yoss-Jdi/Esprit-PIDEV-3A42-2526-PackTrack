package com.gestioncolis.controllers;

import com.gestioncolis.SceneNavigator;
import com.gestioncolis.entities.vehicule;
import com.gestioncolis.services.PdfGenerator;
import com.gestioncolis.services.Servicevehicule;
import javafx.beans.property.ReadOnlyDoubleWrapper;
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

public class displayContoller {
    private final Servicevehicule servicevehicule;
    private final SceneNavigator sceneNavigator;
    private String initialMessage;
    private FilteredList<vehicule> vehiculesFiltres;

    @FXML
    private TableView<vehicule> vehiculeTable;
    @FXML
    private TableColumn<vehicule, Number> idColumn;
    @FXML
    private TableColumn<vehicule, String> matriculeColumn;
    @FXML
    private TableColumn<vehicule, String> marqueColumn;
    @FXML
    private TableColumn<vehicule, String> modeleColumn;
    @FXML
    private TableColumn<vehicule, String> couleurColumn;
    @FXML
    private TableColumn<vehicule, Number> prixColumn;
    @FXML
    private TableColumn<vehicule, String> disponibiliteColumn;
    @FXML
    private TableColumn<vehicule, String> technicienColumn;
    @FXML
    private TextField searchField;
    @FXML
    private Label infoLabel;

    public displayContoller(Servicevehicule servicevehicule, SceneNavigator sceneNavigator) {
        this.servicevehicule = servicevehicule;
        this.sceneNavigator = sceneNavigator;
    }

    @FXML
    private void initialize() {
        configurerColonnes();
        configurerRecherche();
        infoLabel.setText(messageParDefaut());

        vehiculeTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, ancienVehicule, nouveauVehicule) -> mettreAJourMessage(nouveauVehicule));
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
            sceneNavigator.showAddView();
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir le formulaire d'ajout.", exception);
        }
    }

    @FXML
    private void handleModify() {
        vehicule vehiculeSelectionne = vehiculeTable.getSelectionModel().getSelectedItem();
        if (vehiculeSelectionne == null) {
            afficherAvertissement("Veuillez selectionner un vehicule a modifier.");
            return;
        }

        try {
            sceneNavigator.showModifyView(vehiculeSelectionne);
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir le formulaire de modification.", exception);
        }
    }

    @FXML
    private void handleDelete() {
        vehicule vehiculeSelectionne = vehiculeTable.getSelectionModel().getSelectedItem();
        if (vehiculeSelectionne == null) {
            afficherAvertissement("Veuillez selectionner un vehicule a supprimer.");
            return;
        }

        if (!confirmerSuppression(vehiculeSelectionne)) {
            return;
        }

        boolean suppressionReussie = servicevehicule.supprimerVehicule(vehiculeSelectionne.getId());
        if (suppressionReussie) {
            infoLabel.setText("Vehicule supprime : " + vehiculeSelectionne.getMatricule());
        } else {
            afficherAvertissement("La suppression a echoue.");
        }
    }

    @FXML
    private void handleRefresh() {
        vehiculeTable.refresh();
        if (searchField.getText() != null && !searchField.getText().isBlank()) {
            mettreAJourInfoRecherche();
        } else {
            infoLabel.setText("La liste est a jour.");
        }
    }

    @FXML
    private void handleGeneratePdf() {
        vehicule vehiculeSelectionne = vehiculeTable.getSelectionModel().getSelectedItem();
        if (vehiculeSelectionne == null) {
            afficherAvertissement("Veuillez selectionner un vehicule pour generer le PDF.");
            return;
        }

        try {
            String cheminPdf = PdfGenerator.createVehiculePdf(vehiculeSelectionne);
            infoLabel.setText("PDF vehicule genere : " + cheminPdf);
            afficherInformation("PDF genere avec succes.", "Fichier cree : " + cheminPdf);
        } catch (IOException exception) {
            afficherErreur("Impossible de generer le PDF du vehicule.", exception);
        }
    }

    @FXML
    private void handleGoVehicules() {
        infoLabel.setText("Vous etes deja dans la section vehicules.");
    }

    @FXML
    private void handleGoTechniciens() {
        handleTechniciens();
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
    private void handleTechniciens() {
        try {
            sceneNavigator.showDisplayTechnicienView("Gestion des techniciens et affectation des vehicules.");
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir la liste des techniciens.", exception);
        }
    }

    @FXML
    private void handleClearSearch() {
        searchField.clear();
        vehiculeTable.getSelectionModel().clearSelection();
        infoLabel.setText(messageParDefaut());
    }

    private void configurerColonnes() {
        idColumn.setCellValueFactory(cellData -> new ReadOnlyIntegerWrapper(cellData.getValue().getId()));
        matriculeColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getMatricule()));
        marqueColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getMarque()));
        modeleColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getModele()));
        couleurColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getCouleur()));
        prixColumn.setCellValueFactory(cellData -> new ReadOnlyDoubleWrapper(cellData.getValue().getPrixLocation()));
        disponibiliteColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().isDisponible() ? "Disponible" : "Indisponible"));
        technicienColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().getTechnicienAffichage()));
    }

    private void configurerRecherche() {
        vehiculesFiltres = new FilteredList<>(servicevehicule.afficherVehicules(), vehicule -> true);
        SortedList<vehicule> vehiculesTries = new SortedList<>(vehiculesFiltres);
        vehiculesTries.comparatorProperty().bind(vehiculeTable.comparatorProperty());
        vehiculeTable.setItems(vehiculesTries);
        vehiculeTable.setPlaceholder(new Label("Aucun vehicule disponible."));

        searchField.textProperty().addListener((observable, ancienneValeur, nouvelleValeur) -> {
            appliquerRecherche(nouvelleValeur);
            vehiculeTable.getSelectionModel().clearSelection();
            mettreAJourInfoRecherche();
        });
    }

    private void appliquerRecherche(String recherche) {
        String rechercheNormalisee = normaliser(recherche);
        vehiculesFiltres.setPredicate(vehicule -> {
            if (rechercheNormalisee.isBlank()) {
                return true;
            }

            return contient(vehicule.getMatricule(), rechercheNormalisee)
                    || contient(vehicule.getMarque(), rechercheNormalisee)
                    || contient(vehicule.getModele(), rechercheNormalisee)
                    || contient(vehicule.getCouleur(), rechercheNormalisee)
                    || contient(vehicule.getTechnicienAffichage(), rechercheNormalisee)
                    || contient(vehicule.isDisponible() ? "disponible" : "indisponible", rechercheNormalisee)
                    || contient(String.valueOf(vehicule.getId()), rechercheNormalisee)
                    || contient(String.valueOf(vehicule.getPrixLocation()), rechercheNormalisee);
        });

        vehiculeTable.setPlaceholder(new Label(
                rechercheNormalisee.isBlank()
                        ? "Aucun vehicule disponible."
                        : "Aucun vehicule ne correspond a votre recherche."
        ));
    }

    private void mettreAJourMessage(vehicule vehiculeSelectionne) {
        if (vehiculeSelectionne == null) {
            mettreAJourInfoRecherche();
            return;
        }

        infoLabel.setText("Vehicule selectionne : " + vehiculeSelectionne.getMatricule() + " - " + vehiculeSelectionne.getMarque());
    }

    private void mettreAJourInfoRecherche() {
        String recherche = searchField.getText();
        if (recherche != null && !recherche.isBlank()) {
            infoLabel.setText("Recherche vehicules : " + vehiculesFiltres.size() + " resultat(s).");
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

    private boolean confirmerSuppression(vehicule vehiculeSelectionne) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le vehicule " + vehiculeSelectionne.getMatricule() + " ?");
        alert.setContentText("Cette action retirera le vehicule de la liste courante.");

        Optional<ButtonType> resultat = alert.showAndWait();
        return resultat.isPresent() && resultat.get() == ButtonType.OK;
    }

    private String messageParDefaut() {
        return initialMessage != null && !initialMessage.isBlank()
                ? initialMessage
                : "Selectionnez un vehicule pour le modifier ou le supprimer.";
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
