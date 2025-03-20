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

    public Map<Integer, EncounterInfo> getEncounters() {
        return EncounteredNodes;
    }

    public int getSize(){
        return EncounteredNodes.size();
    }

}
