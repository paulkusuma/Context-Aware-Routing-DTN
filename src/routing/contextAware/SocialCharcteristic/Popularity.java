package routing.contextAware.SocialCharcteristic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import core.DTNHost;
import core.SimClock;
import routing.contextAware.ENS.*;

/**
 * Kelas Popularity digunakan untuk menghitung dan memperbarui tingkat popularitas setiap node
 * dalam jaringan Opportunistic berdasarkan jumlah encounter dalam jendela waktu tertentu.
 * Popularity dihitung menggunakan metode exponential smoothing untuk memberikan bobot lebih
 * pada encounter terbaru dibanding encounter lama.
 *
 * Nilai popularitas berada dalam rentang [0, 1], yang mencerminkan seberapa "aktif" atau
 * "terkenal" sebuah node di jaringan berdasarkan encounter-nya.
 */
public class Popularity {

    // Menyimpan Popularity setiap node
    private static Map<DTNHost, Double> popularityMap = new HashMap<>();
    // Threshold untuk menghitung encounter maksimal untuk normalisasi (digunakan sebagai pembagi)
    private static final int NUMth = 35;
    // Interval waktu (200 detik)
    private static final double TIME_WINDOW = 200.0;

    // Smoothing alpha untuk pembaruan popularitas
    private double alphaPopularity;


    /**
     * Konstruktor untuk menginisialisasi Popularity dengan parameter alpha smoothing.
     *
     * @param alphaPopularity Nilai smoothing factor (α) antara 0.0 dan 1.0.
     *                        Semakin tinggi nilainya, semakin cepat perubahan popularitas mengikuti perubahan encounter.
     */
    public Popularity(double alphaPopularity) {
        this.alphaPopularity = alphaPopularity;
    }

    /**
     * Memperbarui nilai Popularity untuk sebuah node berdasarkan jumlah encounter
     * dalam jendela waktu tertentu.
     *
     * @param node Node yang akan diperbarui nilai popularitasnya.
     * @param ens  EncounteredNodeSet yang berisi riwayat encounter node tersebut.
     */
    public void updatePopularity(DTNHost node, EncounteredNodeSet ens) {
        double currentTime = SimClock.getTime();
        double currentPopularity = popularityMap.getOrDefault(node, 0.0);
        int encounterCount = ens.countRecentEncounters(currentTime, TIME_WINDOW);

        // Normalisasi popularitas baru
        double normalizedPopularity = Math.min((double) encounterCount / NUMth, 1.0);

        // Perbarui popularitas dengan exponential smoothing
        double updatedPopularity = (1 - alphaPopularity) * currentPopularity + alphaPopularity * normalizedPopularity;

        popularityMap.put(node, updatedPopularity);

//        System.out.println("[POPULARITY] Node " + node.getAddress() +
//                " updated Popularity: " + updatedPopularity +
//                " | Recent encounters: " + encounterCount);
    }

    /**
     * Mendapatkan nilai Popularity terkini untuk sebuah node.
     *
     * @param node Node yang ingin diambil nilai popularitasnya.
     * @return Nilai popularitas node dalam rentang [0, 1]. Jika belum ada, akan dikembalikan 0.0.
     */
    public double getPopularity(DTNHost node) {
        return popularityMap.getOrDefault(node, 0.0);
    }

}
