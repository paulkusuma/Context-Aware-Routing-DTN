package routing.contextAware;

import core.DTNHost;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.rule.Rule;
import routing.contextAware.ContextSprayAndWaitRouter;
import java.util.List;
import routing.ActiveRouter;


public class NeighborEvaluator {


    public static void evaluateNeighbors(DTNHost host, Popularity popularity, TieStrength tieStrength) {
        List<DTNHost> neighbors = ContextSprayAndWaitRouter.getNeighbors(host, popularity, tieStrength);

//        System.out.println("Evaluating neighbors for node: " + host.getAddress());

        // Ambil FCL langsung dari ContextSprayAndWaitRouter
        FIS fclcontextaware = ((ContextSprayAndWaitRouter) host.getRouter()).getFclcontextaware();

        // Pastikan FCL berhasil dimuat
        if (fclcontextaware == null) {
            System.out.println("Error: FCL tidak tersedia!");
            return;
        }
//        } else{
//            System.out.println(fclcontextaware);
//        }

        // Ambil function block dari FCL
        FunctionBlock fb = fclcontextaware.getFunctionBlock(null);
        if (fb == null) {
            System.out.println("Error: Function Block tidak ditemukan dalam FCL.");
            return;
        }
//        }else{
//            System.out.printf("FCL Block tersedia!");
//            System.out.printf(fb.toString());
//        }

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
            if (router instanceof ContextSprayAndWaitRouter) {
                ContextSprayAndWaitRouter cswRouter = (ContextSprayAndWaitRouter) router;
                energy = cswRouter.getEnergyModel().getEnergy(); // Akses model energi dari router
            }

            int bufferSize = router.getFreeBufferSize();
//            int bufferSize = router.getBufferSize();

            fb.setVariable("energyNeighbor", energy);
            fb.setVariable("bufferNeighbor", bufferSize);
            fb.setVariable("centralityNeighbor", centrality);
            fb.setVariable("tieStrengthNeighbor", tieStrengthValue);
//            fb.setVariable("energyNeighbor", 20);
//            fb.setVariable("bufferNeighbor", 30);
//            fb.setVariable("centralityNeighbor", 0.2);
//            fb.setVariable("tieStrengthNeighbor", 0.5);

            fb.evaluate();

            double AbilityNode=fb.getVariable("ABILITY_NODE").getValue();
//            System.out.println("Nilai ABILITY_NODE setelah defuzzifikasi: " + AbilityNode);
//
//
            double SocialImportance=fb.getVariable("SOCIAL_IMPORTANCE").getValue();
//            System.out.println("Nilai SOCIAL_IMPORTANCE setelah defuzzifikasi: " + SocialImportance);
//
//            System.out.println("ABILITY_NODE: " + AbilityNode);
//            System.out.println("SOCIAL_IMPORTANCE: " + SocialImportance);

            fb.setVariable("ABILITY", AbilityNode);
            fb.setVariable("SOCIAL", SocialImportance);
            fb.evaluate();
            double tfOpp=fb.getVariable("TRANSFER_OPPORTUNITY").getValue();
//            System.out.println("Nilai TRANSFER_OPPORTUNITY setelah defuzzifikasi: " + tfOpp);

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

            // **Tambahkan cetakan untuk memastikan energy digunakan**
//            System.out.println("Neighbor " + neighbor.getAddress() + " | Energy: " + energy + " | Buffer Size: " + bufferSize);


//            // **Pastikan energy digunakan dalam evaluasi**
//            if (energy >= 50 && bufferSize >= 2) {
//                System.out.println("✔ Tetangga " + neighbor.getAddress() + " layak untuk dipilih!");
//            } else {
//                System.out.println("✘ Tetangga " + neighbor.getAddress() + " tidak layak.");
//            }
        }
    }
}
