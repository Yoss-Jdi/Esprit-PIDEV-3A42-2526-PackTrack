package com.gestioncolis.controllers;

import com.gestioncolis.models.Colis;
import com.gestioncolis.models.Livraison;
import com.gestioncolis.services.ColisService;
import com.gestioncolis.services.LivraisonService;
import com.gestioncolis.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class AjouterLivraisonController {

    @FXML private Label              lblLivreur;
    @FXML private Label              lblErreur;
    @FXML private Label              lblSelectionInfo;

    @FXML private TableView<Colis>           tableColisDisponibles;
    @FXML private TableColumn<Colis, String> colCDesc;
    @FXML private TableColumn<Colis, String> colCDep;
    @FXML private TableColumn<Colis, String> colCDest;
    @FXML private TableColumn<Colis, String> colCPoid;
    @FXML private TableColumn<Colis, String> colCMontant;

    private final LivraisonService livraisonService = new LivraisonService();
    private final ColisService     colisService     = new ColisService();

    @FXML
    public void initialize() {
        if (SessionManager.getInstance().isConnecte()) {
            lblLivreur.setText(SessionManager.getInstance().getUtilisateurConnecte().getDisplayName());
        } else {
            lblLivreur.setText("(mode dev)");
        }

        colCDesc   .setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getDescription(), "—")));
        colCDep    .setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getAdresseDepart(), "—")));
        colCDest   .setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getAdresseDestination(), "—")));
        colCPoid   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPoids() + " kg"));
        colCMontant.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.2f DT", d.getValue().calculerMontant())));

        tableColisDisponibles.setRowFactory(tv -> {
            TableRow<Colis> row = new TableRow<>();
            row.selectedProperty().addListener((obs, was, isNow) ->
                    row.setStyle(isNow ? "-fx-background-color: #d6eaf8;" : ""));
            return row;
        });

        tableColisDisponibles.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel != null) {
                        lblSelectionInfo.setText(nvl(sel.getDescription(), "colis")
                                + "  ·  " + sel.getAdresseDepart()
                                + " → " + sel.getAdresseDestination()
                                + "  ·  " + sel.getPoids() + " kg"
                                + "  ·  " + String.format("%.2f DT", sel.calculerMontant()));
                        lblErreur.setText("");
                    } else {
                        lblSelectionInfo.setText("");
                    }
                });

        chargerColisDisponibles();
    }

    private void chargerColisDisponibles() {
        try {
            List<Colis> dispo = colisService.getColisDisponibles();
            tableColisDisponibles.setItems(FXCollections.observableArrayList(dispo));
        } catch (SQLException e) {
            lblErreur.setText("Erreur chargement colis : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void enregistrer() {
        lblErreur.setText("");

        Colis colis = tableColisDisponibles.getSelectionModel().getSelectedItem();
        if (colis == null) {
            lblErreur.setText("Veuillez sélectionner un colis dans la liste.");
            return;
        }

        if (!SessionManager.getInstance().isConnecte()) {
            lblErreur.setText("Erreur : aucun utilisateur connecté.");
            return;
        }

        int livreurId = SessionManager.getInstance().getIdConnecte();

        Livraison liv = new Livraison(colis.getId(), livreurId, 0);
        liv.setDistanceKm(0);
        liv.setDureeEstimeeMinutes(0);

        try {
            livraisonService.ajouter(liv);
            retourListe();
        } catch (Exception e) {
            lblErreur.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void retourListe() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/listeLivraisons.fxml"));
            tableColisDisponibles.getScene().setRoot(root);
        } catch (IOException e) {
            lblErreur.setText("Erreur navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String nvl(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }
}