package com.example.rayen.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class technicien {
    private int id;
    private String nom;
    private String prenom;
    private String specialite;
    private String telephone;
    private String email;
    private List<vehicule> vehicules = new ArrayList<>();

    public technicien() {
    }

    public technicien(String nom, String prenom, String specialite, String telephone, String email, List<vehicule> vehicules) {
        this.nom = nom;
        this.prenom = prenom;
        this.specialite = specialite;
        this.telephone = telephone;
        this.email = email;
        setVehicules(vehicules);
    }

    public technicien(int id, String nom, String prenom, String specialite, String telephone, String email, List<vehicule> vehicules) {
        this(nom, prenom, specialite, telephone, email, vehicules);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<vehicule> getVehicules() {
        return vehicules;
    }

    public void setVehicules(List<vehicule> vehicules) {
        this.vehicules = vehicules == null ? new ArrayList<>() : new ArrayList<>(vehicules);
    }

    public String getNomComplet() {
        String prenomNormalise = prenom == null ? "" : prenom.trim();
        String nomNormalise = nom == null ? "" : nom.trim();
        return (prenomNormalise + " " + nomNormalise).trim();
    }

    public int getNombreVehicules() {
        return vehicules.size();
    }

    public String getVehiculesResume() {
        if (vehicules.isEmpty()) {
            return "Aucun vehicule";
        }

        return vehicules.stream()
                .map(vehicule::getMatricule)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return getNomComplet().isBlank() ? "Technicien" : getNomComplet();
    }
}
