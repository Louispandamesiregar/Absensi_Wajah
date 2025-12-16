package com.absensi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.nio.IntBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
// Import JavaCV
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

public class Absensi {
    
    static LBPHFaceRecognizer recognizer;
    static Map<Integer, Long> lastScanTime = new HashMap<>();
    
    // Variabel Liveness (Kedipan Mata)
    static int blinkCounter = 0;
    static boolean mataTerbuka = true; // Status awal dianggap terbuka
    static int currentFaceID = -1; // Melacak ID wajah yang sedang tampil
    
    static DefaultTableModel tableModel;
    static JTable tabelLog;

    public static void main(String[] args) {
        // --- SETUP GUI ---
        JFrame frame = new JFrame("Sistem Absensi Pro (Liveness + Auto Close)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 600);
        frame.setLayout(new GridLayout(1, 2)); 

        JPanel panelKiri = new JPanel(new BorderLayout());
        JLabel labelGambar = new JLabel();
        labelGambar.setHorizontalAlignment(JLabel.CENTER);
        panelKiri.add(labelGambar, BorderLayout.CENTER);
        JLabel labelJudulCam = new JLabel("KAMERA (Berkedip untuk Absen)", SwingConstants.CENTER);
        labelJudulCam.setFont(new Font("Arial", Font.BOLD, 18));
        labelJudulCam.setForeground(Color.BLUE);
        panelKiri.add(labelJudulCam, BorderLayout.NORTH);

        JPanel panelKanan = new JPanel(new BorderLayout());
        String[] kolom = {"ID", "Nama", "Jam Masuk", "Jam Pulang", "Status"};
        tableModel = new DefaultTableModel(kolom, 0);
        tabelLog = new JTable(tableModel);
        tabelLog.setRowHeight(30);
        tabelLog.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JTableHeader header = tabelLog.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 14));
        header.setBackground(Color.DARK_GRAY);
        header.setForeground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(tabelLog);
        panelKanan.add(new JLabel("LOG HARIAN", SwingConstants.CENTER), BorderLayout.NORTH);
        panelKanan.add(scrollPane, BorderLayout.CENTER);

        frame.add(panelKiri);
        frame.add(panelKanan);
        frame.setVisible(true);

        // --- LOAD DATABASE ---
        System.out.println("Melatih data wajah...");
        if (!trainModel()) {
            JOptionPane.showMessageDialog(null, "Database Kosong! Register dulu.");
            return;
        }
        refreshTabelHarian(); 

        // --- LOAD XML (WAJAH & MATA) ---
        // GANTI PATH INI SESUAI LOKASI DI LAPTOP ANDA!
        String faceXml = "C:\\Users\\M S I\\Documents\\NetBeansProjects\\absen_karyawan\\haarcascade_frontalface_alt.xml";
        String eyeXml  = "C:\\Users\\M S I\\Documents\\NetBeansProjects\\absen_karyawan\\haarcascade_eye_tree_eyeglasses.xml";
        
        CascadeClassifier faceDetector = new CascadeClassifier(faceXml);
        CascadeClassifier eyeDetector = new CascadeClassifier(eyeXml);
        
        if (eyeDetector.empty()) {
            JOptionPane.showMessageDialog(null, "Error: File XML Mata tidak ditemukan!\nDownload dulu haarcascade_eye_tree_eyeglasses.xml");
        }

        VideoCapture camera = new VideoCapture(0);
        Mat frameMat = new Mat();
        RectVector faceDetections = new RectVector();

        while (frame.isVisible()) {
            if (camera.read(frameMat)) {
                opencv_core.flip(frameMat, frameMat, 1);
                faceDetector.detectMultiScale(frameMat, faceDetections);

                // Reset blink counter jika tidak ada wajah
                if (faceDetections.size() == 0) {
                    blinkCounter = 0;
                    currentFaceID = -1;
                }

                for (long i = 0; i < faceDetections.size(); i++) {
                    Rect rect = faceDetections.get(i);
                    
                    // 1. DETEKSI MATA (LIVENESS CHECK)
                    Mat faceROI = new Mat(frameMat, rect); // Fokus hanya di area wajah
                    RectVector eyes = new RectVector();
                    eyeDetector.detectMultiScale(faceROI, eyes);
                    
                    // Visualisasi Mata (Lingkaran Biru)
                    for (long j = 0; j < eyes.size(); j++) {
                        Rect eye = eyes.get(j);
                        org.bytedeco.opencv.opencv_core.Point center = 
                                new org.bytedeco.opencv.opencv_core.Point(rect.x() + eye.x() + eye.width()/2, rect.y() + eye.y() + eye.height()/2);
                        opencv_imgproc.circle(frameMat, center, 5, new Scalar(255, 0, 0, 0), 2, 8, 0);
                    }

                    // Logika Hitung Kedipan
                    if (eyes.size() > 0) {
                        if (!mataTerbuka) { 
                            // Tadi tertutup, sekarang terbuka -> HITUNG 1 KEDIPAN
                            blinkCounter++;
                            mataTerbuka = true;
                        }
                    } else {
                        // Mata hilang (sedang merem)
                        mataTerbuka = false;
                    }

                    // 2. PENGENALAN WAJAH
                    Mat wajah = new Mat(frameMat, rect);
                    opencv_imgproc.cvtColor(wajah, wajah, opencv_imgproc.COLOR_BGR2GRAY);
                    opencv_imgproc.resize(wajah, wajah, new org.bytedeco.opencv.opencv_core.Size(160, 160));
                    
                    IntPointer label = new IntPointer(1);
                    org.bytedeco.javacpp.DoublePointer confidence = new org.bytedeco.javacpp.DoublePointer(1);
                    recognizer.predict(wajah, label, confidence);
                    
                    int idPrediksi = label.get();
                    double nilaiConf = confidence.get();
                    
                    // Reset counter jika ganti orang
                    if (currentFaceID != idPrediksi) {
                        blinkCounter = 0;
                        currentFaceID = idPrediksi;
                    }

                    // 3. TAMPILKAN STATUS
                    String text = "Unknown";
                    Scalar warna = new Scalar(0, 0, 255, 0); // Merah

                    if (idPrediksi != -1 && nilaiConf < 70) {
                        // WAJAH DIKENALI, TAPI SUDAH KEDIP BELUM?
                        if (blinkCounter >= 1) {
                            text = "ID: " + idPrediksi + " (LIVE)";
                            warna = new Scalar(0, 255, 0, 0); // Hijau
                            
                            // PROSES ABSENSI (Hanya jika Live)
                            long currentTime = System.currentTimeMillis();
                            long lastTime = lastScanTime.getOrDefault(idPrediksi, 0L);
                            
                            if (currentTime - lastTime > 30000) {
                                lastScanTime.put(idPrediksi, currentTime);
                                blinkCounter = 0; // Reset agar harus kedip lagi untuk absen berikutnya
                                
                                new Thread(() -> {
                                    prosesAbsensiDatabase(idPrediksi);
                                    refreshTabelHarian(); 
                                }).start();
                            }
                        } else {
                            // Wajah Kenal, tapi Belum Kedip
                            text = "SILAKAN BERKEDIP!";
                            warna = new Scalar(0, 255, 255, 0); // Kuning
                        }
                    }
                    
                    opencv_imgproc.rectangle(frameMat, rect, warna, 2, 8, 0);
                    opencv_imgproc.putText(frameMat, text, new org.bytedeco.opencv.opencv_core.Point(rect.x(), rect.y()-10),
                            opencv_imgproc.FONT_HERSHEY_PLAIN, 1.3, warna);
                }
                labelGambar.setIcon(new ImageIcon(matToBufferedImage(frameMat)));
            }
        }
        camera.release();
        frame.dispose();
    }
    
    // --- DATABASE LOGIC (SHIFT + AUTO CLOSE + JENDELA WAKTU) ---
    public static void prosesAbsensiDatabase(int id) {
        Connection conn = null;
        try {
            conn = KoneksiDb.connect();

            // [LOGIKA 1] AUTO-CLOSE SESI LAMA (GHOST SESSION)
            // Menutup sesi kemarin yang lupa dipulangin
            String sqlAutoClose = "UPDATE log_absensi SET waktu_pulang = CONCAT(DATE(waktu_masuk), ' 23:59:59'), status_kehadiran = CONCAT(status_kehadiran, ' (Lupa Pulang)') WHERE karyawan_id = ? AND waktu_pulang IS NULL AND DATE(waktu_masuk) < CURDATE()";
            PreparedStatement pstClose = conn.prepareStatement(sqlAutoClose);
            pstClose.setInt(1, id);
            pstClose.executeUpdate();

            // [LOGIKA 2] CEK ABSENSI HARI INI
            String sqlCek = "SELECT * FROM log_absensi WHERE karyawan_id = ? AND DATE(waktu_masuk) = CURDATE()";
            PreparedStatement pstCek = conn.prepareStatement(sqlCek);
            pstCek.setInt(1, id);
            ResultSet rs = pstCek.executeQuery();
            
            String namaKaryawan = getNamaByID(id); 
            
            if (rs.next()) {
                // SUDAH MASUK -> CEK PULANG
                Timestamp jamMasuk = rs.getTimestamp("waktu_masuk");
                Timestamp jamPulang = rs.getTimestamp("waktu_pulang");
                
                if (jamPulang == null) {
                    long durasiKerja = System.currentTimeMillis() - jamMasuk.getTime();
                    // Minimal 1 Menit kerja baru boleh pulang
                    if (durasiKerja > 60000) { 
                        String sqlUpdate = "UPDATE log_absensi SET waktu_pulang = NOW() WHERE id = ?";
                        PreparedStatement pstUp = conn.prepareStatement(sqlUpdate);
                        pstUp.setInt(1, rs.getInt("id"));
                        pstUp.executeUpdate();
                        System.out.println("ID " + id + " Pulang.");
                        Suara.ucapkanPerpisahan(namaKaryawan);
                    }
                }
            } else {
                // BELUM MASUK -> CEK JADWAL SHIFT
                Map<String, String> infoShift = getShiftKaryawan(id);
                String namaShift = infoShift.getOrDefault("nama_shift", "General");
                String strJamMasuk = infoShift.getOrDefault("jam_masuk", "08:00:00");
                
                java.time.LocalTime jamMasukShift = java.time.LocalTime.parse(strJamMasuk);
                java.time.LocalTime jamSekarang = java.time.LocalTime.now();
                
                int menitMasuk = jamMasukShift.getHour() * 60 + jamMasukShift.getMinute();
                int menitSekarang = jamSekarang.getHour() * 60 + jamSekarang.getMinute();
                
                // Fix Lintas Hari (Shift Malam)
                if (menitMasuk > 1000 && menitSekarang < 480) { 
                    menitSekarang += 1440;
                }
                
                long selisihMenit = menitSekarang - menitMasuk;
                
                // Jendela Waktu: Tolak jika > 2 jam sebelum shift
                if (selisihMenit < -120) {
                     System.out.println("Ditolak: Terlalu Cepat.");
                     Suara.ucapkanSalam(namaKaryawan + ", belum saatnya masuk " + namaShift);
                     return; 
                }
                
                String status = (selisihMenit > 0) ? "Terlambat" : "Tepat Waktu";
                long telatDB = (selisihMenit > 0) ? selisihMenit : 0;
                
                String sqlInsert = "INSERT INTO log_absensi (karyawan_id, waktu_masuk, status_kehadiran, terlambat_menit) VALUES (?, NOW(), ?, ?)";
                PreparedStatement pstIn = conn.prepareStatement(sqlInsert);
                pstIn.setInt(1, id);
                pstIn.setString(2, status);
                pstIn.setLong(3, telatDB);
                pstIn.executeUpdate();
                
                System.out.println("ID " + id + " Masuk (" + namaShift + "). Status: " + status);
                
                if (status.equals("Terlambat")) {
                    Suara.ucapkanSalam(namaKaryawan + ", kamu terlambat " + telatDB + " menit");
                } else {
                    Suara.ucapkanSalam(namaKaryawan + ", selamat bekerja");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if(conn!=null) conn.close(); } catch(Exception e){}
        }
    }
    
    // --- HELPER METHODS (TIDAK BERUBAH) ---
    public static Map<String, String> getShiftKaryawan(int idKaryawan) {
        Map<String, String> dataShift = new HashMap<>();
        try {
            Connection conn = KoneksiDb.connect();
            String sql = "SELECT s.nama_shift, s.jam_masuk, s.jam_pulang FROM karyawan k JOIN shift s ON k.shift_id = s.id WHERE k.id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, idKaryawan);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                dataShift.put("nama_shift", rs.getString("nama_shift"));
                dataShift.put("jam_masuk", rs.getString("jam_masuk"));
                dataShift.put("jam_pulang", rs.getString("jam_pulang"));
            }
            conn.close();
        } catch (Exception e) {}
        return dataShift;
    }

    public static void refreshTabelHarian() {
        tableModel.setRowCount(0);
        try {
            Connection conn = KoneksiDb.connect();
            String sql = "SELECT log.waktu_masuk, log.waktu_pulang, k.id, k.nama FROM log_absensi log JOIN karyawan k ON log.karyawan_id = k.id WHERE DATE(log.waktu_masuk) = CURDATE() ORDER BY log.waktu_masuk DESC";
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            SimpleDateFormat jamFormat = new SimpleDateFormat("HH:mm:ss");
            while(rs.next()) {
                String jamMasuk = jamFormat.format(rs.getTimestamp("waktu_masuk"));
                Timestamp tglPulang = rs.getTimestamp("waktu_pulang");
                String jamPulang = (tglPulang == null) ? "-" : jamFormat.format(tglPulang);
                String status = (tglPulang == null) ? "Bekerja" : "Pulang";
                Object[] data = {rs.getInt("id"), rs.getString("nama"), jamMasuk, jamPulang, status};
                tableModel.addRow(data);
            }
            conn.close();
        } catch (Exception e) {}
    }

    public static String getNamaByID(int id) {
        String nama = "Karyawan";
        try {
            Connection conn = KoneksiDb.connect();
            PreparedStatement pst = conn.prepareStatement("SELECT nama FROM karyawan WHERE id=?");
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) nama = rs.getString("nama");
            conn.close();
        } catch (Exception e) {}
        return nama;
    }

    public static boolean trainModel() {
        try {
            Connection conn = KoneksiDb.connect();
            String sql = "SELECT id, foto_wajah FROM karyawan WHERE foto_wajah IS NOT NULL";
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            MatVector images = new MatVector();
            Mat labels = new Mat();
            java.util.List<Integer> labelList = new java.util.ArrayList<>();
            int counter = 0;
            while(rs.next()) {
                byte[] blob = rs.getBytes("foto_wajah");
                int id = rs.getInt("id");
                Mat img = opencv_imgcodecs.imdecode(new Mat(new BytePointer(blob)), opencv_imgcodecs.IMREAD_GRAYSCALE);
                images.push_back(img);
                labelList.add(id);
                counter++;
            }
            if (counter == 0) return false;
            labels = new Mat(labelList.size(), 1, opencv_core.CV_32SC1);
            IntBuffer labelBuf = labels.createBuffer();
            for(int i=0; i<labelList.size(); i++) labelBuf.put(i, labelList.get(i));
            recognizer = LBPHFaceRecognizer.create();
            recognizer.train(images, labels);
            return true;
        } catch (Exception e) { return false; }
    }
    
    public static java.awt.image.BufferedImage matToBufferedImage(Mat m) {
        org.bytedeco.javacv.Java2DFrameConverter paintConverter = new org.bytedeco.javacv.Java2DFrameConverter();
        org.bytedeco.javacv.OpenCVFrameConverter.ToMat openCVConverter = new org.bytedeco.javacv.OpenCVFrameConverter.ToMat();
        return paintConverter.getBufferedImage(openCVConverter.convert(m));
    }
}