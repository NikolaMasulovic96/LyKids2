package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;
import servent.message.SnapshotIndicator;

public class LYMarkerMessage extends BasicMessage {

	private static final long serialVersionUID = 388942509576636228L;
	static SnapshotIndicator si = new SnapshotIndicator(-1, -1);
	public LYMarkerMessage(ServentInfo sender, ServentInfo receiver, int collectorId) {
		
		super(MessageType.LY_MARKER, sender, receiver, String.valueOf(collectorId),si);
	}
}
