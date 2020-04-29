package servent.message;

import java.util.List;
import java.util.Map;

import app.AppConfig;
import app.ServentInfo;
import app.ServentStatusInfo;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.LaiYangBitcakeManager;

/**
 * Represents a bitcake transaction. We are sending some bitcakes to another node.
 * 
 * @author bmilojkovic
 *
 */
public class TransactionMessage extends BasicMessage {

	private static final long serialVersionUID = -333251402058492901L;

	private transient BitcakeManager bitcakeManager;
	
	public TransactionMessage(ServentInfo sender, ServentInfo receiver, int amount, BitcakeManager bitcakeManager,SnapshotIndicator snapshotIndicator) {
		super(MessageType.TRANSACTION, sender, receiver, String.valueOf(amount),snapshotIndicator);
		this.bitcakeManager = bitcakeManager;
	}
	
	
	/**
	 * We want to take away our amount exactly as we are sending, so our snapshots don't mess up.
	 * This method is invoked by the sender just before sending, and with a lock that guarantees
	 * that we are white when we are doing this in Chandy-Lamport.
	 */
	@Override
	public void sendEffect() {
		int amount = Integer.parseInt(getMessageText());
		
		bitcakeManager.takeSomeBitcakes(amount);
		if (bitcakeManager instanceof LaiYangBitcakeManager && isWhite()) {
			LaiYangBitcakeManager lyBitcakeManager = (LaiYangBitcakeManager)bitcakeManager;
			 synchronized (AppConfig.colorLock) {
			//zapisujem u moju strukturu sta sam dao
			SnapshotIndicator si = getSpashotIndicator();
			if(si != null) {
				//prvo izvuci za inicijatora
				if(si.getSnapshotId() == -1) {
					AppConfig.timestampedErrorPrint("Usao za prvi-give");
				for (Map.Entry<SnapshotIndicator,ServentStatusInfo> entry : AppConfig.snapshotIndicators.entrySet()) {
					ServentStatusInfo serventInfo = AppConfig.getSnapshotGlobalInfo(entry.getKey().getInitiatorId(),0);
					if(serventInfo != null) {
						serventInfo.updateGive(getReceiverInfo().getId(), amount);
					}else {
						AppConfig.timestampedErrorPrint("Nema servent info za:" + entry.getKey() + "::" + 0);
					}
				}
			}else {
				AppConfig.timestampedErrorPrint("Usao za drugi- give -" + AppConfig.snapshotIndicators);

				for (Map.Entry<SnapshotIndicator,ServentStatusInfo> entry : AppConfig.snapshotIndicators.entrySet()) {
					if(entry.getKey().getInitiatorId() != si.getInitiatorId()) {
						ServentStatusInfo serventInfo = AppConfig.getSnapshotGlobalInfo(entry.getKey().getInitiatorId(),0);
						//AppConfig.timestampedErrorPrint("Izvadio za:"+ entry.toString() + serventInfo);
						if(serventInfo != null) {
							serventInfo.updateGive(getReceiverInfo().getId(), amount);
						}else {
							AppConfig.timestampedErrorPrint("Nema servent info za:" + entry.getKey() + "::" + 0);
						}
					}else {
						SnapshotIndicator indicator = entry.getKey();
						if(indicator.getSnapshotId() == si.getSnapshotId()) {
							SnapshotIndicator newSnapshot = new SnapshotIndicator(si.getInitiatorId(), si.getSnapshotId() + 1);
							ServentStatusInfo newInfo = new ServentStatusInfo(AppConfig.myServentInfo.getId());
							newInfo.updateGive(getReceiverInfo().getId(), amount);
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
				AppConfig.timestampedErrorPrint("Nisam uspeo da zapisem:"+getOriginalSenderInfo().getId()+" - " + getReceiverInfo().getId()+" ="+amount);
			}
			lyBitcakeManager.recordGiveTransaction(getReceiverInfo().getId(), amount);
			 }
		}
	}
}
