package com.esprit.services;

import com.esprit.entities.Facture;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FactureService {

    private Connection connection;

    public FactureService() {
        connection = MyDataBase.getInstance().getConnection();
    }

    public void ajouter(Facture facture) {
        String req = "INSERT INTO factures (numero, montantHT, montantTTC, tva, statut, livraison_id) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setString(1, facture.getNumero());
            ps.setFloat(2, facture.getMontantHT());
            ps.setFloat(3, facture.getMontantTTC());
            ps.setFloat(4, facture.getTva());
            ps.setString(5, facture.getStatut());
            ps.setInt(6, facture.getIdLivraison());
            ps.executeUpdate();
            System.out.println("✅ Facture ajoutée !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur ajout : " + e.getMessage());
        }
    }

    public List<Facture> afficherTous() {
        List<Facture> factures = new ArrayList<>();
        String req = "SELECT * FROM factures";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                factures.add(new Facture(
                        rs.getInt("ID_Facture"),
                        rs.getString("numero"),
                        rs.getDate("dateEmission"),
                        rs.getFloat("montantHT"),
                        rs.getFloat("montantTTC"),
                        rs.getFloat("tva"),
                        rs.getString("statut"),
                        rs.getInt("livraison_id")
                ));
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur affichage : " + e.getMessage());
        }
        return factures;
    }



    public void modifier(Facture facture) {
        String req = "UPDATE factures SET numero=?, montantHT=?, montantTTC=?, tva=?, statut=?, livraison_id=? WHERE ID_Facture=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setString(1, facture.getNumero());
            ps.setFloat(2, facture.getMontantHT());
            ps.setFloat(3, facture.getMontantTTC());
            ps.setFloat(4, facture.getTva());
            ps.setString(5, facture.getStatut());
            ps.setInt(6, facture.getIdLivraison());
            ps.setInt(7, facture.getIdFacture());
            ps.executeUpdate();
            System.out.println("✅ Facture modifiée !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur modification : " + e.getMessage());
        }
    }

    public void supprimer(int idFacture) {
        String req = "DELETE FROM factures WHERE ID_Facture=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, idFacture);
            ps.executeUpdate();
            System.out.println("✅ Facture supprimée !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur suppression : " + e.getMessage());
        }
    }

    public void savePdfUrl(String numero, String pdfUrl) {
        try {
            String sql = "UPDATE factures SET pdf_url = ? WHERE numero = ?";
            PreparedStatement ps = MyDataBase.getInstance().getConnection().prepareStatement(sql);
            ps.setString(1, pdfUrl);
            ps.setString(2, numero);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public Facture rechercherParId(int idFacture) {
        String req = "SELECT * FROM factures WHERE ID_Facture=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, idFacture);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Facture(
                        rs.getInt("ID_Facture"),
                        rs.getString("numero"),
                        rs.getDate("dateEmission"),
                        rs.getFloat("montantHT"),
                        rs.getFloat("montantTTC"),
                        rs.getFloat("tva"),
                        rs.getString("statut"),
                        rs.getInt("livraison_id")
                );
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur recherche : " + e.getMessage());
        }
        return null;
    }
}