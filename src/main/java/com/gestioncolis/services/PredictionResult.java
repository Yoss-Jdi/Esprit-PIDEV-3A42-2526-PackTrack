package com.gestioncolis.services;

/**
 * DTO — résultat renvoyé par le service ML Flask (/predict-complet).
 *
 * Champs JSON mappés :
 *   distance_km, duree_minutes, duree_formatee,
 *   coords_depart, coords_destination
 */
public class PredictionResult {

    private final double  distanceKm;
    private final double  dureeMinutes;
    private final String  dureeFormatee;
    private final boolean succes;
    private final String  erreur;

    /** Constructeur succès */
    public PredictionResult(double distanceKm, double dureeMinutes, String dureeFormatee) {
        this.distanceKm    = distanceKm;
        this.dureeMinutes  = dureeMinutes;
        this.dureeFormatee = dureeFormatee;
        this.succes        = true;
        this.erreur        = null;
    }

    /** Constructeur échec / fallback */
    public PredictionResult(String erreur) {
        this.distanceKm    = 0;
        this.dureeMinutes  = 0;
        this.dureeFormatee = "-";
        this.succes        = false;
        this.erreur        = erreur;
    }

    public double  getDistanceKm()    { return distanceKm; }
    public double  getDureeMinutes()  { return dureeMinutes; }
    public String  getDureeFormatee() { return dureeFormatee; }
    public boolean isSucces()         { return succes; }
    public String  getErreur()        { return erreur; }

    @Override
    public String toString() {
        return succes
                ? String.format("PredictionResult[%.2f km | %.0f min | %s]",
                distanceKm, dureeMinutes, dureeFormatee)
                : "PredictionResult[ERREUR: " + erreur + "]";
    }
}