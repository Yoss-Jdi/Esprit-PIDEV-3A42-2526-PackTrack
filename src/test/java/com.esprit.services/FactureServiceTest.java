package com.esprit.services;

// ===== Imports JUnit 5 =====
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

// ===== Imports JUnit Assertions =====
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// ===== Imports Mockito =====
import org.mockito.MockedStatic;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ===== Imports projet =====
import com.esprit.entities.Facture;
import com.esprit.utils.MyDataBase;

// ===== Imports Java =====
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FactureServiceTest {

    private FactureService factureService;
    private Connection mockConnection;
    private PreparedStatement mockPs;
    private Statement mockSt;
    private ResultSet mockRs;
    private MockedStatic<MyDataBase> mockedStatic;
    private MyDataBase mockDb;

    @BeforeEach
    void setUp() throws SQLException {
        mockConnection = mock(Connection.class);
        mockPs         = mock(PreparedStatement.class);
        mockSt         = mock(Statement.class);
        mockRs         = mock(ResultSet.class);
        mockDb         = mock(MyDataBase.class);

        mockedStatic = mockStatic(MyDataBase.class);
        mockedStatic.when(MyDataBase::getInstance).thenReturn(mockDb);
        when(mockDb.getConnection()).thenReturn(mockConnection);

        factureService = new FactureService();
    }

    @AfterEach
    void tearDown() {
        mockedStatic.close();
    }

    // ============================================================
    // Tests : ajouter()
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("ajouter - insere une facture avec succes")
    void testAjouterFactureSuccess() throws SQLException {
        Facture facture = new Facture("FAC-001", 100.0f, 120.0f, 20.0f, "payee", 1);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPs);
        when(mockPs.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> factureService.ajouter(facture));

        verify(mockPs).setString(1, "FAC-001");
        verify(mockPs).setFloat(2, 100.0f);
        verify(mockPs).setFloat(3, 120.0f);
        verify(mockPs).setFloat(4, 20.0f);
        verify(mockPs).setString(5, "payee");
        verify(mockPs).setInt(6, 1);
        verify(mockPs).executeUpdate();
    }

    @Test
    @Order(2)
    @DisplayName("ajouter - gere erreur SQL sans exception")
    void testAjouterFactureSQLException() throws SQLException {
        Facture facture = new Facture("FAC-ERR", 0f, 0f, 0f, "test", 0);
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Erreur DB simulee"));

        assertDoesNotThrow(() -> factureService.ajouter(facture));
    }

    // ============================================================
    // Tests : afficherTous()
    // ============================================================

    @Test
    @Order(3)
    @DisplayName("afficherTous - retourne liste non vide")
    void testAfficherTousRetourneListeNonVide() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockSt);
        when(mockSt.executeQuery(anyString())).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true, true, false);

        when(mockRs.getInt("ID_Facture")).thenReturn(1, 2);
        when(mockRs.getString("numero")).thenReturn("FAC-001", "FAC-002");
        when(mockRs.getDate("dateEmission")).thenReturn(new java.sql.Date(System.currentTimeMillis()));
        when(mockRs.getFloat("montantHT")).thenReturn(100.0f, 200.0f);
        when(mockRs.getFloat("montantTTC")).thenReturn(120.0f, 240.0f);
        when(mockRs.getFloat("tva")).thenReturn(20.0f);
        when(mockRs.getString("statut")).thenReturn("payee", "en attente");
        when(mockRs.getInt("livraison_id")).thenReturn(1, 2);

        List<Facture> result = factureService.afficherTous();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("FAC-001", result.get(0).getNumero());
        assertEquals("FAC-002", result.get(1).getNumero());
    }

    @Test
    @Order(4)
    @DisplayName("afficherTous - retourne liste vide si aucune facture")
    void testAfficherTousRetourneListeVide() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockSt);
        when(mockSt.executeQuery(anyString())).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(false);

        List<Facture> result = factureService.afficherTous();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ============================================================
    // Tests : modifier()
    // ============================================================

    @Test
    @Order(5)
    @DisplayName("modifier - met a jour une facture correctement")
    void testModifierFactureSuccess() throws SQLException {
        Facture facture = new Facture(1, "FAC-MOD", null, 150.0f, 180.0f, 20.0f, "modifiee", 2);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPs);
        when(mockPs.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> factureService.modifier(facture));

        verify(mockPs).setString(1, "FAC-MOD");
        verify(mockPs).setFloat(2, 150.0f);
        verify(mockPs).setFloat(3, 180.0f);
        verify(mockPs).setFloat(4, 20.0f);
        verify(mockPs).setString(5, "modifiee");
        verify(mockPs).setInt(6, 2);
        verify(mockPs).setInt(7, 1);
    }

    // ============================================================
    // Tests : supprimer()
    // ============================================================

    @Test
    @Order(6)
    @DisplayName("supprimer - supprime une facture par son ID")
    void testSupprimerFactureSuccess() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPs);
        when(mockPs.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> factureService.supprimer(42));

        verify(mockPs).setInt(1, 42);
        verify(mockPs).executeUpdate();
    }

    @Test
    @Order(7)
    @DisplayName("supprimer - aucune exception si ID inexistant")
    void testSupprimerFactureIdInexistant() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPs);
        when(mockPs.executeUpdate()).thenReturn(0);

        assertDoesNotThrow(() -> factureService.supprimer(9999));
    }

    // ============================================================
    // Tests : rechercherParId()
    // ============================================================

    @Test
    @Order(8)
    @DisplayName("rechercherParId - retourne la facture correspondante")
    void testRechercherParIdTrouve() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPs);
        when(mockPs.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true);
        when(mockRs.getInt("ID_Facture")).thenReturn(5);
        when(mockRs.getString("numero")).thenReturn("FAC-005");
        when(mockRs.getDate("dateEmission")).thenReturn(new java.sql.Date(System.currentTimeMillis()));
        when(mockRs.getFloat("montantHT")).thenReturn(500.0f);
        when(mockRs.getFloat("montantTTC")).thenReturn(600.0f);
        when(mockRs.getFloat("tva")).thenReturn(20.0f);
        when(mockRs.getString("statut")).thenReturn("payee");
        when(mockRs.getInt("livraison_id")).thenReturn(3);

        Facture result = factureService.rechercherParId(5);

        assertNotNull(result);
        assertEquals(5, result.getIdFacture());
        assertEquals("FAC-005", result.getNumero());
        assertEquals(500.0f, result.getMontantHT());
        assertEquals("payee", result.getStatut());
    }

    @Test
    @Order(9)
    @DisplayName("rechercherParId - retourne null si facture inexistante")
    void testRechercherParIdNonTrouve() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPs);
        when(mockPs.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(false);

        Facture result = factureService.rechercherParId(999);

        assertNull(result);
    }

    // ============================================================
    // Tests : validation metier
    // ============================================================

    @Test
    @Order(10)
    @DisplayName("Facture - montantTTC superieur ou egal au montantHT")
    void testMontantTTCSuperieurOuEgalHT() {
        Facture f = new Facture("FAC-X", 100.0f, 120.0f, 20.0f, "test", 1);
        assertTrue(f.getMontantTTC() >= f.getMontantHT());
    }

    @Test
    @Order(11)
    @DisplayName("Facture - constructeur sans ID laisse idFacture a 0")
    void testConstructeurSansId() {
        Facture f = new Facture("NUM-001", 80.0f, 96.0f, 20.0f, "en attente", 5);
        assertEquals(0, f.getIdFacture());
        assertEquals("NUM-001", f.getNumero());
        assertEquals(5, f.getIdLivraison());
    }

    @Test
    @Order(12)
    @DisplayName("Facture - toString ne retourne pas null")
    void testToStringNonNull() {
        Facture f = new Facture("FAC-STR", 50f, 60f, 20f, "ok", 1);
        assertNotNull(f.toString());
        assertTrue(f.toString().contains("FAC-STR"));
    }
}