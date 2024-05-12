package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.Map;

public class NoRegionInfoMessage extends BasicMessage {

        private static final long serialVersionUID = 388942121276777228L;
        private Map<Integer, LYSnapshotResult> lyResults;
        private Map<String, Integer> transit;

    public NoRegionInfoMessage(ServentInfo senderInfo, ServentInfo receiverInfo, Map<Integer, Integer> snapshotVersions, Map<Integer, LYSnapshotResult> results, Map<String, Integer> transit) {
            super(MessageType.NO_REGION_INFO, senderInfo, receiverInfo, snapshotVersions);
            this.lyResults = results;
            this.transit = transit;
        }

    public Map<Integer, LYSnapshotResult> getLyResults() {
        return lyResults;
    }

    public Map<String, Integer> getTransit() {
        return transit;
    }
}
