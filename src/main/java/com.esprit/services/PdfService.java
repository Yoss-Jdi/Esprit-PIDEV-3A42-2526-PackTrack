package com.esprit.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.esprit.entities.Facture;
import com.esprit.gui.SignaturePadDialog;
import com.esprit.utils.CloudinaryConfig;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

public class PdfService {

    private final Cloudinary cloudinary = CloudinaryConfig.getInstance();

    /**
     * Génère la facture PDF avec signature électronique.
     * @param f      La facture
     * @param owner  La fenêtre JavaFX parente (pour le popup de signature)
     * @return       L'URL Cloudinary du PDF, ou null si annulé/erreur
     */
    public String generateFacturePdf(Facture f, Stage owner) {
        String cloudinaryUrl = null;

        try {
            // ── Étape 1 : Ouvrir le pad de signature ──────────────────────────
            File signatureFile = demanderSignature(f.getNumero(), owner);

            if (signatureFile == null) {
                System.out.println("[PDF] Génération annulée — signature non fournie.");
                return null;
            }

            // ── Étape 2 : Charger et remplir le template HTML ─────────────────
            String html      = loadTemplate();
            float tvaMontant = f.getMontantTTC() - f.getMontantHT();

            html = html.replace("${numero}",      f.getNumero())
                    .replace("${date}",        String.valueOf(f.getDateEmission()))
                    .replace("${statut}",      f.getStatut())
                    .replace("${ht}",          String.format("%.2f", f.getMontantHT()))
                    .replace("${tva}",         String.valueOf(f.getTva()))
                    .replace("${tva_montant}", String.format("%.2f", tvaMontant))
                    .replace("${ttc}",         String.format("%.2f", f.getMontantTTC()))
                    .replace("${livraison}",   String.valueOf(f.getIdLivraison()));

            // ── Étape 3 : PDF temporaire pour upload Cloudinary ───────────────
            File pdfDir  = new File("pdfs");
            pdfDir.mkdirs();
            File pdfTemp = new File(pdfDir, "facture_" + f.getNumero() + "_temp.pdf");

            try (OutputStream os = new FileOutputStream(pdfTemp)) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.withHtmlContent(html, null);
                builder.toStream(os);
                builder.run();
            }

            // ── Étape 4 : Upload vers Cloudinary ──────────────────────────────
            String   publicId = "factures/facture_" + f.getNumero();
            Map<?,?> result   = cloudinary.uploader().upload(pdfTemp, ObjectUtils.asMap(
                    "resource_type", "raw",
                    "public_id",     publicId,
                    "overwrite",     true
            ));
            cloudinaryUrl = (String) result.get("secure_url");
            System.out.println("[PDF] Uploadé sur Cloudinary : " + cloudinaryUrl);

            // ── Étape 5 : URL de vérification ─────────────────────────────────
            String verificationUrl     = "https://roaring-sable-a097fb.netlify.app"
                    + "?facture=" + f.getNumero() + "&url=" + cloudinaryUrl;
            String verificationUrlHtml = verificationUrl.replace("&", "&amp;");

            // ── Étape 6 : QR Code ─────────────────────────────────────────────
            File qrDir  = new File("pdfs/qrcodes");
            qrDir.mkdirs();
            File qrFile = new File(qrDir, "qr_" + f.getNumero() + ".png");

            QRCodeWriter  qrWriter = new QRCodeWriter();
            BitMatrix     matrix   = qrWriter.encode(verificationUrl, BarcodeFormat.QR_CODE, 200, 200);
            BufferedImage qrImage  = MatrixToImageWriter.toBufferedImage(matrix);
            ImageIO.write(qrImage, "PNG", qrFile);

            // ── Étape 7 : PDF final avec QR + signature ───────────────────────
            String qrUri        = qrFile.toURI().toString();
            String signatureUri = signatureFile.toURI().toString();

            String htmlFinal = html
                    .replace("${qr_code_path}",    qrUri)
                    .replace("${verification_url}", verificationUrlHtml)
                    .replace("${signature_path}",   signatureUri);

            File pdfFinal = new File(pdfDir, "facture_" + f.getNumero() + ".pdf");

            try (OutputStream os2 = new FileOutputStream(pdfFinal)) {
                PdfRendererBuilder builder2 = new PdfRendererBuilder();
                builder2.withHtmlContent(htmlFinal, null);
                builder2.toStream(os2);
                builder2.run();
            }

            pdfTemp.delete();
            System.out.println("[PDF] PDF final généré : " + pdfFinal.getAbsolutePath());

            // ── Étape 8 : Ouvrir le PDF ───────────────────────────────────────
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdfFinal);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return cloudinaryUrl;
    }

    /**
     * Compatibilité avec l'ancien appel sans Stage.
     * Utilise le thread courant s'il est FX, sinon Platform.runLater.
     */
    public String generateFacturePdf(Facture f) {
        return generateFacturePdf(f, null);
    }

    // ── Ouvre le pad de signature dans le thread JavaFX ───────────────────────

    private File demanderSignature(String factureNumero, Stage owner) {

        // Si on est déjà dans le thread JavaFX → appel direct
        if (Platform.isFxApplicationThread()) {
            SignaturePadDialog dialog = new SignaturePadDialog(owner);
            return dialog.showAndWait();
        }

        // Sinon → on passe dans le thread JavaFX
        AtomicReference<File> result = new AtomicReference<>();

        try {
            Platform.runLater(() -> {
                SignaturePadDialog dialog = new SignaturePadDialog(owner);
                result.set(dialog.showAndWait());
            });

            // ⚠️ attendre un peu (simple, sans deadlock)
            Thread.sleep(500);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result.get();
    }

    private String loadTemplate() throws Exception {
        InputStream is = getClass().getResourceAsStream("/com/esprit/templates/facture.html");
        if (is == null) throw new RuntimeException("Template facture.html introuvable !");
        return new String(is.readAllBytes());
    }
}