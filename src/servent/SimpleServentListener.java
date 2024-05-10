package servent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.AppConfig;
import app.Cancellable;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.handler.NullHandler;
import servent.handler.TransactionHandler;
import servent.handler.snapshot.LYMarkerHandler;
import servent.handler.snapshot.LYTellHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.LYTellMessage;
import servent.message.util.MessageUtil;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;
	
	private SnapshotCollector snapshotCollector;
	
	public SimpleServentListener(SnapshotCollector snapshotCollector) {
		this.snapshotCollector = snapshotCollector;
	}

	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	
	private List<Message> redMessages = new ArrayList<>();

	private static final int ATTEMPTS = 500;
	private static int currAttempt = 0;
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100);
			/*
			 * If there is no connection after 1s, wake up and see if we should terminate.
			 */
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
			System.exit(0);
		}
		
		
		while (working) {
			try {
				Message clientMessage;
				
				/*
				 * Lai-Yang stuff. Process any red messages we got before we got the marker.
				 * The marker contains the collector id, so we need to process that as our first
				 * red message. 
				 */

				// if we have unhandled messages (transaction which are from the "future", we might be able to handle them)
				// if we still can't handle them, we will put them back in the syncronized block
				if (redMessages.size() > 0 && currAttempt < ATTEMPTS) {
					clientMessage = redMessages.remove(0);
					currAttempt++;
				} else {
					/*
					 * This blocks for up to 1s, after which SocketTimeoutException is thrown.
					 */
					Socket clientSocket = listenerSocket.accept();
					
					//GOT A MESSAGE! <3
					clientMessage = MessageUtil.readMessage(clientSocket);
					currAttempt = 0;
				}
				synchronized (AppConfig.colorLock) {
					// we have newer massage and it is not tell message (because tell might be newer due to other's nodes snapshots)
					if (AppConfig.isFirstSnapshotGreater(clientMessage.getSnapshotVersions(), AppConfig.snapshotVersions) && clientMessage.getMessageType() != MessageType.LY_TELL) {
						// if message snapshotversion is bigger than our snapshotversion, store it because we need marker first
						if(clientMessage.getMessageType() != MessageType.LY_MARKER) {
							redMessages.add(clientMessage);
							continue;
						} else {
							// This is the marker, we need to pass it to the other nodes (if initiator message snapshot is greater than our own initiator snapshot)
							int initiatorId = Integer.parseInt(clientMessage.getMessageText());
							int messageSnapshot = clientMessage.getSnapshotVersions().get(initiatorId);
							int mySnapshot = AppConfig.snapshotVersions.get(initiatorId);

							if(messageSnapshot > mySnapshot) {
								// also updating our snapshot version regarding this initiator
								AppConfig.snapshotVersions.put(initiatorId, messageSnapshot);

								LaiYangBitcakeManager lyFinancialManager =
										(LaiYangBitcakeManager)snapshotCollector.getBitcakeManager();
								lyFinancialManager.markerEvent(
										Integer.parseInt(clientMessage.getMessageText()), snapshotCollector);
							}
						}
					}
					// if message is marker then we need to pass it to other nodes
					// STARO
//
//
//					if (clientMessage.isWhite() == false && AppConfig.isWhite.get()) {
//						/*
//						 * If the message is red, we are white, and the message isn't a marker,
//						 * then store it. We will get the marker soon, and then we will process
//						 * this message. The point is, we need the marker to know who to send
//						 * our info to, so this is the simplest way to work around that.
//						 */
//						if (clientMessage.getMessageType() != MessageType.LY_MARKER) {
//							redMessages.add(clientMessage);
//							continue;
//						} else {
//							// This is the marker, we need to pass it to the other nodes
//							LaiYangBitcakeManager lyFinancialManager =
//									(LaiYangBitcakeManager)snapshotCollector.getBitcakeManager();
//							lyFinancialManager.markerEvent(
//									Integer.parseInt(clientMessage.getMessageText()), snapshotCollector);
//						}
//					}
				}
				
				MessageHandler messageHandler = new NullHandler(clientMessage);
				
				/*
				 * Each message type has it's own handler.
				 * If we can get away with stateless handlers, we will,
				 * because that way is much simpler and less error prone.
				 */
				switch (clientMessage.getMessageType()) {
				case TRANSACTION: // standard message
					messageHandler = new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager());
					break;
				case LY_MARKER: // marker message
					messageHandler = new LYMarkerHandler();
					break;
				case LY_TELL: // received snapshot
					messageHandler = new LYTellHandler(clientMessage, snapshotCollector);
				}
				
				threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		this.working = false;
		AppConfig.timestampedStandardPrint("Red messages: " + redMessages.size());
	}

}
