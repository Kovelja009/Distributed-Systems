package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.List;
import java.util.Map;

public class ChildrenInfoMessage extends BasicMessage {
    private static final long serialVersionUID = 312832509576777228L;
    private List<Integer> otherRegions;
    private Integer subtreeSize;

    public ChildrenInfoMessage(ServentInfo senderInfo, ServentInfo receiverInfo, Map<Integer, Integer> snapshotVersions, List<Integer> otherRegions, Integer subtreeSize) {
        super(MessageType.CHILDREN_INFO, senderInfo,receiverInfo, snapshotVersions);
        this.otherRegions = otherRegions;
        this.subtreeSize = subtreeSize;
    }

    public List<Integer> getOtherRegions() {
        return otherRegions;
    }

    public Integer getSubtreeSize() {
        return subtreeSize;
    }
}
