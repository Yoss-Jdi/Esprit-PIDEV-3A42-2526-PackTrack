package com.gestioncolis.utils;

import com.gestioncolis.models.Colis;
import com.gestioncolis.models.Livraison;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Affiche les détails d'un colis ou d'une livraison
 * dans une fenêtre modale avec un QR code (sans ID).
 */
public class QrCodeHelper {

    // ── Colis ─────────────────────────────────────────────────────────

    public static void afficherQrColis(Colis colis) {
        String contenu = "COLIS\n"
                + "Description : " + nvl(colis.getDescription()) + "\n"
                + "Départ : "      + nvl(colis.getAdresseDepart()) + "\n"
                + "Destination : " + nvl(colis.getAdresseDestination()) + "\n"
                + "Poids : "       + colis.getPoids() + " kg\n"
                + "Statut : "      + nvl(colis.getStatut()) + "\n"
                + "Montant : "     + String.format("%.2f DT", colis.calculerMontant());

        Stage dialog = buildDialog("QR Code — Colis");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(22));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: white;");

        Label lblTitre = titre("📦 Détails du colis");
        layout.getChildren().addAll(
                lblTitre,
                new Separator(),
                info("Description",  nvl(colis.getDescription())),
                info("Départ",       nvl(colis.getAdresseDepart())),
                info("Destination",  nvl(colis.getAdresseDestination())),
                info("Poids",        colis.getPoids() + " kg"),
                info("Statut",       nvl(colis.getStatut())),
                info("Montant",      String.format("%.2f DT", colis.calculerMontant())),
                new Separator()
        );

        ajouterQrEtBouton(layout, contenu, dialog);

        dialog.setScene(new Scene(layout));
        dialog.showAndWait();
    }

    // ── Livraison ──────────────────────────────────────────────────────

    public static void afficherQrLivraison(Livraison livraison) {
        String contenu = "LIVRAISON\n"
                + "Colis : "       + nvl(livraison.getDescriptionColis()) + "\n"
                + "Livreur : "     + nvl(livraison.getNomLivreur()) + "\n"
                + "Statut : "      + nvl(livraison.getStatut()) + "\n"
                + "Distance : "    + livraison.getDistanceKm() + " km\n"
                + "Durée est. : "  + livraison.getDureeFormatee() + "\n"
                + "Total : "       + String.format("%.2f DT", livraison.getTotal());

        Stage dialog = buildDialog("QR Code — Livraison");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(22));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: white;");

        Label lblTitre = titre("🚚 Détails de la livraison");
        layout.getChildren().addAll(
                lblTitre,
                new Separator(),
                info("Colis",      nvl(livraison.getDescriptionColis())),
                info("Livreur",    nvl(livraison.getNomLivreur())),
                info("Statut",     nvl(livraison.getStatut())),
                info("Distance",   livraison.getDistanceKm() + " km"),
                info("Durée est.", livraison.getDureeFormatee()),
                info("Total",      String.format("%.2f DT", livraison.getTotal())),
                new Separator()
        );

        ajouterQrEtBouton(layout, contenu, dialog);

        dialog.setScene(new Scene(layout));
        dialog.showAndWait();
    }

    // ── Helpers internes ───────────────────────────────────────────────

    private static Stage buildDialog(String titreStr) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(titreStr);
        dialog.setWidth(420);
        dialog.setResizable(false);
        return dialog;
    }

    private static void ajouterQrEtBouton(VBox layout, String contenu, Stage dialog) {
        ImageView ivQr = new ImageView();
        ivQr.setFitWidth(200);
        ivQr.setFitHeight(200);
        ivQr.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1;");

        Label lblStatut = new Label("Chargement du QR code…");
        lblStatut.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaa;");

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 10 30;");
        btnFermer.setOnAction(e -> dialog.close());

        layout.getChildren().addAll(ivQr, lblStatut, btnFermer);

        // Chargement asynchrone
        new Thread(() -> {
            try {
                String enc = URLEncoder.encode(contenu, StandardCharsets.UTF_8);
                String url = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + enc;
                javafx.scene.image.Image img = new javafx.scene.image.Image(url, true);
                Platform.runLater(() -> {
                    ivQr.setImage(img);
                    lblStatut.setText("Scannez pour voir les détails");
                    lblStatut.setStyle("-fx-font-size: 11px; -fx-text-fill: #27ae60;");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatut.setText("⚠ Impossible de charger le QR code (vérifiez la connexion)");
                    lblStatut.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
                });
            }
        }, "qr-load").start();
    }

    private static Label titre(String texte) {
        Label l = new Label(texte);
        l.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        return l;
    }

    private static Label info(String cle, String valeur) {
        Label l = new Label(cle + " : " + valeur);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-wrap-text: true;");
        l.setMaxWidth(370);
        return l;
    }

    private static String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}