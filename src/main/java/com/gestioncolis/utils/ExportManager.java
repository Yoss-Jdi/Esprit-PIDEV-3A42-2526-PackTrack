package com.gestioncolis.utils;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import com.gestioncolis.entities.Utilisateurs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportManager {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Couleurs design (indigo/purple theme)
    private static final Color PRIMARY_COLOR = new DeviceRgb(99, 102, 241);
    private static final Color HEADER_BG = new DeviceRgb(99, 102, 241);
    private static final Color HEADER_TEXT = new DeviceRgb(255, 255, 255);
    private static final Color ROW_ALT = new DeviceRgb(248, 250, 252);
    private static final Color BORDER_COLOR = new DeviceRgb(226, 232, 240);

    /**
     * Exporte la liste des utilisateurs au format PDF
     */
    public static void exportToPDF(List<Utilisateurs> users, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le fichier PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        fileChooser.setInitialFileName("utilisateurs_" + LocalDateTime.now().format(FILE_DATE_FMT) + ".pdf");

        File file = fileChooser.showSaveDialog(stage);
        if (file == null) return;

        try {
            PdfWriter writer = new PdfWriter(file);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4.rotate());
            document.setMargins(20, 20, 20, 20);

            // ─── EN-TÊTE MODERNE ─────────────────────────────────────────
            // Logo / Titre
            Paragraph title = new Paragraph("📊 TrackPack - Liste des utilisateurs")
                    .setFontSize(18)
                    .setBold()
                    .setFontColor(PRIMARY_COLOR)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Généré le " + LocalDateTime.now().format(DATE_FMT))
                    .setFontSize(10)
                    .setFontColor(new DeviceRgb(100, 116, 139))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(subtitle);

            // Statistique rapide
            long totalAdmins = users.stream().filter(u -> u.getRole().name().equals("ADMIN")).count();
            long totalClients = users.stream().filter(u -> u.getRole().name().equals("CLIENT")).count();
            long totalLivreurs = users.stream().filter(u -> u.getRole().name().equals("LIVREUR")).count();

            Paragraph stats = new Paragraph()
                    .add(new Paragraph("Total utilisateurs : " + users.size()).setBold())
                    .add(new Paragraph("👥 Admins : " + totalAdmins + "   👤 Clients : " + totalClients + "   🚚 Livreurs : " + totalLivreurs))
                    .setFontSize(10)
                    .setFontColor(new DeviceRgb(71, 85, 105))
                    .setMarginBottom(15);
            document.add(stats);

            // ─── TABLEAU PRINCIPAL ───────────────────────────────────────
            float[] columnWidths = {40, 80, 80, 160, 100, 90, 90, 100};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setMarginTop(10);

            // En-têtes stylisés
            String[] headers = {"#", "Prénom", "Nom", "Email", "Téléphone", "Rôle", "Date", "Inscrit le"};
            for (String header : headers) {
                Cell headerCell = new Cell()
                        .add(new Paragraph(header).setBold().setFontColor(HEADER_TEXT))
                        .setBackgroundColor(HEADER_BG)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(8)
                        .setBorder(Border.NO_BORDER);
                table.addHeaderCell(headerCell);
            }

            // Remplissage des données
            int rowIndex = 0;
            for (Utilisateurs u : users) {
                boolean isEven = rowIndex % 2 == 0;
                Color rowBg = isEven ? ROW_ALT : new DeviceRgb(255, 255, 255);

                table.addCell(createStyledCell(String.valueOf(u.getIdUtilisateur()), rowBg));
                table.addCell(createStyledCell(u.getPrenom(), rowBg));
                table.addCell(createStyledCell(u.getNom(), rowBg));
                table.addCell(createStyledCell(u.getEmail(), rowBg));
                table.addCell(createStyledCell(u.getTelephone() != null ? u.getTelephone() : "—", rowBg));
                table.addCell(createRoleCell(u.getRole().name(), rowBg));
                table.addCell(createStyledCell(u.getCreatedAt() != null ?
                        u.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—", rowBg));
                table.addCell(createStyledCell(u.getCreatedAt() != null ?
                        u.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")) : "—", rowBg));
                rowIndex++;
            }

            document.add(table);

            // Pied de page
            document.add(new Paragraph("\n📁 TrackPack Administration — Document confidentiel")
                    .setFontSize(8)
                    .setFontColor(new DeviceRgb(148, 163, 184))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20));

            document.close();
            showSuccessNotification("✅ Export PDF réussi : " + file.getName());

        } catch (IOException e) {
            showErrorNotification("❌ Erreur lors de l'export PDF : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Cell createStyledCell(String text, Color bgColor) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "—").setFontSize(9))
                .setBackgroundColor(bgColor)
                .setPadding(6)
                .setBorder(Border.NO_BORDER);
    }

    private static Cell createRoleCell(String role, Color bgColor) {
        DeviceRgb roleColor;
        switch (role) {
            case "ADMIN": roleColor = new DeviceRgb(99, 102, 241); break;
            case "CLIENT": roleColor = new DeviceRgb(59, 130, 246); break;
            case "ENTREPRISE": roleColor = new DeviceRgb(16, 185, 129); break;
            case "LIVREUR": roleColor = new DeviceRgb(245, 158, 11); break;
            case "TECHNICIEN": roleColor = new DeviceRgb(239, 68, 68); break;
            default: roleColor = new DeviceRgb(148, 163, 184);
        }
        return new Cell()
                .add(new Paragraph(role).setFontSize(9).setFontColor(roleColor).setBold())
                .setBackgroundColor(bgColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(6)
                .setBorder(Border.NO_BORDER);
    }

    /**
     * Exporte la liste des utilisateurs au format CSV
     */
    public static void exportToCSV(List<Utilisateurs> users, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le fichier CSV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        fileChooser.setInitialFileName("utilisateurs_" + LocalDateTime.now().format(FILE_DATE_FMT) + ".csv");

        File file = fileChooser.showSaveDialog(stage);
        if (file == null) return;

        try (FileWriter writer = new FileWriter(file);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader("ID", "Prénom", "Nom", "Email", "Téléphone", "Rôle", "Date d'inscription", "Heure")
                     .build())) {

            for (Utilisateurs u : users) {
                csvPrinter.printRecord(
                        u.getIdUtilisateur(),
                        u.getPrenom(),
                        u.getNom(),
                        u.getEmail(),
                        u.getTelephone() != null ? u.getTelephone() : "",
                        u.getRole().name(),
                        u.getCreatedAt() != null ? u.getCreatedAt().toLocalDate().toString() : "",
                        u.getCreatedAt() != null ? u.getCreatedAt().toLocalTime().toString() : ""
                );
            }
            csvPrinter.flush();
            showSuccessNotification("✅ Export CSV réussi : " + file.getName());

        } catch (IOException e) {
            showErrorNotification("❌ Erreur lors de l'export CSV : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void showSuccessNotification(String message) {
        System.out.println("✅ " + message);
        // Si vous avez un système de notification, appelez-le ici
        // Par exemple : NotificationUtil.show(message, NotificationType.SUCCESS);
    }

    private static void showErrorNotification(String message) {
        System.err.println("❌ " + message);
    }
}