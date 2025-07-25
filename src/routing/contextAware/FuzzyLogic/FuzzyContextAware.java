package routing.contextAware.FuzzyLogic;

import core.DTNHost;
import core.DTNHost.*;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import routing.contextAware.ContextAwareRLRouter;

public class FuzzyContextAware {

    private FIS getFISFromHost(DTNHost host) {
        // Ambil FIS dari ContextAwareRLRouter
        ContextAwareRLRouter router = (ContextAwareRLRouter) host.getRouter();
        FIS fclContextAware = router.getFIS();  // Mengambil FIS dari router

        if (fclContextAware == null) {
            System.err.println("Error: FCL tidak tersedia!");
        }

        return fclContextAware;
    }

    private  FunctionBlock getFunctionBlock(FIS fis, String blockName) {
        FunctionBlock fb = fis.getFunctionBlock(blockName);

        if (fb == null) {
            System.err.println("Error: Function Block '" + blockName + "' tidak ditemukan dalam FCL!");
        }

        return fb;
    }

    // EVALUASI UNTUK MENDAPATKAN TRANSFER OPPORTUNITY
    public double evaluateNeighbor(DTNHost host, DTNHost neighbor, int freeBufferNeighbor, int remainingEnergyNeighbor, double popularityNeighbor, double tieStrengthNeighbor) {
        FIS fis = getFISFromHost(host);
        FunctionBlock fb = getFunctionBlock(fis, "FuzzyContextAware");
        // Debug log untuk memastikan variabel telah diset dengan benar
//        System.out.println("[DEBUG] Setting input variables for FuzzyContextAware:");
//        System.out.println("[DEBUG] bufferNeighbor: " + freeBufferNeighbor);
//        System.out.println("[DEBUG] energyNeighbor: " + remainingEnergyNeighbor);
//        System.out.println("[DEBUG] popularityNeighbor: " + popularityNeighbor);
//        System.out.println("[DEBUG] tieStrengthNeighbor: " + tieStrengthNeighbor);
        double bufferKb = freeBufferNeighbor /1024.0;

        final double MAX_BUFFER_KB = 20 * 1024.0; // 500 MB
        final double MAX_ENERGY = 500;
        double normBuffer = Math.min(bufferKb / MAX_BUFFER_KB, 1.0);;
        double normEnergy = Math.min(remainingEnergyNeighbor / MAX_ENERGY, 1.0);

        fb.setVariable("bufferNeighbor", normBuffer);
        fb.setVariable("energyNeighbor", normEnergy);
        fb.setVariable("popularityNeighbor", popularityNeighbor);
        fb.setVariable("tieStrengthNeighbor", tieStrengthNeighbor);

        //Evaaluasi COG Ability Node dan Social Importance
        fb.evaluate();

        double abilityNode=fb.getVariable("ABILITY_NODE").getValue();
        double socialImportance=fb.getVariable("SOCIAL_IMPORTANCE").getValue();
        // Debug log untuk melihat hasil evaluasi Ability Node dan Social Importance
//        System.out.println("[DEBUG] Evaluated values:");
//        System.out.println("[DEBUG] Ability Node: " + abilityNode);
//        System.out.println("[DEBUG] Social Importance: " + socialImportance);

        fb.setVariable("ABILITY", abilityNode);
        fb.setVariable("SOCIAL", socialImportance);
        //Evaluasi COG Nilai Transfer Opportunity
        fb.evaluate();

        // Ambil nilai Transfer Opportunity dan log hasilnya
        double transferOpportunity = fb.getVariable("TRANSFER_OPPORTUNITY").getValue();
//        System.out.println("[DEBUG] Transfer Opportunity: " + transferOpportunity);

        // Kembalikan nilai Transfer Opportunity
        return transferOpportunity;
//        return fb.getVariable("TRANSFER_OPPORTUNITY").getValue();
    }

    // Untuk Pebandingan Nilai Sosial dengan Neighbor di Pembuatan MessageCopies
    public double evaluateSelf(DTNHost host, double popularity, double tieStrength) {
        FIS fis = getFISFromHost(host);
        FunctionBlock fb = getFunctionBlock(fis, "FuzzyContextAware");


        fb.setVariable("popularityNeighbor", popularity);
        fb.setVariable("tieStrengthNeighbor", tieStrength);

        fb.evaluate();
        double selfSocialImportance = fb.getVariable("SOCIAL_IMPORTANCE").getValue();

        return selfSocialImportance;
    }
}
