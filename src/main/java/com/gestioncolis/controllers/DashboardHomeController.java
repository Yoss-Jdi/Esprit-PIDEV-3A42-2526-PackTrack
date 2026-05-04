package com.gestioncolis.controllers;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.enums.Role;
import com.gestioncolis.services.UtilisateursServices;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardHomeController implements Initializable,
        DashboardController.UserAware {

    // ── Welcome ────────────────────────────────────────────────────────────
    @FXML private Label welcomeTitle;
    @FXML private Label welcomeDate;

    // ── KPI cards ──────────────────────────────────────────────────────────
    @FXML private Label kpiTotal;
    @FXML private Label kpiAdmins;
    @FXML private Label kpiClients;
    @FXML private Label kpiLivreurs;
    @FXML private Label kpiTotalTrend;
    @FXML private Label kpiAdminsPct;
    @FXML private Label kpiClientsPct;
    @FXML private Label kpiLivreursPct;
    @FXML private HBox  kpiTotalBar;
    @FXML private HBox  kpiAdminsBar;
    @FXML private HBox  kpiClientsBar;
    @FXML private HBox  kpiLivreursBar;

    // ── Charts ────────────────────────────────────────────────────────────
    @FXML private Canvas donutCanvas;
    @FXML private Canvas barCanvas;
    @FXML private VBox   donutCenter;
    @FXML private Label  donutCenterValue;
    @FXML private VBox   donutLegend;

    // ── Lists ─────────────────────────────────────────────────────────────
    @FXML private VBox recentList;
    @FXML private VBox roleBarList;

    // ── State ─────────────────────────────────────────────────────────────
    private Utilisateurs currentUser;
    private final UtilisateursServices service = new UtilisateursServices();

    private static final Color[] ROLE_COLORS = {
            Color.web("#6366f1"), // ADMIN
            Color.web("#3b82f6"), // CLIENT
            Color.web("#10b981"), // ENTREPRISE
            Color.web("#f59e0b"), // LIVREUR
            Color.web("#ef4444")  // TECHNICIEN
    };
    private static final String[] ROLE_HEX = {
            "#6366f1", "#3b82f6", "#10b981", "#f59e0b", "#ef4444"
    };

    // ═════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        welcomeDate.setText(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH)));
        loadAndRender();
    }

    @Override
    public void setCurrentUser(Utilisateurs user) {
        this.currentUser = user;
        if (user != null)
            welcomeTitle.setText("Bonjour, " + user.getPrenom() + " 👋");
    }

    // ═════════════════════════════════════════════════════════════════════
    private void loadAndRender() {
        new Thread(() -> {
            try {
                List<Utilisateurs> users = service.afficher();
                Platform.runLater(() -> render(users));
            } catch (SQLException e) {
                System.err.println("❌ DashboardHome: " + e.getMessage());
            }
        }).start();
    }

    private void render(List<Utilisateurs> users) {
        int total     = users.size();
        long admins   = users.stream().filter(u -> u.getRole() == Role.ADMIN).count();
        long clients  = users.stream().filter(u -> u.getRole() == Role.CLIENT).count();
        long livreurs = users.stream().filter(u -> u.getRole() == Role.LIVREUR).count();
        long entreprises = users.stream().filter(u -> u.getRole() == Role.ENTREPRISE).count();
        long techniciens = users.stream().filter(u -> u.getRole() == Role.TECHNICIEN).count();

        // ── KPI numbers (animated count-up) ───────────────────────────────
        animateCount(kpiTotal,    total);
        animateCount(kpiAdmins,   (int) admins);
        animateCount(kpiClients,  (int) clients);
        animateCount(kpiLivreurs, (int) livreurs);
        donutCenterValue.setText(String.valueOf(total));

        // ── Percentages ───────────────────────────────────────────────────
        kpiTotalTrend  .setText("↑ " + total + " comptes");
        kpiAdminsPct   .setText(pct(admins,   total) + "%");
        kpiClientsPct  .setText(pct(clients,  total) + "%");
        kpiLivreursPct .setText(pct(livreurs, total) + "%");

        // ── KPI progress bars ──────────────────────────────────────────────
        animateBar(kpiTotalBar,    1.0,                        800);
        animateBar(kpiAdminsBar,   ratio(admins,   total),     900);
        animateBar(kpiClientsBar,  ratio(clients,  total),    1000);
        animateBar(kpiLivreursBar, ratio(livreurs, total),    1100);

        // ── Donut chart ───────────────────────────────────────────────────
        long[] counts = { admins, clients, entreprises, livreurs, techniciens };
        String[] names = { "Admin", "Client", "Entreprise", "Livreur", "Technicien" };
        drawDonutAnimated(donutCanvas, counts, names, total);
        buildDonutLegend(counts, names, total);

        // ── Bar chart ─────────────────────────────────────────────────────
        Map<String, Long> byMonth = buildMonthlyMap(users);
        drawBarChartAnimated(barCanvas, byMonth);

        // ── Recent list ───────────────────────────────────────────────────
        List<Utilisateurs> recent = users.stream()
                .filter(u -> u.getCreatedAt() != null)
                .sorted(Comparator.comparing(Utilisateurs::getCreatedAt).reversed())
                .limit(5)
                .collect(Collectors.toList());
        buildRecentList(recent);

        // ── Role bars ─────────────────────────────────────────────────────
        buildRoleBars(counts, names, total);
    }

    // ═════════════════════════════════════════════════════════════════════
    // COUNT-UP ANIMATION
    // ═════════════════════════════════════════════════════════════════════
    private void animateCount(Label label, int target) {
        final long startTime = System.nanoTime();
        final long duration  = 900_000_000L; // 900 ms
        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) {
                double progress = Math.min(1.0, (double)(now - startTime) / duration);
                double eased    = easeOutCubic(progress);
                label.setText(String.valueOf((int)(eased * target)));
                if (progress >= 1.0) { label.setText(String.valueOf(target)); stop(); }
            }
        };
        timer.start();
    }

    private double easeOutCubic(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    // ═════════════════════════════════════════════════════════════════════
    // PROGRESS BAR ANIMATION
    // ═════════════════════════════════════════════════════════════════════
    private void animateBar(HBox bar, double targetRatio, int delayMs) {
        bar.setPrefWidth(0);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(delayMs),
                        new KeyValue(bar.prefWidthProperty(), 0)),
                new KeyFrame(Duration.millis(delayMs + 600),
                        e -> {},
                        new KeyValue(bar.prefWidthProperty(), 999)) // will be capped by parent
        );
        // Use a simpler width binding via maxWidth trick
        bar.setMaxWidth(Region.USE_PREF_SIZE);
        Timeline anim = new Timeline(
                new KeyFrame(Duration.ZERO,    new KeyValue(bar.prefWidthProperty(), 0)),
                new KeyFrame(Duration.millis(delayMs), new KeyValue(bar.prefWidthProperty(), 0)),
                new KeyFrame(Duration.millis(delayMs + 700),
                        new KeyValue(bar.prefWidthProperty(), targetRatio * 100))
        );
        bar.setMaxWidth(Double.MAX_VALUE);
        // Use percent-width styling instead
        bar.setStyle(bar.getStyle());

        // Bind width proportionally after a delay
        new Thread(() -> {
            try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                bar.setMaxWidth(Region.USE_PREF_SIZE);
                Timeline t2 = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(bar.prefWidthProperty(), 0)),
                        new KeyFrame(Duration.millis(700),
                                new KeyValue(bar.prefWidthProperty(),
                                        targetRatio * bar.getParent().getBoundsInLocal().getWidth()))
                );
                // Fallback: just set a proportion of a reasonable max
                double maxPx = 260;
                Timeline t3 = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(bar.prefWidthProperty(), 0)),
                        new KeyFrame(Duration.millis(700),
                                new KeyValue(bar.prefWidthProperty(), targetRatio * maxPx))
                );
                t3.play();
            });
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════════
    // DONUT CHART
    // ═════════════════════════════════════════════════════════════════════
    private void drawDonutAnimated(Canvas canvas, long[] counts, String[] names, int total) {
        if (total == 0) { drawEmptyDonut(canvas); return; }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        double cx = w / 2, cy = h / 2;
        double outerR = Math.min(w, h) / 2 - 10;
        double innerR = outerR * 0.58;

        // Pre-compute angles
        double[] angles = new double[counts.length];
        for (int i = 0; i < counts.length; i++)
            angles[i] = (double) counts[i] / total * 360.0;

        final long startNs = System.nanoTime();
        final long durNs   = 900_000_000L;
        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) {
                double progress = Math.min(1.0, (double)(now - startNs) / durNs);
                double eased    = easeOutCubic(progress);
                gc.clearRect(0, 0, w, h);

                double startAngle = -90;
                for (int i = 0; i < counts.length; i++) {
                    if (counts[i] == 0) continue;
                    double sweep = angles[i] * eased;
                    gc.setFill(ROLE_COLORS[i]);
                    gc.fillArc(cx - outerR, cy - outerR, outerR * 2, outerR * 2,
                               startAngle, sweep, javafx.scene.shape.ArcType.ROUND);
                    startAngle += angles[i];
                }
                // White inner circle (donut hole)
                gc.setFill(Color.WHITE);
                gc.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

                // Subtle ring border
                gc.setStroke(Color.web("#f1f5f9"));
                gc.setLineWidth(2);
                gc.strokeOval(cx - outerR, cy - outerR, outerR * 2, outerR * 2);
                gc.strokeOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

                if (progress >= 1.0) stop();
            }
        };
        timer.start();
    }

    private void drawEmptyDonut(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        double cx = w/2, cy = h/2, r = Math.min(w,h)/2 - 10;
        gc.setFill(Color.web("#f1f5f9"));
        gc.fillOval(cx-r, cy-r, r*2, r*2);
        gc.setFill(Color.WHITE);
        gc.fillOval(cx-r*0.58, cy-r*0.58, r*0.58*2, r*0.58*2);
    }

    private void buildDonutLegend(long[] counts, String[] names, int total) {
        donutLegend.getChildren().clear();
        for (int i = 0; i < names.length; i++) {
            if (counts[i] == 0) continue;
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Circle dot = new Circle(6);
            dot.setFill(Color.web(ROLE_HEX[i]));
            Label name  = new Label(names[i]);
            name.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
            name.setPrefWidth(80);
            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            Label count = new Label(counts[i] + " (" + pct(counts[i], total) + "%)");
            count.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e1b4b;");
            row.getChildren().addAll(dot, name, spacer, count);
            donutLegend.getChildren().add(row);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // BAR CHART
    // ═════════════════════════════════════════════════════════════════════
    private Map<String, Long> buildMonthlyMap(List<Utilisateurs> users) {
        Locale fr = Locale.FRENCH;
        Map<String, Long> result = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate m = now.minusMonths(i);
            String key = m.getMonth().getDisplayName(TextStyle.SHORT, fr)
                    + " " + m.getYear();
            result.put(key, 0L);
        }
        for (Utilisateurs u : users) {
            if (u.getCreatedAt() == null) continue;
            LocalDate d = u.getCreatedAt().toLocalDate();
            String key = d.getMonth().getDisplayName(TextStyle.SHORT, fr)
                    + " " + d.getYear();
            if (result.containsKey(key))
                result.merge(key, 1L, Long::sum);
        }
        return result;
    }

    private void drawBarChartAnimated(Canvas canvas, Map<String, Long> data) {
        double w = canvas.getWidth(), h = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double padL = 40, padR = 20, padT = 20, padB = 40;
        double chartW = w - padL - padR;
        double chartH = h - padT - padB;
        String[] labels = data.keySet().toArray(new String[0]);
        long[] values   = data.values().stream().mapToLong(Long::longValue).toArray();
        long maxVal = Arrays.stream(values).max().orElse(1);
        if (maxVal == 0) maxVal = 1;

        int n = labels.length;
        double barW  = chartW / n * 0.55;
        double gap   = chartW / n;

        final long startNs = System.nanoTime();
        final long durNs   = 800_000_000L;
        final long maxV    = maxVal;

        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) {
                double progress = Math.min(1.0, (double)(now - startNs) / durNs);
                double eased    = easeOutCubic(progress);
                gc.clearRect(0, 0, w, h);

                // Grid lines
                gc.setStroke(Color.web("#f1f5f9"));
                gc.setLineWidth(1);
                for (int g = 0; g <= 4; g++) {
                    double y = padT + chartH - (chartH / 4.0 * g);
                    gc.strokeLine(padL, y, padL + chartW, y);
                    gc.setFill(Color.web("#94a3b8"));
                    gc.setFont(Font.font("Segoe UI", 10));
                    gc.fillText(String.valueOf(maxV / 4 * g), 2, y + 4);
                }

                // Bars
                for (int i = 0; i < n; i++) {
                    double x    = padL + gap * i + (gap - barW) / 2;
                    double barH = (double) values[i] / maxV * chartH * eased;
                    double y    = padT + chartH - barH;

                    // Gradient fill
                    LinearGradient grad = new LinearGradient(
                            0, y, 0, padT + chartH, false, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.web("#6366f1")),
                            new Stop(1, Color.web("#a5b4fc")));
                    gc.setFill(grad);
                    // Rounded top rect (manual)
                    double arc = Math.min(barW / 2, 8);
                    gc.fillRoundRect(x, y, barW, barH, arc * 2, arc * 2);

                    // Value label on top
                    if (progress >= 1.0 && values[i] > 0) {
                        gc.setFill(Color.web("#4f46e5"));
                        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                        gc.fillText(String.valueOf(values[i]),
                                x + barW / 2 - 5, y - 4);
                    }

                    // X-axis label
                    gc.setFill(Color.web("#64748b"));
                    gc.setFont(Font.font("Segoe UI", 10));
                    String lbl = labels[i].split(" ")[0]; // just month abbrev
                    gc.fillText(lbl, x + barW / 2 - lbl.length() * 3,
                            padT + chartH + 16);
                }

                // X-axis line
                gc.setStroke(Color.web("#e2e8f0"));
                gc.setLineWidth(1.5);
                gc.strokeLine(padL, padT + chartH, padL + chartW, padT + chartH);

                if (progress >= 1.0) stop();
            }
        };
        timer.start();
    }

    // ═════════════════════════════════════════════════════════════════════
    // RECENT LIST
    // ═════════════════════════════════════════════════════════════════════
    private void buildRecentList(List<Utilisateurs> users) {
        recentList.getChildren().clear();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (int i = 0; i < users.size(); i++) {
            Utilisateurs u = users.get(i);
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 16, 10, 16));
            if (i < users.size() - 1)
                row.setStyle("-fx-border-color: transparent transparent #f1f5f9 transparent; -fx-border-width: 0 0 1 0;");

            // Avatar initials
            StackPane av = new StackPane();
            av.setMinSize(36, 36); av.setMaxSize(36, 36);
            av.setStyle("-fx-background-radius: 18; -fx-background-color: "
                    + ROLE_HEX[roleIndex(u.getRole())] + ";");
            String init = "";
            if (u.getPrenom() != null && !u.getPrenom().isEmpty()) init += u.getPrenom().charAt(0);
            if (u.getNom()    != null && !u.getNom().isEmpty())    init += u.getNom().charAt(0);
            Label initLbl = new Label(init.toUpperCase());
            initLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
            av.getChildren().add(initLbl);

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label name = new Label(u.getPrenom() + " " + u.getNom());
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e1b4b;");
            Label email = new Label(u.getEmail());
            email.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            info.getChildren().addAll(name, email);

            VBox right = new VBox(2);
            right.setAlignment(Pos.CENTER_RIGHT);
            Label roleLbl = new Label(u.getRole() != null ? u.getRole().name() : "—");
            roleLbl.setStyle("-fx-background-radius: 12; -fx-padding: 2 8 2 8; "
                    + "-fx-font-size: 10px; -fx-font-weight: bold; "
                    + "-fx-background-color: " + ROLE_HEX[roleIndex(u.getRole())] + "22; "
                    + "-fx-text-fill: " + ROLE_HEX[roleIndex(u.getRole())] + ";");
            Label dateLbl = new Label(u.getCreatedAt() != null ? u.getCreatedAt().format(fmt) : "");
            dateLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1;");
            right.getChildren().addAll(roleLbl, dateLbl);

            row.getChildren().addAll(av, info, right);

            // Fade in staggered
            row.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(300), row);
            ft.setDelay(Duration.millis(i * 80));
            ft.setFromValue(0); ft.setToValue(1); ft.play();
            recentList.getChildren().add(row);
        }
        if (users.isEmpty()) {
            Label empty = new Label("Aucune inscription récente");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-padding: 20;");
            recentList.getChildren().add(empty);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // ROLE HORIZONTAL BARS
    // ═════════════════════════════════════════════════════════════════════
    private void buildRoleBars(long[] counts, String[] names, int total) {
        roleBarList.getChildren().clear();
        for (int i = 0; i < names.length; i++) {
            VBox item = new VBox(6);
            HBox header = new HBox();
            Label name = new Label(names[i]);
            name.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151; -fx-font-weight: bold;");
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            Label cnt = new Label(counts[i] + "  (" + pct(counts[i], total) + "%)");
            cnt.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            header.getChildren().addAll(name, sp, cnt);

            StackPane track = new StackPane();
            track.setPrefHeight(8);
            track.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 4;");
            HBox fill = new HBox();
            fill.setPrefHeight(8);
            fill.setMaxWidth(Region.USE_PREF_SIZE);
            fill.setStyle("-fx-background-color: " + ROLE_HEX[i] + "; -fx-background-radius: 4;");
            fill.setPrefWidth(0);
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            track.getChildren().add(fill);

            item.getChildren().addAll(header, track);
            roleBarList.getChildren().add(item);

            // Animate bar after layout
            double targetRatio = total > 0 ? (double) counts[i] / total : 0;
            int delay = i * 120 + 400;
            new Thread(() -> {
                try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    double maxPx = 220;
                    Timeline tl = new Timeline(
                            new KeyFrame(Duration.ZERO, new KeyValue(fill.prefWidthProperty(), 0)),
                            new KeyFrame(Duration.millis(600),
                                    new KeyValue(fill.prefWidthProperty(), targetRatio * maxPx))
                    );
                    tl.play();
                });
            }).start();
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════
    private int pct(long part, long total) {
        if (total == 0) return 0;
        return (int) Math.round(100.0 * part / total);
    }
    private double ratio(long part, long total) {
        return total == 0 ? 0 : (double) part / total;
    }
    private int roleIndex(Role role) {
        if (role == null) return 0;
        return switch (role) {
            case ADMIN      -> 0;
            case CLIENT     -> 1;
            case ENTREPRISE -> 2;
            case LIVREUR    -> 3;
            case TECHNICIEN -> 4;
        };
    }
}
