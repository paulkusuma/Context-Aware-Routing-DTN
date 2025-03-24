package routing.contextAware.ENS;

import java.util.Map;

public class EncounteredNodeSet {
    private Map<Integer, EncounterInfo> EncounteredNodes;

    public EncounteredNodeSet(Map<Integer, EncounterInfo> encounteredNodes) {
        this.EncounteredNodes = encounteredNodes;
    }

    public void addEncounter(int nodeId, double meetingTime, int remainingBuffer, double remainingEnergy, double connectionDuration) {
        EncounteredNodes.put(nodeId, new EncounterInfo(meetingTime, remainingBuffer, remainingEnergy, connectionDuration));
    }

   // Menghapus encounter berdasarkan nodeId
    public void removeEncounter(int nodeId) {
        if (EncounteredNodes.containsKey(nodeId)) {
            EncounteredNodes.remove(nodeId);  // Menghapus encounter untuk node yang terputus
        } else {
            System.out.println("Node ID " + nodeId + " tidak ditemukan dalam ENS.");
        }
    }

    public Map<Integer, EncounterInfo> getEncounters() {
        return EncounteredNodes;
    }

    public int getEncounterCount(){
        return EncounteredNodes.size();
    }

}
