package servent.handler.snapshot;

import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;

public class ApproveHandler implements MessageHandler {
    private SnapshotCollector snapshotCollector;
    private Message clientMessage;

    public ApproveHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.snapshotCollector = snapshotCollector;
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        int childId = clientMessage.getOriginalSenderInfo().getId();
        snapshotCollector.getChildrenInfoCollector().addRelatedChild(childId);
    }
}
