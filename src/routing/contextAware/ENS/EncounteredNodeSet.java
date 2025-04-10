package routing.contextAware.ENS;

import core.DTNHost;
import core.SimClock;
import routing.contextAware.ContextAwareRLRouter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Kelas EncounteredNodeSet menyimpan informasi node-node yang pernah ditemui oleh suatu node
 * dalam bentuk tabel ENS (Encountered Node Set). Informasi ini digunakan untuk pengambilan
 * keputusan dalam routing berbasis konteks di jaringan Opportunistic.
 * Setiap entri ENS menyimpan data relevan seperti waktu pertemuan terakhir, energi sisa,
 * ukuran buffer, dan durasi koneksi saat pertemuan dengan node tersebut.
 * Kelas ini menyediakan metode untuk memperbarui ENS, menggabungkan ENS dari node lain,
 * menghapus entri yang sudah usang, serta melakukan pertukaran informasi ENS antar node.
 *
 * @author Pauwerfull
 */
public class EncounteredNodeSet {

    private Map<String, EncounteredNode> ensTable = new HashMap<>();

    /**
     * Menambahkan atau memperbarui informasi ENS untuk node tertentu.
     *
     * @param host              Node saat ini
     * @param neighbor          Node tetangga
     * @param nodeId            ID node yang ingin dimasukkan ke ENS
     * @param encounterTime     Waktu pertemuan (detik simulasi)
     * @param remainingEnergy   Energi sisa node dalam persen
     * @param bufferSize        Ukuran buffer dalam MB
     * @param connectionDuration Durasi koneksi saat pertemuan (detik)
     */
    public void updateENS(DTNHost host, DTNHost neighbor, String nodeId, long encounterTime, double remainingEnergy, int bufferSize, long connectionDuration) {
        String myId = String.valueOf(host.getAddress()); // ID host
        // Logging untuk debugging
        System.out.println("[TRACE] updateENS dipanggil oleh " + myId + " untuk nodeId: " + nodeId);
        // Cegah node menyisipkan dirinya sendiri (baik eksplisit maupun implisit)
        if (!nodeId.equals(myId)) {
            EncounteredNode newNode = new EncounteredNode(nodeId, encounterTime, remainingEnergy, bufferSize, connectionDuration);
            updateOrInsert(nodeId, newNode, myId);
        }
    }

    /**
     * Menambahkan entri baru ke ENS atau memperbarui jika informasi lebih relevan.
     *
     * @param nodeId  ID node yang akan dimasukkan atau diperbarui dalam ENS
     * @param newNode Objek EncounteredNode dengan informasi baru
     * @param myId    ID dari node pemilik ENS
     */
    private void updateOrInsert(String nodeId, EncounteredNode newNode, String myId) {

        if (nodeId.equals(myId)) {
            System.out.println("[BUG DETECTED] Node " + myId + " menyisipkan dirinya sendiri sebagai EncounteredNode!");
        }
        if (ensTable.containsKey(nodeId)) {
            System.out.println("[DEBUG] Update ENS untuk node: " + nodeId);
        } else {
            System.out.println("[DEBUG] Tambah ENS baru: " + nodeId);
        }
        if (nodeId.equals("your node id here")) {
            System.out.println("[WARNING] Node menyisipkan dirinya sendiri ke ENS!");
        }


        ensTable.compute(nodeId, (key, existingNode) -> {
            if (existingNode == null || newNode.isMoreRelevantThan(existingNode)) {
                return newNode;
            }
            return existingNode;
        });
    }

    /**
     * Menggabungkan ENS dari node lain, menyaring ID sendiri agar tidak dimasukkan.
     *
     * @param host        Node pemilik ENS
     * @param otherENS    ENS dari node lain yang akan digabungkan
     * @param currentTime Waktu simulasi saat ini
     * @param neighbor    Node tetangga (pengirim ENS)
     */
    public void mergeENS(DTNHost host, EncounteredNodeSet otherENS, long currentTime, DTNHost neighbor) {
        if (otherENS == null || otherENS.ensTable.isEmpty()) return;

        String myId = String.valueOf(host.getAddress());

        // Gabungkan ENS lain (kecuali ID sendiri)
        otherENS.ensTable.forEach((key, newNode) -> {
            if (!key.equals(myId)) {
                ensTable.compute(key, (k, existingNode) -> {
                    if (existingNode == null || newNode.isMoreRelevantThan(existingNode)) {
                        return newNode.clone(); // pakai clone untuk safety
                    }
                    return existingNode;
                });
            } else{
                System.out.println("[WARNING] Merge ENS diabaikan karena key == myId: " + myId);
            }
        });
    }

    /**
     * Mengkloning ENS ke instance baru.
     *
     * @return Salinan dari ENS saat ini
     */
    public EncounteredNodeSet clone() {
        EncounteredNodeSet cloned = new EncounteredNodeSet();
        for (Map.Entry<String, EncounteredNode> entry : this.ensTable.entrySet()) {
            cloned.ensTable.put(entry.getKey(), entry.getValue().clone());
        }
        return cloned;
    }

    /**
     * Menukar ENS antar node dengan menghindari penyisipan data diri sendiri.
     *
     * @param otherENS     ENS milik node lawan bicara
     * @param self         Node pemilik ENS saat ini
     * @param peer         Node lawan bicara
     * @param currentTime  Waktu simulasi saat ini
     */
    public void exchangeWith(EncounteredNodeSet otherENS, DTNHost self, DTNHost peer, long currentTime) {
        // Clone ENS lawan agar tidak mengubah aslinya
        EncounteredNodeSet otherClone = otherENS.clone();
        // Hapus ID si pemilik ENS (peer) dari salinan, supaya gak nyisipin data diri sendiri
        otherClone.removeEncounter(String.valueOf(peer.getAddress()));
        // Gabungkan ENS hasil filter ke ENS kita
        this.mergeENS(self, otherClone, currentTime, peer);
    }


    /**
     * Menghapus satu entri ENS berdasarkan ID node.
     *
     * @param nodeId ID node yang ingin dihapus dari ENS
     */
    public void removeEncounter(String nodeId) {
        if (ensTable.remove(nodeId) != null) {
            System.out.println("[INFO] ENS dihapus untuk NodeID: " + nodeId);
        }
    }

    /**
     * Menghapus seluruh entri ENS yang sudah kadaluarsa.
     */
    public void removeOldEncounters() {
        ensTable.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }


    /**
     * Menghapus ID diri sendiri dari ENS jika secara tidak sengaja tersisip.
     *
     * @param host Node pemilik ENS
     */
    public void cleanSelfFromENS(DTNHost host) {
        String myID = String.valueOf(host.getAddress()); // ID dari node yang memproses

        EncounteredNode removed = ensTable.remove(myID);
        // Hapus ID sendiri dari tabel ENS jika ada
        if (removed != null) {
            System.out.println("[INFO] ENS (self) dihapus untuk NodeID: " + myID);
        }
    }

    /**
     * Mengecek apakah ENS kosong.
     *
     * @return true jika ENS tidak memiliki entri
     */
    public boolean isEmpty() {
        return ensTable.isEmpty();
    }


    /**
     * Mengambil informasi ENS berdasarkan ID node.
     *
     * @param nodeId ID node yang ingin dicari di ENS
     * @return Objek EncounteredNode jika ditemukan, null jika tidak
     */
    public EncounteredNode getENS(String nodeId) {
        return ensTable.get(nodeId);
    }

    /**
     * Mencetak isi ENS milik node tertentu ke konsol.
     *
     * @param hostId ID dari node pemilik ENS yang akan ditampilkan
     */
    public void printENS(String hostId) {
        if (this.ensTable.isEmpty()) {
            System.out.println("  (ENS KOSONG)");
        } else {
            for (Map.Entry<String, EncounteredNode> entry : ensTable.entrySet()) {
                EncounteredNode node = entry.getValue();
                System.out.printf("  NodeID: %-5s | Encounter Time: %-5d | RemainingEnergy : %-5s |getBufferSize: %-5d | Duration: %-5ds\n",
                        node.getNodeId(),
                        node.getEncounterTime(),
                        node.getRemainingEnergy(),
                        node.getBufferSize(),
                        node.getConnectionDuration());
            }
        }
        System.out.println("============================================");
    }

    /**
     * Menampilkan log interaksi ENS saat dua node bertemu.
     *
     * @param host        Node pemilik ENS
     * @param neighborId  ID node tetangga yang sedang bertemu
     * @param neighborENS ENS milik node tetangga
     */
    public void printEncounterLog(DTNHost host, String neighborId, EncounteredNodeSet neighborENS) {
        System.out.println("============================================");
        System.out.printf("Node %s bertemu dengan Node %s\n", host.getAddress(), neighborId);
        System.out.println("\nISI ENS NODE " + host.getAddress());
        this.printENS(String.valueOf(host.getAddress()));
        System.out.println("\nISI ENS NODE " + neighborId);
        neighborENS.printENS(neighborId);
        System.out.println("============================================\n");
    }

    /**
     * Menampilkan isi ENS untuk keperluan debugging.
     *
     * @param host Node pemilik ENS
     */
    public void debugENS(DTNHost host) {
        System.out.println("[DEBUG] ENS milik Node:" + host.getAddress());
        if (ensTable.isEmpty()) {
            System.out.println("ENS kosong");
        } else {
            System.out.println("isi ENS :");
            for (Map.Entry<String, EncounteredNode> entry : ensTable.entrySet()) {
                System.out.println("NodeID: " + entry.getKey() + " -> " + entry.getValue());
            }
        }
    }
}



