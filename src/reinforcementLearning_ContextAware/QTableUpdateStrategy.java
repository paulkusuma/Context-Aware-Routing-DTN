package reinforcementLearning_ContextAware;

import core.DTNHost;
import core.SimClock;
import routing.contextAware.ContextAwareRLRouter;
import routing.contextAware.ENS.ConnectionDuration;
import routing.contextAware.ENS.EncounteredNodeSet;

import java.util.*;

import static reinforcementLearning_ContextAware.Qtable.printQTable;

/**
 * QTableUpdateStrategy adalah kelas yang mengatur pembaruan Q-table berdasarkan algoritma Q-learning.
 */
public class QTableUpdateStrategy {

    // Objek Qtable yang berfungsi untuk menyimpan dan memperbarui Q-values
    private Qtable qtable;
    // Faktor diskonto, untuk mempertimbangkan nilai masa depan dalam pembaruan
    private static final double GAMMA = 0.4;
    // Laju pembelajaran (learning rate), untuk mengontrol seberapa besar pembaruan nilai Q
    private static final double ALPHA = 0.6;
    // Konstanta peluruhan nilai Q (semakin mendekati 1, semakin lambat decay)
    private static final double AGING_CONSTANT = 0.998;
    // Minimum detik sejak koneksi putus agar aging dijalankan
    public static final double MIN_ELAPSED_FOR_AGING = 240.0;
    // Batas bawah nilai Q agar tidak terlalu kecil
    private static final double MIN_Q = 0.05; // batas bawah Q-value


    /**
     * Konstruktor QTableUpdateStrategy.
     *
     * @param qtable Objek Qtable yang digunakan untuk mengelola Q-values
     */
    public QTableUpdateStrategy(Qtable qtable) {
        this.qtable = qtable;
    }

    /**
     * Menghitung reward berdasarkan Encountered Node Set (ENS) milik *tetangga (neighbor)*.
     * Reward diberikan jika node tujuan (destinationId) dari suatu state
     * ditemukan di ENS milik tetangga. Ini merepresentasikan bahwa neighbor
     * memiliki informasi dengan node tujuan, sehingga layak dipertimbangkan sebagai relay.
     * reward diberikan sebagai:
     * 1.0 → jika destinationId ada dalam ENS milik neighbor
     * 0.0 → jika tidak ada
     *
     * @param ensNeighbor ENS milik neighbor
     * @param destinationId ID node tujuan pesan (dari state "hostId,destinationId")
     * @return Reward: 1.0 jika destination ada di ENS neighbor, 0.0 jika tidak
     */
    public double calculateReward(EncounteredNodeSet ensNeighbor, String destinationId) {
//        Set<String> ensIds = ensNeighbor.getAllNodeIds();
//        System.out.printf("[DEBUG] Cek reward: dst=%s | ENS Neighbor=%s%n", destinationId, ensIds);

        // Destinasi ada di ENS neighbor
        if (ensNeighbor.getAllNodeIds().contains(destinationId)) {
            return 1.0;
        } else {
            return 0.0;
        }
    }


    /**
     * Strategi Pembaruan pertama untuk Q-Value berdasarkan koneksi UP.
     * Pembaruan Q-value dilakukan dengan rumus:
     * <p>
     * Q_c(d,m) ← α × [R(d,m) + γ × Fuzz(TOpp) × max(Q_m(d,y)@y ∈ Nm)] + (1 - α) × Q_c(d,m)
     * State = kombinasi host dan destinationId
     * Action = ID neighbor yang sedang terkoneksi
     * Reward dihitung dari ENS milik neighbor
     * maxQ dihitung dari Q-value terhadap node-node dalam ENS milik host
     * @param host    DTNHost yang digunakan untuk mengambil ENS
     *  tujuan yang sedang dipertimbangkan dalam pembaruan Q-value
     *   ID node yang ditemui, yang akan menjadi aksi yang diambil
     * @param fuzzOpp Nilai evaluasi fuzzy untuk transfer opportunity
     */
    public void updateFirstStrategy(DTNHost host, DTNHost neighbor, String destinationId, String nextHop, double fuzzOpp) {
        String hostId =String.valueOf(host.getAddress());
        String neighborId = String.valueOf(neighbor.getAddress());
        // Mengambil nilai Q untuk pasangan state-Action saat ini
        double qCurrent = qtable.getQvalue(destinationId, nextHop);
        EncounteredNodeSet ensNeighbor=((ContextAwareRLRouter) neighbor.getRouter()).getEncounteredNodeSet();
        // Menghitung reward dengan ENS neighbor dan destinationId
        double reward = calculateReward(ensNeighbor, destinationId);
        // Memakai state dari perspektif neighbor
        double maxQ = qtable.getMaxQvalue(destinationId, ensNeighbor);
        // Pembaruan Q-Table First Strategy
        double newQ = ALPHA * (reward + GAMMA * fuzzOpp * maxQ) + (1 - ALPHA) * qCurrent;
//        // Debug sebelum update
//        System.out.printf("[DEBUG] Sebelum update: dst=%s | via=%s | Q=%.4f | reward=%.2f | maxQ=%.4f | fuzzOpp=%.4f%n",
//                destinationId, nextHop, qCurrent, reward, maxQ, fuzzOpp);

        // Menyimpan kembali nilai Q yang telah di perbarui
        qtable.updateQvalue(destinationId, nextHop, newQ);
//        // Debug sesudah update
//        System.out.printf("[Q-UPDATE] host=%s → dst=%s via %s | R=%.2f, maxQ=%.2f, Q=%.4f → %.4f%n",
//                hostId, destinationId, nextHop, reward, maxQ, qCurrent, newQ);
//
//        // Debug Qtable setelah update
//        System.out.println("[DEBUG] Q-Table saat ini:");
//        qtable.debugPrintQTable(hostId);
    }

    /**
     * Menjalankan proses penurunan Q-value (aging) untuk tetangga (neighbor) yang sudah tidak terhubung
     * selama durasi tertentu (delay-based aging).
     * Method ini dieksekusi secara periodik dari `update()` router, dan akan memeriksa semua entri
     * dalam `pendingAging`, yaitu tetangga yang koneksinya telah terputus.
     * Jika waktu sejak koneksi terputus (elapsedTime) melebihi ambang batas (MIN_ELAPSED_FOR_AGING),
     * maka nilai Q untuk pasangan (state, action) akan dikurangi secara eksponensial.
     *
     * @param host Node saat ini (DTNHost) yang menjalankan router
     * @param pendingAging Map<String, Double> berisi {neighborId → waktu koneksi terakhir terputus}
     */
    public void processDelayedAging(DTNHost host, java.util.Map<String, Double> pendingAging){
        double now = SimClock.getTime();
        List<String> expired =new ArrayList<>(); // Untuk mencatat neighborId yang aging-nya sudah diproses
        // Salin semua key agar tidak kena ConcurrentModification saat remove()
        Set<String> keysSnapshot = new java.util.HashSet<>(pendingAging.keySet());

        // Loop untuk setiap neighbor yang pernah putus koneksi
        for (String neighborId : keysSnapshot) {
            double endTime = pendingAging.get(neighborId); // Waktu koneksi terakhir putus
            double elapsed = now - endTime; // Hitung berapa lama sejak koneksi putus

            // Cek apakah sudah cukup lama sejak putus koneksi
            if(elapsed >= MIN_ELAPSED_FOR_AGING){
                // Gunakan ConnectionDuration untuk cari neighbor berdasarkan host
                List<ConnectionDuration> connections = ConnectionDuration.getConnectionsFromHost(host);
                // Cari connection yang cocok dengan neighborId
                for(ConnectionDuration cd : connections){
                    if(String.valueOf(cd.getToNode().getAddress()).equals(neighborId)){
                        // Jalankan strategi 2 jika ketemu koneksi yang cocok
                        if(updateSecondStrategy(host, cd.getToNode())){
                            expired.add(neighborId); // Tandai untuk dihapus dari pending
                        }
                        break; // Tidak perlu cek koneksi lain
                    }
                }
            }
            // Hapus neighbor yang sudah selesai aging-nya dari pending
            for (String id : expired){
                pendingAging.remove(id);
            }
        }
    }

    /**
     * Strategi Pembaharuan kedua melakukan penurunan (decay) terhadap nilai Q jika koneksi ke tetangga
     * telah terputus dan tidak diperbarui selama waktu tertentu.
     * Penurunan dilakukan dengan rumus eksponensial:
     *     Q' = Q × (AGING_CONSTANT) ^ (elapsedTime)
     * Penurunan eksponensial memastikan bahwa semakin lama koneksi terputus, nilai Q turun lebih cepat di awal, lalu melambat seiring waktu.
     * @param host Node utama (DTNHost) pemilik Q-table
     * @param neighbor Node tetangga yang sebelumnya terkoneksi
     * @return true jika ada nilai Q yang diupdate, false jika tidak memenuhi syarat (belum waktunya atau tidak ada entry)
     */
    public boolean updateSecondStrategy(DTNHost host, DTNHost neighbor) {
        String nexthop = String.valueOf(neighbor.getAddress());
        // Ambil informasi koneksi terakhir ke neighbor dari ConnectionDuration
        ConnectionDuration connection = ConnectionDuration.getConnection(host, neighbor);
        // Jika belum pernah terhubung atau koneksi masih aktif, skip aging
        if (connection == null || connection.getEndTime() == -1) {
//            System.out.printf("[AGING-SKIP] Tidak ada koneksi sebelumnya atau koneksi masih aktif → host=%s, neighbor=%s%n",
//                    host.getAddress(), neighbor.getAddress());
            return false;
        }
        // Hitung waktu sejak koneksi terakhir terputus
        double elapsedTime = SimClock.getTime() - connection.getEndTime();
        // Jika waktu belum cukup untuk dilakukan aging, skip
        if (elapsedTime < MIN_ELAPSED_FOR_AGING) {
//            System.out.printf("[AGING-SKIP] Belum cukup waktu sejak koneksi putus (%.2f < %.2f) → host=%s, neighbor=%s%n",
//                    elapsedTime, MIN_ELAPSED_FOR_AGING, host.getAddress(), neighbor.getAddress());
            return false;
        }
        // Akses langsung semua entry tujuan
        Map<String, Map<String, Double>> allQ = qtable.getAllQvalues();
        boolean updated = false;
//        System.out.printf("[AGING-START] host=%s | neighbor=%s | elapsedTime=%.2f | AGING_CONSTANT=%.4f%n",
//                host.getAddress(), neighbor.getAddress(), elapsedTime, AGING_CONSTANT);

        // Untuk setiap state, cek apakah action (neighbor) pernah digunakan di state tersebut
        for (Map.Entry<String, Map<String, Double>> entry : allQ.entrySet()) {
            String destinationId = entry.getKey();
            Map<String, Double> actionMap  = entry.getValue();
            if (!actionMap.containsKey(nexthop)) {
                continue; // Jika tidak ada, skip
            }
            double currentQ = actionMap.get(nexthop);
            // Hitung faktor decay eksponensial berdasarkan waktu yang telah lewat
            double decayFactor = Math.pow(AGING_CONSTANT, elapsedTime);
            double agedQ = Math.max(currentQ * decayFactor, MIN_Q); // Hitung nilai Q baru setelah decay
//            System.out.printf("[AGING] dst=%s via=%s | Q_old=%.4f → Q_new=%.4f | decayFactor=%.6f%n",
//                    destinationId, nexthop, currentQ, agedQ, decayFactor);
            // Update nilai Q dalam Q-table
            qtable.updateQvalue(destinationId, nexthop, agedQ);
//            System.out.printf("[AGING] State: %s | Action: %s | OldQ: %.4f → NewQ: %.4f | Elapsed: %.2f sec | DecayFactor: %.4f%n",
//                    state, action, currentQ, agedQ, elapsedTime, decayFactor);
            updated = true;
        }
//        if (!updated) {
//            System.out.printf("[AGING-END] Tidak ada Q-value yang diperbarui untuk host=%s terhadap neighbor=%s%n",
//                    host.getAddress(), neighbor.getAddress());
//        }
        return updated;
    }

    /**
     * Melakukan sinkronisasi dua arah antara dua Q-table:
     * - Sinkronisasi dilakukan dari sender → receiver dan receiver → sender.
     * - Tujuan: menyebarkan informasi Q-value terbaik di antara dua node
     *   sehingga pembelajaran antar node menjadi lebih cepat dan stabil.
     * - Prinsip: jika satu node punya nilai Q lebih tinggi, nilainya disalin ke node lain.
     *
     * @param senderQtable   Q-table milik node pengirim
     * @param receiverQtable Q-table milik node penerima
     * @param sender         ID dari node pengirim (untuk debug/log)
     * @param receiver       ID dari node penerima (untuk debug/log)
     */
    public static void updateThirdStrategy(Qtable senderQtable, Qtable receiverQtable, String sender, String receiver) {
//        System.out.printf("\n[DEBUG] === Sebelum Sinkronisasi ===\n");
//        printQTable(senderQtable, sender);
//        printQTable(receiverQtable, receiver);
        syncQEntries(senderQtable, receiverQtable);
        syncQEntries(receiverQtable, senderQtable);
//        System.out.printf("\n[DEBUG] === Setelah Sinkronisasi ===\n");
//        printQTable(senderQtable, sender);
//        printQTable(receiverQtable, receiver);
    }

    /**
     * Sinkronisasi satu arah dari `source` ke `target` Q-table.
     * Tujuan: memastikan `target` menyimpan nilai Q yang optimal dari `source`.
     * Untuk setiap entri (state → nextHop):
     * - Jika entry tidak ada di target → dilewati
     * - Jika ada → bandingkan nilai Q:
     *     - Jika sourceQ lebih tinggi → update target
     *     - Jika targetQ lebih tinggi → update balik ke source
     *
     * @param target Q-table yang akan diperbarui (dituju)
     * @param source Q-table sumber data (asal)
     */
    private static void syncQEntries(Qtable target, Qtable source) {
        for (String state : source.getAllDestinations()) {
            // Lewati jika state = ID pemilik Q-table → tidak perlu mempelajari diri sendiri
            if (state.equals(target.getOwnerId())) continue;
            // Ambil semua aksi (nextHop → Q-value) dari state ini
            Map<String, Double> sourceActions = source.getActionMap(state);
            if (sourceActions == null) continue;

            for (Map.Entry<String, Double> entry : sourceActions.entrySet()) {
                String nextHop = entry.getKey();
                double sourceQ = entry.getValue();

                // Lewati jika nextHop = ID pemilik target → tidak perlu rute ke diri sendiri
                if (nextHop.equals(target.getOwnerId())) continue;

                // Hanya sinkronisasi jika target sudah punya entry (state, nextHop)
                if (target.hasAction(state, nextHop)) {
                    double targetQ = target.getQvalue(state, nextHop);
                    // Sinkronisasi hanya jika kedua nilai Q tidak nol (bermakna)
                    if(targetQ != 0.0 && sourceQ != 0.0){
                        if (targetQ < sourceQ) {
                            // Target memiliki nilai lebih rendah → perbarui dengan nilai dari source
                            target.updateQvalue(state, nextHop, sourceQ);
//                            System.out.printf("[SYNC] (target updated) %s:%s from %.4f → %.4f\n", state, nextHop, targetQ, sourceQ);
                        } else if (targetQ > sourceQ) {
                            // Source memiliki nilai lebih rendah → perbarui balik dari target
                            source.updateQvalue(state, nextHop, targetQ);
//                            System.out.printf("[SYNC] (source updated) %s:%s from %.4f → %.4f\n", state, nextHop, sourceQ, targetQ);
                        }
                    }
                }
                // Jika target belum punya entri (state, nextHop) → abaikan
            }
        }
    }
}


