package routing.contextAware;

import core.Settings;
import net.sourceforge.jFuzzyLogic.FIS;
import reinforcementLearning_ContextAware.QTableUpdateStrategy;
import reinforcementLearning_ContextAware.Qtable;
import routing.ActiveRouter;
import core.*;
import routing.MessageRouter;
import routing.contextAware.ENS.*;
//import routing.contextAware.ENS.*;
import routing.contextAware.FuzzyLogic.FuzzyContextAware;
import routing.contextAware.SocialCharcteristic.*;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

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
    private EncounteredNodeSet encounteredNodeSet;
    private FuzzyContextAware fuzzyContextAware;
    private Qtable qtable;

    public ContextAwareRLRouter(Settings s) {
        super(s);

        this.encounteredNodeSet = new EncounteredNodeSet();
        this.alphaPopularity = s.getDouble(ALPHA_POPULARITY);
        this.popularity = new Popularity(alphaPopularity);
        this.tieStrength = new TieStrength(); //Object TieStrength
        //Read FCL file
        String FLCfromString = s.getSetting(FCL_Context);
        fclcontextaware = FIS.load(FLCfromString);
        this.fuzzyContextAware = new FuzzyContextAware();
        this.qtable = new Qtable();

        bufferSize = s.getInt(BUFFER_SIZE);
        initialEnergy = s.getInt(INIT_ENERGY_S); //Energi

    }

    protected ContextAwareRLRouter(ContextAwareRLRouter r) {
        super(r);

        this.encounteredNodeSet = r.encounteredNodeSet.clone();
        this.alphaPopularity = r.alphaPopularity;
        this.popularity = r.popularity;
        this.tieStrength = r.tieStrength;
        this.fclcontextaware = r.fclcontextaware;
        this.fuzzyContextAware = r.fuzzyContextAware;
        this.qtable = r.qtable;

        this.bufferSize = r.bufferSize;
        this.initialEnergy = r.initialEnergy;
    }


    //Getter FLC
    public FIS getFIS() {
        return fclcontextaware;
    }

    //Getter Q-Table
    public Qtable getQtable() {
        return this.qtable;
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

    // Getter ENS untuk bisa diakses oleh node lain
    public EncounteredNodeSet getEncounteredNodeSet() {
        return this.encounteredNodeSet;
    }

    public Popularity getPopularity() {
        return this.popularity;
    }


    /**
     * Metode ini dipanggil ketika ada perubahan status koneksi antara dua node dalam jaringan.
     * Jika koneksi baru terbentuk, informasi pertemuan akan ditambahkan ke Encountered Node Set (ENS),
     * dan pertukaran ENS dilakukan antara kedua node. Jika koneksi terputus, durasi koneksi dicatat
     * dan informasi encounter dari ENS dihapus untuk node yang terputus.
     *
     * @param con Objek Connection yang merepresentasikan koneksi antara dua node.
     */
    public void changedConnection(Connection con) {
        super.changedConnection(con);

        DTNHost host = getHost();
        DTNHost neighbor = con.getOtherNode(getHost());
        ContextAwareRLRouter neighborRouter = (ContextAwareRLRouter) neighbor.getRouter();
        EncounteredNodeSet neighborENS = neighborRouter.getEncounteredNodeSet();

        if (con.isUp()) {
            handleConnectionUp(host, neighbor, neighborRouter, neighborENS);
        } else {
            handleConnectionDown(neighbor, neighborENS);
        }
    }

    private void handleConnectionUp(DTNHost host, DTNHost neighbor, ContextAwareRLRouter neighborRouter, EncounteredNodeSet neighborENS) {
        long currentTime = (long) SimClock.getTime();
        String myId = String.valueOf(this.getHost().getAddress());
        String neighborId = String.valueOf(neighbor.getAddress());

        double neighborEnergy = neighborRouter.getEnergyModel().getEnergy();
        // Casting langsung ke tipe int
        int integerEnergy = (int) neighborEnergy;
        int neighborBuffer = neighborRouter.getFreeBufferSize();

        // Mulai pencatatan durasi koneksi
        ConnectionDuration connectionDuration = ConnectionDuration.startConnection(this.getHost(), neighbor);
        long duration = (long) connectionDuration.getDuration();
//        connectionDuration.printConnectionInfo(host, neighbor);

        // Debug log sebelum update ENS
        System.out.println("[INFO] Sebelum update ENS:");
        // Bersihkan ENS yang sudah kadaluarsa
        this.encounteredNodeSet.removeOldEncounters();
        neighborENS.removeOldEncounters();
//        this.encounteredNodeSet.printEncounterLog(this.getHost(), neighborId, neighborENS);

        this.popularity.updatePopularity(host, this.encounteredNodeSet);
        neighborRouter.getPopularity().updatePopularity(neighbor, neighborENS);
        // Ambil nilai popularitas terbaru setelah update
        double neighborPop = popularity.getPopularity(neighbor);
        double myPop = popularity.getPopularity(host);
        System.out.println("[DEBUG] Popularitas host (" + host.getAddress() + "): " + myPop);
        System.out.println("[DEBUG] Popularitas neighbor (" + neighbor.getAddress() + "): " + neighborPop);


        // Update ENS kedua node
        // Tambahkan ke ENS jika bukan diri sendiri
        if (!myId.equals(neighborId)) {
            this.encounteredNodeSet.updateENS(host, neighbor, neighborId, currentTime, integerEnergy, neighborBuffer, duration, neighborPop);
            neighborENS.updateENS(neighbor, host, myId, currentTime, integerEnergy, neighborBuffer, duration, myPop);
        }

//        // Debug log sebelum merge ENS
//        System.out.println("[INFO] Sebelum merge ENS:");
//        this.encounteredNodeSet.printEncounterLog(this.getHost(), neighborId, neighborENS);

        // Perhitungan density (contextual)
        double density = NetworkDensityCalculator.calculateNodeDensity(
                SimScenario.getInstance().getHosts().size(),
                this.encounteredNodeSet,
                neighborENS,
                myId,
                neighborId
        );
//        System.out.println("[DENSITY] Density antara " + myId + " dan " + neighborId + ": " + density);

        // Pastikan ENS keduanya tidak kosong
        if (!this.encounteredNodeSet.isEmpty() && !neighborENS.isEmpty()) {
            // Saling tukar ENS dengan filtering otomatis
            this.encounteredNodeSet.exchangeWith(neighborENS, this.getHost(), neighbor, currentTime);
            neighborENS.exchangeWith(this.encounteredNodeSet, neighbor, this.getHost(), currentTime);

            System.out.println("[DEBUG] ENS setelah saling tukar di " + getHost().getAddress() + ":");
            this.encounteredNodeSet.printEncounterLog(getHost(), String.valueOf(neighbor.getAddress()), neighborENS);
        }

        // Menghitung TieStrength pada saat koneksi up
        double tieStrength = TieStrength.calculateTieStrength(host, neighbor, this.encounteredNodeSet, connectionDuration);
        // Debug log untuk melihat nilai TieStrength
        System.out.println("[DEBUG] TieStrength antara " + myId + " dan " + neighborId + ": " + tieStrength);

        double transferOpportunity = fuzzyContextAware.evaluateNeighbor(this.getHost(),neighbor, neighborBuffer, integerEnergy, neighborPop, tieStrength);
        System.out.println("[DEBUG] Transfer Opportunity: " + transferOpportunity);

        updateQValueOnConUp(host,neighbor,transferOpportunity);
    }

    private void updateQValueOnConUp(DTNHost host, DTNHost neighbor, double tfOpportunity) {
        MessageRouter router = host.getRouter();
        Collection<Message> messages = router.getMessageCollection();

        String hostId = String.valueOf(this.getHost().getAddress());
        String actionId = String.valueOf(neighbor.getAddress());

        QTableUpdateStrategy qtableUpdate = new QTableUpdateStrategy(this.qtable);

        for (Message msg : messages){
            String destinationId = String.valueOf(msg.getTo().getAddress());

            String state = hostId +","+ destinationId;
            System.out.println("[STATE-ACTION] State = " + state + " Action = " + actionId);

            qtableUpdate.updateFirstStrategy(host, state, actionId, tfOpportunity);
        }
    }

    private void handleConnectionDown(DTNHost neighbor, EncounteredNodeSet neighborENS) {
        String myId = String.valueOf(this.getHost().getAddress());
        String neighborId = String.valueOf(neighbor.getAddress());

//
        try {
            // Akhiri pencatatan durasi koneksi
            ConnectionDuration connDuration = ConnectionDuration.getConnection(this.getHost(), neighbor);
            if (connDuration != null) {
                connDuration.endConnection(this.getHost(), neighbor, neighborENS);
            }

            System.out.println("[DEBUG] Sebelum Dihapus karena terputus ");
            this.encounteredNodeSet.printEncounterLog(this.getHost(), neighborId, neighborENS);
//            connDuration.printConnectionInfo(this.getHost(), neighbor);
        } finally {
//            // Hapus ENS untuk node yang terputus
//            String myId = String.valueOf(this.getHost().getAddress());
//            String neighborId = String.valueOf(neighbor.getAddress());

//            System.out.println("[DEBUG] Sebelum Dihapus karena terputus ");
//            this.encounteredNodeSet.printEncounterLog(this.getHost(), neighborId, neighborENS);
////            ConnectionDuration.printConnectionHistory(this.getHost());

            this.encounteredNodeSet.removeEncounter(neighborId);
            neighborENS.removeEncounter(myId);
        }

    }

    public void update(){
        super.update();


//
//        // Update popularitas berdasarkan waktu simulasi
//        double currentTime = SimClock.getTime(); // Pastikan pakai waktu simulasi
//        popularity.updatePopularity(currentTime);

//        this.encounteredNodeSet.debugENS(getHost());
        //routing.contextAware.ContextAwareNeighborEvaluator.tetagga(this.getHost(), this.popularity, this.tieStrength);
    }

    @Override
    public ContextAwareRLRouter replicate() {
        return new ContextAwareRLRouter(this);
    }
}
