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
 * Semua nilai dikonversi ke skala [0-1] sebelum dikombinasikan agar hasil akhir
 * tetap bervariasi dan terkontrol, serta tidak selalu bernilai maksimal.
 */
public class TieStrength {

    // Menyimpan nilai TieStrength setiap pasangan node dalam Map
    private static Map<DTNHost, Map<DTNHost, Double>> tieStrengthMap = new HashMap<>();

    // Bobot untuk masing-masing faktor dalam perhitungan TieStrength
    private static final double RECENCY_FACTOR = 0.3; // Faktor untuk memberi bobot pada recency
    private static final double FREQUENCY_WEIGHT = 0.5; // Bobot untuk encounter frequency
    private static final double CLOSENESS_WEIGHT = 0.2; // Bobot untuk closeness

    /**
     * Menghitung frekuensi encounter (jumlah pertemuan dalam time window tertentu).
     *
     * @param host Node utama
     * @param neighbor Node tetangga
     * @param encounteredNodeSet Dataset encounter node
     * @return Jumlah encounter
     */
    public static int calculateFrequency(DTNHost host, DTNHost neighbor, EncounteredNodeSet encounteredNodeSet) {
        double now = SimClock.getTime();
        double timeWindow = 600.0;
        return encounteredNodeSet.getFrequencyBetween(host, neighbor, now, timeWindow);
    }

    /**
     * Menghitung total durasi koneksi antar dua node.
     *
     * @param host Node utama
     * @param neighbor Node tetangga
     * @return Total waktu koneksi (dalam detik)
     */
    public static double calculateCloseness(DTNHost host, DTNHost neighbor) {
        return ConnectionDuration.getTotalConnectionDuration(host, neighbor);
    }


    /**
     * Menghitung selisih waktu sejak pertemuan terakhir antar dua node.
     *
     * @param host Node utama
     * @param neighbor Node tetangga
     * @return Waktu sejak encounter terakhir, atau MAX_VALUE jika belum pernah bertemu
     */
    public static double calculateRecency(DTNHost host, DTNHost neighbor) {
        ConnectionDuration cd = ConnectionDuration.getConnection(host, neighbor);
        if (cd != null) {
            double lastEncounterTime = cd.getEndTime();
            return (SimClock.getTime() - lastEncounterTime);
        }
        return Double.MAX_VALUE;
    }

    /**
     * Menghitung dan menyimpan nilai TieStrength antara dua node berdasarkan:
     * - Frekuensi encounter (Frequency)
     * - Durasi koneksi (Closeness)
     * - Kedekatan waktu terakhir encounter (Recency)
     * Semua faktor dinormalisasi dan digabung menggunakan bobot tertentu,
     * lalu disimpan di Map tieStrengthMap.
     *
     * @param host Node utama.
     * @param neighbor Node tetangga dari host.
     * @param encounteredNodeSet Objek EncounteredNodeSet untuk menghitung frekuensi encounter.
     */
    public void calculateTieStrength(DTNHost host, DTNHost neighbor,
                                              EncounteredNodeSet encounteredNodeSet) {

        // Hitung frekuensi encounter dan normalisasi
        int frequency = calculateFrequency(host, neighbor, encounteredNodeSet);
        double normFreq = normalize(frequency, 15);
//        System.out.println("[TieStrength] Frequency antara " + host + " dan " + neighbor + ": " + frequency + " Nilai : " +normFreq);

        // Hitung total durasi koneksi dan normalisasi (max ekspektasi: 300 detik)
        double closenessRaw = calculateCloseness(host, neighbor);
        double closeness = closenessRaw/900.0;

//        System.out.println("[TieStrength] Closeness (durasi koneksi) antara " + host + " dan " + neighbor + " : " + closeness );

        // Menghitung recency (waktu yang telah berlalu sejak encounter terakhir)
        double recencyDecay = Math.exp(-calculateRecency(host, neighbor) / 1000.0);
//        System.out.println("[TieStrength] RecencyFactor untuk " + host + " dan " + neighbor + ": " + recencyFactor);

        // 4. Menghitung TieStrength berdasarkan bobot
        double rawScore  =  (FREQUENCY_WEIGHT * normFreq) + (CLOSENESS_WEIGHT * closeness);
        double tieStrength = rawScore * (1 + RECENCY_FACTOR * recencyDecay); // Menerapkan bobot recency

        // Pastikan nilai akhir tetap dalam rentang [0,1]
        tieStrength = Math.min(Math.max(tieStrength, 0.0), 1.0);
        tieStrengthMap.computeIfAbsent(host, k -> new HashMap<>()).put(neighbor, tieStrength);
//        System.out.println("[TieStrength] Final TieStrength "+SimClock.getTime()+" antara " + host.getAddress() + " dan " + neighbor.getAddress() + ": " + tieStrength);

    }



    /**
     * Mendapatkan nilai TieStrength untuk sebuah node jika sudah dihitung sebelumnya.
     *
     * @param host Node pertama
     * @param neighbor Node kedua
     * @return Nilai TieStrength antara host dan neighbor, jika ada, atau 0 jika belum dihitung.
     */
    public double getTieStrength(DTNHost host, DTNHost neighbor) {
        // Cek apakah nilai TieStrength sudah ada dalam Map
        if (tieStrengthMap.containsKey(host) && tieStrengthMap.get(host).containsKey(neighbor)) {
            // Jika sudah ada, kembalikan nilai yang telah disimpan
            return tieStrengthMap.get(host).get(neighbor);
        }
        // Jika belum ada, return 0.0
        return 0.0;
    }

    /**
     * Fungsi pembantu untuk menormalkan nilai ke rentang [0,1]
     *
     * @param value Nilai aktual
     * @param max Nilai maksimum yang diharapkan
     * @return Hasil normalisasi
     */
    private static double normalize(double value, double max) {
        return Math.min(value / max, 1.0); // Membatasi agar tidak lebih dari 1
    }
}
