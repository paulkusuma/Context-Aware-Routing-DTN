package routing.contextAware.SocialCharcteristic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import routing.contextAware.ENS.*;


public class Popularity {

    private static final int NUM_TH = 50; // Ambang batas default untuk jumlah node yang ditemui
    private static final int TIME_WINDOW = 200; // Time window in seconds


    //Menyiman Nilai Alpha
    private double alphaPopularity;
    private EncounteredNodeSet ens;
    private double currentPopularity;

    public Popularity(double alphaPopularity, EncounteredNodeSet ens) {
        this.alphaPopularity = alphaPopularity;
        this.ens = ens;
        currentPopularity = 0.0;
    }

    public Popularity(Popularity other) {
        this.alphaPopularity = other.alphaPopularity;
        this.currentPopularity = other.currentPopularity;
        this.ens = other.ens;
    }

    public void updatePopularity(double currentTime) {
        int numEncountered = ens.countEncountersInLastPeriod(currentTime, TIME_WINDOW);


//        System.out.println("[DEBUG] updatePopularity() Called at time: " + currentTime);
//        System.out.println("[DEBUG] TIME_WINDOW = " + TIME_WINDOW + " detik");
//        System.out.println("[DEBUG] Alpha = " + alphaPopularity);
//        System.out.println("[DEBUG] Daftar Encounter yang dicek:");
        for (EncounteredNode node : ens.getTable().values()) {
            double age = currentTime - node.getEncounterTime();
            System.out.printf("  - NodeID: %s | EncounterTime: %.2f | Age: %.2f %s\n",
                    node.getNodeId(), node.getEncounterTime(), age,
                    (age <= TIME_WINDOW ? "<= WINDOW ✅" : "> WINDOW ❌"));
        }
        double popularityT = Math.min(1.0, (double) numEncountered / NUM_TH);
        this.currentPopularity = (1 - alphaPopularity) * this.currentPopularity + alphaPopularity * popularityT;
//
//        System.out.println("[DEBUG] Hasil Update Popularity:");
//        System.out.println("  → Jumlah Encounter dalam Window = " + numEncountered);
//        System.out.println("  → PopularityT = " + popularityT);
//        System.out.println("  → Final Popularity = " + currentPopularity);
    }

    public double getPopularity() {
        return currentPopularity;
    }

    public void reset() {
        currentPopularity = 0.0;
    }

}
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

