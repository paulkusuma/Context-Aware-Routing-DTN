package routing.contextAware.SocialCharcteristic;


import java.util.*;
import core.DTNHost;
import core.SimClock;
import routing.contextAware.ENS.ConnectionDuration;
import routing.contextAware.ENS.EncounteredNode;
import routing.contextAware.ENS.EncounteredNodeSet;

/**
 * Kelas ini digunakan untuk menghitung nilai TieStrength antara dua node berdasarkan tiga faktor utama:
 * 1. Frekuensi encounter (Frequency)
 * 2. Durasi koneksi (Closeness)
 * 3. Recency atau kedekatan waktu encounter terakhir (Recency)
 */
public class TieStrength {

    // Bobot untuk masing-masing faktor dalam perhitungan TieStrength
    private static final double RECENCY_FACTOR = 0.5; // Faktor untuk memberi bobot pada recency
    private static final double FREQUENCY_WEIGHT = 0.3; // Bobot untuk encounter frequency
    private static final double CLOSENESS_WEIGHT = 0.1; // Bobot untuk closeness

    /**
     * Menghitung Frequency (Frekuensi encounter) node neighbor.
     *
     * @param neighbor Node neighbor
     * @param encounteredNodeSet Instansi dari kelas EncounteredNodeSet untuk melacak encounter history
     * @return Frekuensi encounter antara nodeA dan nodeB
     */
    public static int calculateFrequency(DTNHost neighbor, EncounteredNodeSet encounteredNodeSet) {
        return encounteredNodeSet.getEncounterCount(neighbor);  // Mengambil jumlah encounter
    }

    /**
     * Menghitung Closeness (Durasi koneksi) antara dua node.
     * @param host Node pertama
     * @param neighbor Node kedua
     * @param connectionDurationInst Instansi dari kelas ConnectionDuration untuk menghitung durasi koneksi
     * @return Durasi koneksi (Closeness) antara host dan neighbor
     */
    public static double calculateCloseness(DTNHost host, DTNHost neighbor, ConnectionDuration connectionDurationInst) {
        return ConnectionDuration.getTotalConnectionDuration(host, neighbor);  // Mengambil total durasi koneksi
    }

    /**
     * Menghitung Recency (Kedekatan waktu encounter terakhir) antara dua node.
     * @param host Node pertama
     * @param neighbor Node kedua
     * @param connectionDurationInst Instansi dari kelas ConnectionDuration untuk mendapatkan waktu terakhir koneksi
     * @return Waktu yang telah berlalu sejak encounter terakhir antara nodeA dan nodeB
     */
    public static double calculateRecency(DTNHost host, DTNHost neighbor, ConnectionDuration connectionDurationInst) {
        if (connectionDurationInst != null) {
            // Ambil waktu terakhir encounter dan hitung selisihnya dengan waktu sekarang
            double lastEncounterTime = connectionDurationInst.getConnection(host, neighbor).getEndTime();
            return (SimClock.getTime() - lastEncounterTime);  // Menghitung waktu yang telah berlalu
        }
        return 0.0; // jika tidak ada koneksi
    }

    /**
     * Menghitung TieStrength antara dua node berdasarkan frekuensi encounter, durasi koneksi (closeness),
     * dan recency encounter.
     * @param host Node pertama
     * @param neighbor Node neighbor
     * @param encounteredNodeSet Instansi dari kelas EncounteredNodeSet untuk melacak encounter history
     * @param connectionDurationInst Instansi dari kelas ConnectionDuration untuk menghitung durasi koneksi
     * @return Nilai TieStrength antara nodeA dan nodeB
     */
    public static double calculateTieStrength(DTNHost host, DTNHost neighbor,
                                              EncounteredNodeSet encounteredNodeSet,
                                              ConnectionDuration connectionDurationInst) {

        // 1. Menghitung frekuensi encounter
        int frequency = calculateFrequency(neighbor, encounteredNodeSet);
//        System.out.println("[TieStrength] Frequency antara " + host + " dan " + neighbor + ": " + frequency);
        // 2. Menghitung kedekatan berdasarkan durasi koneksi (Closeness)
        double closeness = calculateCloseness(host, neighbor, connectionDurationInst);
//        System.out.println("[TieStrength] Closeness (durasi koneksi) antara " + host + " dan " + neighbor + ": " + closeness);
        // 3. Menghitung recency (waktu yang telah berlalu sejak encounter terakhir)
        double recency = calculateRecency(host, neighbor, connectionDurationInst);
//        System.out.println("[TieStrength] Recency (selisih waktu sekarang - encounter terakhir) antara " + host + " dan " + neighbor + ": " + recency);
        // Menambahkan faktor recency untuk meningkatkan TieStrength jika encounter baru terjadi
        double recencyFactor = Math.exp(-recency / 10000.0);
//        System.out.println("[TieStrength] RecencyFactor untuk " + host + " dan " + neighbor + ": " + recencyFactor);

        // 4. Menghitung TieStrength berdasarkan bobot
        double tieStrength = (CLOSENESS_WEIGHT * closeness) +
                (FREQUENCY_WEIGHT * frequency);
        tieStrength *= (1 + RECENCY_FACTOR * recencyFactor); // Menerapkan bobot recency
        // Normalisasi ke rentang 0-1
        // Menggunakan statistik dari data yang telah dihitung sebelumnya (misalnya, kita hitung min dan max)
        double tieStrengthMin = 0.0; // Nilai minimal TieStrength yang mungkin terjadi
        double tieStrengthMax = 1.0; // Nilai maksimal TieStrength yang mungkin terjadi
        // Normalisasi tieStrength ke rentang 0-1 berdasarkan teori bahwa min dan max nilai tieStrength adalah 0 dan 1
        tieStrength = Math.min(Math.max(tieStrength, tieStrengthMin), tieStrengthMax);

        System.out.println("[TieStrength] Final TieStrength (setelah normalisasi) antara " + host + " dan " + neighbor + ": " + tieStrength);
        return tieStrength;
    }
}
