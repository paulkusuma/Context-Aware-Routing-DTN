package routing.contextAware.ENS;

import core.DTNHost;
import routing.contextAware.ContextAwareRLRouter;
import core.Connection;  // Asumsikan ada class Connection untuk mengelola status koneksi

import java.util.HashMap;
import java.util.Map;

public class EncounterManager {

    private final EncounteredNodeSet encounteredNodeSet;

    public EncounterManager() {
        encounteredNodeSet = new EncounteredNodeSet(new HashMap<>());
    }

    // Getter untuk EncounteredNodeSet
    public EncounteredNodeSet getEncounteredNodeSet() {
        return encounteredNodeSet;
    }

    // Metode untuk menambahkan encounter baru ke EncounteredNodeSet
    public void addEncounterToENS(int nodeId, double meetingTime, int remainingBuffer, double remainingEnergy, double connectionDuration) {
        encounteredNodeSet.addEncounter(nodeId, meetingTime, remainingBuffer, remainingEnergy, connectionDuration);
    }

    private boolean isNewerEncounter(int nodeId, double meetingTime) {
        return !encounteredNodeSet.getEncounters().containsKey(nodeId) ||
                meetingTime > encounteredNodeSet.getEncounters().get(nodeId).MeetingTime;
    }
    // Fungsi untuk menukar ENS antara node N1 dan node C
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

            // 4. Kirim informasi ENS N1 ke node tetangga (neighbor)
            neighborManager.addEncounterToENS(nodeId, info.MeetingTime, info.RemainingBuffer, info.RemainingEnergy, info.connDuration);
        }
    }

    // Fungsi untuk menghapus encounter pada saat koneksi terputus
    public void updateENSOnConnectionDown(int nodeId) {
        // Menghapus encounter yang terputus (misalnya Node C)
        removeEncounterFromENS(nodeId);
    }

    // Menghapus encounter terkait dengan node tertentu
    public void removeEncounterFromENS(int nodeId) {
        if (encounteredNodeSet.getEncounters().containsKey(nodeId)) {
            encounteredNodeSet.removeEncounter(nodeId);  // Menghapus encounter berdasarkan nodeId
        } else {
            System.out.println("Node ID " + nodeId + " tidak ditemukan saat menghapus dari ENS.");
        }
    }


    // Mendapatkan jumlah encounter yang tersimpan
    public int getEncounterCount() {
        return encounteredNodeSet.getEncounterCount();
    }

    // Metode untuk mencetak ENS
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