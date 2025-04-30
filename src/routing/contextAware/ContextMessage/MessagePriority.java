package routing.contextAware.ContextMessage;

import core.Message;

public class MessagePriority {
    public Message message;
    public double priority;

    public MessagePriority(Message message, double priority) {
        this.message = message;
        this.priority = priority;
    }
}
