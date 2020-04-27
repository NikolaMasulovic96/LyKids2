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
			
			//zapisujem u moju strukturu sta sam dao
			SnapshotIndicator si = getSpashotIndicator();
			if(si != null) {
				Map<Integer,SnapshotIndicator> initiatorsList = AppConfig.snapshotIndicators;
				for (Map.Entry<Integer,SnapshotIndicator> entry : initiatorsList.entrySet()) {
					int initiatorId = entry.getKey();
					int snapId = 0;
					if(si.getSnapshotId() != -1) {
						snapId = si.getSnapshotId();
					}
					ServentStatusInfo serventInfo = AppConfig.getSnapshotGlobalInfo(initiatorId, snapId, getOriginalSenderInfo().getId());
					AppConfig.timestampedErrorPrint("vratio info:"+getOriginalSenderInfo().getId()+" - " + serventInfo.toString());
					serventInfo.updateGive(getReceiverInfo().getId(), amount);
					AppConfig.timestampedErrorPrint("Zapisao sam da sam dao:"+getOriginalSenderInfo().getId()+" - " + getReceiverInfo().getId()+" ="+amount);

				}
				//
			}else {
				AppConfig.timestampedErrorPrint("Nisam uspeo da zapisem:"+getOriginalSenderInfo().getId()+" - " + getReceiverInfo().getId()+" ="+amount);
			}
			lyBitcakeManager.recordGiveTransaction(getReceiverInfo().getId(), amount);
		}
	}
}
