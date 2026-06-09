package com.esprit.gui;

import com.esprit.entities.Facture;
import com.esprit.services.FactureService;

import javax.swing.*;
import java.awt.*;

public class FactureDialog extends JDialog {

    private JTextField numeroField, htField, tvaField, statutField, livraisonField;
    private JLabel ttcLabel;

    private Facture facture;
    private FactureService service;

    public FactureDialog(Frame parent, Facture facture, FactureService service) {
        super(parent, true);

        this.facture = facture;
        this.service = service;

        setTitle(facture == null ? "Ajouter Facture" : "Modifier Facture");
        setSize(420, 320);
        setLocationRelativeTo(parent);
        setLayout(new GridLayout(7, 2, 10, 10));

        // ================= UI =================
        add(new JLabel("Numéro"));
        numeroField = new JTextField();
        add(numeroField);

        add(new JLabel("Montant HT"));
        htField = new JTextField();
        add(htField);

        add(new JLabel("TVA (%)"));
        tvaField = new JTextField();
        add(tvaField);

        add(new JLabel("Montant TTC"));
        ttcLabel = new JLabel("0.0");
        add(ttcLabel);

        add(new JLabel("Statut"));
        statutField = new JTextField();
        add(statutField);

        add(new JLabel("Livraison ID"));
        livraisonField = new JTextField();
        add(livraisonField);

        JButton saveBtn = new JButton("Enregistrer");
        JButton cancelBtn = new JButton("Annuler");

        add(saveBtn);
        add(cancelBtn);

        // ================= PREFILL =================
        if (facture != null) {
            numeroField.setText(facture.getNumero());
            htField.setText(String.valueOf(facture.getMontantHT()));
            tvaField.setText(String.valueOf(facture.getTva()));
            statutField.setText(facture.getStatut());
            livraisonField.setText(String.valueOf(facture.getIdLivraison()));

            updateTTC();
        }

        // ================= EVENTS =================
        htField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                updateTTC();
            }
        });

        tvaField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                updateTTC();
            }
        });

        saveBtn.addActionListener(e -> saveFacture());
        cancelBtn.addActionListener(e -> dispose());
    }

    // ================= CALCUL TTC =================
    private void updateTTC() {
        try {
            float ht = Float.parseFloat(htField.getText());
            float tva = Float.parseFloat(tvaField.getText());

            float ttc = ht + (ht * tva / 100);
            ttcLabel.setText(String.format("%.2f", ttc));

        } catch (Exception ignored) {
            ttcLabel.setText("0.0");
        }
    }

    // ================= SAVE =================
    private void saveFacture() {
        try {
            String num = numeroField.getText();
            float ht = Float.parseFloat(htField.getText());
            float tva = Float.parseFloat(tvaField.getText());
            String statut = statutField.getText();
            int idLiv = Integer.parseInt(livraisonField.getText());

            float ttc = ht + (ht * tva / 100);

            if (facture == null) {
                service.ajouter(new Facture(num, ht, ttc, tva, statut, idLiv));
            } else {
                facture.setNumero(num);
                facture.setMontantHT(ht);
                facture.setMontantTTC(ttc);
                facture.setTva(tva);
                facture.setStatut(statut);
                facture.setIdLivraison(idLiv);

                service.modifier(facture);
            }

            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erreur : vérifiez les champs numériques !");
        }
    }
}