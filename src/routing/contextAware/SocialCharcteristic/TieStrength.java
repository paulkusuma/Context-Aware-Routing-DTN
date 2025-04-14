package routing.contextAware.SocialCharcteristic;


import java.util.*;
import core.DTNHost;
import core.SimClock;
import routing.contextAware.ENS.ConnectionDuration;
import routing.contextAware.ENS.EncounteredNode;
import routing.contextAware.ENS.EncounteredNodeSet;

public class TieStrength {

    private static final double RECENCY_FACTOR = 0.5; // Faktor untuk memberi bobot pada recency
    private static final double FREQUENCY_WEIGHT = 0.3; // Bobot untuk encounter frequency
    private static final double CLOSENESS_WEIGHT = 0.1; // Bobot untuk closeness


    /**
     * Menghitung Frequency (Frekuensi encounter) antara dua node.
     *
     * @param nodeA              Node pertama
     * @param nodeB              Node kedua
     * @param encounteredNodeSet Instansi dari kelas EncounteredNodeSet untuk melacak encounter history
     * @return Frekuensi encounter antara nodeA dan nodeB
     */
    public static int calculateFrequency(DTNHost nodeA, DTNHost nodeB, EncounteredNodeSet encounteredNodeSet) {
        return encounteredNodeSet.getEncounterCount(nodeA, nodeB);  // Mengambil jumlah encounter

    }

    /**
     * Menghitung Closeness (Durasi koneksi) antara dua node.
     * @param nodeA Node pertama
     * @param nodeB Node kedua
     * @param connectionDurationInst Instansi dari kelas ConnectionDuration untuk menghitung durasi koneksi
     * @return Durasi koneksi (Closeness) antara nodeA dan nodeB
     */
    public static double calculateCloseness(DTNHost nodeA, DTNHost nodeB, ConnectionDuration connectionDurationInst) {
        return ConnectionDuration.getTotalConnectionDuration(nodeA, nodeB);  // Mengambil total durasi koneksi
    }

    /**
     * Menghitung Recency (Kedekatan waktu encounter terakhir) antara dua node.
     * @param nodeA Node pertama
     * @param nodeB Node kedua
     * @param connectionDurationInst Instansi dari kelas ConnectionDuration untuk mendapatkan waktu terakhir koneksi
     * @return Waktu yang telah berlalu sejak encounter terakhir antara nodeA dan nodeB
     */
    public static double calculateRecency(DTNHost nodeA, DTNHost nodeB, ConnectionDuration connectionDurationInst) {
        if (connectionDurationInst != null) {
            // Ambil waktu terakhir encounter dan hitung selisihnya dengan waktu sekarang
            double lastEncounterTime = connectionDurationInst.getConnection(nodeA, nodeB).getEndTime();
            return (SimClock.getTime() - lastEncounterTime);  // Menghitung waktu yang telah berlalu
        }
        return 0.0; // jika tidak ada koneksi
    }

    /**
     * Menghitung TieStrength antara dua node berdasarkan frekuensi encounter, durasi koneksi (closeness),
     * dan recency encounter.
     * @param nodeA Node pertama
     * @param nodeB Node kedua
     * @param encounteredNodeSet Instansi dari kelas EncounteredNodeSet untuk melacak encounter history
     * @param connectionDurationInst Instansi dari kelas ConnectionDuration untuk menghitung durasi koneksi
     * @return Nilai TieStrength antara nodeA dan nodeB
     */
    public static double calculateTieStrength(DTNHost nodeA, DTNHost nodeB,
                                              EncounteredNodeSet encounteredNodeSet,
                                              ConnectionDuration connectionDurationInst) {

        // 1. Menghitung frekuensi encounter
        int frequency = calculateFrequency(nodeA, nodeB, encounteredNodeSet);
        System.out.println("[TieStrength] Frequency antara " + nodeA + " dan " + nodeB + ": " + frequency);

        // 2. Menghitung kedekatan berdasarkan durasi koneksi (Closeness)
        double closeness = calculateCloseness(nodeA, nodeB, connectionDurationInst);
        System.out.println("[TieStrength] Closeness (durasi koneksi) antara " + nodeA + " dan " + nodeB + ": " + closeness);

        // 3. Menghitung recency (waktu yang telah berlalu sejak encounter terakhir)
        double recency = calculateRecency(nodeA, nodeB, connectionDurationInst);
        System.out.println("[TieStrength] Recency (selisih waktu sekarang - encounter terakhir) antara " + nodeA + " dan " + nodeB + ": " + recency);

        // Menambahkan faktor recency untuk meningkatkan TieStrength jika encounter baru terjadi
        double recencyFactor = Math.max(0, 1 - recency / 1000);  // Faktor recency, lebih kecil semakin baru encounter
        System.out.println("[TieStrength] RecencyFactor untuk " + nodeA + " dan " + nodeB + ": " + recencyFactor);

        // 4. Menghitung TieStrength berdasarkan bobot
        double tieStrength = (CLOSENESS_WEIGHT * closeness) +
                (FREQUENCY_WEIGHT * frequency);
        System.out.println("[TieStrength] Base TieStrength (tanpa recency factor): " + tieStrength);


        tieStrength *= (1 + RECENCY_FACTOR * recencyFactor); // Menerapkan bobot recency
        System.out.println("[TieStrength] Final TieStrength antara " + nodeA + " dan " + nodeB + ": " + tieStrength);
        return tieStrength;
    }

}
//    /**
//     * Menghitung Tie Strength antara dua node berdasarkan encounter dan durasi koneksi.
//     *
//     * @param host Node pertama
//     * @param neighbor Node kedua
//     * @param ens EncounteredNodeSet yang berisi informasi encounter
//     * @return Nilai Tie Strength antara node1 dan node2
//     */
//    public double calculateTieStrength(DTNHost host, DTNHost neighbor, EncounteredNodeSet ens) {
//        String myId = String.valueOf(host.getAddress());
//        String neighborId = String.valueOf(neighbor.getAddress());
//
//        // Ambil encounter data untuk kedua node
//        EncounteredNode encounteredNode1 = ens.getENS(myId);
//        EncounteredNode encounteredNode2 = ens.getENS(neighborId);
//
//        // Jika salah satu node tidak ditemukan, return 0 (tidak ada hubungan)
//        if (encounteredNode1 == null || encounteredNode2 == null) {
//            return 0.0;
//        }
//
//        // Menghitung encounter frequency dalam waktu tertentu (misalnya 200 detik terakhir)
//        double frequency = calculateEncounterFrequency(encounteredNode1, encounteredNode2);
//
//        // Menghitung recency berdasarkan encounter terakhir antara kedua node
//        double recency = calculateRecency(encounteredNode1, encounteredNode2);
//
//        // Menghitung closeness (berdasarkan encounter frequency)
//        double closeness = calculateCloseness(frequency);
//
//        // Kombinasikan faktor-faktor di atas untuk menghitung Tie Strength
//        double tieStrength = FREQUENCY_WEIGHT * frequency + RECENCY_FACTOR * recency + CLOSENESS_WEIGHT * closeness;
//
//        // Pastikan Tie Strength berada di antara 0 dan 1
//        return Math.min(1.0, Math.max(0.0, tieStrength));
//    }
//
//    /**
//     * Menghitung frekuensi encounter antara dua node dalam interval waktu tertentu.
//     *
//     * @param node1 Node pertama
//     * @param node2 Node kedua
//     * @return Nilai frekuensi encounter antara kedua node
//     */
//    private double calculateEncounterFrequency(EncounteredNode node1, EncounteredNode node2) {
//        // Misalnya, menghitung jumlah encounter dalam 200 detik terakhir
//        double encounterCount = 0;
//        double currentTime = SimClock.getTime();
//
//        for (Encounter encounter : node1.getEncounters()) {
//            if (encounter.getNodeId() == node2.getNodeId() &&
//                    (currentTime - encounter.getTime()) <= 200.0) {
//                encounterCount++;
//            }
//        }
//        return encounterCount;
//    }
//
//    /**
//     * Menghitung recency berdasarkan encounter terakhir antara dua node.
//     *
//     * @param node1 Node pertama
//     * @param node2 Node kedua
//     * @return Nilai recency
//     */
//    private double calculateRecency(EncounteredNode node1, EncounteredNode node2) {
//        double currentTime = SimClock.getTime();
//        double lastEncounterTime = node1.getLastEncounterTime(node2);
//
//        // Jika encounter terlalu lama, recency menjadi rendah
//        double recency = (currentTime - lastEncounterTime) / 200.0; // Anggap 200 detik sebagai batas waktu
//        return Math.max(0.0, 1.0 - recency); // Pastikan recency berada di antara 0 dan 1
//    }
//
//    /**
//     * Menghitung closeness antara dua node berdasarkan encounter frequency.
//     *
//     * @param frequency Frekuensi encounter antara dua node
//     * @return Nilai closeness
//     */
//    private double calculateCloseness(double frequency) {
//        // Menghitung closeness berdasarkan encounter frequency
//        // Semakin sering bertemu, semakin besar closeness
//        return Math.min(1.0, frequency / 10.0); // Normalisasi antara 0 dan 1
//    }
//}
//
//    /**
//     * Konstruktor untuk inisialisasi struktur data
//     */
//    public TieStrength() {
//        this.encounterCounts = new HashMap<>();
//        this.lastEncounterTime = new HashMap<>();
//    }
//
//    /**
//     * Mencatat pertemuan antara dua node dan memperbarui waktu terakhir mereka bertemu.
//     * @param nodeA Node pertama yang terlibat dalam pertemuan
//     * @param nodeB Node kedua yang terlibat dalam pertemuan
//     */
//    public void recordEncounter(DTNHost nodeA, DTNHost nodeB) {
//        double currentTime = SimClock.getTime();
//
//        // Inisialisasi struktur data jika belum ada entri untuk node yang bersangkutan
//        encounterCounts.putIfAbsent(nodeA, new HashMap<>());
//        encounterCounts.putIfAbsent(nodeB, new HashMap<>());
//        lastEncounterTime.putIfAbsent(nodeA, new HashMap<>());
//        lastEncounterTime.putIfAbsent(nodeB, new HashMap<>());
//
//        // Menambahkan frekuensi pertemuan untuk kedua node
//        encounterCounts.get(nodeA).put(nodeB, encounterCounts.get(nodeA).getOrDefault(nodeB, 0) + 1);
//        encounterCounts.get(nodeB).put(nodeA, encounterCounts.get(nodeB).getOrDefault(nodeA, 0) + 1);
//
//        // Memperbarui waktu terakhir pertemuan antara kedua node
//        lastEncounterTime.get(nodeA).put(nodeB, currentTime);
//        lastEncounterTime.get(nodeB).put(nodeA, currentTime);
//    }
//
//    /**
//     * Menghitung Tie Strength antara dua node berdasarkan frekuensi dan peluruhan waktu.
//     * @param nodeA Node pertama yang dihitung Tie Strength-nya
//     * @param nodeB Node kedua yang dihitung Tie Strength-nya
//     * @return Nilai Tie Strength antara dua node
//     */
//    public double getTieStrength(DTNHost nodeA, DTNHost nodeB) {
//        // Jika tidak ada data pertemuan antara nodeA dan nodeB, kembalikan 0
//        if (!encounterCounts.containsKey(nodeA) || !encounterCounts.get(nodeA).containsKey(nodeB)) {
//            return 0.0;
//        }
//
//        // Mengambil jumlah pertemuan antara kedua node
//        int frequency = encounterCounts.get(nodeA).get(nodeB);
//
//        // Mengambil waktu terakhir mereka bertemu
//        double lastTime = lastEncounterTime.get(nodeA).get(nodeB);
//        double currentTime = SimClock.getTime();
//
//        // Time decay: semakin lama tidak bertemu, semakin kecil nilai hubungan
//        double timeDecay = Math.exp(-(currentTime - lastTime) / 10000);
//
//        // Menghitung dan mengembalikan nilai Tie Strength
//        return frequency * timeDecay;
//    }
//}
