package com.admin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class KoneksiDb {
    public static Connection connect() {
        Connection conn = null;
        try {
            String url = "jdbc:mysql://localhost:3306/db_absensi";
            String user = "root";
            String password = "DB_PASSWORD"; 
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Koneksi Gagal: " + e.getMessage());
        }
        return conn;
    }
}
