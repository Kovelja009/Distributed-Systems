package app.snapshot_bitcake;

import app.AppConfig;
import app.Cancellable;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.ChildrenInfoMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChildrenInfoCollector implements Runnable, Cancellable {
    private volatile boolean working = true;
    private AtomicBoolean collecting = new AtomicBoolean(false);
    private static final Object relatedLock = new Object();
    private static final Object unrelatedLock = new Object();
    private static final Object regionLock = new Object();
    /*
     * for for the spanning tree
     */

    private static List<Integer> relatedChildren = new CopyOnWriteArrayList<>();
    private static List<Integer> unrelatedChildren = new CopyOnWriteArrayList<>();

    private List<Integer> otherRegion = new CopyOnWriteArrayList<>();

    // (childId) -> children of the subtree
    private Map<Integer, List<Integer>> childTreeNodes = new ConcurrentHashMap<>();

    private Map<Integer, LYSnapshotResult> collectedLYValues;

    public ChildrenInfoCollector(Map<Integer, LYSnapshotResult> collectedLYValues) {
        this.collectedLYValues = collectedLYValues;
    }

    // TODO: see who needs to stop it (SimpleServentListener stops it for now)
    @Override
    public void stop() {
        working = false;
    }

    @Override
    public void run() {
        while (working) {
            /*
             * Not collecting yet - just sleep until we start actual work, or finish
             */
            while (collecting.get() == false) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (working == false) {
                    return;
                }
            }

            int related = relatedChildren.size();
            int unrelated = unrelatedChildren.size();

            // waiting for neighbours = related + unrelated
            while(related + unrelated != AppConfig.myServentInfo.getNeighbors().size()) {
                try {
                    related = relatedChildren.size();
                    unrelated = unrelatedChildren.size();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // wait for all children to give me their subtrees and other regions info
            while(childTreeNodes.size() < related) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            ////////////////////////////////////////////////////////////////
            List<Integer> wholeSubtree = new ArrayList<>();
            for(Integer child : relatedChildren)
                wholeSubtree.addAll(childTreeNodes.get(child));
            wholeSubtree.add(AppConfig.myServentInfo.getId());

            // now I send that info to my parent
            if(AppConfig.myServentInfo.getId() != AppConfig.master) {
                Message childInfoMessage = new ChildrenInfoMessage(
                        AppConfig.myServentInfo, AppConfig.getInfoById(AppConfig.parent), AppConfig.snapshotVersions, otherRegion, wholeSubtree);
                MessageUtil.sendMessage(childInfoMessage);
                AppConfig.timestampedStandardPrint("Related children: " + relatedChildren + (relatedChildren.isEmpty() ? "  -> I am LEAF" : ""));
                AppConfig.timestampedStandardPrint("Unrelated children: " + unrelatedChildren);
                AppConfig.timestampedStandardPrint("Other regions: " + otherRegion);
            } else {
                // I am initiator, so I print the info
                AppConfig.timestampedStandardPrint("I am the master, I don't have a parent.");
                AppConfig.timestampedStandardPrint("Related children: " + relatedChildren + "  -> I am ROOT");
                AppConfig.timestampedStandardPrint("Unrelated children: " + unrelatedChildren);
                AppConfig.timestampedStandardPrint("Other regions: " + otherRegion);
                AppConfig.timestampedStandardPrint("Region size: " + wholeSubtree.size());
                calculatingSnapshot(wholeSubtree);
            }


            ///////// reseting for next iteration /////////
            collectedLYValues.clear(); //reset for next invocation
            relatedChildren.clear();
            unrelatedChildren.clear();

            otherRegion.clear();
            childTreeNodes = new ConcurrentHashMap<>();

            AppConfig.master = -1;
            AppConfig.parent = -1;

            collecting.set(false);
        }

    }

    private void calculatingSnapshot(List<Integer> region){
    // 2 wait for responses or finish
        boolean waitingResults = true;
        while (waitingResults) {
            if (collectedLYValues.size() == region.size()) {
                waitingResults = false;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (working == false) {
                return;
            }
        }

//			// print
        int sum = 0;
        for (Map.Entry<Integer, LYSnapshotResult> nodeResult : collectedLYValues.entrySet()) {
            sum += nodeResult.getValue().getRecordedAmount();
            AppConfig.timestampedStandardPrint(
                    "Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount());
        }

        for(int i : region) {
            for (int j : region) {
                if (i != j) {
                    if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
                        AppConfig.getInfoById(j).getNeighbors().contains(i)) {

                        // transit = transit + give - get

                        // give
                        int ijAmount = collectedLYValues.get(i).getGiveHistory().get(j);

                        // get
                        int jiAmount = collectedLYValues.get(j).getGetHistory().get(i);

//                        if(ijAmount != 0 || jiAmount != 0){
//                            AppConfig.timestampedStandardPrint("---------------");
//                            AppConfig.timestampedStandardPrint("Servent " + i + " gave " + ijAmount + " bitcakes to " + j);
//                            AppConfig.timestampedStandardPrint("Servent " + j + " got  " + jiAmount + " bitcakes from " + i);
//
//                        }

                        String transitKey= i + "-" + j;
                        int transitAmount = 0;

                        transitAmount = AppConfig.transit.get(transitKey) + ijAmount - jiAmount;
                        AppConfig.transit.put(transitKey, transitAmount);

                        if (transitAmount != 0) {
                            String outputString = String.format(
                                    "Unreceived bitcake amount: %d from servent %d to servent %d",
                                    transitAmount, i, j);
                            AppConfig.timestampedStandardPrint(outputString);
                            sum += transitAmount;
                        }
                    }
                }
            }
        }

        // TODO: videti kako se tranzit menja, posto ga budu mozda koristili drugacije za sledeci snapshot (trebalo bi da bude oke)
        AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
        AppConfig.timestampedStandardPrint("==================================================================================");

    }

    public boolean isCollecting(){
        return collecting.get();
    }

    public void addRelatedChild(int childId) {
        synchronized (relatedLock) {
            // go through the list to see if we already have this child, and if we don't, add it
            if (!relatedChildren.contains(childId)) {
                relatedChildren.add(childId);
            }
        }
    }

    public void addUnrelatedChild(int childId) {
        synchronized (unrelatedLock) {
            // go through the list to see if we already have this child, and if we don't, add it
            if (!unrelatedChildren.contains(childId)) {
                unrelatedChildren.add(childId);
            }
        }
    }

    public void addOtherRegions(List<Integer> newRegions) {
        synchronized (regionLock) {
            for(Integer newRegion : newRegions){
                // check whether we already have that region or if it's not our region, and if we don't, then add it
                if (!otherRegion.contains(newRegion) && newRegion != AppConfig.master)
                    otherRegion.add(newRegion);
            }

        }
    }

    public void addSubtree(int childId, List<Integer> subtree) {
        childTreeNodes.put(childId, subtree);
    }

    public  void startCollecting(){
        boolean oldValue = this.collecting.getAndSet(true);

        if (oldValue == true) {
            AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
        }
    }
}
