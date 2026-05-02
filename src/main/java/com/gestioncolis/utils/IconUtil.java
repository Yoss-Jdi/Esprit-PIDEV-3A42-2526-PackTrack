package com.gestioncolis.utils;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class IconUtil {

    public static FontIcon createIcon(FontAwesomeSolid icon, int size, Color color) {
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(size);
        fontIcon.setIconColor(color);
        addHoverAnimation(fontIcon);
        return fontIcon;
    }

    public static FontIcon createIcon(MaterialDesign icon, int size, Color color) {
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(size);
        fontIcon.setIconColor(color);
        addHoverAnimation(fontIcon);
        return fontIcon;
    }

    public static FontIcon createIcon(FontAwesomeRegular icon, int size, Color color) {
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(size);
        fontIcon.setIconColor(color);
        addHoverAnimation(fontIcon);
        return fontIcon;
    }

    private static void addHoverAnimation(FontIcon icon) {
        icon.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), icon);
            st.setToX(1.15);
            st.setToY(1.15);
            st.play();

            FadeTransition ft = new FadeTransition(Duration.millis(150), icon);
            ft.setToValue(0.8);
            ft.play();
        });

        icon.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), icon);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();

            FadeTransition ft = new FadeTransition(Duration.millis(150), icon);
            ft.setToValue(1.0);
            ft.play();
        });
    }
}