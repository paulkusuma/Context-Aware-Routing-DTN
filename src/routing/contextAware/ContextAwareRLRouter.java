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

        // Mendapatkan node tetangga dari koneksi ini
        DTNHost neighbor = con.getOtherNode(getHost());
        ContextAwareRLRouter neighborRouter = (ContextAwareRLRouter) neighbor.getRouter();

        // Mengambil informasi yang relevan untuk encounter
        double meetingTime = SimClock.getTime();  // Waktu pertemuan berdasarkan jam simulasi
        int remainingBuffer = neighborRouter.getFreeBufferSize();  // Mendapatkan sisa buffer dari router
        double remainingEnergy = neighborRouter.getEnergyModel().getEnergy();  // Mendapatkan energi yang tersisa dari router


        // Jika koneksi baru terbentuk
        if (con.isUp()) {

            // Memulai pencatatan durasi koneksi antara dua node
            ConnectionDuration connectionDuration = ConnectionDuration.startConnection(this.getHost(), neighbor);
            double connectionDurationValue = connectionDuration.getDuration();  // Mengambil durasi koneksi yang baru saja dibuat

            // Menyimpan encounter baru dalam ENS
            this.encounterManager.addEncounterToENS(neighbor.getAddress(), meetingTime, remainingBuffer, remainingEnergy, connectionDurationValue);

            // Melakukan pertukaran ENS antara node ini dengan node tetangga
            this.encounterManager.exchangeENS(neighborRouter.getEncounterManage()); // Pertukaran ENS dari node ini ke neighbor
            neighborRouter.getEncounterManage().exchangeENS(this.encounterManager); // Pertukaran ENS dari neighbor ke node ini

            // Hitung kepadatan jaringan setelah pertukaran ENS
            int totalNodes =  5;  // Ambil jumlah total node dalam jaringan
            double density = NetworkDensityCalculator.CalculateNodeDensity(totalNodes, this.encounterManager, neighborRouter.getEncounterManage());
//            System.out.println("Kepadatan Jaringan Saat Ini: " + density);
            // Hitung jumlah salinan pesan berdasarkan density
            int messageCopies = NetworkDensityCalculator.calculateCopiesBasedOnDensity(density);

        } else{ // Jika koneksi terputus

            // Mengakhiri pencatatan durasi koneksi
            ConnectionDuration connDuration = ConnectionDuration.getConnection(this.getHost(), neighbor);
            if (connDuration != null) {
                connDuration.endConnection();
//                connDuration.printConnectionInfo(); // Cetak informasi koneksi yang telah berakhir
            }
            // Menghapus informasi encounter dari ENS untuk node yang terputus
            this.encounterManager.updateENSOnConnectionDown(neighbor.getAddress()); // Hapus ENS untuk neighbor di node ini
            neighborRouter.getEncounterManage().removeEncounterFromENS(this.getHost().getAddress());  // Hapus ENS untuk node ini di neighbor
        }
        // Membersihkan ENS dari encounter yang sudah lebih dari 60 detik
        this.encounterManager.checkAndCleanENS();
    }

    public void update(){
        super.update();

//        // Panggil metode untuk mencetak ENS dari host
//        encounterManager.printENS(this.getHost());
        encounterManager.checkAndCleanENS(); // Pastikan ENS diperbarui
        //routing.contextAware.ContextAwareNeighborEvaluator.tetagga(this.getHost(), this.popularity, this.tieStrength);
    }

    @Override
    public ContextAwareRLRouter replicate() {
        return new ContextAwareRLRouter(this);
    }
}
