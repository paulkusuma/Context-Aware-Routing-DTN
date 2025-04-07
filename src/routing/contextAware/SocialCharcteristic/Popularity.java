package routing.contextAware.SocialCharcteristic;

import java.util.HashMap;
import java.util.Map;

import routing.testContext.EncounteredNodeSet;


public class Popularity {

    private static final int NUM_TH = 50; // Ambang batas default untuk jumlah node yang ditemui
    //Menyiman Nilai Alpha
    private double alphaPopularity;
    private EncounteredNodeSet ens;
    private Map<Integer, Double> popularityValues; // Menyimpan nilai popularitas

    public Popularity(double alphaPopularity, EncounteredNodeSet ens) {

        System.out.println("✅ [DEBUG] Popularity Constructor DIPANGGIL");

        if (ens == null) {
            throw new IllegalArgumentException("❌ [ERROR] EncounteredNodeSet is NULL!");
        }

        System.out.println("Popularity Constructor: alpha = " + alphaPopularity);

        this.alphaPopularity = alphaPopularity;
        this.ens = ens;
        this.popularityValues = new HashMap<>(); // ✅ INISIALISASI HashMap

        System.out.println("ENS Size: " + ens.getEncounters().size());

        if (ens.getEncounters().isEmpty()) {
            System.out.println("❌ [ERROR] ENS Kosong saat Konstruktor!");
        }

        // ✅ Inisialisasi nilai awal semua node
        for (Integer nodeId : ens.getEncounters().keySet()) {
            popularityValues.put(nodeId, 0.0);
        }
        System.out.println("Popularity successfully initialized with " + popularityValues.size() + " nodes.");

    }


    /**
     * Menghitung nilai popularitas berdasarkan jumlah node yang ditemui dalam 200 detik terakhir.
     * @param nodeId Node yang popularitasnya dihitung
     * @return Nilai popularitas node
     */
    public double calculatePopularity(int nodeId) {
        int recentEncounters = ens.getRecentEncounterCount(nodeId); // Ambil encounter dari ENS
        // 🔥 DEBUGGING OUTPUT
        System.out.println("📊 [DEBUG] Node " + nodeId +
                " | Recent Encounters: " + recentEncounters +
                " | Popularity Before Update: " + popularityValues.getOrDefault(nodeId, 0.0));


        double popularity = Math.min((double) recentEncounters / NUM_TH, 1.0);
        // 🔥 Debugging Output
        System.out.println("calculatePopularity | Node: " + nodeId +
                " | Recent Encounters: " + recentEncounters +
                " | Calculated Popularity: " + popularity);
        return popularity;
    }

    /**
     * Memperbarui nilai popularitas setiap node.
     */
    public void updatePopularity() {

        if (ens.getEncounters().isEmpty()) {
            System.out.println("⚠️ [WARNING] ENS masih kosong, updatePopularity() ditunda.");
            return;
        }
        if (popularityValues.isEmpty()) {
            System.out.println("🔄 [DEBUG] Inisialisasi Popularity karena HashMap masih kosong.");
            for (Integer nodeId : ens.getEncounters().keySet()) {
                popularityValues.put(nodeId, 0.0);
            }
            System.out.println("✅ [DEBUG] Popularity berhasil diisi dengan " + popularityValues.size() + " nodes.");
        }

        System.out.println("🛠 [DEBUG] updatePopularity() DIPANGGIL!");

        for (Integer nodeId : popularityValues.keySet()) {
            double currentPopularity = calculatePopularity(nodeId);
            double previousPopularity = popularityValues.getOrDefault(nodeId, 0.0);
            double updatedPopularity = (1 - alphaPopularity) * previousPopularity + alphaPopularity * currentPopularity;
            popularityValues.put(nodeId, Math.max(0, Math.min(updatedPopularity, 1))); // Batasi ke [0,1]


            System.out.println("Node " + nodeId + " | Previous Popularity: " + previousPopularity +
                    " | Updated Popularity: " + updatedPopularity);
        }
    }

    /**
     * Mengambil popularitas node.
     * @param nodeId Node target
     * @return Popularitas node
     */
    public double getPopularity(int nodeId) {
        return popularityValues.getOrDefault(nodeId, 0.0);
    }

    /**
     * Mendapatkan daftar semua popularitas node yang tercatat.
     * @return Map yang berisi popularitas untuk setiap node
     */
    public Map<Integer, Double> getAllPopularity() {
        return popularityValues;
    }
}
