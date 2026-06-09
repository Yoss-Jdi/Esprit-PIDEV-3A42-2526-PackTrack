package com.esprit.gui;

import com.esprit.entities.Recompense;
import com.esprit.services.AiService;
import com.esprit.services.RecompenseService;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

public class RecompenseDialog extends JDialog {

    private JTextField typeField, valeurField, seuilField, livreurField, factureField;
    private JTextArea  descArea;

    private Recompense       recompense;
    private RecompenseService service;
    private AiService        aiService = new AiService();

    public RecompenseDialog(Frame parent, Recompense recompense, RecompenseService service) {
        super(parent, true);

        this.recompense = recompense;
        this.service    = service;

        setTitle(recompense == null ? "Ajouter Récompense" : "Modifier Récompense");
        setSize(500, 480);
        setLocationRelativeTo(parent);
        setLayout(new GridLayout(8, 2, 10, 10));

        // =====================================================
        // UI
        // =====================================================
        add(new JLabel("Type"));
        typeField = new JTextField();
        add(typeField);

        add(new JLabel("Valeur"));
        valeurField = new JTextField();
        add(valeurField);

        add(new JLabel("Seuil (points)"));
        seuilField = new JTextField();
        add(seuilField);

        add(new JLabel("Livreur ID"));
        livreurField = new JTextField();
        add(livreurField);

        add(new JLabel("Facture ID"));
        factureField = new JTextField();
        add(factureField);

        // ── Description + bouton IA ───────────────────────────────
        add(new JLabel("Description"));

        // Panel qui contient TextArea + bouton IA côte à côte
        JPanel descPanel = new JPanel(new BorderLayout(6, 0));

        descArea = new JTextArea(3, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descPanel.add(new JScrollPane(descArea), BorderLayout.CENTER);

        // Bouton IA
        JButton btnIA = new JButton("✨ IA");
        btnIA.setBackground(new Color(102, 126, 234));
        btnIA.setForeground(Color.WHITE);
        btnIA.setFocusPainted(false);
        btnIA.setFont(new Font("Arial", Font.BOLD, 12));
        btnIA.setToolTipText("Générer une description avec Google Gemini AI");
        btnIA.setPreferredSize(new Dimension(70, 0));
        descPanel.add(btnIA, BorderLayout.EAST);

        add(descPanel);

        // ── Label statut IA ───────────────────────────────────────
        add(new JLabel(""));
        JLabel labelIA = new JLabel("");
        labelIA.setFont(new Font("Arial", Font.ITALIC, 11));
        labelIA.setForeground(new Color(102, 126, 234));
        add(labelIA);

        // ── Boutons Enregistrer / Annuler ─────────────────────────
        JButton saveBtn   = new JButton("Enregistrer");
        JButton cancelBtn = new JButton("Annuler");
        add(saveBtn);
        add(cancelBtn);

        // =====================================================
        // PREFILL MODE MODIFICATION
        // =====================================================
        if (recompense != null) {
            typeField.setText(recompense.getType());
            valeurField.setText(String.valueOf(recompense.getValeur()));
            descArea.setText(recompense.getDescription());
            seuilField.setText(String.valueOf(recompense.getSeuil()));
            livreurField.setText(String.valueOf(recompense.getIdLivreur()));
            factureField.setText(String.valueOf(recompense.getIdFacture()));
        }

        // =====================================================
        // ACTIONS
        // =====================================================
        saveBtn.addActionListener(e -> saveRecompense());
        cancelBtn.addActionListener(e -> dispose());

        // ── Action bouton IA ──────────────────────────────────────
        btnIA.addActionListener(e -> {
            String nom    = typeField.getText().trim();
            String points = seuilField.getText().trim();

            // Validation
            if (nom.isEmpty()) {
                labelIA.setText("⚠ Remplissez le type d'abord.");
                labelIA.setForeground(new Color(239, 68, 68));
                return;
            }
            if (points.isEmpty()) {
                labelIA.setText("⚠ Remplissez le seuil d'abord.");
                labelIA.setForeground(new Color(239, 68, 68));
                return;
            }

            int pointsInt;
            try {
                pointsInt = Integer.parseInt(points);
            } catch (NumberFormatException ex) {
                labelIA.setText("⚠ Seuil invalide.");
                labelIA.setForeground(new Color(239, 68, 68));
                return;
            }

            // Désactiver bouton + afficher chargement
            btnIA.setEnabled(false);
            btnIA.setText("⏳");
            labelIA.setText("Gemini génère la description...");
            labelIA.setForeground(new Color(102, 126, 234));

            final int pointsFinal = pointsInt;

            // Thread séparé pour ne pas bloquer l'UI Swing
            new Thread(() -> {
                String description = aiService.genererDescriptionRecompense(nom, pointsFinal);

                // Mettre à jour l'UI dans le thread Swing
                SwingUtilities.invokeLater(() -> {
                    descArea.setText(description);
                    btnIA.setEnabled(true);
                    btnIA.setText("✨ IA");
                    labelIA.setText("✅ Description générée par Gemini !");
                    labelIA.setForeground(new Color(22, 163, 74));
                });
            }).start();
        });
    }

    // =====================================================
    // SAVE
    // =====================================================
    private void saveRecompense() {
        try {
            String type   = typeField.getText().trim();
            double valeur = Double.parseDouble(valeurField.getText().trim());
            String desc   = descArea.getText().trim();
            int seuil     = Integer.parseInt(seuilField.getText().trim());
            int livreur   = Integer.parseInt(livreurField.getText().trim());
            int facture   = Integer.parseInt(factureField.getText().trim());

            if (type.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Type obligatoire !");
                return;
            }

            if (recompense == null) {
                service.ajouter(new Recompense(
                        type, valeur, desc, seuil, new Date(), livreur, facture
                ));
            } else {
                recompense.setType(type);
                recompense.setValeur(valeur);
                recompense.setDescription(desc);
                recompense.setSeuil(seuil);
                recompense.setIdLivreur(livreur);
                recompense.setIdFacture(facture);

                service.modifier(recompense);
            }

            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erreur : vérifiez les champs numériques !");
        }
    }
}