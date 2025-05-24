package routing.contextAware.ContextMessage;

import core.DTNHost;
import core.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageListTable {

    DTNHost host;
//    List<MessagePriority> messagePriorityList;
    public final Map<String, Double> messagePriorityMap;

    public MessageListTable(DTNHost host) {
        this.host = host;
        this.messagePriorityMap = new HashMap<String, Double>();
//        this.messagePriorityList = new ArrayList<>();
    }

    public void updateMessagePriority(Message message, double priority) {
        messagePriorityMap.put(message.getId(), priority);
//        for (MessagePriority messagePriority : messagePriorityList) {
//            if(messagePriority.message.getId().equals(message.getId())) {
//                messagePriority.priority = priority;
//                return;
//            }
//        }
//        messagePriorityList.add(new MessagePriority(message, priority));
    }

    public double getPriority(Message message) {
        return messagePriorityMap.getOrDefault(message.getId(), 0.0);
//        for (MessagePriority messagePriority : messagePriorityList) {
//            if(messagePriority.message.getId().equals(message.getId())) {
//                return messagePriority.priority;
//            }
//        }
//        return 0.0;
    }

    public void removeMessage(Message message) {
        messagePriorityMap.remove(message.getId());
//        messagePriorityList.removeIf(messagePriority -> messagePriority.message.getId().equals(message.getId()));
    }

    public boolean containsMessage(Message message) {
        return messagePriorityMap.containsKey(message.getId());
//        for (MessagePriority messagePriority : messagePriorityList) {
//            if(messagePriority.message.getId().equals(message.getId())) {
//                return true;
//            }
//        }
//        return false;
    }

}
