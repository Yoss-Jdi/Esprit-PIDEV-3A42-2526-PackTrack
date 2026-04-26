package com.gestioncolis.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.enums.Role;
import com.gestioncolis.services.UtilisateursServices;
import com.gestioncolis.utils.DialogUtil;
import com.gestioncolis.utils.PhotoManager;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import com.gestioncolis.utils.ExportManager;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import javafx.stage.Stage;
import java.util.ArrayList;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.Interpolator;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import javafx.scene.paint.Color;

public class UsersController implements Initializable,
        DashboardController.UserAware {

    // ─── Table ─────────────────────────────────────────────────────────────
    @FXML private TableView<Utilisateurs>            usersTable;
    @FXML private TableColumn<Utilisateurs, Void>    colPhoto;
    @FXML private TableColumn<Utilisateurs, String>  colPrenom;
    @FXML private TableColumn<Utilisateurs, String>  colNom;
    @FXML private TableColumn<Utilisateurs, String>  colEmail;
    @FXML private TableColumn<Utilisateurs, String>  colTelephone;
    @FXML private TableColumn<Utilisateurs, String>  colRole;
    @FXML private TableColumn<Utilisateurs, String>  colCreatedAt;
    @FXML private TableColumn<Utilisateurs, Void>    colActions;

    // ─── Filters ───────────────────────────────────────────────────────────
    @FXML private TextField         searchField;
    @FXML private ComboBox<String>  filterRole;
    @FXML private ComboBox<String>  filterDate;
    @FXML private Button            btnResetFilters;

    // ─── Stats ─────────────────────────────────────────────────────────────
    @FXML private Label statTotal;
    @FXML private Label statAdmins;
    @FXML private Label statClients;
    @FXML private Label statLivreurs;
    @FXML private Label notifLabel;
    @FXML private Label tableSubtitle;
    @FXML private Label emptySubtitle;

    // ─── Pagination ────────────────────────────────────────────────────────
    @FXML private Label             paginationInfo;
    @FXML private HBox              pageButtons;
    @FXML private Button            btnPrevPage;
    @FXML private Button            btnNextPage;
    @FXML private ComboBox<Integer> pageSizeCombo;

    // ─── State ─────────────────────────────────────────────────────────────
    private final UtilisateursServices service  = new UtilisateursServices();
    private ObservableList<Utilisateurs> allUsers = FXCollections.observableArrayList();
    private FilteredList<Utilisateurs>  filtered;
    private Utilisateurs currentUser;

    private int currentPage = 0;
    private int pageSize    = 10;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Cache pour les photos chargées
    private final java.util.Map<String, Image> photoCache = new java.util.concurrent.ConcurrentHashMap<>();

    // ═════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupFilters();
        setupColumns();
        loadUsers();
    }

    @Override
    public void setCurrentUser(Utilisateurs user) { this.currentUser = user; }

    // ═════════════════════════════════════════════════════════════════════
    // FILTER SETUP
    // ═════════════════════════════════════════════════════════════════════
    private void setupFilters() {
        filterRole.getItems().add("Tous les rôles");
        for (Role r : Role.values()) filterRole.getItems().add(r.name());
        filterRole.setValue("Tous les rôles");

        filterDate.getItems().addAll(
                "Toutes les dates",
                "Aujourd'hui",
                "7 derniers jours",
                "30 derniers jours",
                "Cette année"
        );
        filterDate.setValue("Toutes les dates");

        pageSizeCombo.getItems().addAll(5, 10, 20, 50);
        pageSizeCombo.setValue(pageSize);
    }

    // ═════════════════════════════════════════════════════════════════════
    // COLUMNS
    // ═════════════════════════════════════════════════════════════════════
    private void setupColumns() {
        // Configuration des largeurs fixes pour chaque colonne
        colPhoto.setPrefWidth(60);
        colPhoto.setMinWidth(50);
        colPhoto.setMaxWidth(70);

        colPrenom.setPrefWidth(120);
        colPrenom.setMinWidth(100);

        colNom.setPrefWidth(120);
        colNom.setMinWidth(100);

        colEmail.setPrefWidth(220);
        colEmail.setMinWidth(180);

        colTelephone.setPrefWidth(130);
        colTelephone.setMinWidth(110);

        colRole.setPrefWidth(130);
        colRole.setMinWidth(110);

        colCreatedAt.setPrefWidth(110);
        colCreatedAt.setMinWidth(100);

        // Colonne Actions plus large pour accueillir les deux boutons
        colActions.setPrefWidth(220);
        colActions.setMinWidth(200);
        colActions.setMaxWidth(250);

        // Cell value factories
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));

        // ── Photo Column ─────────────────────────────────────────────────
        colPhoto.setCellFactory(col -> new TableCell<>() {
            private final StackPane pane    = new StackPane();
            private final ImageView imgView = new ImageView();
            private final Label     initLbl = new Label();

            {
                imgView.setFitWidth(36);
                imgView.setFitHeight(36);
                imgView.setPreserveRatio(false);
                Circle clip = new Circle(18, 18, 18);
                imgView.setClip(clip);

                pane.setPrefSize(36, 36);
                pane.setMaxSize(36, 36);
                pane.setMinSize(36, 36);

                initLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:white;");
                pane.getChildren().addAll(initLbl, imgView);
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                Utilisateurs u = getTableRow().getItem();
                if (u == null) {
                    setGraphic(null);
                    return;
                }

                loadUserPhoto(u, imgView, initLbl, pane);
                setGraphic(pane);
            }
        });

        // ── Role badge avec icône réelle ────────────────────────────────────
        colRole.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRole() != null
                        ? c.getValue().getRole().name() : "—"));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Utilisateurs user = getTableRow().getItem();
                if (user == null) {
                    setGraphic(null);
                    return;
                }

                HBox container = new HBox(8);
                container.setAlignment(Pos.CENTER);

                FontIcon roleIcon = getRoleIcon(user.getRole());
                Label roleLabel = new Label(role);
                roleLabel.getStyleClass().addAll("badge", "badge-" + role.toLowerCase());

                if (roleIcon != null) {
                    container.getChildren().add(roleIcon);
                }
                container.getChildren().add(roleLabel);

                setGraphic(container);
                setText(null);
            }
        });

        // ── Date ─────────────────────────────────────────────────────────
        colCreatedAt.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCreatedAt() != null
                        ? c.getValue().getCreatedAt().format(DATE_FMT) : "—"));

        // ── Actions Column améliorée avec animations et icônes réelles ──
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = new Button();
            private final Button deleteBtn = new Button();
            private final HBox   box       = new HBox(12);

            // Création des icônes réelles - utilisation de int pour la taille
            private final FontIcon editIcon = new FontIcon(FontAwesomeSolid.PEN);
            private final FontIcon deleteIcon = new FontIcon(FontAwesomeSolid.TRASH_ALT);

            {
                // Configuration des icônes - setIconSize prend un int
                editIcon.setIconSize(13);
                editIcon.setIconColor(Color.web("#ffffff"));

                deleteIcon.setIconSize(13);
                deleteIcon.setIconColor(Color.web("#ffffff"));

                // Style moderne pour le bouton Modifier
                editBtn.setText(" Modifier");
                editBtn.getStyleClass().addAll("btn-edit", "action-btn");
                editBtn.setPrefWidth(100);
                editBtn.setMinWidth(95);
                editBtn.setMaxWidth(100);
                editBtn.setPrefHeight(34);
                editBtn.setGraphic(editIcon);
                editBtn.setContentDisplay(ContentDisplay.LEFT);
                editBtn.setGraphicTextGap(8);

                // Style moderne pour le bouton Supprimer
                deleteBtn.setText(" Supprimer");
                deleteBtn.getStyleClass().addAll("btn-delete", "action-btn");
                deleteBtn.setPrefWidth(100);
                deleteBtn.setMinWidth(95);
                deleteBtn.setMaxWidth(100);
                deleteBtn.setPrefHeight(34);
                deleteBtn.setGraphic(deleteIcon);
                deleteBtn.setContentDisplay(ContentDisplay.LEFT);
                deleteBtn.setGraphicTextGap(8);

                // Animation de pulsation au survol des icônes
                addIconHoverAnimation(editIcon);
                addIconHoverAnimation(deleteIcon);

                box.setAlignment(Pos.CENTER);
                box.setPadding(new Insets(8, 10, 8, 10));
                box.getChildren().addAll(editBtn, deleteBtn);

                addButtonHoverAnimation(editBtn);
                addButtonHoverAnimation(deleteBtn);

                editBtn.setOnAction(e -> {
                    Utilisateurs user = getTableView().getItems().get(getIndex());
                    if (user != null) {
                        animateButtonClick(editBtn);
                        navigateToForm(user);
                    }
                });

                deleteBtn.setOnAction(e -> {
                    Utilisateurs user = getTableView().getItems().get(getIndex());
                    if (user != null) {
                        animateButtonClick(deleteBtn);
                        confirmDelete(user);
                    }
                });
            }

            private void addIconHoverAnimation(FontIcon icon) {
                icon.setOnMouseEntered(ev -> {
                    ScaleTransition st = new ScaleTransition(Duration.millis(150), icon);
                    st.setToX(1.2);
                    st.setToY(1.2);
                    st.play();
                });

                icon.setOnMouseExited(ev -> {
                    ScaleTransition st = new ScaleTransition(Duration.millis(150), icon);
                    st.setToX(1.0);
                    st.setToY(1.0);
                    st.play();
                });
            }

            private void addButtonHoverAnimation(Button btn) {
                btn.setOnMouseEntered(ev -> {
                    ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
                    st.setToX(1.02);
                    st.setToY(1.02);
                    st.play();
                });

                btn.setOnMouseExited(ev -> {
                    ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
                    st.setToX(1.0);
                    st.setToY(1.0);
                    st.play();
                });
            }

            private void animateButtonClick(Button btn) {
                ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
                st.setToX(0.96);
                st.setToY(0.96);
                st.setAutoReverse(true);
                st.setCycleCount(2);
                st.play();
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });

        // Configuration du tri
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        usersTable.setSortPolicy(tv -> {
            if (filtered == null) return true;
            applyPage();
            return true;
        });
    }

    private void loadUserPhoto(Utilisateurs u, ImageView imgView, Label initLbl, StackPane pane) {
        imgView.setVisible(false);
        imgView.setManaged(false);
        initLbl.setVisible(true);
        initLbl.setManaged(true);

        String photoPath = u.getPhoto();

        if (photoPath != null && !photoPath.isBlank()) {
            Image img = photoCache.get(photoPath);

            if (img == null) {
                Image loadedImg = PhotoManager.loadImage(photoPath);
                if (loadedImg != null && !loadedImg.isError()) {
                    photoCache.put(photoPath, loadedImg);
                    img = loadedImg;
                }
            }

            if (img != null && !img.isError()) {
                imgView.setImage(img);
                imgView.setVisible(true);
                imgView.setManaged(true);
                initLbl.setVisible(false);
                initLbl.setManaged(false);
                pane.setStyle("-fx-background-color:transparent;");
                return;
            }
        }

        String initials = "";
        if (u.getPrenom() != null && !u.getPrenom().isEmpty())
            initials += u.getPrenom().charAt(0);
        if (u.getNom() != null && !u.getNom().isEmpty())
            initials += u.getNom().charAt(0);
        initLbl.setText(initials.toUpperCase());
        pane.setStyle("-fx-background-color:" + roleColor(u.getRole()) + ";-fx-background-radius:18;");
    }

    private String roleColor(Role role) {
        if (role == null) return "#94a3b8";
        return switch (role) {
            case ADMIN      -> "#7c3aed";
            case CLIENT     -> "#2563eb";
            case ENTREPRISE -> "#059669";
            case LIVREUR    -> "#d97706";
            case TECHNICIEN -> "#dc2626";
        };
    }

    private String roleEmoji(String role) {
        return switch (role) {
            case "ADMIN"      -> "🛡️";
            case "CLIENT"     -> "👤";
            case "ENTREPRISE" -> "🏢";
            case "LIVREUR"    -> "🚚";
            case "TECHNICIEN" -> "🔧";
            default           -> "👥";
        };
    }

    // ═════════════════════════════════════════════════════════════════════
    // DATA LOADING
    // ═════════════════════════════════════════════════════════════════════
    private void loadUsers() {
        try {
            List<Utilisateurs> list = service.afficher();
            allUsers = FXCollections.observableArrayList(list);
            filtered = new FilteredList<>(allUsers, p -> true);
            currentPage = 0;
            applyFiltersAndRefresh();
        } catch (SQLException e) {
            showNotif("❌ " + e.getMessage(), false);
        }
    }

    public void refreshUsers() {
        loadUsers();
    }

    // ═════════════════════════════════════════════════════════════════════
    // FILTER LOGIC
    // ═════════════════════════════════════════════════════════════════════
    @FXML
    private void handleSearch() {
        currentPage = 0;
        applyFiltersAndRefresh();
    }

    @FXML
    private void handleFilter() {
        currentPage = 0;
        applyFiltersAndRefresh();
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        filterRole.setValue("Tous les rôles");
        filterDate.setValue("Toutes les dates");
        currentPage = 0;
        applyFiltersAndRefresh();
    }

    private void applyFiltersAndRefresh() {
        String q       = searchField.getText().trim().toLowerCase();
        String role    = filterRole.getValue();
        String dateOpt = filterDate.getValue();

        boolean hasFilter = (!q.isEmpty())
                || (role != null && !role.equals("Tous les rôles"))
                || (dateOpt != null && !dateOpt.equals("Toutes les dates"));

        btnResetFilters.setVisible(hasFilter);
        btnResetFilters.setManaged(hasFilter);

        filtered.setPredicate(u -> {
            if (!q.isEmpty()) {
                boolean match = (u.getNom()    != null && u.getNom()   .toLowerCase().contains(q))
                        || (u.getPrenom() != null && u.getPrenom().toLowerCase().contains(q))
                        || (u.getEmail()  != null && u.getEmail() .toLowerCase().contains(q))
                        || (u.getRole()   != null && u.getRole()  .name().toLowerCase().contains(q))
                        || String.valueOf(u.getIdUtilisateur()).contains(q);
                if (!match) return false;
            }
            if (role != null && !role.equals("Tous les rôles")) {
                if (u.getRole() == null || !u.getRole().name().equals(role)) return false;
            }
            if (dateOpt != null && !dateOpt.equals("Toutes les dates")) {
                if (u.getCreatedAt() == null) return false;
                LocalDate d = u.getCreatedAt().toLocalDate();
                LocalDate today = LocalDate.now();
                boolean dateMatch = switch (dateOpt) {
                    case "Aujourd'hui"       -> d.isEqual(today);
                    case "7 derniers jours"  -> !d.isBefore(today.minusDays(6));
                    case "30 derniers jours" -> !d.isBefore(today.minusDays(29));
                    case "Cette année"       -> d.getYear() == today.getYear();
                    default -> true;
                };
                if (!dateMatch) return false;
            }
            return true;
        });

        int total = filtered.size();
        if (tableSubtitle != null) {
            tableSubtitle.setText(total + " utilisateur(s) " + (hasFilter ? "correspondent à votre recherche" : "au total"));
        }
        applyPage();
    }

    // ═════════════════════════════════════════════════════════════════════
    // PAGINATION
    // ═════════════════════════════════════════════════════════════════════
    private void applyPage() {
        if (filtered == null) return;

        int total   = filtered.size();
        int pages   = Math.max(1, (int) Math.ceil((double) total / pageSize));
        currentPage = Math.max(0, Math.min(currentPage, pages - 1));

        int from = currentPage * pageSize;
        int to   = Math.min(from + pageSize, total);

        ObservableList<Utilisateurs> page = FXCollections.observableArrayList();
        if (total > 0 && from < total) {
            page.addAll(filtered.subList(from, to));
        }

        SortedList<Utilisateurs> sorted = new SortedList<>(page);
        sorted.comparatorProperty().bind(usersTable.comparatorProperty());

        usersTable.setItems(sorted);
        animateTableRows();
        usersTable.refresh();

        paginationInfo.setText(total == 0 ? "Aucun résultat"
                : String.format("Affichage %d – %d sur %d", from + 1, to, total));

        btnPrevPage.setDisable(currentPage == 0);
        btnNextPage.setDisable(currentPage >= pages - 1);

        pageButtons.getChildren().clear();
        int start = Math.max(0, currentPage - 2);
        int end   = Math.min(pages, start + 5);
        for (int i = start; i < end; i++) {
            final int pg = i;
            Button b = new Button(String.valueOf(i + 1));
            b.getStyleClass().add(i == currentPage ? "btn-page-active" : "btn-page");
            b.setOnAction(e -> {
                currentPage = pg;
                applyPage();
                Platform.runLater(() -> usersTable.refresh());
            });
            pageButtons.getChildren().add(b);
        }
    }

    @FXML
    private void handlePrevPage() {
        currentPage--;
        applyPage();
        Platform.runLater(() -> usersTable.refresh());
    }

    @FXML
    private void handleNextPage() {
        currentPage++;
        applyPage();
        Platform.runLater(() -> usersTable.refresh());
    }

    @FXML
    private void handlePageSizeChange() {
        Integer val = pageSizeCombo.getValue();
        if (val != null) {
            pageSize = val;
            currentPage = 0;
            applyPage();
            Platform.runLater(() -> usersTable.refresh());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═════════════════════════════════════════════════════════════════════
    @FXML
    private void handleAdd() {
        navigateToForm(null);
    }

    private void navigateToForm(Utilisateurs existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/UsersFormView.fxml"));
            Node view = loader.load();
            UsersFormController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            ctrl.initForm(existing, () -> {
                loadUsers();
            });
            StackPane ca = findContentArea();
            if (ca != null) fadeSwap(ca, view);
        } catch (IOException e) {
            System.err.println("❌ " + e.getMessage());
        }
    }

    private void confirmDelete(Utilisateurs utilisateur) {
        String userName = utilisateur.getPrenom() + " " + utilisateur.getNom();
        String userInfo = utilisateur.getEmail();

        boolean confirmed = DialogUtil.showDeleteConfirmation(userName, userInfo);

        if (confirmed) {
            try {
                service.supprimer(utilisateur.getIdUtilisateur());
                showNotif("✅ Utilisateur « " + userName + " » supprimé avec succès.", true);
                if (utilisateur.getPhoto() != null && !utilisateur.getPhoto().isBlank()) {
                    photoCache.remove(utilisateur.getPhoto());
                }
                loadUsers();
            } catch (SQLException e) {
                showNotif("❌ Erreur lors de la suppression : " + e.getMessage(), false);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════
    private void showNotif(String msg, boolean success) {
        notifLabel.setText(msg);
        notifLabel.getStyleClass().removeAll("notif-success", "notif-error");
        notifLabel.getStyleClass().add(success ? "notif-success" : "notif-error");
        notifLabel.setVisible(true);
        notifLabel.setManaged(true);
        notifLabel.setOpacity(0);
        FadeTransition fi = new FadeTransition(Duration.millis(300), notifLabel);
        fi.setFromValue(0);
        fi.setToValue(1);
        fi.play();
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                FadeTransition fo = new FadeTransition(Duration.millis(400), notifLabel);
                fo.setFromValue(1);
                fo.setToValue(0);
                fo.setOnFinished(e -> {
                    notifLabel.setVisible(false);
                    notifLabel.setManaged(false);
                });
                fo.play();
            });
        }).start();
    }

    private StackPane findContentArea() {
        return (StackPane) usersTable.getScene().getRoot().lookup("#contentArea");
    }

    private void fadeSwap(StackPane area, Node newNode) {
        if (area.getChildren().isEmpty()) {
            area.getChildren().add(newNode);
            return;
        }
        Node old = area.getChildren().get(0);
        FadeTransition fo = new FadeTransition(Duration.millis(150), old);
        fo.setToValue(0);
        fo.setOnFinished(e -> {
            area.getChildren().setAll(newNode);
            newNode.setOpacity(0);
            FadeTransition fi2 = new FadeTransition(Duration.millis(250), newNode);
            fi2.setFromValue(0);
            fi2.setToValue(1);
            fi2.play();
        });
        fo.play();
    }

    // ─── Export methods ─────────────────────────────────────────────────────────
    @FXML
    private void handleExportPDF() {
        if (filtered == null || filtered.isEmpty()) {
            showNotif("📭 Aucun utilisateur à exporter.", false);
            return;
        }

        int count = filtered.size();
        String details = getFilterDetails();

        boolean confirmed = DialogUtil.showExportConfirmation("PDF", count, details);

        if (confirmed) {
            ExportManager.exportToPDF(new ArrayList<>(filtered),
                    (Stage) usersTable.getScene().getWindow());
        }
    }

    @FXML
    private void handleExportCSV() {
        if (filtered == null || filtered.isEmpty()) {
            showNotif("📭 Aucun utilisateur à exporter.", false);
            return;
        }

        int count = filtered.size();
        String details = getFilterDetails();

        boolean confirmed = DialogUtil.showExportConfirmation("CSV", count, details);

        if (confirmed) {
            ExportManager.exportToCSV(new ArrayList<>(filtered),
                    (Stage) usersTable.getScene().getWindow());
        }
    }

    /**
     * Génère un résumé des filtres actifs pour l'affichage
     */
    private String getFilterDetails() {
        String search = searchField.getText().trim();
        String role = filterRole.getValue();
        String date = filterDate.getValue();

        StringBuilder details = new StringBuilder();

        if (!search.isEmpty()) {
            details.append("🔍 Recherche : \"").append(search).append("\"\n");
        }
        if (role != null && !role.equals("Tous les rôles")) {
            details.append("🎭 Rôle : ").append(role).append("\n");
        }
        if (date != null && !date.equals("Toutes les dates")) {
            details.append("📅 Période : ").append(date).append("\n");
        }

        if (details.length() == 0) {
            details.append("📋 Tous les utilisateurs");
        } else {
            details.insert(0, "Filtres appliqués :\n");
        }

        return details.toString();
    }

    private void showExportWarning(String message) {
        Alert warning = new Alert(AlertType.WARNING);
        warning.setTitle("Export impossible");
        warning.setHeaderText(null);
        warning.setContentText(message);

        DialogPane dp = warning.getDialogPane();
        URL cssUrl = getClass().getResource("/css/dashboard.css");
        if (cssUrl != null) dp.getStylesheets().add(cssUrl.toExternalForm());

        warning.showAndWait();
    }

    /**
     * Anime l'apparition des lignes du tableau avec un effet staggered
     */
    private void animateTableRows() {
        if (usersTable.getItems() == null || usersTable.getItems().isEmpty()) return;

        Platform.runLater(() -> {
            int rowCount = usersTable.getItems().size();
            for (int i = 0; i < rowCount; i++) {
                final int index = i;
                Node row = usersTable.lookup(".table-row-cell");
                if (row != null) {
                    row.setOpacity(0);
                    row.setTranslateX(-20);

                    javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(index * 40));
                    pause.setOnFinished(e -> {
                        FadeTransition ft = new FadeTransition(Duration.millis(300), row);
                        ft.setFromValue(0);
                        ft.setToValue(1);

                        TranslateTransition tt = new TranslateTransition(Duration.millis(300), row);
                        tt.setFromX(-20);
                        tt.setToX(0);
                        tt.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));

                        new ParallelTransition(tt, ft).play();
                    });
                    pause.play();
                }
            }
        });
    }

    private FontIcon getRoleIcon(Role role) {
        if (role == null) return null;
        FontIcon icon;
        int size = 14;  // Utilisation de int au lieu de double

        switch (role) {
            case ADMIN:
                icon = new FontIcon(FontAwesomeSolid.SHIELD_ALT);
                icon.setIconColor(Color.web("#4f46e5"));
                break;
            case CLIENT:
                icon = new FontIcon(FontAwesomeSolid.USER);
                icon.setIconColor(Color.web("#2563eb"));
                break;
            case ENTREPRISE:
                icon = new FontIcon(FontAwesomeSolid.BUILDING);
                icon.setIconColor(Color.web("#059669"));
                break;
            case LIVREUR:
                icon = new FontIcon(FontAwesomeSolid.TRUCK);
                icon.setIconColor(Color.web("#d97706"));
                break;
            case TECHNICIEN:
                icon = new FontIcon(FontAwesomeSolid.WRENCH);
                icon.setIconColor(Color.web("#dc2626"));
                break;
            default:
                icon = new FontIcon(FontAwesomeSolid.USER_CIRCLE);
                icon.setIconColor(Color.web("#94a3b8"));
        }
        icon.setIconSize(size);
        return icon;
    }
}