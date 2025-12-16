package com.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ManajemenAbsensi extends JFrame {
    
    DefaultTableModel tableModel;
    JTable table;
    
    // Format tanggal untuk input manual
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public ManajemenAbsensi() {
        setTitle("Manajemen Data Absensi (Koreksi / Ijin / Sakit)");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- 1. HEADER ---
        JLabel labelJudul = new JLabel("DATA ABSENSI & KOREKSI MANUAL", SwingConstants.CENTER);
        labelJudul.setFont(new Font("Arial", Font.BOLD, 20));
        labelJudul.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(labelJudul, BorderLayout.NORTH);

        // --- 2. TABEL DATA ---
        // Kita tampilkan ID Log biar unik, dan ID Karyawan
        String[] kolom = {"ID Log", "ID Karyawan", "Nama", "Waktu Masuk", "Waktu Pulang", "Status"};
        tableModel = new DefaultTableModel(kolom, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        table = new JTable(tableModel);
        table.setRowHeight(25);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- 3. PANEL TOMBOL ---
        JPanel panelTombol = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        
        JButton btnRefresh = new JButton("Refresh Data");
        JButton btnInput = new JButton("Input Sakit / Ijin / Cuti"); // Tombol Baru
        JButton btnEdit = new JButton("Edit Status Log");
        JButton btnHapus = new JButton("Hapus Log");
        
        // Warna Tombol
        btnInput.setBackground(new Color(39, 174, 96)); btnInput.setForeground(Color.WHITE);
        btnEdit.setBackground(new Color(243, 156, 18)); btnEdit.setForeground(Color.WHITE);
        btnHapus.setBackground(new Color(192, 57, 43)); btnHapus.setForeground(Color.WHITE);
        
        panelTombol.add(btnRefresh);
        panelTombol.add(btnInput);
        panelTombol.add(btnEdit);
        panelTombol.add(btnHapus);
        add(panelTombol, BorderLayout.SOUTH);

        // --- 4. LOGIKA TOMBOL ---
        
        btnRefresh.addActionListener(e -> loadData());
        
        // Tombol INPUT MANUAL (SAKIT/IJIN)
        btnInput.addActionListener(e -> tampilkanFormInputManual());

        // Tombol EDIT
        btnEdit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Pilih baris log dulu!");
                return;
            }
            int idLog = (int) tableModel.getValueAt(row, 0);
            String statusLama = (String) tableModel.getValueAt(row, 5);
            tampilkanFormEdit(idLog, statusLama);
        });

        // Tombol HAPUS
        btnHapus.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Pilih baris log dulu!");
                return;
            }
            int idLog = (int) tableModel.getValueAt(row, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Hapus log absensi ini?", "Hapus", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) hapusLog(idLog);
        });

        loadData();
        setVisible(true);
    }

    // --- READ DATA ---
    private void loadData() {
        tableModel.setRowCount(0);
        try {
            Connection conn = KoneksiDb.connect();
            String sql = "SELECT log.id, k.id as id_karyawan, k.nama, log.waktu_masuk, log.waktu_pulang, log.status_kehadiran " +
                         "FROM log_absensi log JOIN karyawan k ON log.karyawan_id = k.id " +
                         "ORDER BY log.waktu_masuk DESC"; // Urutkan dari yang terbaru
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getInt("id_karyawan"),
                    rs.getString("nama"),
                    rs.getString("waktu_masuk"),
                    rs.getString("waktu_pulang"),
                    rs.getString("status_kehadiran")
                });
            }
            conn.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- CREATE MANUAL (SAKIT/IJIN) ---
    private void tampilkanFormInputManual() {
        // Dropdown Karyawan
        JComboBox<String> comboKaryawan = new JComboBox<>();
        loadKaryawanKeCombo(comboKaryawan); // Isi dropdown dari DB
        
        // Dropdown Status
        String[] statusList = {"Sakit", "Ijin", "Cuti", "Dinas Luar"};
        JComboBox<String> comboStatus = new JComboBox<>(statusList);
        
        Object[] message = {
            "Pilih Karyawan:", comboKaryawan,
            "Jenis Ketidakhadiran:", comboStatus,
            "Keterangan: Data akan dicatat untuk Hari & Jam Saat Ini"
        };
        
        int option = JOptionPane.showConfirmDialog(this, message, "Input Ijin / Sakit", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                // Ambil ID Karyawan dari String "101 - Budi" -> Ambil "101"
                String selected = (String) comboKaryawan.getSelectedItem();
                if (selected == null) return;
                int idKaryawan = Integer.parseInt(selected.split(" - ")[0]);
                String status = (String) comboStatus.getSelectedItem();
                
                Connection conn = KoneksiDb.connect();
                // Insert Manual (Waktu masuk & pulang disamakan saat ini agar tidak null)
                String sql = "INSERT INTO log_absensi (karyawan_id, waktu_masuk, waktu_pulang, status_kehadiran, terlambat_menit) " +
                             "VALUES (?, NOW(), NOW(), ?, 0)";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setInt(1, idKaryawan);
                pst.setString(2, status);
                pst.executeUpdate();
                conn.close();
                
                JOptionPane.showMessageDialog(this, "Data " + status + " Berhasil Disimpan!");
                loadData();
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal: " + e.getMessage());
            }
        }
    }

    // --- UPDATE STATUS ---
    private void tampilkanFormEdit(int idLog, String statusLama) {
        JTextField fieldStatus = new JTextField(statusLama);
        Object[] message = {"Edit Status Kehadiran:", fieldStatus};
        
        int option = JOptionPane.showConfirmDialog(this, message, "Koreksi Status", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                Connection conn = KoneksiDb.connect();
                String sql = "UPDATE log_absensi SET status_kehadiran = ? WHERE id = ?";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setString(1, fieldStatus.getText());
                pst.setInt(2, idLog);
                pst.executeUpdate();
                conn.close();
                loadData();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal Update: " + e.getMessage());
            }
        }
    }
    
    // --- DELETE LOG ---
    private void hapusLog(int idLog) {
        try {
            Connection conn = KoneksiDb.connect();
            String sql = "DELETE FROM log_absensi WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, idLog);
            pst.executeUpdate();
            conn.close();
            loadData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal Hapus: " + e.getMessage());
        }
    }

    // Helper: Mengisi ComboBox dengan nama karyawan dari DB
    private void loadKaryawanKeCombo(JComboBox<String> combo) {
        try {
            Connection conn = KoneksiDb.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, nama FROM karyawan ORDER BY nama ASC");
            while(rs.next()) {
                combo.addItem(rs.getInt("id") + " - " + rs.getString("nama"));
            }
            conn.close();
        } catch (Exception e) {}
    }
}