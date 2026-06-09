package com.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class MainApp extends Application {

    private Process flaskProcess = null;

    @Override
    public void start(Stage stage) throws Exception {

        // ✅ Démarrer Flask automatiquement
        demarrerFlask();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/esprit/admin-dashboard.fxml")
        );

        Scene scene = new Scene(loader.load());
        stage.setTitle("PackTrack Admin");
        stage.setWidth(1100);
        stage.setHeight(700);
        stage.setScene(scene);

        // ✅ Arrêter Flask quand on ferme l'app
        stage.setOnCloseRequest(e -> arreterFlask());

        stage.show();
    }

    // ─── Démarrage Flask ──────────────────────────────────────────────────────
    private void demarrerFlask() {
        new Thread(() -> {
            try {
                // Chemin vers le dossier ml/ (relatif au projet)
                String projetDir = System.getProperty("user.dir");
                String mlDir     = projetDir + File.separator + "ml";
                String appPy     = mlDir + File.separator + "app.py";

                File f = new File(appPy);
                if (!f.exists()) {
                    System.out.println("⚠ app.py introuvable dans : " + mlDir);
                    return;
                }

                // Détecter python (Windows : python, Linux/Mac : python3)
                String python = System.getProperty("os.name")
                        .toLowerCase().contains("win") ? "python" : "python3";

                ProcessBuilder pb = new ProcessBuilder(python, appPy);
                pb.directory(new File(mlDir));
                pb.redirectErrorStream(true);

                flaskProcess = pb.start();
                System.out.println(" Flask démarré automatiquement.");

                // Lire les logs Flask dans la console IntelliJ
                new Thread(() -> {
                    try (var reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(flaskProcess.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("[Flask] " + line);
                        }
                    } catch (IOException ignored) {}
                }).start();

            } catch (IOException e) {
                System.out.println(" Impossible de démarrer Flask : " + e.getMessage());
            }
        }).start();
    }

    // ─── Arrêt Flask ──────────────────────────────────────────────────────────
    private void arreterFlask() {
        if (flaskProcess != null && flaskProcess.isAlive()) {
            flaskProcess.destroy();
            System.out.println("Flask arrêté.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}