package reinforcementLearning_ContextAware;

import routing.contextAware.ENS.EncounteredNodeSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Qtable {

    // Q-table disimpan sebagai map (key: state-action, value: Q-value)
    private Map<String, Double> qtable;

    public Qtable() {
        qtable = new HashMap<>();
    }

    // Mengambil Q-Value untuk kombinasi state action
    public double getQvalue(String state, String action) {
        String key = state + ":" + action;
        //Mengembalikan nilai Q-Value jika ada, atau 0 jika tidak ada
        return qtable.getOrDefault(key, 0.0);
    }


    // Cari Q-maksimum dari node yang pernah ditemui (Nm)
    public double getMaxQvalueForEncounteredNodes(String destinationId, EncounteredNodeSet ens) {
        double maxQvalue = Double.NEGATIVE_INFINITY;

        // Iterasi melalui semua node yang pernah ditemui
        for (String encounteredNodeId : ens.getAllNodeIds()) {
            // Ambil nilai Q untuk pasangan (encounteredNodeId, destinationId)
            double qValue = getQvalue(encounteredNodeId, destinationId);
            System.out.println("[DEBUG] Encountered Node: " + encounteredNodeId +
                    " Q-value: " + qValue);

            // Perbarui maxQvalue dengan nilai Q tertinggi
            maxQvalue = Math.max(maxQvalue, qValue);
        }

        return maxQvalue ;
    }

    // Mengupdate nilai Q untuk pasangan State Action tertentu
    public void updateQValue(String state, String action, double qvalue) {
        String key = state + ":" + action;
        qtable.put(key, qvalue);
    }

}
