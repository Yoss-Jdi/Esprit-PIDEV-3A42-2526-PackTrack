package com.gestioncolis.controllers;

import com.gestioncolis.models.Colis;
import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.services.ColisService;
import com.gestioncolis.services.UtilisateursServices;
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

public class AjouterColisController implements DashboardController.UserAware {

    @FXML private TextField             tfDescription;
    @FXML private TextField             tfArticles;
    @FXML private TextField             tfDepart;
    @FXML private TextField             tfDestination;
    @FXML private TextField             tfPoids;
    @FXML private TextField             tfDimensions;
    @FXML private ComboBox<Utilisateurs> cbDestinataire;
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
    private final UtilisateursServices userService  = new UtilisateursServices();

    // ── État interne du ComboBox ──────────────────────────────────────
    private ObservableList<Utilisateurs> tousLesClients;
    private FilteredList<Utilisateurs>   clientsFiltres;
    /** Référence forte vers l'utilisateur sélectionné — ne dépend pas de getValue() */
    private Utilisateurs utilisateurSelectionne = null;
    /** Flag pour ignorer les événements texte déclenchés programmatiquement */
    private boolean miseAJourProgrammatique = false;
    
    // ✅ NOUVEL ATTRIBUT POUR STOCKER L'UTILISATEUR COURANT
    private Utilisateurs currentUser;

    // ── Converter partagé ─────────────────────────────────────────────
    private final StringConverter<Utilisateurs> converter = new StringConverter<>() {
        @Override
        public String toString(Utilisateurs u) {
            if (u == null) return "";
            String nom = (u.getPrenom() != null ? u.getPrenom() : "")
                    + " " + (u.getNom() != null ? u.getNom() : "");
            return nom.trim() + " — " + u.getEmail();
        }
        @Override
        public Utilisateurs fromString(String s) {
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

    /**
     * ✅ IMPLÉMENTATION DE L'INTERFACE UserAware
     * Permet au contrôleur d'être notifié de l'utilisateur courant lors du chargement
     */
    @Override
    public void setCurrentUser(Utilisateurs user) {
        this.currentUser = user;
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
            protected void updateItem(Utilisateurs u, boolean empty) {
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
            List<Utilisateurs> clients = userService.getClients();
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
        int destinataireId = utilisateurSelectionne.getIdUtilisateur();

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
            Utilisateurs u = currentUser != null ? currentUser : SessionManager.getInstance().getUtilisateurConnecte();
            String target = (u != null && u.getRole() == com.gestioncolis.enums.Role.ADMIN)
                    ? "/views/DashboardLayout.fxml"
                    : "/fxml/listeColis.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(target));
            Parent root = loader.load();
            if (u != null) {
                Object ctrl = loader.getController();
                if (ctrl instanceof DashboardController.UserAware ua) {
                    ua.setCurrentUser(u);
                }
            }
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