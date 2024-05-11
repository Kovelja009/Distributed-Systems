package servent.handler.snapshot;

import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.snapshot.DenyMessage;

import java.util.List;

public class DenyHandler implements MessageHandler {
    private SnapshotCollector snapshotCollector;
    private Message clientMessage;

    public DenyHandler(SnapshotCollector snapshotCollector, Message clientMessage) {
        this.snapshotCollector = snapshotCollector;
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        int unbornChildId = clientMessage.getOriginalSenderInfo().getId();
        int region = ((DenyMessage) clientMessage).getMasterId();
        snapshotCollector.getChildrenInfoCollector().addUnrelatedChild(unbornChildId);
        snapshotCollector.getChildrenInfoCollector().addOtherRegions(List.of(region));
    }
}
