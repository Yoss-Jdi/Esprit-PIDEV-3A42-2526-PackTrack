package com.gestioncolis.services;

import com.gestioncolis.models.Utilisateur;
import com.gestioncolis.utils.MyConnection;
import com.gestioncolis.utils.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurService {

    private final Connection cnx = MyConnection.getInstance().getConnection();

    // Colonnes réelles de la table `utilisateurs`
    // id_utilisateur | email | mot_de_passe | nom | prenom | telephone | role | photo | created_at

    public Utilisateur connecter(String email, String motDePasse) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM utilisateurs WHERE email = ? AND mot_de_passe = ?");
        ps.setString(1, email);
        ps.setString(2, motDePasse);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Utilisateur u = map(rs);
            SessionManager.getInstance().setUtilisateurConnecte(u);
            return u;
        }
        return null;
    }

    // Remplacer getClients()
    public List<Utilisateur> getClients() throws SQLException {
        List<Utilisateur> list = new ArrayList<>();
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM utilisateurs WHERE role IN ('ROLE_CLIENT','CLIENT') ORDER BY Nom, Prenom");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(map(rs));
        return list;
    }

    // Remplacer getLivreurs()
    public List<Utilisateur> getLivreurs() throws SQLException {
        List<Utilisateur> list = new ArrayList<>();
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM utilisateurs WHERE role IN ('ROLE_LIVREUR','LIVREUR') ORDER BY Nom, Prenom");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(map(rs));
        return list;
    }

    public Utilisateur getById(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM utilisateurs WHERE id_utilisateur = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return map(rs);
        return null;
    }

    private Utilisateur map(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");   // ← était "createdAt"
        return new Utilisateur(
                rs.getInt("id_utilisateur"),
                rs.getString("email"),                  // ← était "Email"
                rs.getString("mot_de_passe"),           // ← était "MotDePasse"
                rs.getString("nom"),                    // ← était "Nom"
                rs.getString("prenom"),                 // ← était "Prenom"
                rs.getString("telephone"),
                Utilisateur.Role.fromString(rs.getString("role")),
                rs.getString("photo"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}