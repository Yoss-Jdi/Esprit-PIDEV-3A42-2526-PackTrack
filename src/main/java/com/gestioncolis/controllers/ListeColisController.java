package com.gestioncolis.controllers;

import com.gestioncolis.enums.Role;
import com.gestioncolis.models.Colis;
import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.services.ColisService;
import com.gestioncolis.utils.QrCodeHelper;
import com.gestioncolis.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ListeColisController implements DashboardController.UserAware {

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

    // ── Barre recherche / tri / filtre ────────────────────────────────
    @FXML private TextField        tfRecherche;
    @FXML private ComboBox<String> cbTri;
    @FXML private ComboBox<String> cbFiltreStatut;

    private final ColisService service = new ColisService();
    private ObservableList<Colis> tousLesColis = FXCollections.observableArrayList();
    
    // ✅ NOUVEL ATTRIBUT POUR STOCKER L'UTILISATEUR COURANT
    private Utilisateurs currentUser;

    @FXML
    public void initialize() {

        // ── Colonnes texte ────────────────────────────────────────────
        colDescription.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        colDepart     .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAdresseDepart()));
        colDestination.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAdresseDestination()));
        colPoids      .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPoids() + " kg"));
        colMontant    .setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.2f DT", d.getValue().calculerMontant())));

        // ── Colonne statut avec badge coloré ──────────────────────────
        colStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut()));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(formatStatut(item));
                setStyle(styleStatut(item) + "-fx-font-weight: bold; -fx-font-size: 12px;");
            }
        });

        // ── Filtre statut ─────────────────────────────────────────────
        if (cbFiltreStatut != null) {
            cbFiltreStatut.setItems(FXCollections.observableArrayList(
                    "Tous", "⏳ En attente", "🚚 En cours", "✅ Livré"));
            cbFiltreStatut.setValue("Tous");
            cbFiltreStatut.setOnAction(e -> appliquerFiltreEtTri());
        }

        // ── Tri ───────────────────────────────────────────────────────
        if (cbTri != null) {
            cbTri.setItems(FXCollections.observableArrayList(
                    "Par défaut", "Description A→Z", "Description Z→A",
                    "Poids ↑", "Poids ↓", "Montant ↑", "Montant ↓", "Statut"));
            cbTri.setValue("Par défaut");
            cbTri.setOnAction(e -> appliquerFiltreEtTri());
        }

        // ── Recherche texte ───────────────────────────────────────────
        if (tfRecherche != null) {
            tfRecherche.textProperty().addListener((obs, old, val) -> appliquerFiltreEtTri());
        }

        ajouterColonneActions();
        // ✅ NE PAS appeler chargerDonnees() ici - sera appelé dans setCurrentUser()
        // Pour les appels directs sans setCurrentUser(), charger avec fallback
        if (currentUser == null) {
            configurerSelonRole();
            chargerDonnees();
        }
    }

    // ─────────────────────────────────────────────────────────────────

    private void configurerSelonRole() {
        // ✅ UTILISER SOIT currentUser (SI PASSÉ VIA setCurrentUser) SOIT SessionManager (FALLBACK)
        Utilisateurs u = currentUser != null ? currentUser : SessionManager.getInstance().getUtilisateurConnecte();
        if (u == null) return;
        
        boolean estClient = (u.getRole() == Role.CLIENT);
        boolean estAdmin = (u.getRole() == Role.ADMIN);
        if (btnAjouter != null) {
            btnAjouter.setVisible(!(estClient || estAdmin));
            btnAjouter.setManaged(!(estClient || estAdmin));
        }
        colActions.setVisible(!estClient);
    }

    private void chargerDonnees() {
        try {
            // ✅ UTILISER SOIT currentUser (SI PASSÉ VIA setCurrentUser) SOIT SessionManager (FALLBACK)
            Utilisateurs u = currentUser != null ? currentUser : SessionManager.getInstance().getUtilisateurConnecte();
            if (u == null) {
                afficherErreur("Aucun utilisateur connecté");
                return;
            }
            List<Colis> liste;
            switch (u.getRole()) {
                case ADMIN      -> liste = service.getAll();
                case CLIENT     -> liste = service.getByDestinataire(u.getIdUtilisateur());
                case ENTREPRISE -> liste = service.getByExpediteur(u.getIdUtilisateur());
                default         -> liste = new ArrayList<>();
            }
            tousLesColis = FXCollections.observableArrayList(liste);
            System.out.println("✅ Données chargées pour " + u.getRole().name() + " (id=" + u.getIdUtilisateur() + "): " + liste.size() + " colis");
            appliquerFiltreEtTri();
        } catch (SQLException e) {
            afficherErreur("Erreur chargement : " + e.getMessage());
            System.err.println("❌ Erreur SQL: " + e);
            e.printStackTrace();
        }
    }

    /**
     * ✅ IMPLÉMENTATION DE L'INTERFACE UserAware
     * Permet au contrôleur d'être notifié de l'utilisateur courant lors du chargement
     */
    @Override
    public void setCurrentUser(Utilisateurs user) {
        this.currentUser = user;
        // ✅ Sauvegarder en session
        if (user != null) {
            SessionManager.getInstance().setUtilisateurConnecte(user);
            // Reconfigurer et recharger selon l'utilisateur défini
            configurerSelonRole();
            chargerDonnees();
        }
    }

    private void appliquerFiltreEtTri() {
        String recherche = tfRecherche    != null ? tfRecherche.getText().trim().toLowerCase() : "";
        String tri       = cbTri          != null ? cbTri.getValue()          : "Par défaut";
        String statut    = cbFiltreStatut != null ? cbFiltreStatut.getValue() : "Tous";

        FilteredList<Colis> filtered = new FilteredList<>(tousLesColis, c -> {

            // ── Filtre par statut ──────────────────────────────────────
            if (statut != null && !statut.equals("Tous")) {
                String statutDb = switch (statut) {
                    case "⏳ En attente" -> "en_attente";
                    case "🚚 En cours"   -> "en_cours";
                    case "✅ Livré"       -> "livre";
                    default              -> "";
                };
                if (!statutDb.equals(c.getStatut())) return false;
            }

            // ── Filtre par texte ──────────────────────────────────────
            if (!recherche.isBlank()) {
                return (c.getDescription()       != null && c.getDescription().toLowerCase().contains(recherche))
                        || (c.getAdresseDepart()      != null && c.getAdresseDepart().toLowerCase().contains(recherche))
                        || (c.getAdresseDestination() != null && c.getAdresseDestination().toLowerCase().contains(recherche));
            }
            return true;
        });

        // ── Tri ────────────────────────────────────────────────────────
        SortedList<Colis> sorted = new SortedList<>(filtered);
        if (tri != null) switch (tri) {
            case "Description A→Z" -> sorted.setComparator((a, b) ->
                    nvl(a.getDescription()).compareToIgnoreCase(nvl(b.getDescription())));
            case "Description Z→A" -> sorted.setComparator((a, b) ->
                    nvl(b.getDescription()).compareToIgnoreCase(nvl(a.getDescription())));
            case "Poids ↑"   -> sorted.setComparator((a, b) -> Double.compare(a.getPoids(), b.getPoids()));
            case "Poids ↓"   -> sorted.setComparator((a, b) -> Double.compare(b.getPoids(), a.getPoids()));
            case "Montant ↑" -> sorted.setComparator((a, b) -> Double.compare(a.calculerMontant(), b.calculerMontant()));
            case "Montant ↓" -> sorted.setComparator((a, b) -> Double.compare(b.calculerMontant(), a.calculerMontant()));
            case "Statut"    -> sorted.setComparator((a, b) ->
                    nvl(a.getStatut()).compareToIgnoreCase(nvl(b.getStatut())));
            default -> sorted.setComparator(null);
        }

        tableColis.setItems(sorted);
    }

    // ─────────────────────────────────────────────────────────────────

    private void ajouterColonneActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnQr        = new Button("QR");
            private final Button btnModifier  = new Button("✏️");
            private final Button btnSupprimer = new Button("🗑️");
            private final HBox   box          = new HBox(4, btnQr, btnModifier, btnSupprimer);

            {
                btnQr.setStyle(
                        "-fx-background-color: #8e44ad; -fx-text-fill: white;" +
                                " -fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8;");
                btnModifier.setStyle(
                        "-fx-background-color: #2980b9; -fx-text-fill: white;" +
                                " -fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8;");
                btnSupprimer.setStyle(
                        "-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                                " -fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8;");

                btnQr       .setTooltip(new Tooltip("Afficher le QR Code"));
                btnModifier .setTooltip(new Tooltip("Modifier"));
                btnSupprimer.setTooltip(new Tooltip("Supprimer"));

                btnQr.setOnAction(e ->
                        QrCodeHelper.afficherQrColis(getTableView().getItems().get(getIndex())));
                btnModifier.setOnAction(e ->
                        ouvrirModifier(getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(e ->
                        confirmerSuppression(getTableView().getItems().get(getIndex())));
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
                    String raison = "Colis « " + formatStatut(colis.getStatut()) + " » : action non disponible";
                    btnModifier .setTooltip(new Tooltip(raison));
                    btnSupprimer.setTooltip(new Tooltip(raison));
                } else {
                    btnModifier .setTooltip(new Tooltip("Modifier"));
                    btnSupprimer.setTooltip(new Tooltip("Supprimer"));
                }
                setGraphic(box);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────

    private void ouvrirModifier(Colis colis) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/modifierColis.fxml"));
            Parent root = loader.load();
            ModifierColisController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser != null ? currentUser : SessionManager.getInstance().getUtilisateurConnecte());
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

    @FXML public void ouvrirAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ajouterColis.fxml"));
            Parent root = loader.load();
            AjouterColisController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser != null ? currentUser : SessionManager.getInstance().getUtilisateurConnecte());
            tableColis.getScene().setRoot(root);
        } catch (IOException e) { afficherErreur("Erreur navigation : " + e.getMessage()); }
    }

    @FXML public void retourDashboard() {
        try {
            Utilisateurs u = currentUser != null ? currentUser : SessionManager.getInstance().getUtilisateurConnecte();
            if (u == null) {
                afficherErreur("Erreur : Utilisateur non trouvé");
                return;
            }
            
            // ✅ Sauvegarder l'utilisateur en session
            SessionManager.getInstance().setUtilisateurConnecte(u);
            
            String target = (u.getRole() == Role.ADMIN)
                    ? "/views/DashboardLayout.fxml"
                    : "/views/UserHomeView.fxml";
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(target));
            Parent root = loader.load();
            
            // ✅ Passer l'utilisateur au nouveau contrôleur
            Object ctrl = loader.getController();
            if (ctrl instanceof DashboardController.UserAware ua) {
                ua.setCurrentUser(u);
            } else if (ctrl instanceof UserHomeController uh) {
                uh.setCurrentUser(u);
            }
            
            // ✅ Obtenir le Stage et changer la scène
            Scene currentScene = tableColis.getScene();
            Stage stage = (Stage) currentScene.getWindow();
            Scene newScene = new Scene(root, 1280, 760);
            
            stage.setScene(newScene);
            stage.setMinWidth(1024);
            stage.setMinHeight(700);
        } catch (IOException e) { 
            afficherErreur("Erreur navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private String formatStatut(String s) {
        return switch (s == null ? "" : s) {
            case "en_attente" -> "⏳ En attente";
            case "en_cours"   -> "🚚 En cours";
            case "livre"      -> "✅ Livré";
            default           -> s;
        };
    }

    private String styleStatut(String s) {
        return switch (s == null ? "" : s) {
            case "en_attente" -> "-fx-text-fill: #e67e22;";
            case "en_cours"   -> "-fx-text-fill: #2980b9;";
            case "livre"      -> "-fx-text-fill: #27ae60;";
            default           -> "-fx-text-fill: #555;";
        };
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private void afficherSucces(String msg) {
        lblMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
        lblMessage.setText(msg);
    }
    private void afficherErreur(String msg) {
        lblMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        lblMessage.setText(msg);
    }
}

