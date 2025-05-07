package routing.contextAware;

import core.Settings;
import net.sourceforge.jFuzzyLogic.FIS;
import reinforcementLearning_ContextAware.QTableUpdateStrategy;
import reinforcementLearning_ContextAware.Qtable;
import routing.ActiveRouter;
import core.*;
import routing.MessageRouter;
import routing.contextAware.ContextMessage.MessageListTable;
import routing.contextAware.DensityMCopies.NetworkDensityCalculator;
import routing.contextAware.ENS.*;
//import routing.contextAware.ENS.*;
import routing.contextAware.FuzzyLogic.FuzzyContextAware;
import routing.contextAware.FuzzyLogic.FuzzyContextMsg;
import routing.contextAware.SocialCharcteristic.*;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContextAwareRLRouter extends ActiveRouter {

//    public static final String NROF_HOSTS = "nrofHosts";
    public static final String BUFFER_SIZE = "bufferSize";
    public static final String MSG_TTL = "msgTtl";
    public static final String INIT_ENERGY_S = "initialEnergy"; //Inisialisasi Energi
    public static final String ALPHA_POPULARITY = "alphaPopularity"; //Inisialisasi Alpha Popularity

    public static final String FCL_Context = "fcl"; //FuzzyLogicController
    public static final String FCL_MSG = "fclmsg"; //FuzzyLogicController

//    protected int nrofHosts;
    protected int bufferSize;
    protected int msgTtl;
    protected int initialEnergy;
    protected double alphaPopularity;
    protected FIS fclcontextaware; //FLC
    protected FIS fclcontextmsg;
    private double latestDensity;

    private Popularity popularity; //Instance class popularity
    private TieStrength tieStrength; //Instance class TieStrength
    private EncounteredNodeSet encounteredNodeSet;
    private FuzzyContextAware fuzzyContextAware;
    private FuzzyContextMsg fuzzyContextMsg;
    private Qtable qtable;
    private MessageListTable messageListTable;

    public ContextAwareRLRouter(Settings s) {
        super(s);

        this.encounteredNodeSet = new EncounteredNodeSet();
        this.alphaPopularity = s.getDouble(ALPHA_POPULARITY);
        this.popularity = new Popularity(alphaPopularity);
        this.tieStrength = new TieStrength(); //Object TieStrength
        //Read FCL file
        String FLCfromString = s.getSetting(FCL_Context);
        fclcontextaware = FIS.load(FLCfromString);
        String FCLmsgfromString = s.getSetting(FCL_MSG);
        fclcontextmsg = FIS.load(FCLmsgfromString);

        this.fuzzyContextAware = new FuzzyContextAware();
        this.fuzzyContextMsg = new FuzzyContextMsg();

        this.qtable = new Qtable();
        this.messageListTable = new MessageListTable(this.getHost());

        bufferSize = s.getInt(BUFFER_SIZE);
        msgTtl = s.getInt(MSG_TTL);
        initialEnergy = s.getInt(INIT_ENERGY_S); //Energi
        latestDensity = -1;

    }

    protected ContextAwareRLRouter(ContextAwareRLRouter r) {
        super(r);

        this.encounteredNodeSet = r.encounteredNodeSet.clone();
        this.alphaPopularity = r.alphaPopularity;
        this.popularity = r.popularity;
        this.tieStrength = r.tieStrength;
        this.fclcontextaware = r.fclcontextaware;
        this.fclcontextmsg = r.fclcontextmsg;
        this.fuzzyContextAware = r.fuzzyContextAware;
        this.fuzzyContextMsg = r.fuzzyContextMsg;
        this.qtable = r.qtable;
        this.messageListTable = r.messageListTable;

        this.bufferSize = r.bufferSize;
        this.msgTtl = r.msgTtl;
        this.initialEnergy = r.initialEnergy;
        this.latestDensity = r.latestDensity;
    }


    //Getter FLC
    public FIS getFIS() {
        return fclcontextaware;
    }

    public FIS getFISMsg(){
        return fclcontextmsg;
    }

    //Getter Q-Table
    public Qtable getQtable() {
        return this.qtable;
    }

    // Getter ENS untuk bisa diakses oleh node lain
    public EncounteredNodeSet getEncounteredNodeSet() {
        return this.encounteredNodeSet;
    }

    public Popularity getPopularity() {
        return this.popularity;
    }

    public TieStrength getTieStrength(){
        return this.tieStrength;
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

        // Debug log sebelum update ENS
        System.out.println("[INFO] Sebelum update ENS:");
        // Bersihkan ENS yang sudah kadaluarsa
        this.encounteredNodeSet.removeOldEncounters();
        neighborENS.removeOldEncounters();

        this.popularity.updatePopularity(host, this.encounteredNodeSet);
        neighborRouter.getPopularity().updatePopularity(neighbor, neighborENS);
        // Ambil nilai popularitas terbaru setelah update
        double neighborPop = popularity.getPopularity(neighbor);
        double myPop = popularity.getPopularity(host);
//        System.out.println("[DEBUG] Popularitas host (" + host.getAddress() + "): " + myPop);
//        System.out.println("[DEBUG] Popularitas neighbor (" + neighbor.getAddress() + "): " + neighborPop);


        // Update ENS kedua node
        // Tambahkan ke ENS jika bukan diri sendiri
        if (!myId.equals(neighborId)) {
            this.encounteredNodeSet.updateENS(host, neighbor, neighborId, currentTime, integerEnergy, neighborBuffer, duration, neighborPop);
            neighborENS.updateENS(neighbor, host, myId, currentTime, integerEnergy, neighborBuffer, duration, myPop);
        }
        // Perhitungan density (contextual)
        this.latestDensity = NetworkDensityCalculator.calculateNodeDensity(
                SimScenario.getInstance().getHosts().size(),
                this.encounteredNodeSet,
                neighborENS,
                myId,
                neighborId
        );

        // Pastikan ENS keduanya tidak kosong
        if (!this.encounteredNodeSet.isEmpty() && !neighborENS.isEmpty()) {
            // Saling tukar ENS dengan filtering otomatis
            this.encounteredNodeSet.exchangeWith(neighborENS, this.getHost(), neighbor, currentTime);
            neighborENS.exchangeWith(this.encounteredNodeSet, neighbor, this.getHost(), currentTime);

            System.out.println("[DEBUG] ENS setelah saling tukar di " + getHost().getAddress() + ":");
            this.encounteredNodeSet.printEncounterLog(getHost(), String.valueOf(neighbor.getAddress()), neighborENS);
        }

        this.tieStrength.calculateTieStrength(host, neighbor, this.encounteredNodeSet, connectionDuration);
        double TieStrength = tieStrength.getTieStrength(host, neighbor);

        double transferOpportunity = fuzzyContextAware.evaluateNeighbor(this.getHost(),neighbor, neighborBuffer, integerEnergy, neighborPop, TieStrength);

        updateQValueOnConUp(host,neighbor,transferOpportunity);
        qtable.printQtableByHost(myId);
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

            qtableUpdate.updateFirstStrategy(host, neighbor, state, actionId, tfOpportunity);

        }
    }

    private void handleConnectionDown(DTNHost neighbor, EncounteredNodeSet neighborENS) {
        String myId = String.valueOf(this.getHost().getAddress());
        String neighborId = String.valueOf(neighbor.getAddress());
        try {
            // Akhiri pencatatan durasi koneksi
            ConnectionDuration connDuration = ConnectionDuration.getConnection(this.getHost(), neighbor);
            if (connDuration != null) {
                connDuration.endConnection(this.getHost(), neighbor, neighborENS);
            }
            // Update Q-Value Second Strategy
            QTableUpdateStrategy qTableUpdate = new QTableUpdateStrategy(this.qtable);
            qTableUpdate.updateSecondStrategy(this.getHost(), neighbor);
            qtable.printQtableByHost(myId);
            System.out.println("[DEBUG] Sebelum Dihapus karena terputus ");
            this.encounteredNodeSet.printEncounterLog(this.getHost(), neighborId, neighborENS);
//            connDuration.printConnectionInfo(this.getHost(), neighbor);
        } finally {
            this.encounteredNodeSet.removeEncounter(neighborId);
            neighborENS.removeEncounter(myId);
        }

    }

    @Override
    public int receiveMessage(Message m, DTNHost from) {
//        // Cek apakah properti "copies" tidak ada pada pesan yang diterima
        if (m.getProperty("copies") == null) {
            // Asumsikan pengirim harus sudah menetapkan copies, jadi kita log saja
            System.out.println("Warning: message " + m.getId() + " received from " + from.getAddress() +
                    " does not have 'copies' property. Assigning default value.========");
//            // Berikan nilai default jika diperlukan (misalnya 1), atau bisa dihitung berdasarkan konteks
//            m.addProperty("copies", 1);
        }
        return super.receiveMessage(m, from);
    }

    @Override
    public boolean createNewMessage(Message m) {
        makeRoomForMessage(m.getSize());
        int copies = NetworkDensityCalculator.calculateCopiesBasedOnDensity(this.latestDensity);
        m.setTtl(msgTtl);
        m.addProperty("copies", copies);
        this.addToMessages(m, true);
        if (m.getFrom().equals(this.getHost()) && m.getProperty("copies") == null) {
            for (int i = 1; i < copies; i++) {
                Message copy = m.replicate();
                copy.setTtl(msgTtl);
//                copy.addProperty("copies", copies);
                this.addToMessages(copy, true);
            }
        }
        return true;
    }


    /**
     * Melakukan proses pengiriman semua pesan ke semua koneksi aktif pada node ini.
     * Pesan yang akan dikirim dievaluasi terlebih dahulu menggunakan fuzzy logic
     * untuk menentukan prioritasnya, kemudian diurutkan berdasarkan nilai prioritas tersebut.
     */
    @Override
    protected Connection tryAllMessagesToAllConnections() {
        // Ambil semua koneksi node saat ini
        List<Connection> connections = getConnections();
        // Jika tidak ada koneksi atau tidak ada pesan, langsung return null
        if (connections.isEmpty() || this.getNrofMessages() == 0) {
            return null;
        }
        // Ambil semua pesan yang ada di buffer node ini
        List<Message> messages = new ArrayList<>(this.getMessageCollection());

        // Lakukan evaluasi fuzzy logic untuk setiap pesan
        for (Message msg : messages) {
            int msgTtl = msg.getTtl();
            System.out.println("TTL " + msgTtl);
            int msgHopCount = msg.getHopCount();
//            Object copies = msg.getProperty("copies");
//            System.out.println("Menetapkan copies=" + copies + " untuk pesan: " + msg.getId());
            // Evaluasi prioritas berdasarkan TTL dan hopCount menggunakan modul FLC
            double priority = fuzzyContextMsg.evaluateMsg(this.getHost(), msgTtl, msgHopCount);
            // Simpan atau update prioritas pesan tersebut ke dalam table
            messageListTable.updateMessagePriority(msg, priority); // Menyimpan Prioritas di table
        }
        // Urutkan pesan berdasarkan prioritas (descending) menggunakan sortByQueueMode
        this.sortByQueueMode(messages);

        // Panggil mekanisme kontrol salinan pesan sebelum mengirim
        copiesControlMechanism(messages, connections);

        // Kirim pesan ke koneksi aktif berdasarkan urutan prioritas
        return tryMessagesToConnections(messages, connections);
    }

    /**
     * Mengurutkan daftar pesan atau tuple pesan-koneksi berdasarkan mode antrian (queue mode).
     * Dalam implementasi ini, pesan akan diurutkan berdasarkan nilai prioritas fuzzy (descending).
     * Prioritas diambil dari MessageListTable yang sudah diisi sebelumnya.
     * @param list Daftar objek yang akan diurutkan (bisa berupa List<Message> atau List<Tuple<Message, Connection>>)
     * @return Daftar yang telah diurutkan sesuai prioritas.
     */
    @Override
    protected List sortByQueueMode(List list) {
        // Cek apakah list berisi Message langsung atau Tuple<Message, Connection>
        // Jika list berisi objek Message langsung
        if (!list.isEmpty() && list.get(0) instanceof Message) {
            list.sort((Object o1, Object o2) -> {
                Message m1 = (Message) o1;
                Message m2 = (Message) o2;
                // Ambil nilai prioritas dari masing-masing pesan
                double p1 = messageListTable.getPriority(m1);
                double p2 = messageListTable.getPriority(m2);
                return Double.compare(p2, p1); //Desending (prioritas tinggi di awal)
            });
        }else if(!list.isEmpty() && list.get(0) instanceof Tuple) {
            list.sort((Object o1, Object o2) -> {
                Message m1 = ((Tuple<Message, Connection>) o1).getKey();
                Message m2 = ((Tuple<Message, Connection>) o2).getKey();
                // Ambil nilai prioritas dari masing-masing pesan dalam tuple
                double p1 = messageListTable.getPriority(m1);
                double p2 = messageListTable.getPriority(m2);
                return Double.compare(p2,p1); //Desending (prioritas tinggi di awal)
            });
        }
        // Kembalikan list yang sudah diurutkan
        return list;
    }

    // Memeriksa apakah node ini adalah penerima akhir dari pesan
    private boolean isFinalRecipient(Message m) {
        return m.getTo() == getHost();  // Mengecek apakah penerima pesan adalah node ini
    }

    // Memeriksa apakah ini adalah pengiriman pertama untuk pesan ini
    private boolean isFirstDelivery(Message m) {
        return m.getHopCount() == 0;  // Jika hop count adalah 0, berarti ini pengiriman pertama
    }

    private void copiesControlMechanism(List<Message> messages, List<Connection> connections) {
        DTNHost host = this.getHost();
        String hostId = String.valueOf(this.getHost().getAddress());

        for (Connection c : connections) {
            DTNHost neighbor = c.getOtherNode(this.getHost());
            String neighborId = String.valueOf(neighbor.getAddress());

            double myPop = popularity.getPopularity(host);
            double TieStrength = tieStrength.getTieStrength(this.getHost(), neighbor);
            double mySocial = fuzzyContextAware.evaluateSelf(this.getHost(), myPop, TieStrength);

            double neighborPop = popularity.getPopularity(neighbor);
            double neighborSocial = fuzzyContextAware.evaluateSelf(neighbor, neighborPop, TieStrength);

            for (Message msg : messages) {
                String msgId = msg.getId();
                DTNHost destination = msg.getTo();
                String destinationId = String.valueOf(destination.getAddress());
                System.out.println("Memeriksa copies untuk pesan: " + msgId + ", from: " + msg.getFrom().getAddress() + ", host: " + host.getAddress());

                Object prop = msg.getProperty("copies");
                if(!(prop instanceof Integer)) continue;
                int copies = (Integer) prop;

                double messagePriority = messageListTable.getPriority(msg);
                //Qvalue State
                String state = hostId +","+ destinationId;
                double myQ = qtable.getQvalue(state, neighborId);
                String stateNeighbor = neighborId +","+ destinationId;
                double neighborQ = qtable.getQvalue(stateNeighbor, hostId);

                boolean isDest = neighborId.equals(destinationId);
                boolean better = (neighborSocial > mySocial || neighborQ > myQ);
                boolean canSend = canStartTransfer();

                if(copies > 1){
                    if(isDest && canSend){
                        startTransfer(msg, c);
                        msg.updateProperty("copies", copies - 1);
                        return;
                    } else if (messagePriority >= 0.5 && better){
                        int sendCopies = copies / 2;
                        int remainingCopies = copies - sendCopies;
                        msg.updateProperty("copies", remainingCopies);

                        Message copy = msg.replicate();
                        copy.addProperty("copies", sendCopies);
                        if(canSend){
                            startTransfer(msg, c);
                            return;
                        }
                    }
                } else if (copies == 1){
                    if((isDest || (messagePriority >= 0.5)) && canSend){
                        startTransfer(msg, c);
                        deleteMessage(msgId, true);
                        return;
                    }
                }

//                // Decision Rules
//                // 7. Jika copies > 1
//                if(copies > 1){
//                    // 8. Jika neighbor adalah tujuan
//                    if(neighborId.equals(destinationId)) {
//                        startTransfer(msg, c);
//                    }
//                    // 10. Jika priority >= normal (misal threshold = 0.5)
//                    else if(messagePriority >= 0.5){
//                        // 11. Jika social atau Q-value si neighbor lebih baik
//                        if(neighborSocial > mySocial || neighborQ > myQ){
//                            startTransfer(msg, c);
//                        }
//                    }
//                }
//                // 14. Jika copies == 1 (pesan terakhir)
//                else if(copies == 1){
//                    if(neighborId.equals(destinationId)) {
//                        startTransfer(msg, c);
//                    } else if (messagePriority >= 0.5) {
//                        if(neighborSocial > mySocial && neighborQ > myQ){
//                            startTransfer(msg, c);
//                            this.deleteMessage(msg.getId(), true);
//                        }
//                    }
//                }
            }
        }
    }

    @Override
    protected int startTransfer(Message m, Connection con) {
        // Salin pesan seperti biasa
        int result = super.startTransfer(m, con);

        if (result == RCV_OK) {
            // Salin properti "copies" secara eksplisit ke pesan yang akan dikirim
            Message sendingMsg = con.getMessage(); // ini salinan dari m

            if (sendingMsg != null && m.getProperty("copies") != null) {
                // Cek apakah 'copies' sudah ada pada sendingMsg
                if (sendingMsg.getProperty("copies") == null) {
                    sendingMsg.addProperty("copies", m.getProperty("copies"));
                    System.out.println("[DEBUG] Menyalin properti 'copies' ke pesan " + sendingMsg.getId() +
                            " saat transfer ke " + con.getOtherNode(getHost()).getAddress());
                }
            }
        }

        return result;
    }

    public void update(){
        super.update();
        if(!canStartTransfer() || isTransferring()){
            return;
        }
        this.tryAllMessagesToAllConnections();
//        this.tryAllMessagesToAllConnections();


//        qtable.printQtableByHost(String.valueOf(this.getHost().getAddress()));
//        qtable.printQtableByHost();

    }

    @Override
    public Message messageTransferred(String id, DTNHost from) {
        // Step 1: Panggil super untuk mendapatkan pesan yang ditransfer
        Message m = super.messageTransferred(id, from);

        // Step 2: Tambahkan logika update Q-Table
        if(m != null){
            // Langkah 3: Cek apakah kita penerima final atau ini first delivery
            boolean isFinalRecipient = isFinalRecipient(m);
            boolean isFirstDelivery = isFirstDelivery(m);

            // Step 4: Update Q-table jika penerima final atau pengiriman pertama
            if (isFinalRecipient || isFirstDelivery){
                // Step 5: Ambil QTable sender dan receiver
                Qtable senderQtable = ((ContextAwareRLRouter) from.getRouter()).getQtable();
                Qtable receiverQtable = this.getQtable();

                // Update Qtable
                QTableUpdateStrategy receiverUpdate = new QTableUpdateStrategy(receiverQtable);
                receiverUpdate.updateThirdStrategy(senderQtable, receiverQtable);
                QTableUpdateStrategy senderUpdate = new QTableUpdateStrategy(senderQtable);
                senderUpdate.updateThirdStrategy(receiverQtable, senderQtable);

            }
        }
        return m;
    }

    @Override
    public ContextAwareRLRouter replicate() {
        return new ContextAwareRLRouter(this);
    }
}
