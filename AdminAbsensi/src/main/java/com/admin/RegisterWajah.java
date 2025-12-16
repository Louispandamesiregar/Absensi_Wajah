package com.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.swing.*;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

public class RegisterWajah {
    
    static boolean[] isCaptureClicked = {false};

    public static void run() { // Ganti main jadi run agar bisa dipanggil dashboard
        // Form Input Data
        JTextField fieldID = new JTextField();
        JTextField fieldNama = new JTextField();
        // Ganti Input Jabatan Manual dengan Pilihan SHIFT
        String[] shifts = {"1 - Pagi", "2 - Siang", "3 - Malam"};
        JComboBox<String> comboShift = new JComboBox<>(shifts);
        
        Object[] message = {
            "ID Karyawan (Angka):", fieldID, 
            "Nama Lengkap:", fieldNama, 
            "Pilih Shift:", comboShift
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Register Karyawan Baru", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) return;

        int id = 0;
        try {
            id = Integer.parseInt(fieldID.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "ID Harus Angka!");
            return;
        }
        
        String nama = fieldNama.getText();
        // Ambil ID Shift dari combobox (indeks 0, 1, atau 2 -> tambah 1 biar jadi ID database)
        int shiftId = comboShift.getSelectedIndex() + 1; 

        JFrame frame = new JFrame("Register: " + nama);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(640, 550);
        frame.setLayout(new BorderLayout());
        
        JLabel labelGambar = new JLabel(); 
        labelGambar.setHorizontalAlignment(JLabel.CENTER);
        frame.add(labelGambar, BorderLayout.CENTER);
        
        JButton btnRekam = new JButton("AMBIL FOTO");
        btnRekam.setBackground(Color.RED);
        btnRekam.setForeground(Color.WHITE);
        btnRekam.addActionListener(e -> isCaptureClicked[0] = true);
        frame.add(btnRekam, BorderLayout.SOUTH);
        frame.setVisible(true);

        String xmlPath = "C:\\Users\\M S I\\Documents\\NetBeansProjects\\absen_karyawan\\haarcascade_frontalface_alt.xml";
        CascadeClassifier faceDetector = new CascadeClassifier(xmlPath);
        VideoCapture camera = new VideoCapture(0);

        Mat frameMat = new Mat();
        RectVector faceDetections = new RectVector();
        
        while (frame.isVisible()) {
            if (camera.read(frameMat)) {
                opencv_core.flip(frameMat, frameMat, 1);
                faceDetector.detectMultiScale(frameMat, faceDetections);
                
                for (long i = 0; i < faceDetections.size(); i++) {
                    Rect rect = faceDetections.get(i);
                    opencv_imgproc.rectangle(frameMat, rect, new Scalar(0, 255, 0, 0), 2, 8, 0);
                }

                labelGambar.setIcon(new ImageIcon(matToBufferedImage(frameMat)));

                if (isCaptureClicked[0]) {
                    if (faceDetections.size() > 0) {
                        Rect rect = faceDetections.get(0);
                        Mat wajahOnly = new Mat(frameMat, rect);
                        opencv_imgproc.cvtColor(wajahOnly, wajahOnly, opencv_imgproc.COLOR_BGR2GRAY);
                        opencv_imgproc.resize(wajahOnly, wajahOnly, new Size(160, 160));
                        
                        int jwb = JOptionPane.showConfirmDialog(frame, "Simpan foto ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
                        if (jwb == JOptionPane.YES_OPTION) {
                            simpanKeDB(id, nama, shiftId, wajahOnly); // Update fungsi simpan
                            break;
                        } else {
                            isCaptureClicked[0] = false;
                        }
                    } else {
                        JOptionPane.showMessageDialog(frame, "Wajah tidak terdeteksi!");
                        isCaptureClicked[0] = false;
                    }
                }
            }
        }
        camera.release();
        frame.dispose();
    }
    
    public static void simpanKeDB(int id, String nama, int shiftId, Mat wajah) {
        try {
            Connection conn = KoneksiDb.connect();
            BytePointer buffer = new BytePointer();
            opencv_imgcodecs.imencode(".jpg", wajah, buffer);
            byte[] imageBytes = new byte[(int) buffer.limit()];
            buffer.get(imageBytes);
            
            // Masukkan data termasuk Shift ID
            String sql = "INSERT INTO karyawan (id, nama, shift_id, foto_wajah) VALUES (?, ?, ?, ?)";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, id);
            pst.setString(2, nama);
            pst.setInt(3, shiftId);
            pst.setBytes(4, imageBytes);
            pst.executeUpdate();
            conn.close();
            JOptionPane.showMessageDialog(null, "Karyawan Baru Berhasil Disimpan!");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Gagal: " + e.getMessage());
        }
    }

    public static BufferedImage matToBufferedImage(Mat m) {
        org.bytedeco.javacv.Java2DFrameConverter paintConverter = new org.bytedeco.javacv.Java2DFrameConverter();
        org.bytedeco.javacv.OpenCVFrameConverter.ToMat openCVConverter = new org.bytedeco.javacv.OpenCVFrameConverter.ToMat();
        return paintConverter.getBufferedImage(openCVConverter.convert(m));
    }
}