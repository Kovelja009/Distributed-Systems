package cli.command;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;

public class BitcakeInfoCommand implements CLICommand {

	private SnapshotCollector collector;
	
	public BitcakeInfoCommand(SnapshotCollector collector) {
		this.collector = collector;
	}
	
	@Override
	public String commandName() {
		return "bitcake_info";
	}

	@Override
	public void execute(String args) {
		// check whether the node executing the command is an initiator
		if (!AppConfig.getInitiators().contains(AppConfig.myServentInfo.getId())) {
			AppConfig.timestampedErrorPrint("You are not an initiator. You cannot start a transaction burst.");
			return;
		}
		collector.startCollecting();

	}

}
