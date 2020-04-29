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
						//prvo izvuci za inicijatora
						if(si.getSnapshotId() == -1) {
							//AppConfig.timestampedErrorPrint("Usao za prvi-get");
							
								for (Map.Entry<SnapshotIndicator,ServentStatusInfo> entry : AppConfig.snapshotIndicators.entrySet()) {
									//AppConfig.timestampedErrorPrint("Usao za:"+entry.getKey().getInitiatorId()+0);
									ServentStatusInfo serventInfo = AppConfig.getSnapshotGlobalInfo(entry.getKey().getInitiatorId(),0);
									if(serventInfo != null) { 
										serventInfo.updateGet(clientMessage.getOriginalSenderInfo().getId(), amountNumber);
									}else {
										//AppConfig.timestampedErrorPrint("Nema servent info za:" + entry.getKey() + "::" + 0);
										SnapshotIndicator newSnapshot = new SnapshotIndicator(si.getInitiatorId(), 0);
										ServentStatusInfo newInfo = new ServentStatusInfo(AppConfig.myServentInfo.getId());
										newInfo.updateGet(clientMessage.getOriginalSenderInfo().getId(), amountNumber);
										AppConfig.snapshotIndicators.putIfAbsent(newSnapshot, newInfo);
									}
								}
							
					}else {
						//AppConfig.timestampedErrorPrint("Usao za drugi-get");
						for (Map.Entry<SnapshotIndicator,ServentStatusInfo> entry : AppConfig.snapshotIndicators.entrySet()) {
							//AppConfig.timestampedErrorPrint("" + si.getInitiatorId() + si.getSnapshotId());
							if(entry.getKey().getInitiatorId() != si.getInitiatorId()) {
								ServentStatusInfo serventInfo = AppConfig.getSnapshotGlobalInfo(entry.getKey().getInitiatorId(),0);
								if(serventInfo != null) {
									serventInfo.updateGet(clientMessage.getOriginalSenderInfo().getId(), amountNumber);
								}else {
									//AppConfig.timestampedErrorPrint("Nema servent info za:" + entry.getKey() + "::" + 0);
								}
							}else {
								//AppConfig.timestampedErrorPrint("Usao za treci");
								SnapshotIndicator indicator = entry.getKey();
								if(indicator.getSnapshotId() == si.getSnapshotId()) {
									//AppConfig.timestampedErrorPrint("Usao za cetvrti");
									SnapshotIndicator newSnapshot = new SnapshotIndicator(si.getInitiatorId(), si.getSnapshotId() + 1);
									ServentStatusInfo newInfo = new ServentStatusInfo(AppConfig.myServentInfo.getId());
									newInfo.updateGet(clientMessage.getOriginalSenderInfo().getId(), amountNumber);
									//AppConfig.timestampedErrorPrint("pokusao da napravim novi:" + entry.getKey() + "::" + 0);
									boolean has = false;
									for (Map.Entry<SnapshotIndicator,ServentStatusInfo> entry1 : AppConfig.snapshotIndicators.entrySet()) {
										if(entry1.getKey().getInitiatorId() == si.getInitiatorId() && entry1.getKey().getSnapshotId() == si.getSnapshotId()+1) {
											has = true;
										}
									}
									if(!has) {
										AppConfig.snapshotIndicators.putIfAbsent(newSnapshot, newInfo);
									}
									AppConfig.timestampedErrorPrint("da li je uspeo:" + AppConfig.snapshotIndicators);
								}
							}
						}
					}
					}else {
						
						//AppConfig.timestampedErrorPrint("da li je uspeo:" + AppConfig.snapshotIndicators);
						//AppConfig.timestampedErrorPrint("Nisam uspeo da zapisem:"+clientMessage.getOriginalSenderInfo().getId()+" - " + clientMessage.getReceiverInfo().getId()+" ="+amountNumber );
					}
					
					lyFinancialManager.recordGetTransaction(clientMessage.getOriginalSenderInfo().getId(), amountNumber);
				}
			}
		} else {
			AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
		}
	}

}
