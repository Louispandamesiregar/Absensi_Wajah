package com.absensi;

import java.util.Calendar;

public class Suara {
    
    // Tambahkan fungsi ini di dalam class Suara
    public static void ucapkanPerpisahan(String nama) {
        String kalimat = "Terima kasih " + nama + ", Hati-hati di jalan.";
        
        new Thread(() -> {
            try {
                String command = "powershell.exe -Command \"Add-Type -AssemblyName System.Speech; " +
                                 "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                                 "try { $synth.SelectVoiceByHints('Andika'); } catch { try { $synth.SelectVoiceByHints('Indonesia'); } catch {} }; " +
                                 "$synth.Speak('" + kalimat + "');\"";
                Runtime.getRuntime().exec(command);
            } catch (Exception e) {
                System.out.println("Gagal suara: " + e.getMessage());
            }
        }).start();
    }
    
    public static void ucapkanSalam(String nama) {
        // 1. Tentukan Waktu
        Calendar c = Calendar.getInstance();
        int jam = c.get(Calendar.HOUR_OF_DAY);
        String salam;
        
        if (jam >= 0 && jam < 11) {
            salam = "Selamat Pagi";
        } else if (jam >= 11 && jam < 15) {
            salam = "Selamat Siang";
        } else if (jam >= 15 && jam < 18) {
            salam = "Selamat Sore";
        } else {
            salam = "Selamat Malam";
        }
        
        String kalimat = salam + " " + nama + ", Absensi kamu berhasil.";
        
        // 2. Thread Suara
        new Thread(() -> {
            try {
                // LOGIKA BARU: MEMILIH SUARA INDONESIA
                // Kita tambahkan perintah .SelectVoiceByHints
                String command = "powershell.exe -Command \"Add-Type -AssemblyName System.Speech; " +
                                 "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                                 
                                 // COBA CARI SUARA BERNAMA 'ANDIKA' ATAU 'INDONESIA'
                                 // Jika Andika tidak ada, dia akan pakai default (Bule) agar tidak error
                                 "try { $synth.SelectVoiceByHints('Andika'); } catch { try { $synth.SelectVoiceByHints('Indonesia'); } catch {} }; " +
                                 
                                 "$synth.Speak('" + kalimat + "');\"";
                
                Runtime.getRuntime().exec(command);
                
            } catch (Exception e) {
                System.out.println("Gagal mengeluarkan suara: " + e.getMessage());
            }
        }).start();
    }
}