package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.Map;

public class ApproveMessage extends BasicMessage {

        private static final long serialVersionUID = 388942509576777228L;

        public ApproveMessage(ServentInfo senderInfo, ServentInfo receiverInfo, Map<Integer, Integer> snapshotVersions) {
            super(MessageType.APPROVE, senderInfo, receiverInfo, snapshotVersions);
        }
}
