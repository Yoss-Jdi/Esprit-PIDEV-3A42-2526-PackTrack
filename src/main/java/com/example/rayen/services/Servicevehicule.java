package com.example.rayen.services;

import com.example.rayen.entities.technicien;
import com.example.rayen.entities.vehicule;
import com.example.rayen.utils.DataSource;
import com.example.rayen.utils.ValidationUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class Servicevehicule {
    private final DataSource dataSource;
    private final ObservableList<vehicule> vehicules = FXCollections.observableArrayList();
    private final ObservableList<vehicule> vehiculesLectureSeule = FXCollections.unmodifiableObservableList(vehicules);
    private final List<Consumer<vehicule>> ecouteursSuppression = new ArrayList<>();

    public Servicevehicule() {
        this.dataSource = DataSource.getInstance();
        chargerDepuisBase();
    }

    public ObservableList<vehicule> afficherVehicules() {
        return vehiculesLectureSeule;
    }

    public vehicule ajouterVehicule(vehicule nouveauVehicule) {
        vehicule vehiculeValide = normaliserEtValider(nouveauVehicule, null);

        try (PreparedStatement statement = dataSource.getCnx().prepareStatement("""
                INSERT INTO vehicule (matricule, marque, modele, couleur, prix_location, disponible, technicien_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, vehiculeValide.getMatricule());
            statement.setString(2, vehiculeValide.getMarque());
            statement.setString(3, vehiculeValide.getModele());
            statement.setString(4, vehiculeValide.getCouleur());
            statement.setDouble(5, vehiculeValide.getPrixLocation());
            statement.setBoolean(6, vehiculeValide.isDisponible());
            if (vehiculeValide.getTechnicienId() == null) {
                statement.setNull(7, java.sql.Types.INTEGER);
            } else {
                statement.setInt(7, vehiculeValide.getTechnicienId());
            }

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    vehiculeValide.setId(generatedKeys.getInt(1));
                }
            }

            vehicules.add(vehiculeValide);
            return vehiculeValide;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'ajouter le vehicule dans la base de donnees.", e);
        }
    }

    public Optional<vehicule> rechercherParId(int id) {
        return vehicules.stream()
                .filter(vehicule -> vehicule.getId() == id)
                .findFirst();
    }

    public boolean modifierVehicule(vehicule vehiculeModifie) {
        Optional<vehicule> vehiculeExistant = rechercherParId(vehiculeModifie.getId());
        if (vehiculeExistant.isEmpty()) {
            return false;
        }

        vehicule vehiculeValide = normaliserEtValider(vehiculeModifie, vehiculeModifie.getId());

        try (PreparedStatement statement = dataSource.getCnx().prepareStatement("""
                UPDATE vehicule
                SET matricule = ?, marque = ?, modele = ?, couleur = ?, prix_location = ?, disponible = ?
                WHERE id = ?
                """)) {
            statement.setString(1, vehiculeValide.getMatricule());
            statement.setString(2, vehiculeValide.getMarque());
            statement.setString(3, vehiculeValide.getModele());
            statement.setString(4, vehiculeValide.getCouleur());
            statement.setDouble(5, vehiculeValide.getPrixLocation());
            statement.setBoolean(6, vehiculeValide.isDisponible());
            statement.setInt(7, vehiculeValide.getId());

            if (statement.executeUpdate() == 0) {
                return false;
            }

            vehicule cible = vehiculeExistant.get();
            cible.setMatricule(vehiculeValide.getMatricule());
            cible.setMarque(vehiculeValide.getMarque());
            cible.setModele(vehiculeValide.getModele());
            cible.setCouleur(vehiculeValide.getCouleur());
            cible.setPrixLocation(vehiculeValide.getPrixLocation());
            cible.setDisponible(vehiculeValide.isDisponible());
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de modifier le vehicule dans la base de donnees.", e);
        }
    }

    public boolean supprimerVehicule(int id) {
        Optional<vehicule> vehiculeASupprimer = rechercherParId(id);
        if (vehiculeASupprimer.isEmpty()) {
            return false;
        }

        try (PreparedStatement statement = dataSource.getCnx().prepareStatement("DELETE FROM vehicule WHERE id = ?")) {
            statement.setInt(1, id);

            if (statement.executeUpdate() == 0) {
                return false;
            }

            vehicule vehiculeSupprime = vehiculeASupprimer.get();
            boolean suppressionReussie = vehicules.remove(vehiculeSupprime);
            if (suppressionReussie) {
                notifierSuppression(vehiculeSupprime);
            }
            return suppressionReussie;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer le vehicule depuis la base de donnees.", e);
        }
    }

    public void assignerTechnicien(int vehiculeId, technicien technicienAssocie) {
        Optional<vehicule> vehiculeRecherche = rechercherParId(vehiculeId);
        if (vehiculeRecherche.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = dataSource.getCnx().prepareStatement("""
                UPDATE vehicule
                SET technicien_id = ?
                WHERE id = ?
                """)) {
            if (technicienAssocie == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, technicienAssocie.getId());
            }
            statement.setInt(2, vehiculeId);
            statement.executeUpdate();

            vehicule vehicule = vehiculeRecherche.get();
            vehicule.setTechnicienId(technicienAssocie == null ? null : technicienAssocie.getId());
            vehicule.setTechnicienNom(technicienAssocie == null ? null : technicienAssocie.getNomComplet());
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'affecter le vehicule au technicien.", e);
        }
    }

    public void retirerTechnicien(int vehiculeId) {
        assignerTechnicien(vehiculeId, null);
    }

    public void ajouterEcouteurSuppression(Consumer<vehicule> ecouteur) {
        if (ecouteur != null) {
            ecouteursSuppression.add(ecouteur);
        }
    }

    private void chargerDepuisBase() {
        vehicules.clear();

        try (PreparedStatement statement = dataSource.getCnx().prepareStatement("""
                SELECT v.id,
                       v.matricule,
                       v.marque,
                       v.modele,
                       v.couleur,
                       v.prix_location,
                       v.disponible,
                       v.technicien_id,
                       TRIM(CONCAT(COALESCE(t.prenom, ''), ' ', COALESCE(t.nom, ''))) AS technicien_nom
                FROM vehicule v
                LEFT JOIN technicien t ON t.id = v.technicien_id
                ORDER BY v.id
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                vehicules.add(construireVehicule(resultSet));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les vehicules depuis la base de donnees.", e);
        }
    }

    private vehicule construireVehicule(ResultSet resultSet) throws SQLException {
        vehicule vehicule = new vehicule(
                resultSet.getInt("id"),
                resultSet.getString("matricule"),
                resultSet.getString("marque"),
                resultSet.getString("modele"),
                resultSet.getString("couleur"),
                resultSet.getDouble("prix_location"),
                resultSet.getBoolean("disponible")
        );

        int technicienId = resultSet.getInt("technicien_id");
        if (!resultSet.wasNull()) {
            vehicule.setTechnicienId(technicienId);
        }

        String technicienNom = resultSet.getString("technicien_nom");
        if (technicienNom != null && !technicienNom.isBlank()) {
            vehicule.setTechnicienNom(technicienNom);
        }

        return vehicule;
    }

    private vehicule normaliserEtValider(vehicule vehiculeAVerifier, Integer idExclu) {
        if (vehiculeAVerifier == null) {
            throw new IllegalArgumentException("Le vehicule est obligatoire.");
        }

        String matricule = ValidationUtils.normaliserMatricule(vehiculeAVerifier.getMatricule());
        String marque = normaliserTexte(vehiculeAVerifier.getMarque(), "marque");
        String modele = normaliserTexte(vehiculeAVerifier.getModele(), "modele");
        String couleur = normaliserTexte(vehiculeAVerifier.getCouleur(), "couleur");

        if (vehiculeAVerifier.getPrixLocation() < 0) {
            throw new IllegalArgumentException("Le prix de location doit etre positif.");
        }

        verifierMatriculeUnique(matricule, idExclu);

        vehicule vehiculeNormalise = new vehicule(
                vehiculeAVerifier.getId(),
                matricule,
                marque,
                modele,
                couleur,
                vehiculeAVerifier.getPrixLocation(),
                vehiculeAVerifier.isDisponible()
        );

        vehiculeNormalise.setTechnicienId(vehiculeAVerifier.getTechnicienId());
        vehiculeNormalise.setTechnicienNom(vehiculeAVerifier.getTechnicienNom());
        return vehiculeNormalise;
    }

    private String normaliserTexte(String valeur, String nomChamp) {
        String valeurNormalisee = valeur == null ? "" : valeur.trim();
        if (valeurNormalisee.isEmpty()) {
            throw new IllegalArgumentException("Le " + nomChamp + " est obligatoire.");
        }
        return valeurNormalisee;
    }

    private void verifierMatriculeUnique(String matricule, Integer idExclu) {
        boolean matriculeExisteDeja = vehicules.stream().anyMatch(vehicule ->
                vehicule.getMatricule() != null
                        && vehicule.getMatricule().equalsIgnoreCase(matricule)
                        && (idExclu == null || vehicule.getId() != idExclu)
        );

        if (matriculeExisteDeja) {
            throw new IllegalArgumentException("Un vehicule avec ce matricule existe deja.");
        }
    }

    private void notifierSuppression(vehicule vehiculeSupprime) {
        for (Consumer<vehicule> ecouteur : ecouteursSuppression) {
            ecouteur.accept(vehiculeSupprime);
        }
    }
}
