package com.esprit.entities;

import java.util.Date;

public class Facture {
    private int idFacture;
    private String numero;
    private Date dateEmission;
    private float montantHT;
    private float montantTTC;
    private float tva;
    private String statut;
    private int livraison_id;
    private String pdfUrl;

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
    public Facture() {}

    public Facture(String numero, float montantHT, float montantTTC, float tva, String statut, int livraison_id) {
        this.numero = numero;
        this.montantHT = montantHT;
        this.montantTTC = montantTTC;
        this.tva = tva;
        this.statut = statut;
        this.livraison_id = livraison_id;
    }

    public Facture(int idFacture, String numero, Date dateEmission, float montantHT, float montantTTC, float tva, String statut, int livraison_id) {
        this.idFacture = idFacture;
        this.numero = numero;
        this.dateEmission = dateEmission;
        this.montantHT = montantHT;
        this.montantTTC = montantTTC;
        this.tva = tva;
        this.statut = statut;
        this.livraison_id = livraison_id;
    }

    // Getters & Setters
    public int getIdFacture() { return idFacture; }
    public void setIdFacture(int idFacture) { this.idFacture = idFacture; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Date getDateEmission() { return dateEmission; }
    public void setDateEmission(Date dateEmission) { this.dateEmission = dateEmission; }
    public float getMontantHT() { return montantHT; }
    public void setMontantHT(float montantHT) { this.montantHT = montantHT; }
    public float getMontantTTC() { return montantTTC; }
    public void setMontantTTC(float montantTTC) { this.montantTTC = montantTTC; }
    public float getTva() { return tva; }
    public void setTva(float tva) { this.tva = tva; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public int getIdLivraison() { return livraison_id; }
    public void setIdLivraison(int idLivraison) { this.livraison_id= idLivraison; }

    @Override
    public String toString() {
        return "Facture{" +
                "idFacture=" + idFacture +
                ", numero='" + numero + '\'' +
                ", dateEmission=" + dateEmission +
                ", montantHT=" + montantHT +
                ", montantTTC=" + montantTTC +
                ", tva=" + tva +
                ", statut='" + statut + '\'' +
                ", livraison_id=" + livraison_id+
                '}';
    }
}