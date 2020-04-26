package app;

import java.util.List;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class SnapshotGlobalInfo implements Serializable{

	private static final long serialVersionUID = -8317745351646780040L;
	private AtomicInteger snapshotId = new AtomicInteger(-1);
	private List<ServentStatusInfo> serventStatusInfos = Collections.synchronizedList(new ArrayList<>());
	
	public SnapshotGlobalInfo(Integer snapshotId) {
		this.snapshotId.set(snapshotId);
		for (int i =0; i<AppConfig.getServentCount() ;i++) {
			ServentStatusInfo statusInfo = new ServentStatusInfo(i);
			this.serventStatusInfos.add(statusInfo);
		}
	}

	public AtomicInteger getSnapshotId() {
		return snapshotId;
	}

	public void setSnapshotId(AtomicInteger snapshotId) {
		this.snapshotId = snapshotId;
	}

	public List<ServentStatusInfo> getServentStatusInfos() {
		return serventStatusInfos;
	}

	public void setServentStatusInfos(List<ServentStatusInfo> serventStatusInfos) {
		this.serventStatusInfos = serventStatusInfos;
	}

	@Override
	public String toString() {
		return "SnapshotGlobalInfo [snapshotId=" + snapshotId + ", serventStatusInfos=" + serventStatusInfos + "]";
	}
}
