package com.gestioncolis.services;

import com.gestioncolis.exceptions.ColisIndisponibleException;
import com.gestioncolis.exceptions.StatutInvalideException;
import com.gestioncolis.exceptions.ValidationException;
import com.gestioncolis.models.Colis;
import com.gestioncolis.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ColisService implements ICrudCL<Colis> {

    private final Connection cnx = MyConnection.getInstance().getConnection();


    @Override
    public void ajouter(Colis c) throws SQLException, ValidationException, StatutInvalideException {
        c.valider();
        String sql = """
                INSERT INTO colis
                  (description, articles, adresseDepart, adresseDestination,
                   poids, dimensions, statut, expediteur_id, destinataire_id)
                VALUES (?,?,?,?,?,?,?,?,?)
                """;
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, c.getDescription());
        ps.setString(2, c.getArticles());
        ps.setString(3, c.getAdresseDepart());
        ps.setString(4, c.getAdresseDestination());
        ps.setDouble(5, c.getPoids());
        ps.setString(6, c.getDimensions());
        ps.setString(7, c.getStatut());
        ps.setInt(8, c.getExpediteurId());
        ps.setInt(9, c.getDestinataireId());
        ps.executeUpdate();
        System.out.println("Colis ajouté, Montant calculé : " +
                String.format("%.2f", c.calculerMontant()) + " DT");
    }


    @Override

    public List<Colis> getAll() throws SQLException {
        List<Colis> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM colis ORDER BY ID_Colis DESC");
        while (rs.next()) list.add(map(rs));
        return list;
    }


    public List<Colis> getByExpediteur(int expediteurId) throws SQLException {
        List<Colis> list = new ArrayList<>();
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM colis WHERE expediteur_id = ?");
        ps.setInt(1, expediteurId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(map(rs));
        return list;
    }


    public List<Colis> getColisDisponibles() throws SQLException {
        List<Colis> list = new ArrayList<>();
        String sql = """
                SELECT c.* FROM colis c
                LEFT JOIN livraisons l ON l.colis_id = c.ID_Colis
                WHERE l.ID_Livraison IS NULL
                  AND c.statut = 'en_attente'
                """;
        ResultSet rs = cnx.createStatement().executeQuery(sql);
        while (rs.next()) list.add(map(rs));
        return list;
    }


    /*public void afficherStats() throws SQLException {
        List<Colis> all = getAll();
        long enAttente = all.stream().filter(c -> "en_attente".equals(c.getStatut())).count();
        long enCours   = all.stream().filter(c -> "en_cours".equals(c.getStatut())).count();
        long livre     = all.stream().filter(c -> "livre".equals(c.getStatut())).count();
        System.out.println("Stats colis :");
        System.out.println("   Total      : " + all.size());
        System.out.println("   En attente : " + enAttente);
        System.out.println("   En cours   : " + enCours);
        System.out.println("   Livrés     : " + livre);
    }*/


    @Override
    public void modifier(Colis c)throws SQLException, ValidationException, StatutInvalideException {
        c.valider();
        String sql = """
                UPDATE colis SET
                  description=?, articles=?, adresseDepart=?,
                  adresseDestination=?, poids=?, dimensions=?, statut=?
                WHERE ID_Colis=?
                """;
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, c.getDescription());
        ps.setString(2, c.getArticles());
        ps.setString(3, c.getAdresseDepart());
        ps.setString(4, c.getAdresseDestination());
        ps.setDouble(5, c.getPoids());
        ps.setString(6, c.getDimensions());
        ps.setString(7, c.getStatut());
        ps.setInt(8, c.getId());
        ps.executeUpdate();
        System.out.println("Colis modifié, Nouveau montant : " +
                String.format("%.2f", c.calculerMontant()) + " DT");
    }


    @Override
    public void supprimer(int id) throws SQLException, ColisIndisponibleException, StatutInvalideException {
        PreparedStatement check = cnx.prepareStatement(
                "SELECT statut FROM colis WHERE ID_Colis=?");
        check.setInt(1, id);
        ResultSet rs = check.executeQuery();

        if (!rs.next())
            throw new ColisIndisponibleException(id);

        if (!"en_attente".equals(rs.getString("statut")))
            throw new StatutInvalideException(rs.getString("statut"));

        PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM colis WHERE ID_Colis=?");
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Colis supprimé avec succès");
    }

    private Colis map(ResultSet rs) throws SQLException {
        Timestamp tsExpedition = rs.getTimestamp("dateExpedition");

        return new Colis(
                rs.getInt("ID_Colis"),
                rs.getString("description"),
                rs.getString("articles"),
                rs.getString("adresseDepart"),
                rs.getString("adresseDestination"),
                rs.getDouble("poids"),
                rs.getString("dimensions"),
                rs.getString("statut"),
                tsExpedition != null ? tsExpedition.toLocalDateTime() : null,
                rs.getInt("expediteur_id"),
                rs.getInt("destinataire_id")
        );
    }


    public List<Colis> getByDestinataire(int destinataireId) throws SQLException {
        List<Colis> list = new ArrayList<>();
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM colis WHERE destinataire_id = ? ORDER BY ID_Colis DESC");
        ps.setInt(1, destinataireId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(map(rs));
        return list;
    }
}