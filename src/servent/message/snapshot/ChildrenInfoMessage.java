package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.List;
import java.util.Map;

public class ChildrenInfoMessage extends BasicMessage {
    private static final long serialVersionUID = 312832509576777228L;
    private List<Integer> otherRegions;
    private List<Integer> subtreeChildren;

    public ChildrenInfoMessage(ServentInfo senderInfo, ServentInfo receiverInfo, Map<Integer, Integer> snapshotVersions, List<Integer> otherRegions, List<Integer> subtreeChildren) {
        super(MessageType.CHILDREN_INFO, senderInfo,receiverInfo, snapshotVersions);
        this.otherRegions = otherRegions;
        this.subtreeChildren = subtreeChildren;
    }

    public List<Integer> getOtherRegions() {
        return otherRegions;
    }

    public List<Integer> getSubtreeChildren() {
        return subtreeChildren;
    }
}
