package com.absensi;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class KoneksiDb {
    public static Connection connect() {
        Connection conn = null;
        try {
            // Ganti 'root' dan '' (kosong) sesuai username & password XAMPP/MySQL Anda
            String url = "jdbc:mysql://localhost:3306/db_absensi";
            String user = "root";
            String password = "siregar123"; 
            
            // Register Driver (Untuk versi baru kadang opsional, tapi bagus untuk memastikan)
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            conn = DriverManager.getConnection(url, user, password);
            // System.out.println("Koneksi Berhasil!"); // Un-comment untuk cek
            
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Koneksi Gagal: " + e.getMessage());
        }
        return conn;
    }
}