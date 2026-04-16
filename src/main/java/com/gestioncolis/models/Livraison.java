package com.gestioncolis.models;

import com.gestioncolis.exceptions.StatutInvalideException;
import com.gestioncolis.exceptions.ValidationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Livraison {
    private int           id;
    private String        statut;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private double        distanceKm;
    private double        dureeEstimeeMinutes;
    private double        total;
    private int           colisId;
    private int           livreurId;


    private String nomLivreur;
    private String descriptionColis;

    public Livraison(int id, String statut, LocalDateTime dateDebut, LocalDateTime dateFin, double distanceKm,
                     double dureeEstimeeMinutes, double total, int colisId, int livreurId) {
        this.id                  = id;
        this.statut              = statut;
        this.dateDebut           = dateDebut;
        this.dateFin             = dateFin;
        this.distanceKm          = distanceKm;
        this.dureeEstimeeMinutes = dureeEstimeeMinutes;
        this.total               = total;
        this.colisId             = colisId;
        this.livreurId           = livreurId;
    }

    public Livraison(int colisId, int livreurId, double total) {
        this(0, "en_cours", LocalDateTime.now(), null, 0, 0, total, colisId, livreurId);
    }

    public String getDureeFormatee() {
        if (dureeEstimeeMinutes <= 0) return "-";
        int h = (int) dureeEstimeeMinutes / 60;
        int m = (int) dureeEstimeeMinutes % 60;
        return h > 0 ? h + "h " + m + "min" : m + " min";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public double getDureeEstimeeMinutes() {
        return dureeEstimeeMinutes;
    }

    public void setDureeEstimeeMinutes(double dureeEstimeeMinutes) {
        this.dureeEstimeeMinutes = dureeEstimeeMinutes;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public int getColisId() {
        return colisId;
    }

    public void setColisId(int colisId) {
        this.colisId = colisId;
    }

    public int getLivreurId() {
        return livreurId;
    }

    public void setLivreurId(int livreurId) {
        this.livreurId = livreurId;
    }

    public String getNomLivreur() {
        return nomLivreur != null ? nomLivreur : "-";
    }

    public void setNomLivreur(String nomLivreur) {
        this.nomLivreur = nomLivreur;
    }

    public String getDescriptionColis() {
        return descriptionColis != null ? descriptionColis : "-";
    }

    public void setDescriptionColis(String descriptionColis) {
        this.descriptionColis = descriptionColis;
    }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return "Livraison #" + id + " | " + statut + " | Colis: " + descriptionColis
                + " | Livreur: " + nomLivreur
                + " | " + (dateDebut != null ? dateDebut.format(fmt) : "-");
    }

    public void valider() throws ValidationException, StatutInvalideException {
        if (colisId <= 0)
            throw new ValidationException("colisId", "Le colis est obligatoire");
        if (livreurId <= 0)
            throw new ValidationException("livreurId", "Le livreur est obligatoire");
        List<String> statutsValides = List.of("en_cours", "termine");
        if (statut == null || !statutsValides.contains(statut))
            throw new StatutInvalideException(statut);
        if (distanceKm < 0)
            throw new ValidationException("distanceKm", "La distance ne peut pas etre negative");
        if (distanceKm >= 10000)
            throw new ValidationException("distanceKm", "La distance ne peut pas depasser 10 000 km");
    }
}