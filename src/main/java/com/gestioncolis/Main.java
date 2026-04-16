package com.gestioncolis;

import com.gestioncolis.exceptions.*;
import com.gestioncolis.models.Colis;
import com.gestioncolis.models.Livraison;
import com.gestioncolis.services.ColisService;
import com.gestioncolis.services.LivraisonService;

import java.sql.SQLException;
import java.util.List;

public class Main {

    static ColisService colisService         = new ColisService();
    static LivraisonService livraisonService = new LivraisonService();

    public static void main(String[] args) throws SQLException {

        testerAjoutColisValide();
        testerAjoutColisInvalide();
        testerListerColis();
        testerModifierColis();
        testerColisDisponibles();
        testerPrendreEnCharge();
        testerListerLivraisons();
        testerTerminerLivraison();
        testerSupprimerColis();
    }

    // ── 1. Ajout colis valide ────────────────────────────────────────
    static void testerAjoutColisValide() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║       TEST : AJOUTER COLIS VALIDE    ║");
        System.out.println("╚══════════════════════════════════════╝");
        try {
            Colis c = new Colis(
                    "Laptop fragile", "1 ordinateur",
                    "Tunis Centre", "Sfax Ville",
                    2.5, "40x30x5 cm",
                    1, 2  // expediteurId=1, destinataireId=2
            );
            colisService.ajouter(c);
        } catch (Exception e) {
            System.err.println("ERREUR INATTENDUE : " + e.getMessage());
        }
    }

    // ── 2. Ajout colis invalide → doit déclencher des exceptions ────
    static void testerAjoutColisInvalide() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║     TEST : AJOUTER COLIS INVALIDE    ║");
        System.out.println("╚══════════════════════════════════════╝");

        // Cas 1 : poids négatif
        System.out.println("\n--- Cas 1 : poids négatif ---");
        try {
            Colis c = new Colis(
                    "Test", "articles",
                    "Tunis", "Sfax",
                    -5.0, null,
                    1, 2
            );
            colisService.ajouter(c);
        } catch (ValidationException e) {
            System.out.println("✅ ValidationException attrapée !");
            System.out.println("   Champ   : " + e.getChamp());
            System.out.println("   Message : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Mauvaise exception : " + e.getMessage());
        }

        // Cas 2 : adresse de départ vide
        System.out.println("\n--- Cas 2 : adresse départ vide ---");
        try {
            Colis c = new Colis(
                    "Test", "articles",
                    "",  // ← vide
                    "Sfax",
                    3.0, null,
                    1, 2
            );
            colisService.ajouter(c);
        } catch (ValidationException e) {
            System.out.println("✅ ValidationException attrapée !");
            System.out.println("   Champ   : " + e.getChamp());
            System.out.println("   Message : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Mauvaise exception : " + e.getMessage());
        }

        // Cas 3 : description trop courte
        System.out.println("\n--- Cas 3 : description trop courte ---");
        try {
            Colis c = new Colis(
                    "AB",  // ← moins de 5 caractères
                    "articles",
                    "Tunis", "Sfax",
                    3.0, null,
                    1, 2
            );
            colisService.ajouter(c);
        } catch (ValidationException e) {
            System.out.println("✅ ValidationException attrapée !");
            System.out.println("   Champ   : " + e.getChamp());
            System.out.println("   Message : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Mauvaise exception : " + e.getMessage());
        }
    }

    // ── 3. Lister tous les colis ─────────────────────────────────────
    static void testerListerColis() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║         TEST : LISTER COLIS          ║");
        System.out.println("╚══════════════════════════════════════╝");
        try {
            List<Colis> liste = colisService.getAll();
            System.out.println("Total colis en base : " + liste.size());
            liste.forEach(System.out::println);
        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
    }

    // ── 4. Modifier un colis ─────────────────────────────────────────
    static void testerModifierColis() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║         TEST : MODIFIER COLIS        ║");
        System.out.println("╚══════════════════════════════════════╝");
        try {
            List<Colis> liste = colisService.getAll();
            if (liste.isEmpty()) {
                System.out.println("Aucun colis à modifier.");
                return;
            }
            Colis dernier = liste.get(liste.size() - 1);
            System.out.println("Avant : poids = " + dernier.getPoids());
            dernier.setDescription("Laptop très fragile");
            dernier.setPoids(3.0);
            colisService.modifier(dernier);
            System.out.println("Après : poids = " + dernier.getPoids());
        } catch (ValidationException e) {
            System.out.println("✅ ValidationException : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    // ── 5. Colis disponibles ─────────────────────────────────────────
    static void testerColisDisponibles() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║      TEST : COLIS DISPONIBLES        ║");
        System.out.println("╚══════════════════════════════════════╝");
        try {
            List<Colis> dispos = colisService.getColisDisponibles();
            System.out.println("Colis disponibles : " + dispos.size());
            dispos.forEach(System.out::println);
        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
    }

    // ── 6. Prendre en charge un colis ────────────────────────────────
    static void testerPrendreEnCharge() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║      TEST : PRISE EN CHARGE          ║");
        System.out.println("╚══════════════════════════════════════╝");
        try {
            List<Colis> dispos = colisService.getColisDisponibles();
            if (dispos.isEmpty()) {
                System.out.println("Aucun colis disponible.");
                return;
            }
            int colisId = dispos.get(0).getId();
            Livraison liv = new Livraison(colisId, 3, 0); // livreurId=3
            livraisonService.ajouter(liv);

        } catch (ColisIndisponibleException e) {
            System.out.println("✅ ColisIndisponibleException : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }

        // Cas erreur : prendre le même colis une 2ème fois
        System.out.println("\n--- Cas : reprendre le même colis (doit échouer) ---");
        try {
            List<Colis> dispos = colisService.getColisDisponibles();
            // On essaie un ID qui n'est plus disponible
            Livraison liv = new Livraison(9999, 3, 0);
            livraisonService.ajouter(liv);
        } catch (ColisIndisponibleException e) {
            System.out.println("✅ ColisIndisponibleException attrapée !");
            System.out.println("   Message : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    // ── 7. Lister les livraisons ─────────────────────────────────────
    static void testerListerLivraisons() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║       TEST : LISTER LIVRAISONS       ║");
        System.out.println("╚══════════════════════════════════════╝");
        try {
            List<Livraison> livraisons = livraisonService.getAll();
            System.out.println("Total livraisons : " + livraisons.size());
            livraisons.forEach(System.out::println);
        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
    }

    // ── 8. Terminer une livraison ────────────────────────────────────
    static void testerTerminerLivraison() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║      TEST : TERMINER LIVRAISON       ║");
        System.out.println("╚══════════════════════════════════════╝");
        try {
            List<Livraison> livraisons = livraisonService.getAll();
            if (livraisons.isEmpty()) {
                System.out.println("Aucune livraison à terminer.");
                return;
            }
            // Prendre la dernière en_cours
            Livraison enCours = livraisons.stream()
                    .filter(l -> "en_cours".equals(l.getStatut()))
                    .findFirst()
                    .orElse(null);

            if (enCours == null) {
                System.out.println("Aucune livraison en cours.");
                return;
            }
            livraisonService.terminer(enCours.getId());

        } catch (LivraisonIntrouvableException e) {
            System.out.println("✅ LivraisonIntrouvableException : " + e.getMessage());
        } catch (StatutInvalideException e) {
            System.out.println("✅ StatutInvalideException : " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }

        // Cas erreur : terminer une livraison inexistante
        System.out.println("\n--- Cas : terminer livraison inexistante ---");
        try {
            livraisonService.terminer(99999);
        } catch (LivraisonIntrouvableException e) {
            System.out.println("✅ LivraisonIntrouvableException attrapée !");
            System.out.println("   Message : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    // ── 9. Supprimer un colis ────────────────────────────────────────
    static void testerSupprimerColis() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║        TEST : SUPPRIMER COLIS        ║");
        System.out.println("╚══════════════════════════════════════╝");
        try {
            // Cas valide : supprimer un colis en_attente
            List<Colis> enAttente = colisService.getAll().stream()
                    .filter(c -> "en_attente".equals(c.getStatut()))
                    .toList();

            if (!enAttente.isEmpty()) {
                int idASupprimer = enAttente.get(0).getId();
                System.out.println("Suppression du colis #" + idASupprimer);
                colisService.supprimer(idASupprimer);
            } else {
                System.out.println("Aucun colis en_attente à supprimer.");
            }

        } catch (StatutInvalideException e) {
            System.out.println("✅ StatutInvalideException : " + e.getMessage());
        } catch (ColisIndisponibleException e) {
            System.out.println("✅ ColisIndisponibleException : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }

        // Cas erreur : supprimer un colis inexistant
        System.out.println("\n--- Cas : supprimer colis inexistant ---");
        try {
            colisService.supprimer(99999);
        } catch (ColisIndisponibleException e) {
            System.out.println("✅ ColisIndisponibleException attrapée !");
            System.out.println("   Message : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }
}