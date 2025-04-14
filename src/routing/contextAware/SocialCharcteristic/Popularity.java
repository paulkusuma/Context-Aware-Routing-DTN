package routing.contextAware.SocialCharcteristic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import core.DTNHost;
import core.SimClock;
import routing.contextAware.ENS.*;


public class Popularity {

    // Menyimpan Popularity setiap node
    private static Map<DTNHost, Double> popularityMap = new HashMap<>();
    // Menyimpan encounter history untuk setiap node
    private static Map<DTNHost, Map<DTNHost, Double>> encounterHistory = new HashMap<>();
    // Threshold untuk menghitung encounter dalam 200 detik
    private static final int NUMth = 50;
    // Interval waktu (200 detik)
    private static final double TIME_WINDOW = 200.0;

    // Menyimpan nilai alpha yang diatur melalui konstruktor
    private double alphaPopularity;


    /**
     * Konstruktor untuk menginisialisasi Popularity dengan nilai alpha.
     *
     * @param alphaPopularity Nilai alpha untuk pembaruan Popularity.
     */
    public Popularity(double alphaPopularity) {
        this.alphaPopularity = alphaPopularity;
    }

    public void updatePopularity(DTNHost node, EncounteredNodeSet ens) {
        double currentTime = SimClock.getTime();
        double currentPopularity = popularityMap.getOrDefault(node, 0.0);
        int encounterCount = ens.countRecentEncounters(currentTime, TIME_WINDOW);

        // Normalisasi popularitas baru
        double normalizedPopularity = Math.min((double) encounterCount / NUMth, 1.0);

        double updatedPopularity = (1 - alphaPopularity) * currentPopularity + alphaPopularity * normalizedPopularity;

        popularityMap.put(node, updatedPopularity);

        System.out.println("[POPULARITY] Node " + node.getAddress() +
                " updated Popularity: " + updatedPopularity +
                " | Recent encounters: " + encounterCount);
    }

//    /**
//     * Fungsi untuk memperbarui Popularity node berdasarkan encounter yang terjadi.
//     * @param node Node yang sedang diperbarui Popularity-nya.
//     * @param encounteredNode Node yang terdeteksi sebagai encounter.
//     */
//    public void updatePopularity(DTNHost node, DTNHost encounteredNode) {
//        // Mengambil waktu saat ini
//        double currentTime = SimClock.getTime();
//
//        // Catat encounter pada node jika belum ada
//        encounterHistory.putIfAbsent(node, new HashMap<>());
//        Map<DTNHost, Double> nodeHistory = encounterHistory.get(node);
//
//        // Catat waktu encounter antara node dan encounteredNode
//        nodeHistory.put(encounteredNode, currentTime);
//
//        // Hitung jumlah encounter yang terjadi dalam waktu 200 detik
//        int encounterCount = 0;
//        for (Map.Entry<DTNHost, Double> entry : nodeHistory.entrySet()) {
//            double timeDifference = currentTime - entry.getValue();
//            if (timeDifference <= TIME_WINDOW) {
//                encounterCount++;
//            }
//        }
//
//        // Mendapatkan popularitas saat ini untuk node
//        double currentPopularity = popularityMap.getOrDefault(node, 0.0);
//
//        // Menghitung popularitas baru berdasarkan encounterCount
//        double newPopularity = Math.min(encounterCount, NUMth);
//
//        // Persamaan pembaruan: Popularity_t(i) = (1 - alpha) * Popularity_(t-1)(i) + Popularity_t(i)
//        double updatedPopularity = (1 - alphaPopularity) * currentPopularity + newPopularity;
//
//        // Memperbarui popularitas untuk node tersebut
//        popularityMap.put(node, updatedPopularity);
//
//        // Debugging
//        System.out.println("Updated Popularity for node " + node.getAddress() + ": " + updatedPopularity);
//    }

    /**
     * Mendapatkan nilai Popularity untuk sebuah node.
     * @param node Node yang ingin diperiksa Popularity-nya.
     * @return Popularity dari node tersebut.
     */
    public double getPopularity(DTNHost node) {
        return popularityMap.getOrDefault(node, 0.0);
    }

    /**
     * Fungsi untuk menghapus node yang tidak aktif (lebih dari 200 detik).
     * @param node Node yang akan dibersihkan dari encounter history.
     */
    public void cleanOldEncounters(DTNHost node) {
        if (encounterHistory.containsKey(node)) {
            double currentTime = SimClock.getTime();
            Map<DTNHost, Double> nodeHistory = encounterHistory.get(node);
            nodeHistory.entrySet().removeIf(entry -> currentTime - entry.getValue() > TIME_WINDOW);
        }
    }

    /**
     * Menampilkan Popularity untuk setiap node.
     */
    public void printPopularity() {
        for (Map.Entry<DTNHost, Double> entry : popularityMap.entrySet()) {
            System.out.println("Node " + entry.getKey().getAddress() + " has Popularity: " + entry.getValue());
        }
    }
}
//    //Menyiman Nilai Alpha
//    private double alphaPopularity;
//    private EncounteredNodeSet ens;
//    private double currentPopularity;
//
//    public Popularity(double alphaPopularity, EncounteredNodeSet ens) {
//        this.alphaPopularity = alphaPopularity;
//        this.ens = ens;
//        currentPopularity = 0.0;
//    }
//
//    public Popularity(Popularity other) {
//        this.alphaPopularity = other.alphaPopularity;
//        this.currentPopularity = other.currentPopularity;
//        this.ens = other.ens;
//    }
//
//    public void updatePopularity(double currentTime) {
//        int numEncountered = ens.countEncountersInLastPeriod(currentTime, TIME_WINDOW);
//
//
////        System.out.println("[DEBUG] updatePopularity() Called at time: " + currentTime);
////        System.out.println("[DEBUG] TIME_WINDOW = " + TIME_WINDOW + " detik");
////        System.out.println("[DEBUG] Alpha = " + alphaPopularity);
////        System.out.println("[DEBUG] Daftar Encounter yang dicek:");
//        for (EncounteredNode node : ens.getTable().values()) {
//            double age = currentTime - node.getEncounterTime();
//            System.out.printf("  - NodeID: %s | EncounterTime: %.2f | Age: %.2f %s\n",
//                    node.getNodeId(), node.getEncounterTime(), age,
//                    (age <= TIME_WINDOW ? "<= WINDOW ✅" : "> WINDOW ❌"));
//        }
//        double popularityT = Math.min(1.0, (double) numEncountered / NUM_TH);
//        this.currentPopularity = (1 - alphaPopularity) * this.currentPopularity + alphaPopularity * popularityT;
////
////        System.out.println("[DEBUG] Hasil Update Popularity:");
////        System.out.println("  → Jumlah Encounter dalam Window = " + numEncountered);
////        System.out.println("  → PopularityT = " + popularityT);
////        System.out.println("  → Final Popularity = " + currentPopularity);
//    }
//
//    public double getPopularity() {
//        return currentPopularity;
//    }
//
//    public void reset() {
//        currentPopularity = 0.0;
//    }
//
//}
//    private Map<Integer, Double> popularityValues; // Menyimpan nilai popularitas

//    public Popularity(double alphaPopularity, EncounteredNodeSet ens) {
//
//        System.out.println("✅ [DEBUG] Popularity Constructor DIPANGGIL");
//
//        if (ens == null) {
//            throw new IllegalArgumentException("❌ [ERROR] EncounteredNodeSet is NULL!");
//        }
//
//        System.out.println("Popularity Constructor: alpha = " + alphaPopularity);
//
//        this.alphaPopularity = alphaPopularity;
//        this.ens = ens;
//        this.popularityValues = new HashMap<>(); // ✅ INISIALISASI HashMap
//
//        System.out.println("ENS Size: " + ens.getEncounters().size());
//
//        if (ens.getEncounters().isEmpty()) {
//            System.out.println("❌ [ERROR] ENS Kosong saat Konstruktor!");
//        }
//
//        // ✅ Inisialisasi nilai awal semua node
//        for (Integer nodeId : ens.getEncounters().keySet()) {
//            popularityValues.put(nodeId, 0.0);
//        }
//        System.out.println("Popularity successfully initialized with " + popularityValues.size() + " nodes.");
//
//    }
//
//
//    /**
//     * Menghitung nilai popularitas berdasarkan jumlah node yang ditemui dalam 200 detik terakhir.
//     * @param nodeId Node yang popularitasnya dihitung
//     * @return Nilai popularitas node
//     */
//    public double calculatePopularity(int nodeId) {
//        int recentEncounters = ens.getRecentEncounterCount(nodeId); // Ambil encounter dari ENS
//        // 🔥 DEBUGGING OUTPUT
//        System.out.println("📊 [DEBUG] Node " + nodeId +
//                " | Recent Encounters: " + recentEncounters +
//                " | Popularity Before Update: " + popularityValues.getOrDefault(nodeId, 0.0));
//
//
//        double popularity = Math.min((double) recentEncounters / NUM_TH, 1.0);
//        // 🔥 Debugging Output
//        System.out.println("calculatePopularity | Node: " + nodeId +
//                " | Recent Encounters: " + recentEncounters +
//                " | Calculated Popularity: " + popularity);
//        return popularity;
//    }
//
//    /**
//     * Memperbarui nilai popularitas setiap node.
//     */
//    public void updatePopularity() {
//
//        if (ens.getEncounters().isEmpty()) {
//            System.out.println("⚠️ [WARNING] ENS masih kosong, updatePopularity() ditunda.");
//            return;
//        }
//        if (popularityValues.isEmpty()) {
//            System.out.println("🔄 [DEBUG] Inisialisasi Popularity karena HashMap masih kosong.");
//            for (Integer nodeId : ens.getEncounters().keySet()) {
//                popularityValues.put(nodeId, 0.0);
//            }
//            System.out.println("✅ [DEBUG] Popularity berhasil diisi dengan " + popularityValues.size() + " nodes.");
//        }
//
//        System.out.println("🛠 [DEBUG] updatePopularity() DIPANGGIL!");
//
//        for (Integer nodeId : popularityValues.keySet()) {
//            double currentPopularity = calculatePopularity(nodeId);
//            double previousPopularity = popularityValues.getOrDefault(nodeId, 0.0);
//            double updatedPopularity = (1 - alphaPopularity) * previousPopularity + alphaPopularity * currentPopularity;
//            popularityValues.put(nodeId, Math.max(0, Math.min(updatedPopularity, 1))); // Batasi ke [0,1]
//
//
//            System.out.println("Node " + nodeId + " | Previous Popularity: " + previousPopularity +
//                    " | Updated Popularity: " + updatedPopularity);
//        }
//    }
//
//    /**
//     * Mengambil popularitas node.
//     * @param nodeId Node target
//     * @return Popularitas node
//     */
//    public double getPopularity(int nodeId) {
//        return popularityValues.getOrDefault(nodeId, 0.0);
//    }
//
//    /**
//     * Mendapatkan daftar semua popularitas node yang tercatat.
//     * @return Map yang berisi popularitas untuk setiap node
//     */
//    public Map<Integer, Double> getAllPopularity() {
//        return popularityValues;
//    }

