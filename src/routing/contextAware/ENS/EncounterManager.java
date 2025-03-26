package routing.contextAware.ENS;

import core.DTNHost;
import core.SimClock;
import routing.contextAware.ContextAwareRLRouter;

import java.util.HashMap;
import java.util.Map;

/**
 * Kelas EncounterManager mengelola EncounteredNodeSet (ENS) untuk menyimpan informasi tentang node-node yang telah bertemu.
 * Kelas ini menangani penambahan, pembaruan, penghapusan, pertukaran, dan pembersihan ENS dalam jaringan Delay Tolerant Network (DTN).
 */
public class EncounterManager {

    private final EncounteredNodeSet encounteredNodeSet;
    private double lastCleanupTime = 0; // Waktu terakhir pembersihan

    /**
     * Konstruktor untuk EncounterManager yang menginisialisasi EncounteredNodeSet baru.
     */
    public EncounterManager() {
        encounteredNodeSet = new EncounteredNodeSet(new HashMap<>());
    }

    /**
     * Mengembalikan objek EncounteredNodeSet yang dikelola oleh EncounterManager.
     * @return EncounteredNodeSet yang berisi informasi encounter node.
     */
    public EncounteredNodeSet getEncounteredNodeSet() {
        return encounteredNodeSet;
    }


    /**
     * Menambahkan encounter baru ke dalam ENS.
     * @param nodeId ID node yang ditemui.
     * @param meetingTime Waktu pertemuan dalam simulasi.
     * @param remainingBuffer Kapasitas buffer yang tersisa.
     * @param remainingEnergy Energi yang tersisa.
     * @param connectionDuration Durasi koneksi dengan node yang ditemui.
     */
    public void addEncounterToENS(int nodeId, double meetingTime, int remainingBuffer, double remainingEnergy, double connectionDuration) {
        encounteredNodeSet.addEncounter(nodeId, new EncounterInfo(meetingTime, remainingBuffer, remainingEnergy, connectionDuration));
    }

    /**
     * Memperbarui durasi koneksi dalam ENS berdasarkan informasi dari koneksi antara dua node.
     * @param fromNode Node sumber.
     * @param toNode Node tujuan.
     */
    public void updateConnectionDurationInENS(DTNHost fromNode, DTNHost toNode) {
        ConnectionDuration conn = ConnectionDuration.getConnection(fromNode, toNode);
        if (conn != null) {
            int nodeId = toNode.getAddress();
            encounteredNodeSet.updateEncounterDuration(nodeId, conn.getDuration());
        }
    }

    /**
     * Mengecek apakah encounter dengan node tertentu lebih baru dibandingkan data yang ada.
     * @param nodeId ID node yang ingin diperiksa.
     * @param meetingTime Waktu pertemuan node yang baru.
     * @return True jika encounter baru lebih baru dari data yang ada, false jika tidak.
     */
    private boolean isNewerEncounter(int nodeId, double meetingTime) {
        return !encounteredNodeSet.getEncounters().containsKey(nodeId) ||
                meetingTime > encounteredNodeSet.getEncounters().get(nodeId).MeetingTime;
    }

    /**
     * Menukar ENS antara dua node untuk berbagi informasi tentang encounter yang mereka miliki.
     * @param neighborManager EncounterManager milik node tetangga.
     */
    public void exchangeENS(EncounterManager neighborManager) {
        // 1. Terima ENS dari node tetangga
        for (Map.Entry<Integer, EncounterInfo> entry : neighborManager.getEncounteredNodeSet().getEncounters().entrySet()) {
            int nodeId = entry.getKey();
            EncounterInfo info = entry.getValue();

            // 2. Periksa apakah encounter ini lebih baru dan tambahkan ke ENS N1
            if (isNewerEncounter(nodeId, info.MeetingTime)) {
                this.addEncounterToENS(nodeId, info.MeetingTime, info.RemainingBuffer, info.RemainingEnergy, info.connDuration);
            }
        }

        // 3. Kirim ENS N1 ke node tetangga
        for (Map.Entry<Integer, EncounterInfo> entry : encounteredNodeSet.getEncounters().entrySet()) {
            int nodeId = entry.getKey();
            EncounterInfo info = entry.getValue();

            // Kirim informasi ENS ke node tetangga jika lebih baru
            if (neighborManager.isNewerEncounter(nodeId, info.MeetingTime)) {
                neighborManager.addEncounterToENS(nodeId, info.MeetingTime, info.RemainingBuffer, info.RemainingEnergy, info.connDuration);
            }
        }
    }

    /**
     * Memperbarui ENS saat koneksi ke suatu node terputus dengan menghapus encounter terkait.
     * @param nodeId ID node yang koneksinya terputus.
     */
    public void updateENSOnConnectionDown(int nodeId) {
        // Menghapus encounter yang terputus (misalnya Node C)
        removeEncounterFromENS(nodeId);
    }

    /**
     * Menghapus encounter dari ENS berdasarkan ID node.
     * @param nodeId ID node yang akan dihapus dari ENS.
     */
    public void removeEncounterFromENS(int nodeId) {
        if (encounteredNodeSet.getEncounters().containsKey(nodeId)) {
            encounteredNodeSet.removeEncounter(nodeId);  // Menghapus encounter berdasarkan nodeId
        } else {
            System.out.println("Node ID " + nodeId + " tidak ditemukan saat menghapus dari ENS.");
        }
    }

    /**
     * Memeriksa apakah perlu melakukan pembersihan ENS berdasarkan waktu simulasi.
     * Pembersihan dilakukan setiap 60 detik dalam waktu simulasi.
     */
    public void checkAndCleanENS() {
        double currentTime = SimClock.getTime(); // Ambil waktu simulasi
        if (currentTime - lastCleanupTime >= 60.0) {
            encounteredNodeSet.cleanOldEncounters();
            lastCleanupTime = currentTime; // Perbarui waktu terakhir pembersihan
        }
    }

    /**
     * Mengembalikan jumlah encounter yang tersimpan dalam ENS.
     * @return Jumlah encounter yang ada di ENS.
     */
    public int getEncounterCount() {
        return encounteredNodeSet.getEncounterCount();
    }

    /**
     * Mencetak informasi ENS untuk node tertentu.
     * @param host Node yang ENS-nya akan dicetak.
     */
    public void printENS(DTNHost host) {
        // Mendapatkan router untuk host
        ContextAwareRLRouter router = (ContextAwareRLRouter) host.getRouter();

        // Mendapatkan EncounterManager dari router
        EncounterManager encounterManager = router.getEncounterManage();

        // Mendapatkan EncounteredNodeSet
        EncounteredNodeSet encounteredNodeSet = encounterManager.getEncounteredNodeSet();


        // Mencetak jumlah encounter yang ada
        System.out.println("Node " + host.getAddress() + " memiliki " + encounteredNodeSet.getEncounterCount() + " encounter(s):");

        // Mencetak setiap encounter
        for (Map.Entry<Integer, EncounterInfo> entry : encounteredNodeSet.getEncounters().entrySet()) {
            int nodeId = entry.getKey();
            EncounterInfo encounterInfo = entry.getValue();

            // Mencetak informasi untuk setiap encounter
            System.out.println("Node ID: " + nodeId + ", Meeting Time: " + encounterInfo.MeetingTime +
                    ", Remaining Buffer: " + encounterInfo.RemainingBuffer +
                    ", Remaining Energy: " + encounterInfo.RemainingEnergy +
                    ", Connection Duration: " + encounterInfo.connDuration);
        }
        System.out.println("====================");
    }
}