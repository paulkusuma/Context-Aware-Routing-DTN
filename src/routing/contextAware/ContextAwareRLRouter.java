package routing.contextAware;

import core.Settings;
import net.sourceforge.jFuzzyLogic.FIS;
import routing.ActiveRouter;
import core.*;
import routing.contextAware.ENS.EncounterManager;
import routing.contextAware.SocialCharcteristic.Popularity;
import routing.contextAware.SocialCharcteristic.TieStrength;

import java.util.ArrayList;
import java.util.List;

public class ContextAwareRLRouter extends ActiveRouter {

    public static final String NROF_HOSTS = "nrofHosts";
    public static final String INIT_ENERGY_S = "initialEnergy"; //Inisialisasi Energi
    public static final String ALPHA_POPULARITY = "alphaPopularity"; //Inisialisasi Alpha Popularity

    public static final String FCL_Context = "fcl"; //FuzzyLogicController


    protected int nrofHosts;
    protected int initialEnergy;
    protected double alphaPopularity;
    protected FIS fclcontextaware; //FLC
    private Popularity popularity; //Instance class popularity
    private TieStrength tieStrength; //Instance class TieStrength
    private EncounterManager encounterManager;

    public ContextAwareRLRouter(Settings s) {
        super(s);

        this.popularity = new Popularity(alphaPopularity = s.getDouble(ALPHA_POPULARITY));  //Object Popularity
        this.tieStrength = new TieStrength(); //Object TieStrength
        this.encounterManager = new EncounterManager();
        //Read FCL file
        String FLCfromString = s.getSetting(FCL_Context);
        fclcontextaware = FIS.load(FLCfromString);

        nrofHosts = s.getInt(NROF_HOSTS);
        initialEnergy = s.getInt(INIT_ENERGY_S); //Energi

    }

    protected ContextAwareRLRouter(ContextAwareRLRouter r) {
        super(r);
        this.alphaPopularity = r.alphaPopularity;
        this.popularity = r.popularity;
        this.tieStrength = r.tieStrength;
        this.encounterManager = r.encounterManager;
        this.fclcontextaware = r.fclcontextaware;
        this.nrofHosts = r.nrofHosts;
        this.initialEnergy = r.initialEnergy;
    }

    //Getter FLC
    protected FIS getFIS() {
        return fclcontextaware;
    }

    public static List<DTNHost> getNeighbors(DTNHost host, Popularity popularity, TieStrength tieStrength){
        List<DTNHost> neighbors = new ArrayList<>();
        List<Connection> connections = host.getConnections();

        for (Connection conn : connections) {
            // Periksa apakah koneksi aktif
            if (conn.isUp()) {
                DTNHost neighbor = conn.getOtherNode(host);
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    //Getter EncounterManager
    public EncounterManager getEncounterManage() {
        return encounterManager;
    }


    public void changedConnection(Connection con) {
        super.changedConnection(con);

        DTNHost neighbor = con.getOtherNode(getHost());
        ContextAwareRLRouter neighborRouter = (ContextAwareRLRouter) neighbor.getRouter();

        // Cek apakah koneksi baru terbentuk
        if (con.isUp()) {
            // Lakukan pertukaran ENS jika koneksi aktif
            this.encounterManager.exchangeENS(neighborRouter.getEncounterManage());
            neighborRouter.getEncounterManage().exchangeENS(this.encounterManager);
        } else{
            // Update ENS dengan informasi dari neighbor
            this.encounterManager.updateENSWithNeighborInfo(neighborRouter.getEncounterManage());
            neighborRouter.getEncounterManage().updateENSWithNeighborInfo(this.encounterManager);
        }
    }


    @Override
    public ContextAwareRLRouter replicate() {
        return new ContextAwareRLRouter(this);
    }
}
