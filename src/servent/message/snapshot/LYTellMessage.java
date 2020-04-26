package servent.message.snapshot;

import java.util.List;

import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.SnapshotIndicator;

public class LYTellMessage extends BasicMessage {

	private static final long serialVersionUID = 3116394054726162318L;

	private LYSnapshotResult lySnapshotResult;
	
	private SnapshotIndicator si;
	
	public LYTellMessage(ServentInfo sender, ServentInfo receiver, LYSnapshotResult lySnapshotResult,SnapshotIndicator si) {
		super(MessageType.LY_TELL, sender, receiver,si);
		
		this.lySnapshotResult = lySnapshotResult;
		this.si =si;
	}
	
	private LYTellMessage(MessageType messageType, ServentInfo sender, ServentInfo receiver, 
			boolean white, List<ServentInfo> routeList, String messageText, int messageId,
			LYSnapshotResult lySnapshotResult, SnapshotIndicator si) {
		super(messageType, sender, receiver, white, routeList, messageText, messageId,si);
		this.lySnapshotResult = lySnapshotResult;
		this.si = si;
	}

	public LYSnapshotResult getLYSnapshotResult() {
		return lySnapshotResult;
	}
	
	public SnapshotIndicator getSi() {
		return si;
	}

	@Override
	public Message setRedColor() {
		Message toReturn = new LYTellMessage(getMessageType(), getOriginalSenderInfo(), getReceiverInfo(),
				false, getRoute(), getMessageText(), getMessageId(), getLYSnapshotResult(),getSi());
		return toReturn;
	}
}
