package com.esprit;

import com.esprit.services.FactureService;
import com.esprit.services.RecompenseService;
import com.esprit.entities.Facture;
import com.esprit.entities.Recompense;

import java.util.List;
import java.util.Scanner;
import java.util.Date;

public class Main {

    public static void main(String[] args) {

        FactureService factureService = new FactureService();
        RecompenseService recompenseService = new RecompenseService();
        Scanner scanner = new Scanner(System.in);
        int choix;

        do {
            System.out.println("\n========== GESTION FACTURES & RECOMPENSES ==========");
            System.out.println("1. Ajouter une facture");
            System.out.println("2. Afficher toutes les factures");
            System.out.println("3. Modifier une facture");
            System.out.println("4. Supprimer une facture");
            System.out.println("5. Rechercher une facture par ID");
            System.out.println("6. Ajouter une recompense");
            System.out.println("7. Afficher toutes les recompenses");
            System.out.println("8. Modifier une recompense");
            System.out.println("9. Supprimer une recompense");
            System.out.println("10. Rechercher une recompense par ID");
            System.out.println("0. Quitter");
            System.out.print("Votre choix : ");
            choix = scanner.nextInt();

            switch (choix) {

                // ----------------- FACTURES -----------------
                case 1:
                    System.out.print("Numéro : "); String num = scanner.next();
                    System.out.print("MontantHT : "); float ht = scanner.nextFloat();
                    System.out.print("MontantTTC : "); float ttc = scanner.nextFloat();
                    System.out.print("TVA : "); float tva = scanner.nextFloat();
                    System.out.print("Statut : "); String statut = scanner.next();
                    System.out.print("ID Livraison : "); int idLiv = scanner.nextInt();
                    factureService.ajouter(new Facture(num, ht, ttc, tva, statut, idLiv));
                    break;

                case 2:
                    List<Facture> factures = factureService.afficherTous();
                    System.out.println("-------- Liste des Factures --------");
                    if (factures.isEmpty()) System.out.println("Aucune facture trouvée.");
                    else factures.forEach(System.out::println);
                    break;

                case 3:
                    System.out.print("ID Facture à modifier : "); int idMod = scanner.nextInt();
                    System.out.print("Nouveau numéro : "); String newNum = scanner.next();
                    System.out.print("Nouveau MontantHT : "); float newHT = scanner.nextFloat();
                    System.out.print("Nouveau MontantTTC : "); float newTTC = scanner.nextFloat();
                    System.out.print("Nouvelle TVA : "); float newTVA = scanner.nextFloat();
                    System.out.print("Nouveau Statut : "); String newStatut = scanner.next();
                    System.out.print("Nouveau ID Livraison : "); int newIdLiv = scanner.nextInt();
                    factureService.modifier(new Facture(idMod, newNum, null, newHT, newTTC, newTVA, newStatut, newIdLiv));
                    break;

                case 4:
                    System.out.print("ID Facture à supprimer : "); int idSup = scanner.nextInt();
                    factureService.supprimer(idSup);
                    break;

                case 5:
                    System.out.print("ID Facture à rechercher : "); int idRech = scanner.nextInt();
                    Facture f = factureService.rechercherParId(idRech);
                    System.out.println(f != null ? f : "Facture non trouvée.");
                    break;

                // ----------------- RECOMPENSES -----------------
                case 6:
                    System.out.print("Type : "); String type = scanner.next();
                    System.out.print("Valeur : "); double val = scanner.nextDouble();
                    System.out.print("Description : "); scanner.nextLine(); // consommer le retour chariot
                    String desc = scanner.nextLine();
                    System.out.print("Seuil : "); int seuil = scanner.nextInt();
                    System.out.print("ID Livreur : "); int idLivreur = scanner.nextInt();
                    System.out.print("ID Facture (0 si aucune) : "); int idFact = scanner.nextInt();
                    recompenseService.ajouter(new Recompense(type, val, desc, seuil, new Date(), idLivreur, idFact == 0 ? null : idFact));
                    break;

                case 7:
                    List<Recompense> recompenses = recompenseService.afficherTous();
                    System.out.println("-------- Liste des Recompenses --------");
                    if (recompenses.isEmpty()) System.out.println("Aucune recompense trouvée.");
                    else recompenses.forEach(System.out::println);
                    break;

                case 8:
                    System.out.print("ID Recompense à modifier : "); int idRMod = scanner.nextInt();
                    System.out.print("Nouveau type : "); String newType = scanner.next();
                    System.out.print("Nouvelle valeur : "); double newVal = scanner.nextDouble();
                    System.out.print("Nouvelle description : "); scanner.nextLine();
                    String newDesc = scanner.nextLine();
                    System.out.print("Nouveau seuil : "); int newSeuil = scanner.nextInt();
                    System.out.print("Nouveau ID Livreur : "); int newIdL = scanner.nextInt();
                    System.out.print("Nouveau ID Facture (0 si aucune) : "); int newIdF = scanner.nextInt();
                    recompenseService.modifier(new Recompense(idRMod, newType, newVal, newDesc, newSeuil, new Date(), newIdL, newIdF==0 ? null : newIdF));
                    break;

                case 9:
                    System.out.print("ID Recompense à supprimer : "); int idRSup = scanner.nextInt();
                    recompenseService.supprimer(idRSup);
                    break;

                case 10:
                    System.out.print("ID Recompense à rechercher : "); int idRRech = scanner.nextInt();
                    Recompense r = recompenseService.rechercherParId(idRRech);
                    System.out.println(r != null ? r : "Recompense non trouvée.");
                    break;

                case 0:
                    System.out.println("Au revoir !");
                    break;

                default:
                    System.out.println("Choix invalide !");
            }

        } while (choix != 0);
    }
}