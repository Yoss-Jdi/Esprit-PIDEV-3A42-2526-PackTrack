package com.gestioncolis.controllers;

import com.gestioncolis.models.Colis;
import com.gestioncolis.models.Utilisateur;
import com.gestioncolis.services.ColisService;
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
import java.util.ArrayList;
import java.util.List;

public class ListeColisController {

    @FXML private TableView<Colis>           tableColis;
    @FXML private TableColumn<Colis, String> colDescription;
    @FXML private TableColumn<Colis, String> colDepart;
    @FXML private TableColumn<Colis, String> colDestination;
    @FXML private TableColumn<Colis, String> colPoids;
    @FXML private TableColumn<Colis, String> colStatut;
    @FXML private TableColumn<Colis, String> colMontant;
    @FXML private TableColumn<Colis, Void>   colActions;
    @FXML private Label                      lblMessage;
    @FXML private Button                     btnAjouter;

    private final ColisService service = new ColisService();

    @FXML
    public void initialize() {
        colDescription.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        colDepart     .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAdresseDepart()));
        colDestination.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAdresseDestination()));
        colPoids      .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPoids() + " kg"));
        colStatut     .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut()));
        colMontant    .setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.2f DT", d.getValue().calculerMontant())));

        configurerSelonRole();
        ajouterColonneActions();
        chargerDonnees();
    }

    private void configurerSelonRole() {
        Utilisateur u = SessionManager.getInstance().getUtilisateurConnecte();
        if (u == null) return;

        boolean estClient = (u.getRole() == Utilisateur.Role.ROLE_CLIENT);

        if (btnAjouter != null) {
            btnAjouter.setVisible(!estClient);
            btnAjouter.setManaged(!estClient);
        }

        colActions.setVisible(!estClient);
    }

    private void chargerDonnees() {
        try {
            Utilisateur u = SessionManager.getInstance().getUtilisateurConnecte();
            if (u == null) return;

            List<Colis> liste;
            switch (u.getRole()) {
                case ROLE_ADMIN      -> liste = service.getAll();
                case ROLE_CLIENT     -> liste = service.getByDestinataire(u.getId());
                case ROLE_ENTREPRISE -> liste = service.getByExpediteur(u.getId());
                default              -> liste = new ArrayList<>();
            }

            tableColis.setItems(FXCollections.observableArrayList(liste));
        } catch (SQLException e) {
            afficherErreur("Erreur chargement : " + e.getMessage());
        }
    }

    private void ajouterColonneActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnModifier  = new Button("✏️ Modifier");
            private final Button btnSupprimer = new Button("🗑️ Supprimer");
            private final HBox   box          = new HBox(6, btnModifier, btnSupprimer);

            {
                btnModifier.setStyle(
                        "-fx-background-color: #2980b9; -fx-text-fill: white;" +
                                "-fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8;");
                btnSupprimer.setStyle(
                        "-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                                "-fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8;");

                btnModifier.setOnAction(e -> ouvrirModifier(
                        getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(e -> confirmerSuppression(
                        getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }

                Colis colis = getTableView().getItems().get(getIndex());
                boolean enAttente = "en_attente".equals(colis.getStatut());

                btnModifier .setDisable(!enAttente);
                btnSupprimer.setDisable(!enAttente);
                btnModifier .setOpacity(enAttente ? 1.0 : 0.4);
                btnSupprimer.setOpacity(enAttente ? 1.0 : 0.4);

                if (!enAttente) {
                    String raison = "Colis « " + colis.getStatut() + " » : action non disponible";
                    btnModifier .setTooltip(new Tooltip(raison));
                    btnSupprimer.setTooltip(new Tooltip(raison));
                } else {
                    btnModifier .setTooltip(null);
                    btnSupprimer.setTooltip(null);
                }
                setGraphic(box);
            }
        });
    }

    private void ouvrirModifier(Colis colis) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/modifierColis.fxml"));
            Parent root = loader.load();
            ModifierColisController ctrl = loader.getController();
            ctrl.setColis(colis);
            tableColis.getScene().setRoot(root);
        } catch (IOException e) {
            afficherErreur("Erreur navigation : " + e.getMessage());
        }
    }

    private void confirmerSuppression(Colis colis) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setHeaderText("Supprimer le colis ?");
        alert.setContentText("Cette action est irréversible.");
        alert.showAndWait().ifPresent(rep -> {
            if (rep == ButtonType.OK) {
                try {
                    service.supprimer(colis.getId());
                    afficherSucces("Colis supprimé.");
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
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ajouterColis.fxml"));
            tableColis.getScene().setRoot(root);
        } catch (IOException e) {
            afficherErreur("Erreur navigation : " + e.getMessage());
        }
    }

    @FXML
    public void retourDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            tableColis.getScene().setRoot(root);
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