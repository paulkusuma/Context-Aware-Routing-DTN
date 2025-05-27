package reinforcementLearning_ContextAware;

import core.DTNHost;
import core.SimClock;
import routing.contextAware.ContextAwareRLRouter;
import routing.contextAware.ENS.ConnectionDuration;
import routing.contextAware.ENS.EncounteredNodeSet;

import java.util.*;

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
    private static final double AGING_CONSTANT = 0.9998;
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
     * @param state   ID tujuan yang sedang dipertimbangkan dalam pembaruan Q-value
     * @param action  ID node yang ditemui, yang akan menjadi aksi yang diambil
     * @param fuzzOpp Nilai evaluasi fuzzy untuk transfer opportunity
     */
    public void updateFirstStrategy(DTNHost host, DTNHost neighbor, String state, String action, double fuzzOpp) {
        String hostId =String.valueOf(host.getAddress());
        String neighborId = String.valueOf(neighbor.getAddress());
        // Mengambil nilai Q untuk pasangan state-Action saat ini
        double qCurrent = qtable.getQvalue(state, action);

        // Mendapatkan nilai Q maksimum dari himpunan
        String destinationId = state.split(",")[1];
        EncounteredNodeSet ensNeighbor=((ContextAwareRLRouter) neighbor.getRouter()).getEncounteredNodeSet();

        // Menghitung reward dengan ENS neighbor dan destinationId
        double reward = calculateReward(ensNeighbor, destinationId);

        // maxQ untuk state milik neighbor (Qm(d,y))
        String stateNeighbor = neighborId +","+ destinationId;
//        System.out.println("[QTABLE DEBUG] maxQ lookup state = " + stateNeighbor);
        // Memakai state dari perspektif neighbor
        double maxQ = qtable.getMaxQvalueForEncounteredNodes(stateNeighbor, ensNeighbor);

        // Pembaruan Q-Table First Strategy
        double newQ = ALPHA * (reward + GAMMA * fuzzOpp * maxQ) + (1 - ALPHA) * qCurrent;

//        // logging DEBUG
//        System.out.println("=== [Q-UPDATE] Strategy 1 ===");
//        System.out.println("Current Host (c)   : " + hostId);
//        System.out.println("Neighbor Node (m)  : " + neighborId);
//        System.out.println("Destination (d)    : " + destinationId);
//        System.out.println("State (c,d)        : " + state);
//        System.out.println("Action (m)         : " + action);
//        System.out.printf("Reward             : %.2f%n", reward);
//        System.out.printf("TOpp               : %.2f%n", fuzzOpp);
//        System.out.println("ENS(m)             : " + ensNeighbor.getAllNodeIds());
//        System.out.println("--- Q(m,d,y) values:");
//        for (String y : ensNeighbor.getAllNodeIds()) {
//            double q = qtable.getQvalue(stateNeighbor, y);
//            System.out.printf("  Q(%s, %s) = %.4f%n", stateNeighbor, y, q);
//        }
//        System.out.printf("MaxQ (m,d,y)       : %.4f%n", maxQ);
//        System.out.printf("Old Q(c,d,m)       : %.4f%n", qCurrent);
//        System.out.printf("New Q(c,d,m)       : %.4f%n", newQ);
//        System.out.println("============================");

        // Menyimpan kembali nilai Q yang telah di perbarui
        qtable.updateQValue(state, action, newQ);
//        System.out.println("[QTABLE UPDATE] state=" + state + ", action=" + action + ", Q=" + newQ);

//        System.out.printf(
//                "[UPDATE DETAIL] Host: %s | Dest: %s | Action: %s | Reward: %.1f | TOpp: %.2f | maxQ: %.4f | OldQ: %.4f | NewQ: %.4f%n",
//                host.getAddress(), state.split(",")[1], action, reward, fuzzOpp, maxQ, qCurrent, newQ
//        );
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
        String action = String.valueOf(neighbor.getAddress());
        // Ambil informasi koneksi terakhir ke neighbor dari ConnectionDuration
        ConnectionDuration connection = ConnectionDuration.getConnection(host, neighbor);

        // Jika belum pernah terhubung atau koneksi masih aktif, skip aging
        if (connection == null || connection.getEndTime() == -1) {
            return false;
        }

        // Hitung waktu sejak koneksi terakhir terputus
        double elapsedTime = SimClock.getTime() - connection.getEndTime();
        // Jika waktu belum cukup untuk dilakukan aging, skip
        if (elapsedTime < MIN_ELAPSED_FOR_AGING) {
            return false;
        }

        // Ambil semua state dari Q-table (state berupa pasangan "hostId,destId")
        Set<String> allStates = qtable.getAllStates();
        boolean updated = false;
        // Untuk setiap state, cek apakah action (neighbor) pernah digunakan di state tersebut
        for (String state : allStates) {
            if (!qtable.hasAction(state, action)) {
                continue; // Jika tidak ada, skip
            }
            // Ambil Q-value lama untuk kombinasi state-action ini
            double currentQ = qtable.getQvalue(state, action);
            // Hitung faktor decay eksponensial berdasarkan waktu yang telah lewat
            double decayFactor = Math.pow(AGING_CONSTANT, elapsedTime);
            double agedQ = Math.max(currentQ * decayFactor, MIN_Q); // Hitung nilai Q baru setelah decay

            // Update nilai Q dalam Q-table
            qtable.updateQValue(state, action, agedQ);
//            System.out.printf("[AGING] State: %s | Action: %s | OldQ: %.4f → NewQ: %.4f | Elapsed: %.2f sec | DecayFactor: %.4f%n",
//                    state, action, currentQ, agedQ, elapsedTime, decayFactor);
            updated = true;
        }
        return updated;
    }
    /**
     * Update thirdStrategy
     * Strategi sinkronisasi dua arah antar Q-table.
     * Tujuan: memastikan kedua node (sender dan receiver) saling berbagi informasi
     * Q-value untuk mempercepat pembelajaran. Nilai Q disebarkan jika lebih tinggi,
     * dan digunakan untuk memperbarui entri yang lebih lemah.
     *
     * @param senderQtable   Q-table milik node pengirim
     * @param receiverQtable Q-table milik node penerima
     */
    public static void updateThirdStrategy(Qtable senderQtable, Qtable receiverQtable) {
        syncQEntries(senderQtable, receiverQtable);
        syncQEntries(receiverQtable, senderQtable);
    }

    /**
     * Melakukan sinkronisasi satu arah dari source ke target Q-table.
     * Untuk setiap pasangan (state:action) di source:
     * - Jika target belum memiliki entry tersebut → langsung salin dari source
     * - Jika sudah ada → bandingkan nilai Q dan simpan yang lebih besar
     *   (untuk menyebarkan informasi terbaik).
     * - Jika nilai target lebih besar → update balik nilai Q di source
     *
     * @param target Q-table yang akan diperbarui
     * @param source Q-table yang menjadi sumber data
     */
    private static void syncQEntries(Qtable target, Qtable source) {
        for (Map.Entry<String, Double> entry : source.getAllEntries()) {
            String key = entry.getKey();
            double sourceQ = entry.getValue();

            String[] parts = key.split(":");
            if (parts.length != 2) continue;

            String state = parts[0];
            String action = parts[1];

            if (!target.hasAction(state, action)) {
                // Jika target belum punya, langsung salin
                target.updateQValue(state, action, sourceQ);
            } else {
                double targetQ = target.getQvalue(state, action);
                if (targetQ < sourceQ) {
                    // Jika nilai target lebih kecil, perbarui dengan nilai lebih tinggi dari source
                    target.updateQValue(state, action, sourceQ);
                } else {
                    // Jika nilai target lebih tinggi, balikan ke source (biar dua arah sinkron)
                    source.updateQValue(state, action, targetQ);
                }
            }
        }
    }
}

//    public static void updateQTables(Qtable senderQ, Qtable receiverQ) {
//        Map<String, Double> maxQPerDestAction = new HashMap<>();
//
//        // Gabungkan semua entry dari kedua Q-table
//        for (Map.Entry<String, Double> entry : senderQ.getAllEntries()) {
//            String key = entry.getKey();
//            double qValue = entry.getValue();
//
//            String[] parts = key.split(":");
//            if (parts.length != 2) continue;
//            String state = parts[0]; // "S,D"
//            String action = parts[1];
//            String destination = getDestinationFromState(state);
//
//            String pairKey = destination + ":" + action;
//            maxQPerDestAction.put(pairKey, Math.max(maxQPerDestAction.getOrDefault(pairKey, 0.0), qValue));
//        }
//
//        for (Map.Entry<String, Double> entry : receiverQ.getAllEntries()) {
//            String key = entry.getKey();
//            double qValue = entry.getValue();
//
//            String[] parts = key.split(":");
//            if (parts.length != 2) continue;
//            String state = parts[0]; // "S,D"
//            String action = parts[1];
//            String destination = getDestinationFromState(state);
//
//            String pairKey = destination + ":" + action;
//            maxQPerDestAction.put(pairKey, Math.max(maxQPerDestAction.getOrDefault(pairKey, 0.0), qValue));
//        }
//
//        // Update sender Q-table
//        for (Map.Entry<String, Double> entry : senderQ.getAllEntries()) {
//            String key = entry.getKey();
//            String[] parts = key.split(":");
//            if (parts.length != 2) continue;
//            String state = parts[0];
//            String action = parts[1];
//            String destination = getDestinationFromState(state);
//
//            String pairKey = destination + ":" + action;
//            if (maxQPerDestAction.containsKey(pairKey)) {
//                double maxQ = maxQPerDestAction.get(pairKey);
//                senderQ.updateQValue(state, action, maxQ);
//            }
//        }
//
//        // Update receiver Q-table
//        for (Map.Entry<String, Double> entry : receiverQ.getAllEntries()) {
//            String key = entry.getKey();
//            String[] parts = key.split(":");
//            if (parts.length != 2) continue;
//            String state = parts[0];
//            String action = parts[1];
//            String destination = getDestinationFromState(state);
//
//            String pairKey = destination + ":" + action;
//            if (maxQPerDestAction.containsKey(pairKey)) {
//                double maxQ = maxQPerDestAction.get(pairKey);
//                receiverQ.updateQValue(state, action, maxQ);
//            }
//        }
//
//        // Logging (optional)
//        for (Map.Entry<String, Double> e : maxQPerDestAction.entrySet()) {
//            String[] parts = e.getKey().split(":");
//            System.out.printf("[Q-UPDATE STRAT3] Dest: %s | Action: %s → NewQ: %.3f\n",
//                    parts[0], parts[1], e.getValue());
//        }
//    }
//
//    private static String getDestinationFromState(String state) {
//        // Misal state = "S,D" → kembalikan "D"
//        String[] parts = state.split(",");
//        return (parts.length == 2) ? parts[1] : "";
//    }

//    /**
//     * Update thirdStrategy
//     * Strategi sinkronisasi dua arah antar Q-table.
//     * Tujuan: memastikan kedua node (sender dan receiver) saling berbagi informasi
//     * Q-value untuk mempercepat pembelajaran. Nilai Q disebarkan jika lebih tinggi,
//     * dan digunakan untuk memperbarui entri yang lebih lemah.
//     *
//     * @param senderQtable   Q-table milik node pengirim
//     * @param receiverQtable Q-table milik node penerima
//     */
//    public static void updateThirdStrategy(Qtable senderQtable, Qtable receiverQtable) {
//        syncQEntries(senderQtable, receiverQtable);
//        syncQEntries(receiverQtable, senderQtable);
//    }
//
//    /**
//     * Melakukan sinkronisasi satu arah dari source ke target Q-table.
//     * Untuk setiap pasangan (state:action) di source:
//     * - Jika target belum memiliki entry tersebut → langsung salin dari source
//     * - Jika sudah ada → bandingkan nilai Q dan simpan yang lebih besar
//     *   (untuk menyebarkan informasi terbaik).
//     * - Jika nilai target lebih besar → update balik nilai Q di source
//     *
//     * @param target Q-table yang akan diperbarui
//     * @param source Q-table yang menjadi sumber data
//     */
//    private static void syncQEntries(Qtable target, Qtable source) {
//        for (Map.Entry<String, Double> entry : source.getAllEntries()) {
//            String key = entry.getKey();
//            double sourceQ = entry.getValue();
//
//            String[] parts = key.split(":");
//            if (parts.length != 2) continue;
//
//            String state = parts[0];
//            String action = parts[1];
//
//            if (!target.hasAction(state, action)) {
//                // Jika target belum punya, langsung salin
//                target.updateQValue(state, action, sourceQ);
//            } else {
//                double targetQ = target.getQvalue(state, action);
//                if (targetQ < sourceQ) {
//                    // Jika nilai target lebih kecil, perbarui dengan nilai lebih tinggi dari source
//                    target.updateQValue(state, action, sourceQ);
//                } else {
//                    // Jika nilai target lebih tinggi, balikan ke source (biar dua arah sinkron)
//                    source.updateQValue(state, action, targetQ);
//                }
//            }
//        }
//    }


