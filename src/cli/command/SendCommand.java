package cli.command;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import servent.message.Message;
import servent.message.TransactionMessage;
import servent.message.util.MessageUtil;

public class SendCommand implements CLICommand{

    private BitcakeManager bitcakeManager;

    public SendCommand(BitcakeManager bitcakeManager) {
        this.bitcakeManager = bitcakeManager;
    }

    @Override
    public String commandName() {
        return "send";
    }


    // First argument is the receipient, second is the amount
    @Override
    public void execute(String args) {
        String[] argsArray = args.split(" ");
        if (argsArray.length != 2) {
            AppConfig.timestampedErrorPrint("Send command requires two arguments: receipient and amount");
            return;
        }

        int receipient = Integer.parseInt(argsArray[0]);
        int amount = Integer.parseInt(argsArray[1]);

        // check whether receipient is our neighbor
        if (!AppConfig.myServentInfo.getNeighbors().contains(receipient)) {
            AppConfig.timestampedErrorPrint("Receipient is not our neighbor for send command");
            return;
        }

        ServentInfo neighborInfo = AppConfig.getInfoById(receipient);

        Message transactionMessage = new TransactionMessage(AppConfig.myServentInfo, neighborInfo, amount, bitcakeManager);
        MessageUtil.sendMessage(transactionMessage);
    }
}
