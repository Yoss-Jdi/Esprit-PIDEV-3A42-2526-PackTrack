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
import com.gestioncolis.services.UtilisateursServices;
import com.gestioncolis.utils.PhotoManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FaceLoginController — Reconnaissance faciale JavaFX + OpenCV (JavaCV)
 *
 * Flux :
 *  1. L'utilisateur entre son email  →  on cherche son compte + sa photo de profil
 *  2. La webcam s'ouvre             →  flux vidéo affiché dans l'ImageView
 *  3. "Capturer & Analyser"         →  on capture une frame, on détecte le visage,
 *                                      on compare avec la photo stockée en base
 *  4. Si similarité ≥ THRESHOLD     →  login automatique
 *
 * Comparaison :
 *   - Détection de visage via CascadeClassifier (Haar frontal face)
 *   - Redimensionnement + normalisation des deux images
 *   - Calcul de similarité via histogrammes (méthode CORREL) + comparaison de pixels
 *   - Score final pondéré : 60% histogramme + 40% pixels normalisés
 *
 * ⚠️  Prérequis pom.xml (ajouter dans <dependencies>) :
 *
 *   <dependency>
 *     <groupId>org.bytedeco</groupId>
 *     <artifactId>javacv-platform</artifactId>
 *     <version>1.5.10</version>
 *   </dependency>
 */
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
    @FXML private Label     resultIcon, resultTitle, resultSubtitle;
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
    private Mat capturedFrame                 = null;  // la dernière frame capturée

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
                System.err.println("\u26a0\ufe0f  face-login.css introuvable dans /css/");
            }
        } catch (Exception e) {
            System.err.println("\u26a0\ufe0f  Erreur chargement face-login.css : " + e.getMessage());
        }
    }

    // ─── Charger le détecteur Haar ────────────────────────────────────────────
    private void loadFaceDetector() {
        new Thread(() -> {
            try {
                // Le fichier haarcascade est inclus dans opencv-platform.jar
                // On le copie temporairement sur le disque
                URL cascadeUrl = getClass().getResource(
                        "/org/bytedeco/opencv/linux-x86_64/share/opencv4/haarcascades/"
                                + "haarcascade_frontalface_default.xml");

                if (cascadeUrl == null) {
                    // Fallback: chercher dans les ressources du projet
                    cascadeUrl = getClass().getResource("/haarcascade_frontalface_default.xml");
                }

                if (cascadeUrl != null) {
                    // Copier vers un fichier temporaire
                    File tmpCascade = File.createTempFile("haarcascade_", ".xml");
                    tmpCascade.deleteOnExit();
                    try (var in  = cascadeUrl.openStream();
                         var out = new java.io.FileOutputStream(tmpCascade)) {
                        in.transferTo(out);
                    }
                    faceDetector = new CascadeClassifier(tmpCascade.getAbsolutePath());
                    System.out.println("✅ Détecteur de visages chargé.");
                } else {
                    System.out.println("⚠️  Haar cascade introuvable — comparaison sans détection.");
                }
            } catch (Exception e) {
                System.err.println("⚠️  Erreur chargement cascade : " + e.getMessage());
            }
        }, "cascade-loader").start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STEP 1 — Email
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleStartScan() {
        String email = emailField.getText().trim();

        // Validation basique
        if (!email.matches("^[\\w.%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showEmailError("Adresse email invalide.");
            return;
        }

        // Chercher l'utilisateur
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
            showEmailError("Ce compte n'a pas de photo de profil. "
                    + "Veuillez utiliser la connexion classique.");
            return;
        }

        // Passer à l'étape 2
        hideEmailError();
        transitionTo(emailStep, scanStep, () -> {
            showStep(2);
            startCamera();
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STEP 2 — Webcam
    // ═════════════════════════════════════════════════════════════════════════
    private void startCamera() {
        cameraRunning.set(true);
        btnCapture.setDisable(true);
        setScanStatus("Initialisation de la caméra…", false);
        cameraLoadingBox.setVisible(true);
        webcamView.setOpacity(0);

        cameraThread = new Thread(() -> {
            try {
                grabber = new OpenCVFrameGrabber(0);  // caméra index 0
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
                        // Garder une référence sur la dernière frame pour la capture
                        Mat mat = converter.convert(frame);
                        if (mat != null) {
                            capturedFrame = mat.clone();
                        }

                        // Convertir en JavaFX Image pour l'affichage
                        javafx.scene.image.Image fxImage = frameToFxImage(frame);
                        if (fxImage != null) {
                            Platform.runLater(() -> webcamView.setImage(fxImage));
                        }
                    }
                    Thread.sleep(33); // ~30 fps
                }

                grabber.stop();
                grabber.release();
                grabber = null;

            } catch (Exception e) {
                Platform.runLater(() -> {
                    cameraLoadingBox.setVisible(false);
                    setScanStatus("⚠️  Erreur caméra : " + e.getMessage(), false);
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
        if (progressAnimation  != null) progressAnimation.stop();
    }

    @FXML
    private void handleCancelScan() {
        stopCamera();
        transitionTo(scanStep, emailStep, () -> showStep(1));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CAPTURE & COMPARE
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleCapture() {
        if (capturedFrame == null) {
            setScanStatus("⚠️  Aucune image capturée.", false);
            return;
        }

        btnCapture.setDisable(true);
        setScanStatus("Analyse biométrique en cours…", true);
        startProgressAnimation();

        // Lancer la comparaison dans un thread séparé
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

    /**
     * Compare la frame webcam avec la photo de profil.
     * Retourne un score entre 0.0 et 1.0.
     */
    private double compareWithProfilePhoto(Mat webcamMat, String profilePhotoPath)
            throws Exception {

        // ── 1. Charger la photo de profil ────────────────────────────────────
        String photoUrl = PhotoManager.toUrl(profilePhotoPath);
        if (photoUrl == null) throw new Exception("Photo de profil introuvable : " + profilePhotoPath);

        // Convertir l'URL en chemin fichier pour OpenCV
        String filePath;
        if (photoUrl.startsWith("file:/")) {
            filePath = new java.io.File(new java.net.URI(photoUrl)).getAbsolutePath();
        } else if (photoUrl.startsWith("jar:")) {
            // Extraire depuis le jar vers un fichier temporaire
            File tmp = File.createTempFile("profile_", ".jpg");
            tmp.deleteOnExit();
            try (var in  = new URL(photoUrl).openStream();
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

        // ── 2. Détecter et recadrer le visage dans la webcam ─────────────────
        Mat webcamFace   = extractFace(webcamMat);
        Mat profileFace  = extractFace(profileMat);

        // Si détection impossible, utiliser les images entières
        if (webcamFace  == null) webcamFace  = webcamMat;
        if (profileFace == null) profileFace = profileMat;

        // ── 3. Normaliser la taille (128×128) ────────────────────────────────
        int SIZE = 128;
        Mat webcamResized  = new Mat();
        Mat profileResized = new Mat();
        opencv_imgproc.resize(webcamFace,  webcamResized,  new Size(SIZE, SIZE));
        opencv_imgproc.resize(profileFace, profileResized, new Size(SIZE, SIZE));

        // ── 4. Convertir en niveaux de gris ──────────────────────────────────
        Mat webcamGray   = new Mat();
        Mat profileGray  = new Mat();
        opencv_imgproc.cvtColor(webcamResized,  webcamGray,  opencv_imgproc.COLOR_BGR2GRAY);
        opencv_imgproc.cvtColor(profileResized, profileGray, opencv_imgproc.COLOR_BGR2GRAY);

        // ── 5. Normaliser le contraste (CLAHE) ───────────────────────────────
        org.bytedeco.opencv.opencv_imgproc.CLAHE clahe =
                opencv_imgproc.createCLAHE(2.0, new Size(8, 8));
        Mat webcamEq  = new Mat();
        Mat profileEq = new Mat();
        clahe.apply(webcamGray,  webcamEq);
        clahe.apply(profileGray, profileEq);

        // ── 6. Score histogramme (60%) ────────────────────────────────────────
        double histScore = compareHistograms(webcamEq, profileEq);

        // ── 7. Score pixels (40%) ─────────────────────────────────────────────
        double pixelScore = comparePixels(webcamEq, profileEq);

        // ── 8. Score final ────────────────────────────────────────────────────
        double finalScore = 0.60 * histScore + 0.40 * pixelScore;

        // Mettre à jour le badge en temps réel
        Platform.runLater(() ->
                matchPctLabel.setText(String.format("%.1f%%", finalScore * 100)));

        System.out.printf("🔬 Score hist=%.3f | pixel=%.3f | final=%.3f%n",
                histScore, pixelScore, finalScore);

        return finalScore;
    }

    /** Détecte le visage dans un Mat et retourne la région du visage, ou null. */
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
                // Sécuriser les bounds
                int x = Math.max(0, r.x());
                int y = Math.max(0, r.y());
                int w = Math.min(r.width(),  src.cols() - x);
                int h = Math.min(r.height(), src.rows() - y);
                return new Mat(src, new Rect(x, y, w, h));
            }
        } catch (Exception e) {
            System.err.println("⚠️  Détection visage : " + e.getMessage());
        }
        return null;
    }

    /** Comparaison d'histogrammes (méthode CORREL → valeur entre -1 et 1). */
    private double compareHistograms(Mat a, Mat b) {
        try {
            MatVector vecA = new MatVector(a);
            MatVector vecB = new MatVector(b);
            Mat histA = new Mat();
            Mat histB = new Mat();
            int[] channels = {0};
            int[] histSize = {256};
            float[] ranges  = {0f, 256f};

            opencv_imgproc.calcHist(vecA, new IntPointer(channels), new Mat(),
                    histA, new IntPointer(histSize), new FloatPointer(ranges));
            opencv_imgproc.calcHist(vecB, new IntPointer(channels), new Mat(),
                    histB, new IntPointer(histSize), new FloatPointer(ranges));

            opencv_core.normalize(histA, histA, 0, 1, opencv_core.NORM_MINMAX, -1, new Mat());
            opencv_core.normalize(histB, histB, 0, 1, opencv_core.NORM_MINMAX, -1, new Mat());

            double correl = opencv_imgproc.compareHist(histA, histB, opencv_imgproc.CV_COMP_CORREL);
            return Math.max(0, correl); // 0 à 1
        } catch (Exception e) {
            System.err.println("⚠️  Hist compare : " + e.getMessage());
            return 0;
        }
    }

    /** Comparaison pixel-à-pixel normalisée (1 - différence normalisée). */
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

    // ═════════════════════════════════════════════════════════════════════════
    // STEP 3 — Result
    // ═════════════════════════════════════════════════════════════════════════
    private void showResult(double score) {
        stopCamera();
        boolean success = score >= THRESHOLD;

        // Remplir les détails
        finalScoreLabel.setText(String.format("%.1f%%", score * 100));
        detectedUserLabel.setText(targetUser.getPrenom() + " " + targetUser.getNom());

        if (success) {
            loggedInUser = targetUser;
            resultIconContainer.getStyleClass().setAll("result-icon-bg-success");
            resultIcon.setText("✓");
            resultIcon.getStyleClass().setAll("result-icon-text");
            resultTitle.setText("Identité confirmée !");
            resultTitle.getStyleClass().setAll("result-title-success");
            resultSubtitle.setText("Bienvenue, " + targetUser.getPrenom() + " 👋");
            statusLabel.setText("✅  Correspondance validée");
            statusLabel.getStyleClass().setAll("match-card-status");
            btnContinue.setDisable(false);
            btnContinue.setOpacity(1.0);
        } else {
            resultIconContainer.getStyleClass().setAll("result-icon-bg-error");
            resultIcon.setText("✕");
            resultIcon.getStyleClass().setAll("result-icon-text-error");
            resultTitle.setText("Accès refusé");
            resultTitle.getStyleClass().setAll("result-title-error");
            resultSubtitle.setText(String.format(
                    "Correspondance insuffisante (%.0f%% < %.0f%%)",
                    score * 100, THRESHOLD * 100));
            statusLabel.setText("❌  Échec de vérification");
            statusLabel.getStyleClass().setAll("match-card-status-error");
            btnContinue.setDisable(true);
            btnContinue.setOpacity(0.4);
        }

        // Score final dans le badge webcam
        matchPctLabel.setText(String.format("%.1f%%", score * 100));

        transitionTo(scanStep, resultStep, () -> showStep(3));

        // Animer l'icône résultat
        animateResultIcon(success);
    }

    @FXML
    private void handleContinue() {
        if (loggedInUser == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/DashboardLayout.fxml"));
            Scene scene = new Scene(loader.load());
            DashboardController dc = loader.getController();
            dc.setCurrentUser(loggedInUser);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle("TrackPack — Dashboard");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("❌ Navigation dashboard : " + e.getMessage());
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
            targetUser   = null;
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
            stage.setTitle("TrackPack — Connexion");
            stage.setScene(scene);
            stage.setResizable(false);
        } catch (IOException e) {
            System.err.println("❌ Retour login : " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STEP INDICATOR
    // ═════════════════════════════════════════════════════════════════════════
    private void showStep(int step) {
        // Step 1
        updateStep(step1Circle, step1Label,
                step == 1 ? "step-active" : (step > 1 ? "step-done" : "step-inactive"),
                step == 1 ? "step-label-active" : (step > 1 ? "step-label-done" : "step-label-inactive"),
                step > 1 ? "✓" : "1");
        // Step 2
        updateStep(step2Circle, step2Label,
                step == 2 ? "step-active" : (step > 2 ? "step-done" : "step-inactive"),
                step == 2 ? "step-label-active" : (step > 2 ? "step-label-done" : "step-label-inactive"),
                step > 2 ? "✓" : "2");
        // Step 3
        updateStep(step3Circle, step3Label,
                step == 3 ? "step-active" : "step-inactive",
                step == 3 ? "step-label-active" : "step-label-inactive",
                "3");
    }

    private void updateStep(StackPane circle, Label label,
                            String circleStyle, String labelStyle, String number) {
        circle.getStyleClass().setAll("step-circle", circleStyle);
        label.getStyleClass().setAll(labelStyle);
        // Mettre à jour le numéro / check
        if (circle.getChildren().get(0) instanceof javafx.scene.text.Text t) {
            t.setText(number);
            t.getStyleClass().setAll("step-number");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TRANSITIONS
    // ═════════════════════════════════════════════════════════════════════════
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

    // ═════════════════════════════════════════════════════════════════════════
    // ANIMATIONS
    // ═════════════════════════════════════════════════════════════════════════
    /** Animation de la ligne de scan (monte et descend dans l'oval) */
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

    /** Animation des points biométriques (pulsation) */
    private void startLandmarkAnimation() {
        Circle[] dots = {dotEyeL, dotEyeR, dotNose, dotMouthL, dotMouthR,
                dotChin, dotForehead, dotCheekL, dotCheekR};

        SequentialTransition seq = new SequentialTransition();
        for (int i = 0; i < dots.length; i++) {
            Circle dot = dots[i];
            // Apparition décalée
            FadeTransition appear = new FadeTransition(Duration.millis(80), dot);
            appear.setFromValue(0.2);
            appear.setToValue(1.0);
            seq.getChildren().add(appear);
        }
        seq.setOnFinished(e -> {
            // Pulsation continue
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
        });
        landmarkAnimation = new Timeline(
                new KeyFrame(Duration.millis(10), ev -> seq.play())
        );
        landmarkAnimation.play();
    }

    /** Animation de la barre de progression pendant l'analyse */
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

        // Compteur pourcentage
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

    /** Animation de l'icône de résultat (bounce) */
    private void animateResultIcon(boolean success) {
        // SPLINE valeurs doivent être dans [0,1] — on simule le bounce en 2 étapes
        ScaleTransition bounce = new ScaleTransition(Duration.millis(350), resultIconContainer);
        bounce.setFromX(0.3);
        bounce.setFromY(0.3);
        bounce.setToX(1.12);   // légère sur-extension simulant le bounce
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
            // Effet de pulsation verte sur succès
            ScaleTransition glow = new ScaleTransition(Duration.millis(1000), resultIconContainer);
            glow.setFromX(1.0); glow.setFromY(1.0);
            glow.setToX(1.06); glow.setToY(1.06);
            glow.setCycleCount(4);
            glow.setAutoReverse(true);
            glow.setDelay(Duration.millis(500));
            glow.play();
        }
    }

    /** Blobs de fond animés */
    private void startBlobAnimations() {
        animateBlobPair(blob1, 20000, 1.2, 0.08, 0.15);
        animateBlobPair(blob2, 25000, 1.15, 0.06, 0.12);
        animateBlobPair(blob3, 18000, 1.25, 0.07, 0.14);
        animateBlobPair(blob4, 22000, 1.1,  0.05, 0.10);
        animateBlobPair(blob5, 15000, 1.3,  0.06, 0.12);
    }

    private void animateBlobPair(Circle blob, int ms, double scale,
                                 double opFrom, double opTo) {
        ScaleTransition st = new ScaleTransition(Duration.millis(ms), blob);
        st.setFromX(1); st.setFromY(1);
        st.setToX(scale); st.setToY(scale);
        st.setCycleCount(Animation.INDEFINITE);
        st.setAutoReverse(true);
        st.play();

        FadeTransition ft = new FadeTransition(Duration.millis(ms / 2), blob);
        ft.setFromValue(opFrom); ft.setToValue(opTo);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════════
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

    /**
     * Convertit une Frame JavaCV en javafx.scene.image.Image.
     * Utilise Java2DFrameConverter pour la conversion.
     */
    private javafx.scene.image.Image frameToFxImage(Frame frame) {
        try {
            Java2DFrameConverter java2d = new Java2DFrameConverter();
            java.awt.image.BufferedImage bi = java2d.convert(frame);
            if (bi == null) return null;

            // Convertir BufferedImage → JavaFX Image via un stream en mémoire
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bi, "png", baos);
            byte[] bytes = baos.toByteArray();
            return new Image(new java.io.ByteArrayInputStream(bytes));
        } catch (Exception e) {
            return null;
        }
    }

    // ─── Pointeur int / float helpers pour OpenCV ───────────────────────────
    // (Les classes IntPointer / FloatPointer viennent de JavaCPP)
    private static class IntPointer extends org.bytedeco.javacpp.IntPointer {
        IntPointer(int... values) { super(values); }
    }
    private static class FloatPointer extends org.bytedeco.javacpp.FloatPointer {
        FloatPointer(float... values) { super(values); }
    }
}