package com.gestioncolis.controllers;

import com.gestioncolis.models.Colis;
import com.gestioncolis.models.Livraison;
import com.gestioncolis.services.*;
import com.gestioncolis.utils.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class AjouterLivraisonController implements DashboardController.UserAware {

    // ── Champs FXML ───────────────────────────────────────────────────
    @FXML private Label  lblLivreur;
    @FXML private Label  lblErreur;
    @FXML private Label  lblSelectionInfo;
    @FXML private Label  lblIaStatut;
    @FXML private Label  lblIaDistance;
    @FXML private Label  lblIaDuree;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnLocalisation;
    @FXML private Label  lblIaMeteo;
    @FXML private Label  lblIaTrafic;

    @FXML private TableView<ColisAvecDistance>           tableColisDisponibles;
    @FXML private TableColumn<ColisAvecDistance, String> colCProximite;
    @FXML private TableColumn<ColisAvecDistance, String> colCDesc;
    @FXML private TableColumn<ColisAvecDistance, String> colCDep;
    @FXML private TableColumn<ColisAvecDistance, String> colCDest;
    @FXML private TableColumn<ColisAvecDistance, String> colCPoid;
    @FXML private TableColumn<ColisAvecDistance, String> colCMontant;

    // ── Services ──────────────────────────────────────────────────────
    private final LivraisonService       livraisonService  = new LivraisonService();
    private final ColisService           colisService      = new ColisService();
    private final PredictionService      predictionService = new PredictionService();
    private final GeolocalisationService geoService        = new GeolocalisationService();

    // ── État interne ──────────────────────────────────────────────────
    private PredictionResult dernierePrediction = null;
    private Task<PredictionResult> taskCourante = null;
    private double[] localisationLivreur = null; // [lat, lon]
    private List<Colis> colisDisponiblesBruts = new ArrayList<>();
    
    // ✅ NOUVEL ATTRIBUT POUR STOCKER L'UTILISATEUR COURANT
    private com.gestioncolis.entities.Utilisateurs currentUser;

    // ─────────────────────────────────────────────────────────────────
    // Initialisation
    // ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Nom du livreur connecté
        if (SessionManager.getInstance().isConnecte()) {
            lblLivreur.setText(
                    SessionManager.getInstance().getDisplayName());
        } else {
            lblLivreur.setText("(mode dev)");
        }

        // Colonnes tableau
        colCProximite.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDistanceFormatee()));
        colCDesc.setCellValueFactory(d -> new SimpleStringProperty(
                nvl(d.getValue().getColis().getDescription(), "—")));
        colCDep.setCellValueFactory(d -> new SimpleStringProperty(
                nvl(d.getValue().getColis().getAdresseDepart(), "—")));
        colCDest.setCellValueFactory(d -> new SimpleStringProperty(
                nvl(d.getValue().getColis().getAdresseDestination(), "—")));
        colCPoid.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getColis().getPoids() + " kg"));
        colCMontant.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.2f DT", d.getValue().getColis().calculerMontant())));

        // Style colonne proximité
        colCProximite.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("?")) {
                        setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 12px;");
                    } else if (item.contains("m") || item.startsWith("0.") || item.startsWith("1.")) {
                        // Très proche (< 2 km)
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 12px;");
                    } else {
                        setStyle("-fx-text-fill: #2980b9; -fx-font-size: 12px;");
                    }
                }
            }
        });

        // Surbrillance ligne sélectionnée
        tableColisDisponibles.setRowFactory(tv -> {
            TableRow<ColisAvecDistance> row = new TableRow<>();
            row.selectedProperty().addListener((obs, was, isNow) ->
                    row.setStyle(isNow ? "-fx-background-color: #d6eaf8;" : ""));
            return row;
        });

        // ── Listener sélection → déclenche la prédiction ML ──────────
        tableColisDisponibles.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> {
                    if (sel != null) {
                        afficherResumeColis(sel.getColis());
                        lancerPrediction(sel.getColis());
                    } else {
                        lblSelectionInfo.setText("");
                        reinitialiserBandeauIA();
                    }
                });

        // Vérification santé ML (non bloquante)
        verifierServiceML();

        // Charger les colis
        chargerColisDisponibles();

        // Proposer la géolocalisation immédiatement
        Platform.runLater(this::proposerGeolocalisation);
    }

    /**
     * ✅ IMPLÉMENTATION DE L'INTERFACE UserAware
     * Permet au contrôleur d'être notifié de l'utilisateur courant lors du chargement
     */
    @Override
    public void setCurrentUser(com.gestioncolis.entities.Utilisateurs user) {
        this.currentUser = user;
        // Mettre à jour le label du livreur si fourni
        if (lblLivreur != null && user != null) {
            lblLivreur.setText(user.getPrenom() + " " + user.getNom());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Géolocalisation du livreur
    // ─────────────────────────────────────────────────────────────────

    private void proposerGeolocalisation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Optimisation des colis");
        alert.setHeaderText("📍 Activer le tri intelligent ?");
        alert.setContentText("Définissez votre position actuelle pour afficher les colis " +
                "les plus proches de vous en premier.\n\nVous pouvez aussi le faire plus tard " +
                "via le bouton « Ma position ».");

        ButtonType btnOui = new ButtonType("Définir ma position", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNon = new ButtonType("Plus tard", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnOui, btnNon);

        alert.showAndWait().ifPresent(response -> {
            if (response == btnOui) {
                definirLocalisation();
            }
        });
    }

    @FXML
    public void definirLocalisation() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Définir ma position");
        dialog.setWidth(650);
        dialog.setHeight(500);

        Label lblInfo = new Label("📍 Cliquez sur la carte à votre position actuelle :");
        lblInfo.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label lblCoords = new Label("Position non définie");
        lblCoords.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        WebView webView = new WebView();
        webView.setPrefHeight(350);

        final String[] adresseSelectionnee = {null};
        final double[][] coordsSelectionnees = {null};

        // HTML Leaflet avec clic pour définir la position
        String html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body { margin: 0; padding: 0; }
                #map { width: 100%%; height: 100vh; }
                .leaflet-popup-content { font-size: 12px; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map').setView([36.8190, 10.1658], 12);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '© OpenStreetMap'
                }).addTo(map);
                
                var marker = null;
                
                map.on('click', function(e) {
                    var lat = e.latlng.lat.toFixed(5);
                    var lng = e.latlng.lng.toFixed(5);
                    
                    if (marker) map.removeLayer(marker);
                    marker = L.marker([lat, lng]).addTo(map);
                    marker.bindPopup("📍 Ma position").openPopup();
                    
                    // Géocodage inversé
                    fetch('https://nominatim.openstreetmap.org/reverse?format=json&lat=' + lat + '&lon=' + lng + '&accept-language=fr')
                        .then(r => r.json())
                        .then(data => {
                            var adresse = data.display_name || (lat + ", " + lng);
                            // Appel vers Java
                            if (window.bridge) {
                                window.bridge.setPosition(lat, lng, adresse);
                            }
                        })
                        .catch(() => {
                            if (window.bridge) {
                                window.bridge.setPosition(lat, lng, lat + ", " + lng);
                            }
                        });
                });
            </script>
        </body>
        </html>
        """;

        webView.getEngine().loadContent(html);

        // Bridge JavaScript → Java
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                webView.getEngine().executeScript("window.bridge = " +
                        "{ setPosition: function(lat, lon, adresse) { " +
                        "   console.log('Position: ' + lat + ', ' + lon + ' - ' + adresse); " +
                        "}}");
            }
        });

        // Intercepter les logs console pour récupérer la position
        webView.getEngine().setOnAlert(event -> {
            String msg = event.getData();
            if (msg.startsWith("Position: ")) {
                try {
                    String[] parts = msg.substring(10).split(" - ");
                    String[] coords = parts[0].split(", ");
                    double lat = Double.parseDouble(coords[0]);
                    double lon = Double.parseDouble(coords[1]);
                    String adresse = parts.length > 1 ? parts[1] : (lat + ", " + lon);

                    coordsSelectionnees[0] = new double[]{lat, lon};
                    adresseSelectionnee[0] = adresse;
                    lblCoords.setText("📍 " + adresse);
                    lblCoords.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } catch (Exception ignored) {}
            }
        });

        // Alternative : TextField pour saisie manuelle
        TextField tfAdresse = new TextField();
        tfAdresse.setPromptText("Ou saisissez votre adresse ici...");
        tfAdresse.setStyle("-fx-pref-height: 35px; -fx-font-size: 12px;");

        Button btnGeocode = new Button("Localiser cette adresse");
        btnGeocode.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 6 12;");
        btnGeocode.setOnAction(e -> {
            String adr = tfAdresse.getText().trim();
            if (!adr.isEmpty()) {
                double[] coords = geoService.geocoder(adr);
                if (coords != null) {
                    coordsSelectionnees[0] = coords;
                    adresseSelectionnee[0] = adr;
                    lblCoords.setText("📍 " + adr);
                    lblCoords.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    // Centrer la carte
                    webView.getEngine().executeScript(
                            "map.setView([" + coords[0] + ", " + coords[1] + "], 15);");
                } else {
                    lblCoords.setText("⚠ Adresse introuvable");
                    lblCoords.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c;");
                }
            }
        });

        HBox saisie = new HBox(8, tfAdresse, btnGeocode);
        HBox.setHgrow(tfAdresse, javafx.scene.layout.Priority.ALWAYS);

        Button btnValider = new Button("✅  Valider et trier les colis");
        btnValider.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 10 20;");
        btnValider.setOnAction(e -> {
            if (coordsSelectionnees[0] != null) {
                localisationLivreur = coordsSelectionnees[0];
                trierColisParProximite();
                lblErreur.setText("✅ Colis triés par proximité : " + adresseSelectionnee[0]);
                lblErreur.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
                dialog.close();
            } else {
                lblCoords.setText("⚠ Veuillez cliquer sur la carte ou saisir une adresse");
                lblCoords.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c;");
            }
        });

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: transparent; -fx-border-color: #ccc; " +
                "-fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 12px; " +
                "-fx-cursor: hand; -fx-padding: 8 16;");
        btnAnnuler.setOnAction(e -> dialog.close());

        HBox boutons = new HBox(12, btnValider, btnAnnuler);

        VBox layout = new VBox(12, lblInfo, webView, lblCoords,
                new Label("Saisie manuelle :") {{
                    setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
                }},
                saisie, boutons);
        layout.setPadding(new Insets(16));
        VBox.setVgrow(webView, javafx.scene.layout.Priority.ALWAYS);

        dialog.setScene(new Scene(layout));
        dialog.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────
    // Chargement et tri des colis
    // ─────────────────────────────────────────────────────────────────

    private void chargerColisDisponibles() {
        try {
            colisDisponiblesBruts = colisService.getColisDisponibles();
            trierColisParProximite();
        } catch (SQLException e) {
            lblErreur.setText("Erreur chargement colis : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void trierColisParProximite() {
        List<ColisAvecDistance> colisAvecDist = new ArrayList<>();

        for (Colis colis : colisDisponiblesBruts) {
            Double distance = null;
            if (localisationLivreur != null) {
                distance = geoService.calculerDistanceVersAdresse(
                        localisationLivreur,
                        colis.getAdresseDepart());
            }
            colisAvecDist.add(new ColisAvecDistance(colis, distance));
        }

        // Tri : plus proches en premier
        Collections.sort(colisAvecDist);

        tableColisDisponibles.setItems(FXCollections.observableArrayList(colisAvecDist));
    }

    // ─────────────────────────────────────────────────────────────────
    // Prédiction ML (inchangé)
    // ─────────────────────────────────────────────────────────────────

    private void afficherResumeColis(Colis sel) {
        lblSelectionInfo.setText(
                nvl(sel.getDescription(), "colis")
                        + "  ·  " + sel.getAdresseDepart()
                        + " → " + sel.getAdresseDestination()
                        + "  ·  " + sel.getPoids() + " kg"
                        + "  ·  " + String.format("%.2f DT", sel.calculerMontant()));
        lblErreur.setText("");
    }

    private void verifierServiceML() {
        Thread t = new Thread(() -> {
            boolean dispo = predictionService.estDisponible();
            if (!dispo) {
                Platform.runLater(() -> setStatutIA(
                        "⚠ Service ML hors ligne — estimations indisponibles", "#e67e22"));
            }
        }, "ml-health");
        t.setDaemon(true);
        t.start();
    }

    private void lancerPrediction(Colis colis) {
        if (taskCourante != null && taskCourante.isRunning()) {
            taskCourante.cancel(true);
        }

        dernierePrediction = null;
        if (btnEnregistrer != null) btnEnregistrer.setDisable(true);
        setStatutIA("🔄 Analyse IA en cours (météo + trafic)…", "#2980b9");
        lblIaDistance.setText("");
        lblIaDuree.setText("");

        LocalDateTime maintenant = LocalDateTime.now();

        taskCourante = new Task<>() {
            @Override
            protected PredictionResult call() {
                // Obtenir les coordonnées de départ pour météo/trafic
                GeolocalisationService geoService = new GeolocalisationService();
                double[] coordsDep = geoService.geocoder(colis.getAdresseDepart());

                if (coordsDep == null) {
                    // Fallback sans contexte
                    return predictionService.predire(
                            colis.getAdresseDepart(),
                            colis.getAdresseDestination(),
                            colis.getPoids(),
                            maintenant);
                }

                // Récupérer météo et trafic
                MeteoService meteoService = new MeteoService();
                TraficService traficService = new TraficService();

                MeteoService.MeteoResult meteo = meteoService.obtenirMeteo(
                        coordsDep[0], coordsDep[1]);
                TraficService.TraficResult trafic = traficService.analyserTrafic(
                        coordsDep[0], coordsDep[1]);

                // Conversion conditions météo en code (0-3)
                int conditionsMeteo = 0; // normal par défaut
                if (meteo.isOrage()) conditionsMeteo = 3;
                else if (meteo.isNeige()) conditionsMeteo = 2;
                else if (meteo.isPluie()) conditionsMeteo = 1;

                // Affichage contexte
                System.out.println("[Prédiction] Météo : " + meteo);
                System.out.println("[Prédiction] Trafic : " + trafic);

                Platform.runLater(() -> {
                    // Météo dans son propre label
                    lblIaMeteo.setText(String.format("%s %s (%.1f°C)",
                            meteo.getIcone(), meteo.getDescription(), meteo.getTemperature()));
                    lblIaMeteo.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-font-weight: bold;");

                    // Trafic dans son propre label
                    lblIaTrafic.setText(String.format("%s Trafic %s (×%.2f)",
                            trafic.getIcone(), trafic.getNiveau(), trafic.coefficientImpact()));
                    lblIaTrafic.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-font-weight: bold;");

                    // Statut de progression inchangé
                    lblIaStatut.setText("🔄 Calcul en cours avec contexte temps réel…");
                });

                // Prédiction enrichie
                return predictionService.predireAvecContexte(
                        colis.getAdresseDepart(),
                        colis.getAdresseDestination(),
                        colis.getPoids(),
                        maintenant,
                        conditionsMeteo,
                        trafic.getCongestion(),
                        meteo.getTemperature(),
                        meteo.getVisibilite());
            }
        };

        taskCourante.setOnSucceeded(ev -> {
            PredictionResult r = taskCourante.getValue();
            dernierePrediction = r;
            if (r.isSucces()) {
                afficherPredictionSucces(r);
            } else {
                afficherPredictionEchec(r.getErreur());
            }
            if (btnEnregistrer != null) btnEnregistrer.setDisable(false);
        });

        taskCourante.setOnFailed(ev -> {
            afficherPredictionEchec("Erreur inattendue");
            if (btnEnregistrer != null) btnEnregistrer.setDisable(false);
        });

        Thread t = new Thread(taskCourante, "ml-predict");
        t.setDaemon(true);
        t.start();
    }

    private void afficherPredictionSucces(PredictionResult r) {
        // Statut global
        setStatutIA("✅ Estimation IA disponible", "#27ae60");

        // Distance
        lblIaDistance.setText(String.format("📍 Distance estimée : %.2f km", r.getDistanceKm()));
        lblIaDistance.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e8449;");

        // Durée
        lblIaDuree.setText("⏱ Durée estimée : " + r.getDureeFormatee());
        lblIaDuree.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e8449;");

    }

    private void afficherPredictionEchec(String raison) {
        setStatutIA("⚠ Estimation indisponible — distance et durée seront à 0", "#e67e22");
        lblIaDistance.setText("Raison : " + raison);
        lblIaDistance.setStyle("-fx-font-size: 11px; -fx-text-fill: #e67e22;");
        lblIaDuree.setText("");
    }

    private void reinitialiserBandeauIA() {
        if (taskCourante != null && taskCourante.isRunning()) taskCourante.cancel(true);
        dernierePrediction = null;
        setStatutIA("Sélectionnez un colis pour lancer l'analyse IA", "#888888");
        lblIaMeteo.setText("");      // NOUVEAU
        lblIaTrafic.setText("");
        lblIaDistance.setText("");
        lblIaDuree.setText("");
        if (btnEnregistrer != null) btnEnregistrer.setDisable(false);
    }

    private void setStatutIA(String texte, String couleur) {
        lblIaStatut.setText(texte);
        lblIaStatut.setStyle("-fx-font-size: 12px; -fx-text-fill: " + couleur + ";");
    }

    // ─────────────────────────────────────────────────────────────────
    // Enregistrement (inchangé)
    // ─────────────────────────────────────────────────────────────────

    @FXML
    public void enregistrer() {
        lblErreur.setText("");

        ColisAvecDistance selection = tableColisDisponibles.getSelectionModel().getSelectedItem();
        if (selection == null) {
            lblErreur.setText("Veuillez sélectionner un colis dans la liste.");
            return;
        }

        Colis colis = selection.getColis();

        if (!SessionManager.getInstance().isConnecte()) {
            lblErreur.setText("Erreur : aucun utilisateur connecté.");
            return;
        }

        int livreurId = SessionManager.getInstance().getIdConnecte();
        Livraison liv = new Livraison(colis.getId(), livreurId, 0);

        if (dernierePrediction != null && dernierePrediction.isSucces()) {
            liv.setDistanceKm(dernierePrediction.getDistanceKm());
            liv.setDureeEstimeeMinutes(dernierePrediction.getDureeMinutes());
        } else {
            liv.setDistanceKm(0);
            liv.setDureeEstimeeMinutes(0);
        }

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
            com.gestioncolis.entities.Utilisateurs u = currentUser != null ? currentUser : SessionManager.getInstance().getUtilisateurConnecte();
            String target = (u != null && u.getRole() == com.gestioncolis.enums.Role.ADMIN)
                    ? "/views/DashboardLayout.fxml"
                    : "/fxml/listeLivraisons.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(target));
            Parent root = loader.load();
            if (u != null) {
                Object ctrl = loader.getController();
                if (ctrl instanceof DashboardController.UserAware ua) {
                    ua.setCurrentUser(u);
                }
            }
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