package routing.contextAware.ENS;


import core.SimClock;

/**
 * Representasi dari sebuah node yang pernah ditemui dalam jaringan opportunistic.
 * Menyimpan informasi relevan seperti waktu encounter, energi sisa, buffer, dan durasi koneksi.
 */
public class EncounteredNode {
    String nodeId;
    long encounterTime;  // Waktu saat node ditemukan
    double remainingEnergy;  // Energi sisa dalam %
    int bufferSize;  // Kapasitas buffer node dalam MB
    long connectionDuration;  // Durasi koneksi dalam detik

    private static final double TTL = 900.0;  // Waktu kadaluarsa ENS dalam detik

    private double popularity = 0.0;

    /**
     * Konstruktor untuk EncounteredNode.
     *
     * @param nodeId            ID dari node yang ditemui
     * @param encounterTime     Waktu saat encounter terjadi
     * @param remainingEnergy   Persentase energi sisa pada saat encounter (0-100)
     * @param bufferSize        Ukuran buffer node (dalam MB)
     * @param connectionDuration Durasi koneksi saat encounter (dalam detik)
     */
    public EncounteredNode(String nodeId, long encounterTime, double remainingEnergy, int bufferSize, long connectionDuration) {
        this.nodeId = nodeId;
        update(encounterTime, remainingEnergy, bufferSize, connectionDuration);
    }

    /**
     * Memperbarui nilai encounter node dengan informasi terbaru.
     *
     * @param encounterTime     Waktu saat encounter terjadi
     * @param remainingEnergy   Energi sisa
     * @param bufferSize        Kapasitas buffer
     * @param connectionDuration Durasi koneksi
     */
    public void update(long encounterTime, double remainingEnergy, int bufferSize, long connectionDuration) {
        this.encounterTime = encounterTime;
        this.remainingEnergy = remainingEnergy;
        this.bufferSize = bufferSize;
        this.connectionDuration = connectionDuration;
    }

    // Metode untuk memperbarui durasi koneksi setelah koneksi berakhir
    public void updateConnectionDuration(long duration) {
        this.connectionDuration += duration;
    }

    /**
     * Menentukan apakah informasi dari node ini lebih relevan dibandingkan node lain.
     * Relevansi dihitung berdasarkan urutan prioritas:
     * 1. Waktu encounter lebih baru
     * 2. Durasi koneksi lebih lama
     * 3. Energi sisa lebih besar
     * 4. Buffer lebih besar
     *
     * @param other Node lain yang akan dibandingkan
     * @return true jika informasi dari node ini lebih relevan
     */
    public boolean isMoreRelevantThan(EncounteredNode other) {
        if (this.encounterTime > other.encounterTime) return true;
        if (this.encounterTime == other.encounterTime) {
            if (this.connectionDuration > other.connectionDuration) return true;
            if (this.connectionDuration == other.connectionDuration) {
                if (this.remainingEnergy > other.remainingEnergy) return true;
                if (this.remainingEnergy == other.remainingEnergy) {
                    return this.bufferSize > other.bufferSize;
                }
            }
        }
        return false;
    }

    /**
     * Membuat salinan dari objek EncounteredNode ini.
     *
     * @return Objek EncounteredNode baru dengan nilai yang sama
     */
    public EncounteredNode clone() {
        EncounteredNode clone = new EncounteredNode(this.nodeId, this.encounterTime, this.remainingEnergy, this.bufferSize, this.connectionDuration);
        clone.setPopularity(this.popularity);
        return clone;
//
//        return  new EncounteredNode(this.nodeId, this.encounterTime, this.remainingEnergy, this.bufferSize, this.connectionDuration);

    }

    /**
     * Mengecek apakah informasi node ini sudah kadaluarsa berdasarkan TTL.
     *
     * @return true jika sudah kadaluarsa
     */
    public boolean isExpired() {
        return (SimClock.getTime() - this.encounterTime) > TTL;
    }

    // ===================== Getter ===================== //
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

    // ===================== setter ===================== //
    public void setEncounterTime(long encounterTime) {
        this.encounterTime = encounterTime;
    }

    public void setConnectionDuration(long connectionDuration) {
        this.connectionDuration = connectionDuration;
    }

    public void setRemainingEnergy(double remainingEnergy) {
        this.remainingEnergy = remainingEnergy;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    // ===================== Social Charateristic ===================== //
    public double getPopularity() { return popularity; }
    public void setPopularity(double popularity) {
        this.popularity = popularity;
    }

    public String toString() {
        return "NodeID: " + nodeId + ", Encounter Time: " + encounterTime +
                ", Energy: " + remainingEnergy + "%, Buffer: " + bufferSize + "MB, Duration: " + connectionDuration + "s";
    }
}
