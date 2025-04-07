package routing.contextAware;

import core.Settings;
import net.sourceforge.jFuzzyLogic.FIS;
import routing.ActiveRouter;
import core.*;
import routing.contextAware.ENS.*;
import routing.contextAware.SocialCharcteristic.Popularity;
import routing.contextAware.SocialCharcteristic.TieStrength;


import java.util.ArrayList;
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

    public ContextAwareRLRouter(Settings s) {
        super(s);

        this.encounteredNodeSet = new EncounteredNodeSet();
        this.alphaPopularity = s.getDouble(ALPHA_POPULARITY);
        this.tieStrength = new TieStrength(); //Object TieStrength
        //Read FCL file
        String FLCfromString = s.getSetting(FCL_Context);
        fclcontextaware = FIS.load(FLCfromString);

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

    // Getter ENS untuk bisa diakses oleh node lain
    public EncounteredNodeSet getEncounteredNodeSet() {
        return this.encounteredNodeSet;
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

        // Bersihkan ENS yang sudah kadaluarsa
        this.encounteredNodeSet.removeOldEncounters();
        neighborENS.removeOldEncounters();
    }

    private void handleConnectionUp(DTNHost host, DTNHost neighbor, ContextAwareRLRouter neighborRouter, EncounteredNodeSet neighborENS) {
        long currentTime = (long) SimClock.getTime();
        String myId = String.valueOf(this.getHost().getAddress());
        String neighborId = String.valueOf(neighbor.getAddress());

        double neighborEnergy = neighborRouter.getEnergyModel().getEnergy();
        int neighborBuffer = neighborRouter.getFreeBufferSize();

        // Mulai pencatatan durasi koneksi
        ConnectionDuration connectionDuration = ConnectionDuration.startConnection(this.getHost(), neighbor);
        long duration = (long) connectionDuration.getDuration();

        // Debug log sebelum update ENS
        System.out.println("[INFO] Sebelum update ENS:");
        this.encounteredNodeSet.printEncounterLog(this.getHost(), neighborId, neighborENS);

        // Update ENS kedua node
        // Tambahkan ke ENS jika bukan diri sendiri
        if (!myId.equals(neighborId)) {
            this.encounteredNodeSet.updateENS(host, neighbor, neighborId, currentTime, neighborEnergy, neighborBuffer, duration);
            neighborENS.updateENS(neighbor, host, myId, currentTime, neighborEnergy, neighborBuffer, duration);
        }

        // Debug log sebelum merge ENS
        System.out.println("[INFO] Sebelum merge ENS:");
        this.encounteredNodeSet.printEncounterLog(this.getHost(), neighborId, neighborENS);


        // Pastikan ENS keduanya tidak kosong
        if (!this.encounteredNodeSet.isEmpty() && !neighborENS.isEmpty()) {
            // Clone ENS dan bersihkan ID pengirim sebelum ditambahkan
            EncounteredNodeSet clonedNeighborENS = neighborENS.clone();
            EncounteredNodeSet clonedMyENS = this.encounteredNodeSet.clone();


            // Tukar data
            this.encounteredNodeSet.mergeENS(this.getHost(), clonedNeighborENS, currentTime, neighbor);
            neighborENS.mergeENS(neighbor, clonedMyENS, currentTime, this.getHost());

            System.out.println("[DEBUG] ENS setelah merge di " + getHost().getAddress() + ":");
            this.encounteredNodeSet.printEncounterLog(getHost(), neighborId, neighborENS);

//            clonedNeighborENS.removeEncounter(neighborId); // Hapus ID neighbor dari ENS-nya
//            clonedMyENS.removeEncounter(myId); // Hapus ID host dari ENS-nya
//
//
//            // Bersihkan self dari hasil merge, opsional kalau mergeENS sudah handle ini
//            this.encounteredNodeSet.cleanSelfFromENS(this.getHost());
//            neighborENS.cleanSelfFromENS(neighbor);
        }

        // Debug log setelah merge ENS
//        System.out.println("[INFO] Setelah merge ENS:");
//        this.encounteredNodeSet.printEncounterLog(this.getHost(), neighborId, neighborENS);
    }

    private void handleConnectionDown(DTNHost neighbor, EncounteredNodeSet neighborENS) {
        try {
            // Akhiri pencatatan durasi koneksi
            ConnectionDuration connDuration = ConnectionDuration.getConnection(this.getHost(), neighbor);
            if (connDuration != null) {
                connDuration.endConnection();
            }
        } finally {
            // Hapus ENS untuk node yang terputus
            String myId = String.valueOf(this.getHost().getAddress());
            String neighborId = String.valueOf(neighbor.getAddress());

            this.encounteredNodeSet.removeEncounter(neighborId);
            neighborENS.removeEncounter(myId);
        }

    }

    public void update(){
        super.update();

//        this.encounteredNodeSet.debugENS(getHost());
        //routing.contextAware.ContextAwareNeighborEvaluator.tetagga(this.getHost(), this.popularity, this.tieStrength);
    }

    @Override
    public ContextAwareRLRouter replicate() {
        return new ContextAwareRLRouter(this);
    }
}


//long encounterTime = (long) SimClock.getTime();
//        int remainingBuffer = neighborRouter.getFreeBufferSize();
//        double remainingEnergy = neighborRouter.getEnergyModel().getEnergy();
//
//        // Mulai pencatatan durasi koneksi
//        ConnectionDuration connectionDuration = ConnectionDuration.startConnection(this.getHost(), neighbor);
//        long connectionDurationValue = (long) connectionDuration.getDuration();
//
//        // Tambahkan ENS baru jika node bukan dirinya sendiri
//        String myID = String.valueOf(this.getHost().getAddress());
//        String neighborId = String.valueOf(neighbor.getAddress());
//
//        // Debugging
//        System.out.println("[INFO] Sebelum Updateeeeeee:");
//        this.encounteredNodeSet.printEncounterLog(this.getHost(), neighborId ,neighborENS);
//
//
//            // Jika ID berbeda, lakukan pembaruan ENS
//        this.encounteredNodeSet.updateENS(host,neighbor, neighborId, encounterTime, remainingEnergy, remainingBuffer, connectionDurationValue);
//        this.encounteredNodeSet.updateENS(neighbor, host, myID, encounterTime, remainingEnergy, remainingBuffer, connectionDurationValue);
////        this.encounteredNodeSet.updateENS(host, neighborId, encounterTime, remainingEnergy, remainingBuffer, connectionDurationValue);
////        neighborENS.updateENS(neighbor, myID, encounterTime, remainingEnergy, remainingBuffer, connectionDurationValue);
//
//        // Debugging
//        System.out.println("[INFO] Sebelum ENS digabungkannnnnnnnnnnnnnnnnnnnnn:");
//        this.encounteredNodeSet.printEncounterLog(this.getHost(), neighborId ,neighborENS);
//
//
//
////        // Menggabungkan ENS antar node
////        this.encounteredNodeSet.mergeENS(this.getHost(), neighborENS, (long) SimClock.getTime(), neighbor);
////        neighborENS.mergeENS(neighbor,this.encounteredNodeSet, (long) SimClock.getTime(), this.getHost());
//
//        // Menggabungkan ENS antar node
//        // Pastikan kedua ENS tidak kosong sebelum pertukaran
//        if (!this.encounteredNodeSet.isEmpty() && !neighborENS.isEmpty()) {
//            // Node A menerima ENS dari Node B
//            this.encounteredNodeSet.mergeENS(this.getHost(), neighborENS, (long) SimClock.getTime(), neighbor);
//
//            // Node B menerima ENS dari Node A
//            neighborENS.mergeENS(neighbor, this.encounteredNodeSet, (long) SimClock.getTime(), this.getHost());
//        }
//        // Pastikan ENS setelah pertukaran tidak menyimpan ID diri sendiri
////        this.encounteredNodeSet.cleanSelfFromENS(this.getHost());
////        neighborENS.cleanSelfFromENS(neighbor);
//
//        System.out.println("[INFO] Setelah ENS digabungkan:");
//        this.encounteredNodeSet.printEncounterLog(this.getHost(), neighborId ,neighborENS);
////        this.encounteredNodeSet.debugENS(getHost());