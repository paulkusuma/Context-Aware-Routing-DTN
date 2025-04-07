package routing.contextAware.ENS;

import core.DTNHost;
import core.SimClock;
import routing.contextAware.ContextAwareRLRouter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EncounteredNodeSet {

    private Map<String, EncounteredNode> ensTable = new HashMap<>();

    // Menambahkan atau memperbarui informasi ENS
    public void updateENS(DTNHost host, DTNHost neighbor, String nodeId, long encounterTime, double remainingEnergy, int bufferSize, long connectionDuration) {
        String myId = String.valueOf(host.getAddress()); // ID host
        String neighborId = String.valueOf(neighbor.getAddress()); // ID neighbor

        // Logging untuk debugging
        System.out.println("[TRACE] updateENS dipanggil oleh " + myId + " untuk nodeId: " + nodeId);

        // Cegah node menyisipkan dirinya sendiri (baik eksplisit maupun implisit)
        if (!nodeId.equals(myId)) {
            EncounteredNode newNode = new EncounteredNode(nodeId, encounterTime, remainingEnergy, bufferSize, connectionDuration);
            updateOrInsert(nodeId, newNode, myId);
        }


//
//        EncounteredNode newNode = new EncounteredNode(nodeId, encounterTime, remainingEnergy, bufferSize, connectionDuration);
//
//        // Update ENS milik host (tentang neighbor)
//        updateOrInsert(neighborId, newNode);
//
//        // Update ENS milik neighbor (tentang host)
//        EncounteredNode selfNode = new EncounteredNode(myId, encounterTime, remainingEnergy, bufferSize, connectionDuration);
//        updateOrInsert(myId, selfNode);

    }

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
            if (existingNode == null || newNode.isMoreRelevant(existingNode)) {
                return newNode;
            }
            return existingNode;
        });
    }

    /**
     * Menggabungkan ENS dari node lain
     */
    public void mergeENS(DTNHost host, EncounteredNodeSet otherENS, long currentTime, DTNHost neighbor) {
        if (otherENS == null || otherENS.ensTable.isEmpty()) return;

        String myId = String.valueOf(host.getAddress());
        String neighborId = String.valueOf(neighbor.getAddress());

//        if (!neighborId.equals(myId)) {
//            ensTable.putIfAbsent(neighborId, new EncounteredNode(neighborId, currentTime, 0, 0, 0));
//        }
        // Gabungkan ENS lain (kecuali ID sendiri)
        otherENS.ensTable.forEach((key, newNode) -> {
            if (!key.equals(myId)) {
                ensTable.compute(key, (k, existingNode) -> {
                    if (existingNode == null || newNode.isMoreRelevant(existingNode)) {
                        return newNode.clone(); // pakai clone untuk safety
                    }
                    return existingNode;
                });
            } else{
                System.out.println("[WARNING] Merge ENS diabaikan karena key == myId: " + myId);
            }
        });

//        cleanSelfFromENS(host);
    }

    public EncounteredNodeSet clone() {
        EncounteredNodeSet cloned = new EncounteredNodeSet();
        for (Map.Entry<String, EncounteredNode> entry : this.ensTable.entrySet()) {
            cloned.ensTable.put(entry.getKey(), entry.getValue().clone());
        }
        return cloned;
    }

    public void exchangeWith(EncounteredNodeSet otherENS, DTNHost self, DTNHost peer, long currentTime) {
        // Clone ENS lawan agar tidak mengubah aslinya
        EncounteredNodeSet otherClone = otherENS.clone();

        // Hapus ID si pemilik ENS (peer) dari salinan, supaya gak nyisipin data diri sendiri
        otherClone.removeEncounter(String.valueOf(peer.getAddress()));

        // Gabungkan ENS hasil filter ke ENS kita
        this.mergeENS(self, otherClone, currentTime, peer);
    }

    // Menghapus ENS berdasarkan NodeID jika koneksi terputus
    public void removeEncounter(String nodeId) {
        if (ensTable.remove(nodeId) != null) {
            System.out.println("[INFO] ENS dihapus untuk NodeID: " + nodeId);
        }
    }

    // Menghapus ENS yang sudah lebih dari TTL
    public void removeOldEncounters() {
        ensTable.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }


    public void cleanSelfFromENS(DTNHost host) {
        String myID = String.valueOf(host.getAddress()); // ID dari node yang memproses

        EncounteredNode removed = ensTable.remove(myID);
        // Hapus ID sendiri dari tabel ENS jika ada
        if (removed != null) {
            System.out.println("[INFO] ENS (self) dihapus untuk NodeID: " + myID);
        }
    }

    public boolean isEmpty() {
        return ensTable.isEmpty();
    }


    // Mendapatkan informasi ENS berdasarkan NodeID
    public EncounteredNode getENS(String nodeId) {
        return ensTable.get(nodeId);
    }

    public void printENS(String hostId) {
        if (this.ensTable.isEmpty()) {
            System.out.println("  (ENS KOSONG)");
        } else {
            for (Map.Entry<String, EncounteredNode> entry : ensTable.entrySet()) {
                EncounteredNode node = entry.getValue();
                System.out.printf("  NodeID: %-5s | Encounter Time: %-5d | Duration: %-5ds\n",
                        node.getNodeId(),
                        node.getEncounterTime(),
                        node.getConnectionDuration());
            }
        }
        System.out.println("============================================");
    }

    public void printEncounterLog(DTNHost host, String neighborId, EncounteredNodeSet neighborENS) {
        System.out.println("============================================");
        System.out.printf("Node %s bertemu dengan Node %s\n", host.getAddress(), neighborId);
        System.out.println("\nISI ENS NODE " + host.getAddress());
        this.printENS(String.valueOf(host.getAddress()));
        System.out.println("\nISI ENS NODE " + neighborId);
        neighborENS.printENS(neighborId);
        System.out.println("============================================\n");
    }

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

    //    }
//
////    // Menggabungkan ENS dari node lain
////    public void mergeENS(DTNHost host, EncounteredNodeSet otherENS, long currentTime, DTNHost neighbor) {
////        if (otherENS == null || otherENS.ensTable.isEmpty()) {
////            return; // Jika otherENS kosong, tidak perlu diproses
////        }
////
////        String myID = String.valueOf(host.getAddress()); // Ambil ID host saat ini
////        String neighborId = String.valueOf(neighbor.getAddress());
////
////
////        // Proses ENS dari neighbor ke host
////        otherENS.ensTable.forEach((key, newNode) -> {
////            if (!key.equals(myID) && !key.equals(neighborId)) { // Hindari menambahkan ID sendiri & neighbor
////                EncounteredNode existingNode = ensTable.get(key);
////                if (existingNode == null || newNode.isMoreRelevant(existingNode)) {
////                    ensTable.put(key, newNode);
////                }
////            }
////        });
////
////        // Proses ENS dari host ke neighbor
////        this.ensTable.forEach((key, existingNode) -> {
////            if (!key.equals(myID) && !key.equals(neighborId)) { // Hindari menambahkan ID sendiri & neighbor
////                EncounteredNode neighborExistingNode = otherENS.ensTable.get(key);
////                if (neighborExistingNode == null || existingNode.isMoreRelevant(neighborExistingNode)) {
////                    otherENS.ensTable.put(key, existingNode);
////                }
////            }
////        });
////
////        // Pastikan hanya ID Neighbor yang ada di ENS Host
////        if (!ensTable.containsKey(neighborId)) {
////            ensTable.put(neighborId, new EncounteredNode(neighborId, currentTime, 0, 0, 0));
////            System.out.println("[INFO] Menambahkan Node " + neighborId + " ke ENS Node " + myID);
////        }
////
////        // Pastikan hanya ID Host yang ada di ENS Neighbor
////        if (!otherENS.ensTable.containsKey(myID)) {
////            otherENS.ensTable.put(myID, new EncounteredNode(myID, currentTime, 0, 0, 0));
////            System.out.println("[INFO] Menambahkan Node " + myID + " ke ENS Node " + neighborId);
////        }
////
//////        this.cleanSelfFromENS(host);
//////        otherENS.cleanSelfFromENS(neighbor);
////    }
//
////    // Menggabungkan ENS dari node lain
//    public void mergeENS(DTNHost host, EncounteredNodeSet otherENS, long currentTime, DTNHost neighbor) {
//        if (otherENS == null || otherENS.ensTable.isEmpty()) {
//            return; // Jika otherENS kosong, tidak perlu diproses
//        }
//
//
//        String myID = String.valueOf(host.getAddress()); // Ambil ID host saat ini
//        String neigborId = String.valueOf(neighbor.getAddress());
////
////        // Pastikan node yang bertemu langsung masuk ke ENS masing-masing
////       otherENS.ensTable.putIfAbsent(myID, new EncounteredNode(myID, currentTime, 0,0,0));
////       ensTable.putIfAbsent(neigborId, new EncounteredNode(neigborId, currentTime, 0,0,0));
//
//        // Hanya tambahkan node tetangga, bukan dirinya sendiri
//        if (!ensTable.containsKey(neigborId)) {
//            ensTable.put(neigborId, new EncounteredNode(neigborId, currentTime, 0,0,0));
//        }
////        ensTable.computeIfAbsent(neigborId, id -> new EncounteredNode(id, currentTime, 0,0,0));
//
//        otherENS.ensTable.forEach((key, newNode) -> {
//            if (!key.equals(myID)) { // Pastikan tidak menambahkan ID sendiri
//                ensTable.compute(key, (existingKey, existingNode) -> {
//                    // Jika tidak ada data sebelumnya, langsung pakai newNode
//                    if (existingNode == null) {
//                        // Jika node baru, tambahkan dengan waktu saat ini
//                        return new EncounteredNode(key, currentTime, 0, 0, 0);
//                    }
//                    // Jika newNode lebih relevan, gunakan newNode
//                    return existingNode.isMoreRelevant(newNode) ? existingNode : newNode;
//                });
//            }
//        });
//        cleanSelfFromENS(host);
//
//    }
//    // Menggabungkan ENS dari node lain
////    public void mergeENS(DTNHost host, EncounteredNodeSet otherENS) {
////
////        String myID = String.valueOf(host.getAddress()); // Ambil ID host saat ini
////        otherENS.ensTable.forEach((key, newNode) -> {
////                    if (!key.equals(myID)) {
////                        ensTable.merge(key, newNode, (existing, newInfo) ->
////                                existing.isMoreRelevant(newInfo) ? existing : newInfo
////                        );
////                    }
////        });
////        cleanSelfFromENS(host);
////
////    }
//
////    // Menggabungkan ENS dari node lain
////    public void mergeENS(EncounteredNodeSet otherENS) {
////        otherENS.ensTable.forEach((key, newNode) ->
////
////                ensTable.merge(key, newNode, (existing, newInfo) ->
////                        existing.isMoreRelevant(newInfo) ? existing : newInfo
////                )
////        );
////    }

