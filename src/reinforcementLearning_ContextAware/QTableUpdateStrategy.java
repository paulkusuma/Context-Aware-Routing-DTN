package reinforcementLearning_ContextAware;

import core.DTNHost;
import core.SimClock;
import routing.contextAware.ContextAwareRLRouter;
import routing.contextAware.ENS.ConnectionDuration;
import routing.contextAware.ENS.EncounteredNodeSet;

/**
 * QTableUpdateStrategy adalah kelas yang mengatur pembaruan Q-table berdasarkan algoritma Q-learning.
 */
public class QTableUpdateStrategy {

    // Objek Qtable yang berfungsi untuk menyimpan dan memperbarui Q-values
    private Qtable qtable;
    // Faktor diskonto, untuk mempertimbangkan nilai masa depan dalam pembaruan
    private static final double GAMMA = 0.3;
    // Laju pembelajaran (learning rate), untuk mengontrol seberapa besar pembaruan nilai Q
    private static final double ALPHA = 0.9;
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
            return 0.5;
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
    public void updateFirstStrategy(DTNHost host, String state, String action, double fuzzOpp){
        // Mendapatkan router dari host untuk mengakses ENS
        ContextAwareRLRouter router = (ContextAwareRLRouter) host.getRouter();
        // Mengambil Encountered Node Set (ENS) dari router host
        EncounteredNodeSet ens = router.getEncounteredNodeSet();

        // Mengambil nilai Q untuk pasangan state-Action saat ini
        double qCurrent = qtable.getQvalue(state, action);

        // Menghitung reward
        double reward = calculateReward(host, state);

        // Mendapatkan nilai Q maksimum dari himpunan
        double maxQ = qtable.getMaxQvalueForEncounteredNodes(state, ens);

        // Pembaruan Q-Table First Strategy
        double newQ = ALPHA * (reward + GAMMA * fuzzOpp * maxQ) + (1- ALPHA) * qCurrent;


        // Menyimpan kembali nilai Q yang telah di perbarui
        qtable.updateQValue(state, action, newQ);
        System.out.println("[DEBUG] Updated Q-value for (" + state + ", " + action + "): " + newQ + " reward = " + reward);
    }


    public void updateSecondStrategy(DTNHost host, DTNHost neighbor, String destinationId){
        String state = destinationId;
        String action = String.valueOf(neighbor.getAddress());

        // Mengambil nilai Q saat ini
        double currentQ = qtable.getQvalue(state, action);

        // Mengambil Connection Terakhir
        ConnectionDuration connection =ConnectionDuration.getConnection(host, neighbor);

        if (connection == null || connection.getEndTime() == -1) {
            // tidak ada koneksi sebelumnya atau terputus
            return;
        }

        double endTime = connection.getEndTime();
        double currentTime = SimClock.getTime();
        double elapsedTime = currentTime - endTime;

        // Update Q-Value dengan peluruhan
        double newQ = currentQ * Math.pow(AGING_CONSTANT, elapsedTime);
        // Simpan Q-Value
        qtable.updateQValue(state, action, newQ);

        // Optional: Debug log
        System.out.println("[DEBUG] Aging Q-value for (" + state + "," + action + "): elapsed=" + elapsedTime + " newQ=" + newQ);

    }


}
