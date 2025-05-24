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


import java.util.*;

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
    private Map<String, Double> pendingAging;

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
        pendingAging = new HashMap<>();

        this.deleteDelivered = true; //ACK-based deletion otomatis
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
        this.pendingAging = r.pendingAging;

        this.deleteDelivered = r.deleteDelivered;
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
            evaluateMessagePriorities();
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
//        System.out.println("[INFO] Sebelum update ENS:");
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

//            System.out.println("[DEBUG] ENS setelah saling tukar di " + getHost().getAddress() + ":");
//            this.encounteredNodeSet.printEncounterLog(getHost(), String.valueOf(neighbor.getAddress()), neighborENS);
        }

        this.tieStrength.calculateTieStrength(host, neighbor, this.encounteredNodeSet, connectionDuration);
        double TieStrength = tieStrength.getTieStrength(host, neighbor);

        double transferOpportunity = fuzzyContextAware.evaluateNeighbor(this.getHost(),neighbor, neighborBuffer, integerEnergy, neighborPop, TieStrength);

        updateQValueOnConUp(host,neighbor,transferOpportunity);
//        qtable.printQtableByHost(myId);
    }

    /**
     * Memperbarui Q-Value saat terjadi koneksi UP antara host dan neighbor.
     * Fungsi ini akan memeriksa semua pesan yang dimiliki oleh host.
     * Q-value hanya akan diperbarui jika:
     * Neighbor bukan pengirim asli pesan
     * Jika neighbor bukan tujuan akhir, maka Transfer Opportunity (TOpp) minimal 0.3
     * Q-value diperbarui menggunakan strategi pembaruan pertama.
     *
     * @param host Node yang memegang pesan dan ingin memperbarui Q-table-nya
     * @param neighbor Node yang baru terkoneksi (akan menjadi kandidat relay / action)
     * @param tfOpportunity Nilai Transfer Opportunity hasil evaluasi fuzzy logic (FLC4)
     */
    private void updateQValueOnConUp(DTNHost host, DTNHost neighbor, double tfOpportunity) {
        MessageRouter router = host.getRouter();
        Collection<Message> messages = router.getMessageCollection();
        String hostId = String.valueOf(this.getHost().getAddress());
        String actionId = String.valueOf(neighbor.getAddress());
        QTableUpdateStrategy qtableUpdate = new QTableUpdateStrategy(this.qtable);
//        System.out.println("==== [Q-UPDATE] Connection UP Detected ====");
//        System.out.println("[HOST]     : " + hostId);
//        System.out.println("[NEIGHBOR] : " + actionId);
//        System.out.println("[TOpp]     : " + tfOpportunity);
//        System.out.println("[MESSAGES] : " + messages.size());
//        System.out.printf("[BUFFER-CHECK] Node %s | Total Msgs = %d%n", hostId, messages.size());
        for (Message msg : messages){
//            System.out.printf("  → Msg ID: %s | Dest: %s | From: %s%n",
//                    msg.getId(), msg.getTo(), msg.getFrom());
            String destinationId = String.valueOf(msg.getTo().getAddress());
            String sourceId = String.valueOf(msg.getFrom().getAddress());
            // Jangan update Q jika neighbor adalah pengirim asli (menghindari loop tidak perlu
            if (actionId.equals(sourceId)) {
                System.out.println("[SKIP] Neighbor adalah pengirim asli pesan → " + msg.getId());
                continue;
            }
//            // Jika neighbor bukan tujuan & Transfer Opportunity rendah → skip
//            if (!actionId.equals(destinationId) && tfOpportunity < 0.4) {
//                System.out.println("[SKIP] Neighbor bukan tujuan & TOpp rendah → " + msg.getId());
//                continue;
//            }
            String state = hostId +","+ destinationId;
//            System.out.println("[STATE-ACTION] State = " + state + " Action = " + actionId);
            double oldQ = qtable.getQvalue(state, actionId);
            qtableUpdate.updateFirstStrategy(host, neighbor, state, actionId, tfOpportunity);
            double newQ = qtable.getQvalue(state, actionId);
//            System.out.printf("[Q-CHANGE] %s → %s | Old Q = %.4f | New Q = %.4f%n",
//                    hostId, actionId, oldQ, newQ);
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
//                System.out.printf("[DEBUG] Koneksi %s - %s diakhiri pada %.2f detik%n",
//                        this.getHost().getAddress(), neighbor.getAddress(), SimClock.getTime());
            }
            // Catat waktu putus di pendingAging
            pendingAging.put(neighborId, SimClock.getTime());
//            System.out.println("[DEBUG] Sebelum Dihapus karena terputus ");
//            this.encounteredNodeSet.printEncounterLog(this.getHost(), neighborId, neighborENS);
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
            Message Highest = getHighestPriorityMessage(true);
            if(Highest == null){
                // Tidak ada lagi pesan yang bisa dihapus, gagal membuat ruang
                return false;
            }
            // Validasi sinkronisasi sebelum menghapus dari messageListTable
            if (!messageListTable.containsMessage(Highest)) {
                System.out.printf("[WARNING] Trying to remove message %s " +
                        "but it's not in messageListTable!\n", Highest.getId());
            }
            // Hapus pesan dari buffer (mode drop = true)
            deleteMessage(Highest.getId(),true);
            messageListTable.removeMessage(Highest);
            // Tambah ruang kosong berdasarkan ukuran pesan yang dihapus
            freeBuffer += Highest.getSize();

            System.out.printf("[BUFFER-DROP] Dropping message %s | priority = %.4f\n",
                    Highest.getId(),
                    messageListTable.getPriority(Highest));
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
//    private Message getLowestPriorityMessage(boolean excludeMsgBeingSent){
//        Collection<Message> messages =this.getMessageCollection();
//        Message lowest=null;
//        for (Message m : messages){
//            // Lewati pesan yang sedang dikirim (jika diminta)
//            if(excludeMsgBeingSent && isSending(m.getId())) continue;
//            // Update jika belum ada kandidat, atau jika pesan ini lebih rendah prioritasnya
//            if(lowest==null || messageListTable.getPriority(m) < messageListTable.getPriority(lowest)){
//                lowest = m;
//            }
//        }
//        return lowest;
//    }
    private Message getHighestPriorityMessage(boolean excludeMsgBeingSent){
        Collection<Message> messages = this.getMessageCollection();
        Message highest = null;
        for (Message m : messages){
            if (excludeMsgBeingSent && isSending(m.getId())) continue;
            if (highest == null || messageListTable.getPriority(m) > messageListTable.getPriority(highest)) {
                highest = m;
            }
        }
        return highest;
    }

    /**
     * Mengurutkan daftar pesan berdasarkan nilai prioritas fuzzy (dari tinggi ke rendah).
     * Prioritas masing-masing pesan diambil dari {@code messageListTable} yang telah diisi sebelumnya.
     * Digunakan untuk menentukan urutan pengiriman pesan yang paling penting lebih dulu.
     *
     * @param messages Daftar pesan (List<Message>) yang akan diurutkan.
     */
    protected void sortByFuzzyPriority(List<Message> messages) {
        // Urutkan pesan berdasarkan nilai prioritas fuzzy secara menurun (descending)
        messages.sort(( m1, m2) -> {
            // Ambil nilai prioritas dari masing-masing pesan
            double p1 = messageListTable.getPriority(m1);
            double p2 = messageListTable.getPriority(m2);
            // Urutkan dari prioritas tertinggi ke terendah
            return Double.compare(p2, p1); //Desending (prioritas tinggi di awal)
        });
//        System.out.println("[SORT] Urutan pesan berdasarkan prioritas:");
//        for (Message m : messages) {
//            double pr = messageListTable.getPriority(m);
//            System.out.printf("  → %s (Priority: %.4f)\n", m.getId(), pr);
//        }
    }

    private void evaluateMessagePriorities(){
        Collection<Message> messages = this.getMessageCollection();
        for(Message m : messages){
            double priority = fuzzyContextMsg.evaluateMsg(this.getHost(), m.getTtl(), m.getHopCount());
            messageListTable.updateMessagePriority(m, priority);
        }
    }

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
        // Urutkan menggunakan sortByQueueMode()
        sortByFuzzyPriority(messages);
        for (Message msg : messages){
            for (Connection con : connections){
                copiesControlMechanism(msg, con);
            }
        }
        return null;
    }
//        for (Connection con : connections){
//            for(Message msg : messages){
//                if(copiesControlMechanism(msg, con)){
//                    return con;
//                }
//            }
//        }

//        // 3. Proses forwarding berdasarkan urutan prioritas
//        for (Message msg : messages) {
//
////            // Evaluasi prioritas berdasarkan TTL dan hopCount menggunakan modul FLC
////            double priority = fuzzyContextMsg.evaluateMsg(this.getHost(), msg.getTtl(), msg.getHopCount());
////            // Simpan atau update prioritas pesan tersebut ke dalam table
////            messageListTable.updateMessagePriority(msg, priority); // Menyimpan Prioritas di table
//
//            Connection conn = copiesControlMechanism(msg, connections);
//            if (conn != null) {
//                return conn;
//            }
//        }


    private boolean copiesControlMechanism(Message msg, Connection c){
        DTNHost host = this.getHost();
        DTNHost destination = msg.getTo();
        String hostId = String.valueOf(host.getAddress());
        String destinationId = String.valueOf(destination.getAddress());

        DTNHost neighbor = c.getOtherNode(host);
        String neighborId = String.valueOf(neighbor.getAddress());

        // 1. Cek apakah neighbor adalah pengirim asli pesan
        String sourceId = String.valueOf(msg.getFrom().getAddress());
        if (neighborId.equals(sourceId)) {
//            System.out.printf("[SKIP] Neighbor %s adalah pengirim asli pesan %s%n", neighborId, msg.getId());
            return false;
        }

        boolean isDestination = neighborId.equals(destinationId);
        double messagePriority = messageListTable.getPriority(msg);
        int copies = (int) msg.getProperty("copies");

        // Forward langsung ke tujuan jika sedang terkoneksi ---
        if (isDestination && c.isReadyForTransfer()) {
            if (copies > 1) {
                msg.updateProperty("copies", copies - 1);
            } else {
                if(this.hasMessage(msg.getId())){
                    deleteMessage(msg.getId(), true);
                }
            }
            startTransfer(msg, c);
            return true;
        }

        // --- Forward ke neighbor berdasarkan FLC dan Q-Value ---
        if(c.isReadyForTransfer()){
            double myPop = popularity.getPopularity(host);
            double tie  = tieStrength.getTieStrength(host, neighbor);
            double mySocial = fuzzyContextAware.evaluateSelf(host, myPop, tie);

            double neighborPop  = popularity.getPopularity(neighbor);
            double neighborSocial = fuzzyContextAware.evaluateSelf(neighbor, neighborPop, tie);

            String state = hostId + "," + destinationId;
            String reverseState = neighborId + "," + destinationId;
            double myQ = qtable.getQvalue(state, neighborId);
            double neighborQ = qtable.getQvalue(reverseState, hostId);

            boolean socialBetter = neighborSocial > mySocial;
            boolean qValueBetter = neighborQ > myQ;

            // --- Jika punya lebih dari 1 salinan ---
            if (copies > 1 && messagePriority >= 0.5 && (socialBetter || qValueBetter)) {
                int sendCopies = copies / 2;
                int remainingCopies = copies - sendCopies;

                Message copy = msg.replicate();
                copy.setTtl(msg.getTtl());
                copy.updateProperty("copies", sendCopies);

                MessageRouter targetRouter = neighbor.getRouter();
                boolean alReadyHasMessage = targetRouter.getMessageCollection().stream()
                        .anyMatch(m -> m.getId().equals(msg.getId()));

                if (!alReadyHasMessage) {
                    int result = startTransfer(copy, c);
                    if (result == RCV_OK) {
                        msg.updateProperty("copies", remainingCopies);
                        return true;
                    } else {
                        System.out.printf("[TRANSFER FAIL] %s → %s | BUFFER %.2f | Result: %d%n",
                                hostId, neighborId, (double) targetRouter.getFreeBufferSize(), result);
                    }
                }
//                else {
//                    System.out.printf("[SKIP] Node %s sudah punya pesan %s%n", neighborId, msg.getId());
//                }
            }
            // --- Jika hanya tersisa 1 salinan (final hop) ---
            else if (copies == 1) {
                if(isDestination){
                    int result = startTransfer(msg, c);
                    if(result == RCV_OK){
                        if(this.hasMessage(msg.getId())){
                            deleteMessage(msg.getId(), true);
                            return true;
                        }
                    }
                }
                else if ((messagePriority >= 0.5 && socialBetter && qValueBetter) ||
                        (messagePriority < 0.5 && (socialBetter || qValueBetter))) {
                    int result = startTransfer(msg, c);
                    if(result == RCV_OK){
                        if(this.hasMessage(msg.getId())){
                            deleteMessage(msg.getId(), true);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

//    /**
//     * Mengelola kontrol salinan pesan berdasarkan mekanisme Spray and Focus yang ditingkatkan.
//     * Forwarding diputuskan berdasarkan apakah tetangga adalah tujuan, prioritas pesan,
//     * nilai sosial (social importance), dan nilai Q-value antar node.
//     * Salinan pesan hanya disebar setengah jika memenuhi syarat, untuk mengurangi overhead jaringan.
//     *
//     * @param msg pesan yang ada pada node yang menjalankan metode ini.
//     * @param connections Daftar koneksi aktif antara node saat ini dan node tetangganya.
//     * @return Koneksi yang digunakan untuk transfer (jika ada), null jika tidak ada yang cocok.
//     */
//    private Connection copiesControlMechanism(Message msg, List<Connection> connections) {
//        DTNHost host = this.getHost();
//        String hostId = String.valueOf(this.getHost().getAddress());
//        DTNHost destination = msg.getTo();
//        String destinationId = String.valueOf(destination.getAddress());
//
//        // Prioritas pesan di MessageListTable
//        double messagePriority = messageListTable.getPriority(msg);
//        int copies = (int) msg.getProperty("copies"); // Mengambil copies yang sudah di inisialisasi createM
//
//        // Iterasi untuk memeriksa setiap koneksi yang tersedia antara host dan tetangga
//        for (Connection c : connections) {
//            DTNHost neighbor = c.getOtherNode(host);
//            String neighborId = String.valueOf(neighbor.getAddress());
//            boolean isDestination = neighborId.equals(destinationId);
//
//            // --- PRIORITAS 1: Forward langsung ke tujuan jika sedang terkoneksi ---
//            if (isDestination && c.isReadyForTransfer()) {
//                if (copies > 1) {
//                    msg.updateProperty("copies", copies - 1);
//                } else {
//                    deleteMessage(msg.getId(), true);
//                }
//                startTransfer(msg, c);
//                return c;
//            }
//        }
//        // Cari tetangga terbaik menurut Q-value
//        Connection bestConnection = selectBestNeighbor(msg);
//        if (bestConnection == null || !connections.contains(bestConnection)) {
//            return null; // Tidak ada tetangga terbaik yang sedang terkoneksi
//        }
//        DTNHost bestNeighbor = bestConnection.getOtherNode(host);
//        String bestNeighborId = String.valueOf(bestNeighbor.getAddress());
//
//        if(canStartTransfer() && !bestConnection.isTransferring()) {
//            // Hitung nilai social importance node ini dan neighbor
//            double myPop = popularity.getPopularity(host);
//            double tie = tieStrength.getTieStrength(this.getHost(), bestNeighbor);
//            double mySocial = fuzzyContextAware.evaluateSelf(this.getHost(), myPop, tie);
//            double neighborPop = popularity.getPopularity(bestNeighbor);
//            double neighborSocial = fuzzyContextAware.evaluateSelf(bestNeighbor, neighborPop, tie);
//
//            // Mengambil Q-value untuk host dan tetangga berdasarkan state tujuan pesan
//            String state = hostId +","+ destinationId;
//            double myQ = qtable.getQvalue(state, bestNeighborId);
//            String stateNeighbor = bestNeighborId +","+ destinationId;
//            double neighborQ = qtable.getQvalue(stateNeighbor, hostId);
//
//            // Evaluasi apakah neighbor lebih baik dari segi sosial atau Q-value
//            boolean socialBetter = neighborSocial > mySocial;
//            boolean qvalueBetter = neighborQ > myQ;
//
//            if(copies > 1 && messagePriority >= 0.5 && (socialBetter || qvalueBetter)){
//                int sendCopies = copies/2; // Membagi salinan pesan untuk dikirim
//                int remainingCopies = copies - sendCopies; // Salinan yang tersisa
//
//                Message copy = msg.replicate(); // Membuat salinan pesan
//                copy.setTtl(msg.getTtl());
//                copy.updateProperty("copies", sendCopies); // Menambahkan jumlah salinan untuk salinan pesan baru
//
//                DTNHost targetNode = bestConnection.getOtherNode(getHost());
//                MessageRouter targetRouter = targetNode.getRouter();
//
//                // Mengecek apakah target node sudah memiliki pesan dengan ID yang sama
//                boolean alreadyHasMessage = false;
//                for (Message m : targetRouter.getMessageCollection()) {
//                    if (m.getId().equals(msg.getId())) {
//                        alreadyHasMessage = true;
//                        break;
//                    }
//                }
//                System.out.println("[DEBUG] Trying to send msg " + msg.getId() +
//                        " to node " + targetNode.getAddress());
//                System.out.println("[DEBUG] Connection isUp: " + bestConnection.isUp());
//                System.out.println("[DEBUG] Can start transfer: " + bestConnection.isTransferring());
//                System.out.println("[DEBUG] Node has message already? " + alreadyHasMessage);
//                int result = startTransfer(copy, bestConnection); // Coba kirim salinan
//                if(result == RCV_OK){
//                    // Jika pengiriman salinan berhasil, baru kurangi salinan dari pesan asli
//                    msg.updateProperty("copies", remainingCopies); // Mengupdate jumlah salinan yang tersisa
//                    return bestConnection;
//                } else{
//                    System.out.println("[DEBUG] Target node buffer size: " + targetRouter.getFreeBufferSize());
//                    System.out.println("[DEBUG] Message size: " + copy.getSize());
//                    double bufferSpace = getFreeBufferSize();
//                    System.out.println("[TRANSFER FAIL] Message " + msg.getId() +
//                            " not sent to " + bestNeighbor.getAddress() +
//                            ". Buffer space: " + bufferSpace);
//                }
//                // Jika gagal kirim, tidak jadi update copies, dan tidak return
//
//                // Kondisi jika jumlah salinan pesan adalah 1
//                // Jika hanya tersisa 1 copy, lakukan forwarding final
//            } else if (copies == 1) {
//                if (bestNeighborId.equals(destinationId)) {
//
//                    int result = startTransfer(msg, bestConnection);
//                    if (result == RCV_OK) {
//                        deleteMessage(msg.getId(), true);
//                        return bestConnection;
//                    }
//                } else if ((messagePriority >= 0.5 && socialBetter && qvalueBetter)
//                        || (messagePriority < 0.5 && (socialBetter || qvalueBetter))) {
//
//                    int result = startTransfer(msg, bestConnection);
//                    if (result == RCV_OK) {
//                        deleteMessage(msg.getId(), true);
//                        return bestConnection;
//                    }
//                }
//            }
//        }
//        return null; // Tidak ada koneksi yang layak
//    }

    /**
     * Override proses transfer pesan untuk mencatat hop dan menangani pengurangan salinan (copies)
     * hanya untuk pesan asli (bukan hasil replicate).
     *
     * @param m pesan yang akan ditransfer
     * @param con koneksi tujuan transfer
     * @return status hasil transfer (RCV_OK, TRY_LATER, dsb)
     */
    @Override
    protected int startTransfer(Message m, Connection con) {
        int result = super.startTransfer(m, con);
        if(result == RCV_OK){
            //Tambahkan hop
            m.addNodeOnPath(con.getOtherNode(this.getHost()));
        }
        // Pesan ditolak karena sudah sampai tujuan
        else if (result == DENIED_OLD && m.getTo() == con.getOtherNode(this.getHost())) {
            if (this.hasMessage(m.getId())) {
                System.out.printf("[ACK-DELETE] Pesan %s dihapus karena sudah sampai tujuan %s%n",
                        m.getId(), m.getTo().getAddress());
                deleteMessage(m.getId(), false);
            }
        }
        return result;
    }



    public void update(){
        super.update();
        // Bersihkan pesan dengan TTL kadaluarsa
        this.dropExpiredMessages();

        if (isTransferring() || !canStartTransfer()) {
            return; // transferring, don't try other connections yet
        }

        // Try first the messages that can be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return; // started a transfer, don't try others (yet)
        }
        QTableUpdateStrategy update = new QTableUpdateStrategy(this.qtable);
        update.processDelayedAging(this.getHost(), pendingAging);

        tryAllMessagesToAllConnections();
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
        }
       return m;
    }

    @Override
    public ContextAwareRLRouter replicate() {
        return new ContextAwareRLRouter(this);
    }
}
