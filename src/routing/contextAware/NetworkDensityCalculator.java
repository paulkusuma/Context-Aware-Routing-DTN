package routing.contextAware;

import routing.testContext.EncounterManager;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;



public class NetworkDensityCalculator {

    private static final Random random = new Random();

    /**
     * Menghitung kepadatan node berdasarkan Encountered Node Set (ENS).
     * @param totalNodes Jumlah total node dalam jaringan
     * @param encounterManagerI Instance EncounterManager untuk node pertama
     * @param encounterManagerJ Instance EncounterManager untuk node kedua (neighbor)
     * @return Node density sebagai rasio
     */
    public static double CalculateNodeDensity(int totalNodes, EncounterManager encounterManagerI, EncounterManager encounterManagerJ) {
        // Menggabungkan ENS dari dua node tanpa duplikasi
        Set<Integer> uniqueNodes = new HashSet<>(encounterManagerI.getEncounteredNodeSet().getEncounters().keySet());
        uniqueNodes.addAll(encounterManagerJ.getEncounteredNodeSet().getEncounters().keySet());

         double density = totalNodes == 0 ? 0 : (double) uniqueNodes.size() / totalNodes;
        return density;
    }

    /**
     * Menentukan jumlah salinan pesan berdasarkan kepadatan node.
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
