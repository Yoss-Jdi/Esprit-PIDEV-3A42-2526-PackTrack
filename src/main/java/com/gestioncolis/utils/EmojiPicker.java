package com.gestioncolis.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.function.Consumer;

public class EmojiPicker {

    private static final String[] EMOJIS = {
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
            "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
            "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
            "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "😣", "😖",
            "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬", "🤯",
            "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗", "🤔",
            "🤭", "🤫", "🤥", "😶", "😐", "😑", "😬", "🙄", "😯", "😦",
            "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😵", "🤐", "🥴",
            "🤢", "🤮", "🤧", "😷", "🤒", "🤕", "🤑", "🤠", "😈", "👿",
            "👋", "🤚", "🖐", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞",
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
            "👍", "👎", "👊", "✊", "🤛", "🤜", "👏", "🙌", "👐", "🤲"
    };

    public static void show(Window owner, double x, double y, Consumer<String> onEmojiSelected) {
        Popup popup = new Popup();

        VBox container = new VBox(10);
        container.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5); -fx-padding: 12;");
        container.setMaxWidth(350);

        Text title = new Text("Choisir un émoji");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        FlowPane emojiGrid = new FlowPane();
        emojiGrid.setHgap(8);
        emojiGrid.setVgap(8);
        emojiGrid.setAlignment(Pos.CENTER);

        for (String emoji : EMOJIS) {
            Button emojiBtn = new Button(emoji);
            emojiBtn.setStyle("-fx-font-size: 20px; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;");
            emojiBtn.setOnMouseEntered(e -> emojiBtn.setStyle("-fx-font-size: 20px; -fx-background-color: #f1f5f9; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5;"));
            emojiBtn.setOnMouseExited(e -> emojiBtn.setStyle("-fx-font-size: 20px; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;"));
            emojiBtn.setOnAction(e -> {
                onEmojiSelected.accept(emoji);
                popup.hide();
            });
            emojiGrid.getChildren().add(emojiBtn);
        }

        ScrollPane scrollPane = new ScrollPane(emojiGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(300);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        container.getChildren().addAll(title, scrollPane);
        popup.getContent().add(container);
        popup.show(owner, x, y);
    }
}