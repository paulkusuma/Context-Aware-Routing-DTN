package routing.contextAware.DensityMCopies;

import routing.contextAware.ENS.EncounteredNodeSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;



public class NetworkDensityCalculator {

    private static final Random random = new Random();
    private static int copies;

    /**
     * Menghitung contextual density dari dua ENS (host & neighbor).
     * Densitas ini digunakan sebagai estimasi jumlah node aktif di area saat ini,
     * untuk mengontrol jumlah salinan pesan seperti dijelaskan dalam "Proposed Copy Control Mechanism".
     * @param totalNodes  Jumlah total node di jaringan (misal: 50)
     * @param hostENS     ENS dari node utama (host)
     * @param neighborENS ENS dari node tetangga (neighbor)
     * @param hostId      ID dari node host (opsional, bisa null jika tidak ingin dihitung)
     * @param neighborId  ID dari node neighbor (opsional, bisa null)
     * @return nilai contextual density (0.0 - 1.0)
     */
    public static double calculateNodeDensity(int totalNodes, EncounteredNodeSet hostENS, EncounteredNodeSet neighborENS, String hostId,
                                              String neighborId) {
        Set<String> uniqueNodeIds = new HashSet<>();

        if (hostENS != null) {
            hostENS.removeOldEncounters();
            uniqueNodeIds.addAll(hostENS.getAllNodeIds());
        }

        if (neighborENS != null) {
            neighborENS.removeOldEncounters();
            uniqueNodeIds.addAll(neighborENS.getAllNodeIds());
        }

        // Tambahkan hostId dan neighborId jika tidak null dan tidak kosong
        if (hostId != null && !hostId.isEmpty()) {
            uniqueNodeIds.add(hostId);
        }

        if (neighborId != null && !neighborId.isEmpty()) {
            uniqueNodeIds.add(neighborId);
        }

        // Hindari pembagian dengan nol
        if (totalNodes <= 0) return 0.0;

        double density = (double) uniqueNodeIds.size() / totalNodes;

//        System.out.println("[DEBUG] Node Density: " + uniqueNodeIds.size() + "/" + totalNodes + " = " + density);

        return density;
    }

    /**
     * Menentukan jumlah salinan pesan berdasarkan kepadatan node.
     * Menentukan jumlah salinan pesan (L) berdasarkan node density.
     * Sesuai dengan paper, jumlah L dikontrol berdasarkan kepadatan area
     * untuk mengurangi overhead di area padat dan meningkatkan delivery di area jarang.
     * Rentang nilai random digunakan untuk memberikan fleksibilitas.
     * @param density Kepadatan jaringan
     * @return Jumlah salinan pesan
     */
    public static int calculateCopiesBasedOnDensity(double density) {
        if (density > 0.7) {
            return random.nextInt(3) + 1;  // Rentang: 1 - 3
        } else if (density > 0.3) {
            return random.nextInt(4) + 4;  // Rentang: 4 - 7
        } else {
            return random.nextInt(5) + 8;  // Rentang: 8 - 12
        }
    }
}
