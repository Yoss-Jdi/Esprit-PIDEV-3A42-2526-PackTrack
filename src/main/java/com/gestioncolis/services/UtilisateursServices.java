package com.gestioncolis.services;

import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.enums.Role;
import com.gestioncolis.utils.MyConnection;
import com.gestioncolis.utils.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UtilisateursServices implements ICrud<Utilisateurs> {

    private final Connection cnx = MyConnection.getInstance().getConnection();

    // ─────────────────────────────────────────────────────────────────────────
    // AJOUTER - Le mot de passe est déjà haché avant l'appel
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void ajouter(Utilisateurs u) throws SQLException {
        String query = "INSERT INTO utilisateurs "
                + "(email, mot_de_passe, nom, prenom, telephone, role, photo, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setString(1, u.getEmail());
        ps.setString(2, u.getMotDePasse()); // Déjà haché
        ps.setString(3, u.getNom());
        ps.setString(4, u.getPrenom());
        ps.setString(5, u.getTelephone());
        ps.setString(6, u.getRole().name());
        ps.setString(7, u.getPhoto());
        ps.setTimestamp(8, u.getCreatedAt() != null
                ? Timestamp.valueOf(u.getCreatedAt())
                : Timestamp.valueOf(LocalDateTime.now()));

        ps.executeUpdate();
        System.out.println("✅ Utilisateur ajouté : " + u.getEmail());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUPPRIMER
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void supprimer(int id) throws SQLException {
        String query = "DELETE FROM utilisateurs WHERE id_utilisateur = ?";

        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setInt(1, id);
        int rows = ps.executeUpdate();
        if (rows > 0)
            System.out.println("✅ Utilisateur supprimé (id=" + id + ")");
        else
            System.out.println("⚠️  Aucun utilisateur trouvé avec id=" + id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODIFIER - Le mot de passe est déjà haché si modifié
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void modifier(Utilisateurs u) throws SQLException {
        String query = "UPDATE utilisateurs SET "
                + "email = ?, mot_de_passe = ?, nom = ?, prenom = ?, "
                + "telephone = ?, role = ?, photo = ? "
                + "WHERE id_utilisateur = ?";

        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setString(1, u.getEmail());
        ps.setString(2, u.getMotDePasse()); // Déjà haché (soit nouveau, soit existant)
        ps.setString(3, u.getNom());
        ps.setString(4, u.getPrenom());
        ps.setString(5, u.getTelephone());
        ps.setString(6, u.getRole().name());
        ps.setString(7, u.getPhoto());
        ps.setInt(8, u.getIdUtilisateur());

        int rows = ps.executeUpdate();
        if (rows > 0)
            System.out.println("✅ Utilisateur modifié (id=" + u.getIdUtilisateur() + ")");
        else
            System.out.println("⚠️  Aucun utilisateur trouvé avec id=" + u.getIdUtilisateur());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AFFICHER (SELECT ALL)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<Utilisateurs> afficher() throws SQLException {
        List<Utilisateurs> liste = new ArrayList<>();
        String query = "SELECT * FROM utilisateurs";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(query);

        while (rs.next()) {
            Utilisateurs u = new Utilisateurs();
            u.setIdUtilisateur(rs.getInt("id_utilisateur"));
            u.setEmail(rs.getString("email"));
            u.setMotDePasse(rs.getString("mot_de_passe")); // Récupère le hachage
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setTelephone(rs.getString("telephone"));
            u.setRole(Role.valueOf(rs.getString("role")));
            u.setPhoto(rs.getString("photo"));
            Timestamp ts = rs.getTimestamp("created_at");
            u.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
            liste.add(u);
        }

        return liste;
    }

    public List<Utilisateurs> getClients() throws SQLException {
        List<Utilisateurs> liste = new ArrayList<>();
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM utilisateurs WHERE role = 'CLIENT' ORDER BY nom, prenom");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Utilisateurs u = new Utilisateurs();
            u.setIdUtilisateur(rs.getInt("id_utilisateur"));
            u.setEmail(rs.getString("email"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setTelephone(rs.getString("telephone"));
            u.setRole(Role.valueOf(rs.getString("role")));
            u.setPhoto(rs.getString("photo"));
            liste.add(u);
        }
        return liste;
    }

    /**
     * Authentifie un utilisateur par email et mot de passe
     * @param email - l'email de l'utilisateur
     * @param plainPassword - le mot de passe en clair
     * @return l'utilisateur si authentifié, null sinon
     */
    public Utilisateurs authenticate(String email, String plainPassword) throws SQLException {
        String query = "SELECT * FROM utilisateurs WHERE email = ?";
        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            String hashedPassword = rs.getString("mot_de_passe");
            if (PasswordUtil.verifyPassword(plainPassword, hashedPassword)) {
                Utilisateurs u = new Utilisateurs();
                u.setIdUtilisateur(rs.getInt("id_utilisateur"));
                u.setEmail(rs.getString("email"));
                u.setMotDePasse(hashedPassword);
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setTelephone(rs.getString("telephone"));
                u.setRole(Role.valueOf(rs.getString("role")));
                u.setPhoto(rs.getString("photo"));
                Timestamp ts = rs.getTimestamp("created_at");
                u.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
                return u;
            }
        }
        return null;
    }
}