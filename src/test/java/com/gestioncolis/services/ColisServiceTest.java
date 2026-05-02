package com.gestioncolis.services;

import com.gestioncolis.models.Colis;
import org.junit.jupiter.api.*;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ColisServiceTest {

    static ColisService service;
    static int idColisTest;

    @BeforeAll
    static void initialiser() {
        service = new ColisService();
    }


    @AfterEach
    void nettoyage() throws Exception {
        List<Colis> liste = service.getAll();
        //last colis
        for (int i = liste.size() - 1; i >= 0; i--) {
            Colis c = liste.get(i);
            if (c.getStatut().equals("en_attente") && c.getId() == idColisTest) {
                service.supprimer(c.getId());
                break;
            }
        }
    }

    @Test
    @Order(1)
    void testAjouterColis() throws Exception {
        Colis c = new Colis(
                "Colis test JUnit",
                "Articles test",
                "Tunis",
                "Sfax",
                3.0,
                "15x10x8",
                2,
                3    // destinataire
        );
        service.ajouter(c);

        List<Colis> liste = service.getAll();
        assertFalse(liste.isEmpty());

        boolean trouve = liste.stream()
                .anyMatch(col -> "Colis test JUnit".equals(col.getDescription()));
        assertTrue(trouve);
        System.out.println("***Test Ajout OK***");


        idColisTest = liste.stream()
                .filter(col -> "Colis test JUnit".equals(col.getDescription()))
                .mapToInt(Colis::getId)
                .max()
                .orElse(-1);
    }

    @Test
    @Order(2)
    void testModifierColis() throws Exception {
        Colis c = new Colis(
                "Avant modification",
                "Articles",
                "Tunis",
                "Sousse",
                2.0,
                "10x10x10",
                2,
                3
        );
        service.ajouter(c);

        List<Colis> liste = service.getAll();
        idColisTest = liste.stream()
                .filter(col -> "Avant modification".equals(col.getDescription()))
                .mapToInt(Colis::getId)
                .max()
                .orElse(-1);

        Colis colie_modifie = new Colis(
                idColisTest,
                "Apres modification",
                "Articles modifies",
                "Tunis",
                "Sousse",
                4.0,
                "20x20x20",
                "en_attente",
                null,
                2,
                3
        );
        service.modifier(colie_modifie);

        List<Colis> apres = service.getAll();
        boolean match = apres.stream()
                .anyMatch(col -> "Apres modification".equals(col.getDescription()));
        assertTrue(match);
        System.out.println("***Test modifier OK***");
    }


    @Test
    @Order(3)
    void testSupprimerColis() throws Exception {
        Colis c = new Colis(
                "Colis a supprimer",
                "Articles",
                "Tunis",
                "Nabeul",
                1.5,
                "5x5x5",
                2,
                3
        );
        service.ajouter(c);

        List<Colis> liste = service.getAll();
        idColisTest = liste.stream()
                .filter(col -> "Colis a supprimer".equals(col.getDescription()))
                .mapToInt(Colis::getId)
                .max()
                .orElse(-1);


        service.supprimer(idColisTest);

        List<Colis> apres = service.getAll();
        boolean existe = apres.stream()
                .anyMatch(col -> col.getId() == idColisTest);
        assertFalse(existe);
        System.out.println("***Test Supprimer OK***");

        idColisTest = -1;
    }
}