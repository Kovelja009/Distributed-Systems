package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.Map;

public class DoneMessage extends BasicMessage {
    private static final long serialVersionUID = 388942509777228L;

    public DoneMessage(ServentInfo senderInfo, ServentInfo receiverInfo, Map<Integer, Integer> snapshotVersions) {
        super(MessageType.DONE, senderInfo, receiverInfo, snapshotVersions);
    }
}
