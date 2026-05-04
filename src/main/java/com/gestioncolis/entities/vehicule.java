package com.gestioncolis.entities;

public class vehicule {
    private int id;
    private String matricule;
    private String marque;
    private String modele;
    private String couleur;
    private double prixLocation;
    private boolean disponible;
    private Integer technicienId;
    private String technicienNom;

    public vehicule() {
    }

    public vehicule(String matricule, String marque, String modele, String couleur, double prixLocation, boolean disponible) {
        this.matricule = matricule;
        this.marque = marque;
        this.modele = modele;
        this.couleur = couleur;
        this.prixLocation = prixLocation;
        this.disponible = disponible;
    }

    public vehicule(int id, String matricule, String marque, String modele, String couleur, double prixLocation, boolean disponible) {
        this(matricule, marque, modele, couleur, prixLocation, disponible);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public String getModele() {
        return modele;
    }

    public void setModele(String modele) {
        this.modele = modele;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public double getPrixLocation() {
        return prixLocation;
    }

    public void setPrixLocation(double prixLocation) {
        this.prixLocation = prixLocation;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public Integer getTechnicienId() {
        return technicienId;
    }

    public void setTechnicienId(Integer technicienId) {
        this.technicienId = technicienId;
    }

    public String getTechnicienNom() {
        return technicienNom;
    }

    public void setTechnicienNom(String technicienNom) {
        this.technicienNom = technicienNom;
    }

    public String getTechnicienAffichage() {
        return technicienNom == null || technicienNom.isBlank() ? "Non assigne" : technicienNom;
    }

    @Override
    public String toString() {
        return "vehicule{" +
                "id=" + id +
                ", matricule='" + matricule + '\'' +
                ", marque='" + marque + '\'' +
                ", modele='" + modele + '\'' +
                ", couleur='" + couleur + '\'' +
                ", prixLocation=" + prixLocation +
                ", disponible=" + disponible +
                ", technicien='" + getTechnicienAffichage() + '\'' +
                '}';
    }
}
