package com.gestioncolis.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.enums.Role;
import com.gestioncolis.services.UtilisateursServices;
import com.gestioncolis.utils.PhotoManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

import org.kordamp.ikonli.javafx.FontIcon;

public class FaceLoginController implements Initializable {

    // ─── Seuil de correspondance (0.0 – 1.0) ─────────────────────────────────
    private static final double THRESHOLD = 0.55; // 55% minimum pour valider

    // ─── FXML — Blobs ────────────────────────────────────────────────────────
    @FXML private Circle blob1, blob2, blob3, blob4, blob5;

    // ─── FXML — Steps (left panel) ───────────────────────────────────────────
    @FXML private StackPane step1Circle, step2Circle, step3Circle;
    @FXML private Label     step1Label,  step2Label,  step3Label;

    // ─── FXML — Content stack ────────────────────────────────────────────────
    @FXML private StackPane contentStack;
    @FXML private VBox      emailStep, scanStep, resultStep;

    // ─── Step 1 ───────────────────────────────────────────────────────────────
    @FXML private TextField emailField;
    @FXML private Label     emailError;

    // ─── Step 2 ───────────────────────────────────────────────────────────────
    @FXML private ImageView webcamView;
    @FXML private Pane      webcamOverlay;
    @FXML private VBox      cameraLoadingBox;
    @FXML private Label     scanStatusLabel;
    @FXML private Circle    statusDot;
    @FXML private Label     matchPctLabel;
    @FXML private Pane      progressFill;
    @FXML private Label     progressPctLabel;
    @FXML private Button    btnCapture;

    // Biometric landmarks
    @FXML private Circle    dotEyeL, dotEyeR, dotNose, dotMouthL, dotMouthR;
    @FXML private Circle    dotChin, dotForehead, dotCheekL, dotCheekR;

    // ─── Step 3 ───────────────────────────────────────────────────────────────
    @FXML private StackPane resultIconContainer;
    @FXML private FontIcon  resultIcon;
    @FXML private Label     resultTitle, resultSubtitle;
    @FXML private Label     finalScoreLabel, detectedUserLabel, statusLabel;
    @FXML private Button    btnContinue;
    @FXML private VBox      matchDetailsCard;

    // ─── State ────────────────────────────────────────────────────────────────
    private final UtilisateursServices service = new UtilisateursServices();
    private Utilisateurs targetUser    = null;
    private Utilisateurs loggedInUser  = null;

    private FrameGrabber    grabber           = null;
    private OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
    private CascadeClassifier faceDetector    = null;
    private final AtomicBoolean cameraRunning = new AtomicBoolean(false);
    private Thread cameraThread               = null;
    private Mat capturedFrame                 = null;

    // Animations running
    private Timeline scanLineAnimation        = null;
    private Timeline landmarkAnimation        = null;
    private Timeline progressAnimation        = null;

    // ═════════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadFaceLoginCss();
        startBlobAnimations();
        showStep(1);
        loadFaceDetector();
    }

    @SuppressWarnings("unchecked")
    private void loadFaceLoginCss() {
        try {
            java.net.URL cssUrl = getClass().getResource("/css/face-login.css");
            if (cssUrl != null) {
                final String cssExternal = cssUrl.toExternalForm();
                javafx.beans.value.ChangeListener<javafx.scene.Scene>[] holder =
                        new javafx.beans.value.ChangeListener[1];
                holder[0] = (obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        if (!newScene.getStylesheets().contains(cssExternal))
                            newScene.getStylesheets().add(cssExternal);
                        emailStep.sceneProperty().removeListener(holder[0]);
                    }
                };
                emailStep.sceneProperty().addListener(holder[0]);
            } else {
                System.err.println("⚠️ face-login.css introuvable dans /css/");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur chargement face-login.css : " + e.getMessage());
        }
    }

    private void loadFaceDetector() {
        new Thread(() -> {
            try {
                URL cascadeUrl = getClass().getResource(
                        "/org/bytedeco/opencv/linux-x86_64/share/opencv4/haarcascades/"
                                + "haarcascade_frontalface_default.xml");

                if (cascadeUrl == null) {
                    cascadeUrl = getClass().getResource("/haarcascade_frontalface_default.xml");
                }

                if (cascadeUrl != null) {
                    File tmpCascade = File.createTempFile("haarcascade_", ".xml");
                    tmpCascade.deleteOnExit();
                    try (var in  = cascadeUrl.openStream();
                         var out = new java.io.FileOutputStream(tmpCascade)) {
                        in.transferTo(out);
                    }
                    faceDetector = new CascadeClassifier(tmpCascade.getAbsolutePath());
                    System.out.println("✅ Détecteur de visages chargé.");
                } else {
                    System.out.println("⚠️ Haar cascade introuvable — comparaison sans détection.");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Erreur chargement cascade : " + e.getMessage());
            }
        }, "cascade-loader").start();
    }

    @FXML
    private void handleStartScan() {
        String email = emailField.getText().trim();

        if (!email.matches("^[\\w.%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showEmailError("Adresse email invalide.");
            return;
        }

        try {
            List<Utilisateurs> all = service.afficher();
            targetUser = all.stream()
                    .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                    .findFirst()
                    .orElse(null);
        } catch (SQLException ex) {
            showEmailError("Erreur base de données : " + ex.getMessage());
            return;
        }

        if (targetUser == null) {
            showEmailError("Aucun compte trouvé pour cet email.");
            return;
        }

        if (targetUser.getPhoto() == null || targetUser.getPhoto().isBlank()) {
            showEmailError("Ce compte n'a pas de photo de profil. Veuillez utiliser la connexion classique.");
            return;
        }

        hideEmailError();
        transitionTo(emailStep, scanStep, () -> {
            showStep(2);
            startCamera();
        });
    }

    private void startCamera() {
        cameraRunning.set(true);
        btnCapture.setDisable(true);
        setScanStatus("Initialisation de la caméra…", false);
        cameraLoadingBox.setVisible(true);
        webcamView.setOpacity(0);

        cameraThread = new Thread(() -> {
            try {
                grabber = new OpenCVFrameGrabber(0);
                grabber.setImageWidth(640);
                grabber.setImageHeight(480);
                grabber.start();

                Platform.runLater(() -> {
                    cameraLoadingBox.setVisible(false);
                    webcamView.setOpacity(1.0);
                    btnCapture.setDisable(false);
                    setScanStatus("Caméra active — Positionnez votre visage", true);
                    startScanLineAnimation();
                    startLandmarkAnimation();
                });

                while (cameraRunning.get()) {
                    Frame frame = grabber.grab();
                    if (frame != null && frame.image != null) {
                        Mat mat = converter.convert(frame);
                        if (mat != null) {
                            capturedFrame = mat.clone();
                        }
                        javafx.scene.image.Image fxImage = frameToFxImage(frame);
                        if (fxImage != null) {
                            Platform.runLater(() -> webcamView.setImage(fxImage));
                        }
                    }
                    Thread.sleep(33);
                }

                if (grabber != null) {
                    grabber.stop();
                    grabber.release();
                    grabber = null;
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    cameraLoadingBox.setVisible(false);
                    setScanStatus("⚠️ Erreur caméra : " + e.getMessage(), false);
                    statusDot.getStyleClass().setAll("scan-dot-error");
                    System.err.println("❌ Erreur caméra : " + e.getMessage());
                });
            }
        }, "webcam-thread");
        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private void stopCamera() {
        cameraRunning.set(false);
        if (scanLineAnimation != null) scanLineAnimation.stop();
        if (landmarkAnimation != null) landmarkAnimation.stop();
        if (progressAnimation != null) progressAnimation.stop();
    }

    @FXML
    private void handleCancelScan() {
        stopCamera();
        transitionTo(scanStep, emailStep, () -> showStep(1));
    }

    @FXML
    private void handleCapture() {
        if (capturedFrame == null) {
            setScanStatus("⚠️ Aucune image capturée.", false);
            return;
        }

        btnCapture.setDisable(true);
        setScanStatus("Analyse biométrique en cours…", true);
        startProgressAnimation();

        Mat frameToAnalyze = capturedFrame.clone();
        new Thread(() -> {
            try {
                double score = compareWithProfilePhoto(frameToAnalyze, targetUser.getPhoto());
                final double finalScore = score;

                Platform.runLater(() -> {
                    stopProgressAnimation();
                    showResult(finalScore);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    stopProgressAnimation();
                    setScanStatus("❌ Erreur d'analyse : " + e.getMessage(), false);
                    btnCapture.setDisable(false);
                });
                e.printStackTrace();
            }
        }, "face-compare").start();
    }

    private double compareWithProfilePhoto(Mat webcamMat, String profilePhotoPath)
            throws Exception {

        String photoUrl = PhotoManager.toUrl(profilePhotoPath);
        if (photoUrl == null) throw new Exception("Photo de profil introuvable : " + profilePhotoPath);

        String filePath;
        if (photoUrl.startsWith("file:/")) {
            filePath = new java.io.File(new java.net.URI(photoUrl)).getAbsolutePath();
        } else if (photoUrl.startsWith("jar:")) {
            File tmp = File.createTempFile("profile_", ".jpg");
            tmp.deleteOnExit();
            try (var in = new URL(photoUrl).openStream();
                 var out = new java.io.FileOutputStream(tmp)) {
                in.transferTo(out);
            }
            filePath = tmp.getAbsolutePath();
        } else {
            filePath = photoUrl.replace("file:///", "").replace("file://", "");
        }

        Mat profileMat = opencv_imgcodecs.imread(filePath);
        if (profileMat == null || profileMat.empty()) {
            throw new Exception("Impossible de lire la photo de profil.");
        }

        Mat webcamFace = extractFace(webcamMat);
        Mat profileFace = extractFace(profileMat);

        if (webcamFace == null) webcamFace = webcamMat;
        if (profileFace == null) profileFace = profileMat;

        int SIZE = 128;
        Mat webcamResized = new Mat();
        Mat profileResized = new Mat();
        opencv_imgproc.resize(webcamFace, webcamResized, new Size(SIZE, SIZE));
        opencv_imgproc.resize(profileFace, profileResized, new Size(SIZE, SIZE));

        Mat webcamGray = new Mat();
        Mat profileGray = new Mat();
        opencv_imgproc.cvtColor(webcamResized, webcamGray, opencv_imgproc.COLOR_BGR2GRAY);
        opencv_imgproc.cvtColor(profileResized, profileGray, opencv_imgproc.COLOR_BGR2GRAY);

        org.bytedeco.opencv.opencv_imgproc.CLAHE clahe =
                opencv_imgproc.createCLAHE(2.0, new Size(8, 8));
        Mat webcamEq = new Mat();
        Mat profileEq = new Mat();
        clahe.apply(webcamGray, webcamEq);
        clahe.apply(profileGray, profileEq);

        double histScore = compareHistograms(webcamEq, profileEq);
        double pixelScore = comparePixels(webcamEq, profileEq);
        double finalScore = 0.60 * histScore + 0.40 * pixelScore;

        Platform.runLater(() ->
                matchPctLabel.setText(String.format("%.1f%%", finalScore * 100)));

        System.out.printf("🔬 Score hist=%.3f | pixel=%.3f | final=%.3f%n",
                histScore, pixelScore, finalScore);

        return finalScore;
    }

    private Mat extractFace(Mat src) {
        if (faceDetector == null || faceDetector.empty()) return null;
        try {
            Mat gray = new Mat();
            opencv_imgproc.cvtColor(src, gray, opencv_imgproc.COLOR_BGR2GRAY);
            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(gray, faces, 1.1, 3, 0,
                    new Size(30, 30), new Size());
            if (faces.size() > 0) {
                Rect r = faces.get(0);
                int x = Math.max(0, r.x());
                int y = Math.max(0, r.y());
                int w = Math.min(r.width(), src.cols() - x);
                int h = Math.min(r.height(), src.rows() - y);
                return new Mat(src, new Rect(x, y, w, h));
            }
        } catch (Exception e) {
            System.err.println("⚠️ Détection visage : " + e.getMessage());
        }
        return null;
    }

    private double compareHistograms(Mat a, Mat b) {
        try {
            MatVector vecA = new MatVector(a);
            MatVector vecB = new MatVector(b);
            Mat histA = new Mat();
            Mat histB = new Mat();
            int[] channels = {0};
            int[] histSize = {256};
            float[] ranges = {0f, 256f};

            opencv_imgproc.calcHist(vecA, new IntPointer(channels), new Mat(),
                    histA, new IntPointer(histSize), new FloatPointer(ranges));
            opencv_imgproc.calcHist(vecB, new IntPointer(channels), new Mat(),
                    histB, new IntPointer(histSize), new FloatPointer(ranges));

            opencv_core.normalize(histA, histA, 0, 1, opencv_core.NORM_MINMAX, -1, new Mat());
            opencv_core.normalize(histB, histB, 0, 1, opencv_core.NORM_MINMAX, -1, new Mat());

            double correl = opencv_imgproc.compareHist(histA, histB, opencv_imgproc.CV_COMP_CORREL);
            return Math.max(0, correl);
        } catch (Exception e) {
            System.err.println("⚠️ Hist compare : " + e.getMessage());
            return 0;
        }
    }

    private double comparePixels(Mat a, Mat b) {
        try {
            Mat diff = new Mat();
            opencv_core.absdiff(a, b, diff);
            double sumDiff = opencv_core.sumElems(diff).get(0);
            double maxPossible = 255.0 * a.rows() * a.cols();
            double normalizedDiff = sumDiff / maxPossible;
            return 1.0 - normalizedDiff;
        } catch (Exception e) {
            return 0;
        }
    }

    private void showResult(double score) {
        stopCamera();
        boolean success = score >= THRESHOLD;

        finalScoreLabel.setText(String.format("%.1f%%", score * 100));
        detectedUserLabel.setText(targetUser.getPrenom() + " " + targetUser.getNom());

        if (success) {
            loggedInUser = targetUser;
            resultIconContainer.getStyleClass().setAll("result-icon-bg-success");
            resultIcon.setIconLiteral("fas-check-circle");
            resultIcon.setIconColor(Color.web("#059669"));
            resultIcon.setIconSize(52);
            resultTitle.setText("Identité confirmée !");
            resultTitle.getStyleClass().setAll("result-title-success");
            resultSubtitle.setText("Bienvenue, " + targetUser.getPrenom() + " 👋");
            statusLabel.setText("✅ Correspondance validée");
            statusLabel.getStyleClass().setAll("match-card-status");
            btnContinue.setDisable(false);
            btnContinue.setOpacity(1.0);
        } else {
            resultIconContainer.getStyleClass().setAll("result-icon-bg-error");
            resultIcon.setIconLiteral("fas-times-circle");
            resultIcon.setIconColor(Color.web("#dc2626"));
            resultIcon.setIconSize(52);
            resultTitle.setText("Accès refusé");
            resultTitle.getStyleClass().setAll("result-title-error");
            resultSubtitle.setText(String.format(
                    "Correspondance insuffisante (%.0f%% < %.0f%%)",
                    score * 100, THRESHOLD * 100));
            statusLabel.setText("❌ Échec de vérification");
            statusLabel.getStyleClass().setAll("match-card-status-error");
            btnContinue.setDisable(true);
            btnContinue.setOpacity(0.4);
        }

        matchPctLabel.setText(String.format("%.1f%%", score * 100));
        transitionTo(scanStep, resultStep, () -> showStep(3));
        animateResultIcon(success);
    }

    @FXML
    private void handleContinue() {
        if (loggedInUser == null) return;
        try {
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(600);

            if (loggedInUser.getRole() == com.gestioncolis.enums.Role.ADMIN) {
                // Admin → Dashboard back-office
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/views/DashboardLayout.fxml"));
                Scene scene = new Scene(loader.load(), 1200, 720);
                DashboardController dc = loader.getController();
                dc.setCurrentUser(loggedInUser);
                stage.setTitle("TrackPack — Dashboard Admin");
                stage.setScene(scene);
            } else {
                // CLIENT, LIVREUR, TECHNICIEN, ENTREPRISE → UserHome front-office
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/views/UserHomeView.fxml"));
                Scene scene = new Scene(loader.load(), 1280, 760);
                UserHomeController uhc = loader.getController();
                uhc.setCurrentUser(loggedInUser);
                stage.setTitle("TrackPack — Espace " + loggedInUser.getRole().name());
                stage.setScene(scene);
                stage.setWidth(1280);
                stage.setHeight(760);
            }

            stage.centerOnScreen();

        } catch (IOException e) {
            System.err.println("❌ Navigation après face login : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRetry() {
        capturedFrame = null;
        matchPctLabel.setText("—");
        transitionTo(resultStep, emailStep, () -> {
            showStep(1);
            emailField.clear();
            hideEmailError();
            targetUser = null;
            loggedInUser = null;
        });
    }

    @FXML
    private void handleBackToLogin() {
        stopCamera();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/AuthView.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            Stage stage = (Stage) emailField.getScene().getWindow();

            // Réinitialiser avec les mêmes propriétés que AuthView
            stage.setTitle("TrackPack — Connexion");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.setWidth(1100);
            stage.setHeight(700);
            stage.centerOnScreen();

        } catch (IOException e) {
            System.err.println("❌ Retour login : " + e.getMessage());
        }
    }

    private void showStep(int step) {
        updateStep(step1Circle, step1Label,
                step == 1 ? "step-active" : (step > 1 ? "step-done" : "step-inactive"),
                step == 1 ? "step-label-active" : (step > 1 ? "step-label-done" : "step-label-inactive"));
        updateStep(step2Circle, step2Label,
                step == 2 ? "step-active" : (step > 2 ? "step-done" : "step-inactive"),
                step == 2 ? "step-label-active" : (step > 2 ? "step-label-done" : "step-label-inactive"));
        updateStep(step3Circle, step3Label,
                step == 3 ? "step-active" : "step-inactive",
                step == 3 ? "step-label-active" : "step-label-inactive");
    }

    private void updateStep(StackPane circle, Label label, String circleStyle, String labelStyle) {
        circle.getStyleClass().setAll("step-circle", circleStyle);
        label.getStyleClass().setAll(labelStyle);
    }

    private void transitionTo(VBox from, VBox to, Runnable onComplete) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), from);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            from.setVisible(false);
            from.setManaged(false);
            to.setVisible(true);
            to.setManaged(true);
            to.setOpacity(0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), to);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(300), to);
            slide.setFromY(20);
            slide.setToY(0);
            slide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));

            new ParallelTransition(fadeIn, slide).play();

            if (onComplete != null) onComplete.run();
        });
        fadeOut.play();
    }

    private void startScanLineAnimation() {
        Node scanLine = scanStep.lookup(".scan-line");
        if (scanLine == null) return;
        scanLineAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(scanLine.translateYProperty(), 0)),
                new KeyFrame(Duration.millis(1800),
                        new KeyValue(scanLine.translateYProperty(), 260,
                                Interpolator.EASE_BOTH))
        );
        scanLineAnimation.setAutoReverse(true);
        scanLineAnimation.setCycleCount(Animation.INDEFINITE);
        scanLineAnimation.play();
    }

    private void startLandmarkAnimation() {
        animateBiometricDotsSequential();
        Circle[] dots = {dotEyeL, dotEyeR, dotNose, dotMouthL, dotMouthR,
                dotChin, dotForehead, dotCheekL, dotCheekR};

        for (Circle dot : dots) {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(1200 + (int)(Math.random() * 600)), dot);
            pulse.setFromX(0.8);
            pulse.setFromY(0.8);
            pulse.setToX(1.3);
            pulse.setToY(1.3);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.play();
        }
    }

    private void startProgressAnimation() {
        progressAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(progressFill.prefWidthProperty(), 0),
                        new KeyValue(progressFill.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(3000),
                        new KeyValue(progressFill.prefWidthProperty(), 480,
                                Interpolator.EASE_IN))
        );
        progressAnimation.play();

        Timeline counter = new Timeline();
        for (int i = 0; i <= 100; i++) {
            final int pct = i;
            counter.getKeyFrames().add(
                    new KeyFrame(Duration.millis(i * 30L),
                            e -> progressPctLabel.setText(pct + "%"))
            );
        }
        counter.play();
    }

    private void stopProgressAnimation() {
        if (progressAnimation != null) progressAnimation.stop();
        progressFill.setPrefWidth(480);
        progressPctLabel.setText("100%");
    }

    private void animateResultIcon(boolean success) {
        ScaleTransition bounce = new ScaleTransition(Duration.millis(350), resultIconContainer);
        bounce.setFromX(0.3);
        bounce.setFromY(0.3);
        bounce.setToX(1.12);
        bounce.setToY(1.12);
        bounce.setInterpolator(Interpolator.EASE_OUT);
        bounce.setOnFinished(e -> {
            ScaleTransition settle = new ScaleTransition(Duration.millis(180), resultIconContainer);
            settle.setToX(1.0);
            settle.setToY(1.0);
            settle.setInterpolator(Interpolator.EASE_BOTH);
            settle.play();
        });

        FadeTransition ft = new FadeTransition(Duration.millis(400), resultIconContainer);
        ft.setFromValue(0);
        ft.setToValue(1);

        new ParallelTransition(bounce, ft).play();

        if (success) {
            ScaleTransition glow = new ScaleTransition(Duration.millis(1000), resultIconContainer);
            glow.setFromX(1.0);
            glow.setFromY(1.0);
            glow.setToX(1.06);
            glow.setToY(1.06);
            glow.setCycleCount(4);
            glow.setAutoReverse(true);
            glow.setDelay(Duration.millis(500));
            glow.play();
        }
    }

    private void startBlobAnimations() {
        animateBlobPair(blob1, 20000, 1.2, 0.08, 0.15);
        animateBlobPair(blob2, 25000, 1.15, 0.06, 0.12);
        animateBlobPair(blob3, 18000, 1.25, 0.07, 0.14);
        animateBlobPair(blob4, 22000, 1.1, 0.05, 0.10);
        animateBlobPair(blob5, 15000, 1.3, 0.06, 0.12);
    }

    private void animateBlobPair(Circle blob, int ms, double scale,
                                 double opFrom, double opTo) {
        ScaleTransition st = new ScaleTransition(Duration.millis(ms), blob);
        st.setFromX(1);
        st.setFromY(1);
        st.setToX(scale);
        st.setToY(scale);
        st.setCycleCount(Animation.INDEFINITE);
        st.setAutoReverse(true);
        st.play();

        FadeTransition ft = new FadeTransition(Duration.millis(ms / 2), blob);
        ft.setFromValue(opFrom);
        ft.setToValue(opTo);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();
    }

    private void setScanStatus(String text, boolean green) {
        scanStatusLabel.setText(text);
        statusDot.getStyleClass().setAll(green ? "scan-dot-active" : "scan-dot-error");
    }

    private void showEmailError(String msg) {
        emailError.setText(msg);
        emailError.setVisible(true);
        emailError.setManaged(true);
        TranslateTransition shake = new TranslateTransition(Duration.millis(55), emailField);
        shake.setByX(8);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }

    private void hideEmailError() {
        emailError.setVisible(false);
        emailError.setManaged(false);
    }

    private javafx.scene.image.Image frameToFxImage(Frame frame) {
        try {
            Java2DFrameConverter java2d = new Java2DFrameConverter();
            java.awt.image.BufferedImage bi = java2d.convert(frame);
            if (bi == null) return null;

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bi, "png", baos);
            byte[] bytes = baos.toByteArray();
            return new Image(new java.io.ByteArrayInputStream(bytes));
        } catch (Exception e) {
            return null;
        }
    }

    private void animateBiometricDotsSequential() {
        Circle[] dots = {dotEyeL, dotEyeR, dotNose, dotMouthL, dotMouthR, dotChin, dotForehead, dotCheekL, dotCheekR};
        for (int i = 0; i < dots.length; i++) {
            final int index = i;
            PauseTransition pause = new PauseTransition(Duration.millis(index * 80));
            pause.setOnFinished(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(300), dots[index]);
                st.setFromX(0.3);
                st.setFromY(0.3);
                st.setToX(1);
                st.setToY(1);
                st.setInterpolator(Interpolator.SPLINE(0.34, 0.95, 0.64, 1.0));
                st.play();
            });
            pause.play();
        }
    }

    private static class IntPointer extends org.bytedeco.javacpp.IntPointer {
        IntPointer(int... values) { super(values); }
    }

    private static class FloatPointer extends org.bytedeco.javacpp.FloatPointer {
        FloatPointer(float... values) { super(values); }
    }
}