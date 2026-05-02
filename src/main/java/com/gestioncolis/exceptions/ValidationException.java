package com.gestioncolis.exceptions;

public class ValidationException extends ColisException {
    private final String champ;

    public ValidationException(String champ, String message) {
        super("Champ [" + champ + "] : " + message);
        this.champ = champ;
    }

    public String getChamp() {
        return champ;
    }
}
