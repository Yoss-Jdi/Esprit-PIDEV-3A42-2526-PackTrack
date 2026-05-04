package com.gestioncolis.models;

import com.gestioncolis.exceptions.StatutInvalideException;
import com.gestioncolis.exceptions.ValidationException;

import java.time.LocalDateTime;
import java.util.List;

public class Colis {
    private int id;
    private String description;
    private String articles;
    private String adresseDepart;
    private String adresseDestination;
    private double poids;
    private String dimensions;
    private String statut;
    private LocalDateTime dateCreation;
    private int expediteurId;
    private int destinataireId;
    private LocalDateTime dateExpedition;

    public Colis(int id, String description, String articles, String adresseDepart, String adresseDestination, double poids,
                 String dimensions, String statut, LocalDateTime dateExpedition, int expediteurId, int destinataireId) {
        this.id = id;
        this.description = description;
        this.articles = articles;
        this.adresseDepart = adresseDepart;
        this.adresseDestination = adresseDestination;
        this.poids = poids;
        this.dimensions = dimensions;
        this.statut = statut;
        this.dateCreation = LocalDateTime.now();
        this.expediteurId = expediteurId;
        this.destinataireId = destinataireId;
        this.dateExpedition = dateExpedition;
    }

    public Colis(String description, String articles, String adresseDepart, String adresseDestination, double poids,
                 String dimensions, int expediteurId, int destinataireId) {

        this(0, description, articles, adresseDepart,
                adresseDestination, poids, dimensions,
                "en_attente",
                null,
                expediteurId, destinataireId);
    }

    public double calculerMontant() {
        return 10.0 + (poids * 2.0);
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String d) {
        this.description = d;
    }
    public String getArticles() {
        return articles;
    }
    public void setArticles(String a) {
        this.articles = a;
    }
    public String getAdresseDepart() {
        return adresseDepart;
    }
    public void setAdresseDepart(String a) {
        this.adresseDepart = a;
    }
    public String getAdresseDestination() {
        return adresseDestination;
    }
    public void setAdresseDestination(String a) {
        this.adresseDestination = a;
    }
    public double getPoids() {
        return poids;
    }
    public void setPoids(double p) {
        this.poids = p;
    }
    public String getDimensions() {
        return dimensions;
    }
    public void setDimensions(String d) {
        this.dimensions = d;
    }
    public String getStatut() {
        return statut;
    }
    public void setStatut(String s) {
        this.statut = s;
    }
    public int getExpediteurId() {
        return expediteurId;
    }
    public void setExpediteurId(int e) {
        this.expediteurId = e;
    }
    public int getDestinataireId() {
        return destinataireId;
    }
    public void setDestinataireId(int d) {
        this.destinataireId = d;
    }
    public LocalDateTime getDateExpedition() {
        return dateExpedition;
    }
    public void setDateExpedition(LocalDateTime d) {
        this.dateExpedition = d;
    }

    @Override
    public String toString() {
        return "┌─ Colis #" + id +
                "\n│  Description    : " + description +
                "\n│  Articles       : " + articles +
                "\n│  Départ         : " + adresseDepart +
                "\n│  Destination    : " + adresseDestination +
                "\n│  Poids          : " + poids + " kg" +
                "\n│  Dimensions     : " + dimensions +
                "\n│  Statut         : " + statut +
                "\n│  Montant        : " + calculerMontant() + " DT" +
                "\n│  Expéditeur ID  : " + expediteurId +
                "\n└  Destinataire ID: " + destinataireId;
    }

    public void valider() throws ValidationException, StatutInvalideException {

        if (adresseDepart == null || adresseDepart.isBlank())
            throw new ValidationException("adresseDepart", "L'adresse de départ est obligatoire");

        if (adresseDestination == null || adresseDestination.isBlank())
            throw new ValidationException("adresseDestination", "L'adresse de destination est obligatoire");

        if (poids <= 0)
            throw new ValidationException("poids", "Le poids doit être positif");

        if (poids >= 1000)
            throw new ValidationException("poids", "Le poids ne peut pas dépasser 1000 kg");

        if (description != null && description.length() < 5)
            throw new ValidationException("description", "Minimum 5 caractères");

        if (description != null && description.length() > 500)
            throw new ValidationException("description", "Maximum 500 caractères");

        if (articles != null && articles.length() > 1000)
            throw new ValidationException("articles", "Maximum 1000 caractères");

        if (dimensions != null && dimensions.length() > 100)
            throw new ValidationException("dimensions", "Maximum 100 caractères");

        List<String> statutsValides = List.of("en_attente", "en_cours", "livre");
        if (statut == null || !statutsValides.contains(statut))
            throw new StatutInvalideException(statut);
    }
}