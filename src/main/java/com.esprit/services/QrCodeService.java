package com.esprit.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class QrCodeService {

    /**
     * Génère un QR code PNG pointant vers l'URL de vérification
     * @param factureNumero  ex: FAC-2026-002
     * @param cloudinaryUrl  l'URL du PDF sur Cloudinary
     * @return le fichier PNG du QR code
     */
    public File generateQrCode(String factureNumero, String cloudinaryUrl) throws WriterException, IOException {

        // L'URL encodée dans le QR code
        // En production : remplace par ton vrai serveur
        // Pour PIDEV : on encode directement l'URL Cloudinary + infos
        String verificationUrl = "https://packtrack-verify.netlify.app/?facture="
                + factureNumero
                + "&url=" + cloudinaryUrl;

        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrWriter.encode(verificationUrl, BarcodeFormat.QR_CODE, 200, 200);

        // Sauvegarder le QR code dans le dossier pdfs/
        File qrDir = new File("pdfs/qrcodes");
        qrDir.mkdirs();

        File qrFile = new File(qrDir, "qr_" + factureNumero + ".png");
        Path path = FileSystems.getDefault().getPath(qrFile.getAbsolutePath());
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

        System.out.println("QR Code généré : " + qrFile.getAbsolutePath());
        return qrFile;
    }
}