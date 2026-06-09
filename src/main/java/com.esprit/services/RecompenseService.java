package com.esprit.services;

import com.esprit.entities.Recompense;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecompenseService {

    private Connection connection;
    private final MailService mailService = new MailService();

    public RecompenseService() {
        connection = MyDataBase.getInstance().getConnection();
    }

    // =====================================================
    // AJOUTER + ENVOI MAIL AU LIVREUR
    // =====================================================
    public void ajouter(Recompense r) {
        String req = "INSERT INTO recompenses (type, valeur, description, seuil, dateObtention, livreur_id, facture_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            connection.setAutoCommit(true);

            PreparedStatement ps = connection.prepareStatement(req);
            ps.setString(1, r.getType());
            ps.setDouble(2, r.getValeur());
            ps.setString(3, r.getDescription());
            ps.setInt(4, r.getSeuil());

            ps.setTimestamp(5, r.getDateObtention() != null
                    ? new Timestamp(r.getDateObtention().getTime())
                    : new Timestamp(System.currentTimeMillis()));

            if (r.getIdLivreur() > 0)
                ps.setInt(6, r.getIdLivreur());
            else
                ps.setNull(6, Types.INTEGER);

            if (r.getIdFacture() != null)
                ps.setInt(7, r.getIdFacture());
            else
                ps.setNull(7, Types.INTEGER);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Recompense ajoutée en BDD ! (" + rows + " ligne)");

                // ── ENVOI MAIL ──
                if (r.getIdLivreur() > 0) {
                    envoyerMailSiLivreurExiste(r);
                }

            } else {
                System.out.println("⚠ INSERT exécuté mais 0 lignes affectées !");
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur SQL ajout     : " + e.getMessage());
            System.out.println("❌ SQLState             : " + e.getSQLState());
            System.out.println("❌ ErrorCode            : " + e.getErrorCode());
        }
    }

    // =====================================================
    // RÉCUPÉRER EMAIL + NOM LIVREUR PUIS ENVOYER MAIL
    // =====================================================
    private void envoyerMailSiLivreurExiste(Recompense r) {
        String sql = "SELECT CONCAT(nom, ' ', prenom) AS nom_complet, email " +
                "FROM utilisateurs WHERE id_utilisateur = ? AND role = 'livreur'";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, r.getIdLivreur());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String nomLivreur   = rs.getString("nom_complet");
                String emailLivreur = rs.getString("email");

                if (emailLivreur != null && !emailLivreur.isBlank()) {
                    mailService.envoyerMailRecompense(emailLivreur, nomLivreur, r);
                } else {
                    System.out.println("⚠ Livreur sans email — mail non envoyé.");
                }
            } else {
                System.out.println("⚠ Aucun livreur trouvé avec id=" + r.getIdLivreur());
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération livreur pour mail : " + e.getMessage());
        }
    }

    // =====================================================
    // AFFICHER TOUS
    // =====================================================
    public List<Recompense> afficherTous() {
        List<Recompense> list = new ArrayList<>();
        String req = "SELECT * FROM recompenses";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                list.add(new Recompense(
                        rs.getInt("ID_Recompense"),
                        rs.getString("type"),
                        rs.getDouble("valeur"),
                        rs.getString("description"),
                        rs.getInt("seuil"),
                        rs.getTimestamp("dateObtention"),
                        rs.getInt("livreur_id"),
                        rs.getObject("facture_id") != null ? rs.getInt("facture_id") : null
                ));
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur affichage : " + e.getMessage());
        }
        return list;
    }

    // =====================================================
    // MODIFIER
    // =====================================================
    public void modifier(Recompense r) {
        String req = "UPDATE recompenses SET type=?, valeur=?, description=?, seuil=?, dateObtention=?, livreur_id=?, facture_id=? WHERE ID_Recompense=?";
        try {
            connection.setAutoCommit(true);

            PreparedStatement ps = connection.prepareStatement(req);
            ps.setString(1, r.getType());
            ps.setDouble(2, r.getValeur());
            ps.setString(3, r.getDescription());
            ps.setInt(4, r.getSeuil());

            ps.setTimestamp(5, r.getDateObtention() != null
                    ? new Timestamp(r.getDateObtention().getTime())
                    : new Timestamp(System.currentTimeMillis()));

            if (r.getIdLivreur() > 0)
                ps.setInt(6, r.getIdLivreur());
            else
                ps.setNull(6, Types.INTEGER);

            if (r.getIdFacture() != null)
                ps.setInt(7, r.getIdFacture());
            else
                ps.setNull(7, Types.INTEGER);

            ps.setInt(8, r.getIdRecompense());

            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("✅ Recompense modifiée en BDD !");
            else
                System.out.println("⚠ UPDATE : 0 lignes affectées (ID introuvable ?)");

        } catch (SQLException e) {
            System.out.println("❌ Erreur SQL modification : " + e.getMessage());
            System.out.println("❌ SQLState               : " + e.getSQLState());
            System.out.println("❌ ErrorCode              : " + e.getErrorCode());
        }
    }

    // =====================================================
    // SUPPRIMER
    // =====================================================
    public void supprimer(int id) {
        String req = "DELETE FROM recompenses WHERE ID_Recompense=?";
        try {
            connection.setAutoCommit(true);
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Recompense supprimée !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur suppression : " + e.getMessage());
        }
    }

    // =====================================================
    // RECHERCHER PAR ID
    // =====================================================
    public Recompense rechercherParId(int id) {
        String req = "SELECT * FROM recompenses WHERE ID_Recompense=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Recompense(
                        rs.getInt("ID_Recompense"),
                        rs.getString("type"),
                        rs.getDouble("valeur"),
                        rs.getString("description"),
                        rs.getInt("seuil"),
                        rs.getTimestamp("dateObtention"),
                        rs.getInt("livreur_id"),
                        rs.getObject("facture_id") != null ? rs.getInt("facture_id") : null
                );
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur recherche : " + e.getMessage());
        }
        return null;
    }
}