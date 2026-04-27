package com.example.rayen.services;

import com.example.rayen.entities.technicien;
import com.example.rayen.entities.vehicule;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PdfGenerator {
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private PdfGenerator() {
    }

    public static String createVehiculePdf(vehicule vehiculeSelectionne) throws IOException {
        Path dossier = ensureOutputDirectory();
        String nomFichier = "vehicule_" + safe(vehiculeSelectionne.getMatricule()) + "_" + timestamp() + ".pdf";
        Path destination = dossier.resolve(nomFichier);

        PdfWriter writer = new PdfWriter(destination.toString());
        PdfDocument pdf = new PdfDocument(writer);
        try (Document document = new Document(pdf)) {

            document.add(new Paragraph("Fiche Vehicule"));
            document.add(new Paragraph("Date generation : " + LocalDateTime.now().format(DISPLAY_DATE_FORMAT)));
            document.add(new Paragraph("----------------------------------------"));
            document.add(new Paragraph("ID : " + vehiculeSelectionne.getId()));
            document.add(new Paragraph("Matricule : " + valeur(vehiculeSelectionne.getMatricule())));
            document.add(new Paragraph("Marque : " + valeur(vehiculeSelectionne.getMarque())));
            document.add(new Paragraph("Modele : " + valeur(vehiculeSelectionne.getModele())));
            document.add(new Paragraph("Couleur : " + valeur(vehiculeSelectionne.getCouleur())));
            document.add(new Paragraph("Prix location : " + vehiculeSelectionne.getPrixLocation()));
            document.add(new Paragraph("Disponibilite : " + (vehiculeSelectionne.isDisponible() ? "Disponible" : "Indisponible")));
            document.add(new Paragraph("Technicien assigne : " + valeur(vehiculeSelectionne.getTechnicienAffichage())));
        }

        return destination.toAbsolutePath().toString();
    }

    public static String createTechnicienPdf(technicien technicienSelectionne) throws IOException {
        Path dossier = ensureOutputDirectory();
        String nomFichier = "technicien_" + safe(technicienSelectionne.getNomComplet()) + "_" + timestamp() + ".pdf";
        Path destination = dossier.resolve(nomFichier);

        PdfWriter writer = new PdfWriter(destination.toString());
        PdfDocument pdf = new PdfDocument(writer);
        try (Document document = new Document(pdf)) {

            document.add(new Paragraph("Fiche Technicien"));
            document.add(new Paragraph("Date generation : " + LocalDateTime.now().format(DISPLAY_DATE_FORMAT)));
            document.add(new Paragraph("----------------------------------------"));
            document.add(new Paragraph("ID : " + technicienSelectionne.getId()));
            document.add(new Paragraph("Nom complet : " + valeur(technicienSelectionne.getNomComplet())));
            document.add(new Paragraph("Specialite : " + valeur(technicienSelectionne.getSpecialite())));
            document.add(new Paragraph("Telephone : " + valeur(technicienSelectionne.getTelephone())));
            document.add(new Paragraph("Email : " + valeur(technicienSelectionne.getEmail())));
            document.add(new Paragraph("Nombre de vehicules : " + technicienSelectionne.getNombreVehicules()));
            document.add(new Paragraph("Vehicules assignes : " + valeur(technicienSelectionne.getVehiculesResume())));
        }

        return destination.toAbsolutePath().toString();
    }

    private static Path ensureOutputDirectory() throws IOException {
        Path dossier = Paths.get(System.getProperty("user.home"), "Documents", "rayen-pdfs");
        Files.createDirectories(dossier);
        return dossier;
    }

    private static String timestamp() {
        return LocalDateTime.now().format(FILE_DATE_FORMAT);
    }

    private static String safe(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return "inconnu";
        }
        return valeur.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private static String valeur(String valeur) {
        return (valeur == null || valeur.isBlank()) ? "N/A" : valeur;
    }
}
