package routing.contextAware.SocialCharcteristic;

import java.util.HashMap;
import java.util.Map;
import core.DTNHost;
import core.Settings;
import core.SimClock;
import java.util.Iterator;


public class Popularity {

    private Settings settings;
    private static final int NUM_TH = 50; // Ambang batas default untuk jumlah node yang ditemui
    private static final double TIME_WINDOW = 200.0; // Window waktu untuk perhitungan dalam detik

    //Menyiman Nilai Alpha
    private double alphaPopularity;

    private Map<DTNHost, Integer> nodeEncounters; // Menyimpan jumlah node yang ditemui oleh setiap node
    private Map<DTNHost, Double> popularityValues; // Menyimpan nilai popularitas untuk setiap node
    private Map<DTNHost, Map<Double, Integer>> encounterTimestamps; // Menyimpan pertemuan dengan timestamp


//    public Popularity() {
//        System.out.println("Create");
//        // Jika alpha tidak ditemukan dalam settings, gunakan nilai default
//    }

    public Popularity(double alphaPopularity) {
        this.alphaPopularity=alphaPopularity;
        nodeEncounters = new HashMap<>();
        popularityValues = new HashMap<>();
        encounterTimestamps = new HashMap<>();
    }

    /**
     * Mencatat pertemuan antara dua node dan menyimpannya dalam periode waktu tertentu.
     * @param nodeA Node pertama yang terlibat dalam pertemuan
     * @param nodeB Node kedua yang terlibat dalam pertemuan
     */
    public void recordEncounter(DTNHost nodeA, DTNHost nodeB) {
        double currentTime = SimClock.getTime();

        // Perbarui encounter nodeA
        nodeEncounters.put(nodeA, nodeEncounters.getOrDefault(nodeA, 0) + 1);
        encounterTimestamps.putIfAbsent(nodeA, new HashMap<>());
        encounterTimestamps.get(nodeA).put(currentTime, nodeEncounters.get(nodeA));

        // Perbarui encounter nodeB
        nodeEncounters.put(nodeB, nodeEncounters.getOrDefault(nodeB, 0) + 1);
        encounterTimestamps.putIfAbsent(nodeB, new HashMap<>());
        encounterTimestamps.get(nodeB).put(currentTime, nodeEncounters.get(nodeB));

        // Bersihkan pertemuan lama yang tidak dalam 200 detik terakhir
        cleanOldEncounters();
    }

    /**
     * Membersihkan entri encounter yang lebih lama dari TIME_WINDOW (200 detik).
     */
    private void cleanOldEncounters() {
        double currentTime = SimClock.getTime();

        for (DTNHost node : encounterTimestamps.keySet()) {
            Iterator<Map.Entry<Double, Integer>> it = encounterTimestamps.get(node).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Double, Integer> entry = it.next();
                if (currentTime - entry.getKey() > TIME_WINDOW) {
                    it.remove(); // Hapus entri lama
                }
            }

            // Perbarui jumlah encounter yang masih valid (valid dalam rentang waktu)
            int validEncounters = encounterTimestamps.get(node).size();
            nodeEncounters.put(node, validEncounters);
        }
    }

    /**
     * Menghitung nilai popularitas untuk node berdasarkan pertemuan dalam 200 detik terakhir.
     * @param node Node yang popularitasnya dihitung
     * @return Nilai popularitas node
     */
    public double calculatePopularity(DTNHost node) {
        int encounters = nodeEncounters.getOrDefault(node, 0);
        return Math.min((double) encounters / NUM_TH, 1.0);
    }

    /**
     * Memperbarui nilai popularitas setiap node berdasarkan periode waktu.
     *
     */
    public void updatePopularity() {
//        double alpha = settings.getDouble("alpha");

        for (DTNHost node : nodeEncounters.keySet()) {
            double currentPopularity = calculatePopularity(node);
            double previousPopularity = popularityValues.getOrDefault(node, 0.0);
            double updatedPopularity = (1 - alphaPopularity) * previousPopularity + alphaPopularity * currentPopularity;

            // Pastikan popularitas tetap dalam rentang [0,1]
            updatedPopularity = Math.max(0, Math.min(updatedPopularity, 1));
            popularityValues.put(node, updatedPopularity);
        }
    }

    /**
     * Mendapatkan popularitas untuk node tertentu.
     * @param node Node yang ingin diambil popularitasnya
     * @return Popularitas node
     */
    public double getPopularity(DTNHost node) {
        return popularityValues.getOrDefault(node, 0.0);
    }

    /**
     * Mendapatkan daftar semua popularitas node yang tercatat.
     * @return Map yang berisi popularitas untuk setiap node
     */
    public Map<DTNHost, Double> getAllPopularity() {
        return popularityValues;
    }
}
