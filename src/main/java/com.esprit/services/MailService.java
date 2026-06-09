package com.esprit.services;

import com.esprit.entities.Recompense;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class MailService {

    // ← utilise les mêmes noms que ton MailService existant
    private static final String EMAIL    = "maisayousfi095@gmail.com";
    private static final String PASSWORD = "cxocaiswtbvuvbfc"; // App Password Gmail

    // =====================================================
    // MAIL RÉCOMPENSE
    // =====================================================
    public void envoyerMailRecompense(String emailLivreur, String nomLivreur, Recompense r) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth",            "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host",            "smtp.gmail.com");
                props.put("mail.smtp.port",            "587");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(EMAIL, PASSWORD);
                    }
                });

                Message msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(EMAIL));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailLivreur));
                msg.setSubject("Nouvelle recompense - PackTrack");

                String html =
                        "<html><body style='font-family:Arial,sans-serif;padding:20px;'>"
                                + "<h2 style='color:#4CAF50;'>Felicitations, " + nomLivreur + " !</h2>"
                                + "<p>Une nouvelle recompense vous a ete attribuee :</p>"
                                + "<table border='1' cellpadding='10' "
                                +   "style='border-collapse:collapse;width:400px;'>"
                                + "<tr style='background:#f5f5f5;'>"
                                +   "<td><b>Type</b></td><td>" + r.getType() + "</td></tr>"
                                + "<tr>"
                                +   "<td><b>Valeur</b></td><td>" + r.getValeur() + "</td></tr>"
                                + "<tr style='background:#f5f5f5;'>"
                                +   "<td><b>Description</b></td><td>" + r.getDescription() + "</td></tr>"
                                + "<tr>"
                                +   "<td><b>Seuil</b></td><td>" + r.getSeuil() + " pts</td></tr>"
                                + "<tr style='background:#f5f5f5;'>"
                                +   "<td><b>Date</b></td><td>" + r.getDateObtention() + "</td></tr>"
                                + "</table>"
                                + "<br><p style='color:#555;'>Merci pour votre excellent travail !"
                                + "<br><b>L equipe PackTrack</b></p>"
                                + "</body></html>";

                msg.setContent(html, "text/html; charset=utf-8");
                Transport.send(msg);
                System.out.println("Mail recompense envoye a : " + emailLivreur);

            } catch (MessagingException e) {
                System.err.println("Erreur envoi mail : " + e.getMessage());
            }
        }).start();
    }
}