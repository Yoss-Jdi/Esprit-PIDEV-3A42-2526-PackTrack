import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.enums.Role;
import org.junit.jupiter.api.*;
import com.gestioncolis.services.UtilisateursServices;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour UtilisateursServices
 * Emplacement : src/test/java/com/esprit/services/UtilisateursServicesTest.java
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class
UtilisateursServicesTest {

    static UtilisateursServices service;
    static int idUtilisateurTest = -1;

    // Email unique par run pour éviter les collisions sur la contrainte UNIQUE
    static final String TEST_EMAIL = "testjunit_" + System.currentTimeMillis() + "@esprit.tn";

    // ─────────────────────────────────────────────────────────────────────────
    // INITIALISATION — une seule fois avant tous les tests
    // ─────────────────────────────────────────────────────────────────────────
    @BeforeAll
    static void setup() throws SQLException {
        service = new UtilisateursServices();

        // ── NETTOYAGE PRÉVENTIF ───────────────────────────────────────────────
        // Supprime toute entrée "testjunit@esprit.tn" laissée par un run précédent
        // qui aurait planté avant le tearDown (ancienne adresse fixe).
        List<Utilisateurs> existants = service.afficher();
        for (Utilisateurs u : existants) {
            if (u.getEmail() != null && u.getEmail().startsWith("testjunit")) {
                service.supprimer(u.getIdUtilisateur());
                System.out.println("🧹 [Setup] Orphelin supprimé : " + u.getEmail()
                        + " (id=" + u.getIdUtilisateur() + ")");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 1 : Ajouter un utilisateur
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @Order(1)
    void testAjouterUtilisateur() throws SQLException {
        Utilisateurs u = new Utilisateurs(
                TEST_EMAIL,          // email unique par run (timestamp)
                "motdepasse123",
                "TestNom",
                "TestPrenom",
                "99000000",
                Role.CLIENT,
                null,
                LocalDateTime.now()
        );

        service.ajouter(u);

        List<Utilisateurs> liste = service.afficher();

        assertFalse(liste.isEmpty(), "La liste ne doit pas être vide après ajout");

        boolean trouve = liste.stream()
                .anyMatch(x -> x.getEmail().equals(TEST_EMAIL));
        assertTrue(trouve, "L'utilisateur ajouté doit être présent dans la liste");

        // Mémoriser l'id pour les tests suivants
        idUtilisateurTest = liste.stream()
                .filter(x -> x.getEmail().equals(TEST_EMAIL))
                .mapToInt(Utilisateurs::getIdUtilisateur)
                .max()
                .orElse(-1);

        assertNotEquals(-1, idUtilisateurTest, "L'id récupéré doit être valide");
        System.out.println("✅ [Test 1] idUtilisateurTest = " + idUtilisateurTest
                + "  email=" + TEST_EMAIL);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 2 : Afficher tous les utilisateurs
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @Order(2)
    void testAfficherUtilisateurs() throws SQLException {
        List<Utilisateurs> liste = service.afficher();

        assertNotNull(liste, "La liste retournée ne doit pas être null");
        assertFalse(liste.isEmpty(), "La liste doit contenir au moins un utilisateur");

        System.out.println("✅ [Test 2] Nombre d'utilisateurs : " + liste.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 3 : Modifier un utilisateur
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @Order(3)
    void testModifierUtilisateur() throws SQLException {
        assertNotEquals(-1, idUtilisateurTest, "L'id doit avoir été défini par le test 1");

        Utilisateurs u = new Utilisateurs();
        u.setIdUtilisateur(idUtilisateurTest);
        u.setEmail(TEST_EMAIL);
        u.setMotDePasse("motdepasse123");
        u.setNom("NomModifie");
        u.setPrenom("PrenomModifie");
        u.setTelephone("99111111");
        u.setRole(Role.TECHNICIEN);
        u.setPhoto(null);

        service.modifier(u);

        List<Utilisateurs> liste = service.afficher();
        boolean modifie = liste.stream()
                .anyMatch(x -> x.getIdUtilisateur() == idUtilisateurTest
                        && x.getNom().equals("NomModifie")
                        && x.getRole() == Role.TECHNICIEN);

        assertTrue(modifie, "Les modifications doivent être présentes en base");
        System.out.println("✅ [Test 3] Modification vérifiée pour id=" + idUtilisateurTest);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 4 : Supprimer un utilisateur
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @Order(4)
    void testSupprimerUtilisateur() throws SQLException {
        assertNotEquals(-1, idUtilisateurTest, "L'id doit avoir été défini par le test 1");

        service.supprimer(idUtilisateurTest);

        List<Utilisateurs> liste = service.afficher();
        boolean existe = liste.stream()
                .anyMatch(x -> x.getIdUtilisateur() == idUtilisateurTest);

        assertFalse(existe, "L'utilisateur supprimé ne doit plus exister en base");
        System.out.println("✅ [Test 4] Suppression vérifiée pour id=" + idUtilisateurTest);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NETTOYAGE — après chaque test (filet de sécurité silencieux)
    // ─────────────────────────────────────────────────────────────────────────
    @AfterEach
    void cleanUp() {
        // Pas d'action entre les tests : on laisse les données jusqu'au test 4.
        // Ce bloc existe pour satisfaire la bonne pratique du workshop.
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NETTOYAGE FINAL — supprime les données de test si elles existent encore
    // (cas d'échec du test 4)
    // ─────────────────────────────────────────────────────────────────────────
    @AfterAll
    static void tearDown() {
        if (idUtilisateurTest == -1) return;
        try {
            List<Utilisateurs> liste = service.afficher();
            boolean encorePresent = liste.stream()
                    .anyMatch(x -> x.getIdUtilisateur() == idUtilisateurTest);
            if (encorePresent) {
                service.supprimer(idUtilisateurTest);
                System.out.println("🧹 [TearDown] Données de test supprimées (id=" + idUtilisateurTest + ")");
            }
        } catch (SQLException e) {
            System.err.println("⚠️  tearDown() : " + e.getMessage());
        }
    }
}