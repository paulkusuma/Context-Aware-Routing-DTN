package routing.contextAware.DensityMCopies;

import core.Connection;
import core.DTNHost;
import core.Message;
import reinforcementLearning_ContextAware.Qtable;
import routing.contextAware.ContextMessage.MessageListTable;
import routing.contextAware.FuzzyLogic.FuzzyContextAware;
import routing.contextAware.FuzzyLogic.FuzzyContextMsg;

public class CopyControlMechanism {

    private static int copies;
    private MessageListTable messageListTable;

    public CopyControlMechanism(MessageListTable messageListTable) {
        this.messageListTable = messageListTable;
    }

    public static void updateMessageCopies(int newCopies) {
        copies = newCopies;
    }
    public static int getCopies() {
        return copies;
    }

    public void messageCopiesControl(DTNHost host, Message message, Connection connection, Qtable qtable) {

        for (Message m : host.getMessageCollection()){
            DTNHost peer = connection.getOtherNode(host);
            DTNHost destination = m.getTo(); //detinasi pesan

            double messagePriority = messageListTable.getPriority(m);
            int messageCopies = getCopies();

        }
    }


}
