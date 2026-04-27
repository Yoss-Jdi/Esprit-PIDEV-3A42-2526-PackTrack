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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Servicetechnicien {
    private final Servicevehicule servicevehicule;
    private final DataSource dataSource;
    private final ObservableList<technicien> techniciens = FXCollections.observableArrayList();
    private final ObservableList<technicien> techniciensLectureSeule = FXCollections.unmodifiableObservableList(techniciens);

    public Servicetechnicien(Servicevehicule servicevehicule) {
        if (servicevehicule == null) {
            throw new IllegalArgumentException("Le service vehicule est obligatoire.");
        }

        this.servicevehicule = servicevehicule;
        this.dataSource = DataSource.getInstance();
        this.servicevehicule.ajouterEcouteurSuppression(this::retirerVehiculeDesTechniciens);
        chargerDepuisBase();
    }

    public ObservableList<technicien> afficherTechniciens() {
        return techniciensLectureSeule;
    }

    public ObservableList<vehicule> afficherVehicules() {
        return servicevehicule.afficherVehicules();
    }

    public technicien ajouterTechnicien(technicien nouveauTechnicien) {
        technicien technicienValide = normaliserEtValider(nouveauTechnicien, null);
        technicien technicienAjoute = new technicien(
                insererTechnicien(technicienValide),
                technicienValide.getNom(),
                technicienValide.getPrenom(),
                technicienValide.getSpecialite(),
                technicienValide.getTelephone(),
                technicienValide.getEmail(),
                technicienValide.getVehicules()
        );

        techniciens.add(technicienAjoute);
        synchroniserAffectations(technicienAjoute, List.of());
        return technicienAjoute;
    }

    public Optional<technicien> rechercherParId(int id) {
        return techniciens.stream()
                .filter(technicien -> technicien.getId() == id)
                .findFirst();
    }

    public boolean modifierTechnicien(technicien technicienModifie) {
        Optional<technicien> technicienExistant = rechercherParId(technicienModifie.getId());
        if (technicienExistant.isEmpty()) {
            return false;
        }

        technicien technicienValide = normaliserEtValider(technicienModifie, technicienModifie.getId());
        if (!mettreAJourTechnicien(technicienValide)) {
            return false;
        }

        technicien cible = technicienExistant.get();
        List<vehicule> anciensVehicules = new ArrayList<>(cible.getVehicules());

        cible.setNom(technicienValide.getNom());
        cible.setPrenom(technicienValide.getPrenom());
        cible.setSpecialite(technicienValide.getSpecialite());
        cible.setTelephone(technicienValide.getTelephone());
        cible.setEmail(technicienValide.getEmail());
        cible.setVehicules(technicienValide.getVehicules());

        synchroniserAffectations(cible, anciensVehicules);
        return true;
    }

    public boolean supprimerTechnicien(int id) {
        Optional<technicien> technicienASupprimer = rechercherParId(id);
        if (technicienASupprimer.isEmpty()) {
            return false;
        }

        technicien cible = technicienASupprimer.get();

        try (PreparedStatement statement = dataSource.getCnx().prepareStatement("DELETE FROM technicien WHERE id = ?")) {
            statement.setInt(1, id);

            if (statement.executeUpdate() == 0) {
                return false;
            }

            for (vehicule vehicule : new ArrayList<>(cible.getVehicules())) {
                vehicule.setTechnicienId(null);
                vehicule.setTechnicienNom(null);
            }
            cible.getVehicules().clear();
            return techniciens.remove(cible);
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer le technicien depuis la base de donnees.", e);
        }
    }

    private void chargerDepuisBase() {
        techniciens.clear();
        Map<Integer, technicien> techniciensParId = new LinkedHashMap<>();

        try (PreparedStatement statement = dataSource.getCnx().prepareStatement("""
                SELECT id, nom, prenom, specialite, telephone, email
                FROM technicien
                ORDER BY id
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                technicien technicien = new technicien(
                        resultSet.getInt("id"),
                        resultSet.getString("nom"),
                        resultSet.getString("prenom"),
                        resultSet.getString("specialite"),
                        resultSet.getString("telephone"),
                        resultSet.getString("email"),
                        List.of()
                );

                techniciens.add(technicien);
                techniciensParId.put(technicien.getId(), technicien);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les techniciens depuis la base de donnees.", e);
        }

        for (vehicule vehicule : servicevehicule.afficherVehicules()) {
            Integer technicienId = vehicule.getTechnicienId();
            if (technicienId == null) {
                continue;
            }

            technicien technicien = techniciensParId.get(technicienId);
            if (technicien != null) {
                technicien.getVehicules().add(vehicule);
            }
        }
    }

    private int insererTechnicien(technicien technicien) {
        try (PreparedStatement statement = dataSource.getCnx().prepareStatement("""
                INSERT INTO technicien (nom, prenom, specialite, telephone, email)
                VALUES (?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, technicien.getNom());
            statement.setString(2, technicien.getPrenom());
            statement.setString(3, technicien.getSpecialite());
            statement.setString(4, technicien.getTelephone());
            statement.setString(5, technicien.getEmail());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            throw new IllegalStateException("L'identifiant du technicien n'a pas pu etre recupere.");
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'ajouter le technicien dans la base de donnees.", e);
        }
    }

    private boolean mettreAJourTechnicien(technicien technicien) {
        try (PreparedStatement statement = dataSource.getCnx().prepareStatement("""
                UPDATE technicien
                SET nom = ?, prenom = ?, specialite = ?, telephone = ?, email = ?
                WHERE id = ?
                """)) {
            statement.setString(1, technicien.getNom());
            statement.setString(2, technicien.getPrenom());
            statement.setString(3, technicien.getSpecialite());
            statement.setString(4, technicien.getTelephone());
            statement.setString(5, technicien.getEmail());
            statement.setInt(6, technicien.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de modifier le technicien dans la base de donnees.", e);
        }
    }

    private technicien normaliserEtValider(technicien technicienAVerifier, Integer idExclu) {
        if (technicienAVerifier == null) {
            throw new IllegalArgumentException("Le technicien est obligatoire.");
        }

        String nom = normaliserTexte(technicienAVerifier.getNom(), "nom");
        String prenom = normaliserTexte(technicienAVerifier.getPrenom(), "prenom");
        String specialite = normaliserTexte(technicienAVerifier.getSpecialite(), "specialite");
        String telephone = normaliserTexte(technicienAVerifier.getTelephone(), "telephone");
        String email = ValidationUtils.normaliserEmailObligatoire(technicienAVerifier.getEmail(), "email");

        verifierEmailUnique(email, idExclu);
        List<vehicule> vehiculesSelectionnes = normaliserVehicules(technicienAVerifier.getVehicules());

        return new technicien(
                technicienAVerifier.getId(),
                nom,
                prenom,
                specialite,
                telephone,
                email,
                vehiculesSelectionnes
        );
    }

    private String normaliserTexte(String valeur, String nomChamp) {
        String valeurNormalisee = valeur == null ? "" : valeur.trim();
        if (valeurNormalisee.isEmpty()) {
            throw new IllegalArgumentException("Le " + nomChamp + " est obligatoire.");
        }
        return valeurNormalisee;
    }

    private void verifierEmailUnique(String email, Integer idExclu) {
        boolean emailExisteDeja = techniciens.stream().anyMatch(technicien ->
                technicien.getEmail() != null
                        && technicien.getEmail().equalsIgnoreCase(email)
                        && (idExclu == null || technicien.getId() != idExclu)
        );

        if (emailExisteDeja) {
            throw new IllegalArgumentException("Un technicien avec cet email existe deja.");
        }
    }

    private List<vehicule> normaliserVehicules(List<vehicule> vehiculesSelectionnes) {
        if (vehiculesSelectionnes == null || vehiculesSelectionnes.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Integer, vehicule> vehiculesParId = new LinkedHashMap<>();
        for (vehicule vehiculeSelectionne : vehiculesSelectionnes) {
            if (vehiculeSelectionne == null) {
                continue;
            }

            vehicule vehiculeExistant = servicevehicule.rechercherParId(vehiculeSelectionne.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Le vehicule #" + vehiculeSelectionne.getId() + " est introuvable."));
            vehiculesParId.put(vehiculeExistant.getId(), vehiculeExistant);
        }

        return new ArrayList<>(vehiculesParId.values());
    }

    private void synchroniserAffectations(technicien technicienCible, List<vehicule> anciensVehicules) {
        Set<Integer> nouveauxVehiculesIds = technicienCible.getVehicules().stream()
                .map(vehicule::getId)
                .collect(Collectors.toSet());

        for (vehicule ancienVehicule : anciensVehicules) {
            Integer technicienId = ancienVehicule.getTechnicienId();
            if (!nouveauxVehiculesIds.contains(ancienVehicule.getId())
                    && technicienId != null
                    && technicienId == technicienCible.getId()) {
                servicevehicule.retirerTechnicien(ancienVehicule.getId());
            }
        }

        for (vehicule vehiculeSelectionne : technicienCible.getVehicules()) {
            Integer ancienTechnicienId = vehiculeSelectionne.getTechnicienId();
            if (ancienTechnicienId != null && ancienTechnicienId != technicienCible.getId()) {
                rechercherParId(ancienTechnicienId).ifPresent(ancienTechnicien ->
                        ancienTechnicien.getVehicules().removeIf(vehicule -> vehicule.getId() == vehiculeSelectionne.getId()));
            }

            servicevehicule.assignerTechnicien(vehiculeSelectionne.getId(), technicienCible);
        }
    }

    private void retirerVehiculeDesTechniciens(vehicule vehiculeSupprime) {
        for (technicien technicien : techniciens) {
            technicien.getVehicules().removeIf(vehicule -> vehicule.getId() == vehiculeSupprime.getId());
        }
    }
}
