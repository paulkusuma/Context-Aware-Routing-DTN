package reinforcementLearning_ContextAware;

import core.DTNHost;
import core.SimClock;
import routing.contextAware.ContextAwareRLRouter;
import routing.contextAware.ENS.ConnectionDuration;
import routing.contextAware.ENS.EncounteredNodeSet;

import java.util.Map;
import java.util.Set;

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
    // Konstanta peluruhan untuk Update Second Strategy
    private static final double AGING_CONSTANT = 0.95;


    /**
     * Konstruktor QTableUpdateStrategy.
     *
     * @param qtable Objek Qtable yang digunakan untuk mengelola Q-values
     */
    public QTableUpdateStrategy(Qtable qtable){
        this.qtable = qtable;
    }

    /**
     * Menghitung reward berdasarkan Encountered Node Set (ENS) yang dimiliki oleh host.
     * Reward diberikan jika node tujuan ada dalam ENS.
     *
     * @param host DTNHost yang akan diperiksa untuk ENS.
     * @param state berisi host dan ID tujuan yang akan diperiksa apakah ada dalam ENS
     * @return Reward (1 jika destinationId ada dalam ENS, 0 jika tidak)
     */
    public double calculateReward(DTNHost host, String state){
        // Membongkar state untuk mendapatkan destinationId
        String[] stateParts = state.split(",");
        String destinationId = stateParts[1];  // Mengambil ID tujuan (destinationId)

        ContextAwareRLRouter router = (ContextAwareRLRouter) host.getRouter();
        //Ambil ENS
        EncounteredNodeSet ens = router.getEncounteredNodeSet();

        String hostId = String.valueOf(host.getAddress());
        // Debug output untuk melihat apakah destinationId ada dalam ENS
        System.out.println("[DEBUG] Checking ENS for Host: " + hostId +
                " Destination: " + destinationId +
                " ENS contains: " + ens.getAllNodeIds());

        // Destinasi ada di ENS host
        if(ens.getAllNodeIds().contains(destinationId)){
            return 1.0;
        } else {
            return 0.0;
        }
    }

    /**
     * Strategi Pembaruan pertama untuk Q-Value berdasarkan koneksi UP.
     * Pembaruan Q-value dilakukan dengan rumus:
     *
     * Q_c(d,m) ← α × [R(d,m) + γ × Fuzz(TOpp) × max(Q_m(d,y)@y ∈ Nm)] + (1 - α) × Q_c(d,m)
     *
     * @param host DTNHost yang digunakan untuk mengambil ENS
     * @param state ID tujuan yang sedang dipertimbangkan dalam pembaruan Q-value
     * @param action ID node yang ditemui, yang akan menjadi aksi yang diambil
     * @param fuzzOpp Nilai evaluasi fuzzy untuk transfer opportunity
     */
    public void updateFirstStrategy(DTNHost host, DTNHost neighbor, String state, String action, double fuzzOpp){
        System.out.println("[DEBUG] updateFirstStrategy DIPANGGIL untuk host "
                + host.getAddress() + " dengan neighbor " + neighbor.getAddress());

        // Mendapatkan router dari host untuk mengakses ENS
        ContextAwareRLRouter router = (ContextAwareRLRouter) host.getRouter();
        // Mengambil Encountered Node Set (ENS) dari router host
        EncounteredNodeSet ens = router.getEncounteredNodeSet();

        // Membongkar state untuk mendapatkan destinationId
        String[] stateParts = state.split(",");
        String destinationId = stateParts[1];  // Mengambil ID tujuan (destinationId)


        // Mengambil nilai Q untuk pasangan state-Action saat ini
        double qCurrent = qtable.getQvalue(state, action);
        // Menghitung reward
        double reward = calculateReward(host, state);
        // Mendapatkan nilai Q maksimum dari himpunan
        double maxQ = qtable.getMaxQvalueForEncounteredNodes(destinationId, ens);
        // Pembaruan Q-Table First Strategy
        double newQ = ALPHA * (reward + GAMMA * fuzzOpp * maxQ) + (1- ALPHA) * qCurrent;
        // Menyimpan kembali nilai Q yang telah di perbarui
        qtable.updateQValue(state, action, newQ);
    }


    public void updateSecondStrategy(DTNHost host, DTNHost neighbor) {
        System.out.println("[DEBUG] updateSecondStrategy DIPANGGIL untuk host "
                + host.getAddress() + " dengan neighbor " + neighbor.getAddress());

        //Mengambil State dari Q-Table
        Set<String> allStates = qtable.getAllStates();
        String action = String.valueOf(neighbor.getAddress());

        // Cek apakah ada record connection antara host dan neighbor
        ConnectionDuration connection = ConnectionDuration.getConnection(host, neighbor);
        if (connection == null) {
            System.out.println("[DEBUG] Tidak ada connection record untuk host " + host.getAddress() + " dan neighbor " + neighbor.getAddress());
            return; // Tidak ada koneksi sama sekali, langsung keluar
        }

        if (connection.getEndTime() == -1) {
            System.out.println("[DEBUG] Connection belum berakhir untuk host " + host.getAddress() + " dan neighbor " + neighbor.getAddress());
            return; // Koneksi masih aktif, tidak perlu aging
        }

        double endTime = connection.getEndTime();
        double currentTime = SimClock.getTime();
        double elapsedTime = currentTime - endTime;

        if (elapsedTime <= 0) {
            System.out.println("[DEBUG] Elapsed time <= 0, skip aging");
            return; // Waktu aneh, skip
        }

        // Loop semua state untuk melakukan aging Q-value
        for (String state : allStates) {
            if (!qtable.hasAction(state, action)) {
                continue; // Skip kalau action (neighbor) ini tidak ada di state tsb
            }

            double currentQ = qtable.getQvalue(state, action);

            // Debug log: Menampilkan nilai Q sebelum pembaruan
            System.out.println("[DEBUG] State: " + state + " | Action: " + action);
            System.out.println("[DEBUG] Q sebelum aging: " + currentQ);

            double newQ = currentQ * Math.pow(AGING_CONSTANT, elapsedTime);
            // Debug log: Menampilkan nilai Q setelah aging
            System.out.println("[DEBUG] Q sesudah aging: " + newQ);
            // Update Q-Value
            qtable.updateQValue(state, action, newQ);
        }

    }

    public void updateThirdStrategy(Qtable senderQtable, Qtable reciverQtable){
        for (Map.Entry<String, Double> entry : reciverQtable.getAllEntries()){
            String key = entry.getKey(); //State:Action
            double reciverQvalue = entry.getValue();

            //Split key, mengambil State : Action
            String[] parts = key.split(":");
            if (parts.length != 2) continue; // skip kalau format salah
            String state = parts[0];
            String action = parts[1];

            double senderQvalue = senderQtable.getQvalue(state, action);

            if (!senderQtable.hasAction(state, action)){
                // Jika sender tidak punya pasangan saat ini
                senderQtable.updateQValue(state, action, reciverQvalue);
            } else{
                if (senderQvalue < reciverQvalue){
                    // Update sender pakai reciverQvalue
                    senderQtable.updateQValue(state, action, reciverQvalue);
                } else {
                    // Update reciver pakai sender Qvalue
                    reciverQtable.updateQValue(state, action, senderQvalue);
                }
            }

        }
    }

}
