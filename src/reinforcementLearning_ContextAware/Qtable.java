package reinforcementLearning_ContextAware;

import routing.contextAware.ENS.EncounteredNodeSet;
import java.util.*;

public class Qtable {

    // Q-table disimpan sebagai map (key: state-action, value: Q-value)
    private Map<String, Double> qtable;

    public Qtable() {
        qtable = new HashMap<>();
    }

//    public Qtable(Qtable otherQTable) {
//        this.qtable = new HashMap<>(otherQTable.qtable);
//    }

//    public Qtable clone(){
//        return new Qtable(this);
//    }

//    private String makeKey(String state, String action) {
//        return state + ":" + action;
//    }

    // Mengambil Q-Value untuk kombinasi state action
    public double getQvalue(String state, String action) {
        String key = state + ":" + action;
//        if (!qtable.containsKey(key)) {
//            System.out.printf("  [Q-GET] Key '%s' NOT FOUND → return default 0.1\n", key);
//        } else {
//            System.out.printf("  [Q-GET] Key '%s' FOUND → Q = %.4f\n", key, qtable.get(key));
//        }
        //Mengembalikan nilai Q-Value jika ada, atau 0 jika tidak ada
        return qtable.getOrDefault(key, 0.1);
    }

    // Mengupdate nilai Q untuk pasangan State Action tertentu
    public void updateQValue(String state, String action, double qvalue) {
        String key = state + ":" + action;
        qvalue =Math.min(qvalue, 1.0);
        qtable.put(key, qvalue);
    }

    // Mengecek Kombinasi State Action yang ada di QTable
    public boolean hasAction(String state, String action) {
        String key = state + ":" + action;
        return qtable.containsKey(key);
    }

    // Mendapatkan states yang ada di Q-Table
    public Set<String> getAllStates(){
        Set<String> states = new HashSet<>();
        for (String key : qtable.keySet()) {
            String[] parts = key.split(":");
            if (parts.length == 2) {
                states.add(parts[0]); // hanya bagian state ("hostId,destinationId")
            }
        }
        return states;
    }


    public Set<Map.Entry<String, Double>> getAllEntries(){
        return qtable.entrySet();
    }

    /**
     * Mengambil nilai Q-maksimum (MaxQ) dari node tetangga (m) terhadap semua node
     * yang pernah ditemuinya (y ∈ ENS_m) untuk mencapai tujuan tertentu (d).
     *
     * <p>Digunakan dalam strategi pembaruan Q-learning pertama:
     * <pre>
     *     Q_c(d, m) ← α × [R + γ × TOpp × max(Q_m(d, y))] + (1−α) × Q_c(d, m)
     * </pre>
     *
     * <p>Di mana:
     * - c = current host (yang sedang mengupdate Q-nya)
     * - m = node tetangga (neighbor)
     * - d = destination dari pesan
     * - y ∈ ENS_m = node-node yang pernah ditemui oleh m
     *
     * <p>Method ini mencari nilai Q tertinggi yang dimiliki oleh node m terhadap
     * tujuan d melalui semua node y yang pernah ditemuinya (ENS_m).
     *
     * @param stateNeighbor String dalam bentuk "m,d" (ID node tetangga, ID tujuan)
     * @param ensNeighbor       Himpunan Encountered Nodes milik neighbor (m)
     * @return Nilai Q maksimum dari semua action (y) ∈ ENS_m untuk mencapai d
     *         Jika ENS kosong, maka dikembalikan 0.0
     */
    public double getMaxQvalueForEncounteredNodes(String stateNeighbor, EncounteredNodeSet ensNeighbor) {
        double maxQvalue = Double.NEGATIVE_INFINITY;
//        System.out.println("[MAXQ DEBUG] State = " + state);
//        System.out.println("[MAXQ DEBUG] Mengecek Q untuk semua encountered node:");
        // Iterasi melalui semua node yang pernah ditemui
        for (String y : ensNeighbor.getAllNodeIds()) {
            // Ambil nilai Q untuk pasangan (encounteredNodeId, destinationId)

            double qValue = getQvalue(stateNeighbor, y);
            //            System.out.printf("  → Node: %s | Q(%s,%s) = %.4f%n", y, stateNeighbor, y, qValue);
            // Perbarui maxQvalue dengan nilai Q tertinggi
            maxQvalue = Math.max(maxQvalue, qValue);
        }
        if (maxQvalue == Double.NEGATIVE_INFINITY) {
            return 0.0;
        }

        return maxQvalue;
    }



    public Set<String> getAllActionsForState(String state) {
        Set<String> actions = new HashSet<>();
        for (String key : qtable.keySet()) {
            if (key.startsWith(state + ":")) {
                String action = key.split(":")[1];
                actions.add(action);
            }
        }
        return actions;
    }


    public void printQtable(String hostID) {
        System.out.printf("Q-table of Host %s:\n", hostID);
        for (Map.Entry<String, Double> entry : qtable.entrySet()) {
            System.out.printf("  %s = %.4f\n", entry.getKey(), entry.getValue());
        }
    }
//    /**
//     * Menampilkan seluruh isi Q-table ke console dengan format:
//     * State: <state> | Action: <action> | Q-value: <nilai>
//     *
//     * @param owner Nama atau ID pemilik Q-table (misal: ID node)
//     */
//    public void printQtable(String owner) {
//        System.out.println("====== Q-table milik " + owner + " ======");
//        for (Map.Entry<String, Double> entry : qtable.entrySet()) {
//            String key = entry.getKey();
//            double value = entry.getValue();
//
//            // Pisahkan state dan action dari key
//            String[] parts = key.split(":");
//            if (parts.length == 2) {
//                String state = parts[0];
//                String action = parts[1];
//                System.out.printf("State: %-10s | Action: %-10s | Q-value: %.4f%n", state, action, value);
//            } else {
//                System.out.println("Format key tidak valid: " + key);
//            }
//        }
//        System.out.println("========================================");
//    }
//
//
//    // Mencetak Q-Table untuk Node tertentu berdasarkan hostId
//    public void printQtableByHost(String hostId) {
//        Map<String, List<String>> groupSetByHost = new HashMap<>();
//
//        // Iterasi melalui setiap key dalam qtable
//        for (String key : qtable.keySet()) {
//            String[] parts = key.split(":");
//            String[] stateParts = parts[0].split(",");
//            String currentHostId = stateParts[0];  // Mendapatkan hostId
//            String destinationId = stateParts[1];
//            String action = parts[1];
//            double qvalue = qtable.get(key);
//
//            // Hanya memasukkan entri yang cocok dengan hostId
//            if (currentHostId.equals(hostId)) {
//                String entry = "State = (" + currentHostId + "," + destinationId + ")"
//                        + ", Action = " + action
//                        + ", Q-Value = " + qvalue;
//
//                groupSetByHost.computeIfAbsent(currentHostId, k -> new ArrayList<>()).add(entry);
//            }
//        }
//
//        // Menampilkan Q-Table untuk hostId yang diberikan
//        if (groupSetByHost.containsKey(hostId)) {
//            System.out.println("===== Q-Table Node " + hostId + " =====");
//            for (String line : groupSetByHost.get(hostId)) {
//                System.out.println(line);
//            }
//        } else {
////            System.out.println("Tidak ada data Q-Table untuk Node " + hostId);
//        }
//    }
//
//    // Mencetak Q-Table untuk setiap Node
//    public void printQtable(){
//        Map<String, List<String>> groupSetByHost = new HashMap<>();
//
//        for (String key : qtable.keySet()) {
//            String [] parts = key.split(":");
//            String [] stateParts = parts[0].split(",");
//            String hostId = stateParts[0];
//            String destinationId = stateParts[1];
//            String action = parts[1];
//            double qvalue = qtable.get(key);
//
//            String entry = "State = (" + hostId + "," + destinationId + ")"
//                    + ", Action = " + action
//                    + ", Q-Value = " + qvalue;
//
//            groupSetByHost.computeIfAbsent(hostId, k -> new ArrayList<>()).add(entry);
//        }
//
//        for (Map.Entry<String, List<String>> entry : groupSetByHost.entrySet()) {
//            String hostId = entry.getKey();
//            List<String> entries = entry.getValue();
//            System.out.println("===== Q-Table Node " +hostId+ " =====");
//            for (String line : entries) {
//                System.out.println(line);
//            }
//            System.out.println();
//        }

//    }

}
