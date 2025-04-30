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
    public double evaluateNeighbor(DTNHost host, DTNHost neighbor, int freeBufferNeighbor, int remainingEnergy, double popularity, double tieStrength) {
        FIS fis = getFISFromHost(host);
        FunctionBlock fb = getFunctionBlock(fis, "FuzzyContextAware");

        // Debug log untuk memastikan variabel telah diset dengan benar
        System.out.println("[DEBUG] Setting input variables for FuzzyContextAware:");
        System.out.println("[DEBUG] bufferNeighbor: " + freeBufferNeighbor);
        System.out.println("[DEBUG] energyNeighbor: " + remainingEnergy);
        System.out.println("[DEBUG] popularityNeighbor: " + popularity);
        System.out.println("[DEBUG] tieStrengthNeighbor: " + tieStrength);
        //
        fb.setVariable("bufferNeighbor", freeBufferNeighbor);
        fb.setVariable("energyNeighbor", remainingEnergy);
        fb.setVariable("popularityNeighbor", popularity);
        fb.setVariable("tieStrengthNeighbor", tieStrength);

        //Evaaluasi COG Ability Node dan Social Importance
        fb.evaluate();

        double abilityNode=fb.getVariable("ABILITY_NODE").getValue();
        double socialImportance=fb.getVariable("SOCIAL_IMPORTANCE").getValue();
        // Debug log untuk melihat hasil evaluasi Ability Node dan Social Importance
        System.out.println("[DEBUG] Evaluated values:");
        System.out.println("[DEBUG] Ability Node: " + abilityNode);
        System.out.println("[DEBUG] Social Importance: " + socialImportance);

        fb.setVariable("ABILITY", abilityNode);
        fb.setVariable("SOCIAL", socialImportance);
        //Evaluasi COG Nilai Transfer Opportunity
        fb.evaluate();

        // Ambil nilai Transfer Opportunity dan log hasilnya
        double transferOpportunity = fb.getVariable("TRANSFER_OPPORTUNITY").getValue();
        System.out.println("[DEBUG] Transfer Opportunity: " + transferOpportunity);

        // Kembalikan nilai Transfer Opportunity
        return transferOpportunity;
//        return fb.getVariable("TRANSFER_OPPORTUNITY").getValue();
    }
}
