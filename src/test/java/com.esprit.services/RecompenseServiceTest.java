package com.esprit.services;

import com.esprit.entities.Recompense;
import org.junit.jupiter.api.*;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RecompenseServiceTest {

    static RecompenseService service;
    static int idTest;

    @BeforeAll
    static void setup() {
        service = new RecompenseService();
    }

    @Test
    @Order(1)
    void testAjouter() {
        Recompense r = new Recompense(
                "bonus", 50.0, "Test description",
                100, new Date(), 5, null
        );
        assertDoesNotThrow(() -> service.ajouter(r));
        System.out.println("✅ testAjouter OK");
    }

    @Test
    @Order(2)
    void testAfficherTous() {
        List<Recompense> list = service.afficherTous();
        assertNotNull(list);
        assertFalse(list.isEmpty());
        // Sauvegarder l'id pour les tests suivants
        idTest = list.get(list.size() - 1).getIdRecompense();
        System.out.println("✅ testAfficherTous OK — id=" + idTest);
    }

    @Test
    @Order(3)
    void testRechercherParId() {
        // Récupérer d'abord le dernier id
        List<Recompense> list = service.afficherTous();
        assertFalse(list.isEmpty());
        int id = list.get(list.size() - 1).getIdRecompense();

        Recompense r = service.rechercherParId(id);
        assertNotNull(r);

        // Vérifier que l'id correspond — pas le type
        assertEquals(id, r.getIdRecompense());
        System.out.println("✅ testRechercherParId OK — type=" + r.getType());
    }

    @Test
    @Order(4)
    void testModifier() {
        List<Recompense> list = service.afficherTous();
        assertFalse(list.isEmpty());
        Recompense r = list.get(list.size() - 1);

        // Modifier
        r.setType("prime");
        r.setValeur(75.0);
        assertDoesNotThrow(() -> service.modifier(r));

        // Vérifier modification
        Recompense modifie = service.rechercherParId(r.getIdRecompense());
        assertNotNull(modifie);
        assertEquals("prime", modifie.getType());
        assertEquals(75.0, modifie.getValeur());
        System.out.println("✅ testModifier OK");
    }

    @Test
    @Order(5)
    void testSupprimer() {
        List<Recompense> list = service.afficherTous();
        assertFalse(list.isEmpty());
        int id = list.get(list.size() - 1).getIdRecompense();

        assertDoesNotThrow(() -> service.supprimer(id));

        // Vérifier suppression
        assertNull(service.rechercherParId(id));
        System.out.println("✅ testSupprimer OK");
    }
}