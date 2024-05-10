package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.Map;

public class LYMarkerMessage extends BasicMessage {

	private static final long serialVersionUID = 388942509576636228L;

	public LYMarkerMessage(ServentInfo sender, ServentInfo receiver, int collectorId, Map<Integer, Integer> snapshotVersions) {
		super(MessageType.LY_MARKER, sender, receiver, String.valueOf(collectorId), snapshotVersions);
	}
}
