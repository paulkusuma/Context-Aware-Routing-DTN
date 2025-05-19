package routing.contextAware.ContextMessage;

import core.DTNHost;
import core.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageListTable {

    DTNHost host;
    List<MessagePriority> messagePriorityList;

    public MessageListTable(DTNHost host) {
        this.host = host;
        this.messagePriorityList = new ArrayList<>();
    }

    public void updateMessagePriority(Message message, double priority) {
        for (MessagePriority messagePriority : messagePriorityList) {
            if(messagePriority.message.getId().equals(message.getId())) {
                messagePriority.priority = priority;
                return;
            }
        }
        messagePriorityList.add(new MessagePriority(message, priority));
    }

    public double getPriority(Message message) {
        for (MessagePriority messagePriority : messagePriorityList) {
            if(messagePriority.message.getId().equals(message.getId())) {
                return messagePriority.priority;
            }
        }
        return 0.0;
    }

    public void removeMessageLowPriority(Message message) {
        messagePriorityList.removeIf(messagePriority -> messagePriority.message.getId().equals(message.getId()));
    }

}
