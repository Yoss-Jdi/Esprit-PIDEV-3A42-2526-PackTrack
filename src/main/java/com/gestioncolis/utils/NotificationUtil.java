package com.gestioncolis.utils;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class NotificationUtil {

    public static void showToast(StackPane root, String message, String type) {
        Label toast = new Label(message);
        toast.getStyleClass().add("toast-premium");

        if (type.equals("success")) {
            toast.setStyle("-fx-background-color: #22c55e;");
        } else if (type.equals("error")) {
            toast.setStyle("-fx-background-color: #ef4444;");
        } else {
            toast.setStyle("-fx-background-color: #3b82f6;");
        }

        root.getChildren().add(toast);

        FadeTransition ft = new FadeTransition(Duration.millis(300), toast);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        Timeline timer = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            FadeTransition out = new FadeTransition(Duration.millis(300), toast);
            out.setToValue(0);
            out.setOnFinished(ev -> root.getChildren().remove(toast));
            out.play();
        }));
        timer.play();
    }
}