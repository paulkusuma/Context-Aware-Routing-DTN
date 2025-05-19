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
        }
        finally {
            this.encounteredNodeSet.removeEncounter(neighborId);
            neighborENS.removeEncounter(myId);
        }

    }

    /**
     * Membuat dan menambahkan pesan baru ke buffer node.
     * Jika node ini adalah pengirim utama, akan dibuat salinan tambahan
     * berdasarkan kepadatan jaringan saat ini.
     *
     * @param m Pesan yang akan dibuat dan disimpan.
     * @return true jika pesan berhasil ditambahkan ke buffer.
     */
    @Override
    public boolean createNewMessage(Message m) {
        // Pastikan ada ruang di buffer untuk pesan baru
        makeRoomForMessage(m.getSize());
        // Hitung jumlah copies berdasarkan density node saat ini
        int copies = NetworkDensityCalculator.calculateCopiesBasedOnDensity(this.latestDensity);

        // Set TTL dan properti copies untuk pesan utama
        m.setTtl(msgTtl);
        m.addProperty("copies", copies);
        this.addToMessages(m, true);

//        // Jika node ini adalah pengirim awal, buat salinan tambahan dan pesan belum punya properti "copies"
//        if (m.getFrom().equals(this.getHost()) && copies > 1) {
//            // Buat copies-1 salinan tambahan
//            for (int i = 1; i < copies; i++) {
//                Message copy = m.replicate(); // Salin pesan
//                copy.setTtl(msgTtl);
//                copy.updateProperty("copies", copies);
//                this.addToMessages(copy, true);
//            }
//        }
        return true;
    }

    /**
     * Mencoba mengosongkan buffer agar cukup untuk menyimpan pesan baru.
     * Pesan dengan prioritas paling rendah akan dihapus terlebih dahulu,
     * kecuali yang sedang dikirim (in transfer).
     *
     * @param size Ukuran pesan baru yang ingin dimasukkan ke buffer.
     * @return true jika cukup ruang berhasil disediakan, false jika tidak memungkinkan.
     */
    @Override
    protected boolean makeRoomForMessage(int size) {
        // Jika ukuran pesan lebih besar dari ukuran buffer total, langsung tolak
        if (size > this.getBufferSize()) {
            return false;
        }
        int freeBuffer = this.getFreeBufferSize();
        while (freeBuffer < size){
            // Ambil pesan dengan prioritas terendah (selain yang sedang dikirim)
            Message lowestPriorityMsg = getLowestPriorityMessage(true);
            if(lowestPriorityMsg == null){
                // Tidak ada lagi pesan yang bisa dihapus, gagal membuat ruang
                return false;
            }
            // Hapus pesan dari buffer (mode drop = true)
            deleteMessage(lowestPriorityMsg.getId(),true);
            messageListTable.removeMessageLowPriority(lowestPriorityMsg);
            // Tambah ruang kosong berdasarkan ukuran pesan yang dihapus
            freeBuffer += lowestPriorityMsg.getSize();
        }
        // Berhasil menyediakan ruang yang cukup
        return true;
    }

    /**
     * Mengembalikan pesan dengan prioritas fuzzy paling rendah di buffer,
     * dengan opsi mengecualikan pesan yang sedang dikirim.
     *
     * @param excludeMsgBeingSent Jika true, abaikan pesan yang sedang dikirim
     * @return Pesan dengan prioritas terendah, atau null jika tidak ada kandidat yang valid
     */
    private Message getLowestPriorityMessage(boolean excludeMsgBeingSent){
        Collection<Message> messages =this.getMessageCollection();
        Message lowest=null;
        for (Message m : messages){
            // Lewati pesan yang sedang dikirim (jika diminta)
            if(excludeMsgBeingSent && isSending(m.getId())) continue;
            // Update jika belum ada kandidat, atau jika pesan ini lebih rendah prioritasnya
            if(lowest==null || messageListTable.getPriority(m) < messageListTable.getPriority(lowest)){
                lowest = m;
            }
        }
        return lowest;
    }

    /**
     * Memilih tetangga terbaik untuk relay pesan berdasarkan kombinasi nilai
     * social importance dan Q-value terhadap tujuan pesan.
     *
     * @param msg Pesan yang sedang diproses.
     * @return Koneksi ke tetangga terbaik, atau null jika tidak ada yang cocok.
     */
    private Connection selectBestNeighbor(Message msg) {
        DTNHost host = this.getHost();
        String hostId = String.valueOf(host.getAddress());
        String destId = String.valueOf(msg.getTo().getAddress());

        Connection bestConn = null;
        double bestQVal = Double.NEGATIVE_INFINITY;
        List<Connection> connUp = getConnections();

        for (Connection conn : connUp) {
            DTNHost neighbor = conn.getOtherNode(host);
            String neighborId = String.valueOf(neighbor.getAddress());

            // Skip jika neighbor adalah dirinya sendiri
            if (neighborId.equals(hostId)) continue;

            String state = hostId + "," + destId;
            double qVal = qtable.getQvalue(state, neighborId);

            if (qVal > bestQVal) {
                bestQVal = qVal;
                bestConn = conn;
            }
        }

        return bestConn;
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

        // Lakukan evaluasi fuzzy logic untuk prioritas setiap pesan
        for (Message msg : messages) {
            if (msg == null) {
                return null;
            }
            System.out.println("HopCount : "+ msg.getHopCount());
            // Evaluasi prioritas berdasarkan TTL dan hopCount menggunakan modul FLC
            double priority = fuzzyContextMsg.evaluateMsg(this.getHost(), msg.getTtl(), msg.getHopCount());
            // Simpan atau update prioritas pesan tersebut ke dalam table
            messageListTable.updateMessagePriority(msg, priority); // Menyimpan Prioritas di table
        }

        // 2. Urutkan menggunakan sortByQueueMode() berdasarkan messageListTable
        this.sortByFuzzyPriority(messages);

        // 3. Proses forwarding berdasarkan urutan prioritas
        for (Message msg : messages) {
            Connection conn = copiesControlMechanism(msg, connections);
            if (conn != null) {
                return conn;
            }
        }
        return null;
    }

    /**
     * Mengurutkan daftar pesan berdasarkan nilai prioritas fuzzy (dari tinggi ke rendah).
     * Prioritas masing-masing pesan diambil dari {@code messageListTable} yang telah diisi sebelumnya.
     * Digunakan untuk menentukan urutan pengiriman pesan yang paling penting lebih dulu.
     *
     * @param messages Daftar pesan (List<Message>) yang akan diurutkan.
     * @return Daftar pesan yang telah diurutkan sesuai nilai prioritas fuzzy (descending).
     */
    protected List<Message> sortByFuzzyPriority(List<Message> messages) {
        // Urutkan pesan berdasarkan nilai prioritas fuzzy secara menurun (descending)
        messages.sort(( m1, m2) -> {
            // Ambil nilai prioritas dari masing-masing pesan
            double p1 = messageListTable.getPriority(m1);
            double p2 = messageListTable.getPriority(m2);
            // Urutkan dari prioritas tertinggi ke terendah
            return Double.compare(p2, p1); //Desending (prioritas tinggi di awal)
        });
        // Kembalikan daftar pesan yang telah diurutkan
        return messages;
    }

    /**
     * Mengelola kontrol salinan pesan berdasarkan mekanisme Spray and Focus yang ditingkatkan.
     * Forwarding diputuskan berdasarkan apakah tetangga adalah tujuan, prioritas pesan,
     * nilai sosial (social importance), dan nilai Q-value antar node.
     * Salinan pesan hanya disebar setengah jika memenuhi syarat, untuk mengurangi overhead jaringan.
     *
     * @param msg pesan yang ada pada node yang menjalankan metode ini.
     * @param connections Daftar koneksi aktif antara node saat ini dan node tetangganya.
     * @return Koneksi yang digunakan untuk transfer (jika ada), null jika tidak ada yang cocok.
     */
    private Connection copiesControlMechanism(Message msg, List<Connection> connections) {
        DTNHost host = this.getHost();
        String hostId = String.valueOf(this.getHost().getAddress());
        DTNHost destination = msg.getTo();
        String destinationId = String.valueOf(destination.getAddress());
        String msgId = msg.getId();

        // Prioritas pesan di MessageListTable
        double messagePriority = messageListTable.getPriority(msg);
        int copies = (int) msg.getProperty("copies"); // Mengambil copies yang sudah di inisialisasi createM

        // Iterasi untuk memeriksa setiap koneksi yang tersedia antara host dan tetangga
        for (Connection c : connections) {
            DTNHost neighbor = c.getOtherNode(host);
            String neighborId = String.valueOf(neighbor.getAddress());
//            boolean canSend = c.isReadyForTransfer();//connection is ready to transfer a message
            boolean isDestination = neighborId.equals(destinationId);

//            if (!canStartTransfer()) continue;

            // --- PRIORITAS 1: Forward langsung ke tujuan jika sedang terkoneksi ---
            if (isDestination && c.isReadyForTransfer()) {
                if (copies > 1) {
                    msg.updateProperty("copies", copies - 1);
                } else {
                    deleteMessage(msg.getId(), true);
                }
                //
//                msg.addNodeOnPath(host);
                startTransfer(msg, c);
                return c;
            }
        }
        // Cari tetangga terbaik menurut Q-value
        Connection bestConnection = selectBestNeighbor(msg);
        if (bestConnection == null || !connections.contains(bestConnection)) {
            return null; // Tidak ada tetangga terbaik yang sedang terkoneksi
        }
        DTNHost bestNeighbor = bestConnection.getOtherNode(host);
        String bestNeighborId = String.valueOf(bestNeighbor.getAddress());

        if(canStartTransfer()){
            // Hitung nilai social importance node ini dan neighbor
            double myPop = popularity.getPopularity(host);
            double tie = tieStrength.getTieStrength(this.getHost(), bestNeighbor);
            double mySocial = fuzzyContextAware.evaluateSelf(this.getHost(), myPop, tie);
            double neighborPop = popularity.getPopularity(bestNeighbor);
            double neighborSocial = fuzzyContextAware.evaluateSelf(bestNeighbor, neighborPop, tie);

            // Mengambil Q-value untuk host dan tetangga berdasarkan state tujuan pesan
            String state = hostId +","+ destinationId;
            double myQ = qtable.getQvalue(state, bestNeighborId);
            String stateNeighbor = bestNeighborId +","+ destinationId;
            double neighborQ = qtable.getQvalue(stateNeighbor, hostId);

            // Evaluasi apakah neighbor lebih baik dari segi sosial atau Q-value
            boolean socialBetter = neighborSocial > mySocial;
            boolean qvalueBetter = neighborQ > myQ;

            if(copies > 1 && messagePriority >= 0.5 && (socialBetter || qvalueBetter)){
                int sendCopies = copies/2; // Membagi salinan pesan untuk dikirim
                int remainingCopies = copies - sendCopies; // Salinan yang tersisa

                Message copy = msg.replicate(); // Membuat salinan pesan
                copy.setTtl(msg.getTtl());
                copy.updateProperty("copies", sendCopies); // Menambahkan jumlah salinan untuk salinan pesan baru

                //
//                copy.addNodeOnPath(this.getHost());
                int result = startTransfer(copy, bestConnection); // Coba kirim salinan
                if(result == RCV_OK){
                    // Jika pengiriman salinan berhasil, baru kurangi salinan dari pesan asli
                    msg.updateProperty("copies", remainingCopies); // Mengupdate jumlah salinan yang tersisa
                    return bestConnection;
                } else{
                    double bufferSpace = getFreeBufferSize();
                    System.out.println("[TRANSFER FAIL] Message " + msg.getId() +
                            " not sent to " + bestNeighbor.getAddress() +
                            ". Buffer space: " + bufferSpace);
                }
                // Jika gagal kirim, tidak jadi update copies, dan tidak return

                // Kondisi jika jumlah salinan pesan adalah 1
                // Jika hanya tersisa 1 copy, lakukan forwarding final
            } else if (copies == 1){
                if(bestNeighborId.equals(destinationId)){
                    //
//                    msg.addNodeOnPath(host);
                    int result = startTransfer(msg, bestConnection);
                    if(result == RCV_OK){
                        deleteMessage(msg.getId(), true);
                        return bestConnection;
                    }
                } else if((messagePriority >= 0.5 && socialBetter && qvalueBetter)
                        || (messagePriority < 0.5 && (socialBetter || qvalueBetter))){
                    //
//                    msg.addNodeOnPath(host);
                    int result = startTransfer(msg, bestConnection);
                    if(result == RCV_OK){
                        deleteMessage(msg.getId(), true);
                        return bestConnection;
                    }
                }
//                // Kirim jika social dan qvalue lebih baik, atau salah satunya cukup
//                if((messagePriority >= 0.5 && socialBetter && qvalueBetter)
//                        || (messagePriority < 0.5 && (socialBetter || qvalueBetter))){
//                    int result = startTransfer(msg, bestConnection);
//                    if(result == RCV_OK){
//                        deleteMessage(msgId, true);
//                        return bestConnection;
//                    }
//                }
            }
        }
        return null; // Tidak ada koneksi yang layak
    }

    @Override
    protected int startTransfer(Message m, Connection con) {
        int result = super.startTransfer(m, con);
        if(result == RCV_OK){
            //Tambahkan hop
            m.addNodeOnPath(con.getOtherNode(this.getHost()));
        }
        return result;
    }

    public void update(){
        super.update();
        if(!canStartTransfer() || isTransferring()){
            return;
        }
        // try messages that could be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return;
        }

        this.tryAllMessagesToAllConnections();
    }

    // Memeriksa apakah node ini adalah penerima akhir dari pesan
    private boolean isFinalDest(Message m, DTNHost to) {
        return m.getTo() == to;  // Mengecek apakah penerima pesan adalah node ini
    }

    // Memeriksa apakah ini adalah pengiriman pertama untuk pesan ini
    private boolean isFirstDelivery(Message m) {
        return m.getHopCount() == 0;  // Jika hop count adalah 0, berarti ini pengiriman pertama
    }

    /**
     * Dipanggil saat pesan berhasil ditransfer.
     * Mengelola:
     * - Update Q-table
     * - Pengurangan salinan (copies) jika masih bisa disebar dan kita bukan tujuan akhir.
     *
     * @param id ID pesan yang ditransfer.
     * @param from Node pengirim.
     * @return Objek Message yang diterima, atau null jika tidak ditemukan.
     */
    @Override
    public Message messageTransferred(String id, DTNHost from) {
        // Step 1: Ambil pesan yang ditransfer menggunakan logika standar dari parent class
        Message m = super.messageTransferred(id, from);

        if (m!= null){
            // Step 2: Cek apakah kita adalah tujuan akhir atau ini adalah pengiriman pertama
            boolean isFinalRecipient = isFinalDest(m, getHost());
            boolean isFirstDelivery = isFirstDelivery(m);

            // Step 3: Jika ini pengiriman pertama atau kita adalah penerima akhir,
            // maka sinkronkan Q-table antara pengirim dan penerima
            if (isFinalRecipient || isFirstDelivery) {
                Qtable senderQtable = ((ContextAwareRLRouter) from.getRouter()).getQtable();
                Qtable receiverQtable = this.getQtable();

                QTableUpdateStrategy.updateThirdStrategy(senderQtable, receiverQtable);
            }

//            // Step 4: Jika kita bukan tujuan akhir, kurangi jumlah salinan (copies)
//            if (!isFinalRecipient) {
//                Integer copies = (Integer) m.getProperty("copies");
//                if (copies != null && copies > 1) {
//                    m.updateProperty("copies", copies - 1);
//                }
//            }
        }
       return m;
    }

    @Override
    public ContextAwareRLRouter replicate() {
        return new ContextAwareRLRouter(this);
    }
}
