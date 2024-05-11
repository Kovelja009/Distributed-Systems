package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.Map;

public class DenyMessage  extends BasicMessage {
    private static final long serialVersionUID = 311942509576777228L;
    private Integer masterId;


    public DenyMessage(ServentInfo senderInfo, ServentInfo receiverInfo, Map<Integer, Integer> snapshotVersions, Integer masterId) {
        super(MessageType.DENY, senderInfo,receiverInfo, snapshotVersions);
        this.masterId = masterId;
    }

    public Integer getMasterId() {
        return masterId;
    }
}
