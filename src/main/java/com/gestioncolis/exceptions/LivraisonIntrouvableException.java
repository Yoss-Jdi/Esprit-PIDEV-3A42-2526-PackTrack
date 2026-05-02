package com.gestioncolis.exceptions;

public class LivraisonIntrouvableException extends ColisException {
    public LivraisonIntrouvableException(int colisId) {
        super("Livraison" + colisId + " introuvable");
    }
}
