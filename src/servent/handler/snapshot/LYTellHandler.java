package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.LYTellMessage;

public class LYTellHandler implements MessageHandler {

	private Message clientMessage;
	private SnapshotCollector snapshotCollector;
	
	public LYTellHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
		this.clientMessage = clientMessage;
		this.snapshotCollector = snapshotCollector;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.LY_TELL) {
			LYTellMessage lyTellMessage = (LYTellMessage)clientMessage;
			
			snapshotCollector.addLYSnapshotInfo(
					lyTellMessage.getOriginalSenderInfo().getId(),
					lyTellMessage.getLYSnapshotResult());
			try {
				LYTellMessage m = (LYTellMessage)clientMessage;
				AppConfig.timestampedErrorPrint("GOT SNAP INFO:"+ m.getSnapInfo());
				AppConfig.currSnapshotResults.add(m.getSnapInfo());
			} catch (Exception e) {
				AppConfig.timestampedErrorPrint("NE MEREM KASTOVAT OVO");
			}
			
		} else {
			AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
		}

	}

}
