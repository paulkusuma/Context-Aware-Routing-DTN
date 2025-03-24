package routing.contextAware;

import core.Settings;
import net.sourceforge.jFuzzyLogic.FIS;
import routing.ActiveRouter;
import core.*;
import routing.contextAware.ENS.ConnectionDuration;
import routing.contextAware.ENS.EncounterManager;
import routing.contextAware.SocialCharcteristic.Popularity;
import routing.contextAware.SocialCharcteristic.TieStrength;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextAwareRLRouter extends ActiveRouter {

//    public static final String NROF_HOSTS = "nrofHosts";
    public static final String BUFFER_SIZE = "bufferSize";
    public static final String INIT_ENERGY_S = "initialEnergy"; //Inisialisasi Energi
    public static final String ALPHA_POPULARITY = "alphaPopularity"; //Inisialisasi Alpha Popularity

    public static final String FCL_Context = "fcl"; //FuzzyLogicController


//    protected int nrofHosts;
    protected int bufferSize;
    protected int initialEnergy;
    protected double alphaPopularity;
    protected FIS fclcontextaware; //FLC
    private Popularity popularity; //Instance class popularity
    private TieStrength tieStrength; //Instance class TieStrength
    private EncounterManager encounterManager;

    private Map<Connection, ConnectionDuration> connectionDurations = new HashMap<>();

    public ContextAwareRLRouter(Settings s) {
        super(s);

        this.popularity = new Popularity(alphaPopularity = s.getDouble(ALPHA_POPULARITY));  //Object Popularity
        this.tieStrength = new TieStrength(); //Object TieStrength
        this.encounterManager = new EncounterManager();
        //Read FCL file
        String FLCfromString = s.getSetting(FCL_Context);
        fclcontextaware = FIS.load(FLCfromString);

//        nrofHosts = s.getInt(NROF_HOSTS);
        bufferSize = s.getInt(BUFFER_SIZE);
        initialEnergy = s.getInt(INIT_ENERGY_S); //Energi

    }

    protected ContextAwareRLRouter(ContextAwareRLRouter r) {
        super(r);
        this.alphaPopularity = r.alphaPopularity;
        this.popularity = r.popularity;
        this.tieStrength = r.tieStrength;
        this.encounterManager = r.encounterManager;
        this.fclcontextaware = r.fclcontextaware;
//        this.nrofHosts = r.nrofHosts;
        this.bufferSize = r.bufferSize;
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

        // Mendapatkan informasi yang dibutuhkan
        double meetingTime = SimClock.getTime();  // Waktu pertemuan berdasarkan jam simulasi
        int remainingBuffer = neighborRouter.getFreeBufferSize();  // Mendapatkan sisa buffer dari router
        double remainingEnergy = neighborRouter.getEnergyModel().getEnergy();  // Mendapatkan energi yang tersisa dari router


        // Cek apakah koneksi baru terbentuk
        if (con.isUp()) {
            connectionDurations.put(con, new ConnectionDuration(getHost(), neighbor));
            // Ambil connection duration dari ConnectionDuration yang telah disimpan
            double connectionDuration = connectionDurations.get(con).getDuration();

            // Menyimpan encounter baru dalam ENS dengan data yang diperlukan
            this.encounterManager.addEncounterToENS(neighbor.getAddress(), meetingTime, remainingBuffer, remainingEnergy, connectionDuration);

            // Lakukan pertukaran ENS jika koneksi aktif
            this.encounterManager.exchangeENS(neighborRouter.getEncounterManage());  // Pertukaran ENS dengan node C
            neighborRouter.getEncounterManage().exchangeENS(this.encounterManager); // Node C mengirimkan ENS ke node N1

        } else{
            // Jika koneksi terputus, update ENS

            if (connectionDurations.containsKey(con)) {
                ConnectionDuration duration =connectionDurations.get(con);
                duration.endConnection();
            }

            // Menghapus encounter untuk node yang sudah tidak terhubung di ENS
            // Hapus encounter untuk node C di Node N1
            this.encounterManager.updateENSOnConnectionDown(neighbor.getAddress());
            // Hapus encounter untuk Node N1 di Node C
            neighborRouter.getEncounterManage().removeEncounterFromENS(this.getHost().getAddress());

            // Update ENS
            this.encounterManager.updateENSOnConnectionDown(neighbor.getAddress());
            neighborRouter.getEncounterManage().updateENSOnConnectionDown(this.getHost().getAddress());  // Hanya update, tidak perlu pertukaran lagi
        }
        connectionDurations.remove(con);
    }

    public void update(){
        super.update();

//        // Panggil metode untuk mencetak ENS dari host
//        encounterManager.printENS(this.getHost());
        //routing.contextAware.ContextAwareNeighborEvaluator.tetagga(this.getHost(), this.popularity, this.tieStrength);
    }

    @Override
    public ContextAwareRLRouter replicate() {
        return new ContextAwareRLRouter(this);
    }
}
