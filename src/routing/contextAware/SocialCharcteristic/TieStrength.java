package routing.contextAware.SocialCharcteristic;


import java.util.*;
import core.DTNHost;
import core.SimClock;

public class TieStrength {
    // Menyimpan jumlah pertemuan antara setiap pasangan node
    private Map<DTNHost, Map<DTNHost, Integer>> encounterCounts;

    // Menyimpan waktu terakhir pertemuan antara setiap pasangan node
    private Map<DTNHost, Map<DTNHost, Double>> lastEncounterTime;

    /**
     * Konstruktor untuk inisialisasi struktur data
     */
    public TieStrength() {
        this.encounterCounts = new HashMap<>();
        this.lastEncounterTime = new HashMap<>();
    }

    /**
     * Mencatat pertemuan antara dua node dan memperbarui waktu terakhir mereka bertemu.
     * @param nodeA Node pertama yang terlibat dalam pertemuan
     * @param nodeB Node kedua yang terlibat dalam pertemuan
     */
    public void recordEncounter(DTNHost nodeA, DTNHost nodeB) {
        double currentTime = SimClock.getTime();

        // Inisialisasi struktur data jika belum ada entri untuk node yang bersangkutan
        encounterCounts.putIfAbsent(nodeA, new HashMap<>());
        encounterCounts.putIfAbsent(nodeB, new HashMap<>());
        lastEncounterTime.putIfAbsent(nodeA, new HashMap<>());
        lastEncounterTime.putIfAbsent(nodeB, new HashMap<>());

        // Menambahkan frekuensi pertemuan untuk kedua node
        encounterCounts.get(nodeA).put(nodeB, encounterCounts.get(nodeA).getOrDefault(nodeB, 0) + 1);
        encounterCounts.get(nodeB).put(nodeA, encounterCounts.get(nodeB).getOrDefault(nodeA, 0) + 1);

        // Memperbarui waktu terakhir pertemuan antara kedua node
        lastEncounterTime.get(nodeA).put(nodeB, currentTime);
        lastEncounterTime.get(nodeB).put(nodeA, currentTime);
    }

    /**
     * Menghitung Tie Strength antara dua node berdasarkan frekuensi dan peluruhan waktu.
     * @param nodeA Node pertama yang dihitung Tie Strength-nya
     * @param nodeB Node kedua yang dihitung Tie Strength-nya
     * @return Nilai Tie Strength antara dua node
     */
    public double getTieStrength(DTNHost nodeA, DTNHost nodeB) {
        // Jika tidak ada data pertemuan antara nodeA dan nodeB, kembalikan 0
        if (!encounterCounts.containsKey(nodeA) || !encounterCounts.get(nodeA).containsKey(nodeB)) {
            return 0.0;
        }

        // Mengambil jumlah pertemuan antara kedua node
        int frequency = encounterCounts.get(nodeA).get(nodeB);

        // Mengambil waktu terakhir mereka bertemu
        double lastTime = lastEncounterTime.get(nodeA).get(nodeB);
        double currentTime = SimClock.getTime();

        // Time decay: semakin lama tidak bertemu, semakin kecil nilai hubungan
        double timeDecay = Math.exp(-(currentTime - lastTime) / 10000);

        // Menghitung dan mengembalikan nilai Tie Strength
        return frequency * timeDecay;
    }
}
