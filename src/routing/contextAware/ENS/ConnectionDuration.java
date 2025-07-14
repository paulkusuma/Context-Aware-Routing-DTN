package routing.contextAware.ENS;

import core.DTNHost;
import  core.SimClock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kelas ConnectionDuration digunakan untuk merekam durasi koneksi antara dua node.
 */
public class ConnectionDuration {
    private DTNHost fromNode;
    private DTNHost toNode;
    private double startTime;
    private double endTime;
    private double totalDuration;

    // Riwayat koneksi menggunakan HashMap
    private static Map<DTNHost, Map<DTNHost, ConnectionDuration>> connectionHistory = new HashMap<>();

    /**
     * Konstruktor untuk membuat instance ConnectionDuration.
     * @param fromNode Node pengirim.
     * @param toNode Node penerima.
     */
    public ConnectionDuration(DTNHost fromNode, DTNHost toNode) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.startTime = SimClock.getTime();
        this.endTime = -1; //-1 berarti koneksi masih aktif
        this.totalDuration = 0;
    }

    /**
     * Memulai koneksi antara dua node.
     * @param fromNode Node pengirim.
     * @param toNode Node penerima.
     * @return Instance ConnectionDuration yang merepresentasikan koneksi tersebut.
     */
    public static ConnectionDuration startConnection(DTNHost fromNode, DTNHost toNode) {
        // Ambil ID dari kedua node
        String fromNodeId = String.valueOf(fromNode.getAddress());
        String toNodeId = String.valueOf(toNode.getAddress());
        //DEBUG
//        printDebugLog(fromNode, toNode, "Mulai");

        connectionHistory.putIfAbsent(fromNode, new HashMap<>());
        //Jika sudah ada kembalikan
        if(connectionHistory.get(fromNode).containsKey(toNode)){
            ConnectionDuration existingConnection = connectionHistory.get(fromNode).get(toNode);
            existingConnection.startTime = SimClock.getTime();
            existingConnection.endTime = -1;
            //DEBUG
//            printDebugLog(fromNode, toNode, "Melanjutkan koneksi sebelumnya");
            return existingConnection;
        }
        //buat koneksi baru
        ConnectionDuration newconnectionDuration = new ConnectionDuration(fromNode, toNode);
        connectionHistory.get(fromNode).put(toNode, newconnectionDuration);
        //DEBUG
//        printDebugLog(fromNode, toNode, "Koneksi baru dimulai");
        return newconnectionDuration;
    }


    /**
     * Mengakhiri koneksi dan mencatat waktu akhir koneksi.
     */
    public void endConnection(DTNHost fromNode, DTNHost toNode, EncounteredNodeSet encounteredNodeSet) {

        this.endTime = SimClock.getTime();

//        // Ambil durasi sebelum diupdate
//        double previousDuration = this.totalDuration;
        // Ambil durasi sebelumnya sebelum update
        double previousDuration = getTotalConnectionDuration(fromNode, toNode);

        double sessionDuration = this.endTime - this.startTime;
        this.totalDuration += sessionDuration;

        // Ambil ID dari kedua node
        String fromNodeId = String.valueOf(fromNode.getAddress());
        String toNodeId = String.valueOf(toNode.getAddress());

//        System.out.println("=======================================================");
//        // Mencetak informasi durasi koneksi sebelumnya
////        double previousDuration = ConnectionDuration.getTotalConnectionDuration(fromNode, toNode);
//        System.out.println("[DEBUG] Durasi sebelumnya: " + previousDuration + " detik.");
//
//        // Log waktu akhir koneksi
//        System.out.println("[DEBUG] Waktu akhir koneksi antara node " + fromNode.getAddress() + " dan " + toNode.getAddress() + ": " + this.endTime);
//        System.out.println("[DEBUG] Durasi sesi: " + sessionDuration + " detik");
//        System.out.println("[DEBUG] Total durasi koneksi sejauh ini: " + this.totalDuration + " detik");

        // Konversi ke long untuk update ENS
        long sessionDurationLong = (long) sessionDuration;
        // Update durasi koneksi di ENS untuk kedua node
        encounteredNodeSet.updateConnectionDuration(fromNodeId, sessionDurationLong);
        encounteredNodeSet.updateConnectionDuration(toNodeId, sessionDurationLong);

        // Setelah update totalDuration, dan cetak semua debug
        // Sinkronkan kembali this ke dalam map
        connectionHistory.get(fromNode).put(toNode, this);
        // Mencetak durasi koneksi yang baru setelah koneksi berakhir
        double newTotalDuration = getTotalConnectionDuration(fromNode, toNode);

//        System.out.printf("[DEBUG] Koneksi %s - %s diakhiri pada %.2f detik\n",
//                fromNode.getAddress(), toNode.getAddress(), SimClock.getTime());
//        System.out.println("[DEBUG] Durasi total setelah koneksi: " + newTotalDuration + " detik");
    }


    /**
     * Menghitung durasi koneksi.
     * @return Durasi koneksi dalam detik.
     */
    public double getDuration() {
        if (endTime == -1) {
            return totalDuration + (SimClock.getTime() - startTime);
        }
        return totalDuration;
    }

    /**
     * Mengambil ConnectionDuration berdasarkan fromNode dan toNode.
     * @param fromNode Node pengirim.
     * @param toNode Node penerima.
     * @return Instance ConnectionDuration jika ditemukan, null jika tidak ada.
     */
    public static ConnectionDuration getConnection(DTNHost fromNode, DTNHost toNode) {
        return connectionHistory.getOrDefault(fromNode, new HashMap<>()).get(toNode);
    }

    public static List<ConnectionDuration> getConnectionsFromHost(DTNHost host){
        if (!connectionHistory.containsKey(host)) return new ArrayList<>();
        return  new ArrayList<>(connectionHistory.get(host).values());
    }

    public static double getTotalConnectionDuration(DTNHost nodeA, DTNHost nodeB) {
        // Cek apakah nodeA ada di map
        if (connectionHistory.containsKey(nodeA)) {
            Map<DTNHost, ConnectionDuration> innerMap = connectionHistory.get(nodeA);
            if (innerMap.containsKey(nodeB)) {
                return innerMap.get(nodeB).getDuration();
            }
        }

        // Kalau belum pernah ada koneksi
        return 0.0;
    }

    // Menambahkan method getEndTime()
    public double getEndTime() {
        return this.endTime;
    }

    public boolean isActive() {
        return endTime == -1;
    }

    /**
     * Menghapus koneksi dari history berdasarkan fromNode dan toNode.
     * @param fromNode Node pengirim.
     * @param toNode Node penerima.
     */
    public static void removeConnection(DTNHost fromNode, DTNHost toNode, double startTime) {
        if(connectionHistory.containsKey(fromNode)){
            connectionHistory.get(fromNode).remove(toNode);
            if(connectionHistory.get(fromNode).isEmpty()){
                connectionHistory.remove(fromNode);
            }
        }
    }


    /**
     * Fungsi untuk mencetak log debug dalam satu tempat yang terstruktur.
     * @param fromNode Node pengirim.
     * @param toNode Node penerima.
     * @param action Deskripsi dari tindakan yang dilakukan (start atau end).
     */
    private static void printDebugLog(DTNHost fromNode, DTNHost toNode, String action) {
        // Ambil ID dari kedua node
        String fromNodeId = String.valueOf(fromNode.getAddress());
        String toNodeId = String.valueOf(toNode.getAddress());

        System.out.println("[DEBUG] -----------------------------------------");
        System.out.println("[DEBUG] " + action + " Koneksi antara node " + fromNodeId + " dan " + toNodeId);

        // Cek riwayat koneksi jika aksi "end"
        if (action.equals("end")) {
            double previousDuration = ConnectionDuration.getTotalConnectionDuration(fromNode, toNode);
            System.out.println("[DEBUG] Durasi sebelumnya: " + previousDuration + " detik.");
        }

        // Waktu dan durasi koneksi
        System.out.println("[DEBUG] Waktu sekarang: " + SimClock.getTime() + " detik.");
        System.out.println("[DEBUG] -----------------------------------------");
    }



    public static void printConnectionHistory(DTNHost node) {
        // Periksa apakah node ada dalam history
        if (connectionHistory.containsKey(node)) {
            // Iterasi semua koneksi yang dimiliki oleh node
            for (Map.Entry<DTNHost, ConnectionDuration> subEntry : connectionHistory.get(node).entrySet()) {
                // Cetak informasi koneksi untuk node tertentu
                System.out.println("[DEBUG] Koneksi dari " + node.getAddress() + " ke " + subEntry.getKey().getAddress() + " durasi: " + subEntry.getValue().getDuration() + " detik.");
            }
        } else {
            System.out.println("[DEBUG] Tidak ada riwayat koneksi untuk node " + node.getAddress());
        }
    }

    public void printConnectionInfo(DTNHost host, DTNHost neighbor) {
        System.out.println("===============================");
        System.out.println("Connection Info:");
        System.out.println("From Node: " + host.getAddress());
        System.out.println("To Node: " + neighbor.getAddress());
        System.out.println("Start Time: " + startTime);

        // Jika endTime masih -1, koneksi masih aktif
        if (endTime == -1) {
            System.out.println("End Time: Still Active");
            // Menghitung durasi aktif hingga waktu sekarang
            System.out.println("Current Duration: " + (SimClock.getTime() - startTime) + " seconds.");
        } else {
            // Jika koneksi sudah selesai
            System.out.println("End Time: " + endTime);
            // Menghitung durasi koneksi
            System.out.println("Final Duration: " + getDuration() + " seconds.");
        }
        System.out.println("===============================");
    }

    public DTNHost getFromNode() {
        return fromNode;
    }
    public DTNHost getToNode() {
        return toNode;
    }

}
