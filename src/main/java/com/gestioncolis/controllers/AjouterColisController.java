package com.gestioncolis.controllers;

import com.gestioncolis.models.Colis;
import com.gestioncolis.models.Utilisateur;
import com.gestioncolis.services.ColisService;
import com.gestioncolis.services.UtilisateurService;
import com.gestioncolis.utils.MapHelper;
import com.gestioncolis.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
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
    @FXML private Button                btnMapDepart;
    @FXML private Button                btnMapDestination;

    // ── Labels d'erreur individuels (un par champ) ────────────────────
    @FXML private Label lblErrDescription;
    @FXML private Label lblErrDepart;
    @FXML private Label lblErrDestination;
    @FXML private Label lblErrPoids;
    @FXML private Label lblErrDestinataire;
    @FXML private Label lblErrGlobal;

    private final ColisService       colisService = new ColisService();
    private final UtilisateurService userService  = new UtilisateurService();

    // ── État interne du ComboBox ──────────────────────────────────────
    private ObservableList<Utilisateur> tousLesClients;
    private FilteredList<Utilisateur>   clientsFiltres;
    /** Référence forte vers l'utilisateur sélectionné — ne dépend pas de getValue() */
    private Utilisateur utilisateurSelectionne = null;
    /** Flag pour ignorer les événements texte déclenchés programmatiquement */
    private boolean miseAJourProgrammatique = false;

    // ── Converter partagé ─────────────────────────────────────────────
    private final StringConverter<Utilisateur> converter = new StringConverter<>() {
        @Override
        public String toString(Utilisateur u) {
            if (u == null) return "";
            String nom = (u.getPrenom() != null ? u.getPrenom() : "")
                    + " " + (u.getNom() != null ? u.getNom() : "");
            return nom.trim() + " — " + u.getEmail();
        }
        @Override
        public Utilisateur fromString(String s) {
            if (s == null || tousLesClients == null) return null;
            return tousLesClients.stream()
                    .filter(u -> converter.toString(u).equals(s))
                    .findFirst().orElse(null);
        }
    };

    @FXML
    public void initialize() {
        configurerComboBoxAvecRecherche();
        chargerClients();
        configurerBoutonsMap();
    }

    // ─────────────────────────────────────────────────────────────────
    // Boutons carte — délégation à MapHelper
    // ─────────────────────────────────────────────────────────────────

    private void configurerBoutonsMap() {
        if (btnMapDepart != null) {
            btnMapDepart.setOnAction(e -> {
                effacerErreur(lblErrDepart);
                MapHelper.ouvrirCarte("📍 Choisir l'adresse de départ", tfDepart);
            });
        }
        if (btnMapDestination != null) {
            btnMapDestination.setOnAction(e -> {
                effacerErreur(lblErrDestination);
                MapHelper.ouvrirCarte("🏁 Choisir l'adresse de destination", tfDestination);
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ComboBox avec autocomplete
    // ─────────────────────────────────────────────────────────────────

    private void configurerComboBoxAvecRecherche() {
        cbDestinataire.setEditable(true);
        cbDestinataire.setConverter(converter);

        cbDestinataire.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Utilisateur u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) { setText(null); setStyle(""); return; }
                String nom = (u.getPrenom() != null ? u.getPrenom() : "")
                        + " " + (u.getNom() != null ? u.getNom() : "");
                setText(nom.trim() + " — " + u.getEmail());
                setStyle("-fx-padding: 8 12; -fx-font-size: 13px;");
            }
        });

        cbDestinataire.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (miseAJourProgrammatique) return;
            if (clientsFiltres == null) return;

            utilisateurSelectionne = null;
            effacerErreur(lblErrDestinataire);

            String filtre = newVal == null ? "" : newVal.toLowerCase().trim();
            clientsFiltres.setPredicate(u -> {
                if (filtre.isEmpty()) return true;
                String nom   = ((u.getPrenom() != null ? u.getPrenom() : "")
                        + " " + (u.getNom() != null ? u.getNom() : "")).toLowerCase();
                String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                return nom.contains(filtre) || email.contains(filtre);
            });

            if (!filtre.isEmpty() && !cbDestinataire.isShowing()) {
                cbDestinataire.show();
            } else if (filtre.isEmpty()) {
                cbDestinataire.hide();
            }
        });

        cbDestinataire.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                utilisateurSelectionne = newVal;
                effacerErreur(lblErrDestinataire);

                miseAJourProgrammatique = true;
                Platform.runLater(() -> {
                    cbDestinataire.getEditor().setText(converter.toString(newVal));
                    cbDestinataire.hide();
                    miseAJourProgrammatique = false;
                });
            }
        });
    }

    private void chargerClients() {
        try {
            List<Utilisateur> clients = userService.getClients();
            if (clients.isEmpty()) {
                afficherErreur(lblErrGlobal, "Aucun client (ROLE_CLIENT) trouvé en base.");
            }
            tousLesClients = FXCollections.observableArrayList(clients);
            clientsFiltres = new FilteredList<>(tousLesClients, u -> true);
            cbDestinataire.setItems(clientsFiltres);
        } catch (SQLException e) {
            afficherErreur(lblErrGlobal, "Erreur chargement destinataires : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Validation et enregistrement
    // ─────────────────────────────────────────────────────────────────

    @FXML
    public void enregistrer() {
        effacerToutesLesErreurs();

        boolean valide = true;

        String desc = tfDescription.getText().trim();
        if (!desc.isBlank() && desc.length() < 5) {
            afficherErreur(lblErrDescription, "Minimum 5 caractères.");
            valide = false;
        }

        if (tfDepart.getText().isBlank()) {
            afficherErreur(lblErrDepart, "L'adresse de départ est obligatoire.");
            valide = false;
        }

        if (tfDestination.getText().isBlank()) {
            afficherErreur(lblErrDestination, "L'adresse de destination est obligatoire.");
            valide = false;
        }

        double poids = 0;
        if (tfPoids.getText().isBlank()) {
            afficherErreur(lblErrPoids, "Le poids est obligatoire.");
            valide = false;
        } else {
            try {
                poids = Double.parseDouble(tfPoids.getText().trim());
                if (poids <= 0) {
                    afficherErreur(lblErrPoids, "Le poids doit être positif.");
                    valide = false;
                } else if (poids >= 1000) {
                    afficherErreur(lblErrPoids, "Le poids ne peut pas dépasser 1000 kg.");
                    valide = false;
                }
            } catch (NumberFormatException e) {
                afficherErreur(lblErrPoids, "Le poids doit être un nombre décimal (ex : 2.5).");
                valide = false;
            }
        }

        if (utilisateurSelectionne == null) {
            afficherErreur(lblErrDestinataire, "Veuillez sélectionner un destinataire dans la liste.");
            valide = false;
        }

        if (!valide) return;

        int expediteurId   = SessionManager.getInstance().getIdConnecte();
        int destinataireId = utilisateurSelectionne.getId();

        Colis c = new Colis(
                desc,
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
            afficherErreur(lblErrGlobal, "Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void retourListe() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/listeColis.fxml"));
            tfDescription.getScene().setRoot(root);
        } catch (IOException e) {
            afficherErreur(lblErrGlobal, "Erreur navigation : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers affichage erreurs
    // ─────────────────────────────────────────────────────────────────

    private void afficherErreur(Label lbl, String msg) {
        if (lbl == null) return;
        lbl.setText("⚠ " + msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    private void effacerErreur(Label lbl) {
        if (lbl == null) return;
        lbl.setText("");
        lbl.setVisible(false);
        lbl.setManaged(false);
    }

    private void effacerToutesLesErreurs() {
        effacerErreur(lblErrDescription);
        effacerErreur(lblErrDepart);
        effacerErreur(lblErrDestination);
        effacerErreur(lblErrPoids);
        effacerErreur(lblErrDestinataire);
        effacerErreur(lblErrGlobal);
    }
}