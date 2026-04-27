package com.gestioncolis.utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class EmailService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String EMAIL_FROM = "bargaouiyassine860@gmail.com";
    private static final String EMAIL_PASSWORD = "gser tfcx nwtw ntbs";

    /**
     * Envoi asynchrone — ne bloque pas le thread JavaFX UI.
     * onResult reçoit true si succès, false sinon (appelé sur un thread background).
     * Utilisez Platform.runLater() dans le callback pour mettre à jour l'UI.
     */
    public static void sendResetCodeAsync(String toEmail, String resetCode,
                                          String userName, Consumer<Boolean> onResult) {
        CompletableFuture.supplyAsync(() -> sendResetCode(toEmail, resetCode, userName))
                .thenAccept(onResult);
    }

    /**
     * Tente d'abord STARTTLS sur le port 587, puis SSL sur le port 465 en fallback.
     * Le port 587 est souvent bloqué par les firewalls/proxies réseau —
     * le port 465 (SSL direct) passe plus facilement.
     */
    public static boolean sendResetCode(String toEmail, String resetCode, String userName) {
        // Tentative 1 : STARTTLS port 587 (standard)
        if (trySend(toEmail, resetCode, userName, "587", false)) return true;

        System.err.println("⚠️ Port 587 échoué, tentative sur port 465 (SSL)...");

        // Tentative 2 : SSL direct port 465 (fallback si 587 bloqué par firewall)
        return trySend(toEmail, resetCode, userName, "465", true);
    }

    private static boolean trySend(String toEmail, String resetCode, String userName,
                                   String port, boolean useSSL) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        if (useSSL) {
            // Port 465 : SSL direct (socketFactory)
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", port);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        } else {
            // Port 587 : STARTTLS
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });
        // session.setDebug(true); // Décommentez pour voir les échanges SMTP en détail

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, "TrackPack Support"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("🔐 Réinitialisation de votre mot de passe - TrackPack");
            message.setContent(generateHtmlEmail(userName, resetCode), "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email envoyé avec succès à " + toEmail + " (port " + port + ")");
            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email (port " + port + "): " + e.getMessage());
            return false;
        }
    }

    private static String generateHtmlEmail(String userName, String resetCode) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: 'Segoe UI', Arial, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        margin: 0;
                        padding: 0;
                    }
                    .container {
                        max-width: 550px;
                        margin: 40px auto;
                        background: white;
                        border-radius: 24px;
                        overflow: hidden;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                    }
                    .header {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        padding: 40px 30px;
                        text-align: center;
                    }
                    .logo {
                        font-size: 48px;
                        margin-bottom: 10px;
                    }
                    .logo-text {
                        font-size: 28px;
                        font-weight: bold;
                        color: white;
                        letter-spacing: 2px;
                    }
                    .content {
                        padding: 40px 35px;
                        background: white;
                    }
                    .greeting {
                        font-size: 24px;
                        font-weight: bold;
                        color: #1e1b4b;
                        margin-bottom: 15px;
                    }
                    .message {
                        color: #4b5563;
                        line-height: 1.6;
                        margin: 20px 0;
                        font-size: 15px;
                    }
                    .code-container {
                        background: linear-gradient(135deg, #f0f4ff 0%%, #e8eeff 100%%);
                        border-radius: 16px;
                        padding: 25px;
                        text-align: center;
                        margin: 30px 0;
                        border: 2px solid #667eea;
                    }
                    .reset-code {
                        font-size: 36px;
                        font-weight: bold;
                        letter-spacing: 8px;
                        color: #4f46e5;
                        font-family: 'Courier New', monospace;
                        background: white;
                        padding: 15px 20px;
                        border-radius: 12px;
                        display: inline-block;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }
                    .expiry-note {
                        font-size: 12px;
                        color: #ef4444;
                        text-align: center;
                        margin-top: 15px;
                    }
                    .footer {
                        text-align: center;
                        padding: 25px;
                        background: #f8fafc;
                        color: #94a3b8;
                        font-size: 12px;
                        border-top: 1px solid #e2e8f0;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">🎯</div>
                        <div class="logo-text">TrackPack</div>
                    </div>
                    <div class="content">
                        <div class="greeting">Bonjour %s !</div>
                        <div class="message">
                            Nous avons reçu une demande de réinitialisation de votre mot de passe 
                            pour votre compte TrackPack. Si vous n'êtes pas à l'origine de cette 
                            demande, vous pouvez ignorer cet email en toute sécurité.
                        </div>
                        <div class="code-container">
                            <div style="margin-bottom: 10px; color: #4f46e5; font-weight: bold;">
                                🔐 Votre code de réinitialisation
                            </div>
                            <div class="reset-code">%s</div>
                            <div class="expiry-note">
                                ⏰ Ce code expirera dans 15 minutes
                            </div>
                        </div>
                        <div class="message">
                            Pour réinitialiser votre mot de passe, entrez ce code sur la page 
                            de réinitialisation de TrackPack.
                        </div>
                    </div>
                    <div class="footer">
                        <div>© 2024 TrackPack - Tous droits réservés</div>
                        <div style="margin-top: 10px; font-size: 11px;">
                            Cet email a été envoyé automatiquement, merci de ne pas y répondre.
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, userName, resetCode);
    }

    public static String generateResetCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}