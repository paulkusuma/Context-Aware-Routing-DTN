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
    private static final double RECENCY_FACTOR = 0.5; // Faktor untuk memberi bobot pada recency
    private static final double FREQUENCY_WEIGHT = 0.3; // Bobot untuk encounter frequency
    private static final double CLOSENESS_WEIGHT = 0.1; // Bobot untuk closeness

    /**
     * Menghitung jumlah encounter (pertemuan) antara node ini dan neighbor-nya.
     *
     * @param neighbor Node yang dihitung frekuensinya.
     * @param encounteredNodeSet Struktur data EncounteredNodeSet untuk menyimpan riwayat encounter.
     * @return Jumlah encounter node tersebut dengan neighbor.
     */
    public static int calculateFrequency(DTNHost host, DTNHost neighbor, EncounteredNodeSet encounteredNodeSet) {
//        return encounteredNodeSet.getEncounterCount(neighbor);  // Mengambil jumlah encounter
        return encounteredNodeSet.getFrequencyBetween(host, neighbor);
    }

    /**
     * Menghitung total durasi koneksi antara dua node (closeness).
     * Ini mencerminkan berapa lama dua node saling terhubung dalam semua encounter.
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
     * Semakin kecil nilai recency, semakin "baru" hubungannya.
     *
     * @param host Node pertama
     * @param neighbor Node kedua
     * @param connectionDurationInst Instansi dari kelas ConnectionDuration untuk mendapatkan waktu terakhir koneksi
     * @return Waktu yang telah berlalu sejak encounter terakhir antara nodeA dan nodeB
     */
    public static double calculateRecency(DTNHost host, DTNHost neighbor, ConnectionDuration connectionDurationInst) {
        if (connectionDurationInst != null) {
            // Ambil data koneksi dua node dari ConnectionDuration
            ConnectionDuration cd = connectionDurationInst.getConnection(host, neighbor);
            if (cd != null) {
                // Hitung berapa detik sejak encounter terakhir
                double lastEncounterTime = cd.getEndTime();
                return (SimClock.getTime() - lastEncounterTime);
            }
        }
        // Jika belum pernah encounter, anggap waktunya sangat lama
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
     * @param connectionDurationInst Objek ConnectionDuration untuk menghitung durasi dan recency.
     */
    public void calculateTieStrength(DTNHost host, DTNHost neighbor,
                                              EncounteredNodeSet encounteredNodeSet,
                                              ConnectionDuration connectionDurationInst) {

        // Hitung frekuensi encounter dan normalisasi (max ekspektasi: 5)
        int frequency = calculateFrequency(host, neighbor, encounteredNodeSet);
        double normFreq = normalize(frequency, 20);
//        System.out.println("[TieStrength] Frequency antara " + host + " dan " + neighbor + ": " + frequency + " Nilai : " +normFreq);

        // Hitung total durasi koneksi dan normalisasi (max ekspektasi: 300 detik)
        double closeness = calculateCloseness(host, neighbor, connectionDurationInst);
        double normCloseness = normalize(closeness, 500.0);
//        System.out.println("[TieStrength] Closeness (durasi koneksi) antara " + host + " dan " + neighbor + ": " + closeness + " Nilai : " +normCloseness);

        // 3. Menghitung recency (waktu yang telah berlalu sejak encounter terakhir)
        double recency = calculateRecency(host, neighbor, connectionDurationInst);
//        System.out.println("[TieStrength] Recency (selisih waktu sekarang - encounter terakhir) antara " + host + " dan " + neighbor + ": " + recency);
        // Menambahkan faktor recency untuk meningkatkan TieStrength jika encounter baru terjadi
        double recencyFactor = Math.exp(-recency / 1000.0);
//        System.out.println("[TieStrength] RecencyFactor untuk " + host + " dan " + neighbor + ": " + recencyFactor);

        // 4. Menghitung TieStrength berdasarkan bobot
        double rawScore  = (CLOSENESS_WEIGHT * normCloseness) +
                (FREQUENCY_WEIGHT * normFreq);
        double tieStrength = rawScore * (1 + RECENCY_FACTOR * recencyFactor); // Menerapkan bobot recency

        // Pastikan nilai akhir tetap dalam rentang [0,1]
        tieStrength = Math.min(Math.max(tieStrength, 0.0), 1.0);
        // Normalisasi ke rentang 0-1
        // Menggunakan statistik dari data yang telah dihitung sebelumnya (misalnya, kita hitung min dan max)
//        double tieStrengthMin = 0.0; // Nilai minimal TieStrength yang mungkin terjadi
//        double tieStrengthMax = 1.0; // Nilai maksimal TieStrength yang mungkin terjadi
        // Normalisasi tieStrength ke rentang 0-1 berdasarkan teori bahwa min dan max nilai tieStrength adalah 0 dan 1
//        tieStrength = Math.min(Math.max(tieStrength, tieStrengthMin), tieStrengthMax);

        tieStrengthMap.computeIfAbsent(host, k -> new HashMap<>()).put(neighbor, tieStrength);
//        System.out.println("[TieStrength] Final TieStrength (setelah normalisasi) antara " + host.getAddress() + " dan " + neighbor.getAddress() + ": " + tieStrength);

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
