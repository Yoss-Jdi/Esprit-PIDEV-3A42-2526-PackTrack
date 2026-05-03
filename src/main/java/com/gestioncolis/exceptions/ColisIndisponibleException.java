package com.gestioncolis.exceptions;

public class ColisIndisponibleException extends ColisException {
    public ColisIndisponibleException(int colisId) {
        super("Colis #" + colisId + " introuvable ou déjà pris en charge");
    }
}