package routing.contextAware.ENS;

import core.SimClock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Kelas EncounteredNodeSet digunakan untuk menyimpan informasi tentang node-node
 * yang telah bertemu dalam jaringan. Data encounter ini digunakan dalam routing
 * berbasis konteks.
 */
public class EncounteredNodeSet {

    private Map<Integer, EncounterInfo> EncounteredNodes = new HashMap<>();
    private static final double EXPIRATION_TIME = 60.0; // 60 detik dalam unit waktu simulasi

    /**
     * Konstruktor untuk inisialisasi EncounteredNodeSet dengan data encounter yang diberikan.
     * @param encounteredNodes Map yang berisi data encounter awal.
     */
    public EncounteredNodeSet(Map<Integer, EncounterInfo> encounteredNodes) {
        EncounteredNodes = encounteredNodes;
    }

    /**
     * Menambahkan encounter baru ke dalam ENS.
     * @param nodeId ID node yang ditemui.
     * @param encounterInfo Informasi encounter yang mencakup waktu pertemuan, buffer, energi, dan durasi koneksi.
     */
    public void addEncounter(int nodeId, EncounterInfo encounterInfo ) {
        if (EncounteredNodes.containsKey(nodeId)) {
            System.out.println("⚠ Node " + nodeId + " sudah ada dalam ENS! Memperbarui info.");
        } else {
            System.out.println("➕ Menambahkan node " + nodeId + " ke ENS.");
        }

        EncounteredNodes.put(nodeId, encounterInfo);

//        System.out.println("Node " + nodeId + " ditambahkan ke ENS.");
    }

    /**
     * Memperbarui durasi koneksi untuk node yang sudah ada dalam ENS.
     * @param nodeId ID node yang ingin diperbarui.
     * @param newDuration Durasi koneksi baru dalam satuan waktu simulasi.
     */
    public void updateEncounterDuration(int nodeId, double newDuration) {
        if (EncounteredNodes.containsKey(nodeId)) {
            EncounterInfo encounter = EncounteredNodes.get(nodeId);
            encounter.connDuration = newDuration;
            encounter.lastSeenTime = SimClock.getTime(); // Perbarui waktu terakhir terlihat dengan waktu simulasi
        }
    }

    /**
     * Menghapus encounter berdasarkan nodeId dari ENS.
     * @param nodeId ID node yang ingin dihapus dari ENS.
     */
    public void removeEncounter(int nodeId) {
        System.out.println("ENS Saat Ini: " + EncounteredNodes.keySet());
        if (EncounteredNodes.containsKey(nodeId)) {
            EncounteredNodes.remove(nodeId);  // Menghapus encounter untuk node yang terputus
            System.out.println("Node " + nodeId + " berhasil dihapus dari ENS.");
        } else {
            System.out.println("Node ID " + nodeId + " tidak ditemukan dalam ENS.");
        }
        System.out.println("Setelah penghapusan: " + EncounteredNodes.keySet());
    }

    /**
     * Membersihkan encounter yang sudah lebih dari 60 detik dalam waktu simulasi.
     * Encounter yang kadaluwarsa akan dihapus dari ENS.
     */
    public void cleanOldEncounters() {
        double currentTime = SimClock.getTime(); // Ambil waktu simulasi saat ini
        System.out.println("Waktu simulasi saat ini: " + currentTime);

        Iterator<Map.Entry<Integer, EncounterInfo>> iterator = EncounteredNodes.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, EncounterInfo> entry = iterator.next();

            double age = currentTime - entry.getValue().lastSeenTime;
            System.out.println("Node " + entry.getKey() + " terakhir terlihat " + entry.getValue().lastSeenTime + " detik lalu (Usia: " + age + " detik)");

            if (currentTime - entry.getValue().lastSeenTime >= EXPIRATION_TIME) {
                iterator.remove(); // Hapus encounter yang sudah lebih dari 60 detik
                System.out.println("Node ID " + entry.getKey() + " dihapus karena sudah lebih dari 60 detik dalam simulasi.");
            }
        }
        System.out.println("ENS setelah pembersihan: " + EncounteredNodes.keySet());
    }

     /**
      * Mengembalikan daftar encounter yang tersimpan dalam ENS.
      * @return Map yang berisi informasi encounter dengan key sebagai nodeId.
     */
    public Map<Integer, EncounterInfo> getEncounters() {
        return EncounteredNodes;
    }

    /**
     * Mengembalikan jumlah node yang terdapat dalam ENS.
     * @return Jumlah encounter yang tersimpan dalam ENS.
     */
    public int getEncounterCount(){
        return EncounteredNodes.size();
    }

}
