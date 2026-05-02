package com.gestioncolis.services;

import com.gestioncolis.exceptions.ColisIndisponibleException;
import com.gestioncolis.exceptions.LivraisonIntrouvableException;
import com.gestioncolis.exceptions.StatutInvalideException;
import com.gestioncolis.exceptions.ValidationException;
import com.gestioncolis.models.Colis;
import com.gestioncolis.models.Livraison;
import com.gestioncolis.utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LivraisonService implements ICrud<Livraison> {

    private Connection cnx;
    private final ColisService colisService = new ColisService();

    public LivraisonService() {
        cnx = MyConnection.getInstance().getConnection();
    }

    private static final String select =
            "SELECT l.*, CONCAT(u.prenom, ' ', u.nom) AS nomLivreur, c.description AS descriptionColis "
                    + "FROM livraisons l "
                    + "LEFT JOIN utilisateurs u ON u.id_utilisateur = l.livreur_id "
                    + "LEFT JOIN colis c ON c.ID_Colis = l.colis_id";

    @Override
    public void ajouter(Livraison l) throws SQLException, ColisIndisponibleException {
        List<Colis> dispo = colisService.getColisDisponibles();
        boolean existe = dispo.stream().anyMatch(c -> c.getId() == l.getColisId());
        if (!existe)
            throw new ColisIndisponibleException(l.getColisId());

        try {
            String queryPoids = "SELECT poids FROM colis WHERE ID_Colis=" + l.getColisId();
            Statement stPoids = cnx.createStatement();
            ResultSet rs = stPoids.executeQuery(queryPoids);
            if (rs.next()) {
                double poids = rs.getDouble("poids");
                l.setTotal(10.0 + (poids * 2.0));
            }


            String now = Timestamp.valueOf(LocalDateTime.now()).toString();
            String query = "INSERT INTO livraisons (statut, dateDebut, distanceKm, dureeEstimeeMinutes, total, colis_id, livreur_id) "
                    + "VALUES ('en_cours','" + now + "'," + l.getDistanceKm() + ","
                    + l.getDureeEstimeeMinutes() + "," + l.getTotal() + ","
                    + l.getColisId() + "," + l.getLivreurId() + ")";
            Statement st = cnx.createStatement();
            st.executeUpdate(query);


            String queryUpd = "UPDATE colis SET statut='en_cours', dateExpedition='" + now + "' WHERE ID_Colis=" + l.getColisId();
            Statement stUpd = cnx.createStatement();
            stUpd.executeUpdate(queryUpd);

            System.out.println("Colis pris en charge, Total : " + String.format("%.2f", l.getTotal()) + " DT");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Livraison> getAll() throws SQLException {
        List<Livraison> list = new ArrayList<>();
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(select);
            while (rs.next()) {
                list.add(construireLivraison(rs));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw e;
        }
        return list;
    }

    public List<Livraison> getByLivreur(int livreurId) throws SQLException {
        List<Livraison> list = new ArrayList<>();
        String query = select + " WHERE l.livreur_id=" + livreurId;
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                list.add(construireLivraison(rs));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw e;
        }
        return list;
    }

    @Override
    public void modifier(Livraison l) throws SQLException, ValidationException, StatutInvalideException {
        l.valider();
        String query = "UPDATE livraisons SET statut='" + l.getStatut()
                + "', distanceKm=" + l.getDistanceKm()
                + ", dureeEstimeeMinutes=" + l.getDureeEstimeeMinutes()
                + ", total=" + l.getTotal()
                + " WHERE ID_Livraison=" + l.getId();
        try {
            Statement st = cnx.createStatement();
            st.executeUpdate(query);
            System.out.println("Livraison modifiee.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw e;
        }
    }

    public void terminer(int livraisonId) throws SQLException, LivraisonIntrouvableException, StatutInvalideException {
        try {
            Statement stGet = cnx.createStatement();
            ResultSet rs = stGet.executeQuery("SELECT * FROM livraisons WHERE ID_Livraison=" + livraisonId);

            if (!rs.next())
                throw new LivraisonIntrouvableException(livraisonId);
            if ("termine".equals(rs.getString("statut")))
                throw new StatutInvalideException("termine");

            int colisId = rs.getInt("colis_id");
            double total = rs.getDouble("total");
            String now = Timestamp.valueOf(LocalDateTime.now()).toString();

            Statement stUpd = cnx.createStatement();
            stUpd.executeUpdate("UPDATE livraisons SET statut='termine', dateFin='" + now + "' WHERE ID_Livraison=" + livraisonId);

            Statement stColis = cnx.createStatement();
            stColis.executeUpdate("UPDATE colis SET statut='livre' WHERE ID_Colis=" + colisId);

            System.out.println("Livraison terminee");
            System.out.println("Total : " + String.format("%.2f", total) + " DT");
            System.out.println("Colis marque comme livre.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw e;
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        try {
            Statement st = cnx.createStatement();
            st.executeUpdate("DELETE FROM livraisons WHERE ID_Livraison=" + id);
            System.out.println("Livraison supprimee.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw e;
        }
    }

    private Livraison construireLivraison(ResultSet rs) throws SQLException {
        Timestamp tsDebut = rs.getTimestamp("dateDebut");
        Timestamp tsFin   = rs.getTimestamp("dateFin");
        Livraison liv = new Livraison(
                rs.getInt("ID_Livraison"),
                rs.getString("statut"),
                tsDebut != null ? tsDebut.toLocalDateTime() : null,
                tsFin   != null ? tsFin.toLocalDateTime()   : null,
                rs.getDouble("distanceKm"),
                rs.getDouble("dureeEstimeeMinutes"),
                rs.getDouble("total"),
                rs.getInt("colis_id"),
                rs.getInt("livreur_id")
        );
        try { liv.setNomLivreur(rs.getString("nomLivreur")); }       catch (SQLException ignored) {}
        try { liv.setDescriptionColis(rs.getString("descriptionColis")); } catch (SQLException ignored) {}
        return liv;
    }
}