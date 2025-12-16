package com.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class DashboardAdmin extends JFrame {

    public DashboardAdmin() {
        setTitle("ADMINISTRATOR PANEL - Sistem Absensi");
        setSize(900, 600); // Ukuran diperbesar sedikit
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout()); // Ganti jadi BorderLayout biar lebih fleksibel

        // --- HEADER ---
        JLabel labelJudul = new JLabel("PUSAT KONTROL ADMIN", SwingConstants.CENTER);
        labelJudul.setFont(new Font("Arial", Font.BOLD, 28));
        labelJudul.setOpaque(true);
        labelJudul.setBackground(new Color(44, 62, 80)); // Dark Blue
        labelJudul.setForeground(Color.WHITE);
        labelJudul.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        add(labelJudul, BorderLayout.NORTH);

        // --- PANEL TOMBOL (GRID 2x2) ---
        JPanel panelTombol = new JPanel();
        // Kita ubah jadi 2 Baris, 2 Kolom (Total 4 Tombol)
        panelTombol.setLayout(new GridLayout(3, 2, 20, 20)); 
        panelTombol.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        // 1. Tombol Tambah Karyawan
        JButton btnRegister = createButton("TAMBAH KARYAWAN", new Color(39, 174, 96));
        btnRegister.addActionListener(e -> new Thread(() -> RegisterWajah.run()).start());

        // 2. Tombol Manajemen Data (CRUD)
        JButton btnManajemen = createButton("MANAJEMEN DATA", new Color(52, 152, 219));
        btnManajemen.addActionListener(e -> new Thread(() -> new ManajemenKaryawan()).start());
        
        // 3. Tombol Download Laporan
        JButton btnLaporan = createButton("DOWNLOAD LAPORAN", new Color(243, 156, 18));
        btnLaporan.addActionListener(e -> new Thread(() -> LaporanExcel.exportLaporan()).start());

        // 4. TOMBOL BARU: RESET LOG (Testing)
        JButton btnReset = createButton("RESET / HAPUS LOG ABSENSI", new Color(192, 57, 43)); // Merah Gelap
        btnReset.addActionListener(e -> hapusSemuaLog());

         // 5. TOMBOL BARU: Manajemen Absensi (Ijin/Sakit)
        JButton btnAbsensi = createButton("MANAJEMEN ABSENSI (Ijin/Sakit)", new Color(142, 68, 173)); // Ungu
        btnAbsensi.addActionListener(e -> new Thread(() -> new ManajemenAbsensi()).start());
        
        // Masukkan ke Panel
        panelTombol.add(btnRegister);
        panelTombol.add(btnManajemen);
        panelTombol.add(btnAbsensi);
        panelTombol.add(btnLaporan);
        panelTombol.add(btnReset); // Tombol baru
        
        add(panelTombol, BorderLayout.CENTER);

        // --- FOOTER ---
        JLabel labelFooter = new JLabel("Mode Developer: Tombol Reset Log Aktif", SwingConstants.CENTER);
        labelFooter.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(labelFooter, BorderLayout.SOUTH);
    }
    
    // Fungsi Helper untuk Desain Tombol
    private JButton createButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 18));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        return btn;
    }

    // --- FUNGSI HAPUS LOG (LOGIKA UTAMA) ---
    private void hapusSemuaLog() {
        // 1. Konfirmasi Keamanan (Biar gak kepencet)
        int confirm = JOptionPane.showConfirmDialog(this, 
                "PERINGATAN KERAS!\n\n" +
                "Anda akan menghapus SEMUA RIWAYAT ABSENSI di database.\n" +
                "Data yang dihapus TIDAK BISA kembali.\n\n" +
                "Apakah Anda yakin ingin melakukan RESET untuk testing?", 
                "Reset Database", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Connection conn = KoneksiDb.connect();
                // SQL TRUNCATE: Menghapus isi tabel dan mereset ID auto-increment kembali ke 1
                String sql = "TRUNCATE TABLE log_absensi"; 
                // Jika TRUNCATE gagal karena Foreign Key, gunakan: "DELETE FROM log_absensi"
                
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.executeUpdate();
                conn.close();
                
                JOptionPane.showMessageDialog(this, "SUKSES!\nSemua Log Absensi telah dibersihkan.\nSilakan lakukan testing ulang.");
                
            } catch (Exception e) {
                // Jika error (biasanya karena constraint), coba pakai DELETE biasa
                try {
                    Connection conn2 = KoneksiDb.connect();
                    String sql2 = "DELETE FROM log_absensi";
                    PreparedStatement pst2 = conn2.prepareStatement(sql2);
                    pst2.executeUpdate();
                    conn2.close();
                    JOptionPane.showMessageDialog(this, "SUKSES!\nData Log dihapus (Mode DELETE).");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Gagal Reset: " + ex.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DashboardAdmin().setVisible(true));
    }
}