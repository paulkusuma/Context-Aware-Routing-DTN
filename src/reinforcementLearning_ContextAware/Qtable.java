package reinforcementLearning_ContextAware;

import routing.contextAware.ENS.EncounteredNodeSet;

import java.io.FileWriter;
import java.util.*;

import java.io.PrintWriter;
import java.io.File;
import java.io.IOException;

public class Qtable {

    private String ownerId;
    private Map<String, Map<String, Double>> qtable;

    public Qtable(String ownerId) {
        this.ownerId = ownerId;
        this.qtable = new HashMap<>();
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void initializeAllQvalues(Set<String> allNodeIds) {
//        System.out.println("Menginisialisasi Q-values...");
        for (String destinationId : allNodeIds) {
            if (destinationId.equals(ownerId)) continue;
            for (String nextHop : allNodeIds) {
                if (nextHop.equals(ownerId)) continue;
                updateQvalue(destinationId, nextHop, 0.0);
            }
        }
    }

    public double getQvalue(String destinationId, String nextHop) {
        Map<String, Double> nextHopMap = qtable.get(destinationId);
        if (nextHopMap == null) return 0.0;
        return nextHopMap.getOrDefault(nextHop, 0.0);
    }

    public void updateQvalue(String destinationId, String nextHop, double qvalue) {
        qvalue = Math.min(qvalue, 1.0);
        qtable.computeIfAbsent(destinationId, k -> new HashMap<>()).put(nextHop, qvalue);
    }


    public boolean hasAction(String destinationId, String nextHop) {
        return qtable.containsKey(destinationId) && qtable.get(destinationId).containsKey(nextHop);
    }


    public Set<String> getAllDestinations() {
        return qtable.keySet();
    }

    public Map<String, Map<String, Double>> getRawTable() {
        return qtable;
    }

    public Set<Map.Entry<String, Map<String, Double>>> getAllEntries() {
        return qtable.entrySet();
    }
    public Map<String, Map<String, Double>> getAllQvalues() {
        return this.qtable;
    }
    public synchronized Map<String, Double> getActionMap(String destination) {
//        return qtable.getOrDefault(destination, new HashMap<>());
        return qtable.get(destination);
    }

    public double getMaxQvalue(String destinationId, EncounteredNodeSet ensNeighbor) {
        double maxQvalue = Double.NEGATIVE_INFINITY;
//        System.out.printf("[DEBUG] Mencari maxQ untuk dst=%s dengan ENS: %s%n",
//                destinationId, ensNeighbor.getAllNodeIds());
        for (String y : ensNeighbor.getAllNodeIds()) {
            double q = getQvalue(destinationId, y);
//            System.out.printf("[DEBUG] dst=%s via=%s | Q=%.4f%n", destinationId, y, q);
            maxQvalue = Math.max(maxQvalue, q);
        }

        return (maxQvalue == Double.NEGATIVE_INFINITY) ? 0.0 : maxQvalue;
//        double finalMaxQ = (maxQvalue == Double.NEGATIVE_INFINITY) ? 0.0 : maxQvalue;
//        System.out.printf("[DEBUG] MAX Q-VALUE untuk dst=%s adalah %.4f%n", destinationId, finalMaxQ);
//        return finalMaxQ;
    }

    public Qtable clone() {
        Qtable copy = new Qtable(this.ownerId);
        for (Map.Entry<String, Map<String, Double>> entry : this.qtable.entrySet()) {
            String destinationId = entry.getKey();
            Map<String, Double> nextHopMap = new HashMap<>(entry.getValue());
            copy.qtable.put(destinationId, nextHopMap);
        }
        return copy;
    }

    public String toString() {
        return "Qtable milik" + ownerId + ":\n" + qtable.toString();
    }

    private Set<String> getAllActions() {
        Set<String> actions = new TreeSet<>();
        for (Map<String, Double> actionMap : qtable.values()) {
            actions.addAll(actionMap.keySet());
        }
        return actions;
    }

    public void exportToCSV(String filePath, boolean append) {
        System.out.println("Menulis ke file: " + filePath);
        int columnWidth = 12;
        try (FileWriter writer = new FileWriter(filePath, append)) {
            // Header per Node
//            writer.append("Qtable").append(ownerId).append("\n");

            // Ambil semua next hop unik untuk header kolom
            Set<String> allNextHops = new TreeSet<>();
            for (Map<String, Double> map : qtable.values()) {
                allNextHops.addAll(map.keySet());
            }

            // Tulis header kolom
            writer.append(padRight("Qtab nd " + getOwnerId(), columnWidth));
            for (String nextHop : allNextHops) {
                writer.append(padRight("Action " + nextHop, columnWidth));
            }
            writer.append("\n");

            // Tulis isi Q-value
            for (String state : new TreeSet<>(qtable.keySet())) {
                writer.append(padRight("State " + state, columnWidth));
                for (String nextHop : allNextHops) {
                    double qvalue = qtable.getOrDefault(state, new HashMap<>())
                            .getOrDefault(nextHop, 0.0);
                    writer.append(padRight(String.format("%.4f", qvalue), columnWidth));
                }
                writer.append("\n");
            }

            writer.append("\n"); // spasi antar node
        } catch (IOException e) {
            System.err.println("Error writing pivoted Q-table to CSV: " + e.getMessage());
        }
    }

    private String padRight(String str, int width) {
        return String.format("%-" + width + "s", str);
    }


    public static void printQTable(Qtable qtable, String ownerId) {
        Map<String, Map<String, Double>> qMap = qtable.getAllQvalues();

        // Kumpulkan semua action (next hops) unik dan urutkan
        Set<String> allActions = new TreeSet<>();
        for (Map<String, Double> actions : qMap.values()) {
            allActions.addAll(actions.keySet());
        }
        List<String> actionList = new ArrayList<>(allActions);

        // Header
        System.out.println();
        System.out.printf("%-10s", "Qtab Nd" + ownerId); // Kolom pertama
        for (String action : actionList) {
            System.out.printf("%10s", "Act " + action);
        }
        System.out.println();

        // Isi Q-table
        // Semua state disortir berdasarkan nama
        List<String> sortedStates = new ArrayList<>(qMap.keySet());
        Collections.sort(sortedStates);

        for (String state : sortedStates) {
            Map<String, Double> actionMap = qMap.get(state);
            System.out.printf("%-10s", "State" + state);
            for (String action : actionList) {
                double q = actionMap.getOrDefault(action, 0.0);
                System.out.printf("%10.4f", q);
            }
            System.out.println();
        }
    }
}


//    // Mengecek Kombinasi State Action yang ada di QTable
//    public boolean hasAction(String state, String action) {
//        String key = state + ":" + action;
//        return qtable.containsKey(key);
//    }
//    // Mengupdate nilai Q untuk pasangan State Action tertentu
//    public void updateQValue(String state, String action, double qvalue) {
//        String key = state + ":" + action;
//        qvalue =Math.min(qvalue, 1.0);
//        qtable.put(key, qvalue);
//    }

//    // Mengambil Q-Value untuk kombinasi state action
//    public double getQvalue(String state, String action) {
//        String key = state + ":" + action;

/// /        if (!qtable.containsKey(key)) {
//    / /            System.out.printf("  [Q-GET] Key '%s' NOT FOUND → return default 0.1\n", key);
/// /        } else {
//    / /            System.out.printf("  [Q-GET] Key '%s' FOUND → Q = %.4f\n", key, qtable.get(key));
/// /        }
//        //Mengembalikan nilai Q-Value jika ada, atau 0 jika tidak ada
//        return qtable.getOrDefault(key, 0.1);
//    }
//    // Mendapatkan states yang ada di Q-Table
//    public Set<String> getAllStates(){
//        Set<String> states = new HashSet<>();
//        for (String key : qtable.keySet()) {
//            String[] parts = key.split(":");
//            if (parts.length == 2) {
//                states.add(parts[0]); // hanya bagian state ("hostId,destinationId")
//            }
//        }
//        return states;
//    }


//    public Set<Map.Entry<String, Double>> getAllEntries(){
//        return qtable.entrySet();
//    }

//    /**
//     * Mengambil nilai Q-maksimum (MaxQ) dari node tetangga (m) terhadap semua node
//     * yang pernah ditemuinya (y ∈ ENS_m) untuk mencapai tujuan tertentu (d).
//     *
//     * <p>Digunakan dalam strategi pembaruan Q-learning pertama:
//     * <pre>
//     *     Q_c(d, m) ← α × [R + γ × TOpp × max(Q_m(d, y))] + (1−α) × Q_c(d, m)
//     * </pre>
//     *
//     * <p>Di mana:
//     * - c = current host (yang sedang mengupdate Q-nya)
//     * - m = node tetangga (neighbor)
//     * - d = destination dari pesan
//     * - y ∈ ENS_m = node-node yang pernah ditemui oleh m
//     *
//     * <p>Method ini mencari nilai Q tertinggi yang dimiliki oleh node m terhadap
//     * tujuan d melalui semua node y yang pernah ditemuinya (ENS_m).
//     *
//     * @param stateNeighbor String dalam bentuk "m,d" (ID node tetangga, ID tujuan)
//     * @param ensNeighbor       Himpunan Encountered Nodes milik neighbor (m)
//     * @return Nilai Q maksimum dari semua action (y) ∈ ENS_m untuk mencapai d
//     *         Jika ENS kosong, maka dikembalikan 0.0
//     */
//    public double getMaxQvalueForEncounteredNodes(String stateNeighbor, EncounteredNodeSet ensNeighbor) {
//        double maxQvalue = Double.NEGATIVE_INFINITY;

    /// /        System.out.println("[ ] State = " + state);
    /// /        System.out.println("[ ] Mengecek Q untuk semua encountered node:");
//        // Iterasi melalui semua node yang pernah ditemui
//        for (String y : ensNeighbor.getAllNodeIds()) {
//            // Ambil nilai Q untuk pasangan (encounteredNodeId, destinationId)
//
//            double qValue = getQvalue(stateNeighbor, y);
//            //            System.out.printf("  → Node: %s | Q(%s,%s) = %.4f%n", y, stateNeighbor, y, qValue);
//            // Perbarui maxQvalue dengan nilai Q tertinggi
//            maxQvalue = Math.max(maxQvalue, qValue);
//        }
//        if (maxQvalue == Double.NEGATIVE_INFINITY) {
//            return 0.0;
//        }
//
////        return maxQvalue;
////    }
//    public Set<String> getAllActionsForState(String state) {
////        Set<String> actions = new HashSet<>();
////        for (String key : qtable.keySet()) {
////            if (key.startsWith(state + ":")) {
////                String action = key.split(":")[1];
////                actions.add(action);
////            }
////        }
////        return actions;
////    }
////
////
////    public void printQtable(String hostID) {
////        System.out.printf("Q-table of Host %s:\n", hostID);
////        for (Map.Entry<String, Double> entry : qtable.entrySet()) {
////            System.out.printf("  %s = %.4f\n", entry.getKey(), entry.getValue());
////        }
////    }
////}
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


