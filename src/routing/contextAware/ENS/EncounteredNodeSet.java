package routing.contextAware.ENS;

import core.DTNHost;
import core.SimClock;
import routing.contextAware.ContextAwareRLRouter;

import java.util.*;
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
    public void updateENS(DTNHost host, DTNHost neighbor, String nodeId, long encounterTime, double remainingEnergy, int bufferSize, long connectionDuration, double popularity) {
        String myId = String.valueOf(host.getAddress()); // ID host
        // Logging untuk debugging
//        System.out.println("[TRACE] updateENS dipanggil oleh " + myId + " untuk nodeId: " + nodeId);
        // Cegah node menyisipkan dirinya sendiri (baik eksplisit maupun implisit)
        if (!nodeId.equals(myId)) {
            EncounteredNode newNode = new EncounteredNode(nodeId, encounterTime, remainingEnergy, bufferSize, connectionDuration);
            newNode.setPopularity(popularity);

            // Jika node sudah ada, tambah encounter count-nya
            if (ensTable.containsKey(nodeId)) {
                EncounteredNode existingNode = ensTable.get(nodeId);
                existingNode.incrementEncounterCount();  // Menambah encounter count setiap kali update
            }

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
//            System.out.println("[BUG DETECTED] Node " + myId + " menyisipkan dirinya sendiri sebagai EncounteredNode!");
        }
        if (ensTable.containsKey(nodeId)) {
//            System.out.println("[DEBUG] Update ENS untuk node: " + nodeId);
        } else {
//            System.out.println("[DEBUG] Tambah ENS baru: " + nodeId);
        }
        if (nodeId.equals("your node id here")) {
//            System.out.println("[WARNING] Node menyisipkan dirinya sendiri ke ENS!");
        }

        EncounteredNode existingNode = ensTable.get(nodeId);

        if (existingNode == null) {
//            System.out.println("[DEBUG] Tambah ENS baru: " + nodeId);
            ensTable.put(nodeId, newNode);
            // Inisialisasi encounter count pada encounter pertama kali
            newNode.incrementEncounterCount();
        } else {
//            System.out.println("[DEBUG] Update ENS untuk node: " + nodeId);

            // Update encounter count jika node sudah ada
            existingNode.incrementEncounterCount();

            // Field yang *selalu* di-update
            existingNode.setEncounterTime(newNode.getEncounterTime());
            existingNode.setPopularity(newNode.getPopularity());

            // Update detail only if more relevant
            if (newNode.isMoreRelevantThan(existingNode)) {
                existingNode.setRemainingEnergy(newNode.getRemainingEnergy());
                existingNode.setBufferSize(newNode.getBufferSize());
                existingNode.setConnectionDuration(newNode.getConnectionDuration());
            }
        }

    }

    // Memperbarui durasi koneksi dari node yang terlibat dalam encounter
    public void updateConnectionDuration(String nodeId, long duration) {
        EncounteredNode encounteredNode = ensTable.get(nodeId);
        if (encounteredNode != null) {
            encounteredNode.updateConnectionDuration(duration);
        }
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
//                System.out.println("[WARNING] Merge ENS diabaikan karena key == myId: " + myId);
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
//            System.out.println("[INFO] NodeID: " + nodeId +" dihapus dari Tabel karena terputus");
        }
    }

    /**
     * Menghapus seluruh entri ENS yang sudah kadaluarsa.
     */
    public void removeOldEncounters() {

        ensTable.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired();
            if (expired) {
//                System.out.println("[INFO] Menghapus encounter kadaluarsa: NodeID " + entry.getKey());
            }
            return expired;
        });
//        ensTable.entrySet().removeIf(entry -> entry.getValue().isExpired());
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
//            System.out.println("[INFO] ENS (self) dihapus untuk NodeID: " + myID);
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

    public Map<String, EncounteredNode> getTable() {
        return this.ensTable;
    }


    public int countRecentEncounters(double currentTime, double timeWindow) {
        int count = 0;
        for (Map.Entry<String, EncounteredNode> entry : ensTable.entrySet()) {
            EncounteredNode node = entry.getValue();
            double lastSeen = node.getEncounterTime();
            double delta = currentTime - lastSeen;
            if (delta <= timeWindow) {
                count++;
//                System.out.printf("  [RECENT] Node %s: LastSeen %.1f detik lalu\n", entry.getKey(), delta);
            } else {
//                System.out.printf("  [EXPIRED] Node %s: LastSeen %.1f detik lalu\n", entry.getKey(), delta);
            }
        }
        return count;
    }


    /**
     * Mengembalikan jumlah encounter dengan target node dari ENS milik thisHost.
     *
     * @param neighbor Node yang ingin dicek encounter-nya
     * @return Jumlah encounter dengan nodeB, 0 jika tidak ditemukan
     */
    public int getEncounterCount(DTNHost neighbor) {
        String neighborId = String.valueOf(neighbor.getAddress());

        EncounteredNode data = ensTable.get(neighborId);

        if (data != null) {
//            System.out.println("[DEBUG] Encounter count untuk Node " + neighborId + " = " + data.getEncounterCount());
            return data.getEncounterCount();
        }

//        System.out.println("[DEBUG] Node " + neighborId + " tidak ditemukan dalam ENS.");
        return 0;
    }

    /**
     * Mengambil semua NodeID dari ENS sebagai himpunan (set).
     * @return Set berisi ID semua node yang pernah ditemui
     */
    public Set<String> getAllNodeIds() {
        return new HashSet<>(ensTable.keySet());
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
