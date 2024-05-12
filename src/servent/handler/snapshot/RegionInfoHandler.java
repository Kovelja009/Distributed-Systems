package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.snapshot.RegionInfoMessage;

public class RegionInfoHandler implements MessageHandler {
    private SnapshotCollector snapshotCollector;
    private Message clientMessage;

    public RegionInfoHandler(SnapshotCollector snapshotCollector, Message clientMessage) {
        this.snapshotCollector = snapshotCollector;
        this.clientMessage = clientMessage;
    }
    @Override
    public void run() {
        // increment regionInfoCounter and add new info to the otherRegion results
        RegionInfoMessage rim = (RegionInfoMessage)clientMessage;
        snapshotCollector.getChildrenInfoCollector().addRegionInfo(rim.getCollectedLYValues());
        snapshotCollector.getChildrenInfoCollector().addTransitInfo(rim.getTransit());
        AppConfig.timestampedStandardPrint("Region info received is: " + rim.getCollectedLYValues());
        snapshotCollector.getChildrenInfoCollector().incrementRegionInfocnt();
    }
}
