package com.gestioncolis.utils;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

/**
 * Utilitaire partagé pour ouvrir la carte Leaflet / OpenStreetMap
 * dans un dialog modal et récupérer une adresse via Nominatim.
 *
 * Utilisé par AjouterColisController ET ModifierColisController.
 */
public class MapHelper {

    /**
     * Ouvre le dialog carte et insère l'adresse choisie dans {@code cible}.
     *
     * @param titre Titre de la fenêtre
     * @param cible TextField dans lequel écrire l'adresse sélectionnée
     */
    public static void ouvrirCarte(String titre, TextField cible) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(titre);
        dialog.setWidth(820);
        dialog.setHeight(580);

        WebView webView   = new WebView();
        WebEngine engine  = webView.getEngine();

        Label lblAdresse = new Label("Cliquez sur la carte pour sélectionner une adresse...");
        lblAdresse.setStyle("-fx-font-size: 13px; -fx-text-fill: #2980b9; -fx-padding: 0 0 4 0;");
        lblAdresse.setWrapText(true);

        Button btnValider = new Button("✅  Valider cette adresse");
        btnValider.setStyle(
                "-fx-background-color: #2c3e50; -fx-text-fill: white; " +
                        "-fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 10 24;");
        btnValider.setDisable(true);

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle(
                "-fx-background-color: transparent; -fx-border-color: #ccc; " +
                        "-fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 13px; " +
                        "-fx-cursor: hand; -fx-padding: 10 20;");

        final String[] adresseSelectionnee = {""};

        // Bridge Java ↔ JavaScript
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaApp", new MapBridge(lblAdresse, btnValider, adresseSelectionnee));
            }
        });

        engine.loadContent(buildHtml());

        btnValider.setOnAction(e -> {
            if (!adresseSelectionnee[0].isEmpty()) {
                cible.setText(adresseSelectionnee[0]);
            }
            dialog.close();
        });
        btnAnnuler.setOnAction(e -> dialog.close());

        HBox boutons = new HBox(12, btnValider, btnAnnuler);
        boutons.setStyle("-fx-alignment: CENTER_LEFT;");

        VBox layout = new VBox(8, webView, lblAdresse, boutons);
        layout.setPadding(new Insets(12));
        VBox.setVgrow(webView, Priority.ALWAYS);

        dialog.setScene(new Scene(layout));
        dialog.showAndWait();
    }

    // ── Bridge Java ↔ JavaScript ──────────────────────────────────────

    /**
     * Classe exposée au JavaScript via JSObject.
     * Le JS appelle {@code javaApp.setAdresse(adresseStr)} après le geocoding.
     */
    public static class MapBridge {
        private final Label    lblAdresse;
        private final Button   btnValider;
        private final String[] adresse;

        public MapBridge(Label lbl, Button btn, String[] adresse) {
            this.lblAdresse = lbl;
            this.btnValider = btn;
            this.adresse    = adresse;
        }

        /** Appelée depuis JavaScript */
        public void setAdresse(String adresseStr) {
            Platform.runLater(() -> {
                adresse[0] = adresseStr;
                lblAdresse.setText("📍 " + adresseStr);
                lblAdresse.setStyle(
                        "-fx-font-size: 13px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
                btnValider.setDisable(false);
            });
        }
    }

    // ── HTML Leaflet / OpenStreetMap ──────────────────────────────────

    private static String buildHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8"/>
                  <title>Choisir une adresse</title>
                  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                  <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { font-family: sans-serif; }
                    #map { width: 100%; height: 492px; }
                    #info {
                      padding: 8px 14px;
                      background: #eaf4fb;
                      font-size: 13px;
                      color: #2980b9;
                      min-height: 36px;
                    }
                    .leaflet-popup-content { font-size: 13px; }
                  </style>
                </head>
                <body>
                  <div id="map"></div>
                  <div id="info">Cliquez sur la carte pour sélectionner une position...</div>
                  <script>
                    var map = L.map('map').setView([36.8065, 10.1815], 12); // Tunis par défaut

                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                      attribution: '© OpenStreetMap contributors',
                      maxZoom: 19
                    }).addTo(map);

                    var marker = null;

                    map.on('click', function(e) {
                      var lat = e.latlng.lat;
                      var lng = e.latlng.lng;

                      if (marker) {
                        marker.setLatLng(e.latlng);
                      } else {
                        marker = L.marker(e.latlng, { draggable: true }).addTo(map);
                        marker.on('dragend', function(ev) {
                          reverseGeocode(ev.target.getLatLng().lat, ev.target.getLatLng().lng);
                        });
                      }
                      reverseGeocode(lat, lng);
                    });

                    function reverseGeocode(lat, lng) {
                      document.getElementById('info').textContent = 'Recherche de l\\'adresse...';
                      var url = 'https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat='
                                + lat + '&lon=' + lng + '&accept-language=fr';

                      fetch(url, { headers: { 'Accept': 'application/json' } })
                        .then(function(r) { return r.json(); })
                        .then(function(data) {
                          var adresse = data.display_name || (lat.toFixed(5) + ', ' + lng.toFixed(5));
                          document.getElementById('info').textContent = '📍 ' + adresse;
                          if (window.javaApp) { window.javaApp.setAdresse(adresse); }
                          if (marker) { marker.bindPopup('<b>' + adresse + '</b>').openPopup(); }
                        })
                        .catch(function() {
                          var coords = lat.toFixed(5) + ', ' + lng.toFixed(5);
                          document.getElementById('info').textContent = '📍 ' + coords;
                          if (window.javaApp) { window.javaApp.setAdresse(coords); }
                        });
                    }
                  </script>
                </body>
                </html>
                """;
    }
}