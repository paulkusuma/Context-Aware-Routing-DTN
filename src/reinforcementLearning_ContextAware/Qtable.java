package reinforcementLearning_ContextAware;

import routing.contextAware.ENS.EncounteredNodeSet;
import java.util.*;

public class Qtable {

    // Q-table disimpan sebagai map (key: state-action, value: Q-value)
    private Map<String, Double> qtable;

    public Qtable() {
        qtable = new HashMap<>();
    }

    // Mengambil Q-Value untuk kombinasi state action
    public double getQvalue(String state, String action) {
        String key = state + ":" + action;
        //Mengembalikan nilai Q-Value jika ada, atau 0 jika tidak ada
        return qtable.getOrDefault(key, 0.1);
    }

    // Mengupdate nilai Q untuk pasangan State Action tertentu
    public void updateQValue(String state, String action, double qvalue) {
        String key = state + ":" + action;
        qvalue =Math.min(qvalue, 1.0);
        qtable.put(key, qvalue);
    }

    /**
     * Mengambil nilai Q-maksimum (MaxQ) dari node-node yang pernah ditemui (Nm) oleh host,
     * untuk sebuah state (kombinasi hostId, destinationId).
     * yang berarti mencari nilai Q tertinggi yang dimiliki host (node m)
     * untuk tujuan d, melalui setiap node y yang pernah ditemui (y ∈ ENS).
     * @param state State dalam bentuk "hostId,destinationId"
     * @param ens EncounteredNodeSet milik host (berisi semua node yang ditemui)
     * @return Nilai Q maksimum dari semua action (y) dalam ENS
     * Semakin besar MaxQ → host semakin yakin bahwa jalur tidak langsung via tetangga masih efektif
     * Kalau MaxQ rendah → host ragu, nilai Q tidak akan naik banyak
     */
    public double getMaxQvalueForEncounteredNodes(String state, EncounteredNodeSet ens) {
        double maxQvalue = Double.NEGATIVE_INFINITY;
//        System.out.println("[MAXQ DEBUG] State = " + state);
//        System.out.println("[MAXQ DEBUG] Mengecek Q untuk semua encountered node:");
        // Iterasi melalui semua node yang pernah ditemui
        for (String encounteredNodeId : ens.getAllNodeIds()) {
            // Ambil nilai Q untuk pasangan (encounteredNodeId, destinationId)
            double qValue = getQvalue(state, encounteredNodeId);
//            System.out.printf("  → Node: %s | Q(%s,%s) = %.4f%n", encounteredNodeId, state, encounteredNodeId, qValue);
            // Perbarui maxQvalue dengan nilai Q tertinggi
            maxQvalue = Math.max(maxQvalue, qValue);
        }
        if (maxQvalue == Double.NEGATIVE_INFINITY) {
//            System.out.println("[MAXQ DEBUG] Tidak ada node dalam ENS → maxQ = 0.0");
            return 0.0;
        }

//        System.out.printf("[MAXQ RESULT] maxQ = %.4f%n", maxQvalue);
        return maxQvalue ;
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
    public Set<Map.Entry<String, Double>> getAllEntries(){
        return qtable.entrySet();
    }






    // Mencetak Q-Table untuk Node tertentu berdasarkan hostId
    public void printQtableByHost(String hostId) {
        Map<String, List<String>> groupSetByHost = new HashMap<>();

        // Iterasi melalui setiap key dalam qtable
        for (String key : qtable.keySet()) {
            String[] parts = key.split(":");
            String[] stateParts = parts[0].split(",");
            String currentHostId = stateParts[0];  // Mendapatkan hostId
            String destinationId = stateParts[1];
            String action = parts[1];
            double qvalue = qtable.get(key);

            // Hanya memasukkan entri yang cocok dengan hostId
            if (currentHostId.equals(hostId)) {
                String entry = "State = (" + currentHostId + "," + destinationId + ")"
                        + ", Action = " + action
                        + ", Q-Value = " + qvalue;

                groupSetByHost.computeIfAbsent(currentHostId, k -> new ArrayList<>()).add(entry);
            }
        }

        // Menampilkan Q-Table untuk hostId yang diberikan
        if (groupSetByHost.containsKey(hostId)) {
            System.out.println("===== Q-Table Node " + hostId + " =====");
            for (String line : groupSetByHost.get(hostId)) {
                System.out.println(line);
            }
        } else {
//            System.out.println("Tidak ada data Q-Table untuk Node " + hostId);
        }
    }

    // Mencetak Q-Table untuk setiap Node
    public void printQtable(){
        Map<String, List<String>> groupSetByHost = new HashMap<>();

        for (String key : qtable.keySet()) {
            String [] parts = key.split(":");
            String [] stateParts = parts[0].split(",");
            String hostId = stateParts[0];
            String destinationId = stateParts[1];
            String action = parts[1];
            double qvalue = qtable.get(key);

            String entry = "State = (" + hostId + "," + destinationId + ")"
                    + ", Action = " + action
                    + ", Q-Value = " + qvalue;

            groupSetByHost.computeIfAbsent(hostId, k -> new ArrayList<>()).add(entry);
        }

        for (Map.Entry<String, List<String>> entry : groupSetByHost.entrySet()) {
            String hostId = entry.getKey();
            List<String> entries = entry.getValue();
            System.out.println("===== Q-Table Node " +hostId+ " =====");
            for (String line : entries) {
                System.out.println(line);
            }
            System.out.println();
        }

    }

}
