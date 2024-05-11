package app.snapshot_bitcake;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import app.AppConfig;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	
	private AtomicBoolean collecting = new AtomicBoolean(false);
	
	private Map<Integer, LYSnapshotResult> collectedLYValues;
	
	private BitcakeManager bitcakeManager;

	public SnapshotCollectorWorker(ChildrenInfoCollector childrenInfoCollector, Map<Integer, LYSnapshotResult> collectedLYValues) {
		bitcakeManager = new LaiYangBitcakeManager(childrenInfoCollector);
		this.collectedLYValues = collectedLYValues;

	}
	
	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}
	
	@Override
	public void run() {
		while(working) {
			
			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (collecting.get() == false) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */
			
			// 1 send asks
			synchronized (AppConfig.colorLock) {

				// update our (initiator) snapshot version
				AppConfig.snapshotVersions.compute(AppConfig.myServentInfo.getId(), (k, v) -> v + 1);
				// we are our own parent and master
				AppConfig.parent = AppConfig.myServentInfo.getId();
				AppConfig.master = AppConfig.myServentInfo.getId();

				// send markers to neighbors
				((LaiYangBitcakeManager)bitcakeManager).markerEvent(AppConfig.myServentInfo.getId(), this, AppConfig.myServentInfo.getId());
			}

//			// 2 wait for responses or finish
//			boolean waiting = true;
//			while (waiting) {
//				if (collectedLYValues.size() == AppConfig.getServentCount()) {
//					waiting = false;
//				}
//
//				try {
//					Thread.sleep(1000);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//
//				if (working == false) {
//					return;
//				}
//			}
//
//			// print
//			int sum;
//			sum = 0;
//			for (Entry<Integer, LYSnapshotResult> nodeResult : collectedLYValues.entrySet()) {
//				sum += nodeResult.getValue().getRecordedAmount();
//				AppConfig.timestampedStandardPrint(
//						"Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount());
//			}
//
//			for(int i = 0; i < AppConfig.getServentCount(); i++) {
//				for (int j = 0; j < AppConfig.getServentCount(); j++) {
//					if (i != j) {
//						if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
//							AppConfig.getInfoById(j).getNeighbors().contains(i)) {
//
//							// transit = transit + give - get
//
//							// give
//							int ijAmount = collectedLYValues.get(i).getGiveHistory().get(j);
//
//							// get
//							int jiAmount = collectedLYValues.get(j).getGetHistory().get(i);
//
////							if(ijAmount != 0 || jiAmount != 0){
////								AppConfig.timestampedStandardPrint("---------------");
////								AppConfig.timestampedStandardPrint("Servent " + i + " gave " + ijAmount + " bitcakes to " + j);
////								AppConfig.timestampedStandardPrint("Servent " + j + " got  " + jiAmount + " bitcakes from " + i);
////
////							}
//
//							String transitKey= i + "-" + j;
//							int transitAmount = 0;
//
//							transitAmount = AppConfig.transit.get(transitKey) + ijAmount - jiAmount;
//							AppConfig.transit.put(transitKey, transitAmount);
//
//							if (transitAmount != 0) {
//								String outputString = String.format(
//										"Unreceived bitcake amount: %d from servent %d to servent %d",
//										transitAmount, i, j);
//								AppConfig.timestampedStandardPrint(outputString);
//								sum += transitAmount;
//							}
//						}
//					}
//				}
//			}
//
//			AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
//
//			AppConfig.timestampedStandardPrint("==================================================================================");
//
//			collectedLYValues.clear(); //reset for next invocation


			// wait until childInfoCollector finishes
			while (((LaiYangBitcakeManager) bitcakeManager).getChildrenInfoCollector().isCollecting()){
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			collecting.set(false);
		}

	}
	
	@Override
	public void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult) {
		try {
			collectedLYValues.put(id, lySnapshotResult);
		} catch (Exception e) {
			AppConfig.timestampedErrorPrint("Couldn't add addLYSnapshotInfo info for " + id);
			e.printStackTrace();
		}
	}

	@Override
	public Map<Integer, LYSnapshotResult> getCollectedLYValues() {
		return collectedLYValues;
	}

	@Override
	public ChildrenInfoCollector getChildrenInfoCollector() {
		return ((LaiYangBitcakeManager)bitcakeManager).getChildrenInfoCollector();
	}

	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue == true) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}
	
	@Override
	public void stop() {
		working = false;
	}

}
