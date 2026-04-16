package com.gestioncolis.controllers;

import com.gestioncolis.models.Colis;
import com.gestioncolis.models.Utilisateur;
import com.gestioncolis.services.ColisService;
import com.gestioncolis.services.UtilisateurService;
import com.gestioncolis.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class AjouterColisController {

    @FXML private TextField             tfDescription;
    @FXML private TextField             tfArticles;
    @FXML private TextField             tfDepart;
    @FXML private TextField             tfDestination;
    @FXML private TextField             tfPoids;
    @FXML private TextField             tfDimensions;
    @FXML private ComboBox<Utilisateur> cbDestinataire;
    @FXML private Label                 lblErreur;

    private final ColisService       colisService = new ColisService();
    private final UtilisateurService userService  = new UtilisateurService();

    @FXML
    public void initialize() {
        configurerComboBox();
        chargerClients();
    }

    //drop down liste
    private void configurerComboBox() {
        StringConverter<Utilisateur> converter = new StringConverter<>() {
            @Override public String toString(Utilisateur u) {
                if (u == null) return "";
                String nom = (u.getPrenom() != null ? u.getPrenom() : "")
                        + " " + (u.getNom() != null ? u.getNom() : "");
                return nom.trim() + " — " + u.getEmail();
            }
            @Override public Utilisateur fromString(String s) { return null; }
        };

        cbDestinataire.setConverter(converter);

        cbDestinataire.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Utilisateur u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : converter.toString(u));
            }
        });
    }

    private void chargerClients() {
        try {
            List<Utilisateur> clients = userService.getClients();
            System.out.println("Clients trouvés : " + clients.size());
            clients.forEach(c -> System.out.println("  → " + c.getId() + " | " + c.getEmail() + " | " + c.getRole()));

            if (clients.isEmpty()) {
                lblErreur.setText("Aucun client (ROLE_CLIENT) trouvé en base de données.");
            }
            cbDestinataire.setItems(FXCollections.observableArrayList(clients));
        } catch (SQLException e) {
            lblErreur.setText("Erreur chargement destinataires : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void enregistrer() {
        lblErreur.setText("");

        if (tfDepart.getText().isBlank() || tfDestination.getText().isBlank()
                || tfPoids.getText().isBlank()) {
            lblErreur.setText("Départ, destination et poids sont obligatoires.");
            return;
        }

        if (cbDestinataire.getValue() == null) {
            lblErreur.setText("Veuillez sélectionner un destinataire.");
            return;
        }

        double poids;
        try {
            poids = Double.parseDouble(tfPoids.getText().trim());
        } catch (NumberFormatException e) {
            lblErreur.setText("Le poids doit être un nombre décimal.");
            return;
        }

        int expediteurId   = SessionManager.getInstance().getIdConnecte();
        int destinataireId = cbDestinataire.getValue().getId();

        Colis c = new Colis(
                tfDescription.getText().trim(),
                tfArticles.getText().trim(),
                tfDepart.getText().trim(),
                tfDestination.getText().trim(),
                poids,
                tfDimensions.getText().trim(),
                expediteurId,
                destinataireId
        );

        try {
            colisService.ajouter(c);
            retourListe();
        } catch (Exception e) {
            lblErreur.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void retourListe() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/listeColis.fxml"));
            tfDescription.getScene().setRoot(root);
        } catch (IOException e) {
            lblErreur.setText("Erreur navigation : " + e.getMessage());
        }
    }
}