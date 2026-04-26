package com.gestioncolis;

import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.enums.Role;
import com.gestioncolis.services.UtilisateursServices;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        UtilisateursServices service = new UtilisateursServices();

        System.out.println("\n AJOUTER ");

        Utilisateurs testjava1 = new Utilisateurs(
                "testjava1@esprit.tn",
                "mdp123",
                "Test",
                "Java1",
                "99123456",
                Role.CLIENT,
                null,
                LocalDateTime.now()
        );

        Utilisateurs testjava2 = new Utilisateurs(
                "testjava2@esprit.tn",
                "mdp123",
                "Test",
                "Java2",
                "99123456",
                Role.LIVREUR,
                null,
                LocalDateTime.now()
        );

        try {
            service.ajouter(testjava1);
            service.ajouter(testjava2);
        } catch (SQLException e) {
            System.err.println("Erreur ajout : " + e.getMessage());
        }

        System.out.println("\n AFFICHER TOUS ");
        List<Utilisateurs> liste = null;
        try {
            liste = service.afficher();
            liste.forEach(System.out::println);
        } catch (SQLException e) {
            System.err.println("Erreur affichage : " + e.getMessage());
        }

        if (liste == null || liste.size() < 2) {
            System.err.println("Impossible de continuer : pas assez d'utilisateurs en BDD.");
            return;
        }

        Utilisateurs u1 = liste.get(liste.size() - 2); // testjava1
        Utilisateurs u2 = liste.get(liste.size() - 1); // testjava2
        System.out.println("\nID testjava1 = " + u1.getIdUtilisateur());
        System.out.println("ID testjava2 = " + u2.getIdUtilisateur());

        System.out.println("\n MODIFIER testjava1 (id=" + u1.getIdUtilisateur() + ") ");
        try {
            u1.setNom("NomModifie");
            u1.setPrenom("PrenomModifie");
            u1.setTelephone("99999999");
            u1.setRole(Role.TECHNICIEN);
            service.modifier(u1);
        } catch (SQLException e) {
            System.err.println("Erreur modification : " + e.getMessage());
        }

        System.out.println("\n SUPPRIMER testjava2 (id=" + u2.getIdUtilisateur() + ") ");
        try {
            service.supprimer(u2.getIdUtilisateur());
        } catch (SQLException e) {
            System.err.println("Erreur suppression : " + e.getMessage());
        }

        System.out.println("\n AFFICHER APRES SUPPRESSION ");
        try {
            List<Utilisateurs> listefinale = service.afficher();
            if (listefinale.isEmpty())
                System.out.println("(aucun utilisateur en base)");
            else
                listefinale.forEach(System.out::println);
        } catch (SQLException e) {
            System.err.println("Erreur affichage final : " + e.getMessage());
        }
    }
}