package servent.message;

import java.io.Serializable;

public class SnapshotIndicator implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -9160977362084426283L;
	private  Integer initiatorId;
	private  Integer snapshotId;
	
	public SnapshotIndicator(Integer initiatorId, Integer snapshotId) {
		// TODO Auto-generated constructor stub
		this.initiatorId = initiatorId;
		this.snapshotId = snapshotId;
	}

	public Integer getInitiatorId() {
		return initiatorId;
	}

	public void setInitiatorId(Integer initiatorId) {
		this.initiatorId = initiatorId;
	}

	public Integer getSnapshotId() {
		return snapshotId;
	}

	public void setSnapshotId(Integer snapshotId) {
		this.snapshotId = snapshotId;
	}

	@Override
	public String toString() {
		return "SnapshotIndicator <initiatorId:" + initiatorId + ", snap:" + snapshotId + ">";
	}
	
	
	
	
}
