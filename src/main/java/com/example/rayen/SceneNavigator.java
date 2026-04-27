package com.example.rayen;

import com.example.rayen.controllers.addContoller;
import com.example.rayen.controllers.addtechnicien;
import com.example.rayen.controllers.chatbot;
import com.example.rayen.controllers.displayContoller;
import com.example.rayen.controllers.displaytechnicien;
import com.example.rayen.controllers.modifyContoller;
import com.example.rayen.controllers.modifytechnicien;
import com.example.rayen.controllers.stat;
import com.example.rayen.entities.technicien;
import com.example.rayen.entities.vehicule;
import com.example.rayen.services.OllamaService;
import com.example.rayen.services.Servicetechnicien;
import com.example.rayen.services.Servicevehicule;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

public class SceneNavigator {
    private final Stage stage;
    private final Servicevehicule servicevehicule;
    private final Servicetechnicien servicetechnicien;
    private final OllamaService ollamaService;
    private Stage chatbotStage;

    public SceneNavigator(Stage stage, Servicevehicule servicevehicule, Servicetechnicien servicetechnicien, OllamaService ollamaService) {
        this.stage = stage;
        this.servicevehicule = servicevehicule;
        this.servicetechnicien = servicetechnicien;
        this.ollamaService = ollamaService;
    }

    public void showDisplayView() throws IOException {
        showDisplayView(null);
    }

    public void showDisplayView(String message) throws IOException {
        chargerVue("display.fxml", "Gestion des vehicules", controller -> {
            if (message != null && !message.isBlank()) {
                ((displayContoller) controller).setInitialMessage(message);
            }
        });
    }

    public void showAddView() throws IOException {
        chargerVue("add.fxml", "Ajouter un vehicule", null);
    }

    public void showModifyView(vehicule vehiculeSelectionne) throws IOException {
        chargerVue("modify.fxml", "Modifier un vehicule", controller ->
                ((modifyContoller) controller).setVehicule(vehiculeSelectionne));
    }

    public void showDisplayTechnicienView() throws IOException {
        showDisplayTechnicienView(null);
    }

    public void showDisplayTechnicienView(String message) throws IOException {
        chargerVue("displaytechnicien.fxml", "Gestion des techniciens", controller -> {
            if (message != null && !message.isBlank()) {
                ((displaytechnicien) controller).setInitialMessage(message);
            }
        });
    }

    public void showAddTechnicienView() throws IOException {
        chargerVue("addtechnicien.fxml", "Ajouter un technicien", null);
    }

    public void showModifyTechnicienView(technicien technicienSelectionne) throws IOException {
        chargerVue("modifytechnicien.fxml", "Modifier un technicien", controller ->
                ((modifytechnicien) controller).setTechnicien(technicienSelectionne));
    }

    public void showStatView() throws IOException {
        chargerVue("stat.fxml", "Statistiques des techniciens", null);
    }

    public void showChatbotWindow() throws IOException {
        if (chatbotStage != null && chatbotStage.isShowing()) {
            chatbotStage.toFront();
            return;
        }

        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("chatbot.fxml"));
        loader.setControllerFactory(this::creerControleur);

        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(HelloApplication.class.getResource("style.css").toExternalForm());

        Stage popup = new Stage(StageStyle.TRANSPARENT);
        popup.initOwner(stage);
        popup.setResizable(false);
        popup.setScene(scene);
        popup.setAlwaysOnTop(true);
        popup.setOnHidden(event -> chatbotStage = null);
        popup.show();

        positionnerChatbot(popup);
        chatbotStage = popup;
    }

    private void chargerVue(String nomFxml, String titreFenetre, Consumer<Object> configurationControleur) throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(nomFxml));
        loader.setControllerFactory(this::creerControleur);

        Parent root = loader.load();
        Object controller = loader.getController();
        if (configurationControleur != null && controller != null) {
            configurationControleur.accept(controller);
        }

        Scene scene = new Scene(root, 1080, 650);
        scene.getStylesheets().add(HelloApplication.class.getResource("style.css").toExternalForm());

        stage.setTitle(titreFenetre);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    private void positionnerChatbot(Stage popup) {
        popup.sizeToScene();

        double marge = 24;
        double x = Math.max(stage.getX() + marge, stage.getX() + stage.getWidth() - popup.getWidth() - marge);
        double y = Math.max(stage.getY() + marge, stage.getY() + stage.getHeight() - popup.getHeight() - marge);

        popup.setX(x);
        popup.setY(y);
    }

    private Object creerControleur(Class<?> type) {
        if (type == displayContoller.class) {
            return new displayContoller(servicevehicule, this);
        }
        if (type == addContoller.class) {
            return new addContoller(servicevehicule, this);
        }
        if (type == modifyContoller.class) {
            return new modifyContoller(servicevehicule, this);
        }
        if (type == displaytechnicien.class) {
            return new displaytechnicien(servicetechnicien, this);
        }
        if (type == addtechnicien.class) {
            return new addtechnicien(servicetechnicien, this);
        }
        if (type == modifytechnicien.class) {
            return new modifytechnicien(servicetechnicien, this);
        }
        if (type == stat.class) {
            return new stat(servicetechnicien, this);
        }
        if (type == chatbot.class) {
            return new chatbot(ollamaService);
        }

        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            throw new IllegalStateException("Impossible d'instancier le controleur " + type.getName(), exception);
        }
    }
}
