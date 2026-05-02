package com.gestioncolis.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/AuthView.fxml")
        );

        Scene scene = new Scene(loader.load(), 1100, 700);

        stage.setTitle("TrackPack — Authentification");
        stage.setScene(scene);

        // Permettre le redimensionnement
        stage.setResizable(true);

        // Définir les tailles minimales pour éviter un affichage trop petit
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        // Centrer la fenêtre sur l'écran
        stage.centerOnScreen();

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}