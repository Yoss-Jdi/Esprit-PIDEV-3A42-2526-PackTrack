package com.gestioncolis.controllers;

import com.gestioncolis.enums.Role;
import com.gestioncolis.models.Livraison;
import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.services.LivraisonService;
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
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ListeLivraisonsController implements DashboardController.UserAware {

    @FXML private TableView<Livraison>           tableLivraisons;
    @FXML private TableColumn<Livraison, String> colDescriptionColis;
    @FXML private TableColumn<Livraison, String> colNomLivreur;
    @FXML private TableColumn<Livraison, String> colStatut;
    @FXML private TableColumn<Livraison, String> colDistance;
    @FXML private TableColumn<Livraison, String> colDuree;
    @FXML private TableColumn<Livraison, String> colTotal;
    @FXML private TableColumn<Livraison, String> colDateDebut;
    @FXML private TableColumn<Livraison, Void>   colActions;
    @FXML private Label                          lblMessage;
    @FXML private Button                         btnAjouter;

    // ── Barre recherche / tri / filtre ────────────────────────────────
    @FXML private TextField        tfRecherche;
    @FXML private ComboBox<String> cbTri;
    @FXML private ComboBox<String> cbFiltreStatut;

    private final LivraisonService service = new LivraisonService();
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ObservableList<Livraison> toutesLesLivraisons = FXCollections.observableArrayList();
    
    // ✅ NOUVEL ATTRIBUT POUR STOCKER L'UTILISATEUR COURANT
    private Utilisateurs currentUser;

    @FXML
    public void initialize() {

        // ── Colonnes texte ────────────────────────────────────────────
        colDescriptionColis.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescriptionColis()));
        colDistance .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDistanceKm() + " km"));
        colDuree    .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDureeFormatee()));
        colTotal    .setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.2f DT", d.getValue().getTotal())));
        colDateDebut.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDateDebut() != null ? d.getValue().getDateDebut().format(FMT) : "—"));

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

        // ── Colonne livreur (admin seulement) ─────────────────────────
        // ✅ UTILISER SOIT currentUser (SI PASSÉ VIA setCurrentUser) SOIT SessionManager (FALLBACK)
        Utilisateurs u = currentUser != null ? currentUser : SessionManager.getInstance().getUtilisateurConnecte();
        boolean estAdmin = (u != null && u.getRole() == Role.ADMIN);
        colNomLivreur.setVisible(estAdmin);
        if (estAdmin) {
            colNomLivreur.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNomLivreur()));
        }
        if (btnAjouter != null) {
            btnAjouter.setVisible(!estAdmin);
            btnAjouter.setManaged(!estAdmin);
        }

        // ── Filtre statut ─────────────────────────────────────────────
        if (cbFiltreStatut != null) {
            cbFiltreStatut.setItems(FXCollections.observableArrayList(
                    "Tous", "🚚 En cours", "✅ Terminé"));
            cbFiltreStatut.setValue("Tous");
            cbFiltreStatut.setOnAction(e -> appliquerFiltreEtTri());
        }

        // ── Tri ───────────────────────────────────────────────────────
        if (cbTri != null) {
            cbTri.setItems(FXCollections.observableArrayList(
                    "Par défaut", "Colis A→Z", "Colis Z→A",
                    "Distance ↑", "Distance ↓",
                    "Total ↑", "Total ↓",
                    "Date ↑", "Date ↓", "Statut"));
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
            chargerDonnees();
        }
    }

    // ─────────────────────────────────────────────────────────────────

    private void chargerDonnees() {
        try {
            // ✅ UTILISER SOIT currentUser (SI PASSÉ VIA setCurrentUser) SOIT SessionManager (FALLBACK)
            Utilisateurs u = currentUser != null ? currentUser : SessionManager.getInstance().getUtilisateurConnecte();
            if (u == null) {
                afficherErreur("Aucun utilisateur connecté");
                return;
            }
            List<Livraison> liste = (u.getRole() == Role.ADMIN)
                    ? service.getAll()
                    : service.getByLivreur(u.getIdUtilisateur());
            toutesLesLivraisons = FXCollections.observableArrayList(liste);
            System.out.println("✅ Données chargées pour " + u.getRole().name() + " (id=" + u.getIdUtilisateur() + "): " + liste.size() + " livraisons");
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
        // Reconfigurer et recharger selon l'utilisateur défini
        if (currentUser != null) {
            chargerDonnees();
        }
    }

    private void appliquerFiltreEtTri() {
        String recherche = tfRecherche    != null ? tfRecherche.getText().trim().toLowerCase() : "";
        String tri       = cbTri          != null ? cbTri.getValue()          : "Par défaut";
        String statut    = cbFiltreStatut != null ? cbFiltreStatut.getValue() : "Tous";

        FilteredList<Livraison> filtered = new FilteredList<>(toutesLesLivraisons, l -> {

            // ── Filtre par statut ──────────────────────────────────────
            if (statut != null && !statut.equals("Tous")) {
                String statutDb = switch (statut) {
                    case "🚚 En cours"  -> "en_cours";
                    case "✅ Terminé"   -> "termine";
                    default            -> "";
                };
                if (!statutDb.equals(l.getStatut())) return false;
            }

            // ── Filtre par texte ──────────────────────────────────────
            if (!recherche.isBlank()) {
                return (l.getDescriptionColis() != null && l.getDescriptionColis().toLowerCase().contains(recherche))
                        || (l.getNomLivreur()       != null && l.getNomLivreur().toLowerCase().contains(recherche));
            }
            return true;
        });

        // ── Tri ────────────────────────────────────────────────────────
        SortedList<Livraison> sorted = new SortedList<>(filtered);
        if (tri != null) switch (tri) {
            case "Colis A→Z"  -> sorted.setComparator((a, b) ->
                    nvl(a.getDescriptionColis()).compareToIgnoreCase(nvl(b.getDescriptionColis())));
            case "Colis Z→A"  -> sorted.setComparator((a, b) ->
                    nvl(b.getDescriptionColis()).compareToIgnoreCase(nvl(a.getDescriptionColis())));
            case "Distance ↑" -> sorted.setComparator((a, b) -> Double.compare(a.getDistanceKm(), b.getDistanceKm()));
            case "Distance ↓" -> sorted.setComparator((a, b) -> Double.compare(b.getDistanceKm(), a.getDistanceKm()));
            case "Total ↑"    -> sorted.setComparator((a, b) -> Double.compare(a.getTotal(), b.getTotal()));
            case "Total ↓"    -> sorted.setComparator((a, b) -> Double.compare(b.getTotal(), a.getTotal()));
            case "Date ↑"     -> sorted.setComparator((a, b) -> {
                if (a.getDateDebut() == null) return 1;
                if (b.getDateDebut() == null) return -1;
                return a.getDateDebut().compareTo(b.getDateDebut());
            });
            case "Date ↓"     -> sorted.setComparator((a, b) -> {
                if (a.getDateDebut() == null) return 1;
                if (b.getDateDebut() == null) return -1;
                return b.getDateDebut().compareTo(a.getDateDebut());
            });
            case "Statut"     -> sorted.setComparator((a, b) ->
                    nvl(a.getStatut()).compareToIgnoreCase(nvl(b.getStatut())));
            default -> sorted.setComparator(null);
        }

        tableLivraisons.setItems(sorted);
    }

    // ─────────────────────────────────────────────────────────────────

    private void ajouterColonneActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnQr        = new Button("QR");
            private final Button btnTerminer  = new Button("✅");
            private final Button btnSupprimer = new Button("🗑️");
            private final HBox   box          = new HBox(4, btnQr, btnTerminer, btnSupprimer);

            {
                btnQr.setStyle(
                        "-fx-background-color: #8e44ad; -fx-text-fill: white;" +
                                " -fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8;");
                btnTerminer.setStyle(
                        "-fx-background-color: #27ae60; -fx-text-fill: white;" +
                                " -fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8;");
                btnSupprimer.setStyle(
                        "-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                                " -fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8;");

                btnQr       .setTooltip(new Tooltip("Afficher le QR Code"));
                btnTerminer .setTooltip(new Tooltip("Terminer la livraison"));
                btnSupprimer.setTooltip(new Tooltip("Supprimer"));

                btnQr.setOnAction(e ->
                        QrCodeHelper.afficherQrLivraison(getTableView().getItems().get(getIndex())));
                btnTerminer.setOnAction(e ->
                        confirmerTerminer(getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(e ->
                        confirmerSuppression(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Livraison liv = getTableView().getItems().get(getIndex());
                boolean termine = "termine".equals(liv.getStatut());
                btnTerminer.setDisable(termine);
                btnTerminer.setOpacity(termine ? 0.4 : 1.0);
                if (termine) {
                    btnTerminer.setTooltip(new Tooltip("Livraison déjà terminée"));
                } else {
                    btnTerminer.setTooltip(new Tooltip("Terminer la livraison"));
                }
                setGraphic(box);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────

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

    @FXML public void ouvrirAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ajouterLivraison.fxml"));
            Parent root = loader.load();
            AjouterLivraisonController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser != null ? currentUser : SessionManager.getInstance().getUtilisateurConnecte());
            tableLivraisons.getScene().setRoot(root);
        } catch (IOException e) { afficherErreur("Erreur navigation : " + e.getMessage()); }
    }

    @FXML public void retourDashboard() {
        try {
            Utilisateurs u = currentUser != null ? currentUser : SessionManager.getInstance().getUtilisateurConnecte();
            String target = (u != null && u.getRole() == Role.ADMIN)
                    ? "/views/DashboardLayout.fxml"
                    : "/views/UserHomeView.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(target));
            Parent root = loader.load();
            if (u != null) {
                Object ctrl = loader.getController();
                if (ctrl instanceof DashboardController.UserAware ua) {
                    ua.setCurrentUser(u);
                }
            }
            tableLivraisons.getScene().setRoot(root);
        } catch (IOException e) { afficherErreur("Erreur navigation : " + e.getMessage()); }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private String formatStatut(String s) {
        return switch (s == null ? "" : s) {
            case "en_cours" -> "🚚 En cours";
            case "termine"  -> "✅ Terminé";
            default         -> s;
        };
    }

    private String styleStatut(String s) {
        return switch (s == null ? "" : s) {
            case "en_cours" -> "-fx-text-fill: #2980b9;";
            case "termine"  -> "-fx-text-fill: #27ae60;";
            default         -> "-fx-text-fill: #555;";
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