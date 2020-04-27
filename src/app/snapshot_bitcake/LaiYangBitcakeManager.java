package app.snapshot_bitcake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import app.AppConfig;
import app.MapValueUpdater;
import app.ServentStatusInfo;
import servent.message.Message;
import servent.message.SnapshotIndicator;
import servent.message.snapshot.LYMarkerMessage;
import servent.message.snapshot.LYTellMessage;
import servent.message.util.MessageUtil;

public class LaiYangBitcakeManager implements BitcakeManager {

	private final AtomicInteger currentAmount = new AtomicInteger(1000);
	
	public void takeSomeBitcakes(int amount) {
		currentAmount.getAndAdd(-amount);
	}
	
	public void addSomeBitcakes(int amount) {
		currentAmount.getAndAdd(amount);
	}
	
	public int getCurrentBitcakeAmount() {
		return currentAmount.get();
	}
	
	private Map<Integer, Integer> giveHistory = new ConcurrentHashMap<>();
	private Map<Integer, Integer> getHistory = new ConcurrentHashMap<>();
	
	public LaiYangBitcakeManager() {
		for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			giveHistory.put(neighbor, 0);
			getHistory.put(neighbor, 0);
		}
	}
	
	/*
	 * This value is protected by AppConfig.colorLock.
	 * Access it only if you have the blessing.
	 */
	public int recordedAmount = 0;
	
	public void markerEvent(int collectorId, SnapshotCollector snapshotCollector,SnapshotIndicator snapshotIndicator) {
		synchronized (AppConfig.colorLock) {
			AppConfig.isWhite.set(false);
			recordedAmount = getCurrentBitcakeAmount();
			SnapshotIndicator si = new SnapshotIndicator(collectorId, 0);

			if(snapshotCollector == null) {
				Integer snapId = 0;
				for (Map.Entry<Integer,SnapshotIndicator> entry : AppConfig.snapshotIndicators.entrySet()) {
					if(entry.getKey() == collectorId) {
						snapId = entry.getValue().getSnapshotId();
					}
				}
				si.setSnapshotId(snapId);
			}

			LYSnapshotResult snapshotResult = new LYSnapshotResult(
					AppConfig.myServentInfo.getId(), recordedAmount, giveHistory, getHistory);
			
			if (collectorId == AppConfig.myServentInfo.getId()) {
				snapshotCollector.addLYSnapshotInfo(
						AppConfig.myServentInfo.getId(),
						snapshotResult);
				AppConfig.currSnapshotResults.add(AppConfig.getSnapshotGlobalInfo(collectorId, si.getSnapshotId(), collectorId));
			} else {
				AppConfig.currentSnapshotInitiator = collectorId;
				AppConfig.currentSnapshotId.set(si.getSnapshotId());
				ServentStatusInfo snapInfo = AppConfig.getSnapshotGlobalInfo(collectorId, si.getSnapshotId(), AppConfig.myServentInfo.getId());
				Message tellMessage = new LYTellMessage(AppConfig.myServentInfo, AppConfig.getInfoById(collectorId), snapshotResult,si,snapInfo);
				
				MessageUtil.sendMessage(tellMessage);
			}
			
			for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
				Message clMarker = new LYMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId,si);
				MessageUtil.sendMessage(clMarker);
				try {
					/*
					 * This sleep is here to artificially produce some white node -> red node messages.
					 * Not actually recommended, as we are sleeping while we have colorLock.
					 */
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
//	public class MapValueUpdater implements BiFunction<Integer, Integer, Integer> {
//		
//		private int valueToAdd;
//		
//		public MapValueUpdater(int valueToAdd) {
//			this.valueToAdd = valueToAdd;
//		}
//		
//		@Override
//		public Integer apply(Integer key, Integer oldValue) {
//			return oldValue + valueToAdd;
//		}
//	}
	
	public void recordGiveTransaction(int neighbor, int amount) {
		AppConfig.timestampedErrorPrint("----Zapisao sam da sam dao:"+AppConfig.myServentInfo.getId()+" - " + neighbor+" ="+amount);
		AppConfig.timestampedErrorPrint("*********************************************************************************************");
		giveHistory.compute(neighbor, new MapValueUpdater(amount));
	}
	
	public void recordGetTransaction(int neighbor, int amount) {
		getHistory.compute(neighbor, new MapValueUpdater(amount));
	}
}
