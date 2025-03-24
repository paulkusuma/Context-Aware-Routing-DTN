package routing.contextAware.ENS;

import core.DTNHost;
import  core.SimClock;

import java.util.HashMap;
import java.util.Map;


public class ConnectionDuration {
    private DTNHost fromNode;
    private DTNHost toNode;
    private double startTime;
    private double endTime;

    // Menggunakan Map untuk menyimpan riwayat koneksi antar node
    private static Map<DTNHost, Map<DTNHost, ConnectionDuration>> connectionHistory = new HashMap<>();

    public ConnectionDuration(DTNHost fromNode, DTNHost toNode) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.startTime = SimClock.getTime();
        this.endTime = -1; //-1 berarti koneksi masih aktif
    }

    public void endConnection() {
        this.endTime = SimClock.getTime();
        connectionHistory.putIfAbsent(fromNode, new HashMap<>()); //cek history from node, jika tidak ada buat baru
        connectionHistory.get(fromNode).put(toNode, this);
    }


    public double getDuration() {
        if (endTime == -1) {
            return SimClock.getTime() - startTime; //Jika masih aktif hitung sementara
        }
        return endTime - startTime; //jika sudah selesai hitung duration final
    }

    //Mengambil connectionDuration berdasarkan  fromnode dan tonode
    public static ConnectionDuration getConnection(DTNHost fromNode, DTNHost toNode) {
        return connectionHistory.getOrDefault(fromNode, new HashMap<>()).get(toNode);
    }

    // Menambahkan metode printConnectionInfo untuk menampilkan informasi koneksi
    public void printConnectionInfo() {
        System.out.println("Connection Info:");
        System.out.println("From Node: " + fromNode.getAddress());
        System.out.println("To Node: " + toNode.getAddress());
        System.out.println("Start Time: " + startTime);
        System.out.println("End Time: " + (endTime == -1 ? "Still Active" : endTime));
        System.out.println("Duration: " + getDuration() + " seconds.");
    }

    // Menambahkan metode untuk menampilkan seluruh riwayat koneksi
    public static void printAllConnections() {
        System.out.println("All Connection History:");
        connectionHistory.forEach((fromNode, toMap) -> {
            toMap.forEach((toNode, connection) -> {
                System.out.println("From: " + fromNode.getAddress() + " to: " + toNode.getAddress());
                connection.printConnectionInfo();
            });
        });
    }

    public DTNHost getFromNode() {
        return fromNode;
    }
    public DTNHost getToNode() {
        return toNode;
    }

}
