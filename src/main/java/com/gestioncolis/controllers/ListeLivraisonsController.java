package com.gestioncolis.controllers;

import com.gestioncolis.models.Livraison;
import com.gestioncolis.models.Utilisateur;
import com.gestioncolis.services.LivraisonService;
import com.gestioncolis.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ListeLivraisonsController {

    @FXML private TableView<Livraison>           tableLivraisons;
    @FXML private TableColumn<Livraison, String> colDescriptionColis;
    @FXML private TableColumn<Livraison, String> colNomLivreur;      // visible uniquement pour ROLE_ADMIN
    @FXML private TableColumn<Livraison, String> colStatut;
    @FXML private TableColumn<Livraison, String> colDistance;
    @FXML private TableColumn<Livraison, String> colDuree;
    @FXML private TableColumn<Livraison, String> colTotal;
    @FXML private TableColumn<Livraison, String> colDateDebut;
    @FXML private TableColumn<Livraison, Void>   colActions;
    @FXML private Label                          lblMessage;

    private final LivraisonService service = new LivraisonService();
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        colDescriptionColis.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescriptionColis()));
        colStatut   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut()));
        colDistance .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDistanceKm() + " km"));
        colDuree    .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDureeFormatee()));
        colTotal    .setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.2f DT", d.getValue().getTotal())));
        colDateDebut.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDateDebut() != null ? d.getValue().getDateDebut().format(FMT) : "-"));

        Utilisateur u = SessionManager.getInstance().getUtilisateurConnecte();
        boolean estAdmin = (u != null && u.getRole() == Utilisateur.Role.ROLE_ADMIN);
        colNomLivreur.setVisible(estAdmin);
        if (estAdmin) {
            //col livreur
            colNomLivreur.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNomLivreur()));
        }

        ajouterColonneActions();
        chargerDonnees();
    }

    private void chargerDonnees() {
        try {
            Utilisateur u = SessionManager.getInstance().getUtilisateurConnecte();
            if (u == null) return;

            List<Livraison> liste;
            if (u.getRole() == Utilisateur.Role.ROLE_ADMIN) {
                liste = service.getAll();
            } else {
                liste = service.getByLivreur(u.getId());
            }
            tableLivraisons.setItems(FXCollections.observableArrayList(liste));
        } catch (SQLException e) {
            afficherErreur("Erreur chargement : " + e.getMessage());
        }
    }

    private void ajouterColonneActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnTerminer  = new Button("✅ Terminer");
            private final Button btnSupprimer = new Button("🗑️ Supprimer");
            private final HBox   box          = new HBox(6, btnTerminer, btnSupprimer);

            {
                btnTerminer.setStyle(
                        "-fx-background-color: #27ae60; -fx-text-fill: white;" +
                                "-fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8;");
                btnSupprimer.setStyle(
                        "-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                                "-fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8;");

                btnTerminer.setOnAction(e -> confirmerTerminer(
                        getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(e -> confirmerSuppression(
                        getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Livraison liv = getTableView().getItems().get(getIndex());
                btnTerminer.setDisable("termine".equals(liv.getStatut()));
                btnTerminer.setOpacity("termine".equals(liv.getStatut()) ? 0.4 : 1.0);
                setGraphic(box);
            }
        });
    }

    private void confirmerTerminer(Livraison liv) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Terminer la livraison");
        alert.setHeaderText("Terminer la livraison du colis « " + liv.getDescriptionColis() + " » ?");
        alert.setContentText("Le colis associé sera marqué comme 'livré'.");
        alert.showAndWait().ifPresent(rep -> {
            if (rep == ButtonType.OK) {
                try {
                    service.terminer(liv.getId());
                    afficherSucces("Livraison terminée avec succès.");
                    chargerDonnees();
                } catch (Exception e) {
                    afficherErreur("Erreur : " + e.getMessage());
                }
            }
        });
    }

    private void confirmerSuppression(Livraison liv) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setHeaderText("Supprimer la livraison du colis « " + liv.getDescriptionColis() + " » ?");
        alert.setContentText("Cette action est irréversible.");
        alert.showAndWait().ifPresent(rep -> {
            if (rep == ButtonType.OK) {
                try {
                    service.supprimer(liv.getId());
                    afficherSucces("Livraison supprimée.");
                    chargerDonnees();
                } catch (Exception e) {
                    afficherErreur("Erreur : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    public void ouvrirAjouter() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ajouterLivraison.fxml"));
            tableLivraisons.getScene().setRoot(root);
        } catch (IOException e) {
            afficherErreur("Erreur navigation : " + e.getMessage());
        }
    }

    @FXML
    public void retourDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            tableLivraisons.getScene().setRoot(root);
        } catch (IOException e) {
            afficherErreur("Erreur navigation : " + e.getMessage());
        }
    }

    private void afficherSucces(String msg) {
        lblMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
        lblMessage.setText(msg);
    }

    private void afficherErreur(String msg) {
        lblMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        lblMessage.setText(msg);
    }
}