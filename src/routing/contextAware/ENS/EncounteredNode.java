package routing.contextAware.ENS;


import core.SimClock;

public class EncounteredNode {
    String nodeId;
    long encounterTime;  // Waktu saat node ditemukan
    double remainingEnergy;  // Energi sisa dalam %
    int bufferSize;  // Kapasitas buffer node dalam MB
    long connectionDuration;  // Durasi koneksi dalam detik

    private static final double TTL = 360.0;  // Waktu kadaluarsa ENS dalam detik


    public EncounteredNode(String nodeId, long encounterTime, double remainingEnergy, int bufferSize, long connectionDuration) {
        this.nodeId = nodeId;
        update(encounterTime, remainingEnergy, bufferSize, connectionDuration);
    }

    public void update(long encounterTime, double remainingEnergy, int bufferSize, long connectionDuration) {
        this.encounterTime = encounterTime;
        this.remainingEnergy = remainingEnergy;
        this.bufferSize = bufferSize;
        this.connectionDuration = connectionDuration;
    }

    public boolean isMoreRelevant(EncounteredNode other) {
        // Jika encounterTime saat ini masih 0, maka data baru pasti lebih relevan
        if (this.encounterTime == 0) {
            return false;
        }

        // Jika encounterTime baru lebih besar atau sama, maka update
        if (other.encounterTime >= this.encounterTime) {
            // Jika encounterTime sama, cek connectionDuration
            if (other.encounterTime == this.encounterTime) {
                if (other.connectionDuration > this.connectionDuration) {
                    return false; // Durasi lebih lama, newInfo lebih relevan
                } else if (other.connectionDuration == this.connectionDuration) {
                    if (other.remainingEnergy > this.remainingEnergy) {
                        return false; // Baterai lebih besar, newInfo lebih relevan
                    } else if (other.remainingEnergy == this.remainingEnergy) {
                        if (other.bufferSize > this.bufferSize) {
                            return false; // Buffer lebih besar, newInfo lebih relevan
                        }
                    }
                }
            }
            return false; // Jika encounterTime lebih besar, update
        }

        return true; // Data lama lebih relevan
    }

    public EncounteredNode clone() {
        return  new EncounteredNode(this.nodeId, this.encounterTime, this.remainingEnergy, this.bufferSize, this.connectionDuration);

    }

    // Mengecek apakah ENS sudah kadaluarsa
    public boolean isExpired() {
        return (SimClock.getTime() - this.encounterTime) > TTL;
    }

    public String getNodeId() {
        return nodeId;
    }

    public long getEncounterTime() {
        return encounterTime;
    }

    public double getRemainingEnergy() {
        return remainingEnergy;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public long getConnectionDuration() {
        return connectionDuration;
    }

    public String toString() {
        return "NodeID: " + nodeId + ", Encounter Time: " + encounterTime +
                ", Energy: " + remainingEnergy + "%, Buffer: " + bufferSize + "MB, Duration: " + connectionDuration + "s";
    }
}
