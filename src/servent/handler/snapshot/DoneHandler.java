package servent.handler.snapshot;

import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;

public class DoneHandler implements MessageHandler {
    private SnapshotCollector snapshotCollector;

    public DoneHandler(SnapshotCollector snapshotCollector) {
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        snapshotCollector.getChildrenInfoCollector().finishSnapshot();
    }
}
