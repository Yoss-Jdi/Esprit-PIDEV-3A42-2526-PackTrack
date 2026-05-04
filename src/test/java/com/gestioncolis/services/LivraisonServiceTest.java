package com.gestioncolis.services;

import com.gestioncolis.models.Livraison;
import com.gestioncolis.utils.MyConnection;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LivraisonServiceTest {

    static LivraisonService service;
    static int idLivraisonTest = -1;

    static final int COLIS_ID   = 13;
    static final int LIVREUR_ID = 4;

    @BeforeAll
    static void initialiser() {
        service = new LivraisonService();
    }

    @AfterEach
    void nettoyage() throws Exception {
        if (idLivraisonTest != -1) {
            service.supprimer(idLivraisonTest);
            idLivraisonTest = -1;
        }
        remettreColisEnAttente(COLIS_ID);
    }
    private void remettreColisEnAttente(int colisId) throws Exception {
        Connection cnx = MyConnection.getInstance().getConnection();
        PreparedStatement ps = cnx.prepareStatement(
                "UPDATE colis SET statut = 'en_attente', dateExpedition = NULL WHERE ID_Colis = ?");
        ps.setInt(1, colisId);
        ps.executeUpdate();
    }

    @Test
    @Order(1)
    void testAjouterLivraison() throws Exception {
        Livraison liv = new Livraison(COLIS_ID, LIVREUR_ID, 0);
        service.ajouter(liv);

        List<Livraison> liste = service.getAll();
        assertFalse(liste.isEmpty());

        boolean trouve = liste.stream()
                .anyMatch(l -> l.getColisId() == COLIS_ID
                        && l.getLivreurId() == LIVREUR_ID);
        assertTrue(trouve);
        System.out.println("***Test Ajout OK ****");

        idLivraisonTest = liste.stream()
                .filter(l -> l.getColisId() == COLIS_ID && l.getLivreurId() == LIVREUR_ID)
                .mapToInt(Livraison::getId)
                .max()
                .orElse(-1);
    }

    @Test
    @Order(2)
    void testTerminerLivraison() throws Exception {
        Livraison liv = new Livraison(COLIS_ID, LIVREUR_ID, 0);
        service.ajouter(liv);

        List<Livraison> liste = service.getAll();
        idLivraisonTest = liste.stream()
                .filter(l -> l.getColisId() == COLIS_ID && l.getLivreurId() == LIVREUR_ID)
                .mapToInt(Livraison::getId)
                .max()
                .orElse(-1);

        service.terminer(idLivraisonTest);

        //statut: termineee
        List<Livraison> apres = service.getAll();
        boolean termine = apres.stream()
                .anyMatch(l -> l.getId() == idLivraisonTest
                        && "termine".equals(l.getStatut()));
        assertTrue(termine);
        System.out.println("***Test terminaison liv OK****");
    }




    @Test
    @Order(3)
    void testSupprimerLivraison() throws Exception {
        Livraison liv = new Livraison(COLIS_ID, LIVREUR_ID, 0);
        service.ajouter(liv);

        List<Livraison> liste = service.getAll();
        idLivraisonTest = liste.stream()
                .filter(l -> l.getColisId() == COLIS_ID && l.getLivreurId() == LIVREUR_ID)
                .mapToInt(Livraison::getId)
                .max()
                .orElse(-1);

        service.supprimer(idLivraisonTest);

        List<Livraison> apres = service.getAll();
        boolean existe = apres.stream()
                .anyMatch(l -> l.getId() == idLivraisonTest);
        assertFalse(existe);
        System.out.println("***Test Supprimer OK***");

        idLivraisonTest = -1;
    }
}