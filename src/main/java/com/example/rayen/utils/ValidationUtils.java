package com.example.rayen.utils;

import java.util.regex.Pattern;

public final class ValidationUtils {
    private static final Pattern MATRICULE_PATTERN = Pattern.compile("^\\d{3}TN\\d{4}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private ValidationUtils() {
    }

    public static String normaliserMatricule(String matricule) {
        String valeurNormalisee = matricule == null ? "" : matricule.trim().toUpperCase().replace(" ", "");
        if (valeurNormalisee.isEmpty()) {
            throw new IllegalArgumentException("Le matricule est obligatoire.");
        }

        if (!MATRICULE_PATTERN.matcher(valeurNormalisee).matches()) {
            throw new IllegalArgumentException("Le matricule doit respecter le format 123TN4567.");
        }

        return valeurNormalisee;
    }

    public static String normaliserEmailObligatoire(String email, String nomChamp) {
        String valeurNormalisee = email == null ? "" : email.trim().toLowerCase();
        if (valeurNormalisee.isEmpty()) {
            throw new IllegalArgumentException("Le " + nomChamp + " est obligatoire.");
        }

        if (!EMAIL_PATTERN.matcher(valeurNormalisee).matches()) {
            throw new IllegalArgumentException("L'email doit etre valide et contenir le caractere @.");
        }

        return valeurNormalisee;
    }

    public static String normaliserEmailOptionnel(String email) {
        String valeurNormalisee = email == null ? "" : email.trim().toLowerCase();
        if (valeurNormalisee.isEmpty()) {
            return "";
        }

        if (!EMAIL_PATTERN.matcher(valeurNormalisee).matches()) {
            throw new IllegalArgumentException("L'email doit etre valide et contenir le caractere @.");
        }

        return valeurNormalisee;
    }
}
