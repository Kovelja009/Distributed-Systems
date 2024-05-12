package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.Map;

public class RegionInfoMessage extends BasicMessage {
    private static final long serialVersionUID = 388942223576777228L;

    private Map<Integer, LYSnapshotResult> collectedLYValues;
    private Map<String, Integer> transit;

    public RegionInfoMessage(ServentInfo senderInfo, ServentInfo receiverInfo, Map<Integer, Integer> snapshotVersions, Map<Integer, LYSnapshotResult> collectedLYValues, Map<String, Integer> transit) {
        super(MessageType.REGION_INFO, senderInfo, receiverInfo, snapshotVersions);
        this.collectedLYValues = collectedLYValues;
        this.transit = transit;
    }

    public Map<Integer, LYSnapshotResult> getCollectedLYValues() {
        return collectedLYValues;
    }

    public Map<String, Integer> getTransit() {
        return transit;
    }
}