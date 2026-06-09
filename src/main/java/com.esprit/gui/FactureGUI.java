package com.esprit.gui;

import com.esprit.entities.Facture;
import com.esprit.entities.Recompense;
import com.esprit.services.FactureService;
import com.esprit.services.RecompenseService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class FactureGUI extends JFrame {

    private final FactureService factureService = new FactureService();
    private final RecompenseService recompenseService = new RecompenseService();

    private JTable factureTable;
    private JTable recompenseTable;

    private DefaultTableModel factureModel;
    private DefaultTableModel recompenseModel;

    public FactureGUI() {

        setTitle("Gestion Factures & Récompenses");
        setSize(950, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();

        // ================= FACTURES =================
        JPanel facturePanel = new JPanel(new BorderLayout());

        factureModel = new DefaultTableModel(
                new String[]{"ID", "Numéro", "Date", "HT", "TTC", "TVA", "Statut", "Livraison"}, 0);

        factureTable = new JTable(factureModel);
        factureTable.setRowHeight(25);

        facturePanel.add(new JScrollPane(factureTable), BorderLayout.CENTER);

        JPanel btnF = new JPanel();

        JButton addF = new JButton("Ajouter");
        JButton editF = new JButton("Modifier");
        JButton delF = new JButton("Supprimer");
        JButton refreshF = new JButton("Actualiser");

        btnF.add(addF);
        btnF.add(editF);
        btnF.add(delF);
        btnF.add(refreshF);

        facturePanel.add(btnF, BorderLayout.SOUTH);

        tabs.addTab("Factures", facturePanel);

        // ================= RECOMPENSES =================
        JPanel recompPanel = new JPanel(new BorderLayout());

        recompenseModel = new DefaultTableModel(
                new String[]{"ID", "Type", "Valeur", "Description", "Seuil", "Date", "Livreur", "Facture"}, 0);

        recompenseTable = new JTable(recompenseModel);
        recompenseTable.setRowHeight(25);

        recompPanel.add(new JScrollPane(recompenseTable), BorderLayout.CENTER);

        JPanel btnR = new JPanel();

        JButton addR = new JButton("Ajouter");
        JButton editR = new JButton("Modifier");
        JButton delR = new JButton("Supprimer");
        JButton refreshR = new JButton("Actualiser");

        btnR.add(addR);
        btnR.add(editR);
        btnR.add(delR);
        btnR.add(refreshR);

        recompPanel.add(btnR, BorderLayout.SOUTH);

        tabs.addTab("Récompenses", recompPanel);

        add(tabs);

        // ================= ACTIONS =================

        refreshF.addActionListener(e -> loadFactures());
        refreshR.addActionListener(e -> loadRecompenses());

        addF.addActionListener(e -> openFactureDialog(null));
        editF.addActionListener(e -> editFacture());
        delF.addActionListener(e -> deleteFacture());

        addR.addActionListener(e -> openRecompenseDialog(null));
        editR.addActionListener(e -> editRecompense());
        delR.addActionListener(e -> deleteRecompense());

        loadFactures();
        loadRecompenses();
    }

    // ================= FACTURES =================
    private void loadFactures() {
        factureModel.setRowCount(0);

        List<Facture> list = factureService.afficherTous();

        for (Facture f : list) {
            factureModel.addRow(new Object[]{
                    f.getIdFacture(),
                    f.getNumero(),
                    f.getDateEmission(),
                    f.getMontantHT(),
                    f.getMontantTTC(),
                    f.getTva(),
                    f.getStatut(),
                    f.getIdLivraison()
            });
        }
    }

    private void openFactureDialog(Facture f) {
        FactureDialog dialog = new FactureDialog(this, f, factureService);
        dialog.setVisible(true);
        loadFactures();
    }

    private void editFacture() {
        int row = factureTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionnez une facture");
            return;
        }

        int id = (int) factureModel.getValueAt(row, 0);
        Facture f = factureService.rechercherParId(id);

        openFactureDialog(f);
    }

    private void deleteFacture() {
        int row = factureTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionnez une facture");
            return;
        }

        int id = (int) factureModel.getValueAt(row, 0);
        factureService.supprimer(id);

        loadFactures();
    }

    // ================= RECOMPENSES =================
    private void loadRecompenses() {
        recompenseModel.setRowCount(0);

        List<Recompense> list = recompenseService.afficherTous();

        for (Recompense r : list) {
            recompenseModel.addRow(new Object[]{
                    r.getIdRecompense(),
                    r.getType(),
                    r.getValeur(),
                    r.getDescription(),
                    r.getSeuil(),
                    r.getDateObtention(),
                    r.getIdLivreur(),
                    r.getIdFacture()
            });
        }
    }

    private void openRecompenseDialog(Recompense r) {

        RecompenseDialog dialog = new RecompenseDialog((Frame) this, r, recompenseService);        dialog.setVisible(true);
        loadRecompenses();
    }

    private void editRecompense() {
        int row = recompenseTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionnez une récompense");
            return;
        }

        int id = (int) recompenseModel.getValueAt(row, 0);
        Recompense r = recompenseService.rechercherParId(id);

        openRecompenseDialog(r);
    }

    private void deleteRecompense() {
        int row = recompenseTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionnez une récompense");
            return;
        }

        int id = (int) recompenseModel.getValueAt(row, 0);
        recompenseService.supprimer(id);

        loadRecompenses();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FactureGUI().setVisible(true));
    }
}