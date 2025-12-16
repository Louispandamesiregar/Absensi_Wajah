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
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ManajemenKaryawan extends JFrame {
    
    DefaultTableModel tableModel;
    JTable table;

    public ManajemenKaryawan() {
        setTitle("Manajemen Data Karyawan");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Jangan matikan app, tutup jendela saja
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- 1. HEADER ---
        JLabel labelJudul = new JLabel("DAFTAR KARYAWAN AKTIF", SwingConstants.CENTER);
        labelJudul.setFont(new Font("Arial", Font.BOLD, 20));
        labelJudul.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(labelJudul, BorderLayout.NORTH);

        // --- 2. TABEL DATA ---
        String[] kolom = {"ID", "Nama Lengkap", "Shift", "Status"};
        tableModel = new DefaultTableModel(kolom, 0) {
            @Override // Agar sel tidak bisa diedit langsung (harus via tombol)
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- 3. PANEL TOMBOL AKSI ---
        JPanel panelTombol = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        
        JButton btnRefresh = new JButton("Refresh Data");
        JButton btnEdit = new JButton("Edit Karyawan");
        JButton btnHapus = new JButton("Hapus Karyawan");
        
        // Styling Tombol
        btnEdit.setBackground(new Color(52, 152, 219)); btnEdit.setForeground(Color.WHITE);
        btnHapus.setBackground(new Color(231, 76, 60)); btnHapus.setForeground(Color.WHITE);
        
        panelTombol.add(btnRefresh);
        panelTombol.add(btnEdit);
        panelTombol.add(btnHapus);
        add(panelTombol, BorderLayout.SOUTH);

        // --- 4. LOGIKA TOMBOL ---
        
        // Tombol Refresh
        btnRefresh.addActionListener(e -> loadData());

        // Tombol Hapus
        btnHapus.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Pilih baris karyawan dulu!");
                return;
            }
            
            int id = (int) tableModel.getValueAt(selectedRow, 0);
            String nama = (String) tableModel.getValueAt(selectedRow, 1);
            
            int confirm = JOptionPane.showConfirmDialog(this, 
                    "Yakin ingin menghapus " + nama + "?\nSemua data absensi dia juga akan terhapus!", 
                    "Hapus Data", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                hapusKaryawan(id);
            }
        });

        // Tombol Edit
        btnEdit.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Pilih baris karyawan dulu!");
                return;
            }
            
            int id = (int) tableModel.getValueAt(selectedRow, 0);
            String namaLama = (String) tableModel.getValueAt(selectedRow, 1);
            // Panggil Form Edit Popup
            tampilkanFormEdit(id, namaLama);
        });

        // Load data saat pertama buka
        loadData();
        setVisible(true);
    }

    // --- FUNGSI LOAD DATA (READ) ---
    private void loadData() {
        tableModel.setRowCount(0); // Bersihkan tabel
        try {
            Connection conn = KoneksiDb.connect();
            // JOIN agar kita dapat Nama Shift, bukan cuma ID Shift
            String sql = "SELECT k.id, k.nama, s.nama_shift FROM karyawan k " +
                         "JOIN shift s ON k.shift_id = s.id ORDER BY k.id ASC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while(rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("nama"),
                    rs.getString("nama_shift"),
                    "Aktif"
                };
                tableModel.addRow(row);
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- FUNGSI HAPUS (DELETE) ---
    private void hapusKaryawan(int id) {
        try {
            Connection conn = KoneksiDb.connect();
            
            // 1. Hapus Log Absensi dulu (Karena Foreign Key)
            String sqlLog = "DELETE FROM log_absensi WHERE karyawan_id = ?";
            PreparedStatement pstLog = conn.prepareStatement(sqlLog);
            pstLog.setInt(1, id);
            pstLog.executeUpdate();
            
            // 2. Baru Hapus Karyawannya
            String sqlKaryawan = "DELETE FROM karyawan WHERE id = ?";
            PreparedStatement pstK = conn.prepareStatement(sqlKaryawan);
            pstK.setInt(1, id);
            pstK.executeUpdate();
            
            conn.close();
            JOptionPane.showMessageDialog(this, "Data berhasil dihapus!");
            loadData(); // Refresh tabel
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal Hapus: " + e.getMessage());
        }
    }

    // --- FUNGSI EDIT (UPDATE) ---
    private void tampilkanFormEdit(int id, String namaLama) {
        JTextField fieldNama = new JTextField(namaLama);
        String[] shifts = {"1 - Shift Pagi", "2 - Shift Siang", "3 - Shift Malam"};
        JComboBox<String> comboShift = new JComboBox<>(shifts);
        
        Object[] message = {
            "ID Karyawan: " + id, // ID tidak bisa diedit
            "Nama Lengkap:", fieldNama,
            "Ganti Shift:", comboShift
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Edit Data Karyawan", JOptionPane.OK_CANCEL_OPTION);
        
        if (option == JOptionPane.OK_OPTION) {
            try {
                Connection conn = KoneksiDb.connect();
                String sql = "UPDATE karyawan SET nama = ?, shift_id = ? WHERE id = ?";
                PreparedStatement pst = conn.prepareStatement(sql);
                
                pst.setString(1, fieldNama.getText());
                pst.setInt(2, comboShift.getSelectedIndex() + 1); // Index 0 jadi ID 1, dst
                pst.setInt(3, id);
                
                pst.executeUpdate();
                conn.close();
                
                JOptionPane.showMessageDialog(this, "Data berhasil diupdate!");
                loadData();
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal Update: " + e.getMessage());
            }
        }
    }
}