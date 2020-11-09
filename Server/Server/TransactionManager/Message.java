package Server.TransactionManager;

public class Message {

    private MessageType m;

    public enum MessageType {
        RESET_TIMER,
        CLOSE_TIMER,
        ABORT_TRANSACTION
    };

    public Message(MessageType message) {
        m = message;
    }
    
    public MessageType getMessage() {
        return m;
    }
}
