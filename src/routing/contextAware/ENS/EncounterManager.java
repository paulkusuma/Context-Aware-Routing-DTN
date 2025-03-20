package routing.contextAware.ENS;

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

    //agar thread-safe jika sistem berjalan secara paralel.
    private synchronized void mergeEncounterData(EncounterManager neighborManager) {
        if (neighborManager == null) return;
        for (Map.Entry<Integer, EncounterInfo> entry : neighborManager.getEncounteredNodeSet().getEncounters().entrySet()) {
            int nodeId = entry.getKey();
            EncounterInfo info = entry.getValue();

            // Cek apakah informasi lebih baru sebelum menambahkan
            if (isNewerEncounter(nodeId, info.MeetingTime)) {
                this.addEncounterToENS(nodeId, info.MeetingTime, info.RemainingBuffer, info.RemainingEnergy, info.connDuration);
            }
        }
    }

    // Bertukar ENS dengan EncounterManager lain
    public void exchangeENS(EncounterManager neighborENS) {
        mergeEncounterData(neighborENS);
    }

    // Menambahkan informasi terbaru dari tetangga ke ENS
    public void updateENSWithNeighborInfo(EncounterManager neighborManager) {
        mergeEncounterData(neighborManager);
    }

    // Mendapatkan jumlah encounter yang tersimpan
    public int getEncounterCount() {
        return encounteredNodeSet.getSize();
    }
}