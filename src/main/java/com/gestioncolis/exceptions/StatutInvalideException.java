package com.gestioncolis.exceptions;

public class StatutInvalideException extends ColisException {
  public StatutInvalideException(String statut) {
    super("Statut invalide ou transition non autorisée : '" + statut + "'");
  }
}