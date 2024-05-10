package app.snapshot_bitcake;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import app.AppConfig;
import servent.message.Message;
import servent.message.snapshot.LYMarkerMessage;
import servent.message.snapshot.LYTellMessage;
import servent.message.util.MessageUtil;

public class LaiYangBitcakeManager implements BitcakeManager {

	private final AtomicInteger currentAmount = new AtomicInteger(1000);
	
	public void takeSomeBitcakes(int amount) {
		currentAmount.getAndAdd(-amount);
	}

	public static final Object historyLock = new Object();
	
	public void addSomeBitcakes(int amount) {
		currentAmount.getAndAdd(amount);
	}
	
	public int getCurrentBitcakeAmount() {
		return currentAmount.get();
	}
	
	private Map<Integer, Integer> giveHistory = new ConcurrentHashMap<>();
	private Map<Integer, Integer> getHistory = new ConcurrentHashMap<>();

	/*
	 * give and get history for Li variation
	 * initiator -> (neighbour, amount)
	 */

	private Map<Integer, Map<Integer, Integer>> giveHistoryLi = new ConcurrentHashMap<>();
	private Map<Integer, Map<Integer, Integer>> getHistoryLi = new ConcurrentHashMap<>();
	
	public LaiYangBitcakeManager() {
		for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			giveHistory.put(neighbor, 0);
			getHistory.put(neighbor, 0);
		}


		// hopefully this will work
		for(int initiator : AppConfig.getInitiators()) {
			Map<Integer, Integer> giveMap = new ConcurrentHashMap<>();
			Map<Integer, Integer> getMap = new ConcurrentHashMap<>();
			for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
				giveMap.put(neighbor, 0);
				getMap.put(neighbor, 0);
			}
			giveHistoryLi.put(initiator, giveMap);
			getHistoryLi.put(initiator, getMap);
		}
	}
	
	/*
	 * This value is protected by AppConfig.colorLock.
	 * Access it only if you have the blessing.
	 */
	public int recordedAmount = 0;
	
	public void markerEvent(int collectorId, SnapshotCollector snapshotCollector) {
		synchronized (AppConfig.colorLock) {
//			AppConfig.isWhite.set(false);
			recordedAmount = getCurrentBitcakeAmount();

			LYSnapshotResult snapshotResult = new LYSnapshotResult(
					AppConfig.myServentInfo.getId(), recordedAmount, giveHistoryLi.get(collectorId), getHistoryLi.get(collectorId));

			// reset history for given collector
			resetGiveHistory(collectorId);
			resetGetHistory(collectorId);


			// we are initiating the snapshot
			if (collectorId == AppConfig.myServentInfo.getId()) {
				snapshotCollector.addLYSnapshotInfo(
						AppConfig.myServentInfo.getId(),
						snapshotResult);
			} else {


				// we need to send it to the initiator
				Message tellMessage = new LYTellMessage(
						AppConfig.myServentInfo, AppConfig.getInfoById(collectorId), snapshotResult, AppConfig.snapshotVersions);
				
				MessageUtil.sendMessage(tellMessage);
			}

			// send markers to neighbors
			for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
				Message lyMarker = new LYMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId, AppConfig.snapshotVersions);
				MessageUtil.sendMessage(lyMarker);
				try {
					/*
					 * This sleep is here to artificially produce some white node -> red node messages.
					 * Not actually recommended, as we are sleeping while we have colorLock.
					 */
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void resetGiveHistory(int collector){
		Map<Integer, Integer> collectorHistory =  giveHistoryLi.get(collector);
			for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()){
				collectorHistory.put(neighbor, 0);
			}
	}

	private void resetGetHistory(int collector){
		Map<Integer, Integer> collectorHistory =  getHistoryLi.get(collector);
			for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()){
				collectorHistory.put(neighbor, 0);
			}
	}


	
	private class MapValueUpdater implements BiFunction<Integer, Integer, Integer> {
		
		private int valueToAdd;
		
		public MapValueUpdater(int valueToAdd) {
			this.valueToAdd = valueToAdd;
		}
		
		@Override
		public Integer apply(Integer key, Integer oldValue) {
			return oldValue + valueToAdd;
		}
	}

	public void recordGiveTransactionLi(int neighbour, int amount) {
		synchronized (AppConfig.colorLock) {
			for(Map<Integer, Integer> history : giveHistoryLi.values())
				history.compute(neighbour, new MapValueUpdater(amount));
		}
	}

	public void recordGetTransactionLi(int neighbour, int amount) {
		synchronized (AppConfig.colorLock) {
			for(Map<Integer, Integer> history : getHistoryLi.values())
				history.compute(neighbour, new MapValueUpdater(amount));
		}
	}
	
//	public void recordGiveTransaction(int neighbor, int amount) {
//		giveHistory.compute(neighbor, new MapValueUpdater(amount));
//	}
//
//	public void recordGetTransaction(int neighbor, int amount) {
//		getHistory.compute(neighbor, new MapValueUpdater(amount));
//	}
}
