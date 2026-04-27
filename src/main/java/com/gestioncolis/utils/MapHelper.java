package com.gestioncolis.utils;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MapHelper {

    public static void ouvrirCarte(String titre, TextField cible) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(titre);
        dialog.setWidth(860);
        dialog.setHeight(660);

        WebView webView  = new WebView();
        WebEngine engine = webView.getEngine();
        VBox.setVgrow(webView, Priority.ALWAYS);

        // Champ saisie
        TextField tfSaisie = new TextField(cible.getText());
        tfSaisie.setPromptText("Tapez une adresse ou cliquez directement sur la carte…");
        tfSaisie.setStyle("-fx-pref-height: 38px; -fx-font-size: 13px; " +
                "-fx-border-color: #ddd; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 0 12;");
        HBox.setHgrow(tfSaisie, Priority.ALWAYS);

        Button btnLocaliser = new Button("🔍 Localiser");
        btnLocaliser.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 8 16;");

        HBox hbSaisie = new HBox(8, tfSaisie, btnLocaliser);

        // Liste suggestions
        ListView<String> listSuggestions = new ListView<>();
        listSuggestions.setStyle("-fx-font-size: 12px;");
        listSuggestions.setPrefHeight(120);
        listSuggestions.setMaxHeight(120);
        listSuggestions.setVisible(false);
        listSuggestions.setManaged(false);
        List<String> adressesCompletes = new ArrayList<>();

        // Label statut + boutons
        Label lblStatut = new Label("Cliquez sur la carte ou saisissez une adresse ci-dessous.");
        lblStatut.setStyle("-fx-font-size: 12px; -fx-text-fill: #2980b9; -fx-wrap-text: true;");

        Button btnValider = new Button("✅  Valider cette adresse");
        btnValider.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 10 24;");
        btnValider.setDisable(cible.getText().isBlank());

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: transparent; -fx-border-color: #ccc; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 13px; " +
                "-fx-cursor: hand; -fx-padding: 10 20;");

        final String[] adresseSelectionnee = { cible.getText() };

        // Bridge
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaApp",
                        new MapBridge(lblStatut, btnValider, adresseSelectionnee, tfSaisie));
                if (!cible.getText().isBlank()) {
                    localiserDansMap(engine, cible.getText(), lblStatut, btnValider,
                            adresseSelectionnee, tfSaisie);
                }
            }
        });
        engine.loadContent(buildHtml());

        // Autocomplete
        tfSaisie.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().length() < 3) {
                listSuggestions.setVisible(false);
                listSuggestions.setManaged(false);
                return;
            }
            String q = val.trim();
            new Thread(() -> {
                List<String> s = rechercherNominatim(q);
                Platform.runLater(() -> {
                    adressesCompletes.clear();
                    adressesCompletes.addAll(s);
                    listSuggestions.getItems().setAll(s);
                    boolean v = !s.isEmpty();
                    listSuggestions.setVisible(v);
                    listSuggestions.setManaged(v);
                });
            }, "nominatim-suggest").start();
        });

        listSuggestions.setOnMouseClicked(e -> {
            int idx = listSuggestions.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < adressesCompletes.size()) {
                String adr = adressesCompletes.get(idx);
                tfSaisie.setText(adr);
                adresseSelectionnee[0] = adr;
                listSuggestions.setVisible(false);
                listSuggestions.setManaged(false);
                localiserDansMap(engine, adr, lblStatut, btnValider, adresseSelectionnee, tfSaisie);
                btnValider.setDisable(false);
            }
        });

        btnLocaliser.setOnAction(e -> {
            String adr = tfSaisie.getText().trim();
            if (adr.isBlank()) return;
            listSuggestions.setVisible(false);
            listSuggestions.setManaged(false);
            lblStatut.setText("Recherche en cours…");
            localiserDansMap(engine, adr, lblStatut, btnValider, adresseSelectionnee, tfSaisie);
        });
        tfSaisie.setOnAction(e -> btnLocaliser.fire());

        btnValider.setOnAction(e -> {
            if (!adresseSelectionnee[0].isBlank()) cible.setText(adresseSelectionnee[0]);
            dialog.close();
        });
        btnAnnuler.setOnAction(e -> dialog.close());

        Label lblTitre = new Label(titre);
        lblTitre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        VBox layout = new VBox(8, lblTitre, webView,
                new Label("📍 Adresse :") {{ setStyle("-fx-font-size: 12px; -fx-text-fill:#555;"); }},
                hbSaisie, listSuggestions, lblStatut,
                new HBox(12, btnValider, btnAnnuler));
        layout.setPadding(new Insets(14));

        dialog.setScene(new Scene(layout));
        dialog.showAndWait();
    }

    private static void localiserDansMap(WebEngine engine, String adresse,
                                         Label lblStatut, Button btnValider,
                                         String[] sel, TextField tfSaisie) {
        new Thread(() -> {
            double[] coords = geocoderNominatim(adresse);
            if (coords == null) {
                Platform.runLater(() -> lblStatut.setText(
                        "⚠ Adresse introuvable — vous pouvez quand même cliquer sur la carte."));
                return;
            }
            List<String> noms = rechercherNominatim(adresse);
            String nom = noms.isEmpty() ? adresse : noms.get(0);
            Platform.runLater(() -> {
                engine.executeScript(String.format("setMarkerFromJava(%s, %s, '%s')",
                        coords[0], coords[1],
                        nom.replace("'", "\\'").replace("\n", " ")));
                sel[0] = nom;
                tfSaisie.setText(nom);
                lblStatut.setText("📍 " + nom);
                lblStatut.setStyle("-fx-font-size:12px; -fx-text-fill:#27ae60; -fx-font-weight:bold;");
                btnValider.setDisable(false);
            });
        }, "nominatim-geocode").start();
    }

    private static List<String> rechercherNominatim(String query) {
        List<String> results = new ArrayList<>();
        try {
            String enc = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search"
                    + "?format=json&q=" + enc + "&accept-language=fr&limit=6&countrycodes=tn";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "PackTrack/1.0");
            conn.setConnectTimeout(4000); conn.setReadTimeout(4000);
            StringBuilder sb = new StringBuilder();
            try (InputStreamReader r = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                char[] buf = new char[4096]; int n;
                while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
            }
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++)
                results.add(arr.getJSONObject(i).optString("display_name", ""));
        } catch (Exception e) { System.err.println("[MapHelper] suggest: " + e.getMessage()); }
        return results;
    }

    private static double[] geocoderNominatim(String adresse) {
        try {
            List<String> r = rechercherNominatim(adresse);
            if (r.isEmpty()) return null;
            // On re-geocode le premier résultat pour avoir les coords
            String enc = URLEncoder.encode(r.get(0), StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search"
                    + "?format=json&q=" + enc + "&accept-language=fr&limit=1&countrycodes=tn";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "PackTrack/1.0");
            conn.setConnectTimeout(4000); conn.setReadTimeout(4000);
            StringBuilder sb = new StringBuilder();
            try (InputStreamReader rd = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                char[] buf = new char[4096]; int n;
                while ((n = rd.read(buf)) != -1) sb.append(buf, 0, n);
            }
            JSONArray arr = new JSONArray(sb.toString());
            if (arr.length() > 0) {
                JSONObject obj = arr.getJSONObject(0);
                return new double[]{ Double.parseDouble(obj.getString("lat")),
                        Double.parseDouble(obj.getString("lon")) };
            }
        } catch (Exception e) { System.err.println("[MapHelper] geocode: " + e.getMessage()); }
        return null;
    }

    public static class MapBridge {
        private final Label lblStatut;
        private final Button btnValider;
        private final String[] adresse;
        private final TextField tfSaisie;

        public MapBridge(Label l, Button b, String[] a, TextField tf) {
            lblStatut = l; btnValider = b; adresse = a; tfSaisie = tf;
        }

        public void setAdresse(String adresseStr) {
            Platform.runLater(() -> {
                adresse[0] = adresseStr;
                tfSaisie.setText(adresseStr);
                lblStatut.setText("📍 " + adresseStr);
                lblStatut.setStyle("-fx-font-size:12px; -fx-text-fill:#27ae60; -fx-font-weight:bold;");
                btnValider.setDisable(false);
            });
        }
    }

    private static String buildHtml() {
        return """
                <!DOCTYPE html><html>
                <head>
                  <meta charset="utf-8"/>
                  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                  <style>* {margin:0;padding:0;box-sizing:border-box} #map{width:100%;height:400px}</style>
                </head>
                <body>
                  <div id="map"></div>
                  <script>
                    var map = L.map('map').setView([36.8065, 10.1815], 12);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
                      {attribution:'© OpenStreetMap', maxZoom:19}).addTo(map);
                    var marker = null;

                    function setMarkerFromJava(lat, lon, label) {
                      var ll = L.latLng(lat, lon);
                      if (marker) { marker.setLatLng(ll); } else {
                        marker = L.marker(ll, {draggable:true}).addTo(map);
                        marker.on('dragend', function(ev) {
                          reverseGeocode(ev.target.getLatLng().lat, ev.target.getLatLng().lng); });
                      }
                      map.setView(ll, 15);
                      if (label) marker.bindPopup('<b>' + label + '</b>').openPopup();
                    }

                    map.on('click', function(e) {
                      setMarkerFromJava(e.latlng.lat, e.latlng.lng, null);
                      reverseGeocode(e.latlng.lat, e.latlng.lng);
                    });

                    function reverseGeocode(lat, lng) {
                      fetch('https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat='+lat+'&lon='+lng+'&accept-language=fr',
                            {headers:{'Accept':'application/json'}})
                        .then(function(r){return r.json();})
                        .then(function(data){
                          var adr = data.display_name || (lat.toFixed(5)+', '+lng.toFixed(5));
                          if(window.javaApp) window.javaApp.setAdresse(adr);
                          if(marker) marker.bindPopup('<b>'+adr+'</b>').openPopup();
                        }).catch(function(){
                          var c = lat.toFixed(5)+', '+lng.toFixed(5);
                          if(window.javaApp) window.javaApp.setAdresse(c);
                        });
                    }
                  </script>
                </body></html>
                """;
    }
}