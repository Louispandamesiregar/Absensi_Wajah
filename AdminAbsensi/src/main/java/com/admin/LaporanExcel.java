package com.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.JOptionPane;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class LaporanExcel {
    
    public static void exportLaporan() {
        try {
            String folderPath = "C:\\Users\\M S I\\Documents\\Louis\\Laporan_Absensi";
            File folder = new File(folderPath);
            if (!folder.exists()) folder.mkdirs();

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Laporan Absensi");
            
            Row headerRow = sheet.createRow(0);
            
            // --- PERUBAHAN 1: Judul Kolom ---
            // "ID Log" diganti jadi "ID Karyawan" agar sesuai inputan registrasi
            String[] columns = {"ID Karyawan", "Nama Karyawan", "Shift", "Tgl/Jam Masuk", "Tgl/Jam Pulang", "Status", "Telat (Menit)"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            Connection conn = KoneksiDb.connect();
            
            // --- PERUBAHAN 2: Query SQL ---
            // Kita ambil k.id (ID Karyawan) BUKAN log.id (ID Transaksi)
            String sql = "SELECT k.id AS id_karyawan, k.nama, s.nama_shift, log.waktu_masuk, log.waktu_pulang, log.status_kehadiran, log.terlambat_menit " +
                         "FROM log_absensi log " +
                         "JOIN karyawan k ON log.karyawan_id = k.id " +
                         "JOIN shift s ON k.shift_id = s.id " +
                         "ORDER BY log.waktu_masuk DESC";
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            int rowNum = 1; 
            while(rs.next()) {
                Row row = sheet.createRow(rowNum++);
                
                // --- PERUBAHAN 3: Ambil Data ---
                // Mengambil kolom 'id_karyawan'
                row.createCell(0).setCellValue(rs.getInt("id_karyawan"));
                
                row.createCell(1).setCellValue(rs.getString("nama"));
                row.createCell(2).setCellValue(rs.getString("nama_shift"));
                row.createCell(3).setCellValue(rs.getString("waktu_masuk"));
                
                String jamPulang = rs.getString("waktu_pulang");
                if(jamPulang == null) jamPulang = "-";
                row.createCell(4).setCellValue(jamPulang);
                
                row.createCell(5).setCellValue(rs.getString("status_kehadiran"));
                row.createCell(6).setCellValue(rs.getInt("terlambat_menit"));
            }
            
            for(int i=0; i<columns.length; i++) sheet.autoSizeColumn(i);
            
            String namaFile = "Laporan_" + System.currentTimeMillis() + ".xlsx";
            String fullPath = folderPath + File.separator + namaFile;
            
            FileOutputStream fileOut = new FileOutputStream(fullPath);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            conn.close();
            
            JOptionPane.showMessageDialog(null, "Laporan Tersimpan!\n" + fullPath);
            java.awt.Desktop.getDesktop().open(folder);
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Gagal Export: " + e.getMessage());
        }
    }
}