package app;

import app.snapshot_bitcake.ChildrenInfoCollector;
import app.snapshot_bitcake.LYSnapshotResult;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import cli.CLIParser;
import servent.SimpleServentListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Describes the procedure for starting a single Servent
 *
 * @author bmilojkovic
 */
public class ServentMain {

	/**
	 * Command line arguments are:
	 * 0 - path to servent list file
	 * 1 - this servent's id
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			AppConfig.timestampedErrorPrint("Please provide servent list file and id of this servent.");
		}
		
		int serventId = -1;
		int portNumber = -1;
		
		String serventListFile = args[0];
		
		AppConfig.readConfig(serventListFile);
		
		try {
			serventId = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Second argument should be an int. Exiting...");
			System.exit(0);
		}
		
		if (serventId >= AppConfig.getServentCount()) {
			AppConfig.timestampedErrorPrint("Invalid servent id provided");
			System.exit(0);
		}
		
		AppConfig.myServentInfo = AppConfig.getInfoById(serventId);
		
		try {
			portNumber = AppConfig.myServentInfo.getListenerPort();
			
			if (portNumber < 1000 || portNumber > 2000) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Port number should be in range 1000-2000. Exiting...");
			System.exit(0);
		}
		
		AppConfig.timestampedStandardPrint("Starting servent " + AppConfig.myServentInfo);

		Map<Integer, LYSnapshotResult> collectedLYValues = new ConcurrentHashMap<>();

		// for Spezialetti-Kearns
		ChildrenInfoCollector childrenInfoCollector = new ChildrenInfoCollector(collectedLYValues);
		Thread childrenInfoCollectorThread = new Thread(childrenInfoCollector);
		childrenInfoCollectorThread.start();

		SnapshotCollector snapshotCollector;
		snapshotCollector = new SnapshotCollectorWorker(childrenInfoCollector, collectedLYValues);
		Thread snapshotCollectorThread = new Thread(snapshotCollector);
		snapshotCollectorThread.start();


		SimpleServentListener simpleListener = new SimpleServentListener(snapshotCollector);
		Thread listenerThread = new Thread(simpleListener);
		listenerThread.start();
		
		CLIParser cliParser = new CLIParser(simpleListener, snapshotCollector);
		Thread cliThread = new Thread(cliParser);
		cliThread.start();
		
	}
}
