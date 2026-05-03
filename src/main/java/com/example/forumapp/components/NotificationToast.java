package com.example.forumapp.components;

import com.example.forumapp.entities.Notification;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Animated toast notification that slides in from the bottom-right corner.
 * Shows a brief preview when a new comment is posted on the user's post.
 */
public class NotificationToast extends StackPane {

    private static final double TOAST_WIDTH = 340;
    private static final double TOAST_HEIGHT = 90;
    private static final Duration SLIDE_DURATION = Duration.millis(400);
    private static final Duration DISPLAY_DURATION = Duration.seconds(5);
    private static final Duration FADE_DURATION = Duration.millis(600);

    private final VBox toastContainer;
    private java.util.function.Consumer<Long> onNotificationClick;

    public NotificationToast() {
        setPickOnBounds(false);
        setAlignment(Pos.BOTTOM_RIGHT);
        setPadding(new Insets(0, 24, 24, 0));

        toastContainer = new VBox(8);
        toastContainer.setPickOnBounds(false);
        toastContainer.setAlignment(Pos.BOTTOM_RIGHT);
        toastContainer.setMaxWidth(TOAST_WIDTH);

        getChildren().add(toastContainer);
    }

    public void setOnNotificationClick(java.util.function.Consumer<Long> onNotificationClick) {
        this.onNotificationClick = onNotificationClick;
    }

    /**
     * Shows an animated toast for the given notification.
     */
    public void showToast(Notification notification) {
        HBox toast = buildToastCard(notification);
        toast.setTranslateX(TOAST_WIDTH + 50);
        toast.setOpacity(0);

        toastContainer.getChildren().add(toast);

        // Slide in from right
        TranslateTransition slideIn = new TranslateTransition(SLIDE_DURATION, toast);
        slideIn.setFromX(TOAST_WIDTH + 50);
        slideIn.setToX(0);

        FadeTransition fadeIn = new FadeTransition(SLIDE_DURATION, toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        slideIn.play();
        fadeIn.play();

        // Auto-dismiss after delay
        PauseTransition pause = new PauseTransition(DISPLAY_DURATION);
        pause.setOnFinished(e -> dismissToast(toast));
        pause.play();

        // Click to dismiss early and navigate
        toast.setOnMouseClicked(e -> {
            pause.stop();
            dismissToast(toast);
            if (onNotificationClick != null) {
                onNotificationClick.accept(notification.getPostId());
            }
        });
    }

    private void dismissToast(HBox toast) {
        TranslateTransition slideOut = new TranslateTransition(SLIDE_DURATION, toast);
        slideOut.setToX(TOAST_WIDTH + 50);

        FadeTransition fadeOut = new FadeTransition(FADE_DURATION, toast);
        fadeOut.setToValue(0);

        slideOut.play();
        fadeOut.play();

        fadeOut.setOnFinished(e -> toastContainer.getChildren().remove(toast));
    }

    private HBox buildToastCard(Notification notification) {
        HBox card = new HBox(12);
        card.getStyleClass().add("notification-toast");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(TOAST_WIDTH);
        card.setMaxWidth(TOAST_WIDTH);
        card.setPrefHeight(TOAST_HEIGHT);
        card.setPadding(new Insets(14, 18, 14, 18));

        // Avatar circle with bell icon
        Label avatar = new Label("💬");
        avatar.getStyleClass().add("notification-toast-icon");
        avatar.setMinSize(40, 40);
        avatar.setMaxSize(40, 40);
        avatar.setAlignment(Pos.CENTER);

        // Text content
        VBox textBox = new VBox(4);
        textBox.setAlignment(Pos.CENTER_LEFT);

        Label header = new Label("Nouveau commentaire");
        header.getStyleClass().add("notification-toast-header");

        String actorName = notification.getActorName() != null ? notification.getActorName() : "Quelqu'un";
        String postTitle = notification.getPostTitle() != null ? notification.getPostTitle() : "votre post";
        if (postTitle.length() > 30) {
            postTitle = postTitle.substring(0, 27) + "...";
        }

        Label body = new Label(actorName + " a commenté sur \"" + postTitle + "\"");
        body.getStyleClass().add("notification-toast-body");
        body.setWrapText(true);
        body.setMaxWidth(TOAST_WIDTH - 90);

        textBox.getChildren().addAll(header, body);
        card.getChildren().addAll(avatar, textBox);

        return card;
    }
}
