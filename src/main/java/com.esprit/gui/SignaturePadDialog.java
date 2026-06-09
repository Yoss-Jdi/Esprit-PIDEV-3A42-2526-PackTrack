package com.esprit.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * SignatureDialog — Fenêtre popup de signature électronique professionnelle.
 *
 * Usage dans PdfService :
 *   SignatureDialog dialog = new SignatureDialog(ownerStage);
 *   File sigFile = dialog.showAndWait();  // null si annulé
 *   if (sigFile != null) { ... insérer sigFile dans le PDF ... }
 */
public class SignaturePadDialog {

    // Couleurs charte TrackPack
    private static final String BRAND_COLOR   = "#667eea";
    private static final String BRAND_DARK    = "#4f46e5";
    private static final String BG_COLOR      = "#f8f9ff";
    private static final String CANVAS_BG     = "#ffffff";
    private static final String BORDER_COLOR  = "#667eea";

    private final Stage owner;
    private File signatureFile = null;

    public SignaturePadDialog(Stage owner) {
        this.owner = owner;
    }

    /**
     * Affiche la fenêtre et attend la signature.
     * @return File PNG de la signature, ou null si annulé.
     */
    public File showAndWait() {

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Signature électronique — TrackPack");

        // ── Titre ──────────────────────────────────────────────────────────
        Label title = new Label("Signature Électronique");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        title.setTextFill(Color.web(BRAND_COLOR));

        Label subtitle = new Label("Signez dans le cadre ci-dessous avec votre souris");
        subtitle.setFont(Font.font("Arial", 12));
        subtitle.setTextFill(Color.web("#888888"));

        // Ligne décorative
        Region line = new Region();
        line.setPrefHeight(3);
        line.setStyle("-fx-background-color: linear-gradient(to right, " + BRAND_COLOR + ", #a78bfa, transparent);");

        VBox header = new VBox(6, title, subtitle, line);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));

        // ── Canvas signature ───────────────────────────────────────────────
        Canvas canvas = new Canvas(580, 220);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        clearCanvas(gc, canvas);

        // Filigrane "Signez ici"
        gc.setFont(Font.font("Arial", 18));
        gc.setFill(Color.web("#e0e4ff"));
        gc.fillText("Signez ici...", 210, 120);

        // Ligne de base
        gc.setStroke(Color.web("#c7d0f8"));
        gc.setLineWidth(1);
        gc.setLineDashes(6, 4);
        gc.strokeLine(40, 175, 540, 175);
        gc.setLineDashes(0);

        // Encadré canvas
        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setStyle(
                "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-color: " + CANVAS_BG + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.2), 12, 0, 0, 4);"
        );

        // ── Dessin souris ──────────────────────────────────────────────────
        final boolean[] drawing = {false};
        final boolean[] hasSignature = {false};

        canvas.setOnMousePressed(e -> {
            drawing[0] = true;
            hasSignature[0] = true;
            gc.setStroke(Color.web("#1e1b4b"));
            gc.setLineWidth(2.2);
            gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
        });

        canvas.setOnMouseDragged(e -> {
            if (drawing[0]) {
                gc.lineTo(e.getX(), e.getY());
                gc.stroke();
                gc.moveTo(e.getX(), e.getY());
            }
        });

        canvas.setOnMouseReleased(e -> drawing[0] = false);

        // ── Infos sous le canvas ───────────────────────────────────────────
        Label infoLeft = new Label("✎  Utilisez votre souris pour signer");
        infoLeft.setFont(Font.font("Arial", 10));
        infoLeft.setTextFill(Color.web("#aaaaaa"));

        Label infoRight = new Label("Document : TrackPack — Signature électronique certifiée");
        infoRight.setFont(Font.font("Arial", 10));
        infoRight.setTextFill(Color.web("#aaaaaa"));

        HBox infoBar = new HBox(infoLeft, new Region(), infoRight);
        HBox.setHgrow(infoBar.getChildren().get(1), Priority.ALWAYS);
        infoBar.setPadding(new Insets(4, 4, 0, 4));

        // ── Boutons ────────────────────────────────────────────────────────
        Button btnEffacer = new Button("⟳  Effacer");
        btnEffacer.setPrefSize(130, 40);
        btnEffacer.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: " + BRAND_COLOR + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;"
        );
        btnEffacer.setOnMouseEntered(e -> btnEffacer.setStyle(
                "-fx-background-color: #f0f0ff;" +
                        "-fx-border-color: " + BRAND_COLOR + ";" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: " + BRAND_COLOR + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;"
        ));
        btnEffacer.setOnMouseExited(e -> btnEffacer.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: " + BRAND_COLOR + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;"
        ));
        btnEffacer.setOnAction(e -> {
            clearCanvas(gc, canvas);
            hasSignature[0] = false;
            // Redessiner filigrane et ligne
            gc.setFont(Font.font("Arial", 18));
            gc.setFill(Color.web("#e0e4ff"));
            gc.fillText("Signez ici...", 210, 120);
            gc.setStroke(Color.web("#c7d0f8"));
            gc.setLineWidth(1);
            gc.setLineDashes(6, 4);
            gc.strokeLine(40, 175, 540, 175);
            gc.setLineDashes(0);
        });

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setPrefSize(130, 40);
        btnAnnuler.setStyle(
                "-fx-background-color: #f3f4f6;" +
                        "-fx-border-color: #d1d5db;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: #6b7280;" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;"
        );
        btnAnnuler.setOnAction(e -> {
            signatureFile = null;
            dialog.close();
        });

        Button btnValider = new Button("✔  Valider la signature");
        btnValider.setPrefSize(200, 40);
        btnValider.setStyle(
                "-fx-background-color: linear-gradient(to right, " + BRAND_COLOR + ", " + BRAND_DARK + ");" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.4), 8, 0, 0, 3);"
        );
        btnValider.setOnMouseEntered(e -> btnValider.setStyle(
                "-fx-background-color: linear-gradient(to right, " + BRAND_DARK + ", #3730a3);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(79,70,229,0.5), 10, 0, 0, 4);"
        ));
        btnValider.setOnMouseExited(e -> btnValider.setStyle(
                "-fx-background-color: linear-gradient(to right, " + BRAND_COLOR + ", " + BRAND_DARK + ");" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.4), 8, 0, 0, 3);"
        ));
        btnValider.setOnAction(e -> {
            if (!hasSignature[0]) {
                // Secouer le canvas pour indiquer qu'il faut signer
                canvasPane.setStyle(
                        "-fx-border-color: #ef4444;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 10;" +
                                "-fx-background-color: " + CANVAS_BG + ";" +
                                "-fx-background-radius: 10;"
                );
                new Thread(() -> {
                    try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> canvasPane.setStyle(
                            "-fx-border-color: " + BORDER_COLOR + ";" +
                                    "-fx-border-width: 2;" +
                                    "-fx-border-radius: 10;" +
                                    "-fx-background-color: " + CANVAS_BG + ";" +
                                    "-fx-background-radius: 10;" +
                                    "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.2), 12, 0, 0, 4);"
                    ));
                }).start();
                return;
            }

            // Capturer le canvas en PNG
            try {
                WritableImage snapshot = canvas.snapshot(null, null);
                File sigDir = new File("pdfs/signatures");
                sigDir.mkdirs();
                signatureFile = new File(sigDir, "signature_" + System.currentTimeMillis() + ".png");
                ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", signatureFile);
                dialog.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox btnBar = new HBox(12, btnEffacer, spacer, btnAnnuler, btnValider);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        // ── Badge certifié ────────────────────────────────────────────────
        Label badge = new Label("🔒  Signature électronique sécurisée — TrackPack © 2026");
        badge.setFont(Font.font("Arial", 10));
        badge.setTextFill(Color.web("#9ca3af"));
        badge.setAlignment(Pos.CENTER);

        // ── Layout global ──────────────────────────────────────────────────
        VBox root = new VBox(16, header, canvasPane, infoBar, btnBar, badge);
        root.setPadding(new Insets(30));
        root.setStyle(
                "-fx-background-color: " + BG_COLOR + ";" +
                        "-fx-border-color: " + BRAND_COLOR + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 16;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 24, 0, 0, 8);"
        );
        root.setMinWidth(660);
        root.setMaxWidth(660);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.showAndWait();

        return signatureFile;
    }

    private void clearCanvas(GraphicsContext gc, Canvas canvas) {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
}