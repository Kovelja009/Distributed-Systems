package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.snapshot.NoRegionInfoMessage;

public class NoRegionInfoHandler implements MessageHandler {
    private SnapshotCollector snapshotCollector;
    private Message clientMessage;

    public NoRegionInfoHandler(SnapshotCollector snapshotCollector, Message clientMessage) {
        this.snapshotCollector = snapshotCollector;
        this.clientMessage = clientMessage;
    }
    @Override
    public void run() {
        NoRegionInfoMessage noRegionInfoMessage = (NoRegionInfoMessage) clientMessage;
        snapshotCollector.getChildrenInfoCollector().incrementNoRegionInfocnt();
        snapshotCollector.getChildrenInfoCollector().addRegionInfo(noRegionInfoMessage.getLyResults());
        snapshotCollector.getChildrenInfoCollector().addTransitInfo(noRegionInfoMessage.getTransit());
        AppConfig.timestampedStandardPrint("ALL REGIONS INFO RECEIVED: " + noRegionInfoMessage.getLyResults().size());
    }
}
