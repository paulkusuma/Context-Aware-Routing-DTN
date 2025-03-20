package routing.contextAware;

import core.DTNHost;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import routing.ActiveRouter;
import routing.contextAware.SocialCharcteristic.Popularity;
import routing.contextAware.SocialCharcteristic.TieStrength;

import java.util.List;


public class ContextAwareNeighborEvaluator {


    public static void evaluateNeighbors(DTNHost host, Popularity popularity, TieStrength tieStrength) {
        List<DTNHost> neighbors = ContextAwareRLRouter.getNeighbors(host, popularity, tieStrength);

        // Ambil FCL langsung dari ContextAwareRLRouter
        FIS FclContextAware = ((ContextAwareRLRouter) host.getRouter()).getFIS();

        // Pastikan FCL berhasil dimuat
        if (FclContextAware == null) {
            System.out.println("Error: FCL tidak tersedia!");
            return;
        }

        // Ambil function block dari FCL
        FunctionBlock fb = FclContextAware.getFunctionBlock(null);

        // Variabel untuk menyimpan node dengan tfOpp tertinggi
        DTNHost bestNeighbor = null;

        double highestTfOpp = -Double.MAX_VALUE; // Inisialisasi dengan nilai yang sangat kecil

        for (DTNHost neighbor : neighbors) {
            // Dapatkan nilai popularitas node tetangga
            double centrality = popularity.getPopularity(neighbor);

            //Nilai TieStrength
            double tieStrengthValue = tieStrength.getTieStrength(host, neighbor);

            // Ambil router dari node tetangga
            ActiveRouter router = (ActiveRouter) neighbor.getRouter();

            double energy = 0.0;
            // Cek apakah router memiliki model energi
            if (router instanceof ContextAwareRLRouter CARouter) {
                energy = CARouter.getEnergyModel().getEnergy(); // Akses model energi dari router
            }

            int bufferSize = router.getFreeBufferSize();

            fb.setVariable("energyNeighbor", energy);
            fb.setVariable("bufferNeighbor", bufferSize);
            fb.setVariable("centralityNeighbor", centrality);
            fb.setVariable("tieStrengthNeighbor", tieStrengthValue);

            //Evaaluasi COG Ability Node dan Social Importance
            fb.evaluate();

            double AbilityNode=fb.getVariable("ABILITY_NODE").getValue();
            double SocialImportance=fb.getVariable("SOCIAL_IMPORTANCE").getValue();

            fb.setVariable("ABILITY", AbilityNode);
            fb.setVariable("SOCIAL", SocialImportance);
            //Evaluasi COG Nilai Transfer Opportunity
            fb.evaluate();
            double tfOpp=fb.getVariable("TRANSFER_OPPORTUNITY").getValue();

            if (tfOpp > highestTfOpp) {
                highestTfOpp = tfOpp;
                bestNeighbor = neighbor;
            }

            // Setelah mengevaluasi semua tetangga, pilih node terbaik sebagai relay
            if (bestNeighbor != null) {
//                System.out.println("Node terbaik sebagai relay: " + bestNeighbor.getAddress() + " dengan nilai tfOpp: " + highestTfOpp);

                // Mengirimkan node relay yang dipilih ke router ContextSprayAndWaitRouter
                ((ContextSprayAndWaitRouter) host.getRouter()).setRelayNode(bestNeighbor);

            } else {
                System.out.println("Tidak ada node yang cocok untuk dijadikan relay.");
            }

        }
    }
}
