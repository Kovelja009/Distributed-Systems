package servent.handler.snapshot;

import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.snapshot.ChildrenInfoMessage;

public class ChildrenInfoHandler implements MessageHandler {
    private SnapshotCollector snapshotCollector;
    private Message clientMessage;

    public ChildrenInfoHandler(SnapshotCollector snapshotCollector, Message clientMessage) {
        this.snapshotCollector = snapshotCollector;
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        ChildrenInfoMessage childrenInfoMessage = (ChildrenInfoMessage) clientMessage;
        snapshotCollector.getChildrenInfoCollector().addOtherRegions(childrenInfoMessage.getOtherRegions());
        snapshotCollector.getChildrenInfoCollector().addSubtreeSize(childrenInfoMessage.getOriginalSenderInfo().getId(), childrenInfoMessage.getSubtreeSize());
    }
}
