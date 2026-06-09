package com.esprit.entities;

import java.util.Date;

public class Recompense {

    private int idRecompense;
    private String type;
    private double valeur;
    private String description;
    private int seuil;
    private Date dateObtention;
    private int idLivreur;
    private Integer idFacture;

    public Recompense() {}

    // Constructeur sans id (pour ajouter)
    public Recompense(String type, double valeur, String description,
                      int seuil, Date dateObtention, int idLivreur, Integer idFacture) {
        this.type = type;
        this.valeur = valeur;
        this.description = description;
        this.seuil = seuil;
        this.dateObtention = dateObtention;
        this.idLivreur = idLivreur;
        this.idFacture = idFacture;
    }

    // Constructeur complet avec id (pour afficher)
    public Recompense(int idRecompense, String type, double valeur, String description,
                      int seuil, Date dateObtention, int idLivreur, Integer idFacture) {
        this.idRecompense = idRecompense;
        this.type = type;
        this.valeur = valeur;
        this.description = description;
        this.seuil = seuil;
        this.dateObtention = dateObtention;
        this.idLivreur = idLivreur;
        this.idFacture = idFacture;
    }

    // Getters & Setters
    public int getIdRecompense() { return idRecompense; }
    public void setIdRecompense(int idRecompense) { this.idRecompense = idRecompense; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getValeur() { return valeur; }
    public void setValeur(double valeur) { this.valeur = valeur; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getSeuil() { return seuil; }
    public void setSeuil(int seuil) { this.seuil = seuil; }
    public Date getDateObtention() { return dateObtention; }
    public void setDateObtention(Date dateObtention) { this.dateObtention = dateObtention; }
    public int getIdLivreur() { return idLivreur; }
    public void setIdLivreur(int idLivreur) { this.idLivreur = idLivreur; }
    public Integer getIdFacture() { return idFacture; }
    public void setIdFacture(Integer idFacture) { this.idFacture = idFacture; }

    @Override
    public String toString() {
        return "Recompense{" +
                "idRecompense=" + idRecompense +
                ", type='" + type + '\'' +
                ", valeur=" + valeur +
                ", description='" + description + '\'' +
                ", seuil=" + seuil +
                ", dateObtention=" + dateObtention +
                ", idLivreur=" + idLivreur +
                ", idFacture=" + idFacture +
                '}';
    }
}