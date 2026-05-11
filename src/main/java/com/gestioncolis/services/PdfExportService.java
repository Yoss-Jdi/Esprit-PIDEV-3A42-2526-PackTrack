package com.gestioncolis.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.gestioncolis.entities.Post;
import com.gestioncolis.utils.DateTimeUtils;

public class PdfExportService {

    // PDF: Constantes de mise en page
    private static final float PAGE_WIDTH = 595;
    private static final float PAGE_HEIGHT = 842;
    private static final float MARGIN_LEFT = 40;
    private static final float MARGIN_BOTTOM = 60;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - 40;

    // PDF: Polices PDFBox 2.0.29
    private final PDType1Font fontBold;
    private final PDType1Font fontRegular;

    public PdfExportService() {
        // PDF: Initialisation des polices version 2.0.29
        this.fontBold = PDType1Font.HELVETICA_BOLD;
        this.fontRegular = PDType1Font.HELVETICA;
    }

    /**
     * PDF: Exporte un post individuel en PDF
     */
    public String exportPost(Post post) {
        if (post == null) {
            throw new IllegalArgumentException("Post invalide.");
        }

        String outputPath = getDownloadsPath();
        String filename = "post_" + post.getId() + ".pdf";
        String filepath = Paths.get(outputPath, filename).toString();

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - 60;

                stream.setFont(fontBold, 16);
                stream.beginText();
                stream.newLineAtOffset(MARGIN_LEFT, y);
                stream.showText(truncate(sanitizePdfText(post.getTitle()), 70));
                stream.endText();
                y -= 25;

                stream.setFont(fontRegular, 10);
                stream.beginText();
                stream.newLineAtOffset(MARGIN_LEFT, y);
                String forum = post.getForum() != null ? post.getForum().getTitle() : "Forum inconnu";
                stream.showText("Forum : " + sanitizePdfText(forum));
                stream.endText();
                y -= 15;

                stream.setFont(fontRegular, 10);
                stream.beginText();
                stream.newLineAtOffset(MARGIN_LEFT, y);
                String auteur = post.getAuthor() != null ? post.getAuthor().getNom() : "Auteur inconnu";
                stream.showText("Auteur : " + sanitizePdfText(auteur));
                stream.endText();
                y -= 15;

                stream.setFont(fontRegular, 10);
                stream.beginText();
                stream.newLineAtOffset(MARGIN_LEFT, y);
                String date = post.getCreatedAt() != null
                        ? DateTimeUtils.format(post.getCreatedAt()) : "";
                stream.showText("Date : " + sanitizePdfText(date));
                stream.endText();
                y -= 15;

                stream.setFont(fontRegular, 10);
                stream.beginText();
                stream.newLineAtOffset(MARGIN_LEFT, y);
                stream.showText("Likes : " + post.getLikeCount());
                stream.endText();
                y -= 20;

                stream.setLineWidth(0.5f);
                stream.moveTo(MARGIN_LEFT, y);
                stream.lineTo(PAGE_WIDTH - 40, y);
                stream.stroke();
                y -= 15;

                stream.setFont(fontRegular, 11);
                y = addWrappedText(stream, sanitizePdfText(post.getContent()), 11, MARGIN_LEFT, y);
            }

            document.save(filepath);
            return filepath;

        } catch (IOException e) {
            throw new RuntimeException("Erreur génération PDF : " + e.getMessage(), e);
        }
    }

    /**
     * PDF: Exporte tous les posts d'un forum en rapport PDF trié par likes DESC
     */
    public String exportForumReport(List<Post> posts, String forumName) {
        if (posts == null || posts.isEmpty()) {
            throw new IllegalArgumentException("Aucun post à exporter.");
        }

        posts.sort(Comparator.comparingLong(Post::getLikeCount).reversed());

        String outputPath = getDownloadsPath();
        String filename = "rapport_" + sanitize(forumName) + ".pdf";
        String filepath = Paths.get(outputPath, filename).toString();

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            float y = PAGE_HEIGHT - 60;

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.setFont(fontBold, 18);
                stream.beginText();
                stream.newLineAtOffset(MARGIN_LEFT, y);
                stream.showText(sanitizePdfText("Rapport - " + truncate(forumName, 50)));
                stream.endText();
                y -= 20;

                stream.setFont(fontRegular, 9);
                stream.beginText();
                stream.newLineAtOffset(MARGIN_LEFT, y);
                stream.showText("Genere le " + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                stream.endText();
                y -= 10;

                stream.setFont(fontRegular, 9);
                stream.beginText();
                stream.newLineAtOffset(MARGIN_LEFT, y);
                stream.showText(posts.size() + " post(s) - tries par likes");
                stream.endText();
                y -= 20;

                stream.setLineWidth(1f);
                stream.moveTo(MARGIN_LEFT, y);
                stream.lineTo(PAGE_WIDTH - 40, y);
                stream.stroke();
                y -= 20;
            }

            for (Post post : posts) {
                if (y < MARGIN_BOTTOM + 80) {
                    page = new PDPage();
                    document.addPage(page);
                    y = PAGE_HEIGHT - 60;
                }

                PDPage currentPage = document.getPage(document.getNumberOfPages() - 1);
                try (PDPageContentStream stream = new PDPageContentStream(
                        document, currentPage, PDPageContentStream.AppendMode.APPEND, true)) {

                    stream.setFont(fontBold, 12);
                    stream.beginText();
                    stream.newLineAtOffset(MARGIN_LEFT, y);
                    stream.showText(truncate(sanitizePdfText(post.getTitle()), 70));
                    stream.endText();
                    y -= 14;

                    stream.setFont(fontRegular, 9);
                    stream.beginText();
                    stream.newLineAtOffset(MARGIN_LEFT, y);
                    String auteur = post.getAuthor() != null ? post.getAuthor().getNom() : "?";
                    String date = post.getCreatedAt() != null
                            ? DateTimeUtils.format(post.getCreatedAt()) : "";
                    stream.showText("Auteur : " + sanitizePdfText(auteur)
                            + "  |  Likes : " + post.getLikeCount()
                            + "  |  Date : " + sanitizePdfText(date));
                    stream.endText();
                    y -= 12;

                    stream.setFont(fontRegular, 10);
                    y = addWrappedText(stream, sanitizePdfText(post.getContent()), 10, MARGIN_LEFT, y);
                    y -= 10;

                    stream.setLineWidth(0.3f);
                    stream.moveTo(MARGIN_LEFT, y);
                    stream.lineTo(PAGE_WIDTH - 40, y);
                    stream.stroke();
                    y -= 15;
                }
            }

            document.save(filepath);
            return filepath;

        } catch (IOException e) {
            throw new RuntimeException("Erreur generation rapport PDF : " + e.getMessage(), e);
        }
    }

    private float addWrappedText(PDPageContentStream stream, String text,
                                  float fontSize, float x, float y) throws IOException {
        if (text == null || text.isBlank()) return y;

        float lineHeight = fontSize + 3;
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        float charWidth = fontSize * 0.52f;

        for (String word : words) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (test.length() * charWidth > CONTENT_WIDTH) {
                if (!line.isEmpty()) {
                    stream.beginText();
                    stream.newLineAtOffset(x, y);
                    stream.showText(line.toString());
                    stream.endText();
                    y -= lineHeight;
                    line = new StringBuilder(word);
                } else {
                    stream.beginText();
                    stream.newLineAtOffset(x, y);
                    stream.showText(truncate(word, 80));
                    stream.endText();
                    y -= lineHeight;
                }
            } else {
                line = new StringBuilder(test);
            }
        }

        if (!line.isEmpty()) {
            stream.beginText();
            stream.newLineAtOffset(x, y);
            stream.showText(line.toString());
            stream.endText();
            y -= lineHeight;
        }

        return y;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) return "export";
        return name.replaceAll("[^a-zA-Z0-9_-]", "_")
                .substring(0, Math.min(name.length(), 30));
    }

    // PDF: Nettoie le texte pour éviter les crashs PDFBox liés à WinAnsiEncoding
    private String sanitizePdfText(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Ignorer les surrogates (emojis)
            if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
                continue;
            }
            // WinAnsiEncoding range + Euro et caractères français courants
            if (c == 0x20AC || c == 0x0153 || c == 0x0152 || (c >= 0x0020 && c <= 0x00FF)) {
                sb.append(c);
            } else {
                // Remplacer caractères inconnus par '?'
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private String getDownloadsPath() {
        String path = Paths.get(System.getProperty("user.home"), "Downloads").toString();
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'acceder au dossier Telechargements.", e);
        }
        return path;
    }
}