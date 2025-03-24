package routing.contextAware;

import routing.contextAware.ENS.EncounterManager;
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

        //Ambil semua node dari nodeI
        Set<Integer> encounterNodesI = encounterManagerI.getEncounteredNodeSet().getEncounters().keySet();

        //Ambil semua nodde dari nodeJ
        Set<Integer> encounterNodesJ = encounterManagerJ.getEncounteredNodeSet().getEncounters().keySet();

        // Set untuk menyimpan node unik dari ENSI
        Set<Integer>uniqueNodes = new HashSet<>();

        // Menambahkan semua node dari encounterNodesI dan encounterNodesJ
        // Dengan addAll() kita menggabungkan dua set tersebut
        uniqueNodes.addAll(encounterNodesI);
        uniqueNodes.addAll(encounterNodesJ);

        // Menghitung kepadatan node dan memastikan tidak ada duplikasi
        return (double) uniqueNodes.size() / totalNodes;

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
