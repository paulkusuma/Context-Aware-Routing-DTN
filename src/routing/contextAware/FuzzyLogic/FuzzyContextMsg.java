package routing.contextAware.FuzzyLogic;

import core.DTNHost;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import routing.contextAware.ContextAwareRLRouter;

public class FuzzyContextMsg {


    private FIS getFISFromHost(DTNHost host) {
        // Ambil FIS dari ContextAwareRLRouter
        ContextAwareRLRouter router = (ContextAwareRLRouter) host.getRouter();
        FIS fclMsg = router.getFISMsg();

        if (fclMsg == null) {
            System.err.println("Error: FCL tidak tersedia!");
        }

        return fclMsg;
    }

    private FunctionBlock getFunctionBlock(FIS fis, String blockName) {
        FunctionBlock fb = fis.getFunctionBlock(blockName);

        if (fb == null) {
            System.err.println("Error: Function Block '" + blockName + "' tidak ditemukan dalam FCL!");
        }

        return fb;
    }

    public double evaluateMsg(DTNHost host, int msgTTL, int msgHopCount){
        FIS fis = getFISFromHost(host);
        FunctionBlock fb = getFunctionBlock(fis, "FuzzyMessageContext");
//        System.out.println("[DEBUG] Setting input variables for message priority:");

//        System.out.println("[DEBUG] Msg TTL : " + msgTTL);
//        System.out.println("[DEBUG] Msg HopCount: " + msgHopCount);
        final double MAX_TTL= 120; // 500 MB
        final double MAX_HopCount = 10;
        double normTTL = Math.min(msgTTL / MAX_TTL, 1.0);
        double normHopCount = Math.min(msgHopCount / MAX_HopCount, 1.0);

        fb.setVariable("msgTTL", normTTL);
        fb.setVariable("msgHopCount", normHopCount);

        fb.evaluate();
        double msgPriority = fb.getVariable("MESSAGE_PRIORITY").getValue();
//        System.out.println("[DEBUG] MSG Priority : " + msgPriority);

        return msgPriority;
    }

}
