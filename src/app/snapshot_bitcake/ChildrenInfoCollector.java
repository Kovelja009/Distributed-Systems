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

    // (childId) -> number of children in the subtree
    private Map<Integer, Integer> childTreeNodes = new ConcurrentHashMap<>();


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

            // wait for all children to give me their subtree size and other regions info
            while(childTreeNodes.size() < related) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // now I send that info to my parent
            if(AppConfig.myServentInfo.getId() != AppConfig.master) {
                int subtreeSize = 0;
                for (Integer child : relatedChildren)
                    subtreeSize += childTreeNodes.get(child);

                subtreeSize++;

                Message childInfoMessage = new ChildrenInfoMessage(
                        AppConfig.myServentInfo, AppConfig.getInfoById(AppConfig.parent), AppConfig.snapshotVersions, otherRegion, subtreeSize);
                MessageUtil.sendMessage(childInfoMessage);
            } else {
                // I am initiator, so I print the info
                AppConfig.timestampedStandardPrint("I am the master, I don't have a parent.");
                AppConfig.timestampedStandardPrint("Related children: " + relatedChildren);
                AppConfig.timestampedStandardPrint("Unrelated children: " + unrelatedChildren);
                AppConfig.timestampedStandardPrint("Other regions: " + otherRegion);
                int subtreeSize = 0;
                for(Integer child : relatedChildren)
                    subtreeSize += childTreeNodes.get(child);

                subtreeSize++;
                AppConfig.timestampedStandardPrint("Subtree sizes: " + subtreeSize);
            }

            collecting.set(false);
        }

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

    public void addSubtreeSize(int childId, int subtreeSize) {
        childTreeNodes.put(childId, subtreeSize);
    }

    public  void startCollecting(){
        boolean oldValue = this.collecting.getAndSet(true);

        if (oldValue == true) {
            AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
        }
    }
}
