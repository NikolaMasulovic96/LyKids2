package servent.handler;

import java.util.Map;

import app.AppConfig;
import app.ServentStatusInfo;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.SnapshotIndicator;

public class TransactionHandler implements MessageHandler {

	private Message clientMessage;
	private BitcakeManager bitcakeManager;
	
	public TransactionHandler(Message clientMessage, BitcakeManager bitcakeManager) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = bitcakeManager;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
			String amountString = clientMessage.getMessageText();
			
			int amountNumber = 0;
			try {
				amountNumber = Integer.parseInt(amountString);
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
				return;
			}
			
			bitcakeManager.addSomeBitcakes(amountNumber);
			synchronized (AppConfig.colorLock) {
				if (bitcakeManager instanceof LaiYangBitcakeManager && clientMessage.isWhite()) {
					LaiYangBitcakeManager lyFinancialManager = (LaiYangBitcakeManager)bitcakeManager;
					
					//zapisujem u moju strukturu sta sam primio
					SnapshotIndicator si = clientMessage.getSnapshotIndicator();
					if(si != null) {
						Map<Integer,SnapshotIndicator> initiatorsList = AppConfig.snapshotIndicators;
						for (Map.Entry<Integer,SnapshotIndicator> entry : initiatorsList.entrySet()) {
							int initiatorId = entry.getKey();
							int snapId = 0;
							if(si.getSnapshotId() != -1) {
								snapId = si.getSnapshotId();
							}
							ServentStatusInfo serventInfo = AppConfig.getSnapshotGlobalInfo(initiatorId, snapId, clientMessage.getReceiverInfo().getId());
							AppConfig.timestampedErrorPrint("vratio info:"+ clientMessage.getReceiverInfo().getId()+" - " + serventInfo.toString());
							serventInfo.updateGet(clientMessage.getOriginalSenderInfo().getId(), amountNumber);
							AppConfig.timestampedErrorPrint("Zapisao sam da sam primio:"+clientMessage.getOriginalSenderInfo().getId()+" - " + clientMessage.getReceiverInfo().getId()+" ="+amountNumber+" /// msgID:"+clientMessage.getMessageId());
						}
						//
					}else {
						AppConfig.timestampedErrorPrint("Nisam uspeo da zapisem:"+clientMessage.getOriginalSenderInfo().getId()+" - " + clientMessage.getReceiverInfo().getId()+" ="+amountNumber );
					}
					
					lyFinancialManager.recordGetTransaction(clientMessage.getOriginalSenderInfo().getId(), amountNumber);
				}
			}
		} else {
			AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
		}
	}

}
