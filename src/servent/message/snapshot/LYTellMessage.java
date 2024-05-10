package servent.message.snapshot;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class LYTellMessage extends BasicMessage {

	private static final long serialVersionUID = 3116394054726162318L;

	private LYSnapshotResult lySnapshotResult;

	public LYTellMessage(ServentInfo sender, ServentInfo receiver, LYSnapshotResult lySnapshotResult, Map<Integer, Integer> snapshotVersions) {
		super(MessageType.LY_TELL, sender, receiver, snapshotVersions);
		this.lySnapshotResult = lySnapshotResult;
	}
	
	private LYTellMessage(MessageType messageType, ServentInfo sender, ServentInfo receiver, 
			boolean white, List<ServentInfo> routeList, String messageText, int messageId,
			LYSnapshotResult lySnapshotResult, Map<Integer, Integer> snapshotVersions) {
		super(messageType, sender, receiver, white, routeList, messageText, messageId, snapshotVersions);
		this.lySnapshotResult = lySnapshotResult;
	}

	public LYSnapshotResult getLYSnapshotResult() {
		return lySnapshotResult;
	}

	@Override
	public Message setRedColor() {
		Message toReturn = new LYTellMessage(getMessageType(), getOriginalSenderInfo(), getReceiverInfo(),
				false, getRoute(), getMessageText(), getMessageId(), getLYSnapshotResult(), getSnapshotVersions());
		return toReturn;
	}

	@Override
	public Message setSnapshotVersions(Map<Integer, Integer> snapshotVersions) {
		Message toReturn = new LYTellMessage(getMessageType(), getOriginalSenderInfo(), getReceiverInfo(),
				false, getRoute(), getMessageText(), getMessageId(), getLYSnapshotResult(), new ConcurrentHashMap<>(snapshotVersions));
		return toReturn;
	}
}
