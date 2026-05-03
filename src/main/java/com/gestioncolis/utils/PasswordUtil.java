package com.gestioncolis.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // Facteur de coût - plus il est élevé, plus le hachage est lent et sécurisé
    // Recommandé: entre 10 et 12
    private static final int LOG_ROUNDS = 10;

    /**
     * Hache un mot de passe en utilisant BCrypt
     * @param plainPassword - le mot de passe en clair
     * @return le mot de passe haché
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    /**
     * Vérifie si un mot de passe en clair correspond à un hachage BCrypt
     * @param plainPassword - le mot de passe en clair à vérifier
     * @param hashedPassword - le hachage stocké en base de données
     * @return true si le mot de passe correspond, false sinon
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || plainPassword.isEmpty() || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}