package routing.contextAware.ENS;

import core.DTNHost;
import  core.SimClock;
import routing.testContext.EncounterManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Kelas ConnectionDuration digunakan untuk merekam durasi koneksi antara dua node.
 */
public class ConnectionDuration {
    private DTNHost fromNode;
    private DTNHost toNode;
    private double startTime;
    private double endTime;

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
    }

    /**
     * Memulai koneksi antara dua node.
     * @param fromNode Node pengirim.
     * @param toNode Node penerima.
     * @return Instance ConnectionDuration yang merepresentasikan koneksi tersebut.
     */
    public static ConnectionDuration startConnection(DTNHost fromNode, DTNHost toNode) {
        connectionHistory.putIfAbsent(fromNode, new HashMap<>());
        //Jika sudah ada kembalikan
        if(connectionHistory.get(fromNode).containsKey(toNode)){
            return connectionHistory.get(fromNode).get(toNode);
        }
        //buat koneksi baru
        ConnectionDuration newconnectionDuration = new ConnectionDuration(fromNode, toNode);
        connectionHistory.get(fromNode).put(toNode, newconnectionDuration);
        return newconnectionDuration;
    }


    /**
     * Mengakhiri koneksi dan mencatat waktu akhir koneksi.
     */
    public void endConnection() {
        this.endTime = SimClock.getTime();

        // Update ENS setelah koneksi berakhir
        EncounterManager encounterManager = new EncounterManager();
        encounterManager.updateConnectionDurationInENS(fromNode, toNode);
    }


    /**
     * Menghitung durasi koneksi.
     * @return Durasi koneksi dalam detik.
     */
    public double getDuration() {
        if (endTime == -1) {
            return SimClock.getTime() - startTime; //Jika masih aktif hitung sementara
        }
        return endTime - startTime; //jika sudah selesai hitung duration final
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


    public void printConnectionInfo() {
        System.out.println("===============================");
        System.out.println("Connection Info:");
        System.out.println("From Node: " + fromNode.getAddress());
        System.out.println("To Node: " + toNode.getAddress());
        System.out.println("Start Time: " + startTime);

        if (endTime == -1) {
            System.out.println("End Time: Still Active");
            System.out.println("Current Duration: " + (SimClock.getTime() - startTime) + " seconds.");
        } else {
            System.out.println("End Time: " + endTime);
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
