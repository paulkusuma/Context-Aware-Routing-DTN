package reinforcementLearning_ContextAware;

import routing.contextAware.ENS.EncounteredNodeSet;

import java.io.FileWriter;
import java.util.*;

import java.io.IOException;

public class Qtable {

    private String ownerId; // ID dari node pemilik Q-table ini
    // Struktur Q-table: Map<destinationId, Map<nextHopId, Q-value>>
    private Map<String, Map<String, Double>> qtable;

    /**
     * Konstruktor Qtable.
     * @param ownerId ID dari node yang memiliki Q-table ini.
     */
    public Qtable(String ownerId) {
        this.ownerId = ownerId;
        this.qtable = new HashMap<>();
    }

    /**
     * Mengembalikan ID pemilik Q-table.
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Inisialisasi seluruh Q-value ke 0.0 untuk semua kombinasi
     * (destinationId ≠ ownerId) dan (nextHop ≠ ownerId).
     *
     * @param allNodeIds Set berisi semua node ID dalam jaringan.
     */
    public void initializeAllQvalues(Set<String> allNodeIds) {
        for (String destinationId : allNodeIds) {
            if (destinationId.equals(ownerId)) continue; // Lewati diri sendiri sebagai tujuan
            for (String nextHop : allNodeIds) {
                if (nextHop.equals(ownerId)) continue; // Lewati diri sendiri sebagai nextHop
                updateQvalue(destinationId, nextHop, 0.0);
            }
        }
    }

    /**
     * Mengambil nilai Q untuk kombinasi (destinationId → nextHop).
     * Jika tidak ditemukan, default-nya 0.0.
     */
    public double getQvalue(String destinationId, String nextHop) {
        Map<String, Double> nextHopMap = qtable.get(destinationId);
        if (nextHopMap == null) return 0.0;
        return nextHopMap.getOrDefault(nextHop, 0.0);
    }

    /**
     * Memperbarui nilai Q untuk (destinationId → nextHop).
     * Q-value dipaksa maksimal 1.0 (normalisasi).
     */
    public void updateQvalue(String destinationId, String nextHop, double qvalue) {
        qvalue = Math.min(qvalue, 1.0);
        qtable.computeIfAbsent(destinationId, k -> new HashMap<>()).put(nextHop, qvalue);
    }

    /**
     * Mengecek apakah Q-table memiliki entri untuk (destinationId → nextHop).
     */
    public boolean hasAction(String destinationId, String nextHop) {
        return qtable.containsKey(destinationId) && qtable.get(destinationId).containsKey(nextHop);
    }

    /**
     * Mengambil semua ID tujuan (destination) yang terdapat dalam Q-table.
     */
    public Set<String> getAllDestinations() {
        return qtable.keySet();
    }

    /**
     * Mengembalikan referensi langsung ke seluruh struktur Q-table.
     */
    public Map<String, Map<String, Double>> getAllQvalues() {
        return this.qtable;
    }

    /**
     * Mengambil semua aksi (nextHop → Q-value) untuk suatu tujuan.
     * Sinkronisasi digunakan untuk mencegah race condition jika digunakan multithread.
     */
    public synchronized Map<String, Double> getActionMap(String destination) {
//        return qtable.getOrDefault(destination, new HashMap<>());
        return qtable.get(destination);
    }

    /**
     * Menghitung nilai Q (Q-value) terbesar dari node ini ke tujuan tertentu (destinationId),
     * melalui semua node dalam himpunan tetangga yang pernah ditemui (ENS - EncounteredNodeSet).
     * Digunakan dalam konteks Q-routing untuk menentukan seberapa "bagus" jalur tercepat
     * ke tujuan dari perspektif node tetangga.
     * Jika tidak ada nilai Q yang tersedia (belum pernah diinisialisasi), maka akan
     * dikembalikan nilai default 0.0.
     *
     * @param destinationId ID dari node tujuan yang ingin dituju.
     * @param ensNeighbor   Kumpulan node yang pernah ditemui (dapat dijadikan nextHop).
     * @return Nilai Q maksimum dari node ini ke destinationId melalui salah satu tetangga dalam ensNeighbor.
     */
    public double getMaxQvalue(String destinationId, EncounteredNodeSet ensNeighbor) {
        double maxQvalue = Double.NEGATIVE_INFINITY;
//        System.out.printf("[DEBUG] Mencari maxQ untuk dst=%s dengan ENS: %s%n",
//                destinationId, ensNeighbor.getAllNodeIds());
        for (String y : ensNeighbor.getAllNodeIds()) { // Iterasi setiap tetangga y dalam himpunan ENS milik M
            // Ambil nilai Q untuk (destinationId, nextHop=y)
            double q = getQvalue(destinationId, y);
//            System.out.printf("[DEBUG] dst=%s via=%s | Q=%.4f%n", destinationId, y, q);
            maxQvalue = Math.max(maxQvalue, q);
        }
        // Jika tidak ada nilai Q valid ditemukan, kembalikan 0.0 sebagai default
        return (maxQvalue == Double.NEGATIVE_INFINITY) ? 0.0 : maxQvalue;
//        double finalMaxQ = (maxQvalue == Double.NEGATIVE_INFINITY) ? 0.0 : maxQvalue;
//        System.out.printf("[DEBUG] MAX Q-VALUE untuk dst=%s adalah %.4f%n", destinationId, finalMaxQ);
//        return finalMaxQ;
    }

    public String toString() {
        return "Qtable milik" + ownerId + ":\n" + qtable.toString();
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