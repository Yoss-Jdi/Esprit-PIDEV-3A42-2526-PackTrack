package com.esprit.controllers;

import com.esprit.utils.MyDataBase;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RecompenseStatsController {

    @FXML private BarChart<String, Number> barChart;
    @FXML private GridPane calendarGrid;
    @FXML private Label lblMonthYear;
    @FXML private Label lblSelectedDate;
    @FXML private Label lblChartTitle;
    @FXML private Label lblTotalCount;
    @FXML private Label lblLivreurCount;
    @FXML private Button btnClearDate;
    @FXML private HBox legendBox;

    private YearMonth currentMonth = YearMonth.now();
    private LocalDate selectedDate = null;

    // Couleurs heatmap (blanc → vert foncé)
    private static final String[] HEATMAP_COLORS = {
            "#edf2f7",  // 0 récompenses
            "#c6f6d5",  // faible
            "#68d391",  // moyen-faible
            "#38a169",  // moyen-fort
            "#276749"   // élevé
    };

    @FXML
    public void initialize() {
        buildLegend();
        buildCalendar();
        loadStats(null);
    }

    // ─── CALENDRIER ────────────────────────────────────────────────

    private void buildCalendar() {
        calendarGrid.getChildren().clear();

        // En-têtes
        String[] jours = {"Lu", "Ma", "Me", "Je", "Ve", "Sa", "Di"};
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(jours[i]);
            lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#a0aec0; -fx-font-weight:bold;");
            lbl.setMinWidth(34);
            lbl.setAlignment(Pos.CENTER);
            calendarGrid.add(lbl, i, 0);
        }

        // Mise à jour du label mois/année
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
        lblMonthYear.setText(currentMonth.format(fmt));

        // Récupérer les données du mois
        Map<LocalDate, Integer> dataMap = fetchMonthData(currentMonth);
        int maxVal = dataMap.values().stream().mapToInt(v -> v).max().orElse(1);

        // Remplir les jours
        LocalDate first = currentMonth.atDay(1);
        int startCol = first.getDayOfWeek().getValue() - 1; // Lundi = 0

        int totalDays = currentMonth.lengthOfMonth();
        for (int day = 1; day <= totalDays; day++) {
            LocalDate date = currentMonth.atDay(day);
            int count = dataMap.getOrDefault(date, 0);

            // Calcul de la couleur
            int colorIdx = 0;
            if (count > 0) {
                colorIdx = Math.min(1 + (int) ((double) count / maxVal * 3), 4);
            }
            String color = HEATMAP_COLORS[colorIdx];

            // Cellule du jour
            Label cell = new Label(String.valueOf(day));
            cell.setMinSize(34, 30);
            cell.setMaxSize(34, 30);
            cell.setAlignment(Pos.CENTER);
            cell.setStyle(
                    "-fx-background-color:" + color + ";" +
                            "-fx-background-radius:6;" +
                            "-fx-font-size:12px;" +
                            "-fx-cursor:hand;" +
                            (date.equals(selectedDate)
                                    ? "-fx-border-color:#3182ce; -fx-border-radius:6; -fx-border-width:2; -fx-font-weight:bold;"
                                    : "") +
                            (date.equals(LocalDate.now())
                                    ? "-fx-font-weight:bold;"
                                    : "")
            );

            // Tooltip
            String tooltipText = count > 0
                    ? count + " récompense" + (count > 1 ? "s" : "")
                    : "Aucune récompense";
            Tooltip.install(cell, new Tooltip(
                    date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " — " + tooltipText
            ));

            // Clic sur un jour
            cell.setOnMouseClicked(e -> {
                selectedDate = date;
                String dateStr = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH));
                lblSelectedDate.setText("Filtré : " + dateStr);
                btnClearDate.setVisible(true);
                buildCalendar(); // Rafraîchir pour afficher la sélection
                loadStats(date);
            });

            int col = (startCol + day - 1) % 7;
            int row = 1 + (startCol + day - 1) / 7;
            calendarGrid.add(cell, col, row);
        }
    }

    private Map<LocalDate, Integer> fetchMonthData(YearMonth month) {
        Map<LocalDate, Integer> map = new HashMap<>();
        try {
            Connection cnx = MyDataBase.getInstance().getConnection();
            PreparedStatement ps = cnx.prepareStatement(
                    "SELECT DATE(date_attribution) AS jour, COUNT(*) AS total " +
                            "FROM recompenses " +
                            "WHERE YEAR(date_attribution) = ? AND MONTH(date_attribution) = ? " +
                            "GROUP BY DATE(date_attribution)"
            );
            ps.setInt(1, month.getYear());
            ps.setInt(2, month.getMonthValue());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LocalDate d = rs.getDate("jour").toLocalDate();
                map.put(d, rs.getInt("total"));
            }
        } catch (Exception e) {
            System.out.println("Erreur heatmap: " + e.getMessage());
        }
        return map;
    }

    // ─── GRAPHIQUE ─────────────────────────────────────────────────

    private void loadStats(LocalDate date) {
        barChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Récompenses par livreur");

        int total = 0;
        int livreurCount = 0;

        try {
            Connection cnx = MyDataBase.getInstance().getConnection();

            String sql;
            PreparedStatement ps;

            if (date != null) {
                sql = "SELECT u.nom, u.prenom, COUNT(*) AS total " +
                        "FROM recompenses r " +
                        "JOIN utilisateurs u ON r.livreur_id = u.id_utilisateur " +
                        "WHERE DATE(r.date_attribution) = ? " +
                        "GROUP BY u.nom, u.prenom";
                ps = cnx.prepareStatement(sql);
                ps.setDate(1, Date.valueOf(date));
            } else {
                sql = "SELECT u.nom, u.prenom, COUNT(*) AS total " +
                        "FROM recompenses r " +
                        "JOIN utilisateurs u ON r.livreur_id = u.id_utilisateur " +
                        "GROUP BY u.nom, u.prenom";
                ps = cnx.prepareStatement(sql);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String livreur = rs.getString("nom") + " " + rs.getString("prenom");
                int count = rs.getInt("total");
                series.getData().add(new XYChart.Data<>(livreur, count));
                total += count;
                livreurCount++;
            }

        } catch (Exception e) {
            System.out.println("Erreur stats: " + e.getMessage());
        }

        barChart.getData().add(series);
        lblTotalCount.setText(String.valueOf(total));
        lblLivreurCount.setText(String.valueOf(livreurCount));

        // Titre dynamique
        if (date != null) {
            String dateStr = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            lblChartTitle.setText("Récompenses du " + dateStr);
        } else {
            lblChartTitle.setText("Récompenses — Toutes les dates");
        }
    }

    // ─── NAVIGATION ────────────────────────────────────────────────

    @FXML
    private void previousMonth() {
        currentMonth = currentMonth.minusMonths(1);
        buildCalendar();
    }

    @FXML
    private void nextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        buildCalendar();
    }

    @FXML
    private void clearDateFilter() {
        selectedDate = null;
        lblSelectedDate.setText("Toutes les dates");
        btnClearDate.setVisible(false);
        buildCalendar();
        loadStats(null);
    }

    // ─── LÉGENDE ───────────────────────────────────────────────────

    private void buildLegend() {
        legendBox.getChildren().clear();
        for (String color : HEATMAP_COLORS) {
            Rectangle r = new Rectangle(14, 14);
            r.setFill(Color.web(color));
            r.setArcWidth(4);
            r.setArcHeight(4);
            r.setStyle("-fx-stroke:#cbd5e0; -fx-stroke-width:0.5;");
            legendBox.getChildren().add(r);
        }
    }
}