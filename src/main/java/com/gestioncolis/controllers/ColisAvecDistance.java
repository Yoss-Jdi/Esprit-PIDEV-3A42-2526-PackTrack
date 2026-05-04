package com.gestioncolis.controllers;

import com.gestioncolis.models.Colis;

/**
 * Wrapper pour associer un colis à sa distance par rapport au livreur.
 */
public class ColisAvecDistance implements Comparable<ColisAvecDistance> {
    private final Colis colis;
    private final Double distanceLivreurKm; // null si non calculable

    public ColisAvecDistance(Colis colis, Double distanceLivreurKm) {
        this.colis = colis;
        this.distanceLivreurKm = distanceLivreurKm;
    }

    public Colis getColis() {
        return colis;
    }

    public Double getDistanceLivreurKm() {
        return distanceLivreurKm;
    }

    public boolean hasDistance() {
        return distanceLivreurKm != null && distanceLivreurKm >= 0;
    }

    public String getDistanceFormatee() {
        if (!hasDistance()) return "?";
        if (distanceLivreurKm < 1) return String.format("%d m", (int)(distanceLivreurKm * 1000));
        return String.format("%.1f km", distanceLivreurKm);
    }

    @Override
    public int compareTo(ColisAvecDistance autre) {
        // Les colis avec distance calculée en premier
        if (this.hasDistance() && !autre.hasDistance()) return -1;
        if (!this.hasDistance() && autre.hasDistance()) return 1;
        if (!this.hasDistance() && !autre.hasDistance()) return 0;
        // Tri par distance croissante
        return this.distanceLivreurKm.compareTo(autre.distanceLivreurKm);
    }
}