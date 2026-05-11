package com.gestioncolis.utils;

import com.gestioncolis.ForumApplication;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;

public class SceneManager {

    private final Stage stage;
    private final Callback<Class<?>, Object> controllerFactory;

    public SceneManager(Stage stage, Callback<Class<?>, Object> controllerFactory) {
        this.stage = stage;
        this.controllerFactory = controllerFactory;
    }

    public void showLogin() {
        switchScene("/views/AuthView.fxml", "TrackPack — Connexion", 1100, 700);
    }

    public void showUserDashboard() {
        switchScene("/com/example/forumapp/view/user-dashboard-view.fxml", "Forum — Espace utilisateur", 1320, 740);
    }

    public void showAdminDashboard() {
        switchScene("/com/example/forumapp/view/admin-dashboard-view.fxml", "Forum — Administration", 1340, 740);
    }

    private void switchScene(String fxmlPath, String title, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(ForumApplication.class.getResource(fxmlPath));
            loader.setControllerFactory(controllerFactory);
            Parent root = loader.load();

            Scene scene = new Scene(root, width, height);
            java.net.URL cssUrl = ForumApplication.class.getResource("/com/example/forumapp/css/forum-theme.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setTitle(title);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger la vue: " + fxmlPath, e);
        }
    }
}
