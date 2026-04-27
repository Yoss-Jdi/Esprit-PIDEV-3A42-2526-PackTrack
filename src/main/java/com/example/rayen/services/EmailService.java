package com.example.rayen.services;

import com.example.rayen.entities.technicien;
import com.example.rayen.entities.vehicule;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class EmailService {
    private static final String DEFAULT_SMTP_HOST = "smtp.gmail.com";
    private static final String DEFAULT_SMTP_PORT = "587";
    private static final String DEFAULT_MAIL_USERNAME = "amir.belhajs@gmail.com";
    private static final String DEFAULT_MAIL_PASSWORD = "uxcdqkfsnzwjfptn";
    private static final String DEFAULT_MAIL_FROM = DEFAULT_MAIL_USERNAME;

    private EmailService() {
    }

    public static void sendVehiculeCreationEmail(String to, vehicule vehiculeAjoute) {
        if (vehiculeAjoute == null) {
            throw new IllegalArgumentException("Le vehicule a envoyer par email est obligatoire.");
        }

        sendEmail(
                to,
                "Confirmation ajout vehicule " + vehiculeAjoute.getMatricule(),
                construireMessageVehicule(vehiculeAjoute)
        );
    }

    public static void sendTechnicienCreationEmail(technicien technicienAjoute) {
        if (technicienAjoute == null) {
            throw new IllegalArgumentException("Le technicien a envoyer par email est obligatoire.");
        }

        sendEmail(
                technicienAjoute.getEmail(),
                "Confirmation ajout technicien " + technicienAjoute.getNomComplet(),
                construireMessageTechnicien(technicienAjoute)
        );
    }

    public static void sendEmail(String to, String subject, String body) {
        String destinataire = normaliserEmail(to, "destinataire");
        String username = lireConfigurationObligatoire("MAIL_USERNAME", DEFAULT_MAIL_USERNAME);
        String password = lireConfigurationObligatoire("MAIL_PASSWORD", DEFAULT_MAIL_PASSWORD);
        String from = lireConfiguration("MAIL_FROM", DEFAULT_MAIL_FROM);

        Properties props = new Properties();
        props.put("mail.smtp.host", lireConfiguration("MAIL_HOST", DEFAULT_SMTP_HOST));
        props.put("mail.smtp.port", lireConfiguration("MAIL_PORT", DEFAULT_SMTP_PORT));
        props.put("mail.smtp.auth", lireConfiguration("MAIL_SMTP_AUTH", "true"));
        props.put("mail.smtp.starttls.enable", lireConfiguration("MAIL_SMTP_STARTTLS", "true"));

        String sslTrust = lireConfiguration("MAIL_SMTP_SSL_TRUST", null);
        if (sslTrust != null && !sslTrust.isBlank()) {
            props.put("mail.smtp.ssl.trust", sslTrust);
        }

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject(subject, StandardCharsets.UTF_8.name());
            message.setText(body, StandardCharsets.UTF_8.name());
            Transport.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException(
                    "Impossible d'envoyer l'email. Verifiez la configuration SMTP et l'acces au serveur mail.",
                    exception
            );
        }
    }

    private static String construireMessageVehicule(vehicule vehiculeAjoute) {
        return """
                Bonjour,

                Le vehicule a bien ete ajoute avec les informations suivantes :

                Matricule : %s
                Marque : %s
                Modele : %s
                Couleur : %s
                Prix location : %s
                Disponibilite : %s
                Technicien assigne : %s

                Cordialement,
                Application Rayen
                """.formatted(
                valeur(vehiculeAjoute.getMatricule()),
                valeur(vehiculeAjoute.getMarque()),
                valeur(vehiculeAjoute.getModele()),
                valeur(vehiculeAjoute.getCouleur()),
                vehiculeAjoute.getPrixLocation(),
                vehiculeAjoute.isDisponible() ? "Disponible" : "Indisponible",
                valeur(vehiculeAjoute.getTechnicienAffichage())
        );
    }

    private static String construireMessageTechnicien(technicien technicienAjoute) {
        return """
                Bonjour,

                Le technicien a bien ete ajoute avec les informations suivantes :

                Nom complet : %s
                Specialite : %s
                Telephone : %s
                Email : %s
                Vehicules assignes : %s

                Cordialement,
                Application Rayen
                """.formatted(
                valeur(technicienAjoute.getNomComplet()),
                valeur(technicienAjoute.getSpecialite()),
                valeur(technicienAjoute.getTelephone()),
                valeur(technicienAjoute.getEmail()),
                valeur(technicienAjoute.getVehiculesResume())
        );
    }

    private static String lireConfigurationObligatoire(String cle, String valeurParDefaut) {
        String valeur = lireConfiguration(cle, valeurParDefaut);
        if (valeur == null || valeur.isBlank() || "your_app_password".equals(valeur)) {
            throw new IllegalStateException(
                    "Configuration email incomplete. Definissez " + cle + " dans EmailService.java, en variable d'environnement ou en propriete Java."
            );
        }
        return valeur;
    }

    private static String lireConfiguration(String cle, String valeurParDefaut) {
        String valeur = System.getProperty(cle);
        if (valeur == null || valeur.isBlank()) {
            valeur = System.getenv(cle);
        }

        if (valeur == null || valeur.isBlank()) {
            return valeurParDefaut;
        }

        return valeur.trim();
    }

    private static String normaliserEmail(String email, String typeEmail) {
        String emailNormalise = email == null ? "" : email.trim().toLowerCase();
        if (emailNormalise.isEmpty()) {
            throw new IllegalArgumentException("L'email du " + typeEmail + " est obligatoire.");
        }
        if (!emailNormalise.contains("@")) {
            throw new IllegalArgumentException("L'email du " + typeEmail + " doit contenir le caractere @.");
        }
        return emailNormalise;
    }

    private static String valeur(String valeur) {
        return valeur == null || valeur.isBlank() ? "N/A" : valeur;
    }
}
