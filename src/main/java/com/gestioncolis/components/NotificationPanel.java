package com.gestioncolis.components;

import java.util.List;

import com.gestioncolis.entities.Notification;
import com.gestioncolis.services.NotificationService;
import com.gestioncolis.utils.DateTimeUtils;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Dropdown notification panel that appears below the bell icon.
 * Shows a scrollable list of notifications with read/unread states.
 */
public class NotificationPanel extends VBox {

    private final NotificationService notificationService;
    private final long userId;
    private final VBox notificationList;
    private final Label emptyLabel;
    private Runnable onBadgeUpdate;
    private java.util.function.Consumer<Long> onNotificationClick;

    public NotificationPanel(NotificationService notificationService, long userId) {
        this.notificationService = notificationService;
        this.userId = userId;

        getStyleClass().add("notification-panel");
        setVisible(false);
        setManaged(false);
        setPrefWidth(380);
        setMaxWidth(380);
        setMaxHeight(420);

        // Header
        HBox header = buildHeader();

        // Notification list
        notificationList = new VBox(0);
        notificationList.getStyleClass().add("notification-list");

        emptyLabel = new Label("🔔  Aucune notification");
        emptyLabel.getStyleClass().add("notification-empty");
        emptyLabel.setMaxWidth(Double.MAX_VALUE);
        emptyLabel.setAlignment(Pos.CENTER);
        emptyLabel.setPadding(new Insets(30, 0, 30, 0));

        ScrollPane scrollPane = new ScrollPane(notificationList);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("notification-scroll");
        scrollPane.setPrefHeight(340);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(header, scrollPane);
    }

    public void setOnBadgeUpdate(Runnable onBadgeUpdate) {
        this.onBadgeUpdate = onBadgeUpdate;
    }

    public void setOnNotificationClick(java.util.function.Consumer<Long> onNotificationClick) {
        this.onNotificationClick = onNotificationClick;
    }

    /**
     * Toggles the panel visibility with animation.
     */
    public void toggle() {
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }

    public void show() {
        refresh();
        setVisible(true);
        setManaged(true);

        // Scale + fade in animation
        setScaleY(0.8);
        setOpacity(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(200), this);
        scale.setFromY(0.8);
        scale.setToY(1.0);

        FadeTransition fade = new FadeTransition(Duration.millis(200), this);
        fade.setFromValue(0);
        fade.setToValue(1);

        scale.play();
        fade.play();
    }

    public void hide() {
        FadeTransition fade = new FadeTransition(Duration.millis(150), this);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            setVisible(false);
            setManaged(false);
        });
        fade.play();
    }

    /**
     * Refreshes the notification list from the database.
     */
    public void refresh() {
        notificationList.getChildren().clear();
        List<Notification> notifications = notificationService.getNotifications(userId, 30);

        if (notifications.isEmpty()) {
            notificationList.getChildren().add(emptyLabel);
            return;
        }

        for (Notification notif : notifications) {
            notificationList.getChildren().add(buildNotificationItem(notif));
        }
    }

    private HBox buildHeader() {
        HBox header = new HBox(10);
        header.getStyleClass().add("notification-panel-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 18, 14, 18));

        Label title = new Label("🔔  Notifications");
        title.getStyleClass().add("notification-panel-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button markAllBtn = new Button("✓ Tout lu");
        markAllBtn.getStyleClass().add("notification-mark-all-btn");
        markAllBtn.setOnAction(e -> {
            notificationService.markAllAsRead(userId);
            refresh();
            if (onBadgeUpdate != null) onBadgeUpdate.run();
        });

        header.getChildren().addAll(title, spacer, markAllBtn);
        return header;
    }

    private HBox buildNotificationItem(Notification notif) {
        HBox item = new HBox(12);
        item.getStyleClass().add("notification-item");
        if (!notif.isRead()) {
            item.getStyleClass().add("notification-unread");
        }
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 16, 12, 16));
        item.setCursor(javafx.scene.Cursor.HAND);

        // Unread indicator dot
        Label dot = new Label();
        dot.getStyleClass().add("notification-dot");
        dot.setMinSize(8, 8);
        dot.setMaxSize(8, 8);
        if (!notif.isRead()) {
            dot.getStyleClass().add("notification-dot-active");
        }

        // Icon
        Label icon = new Label("💬");
        icon.getStyleClass().add("notification-item-icon");
        icon.setMinSize(36, 36);
        icon.setMaxSize(36, 36);
        icon.setAlignment(Pos.CENTER);

        // Text content
        VBox textBox = new VBox(3);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        String actorName = notif.getActorName() != null ? notif.getActorName() : "Utilisateur";
        String postTitle = notif.getPostTitle() != null ? notif.getPostTitle() : "votre post";
        if (postTitle.length() > 35) {
            postTitle = postTitle.substring(0, 32) + "...";
        }

        Label message = new Label(actorName + " a commenté sur \"" + postTitle + "\"");
        message.getStyleClass().add("notification-item-message");
        message.setWrapText(true);

        Label time = new Label(DateTimeUtils.format(notif.getCreatedAt()));
        time.getStyleClass().add("notification-item-time");

        textBox.getChildren().addAll(message, time);

        item.getChildren().addAll(dot, icon, textBox);

        // Click to mark as read and navigate
        item.setOnMouseClicked(e -> {
            if (!notif.isRead()) {
                notificationService.markAsRead(notif.getId());
                notif.setRead(true);
                item.getStyleClass().remove("notification-unread");
                dot.getStyleClass().remove("notification-dot-active");
                if (onBadgeUpdate != null) onBadgeUpdate.run();
            }
            if (onNotificationClick != null) {
                onNotificationClick.accept(notif.getPostId());
                hide(); // Hide panel after click
            }
        });

        return item;
    }
}
