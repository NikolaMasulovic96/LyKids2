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

			LYSnapshotResult snapshotResult = new LYSnapshotResult(
					AppConfig.myServentInfo.getId(), recordedAmount, giveHistory, getHistory);
			//ja sam kolektor za taj snapshot, sam sebi zappisem rezultat
			if (collectorId == AppConfig.myServentInfo.getId()) {
				snapshotCollector.addLYSnapshotInfo(AppConfig.myServentInfo.getId(),snapshotResult);
				if(snapshotIndicator.getSnapshotId() == -1) {
					AppConfig.currentSnapshotIndicator.setSnapshotId(0);
				}else {
					AppConfig.currentSnapshotIndicator.setSnapshotId(snapshotIndicator.getSnapshotId());
				}
				
				ServentStatusInfo s = AppConfig.getSnapshotGlobalInfo(collectorId, AppConfig.currentSnapshotIndicator.getSnapshotId());
				ServentStatusInfo newS = new  ServentStatusInfo(s);
				AppConfig.currentSnapshotIndicator.setInitiatorId(collectorId);
				AppConfig.currSnapshotResults.add(newS);
			} else {
				ServentStatusInfo snapInfo = new ServentStatusInfo(AppConfig.getSnapshotGlobalInfo(collectorId, snapshotIndicator.getSnapshotId()));
				Message tellMessage = new LYTellMessage(AppConfig.myServentInfo, AppConfig.getInfoById(collectorId), snapshotResult,snapshotIndicator,snapInfo);
				AppConfig.timestampedErrorPrint("Poslao ovaj info "+tellMessage.getMessageId()+":" + snapInfo.toString());
				AppConfig.currentSnapshotIndicator.setInitiatorId(snapshotIndicator.getInitiatorId());
				AppConfig.currentSnapshotIndicator.setSnapshotId(snapshotIndicator.getSnapshotId());
				MessageUtil.sendMessage(tellMessage);
			}
			
			for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
				Message clMarker = new LYMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId,snapshotIndicator);
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
	
	public void recordGiveTransaction(int neighbor, int amount) {
		giveHistory.compute(neighbor, new MapValueUpdater(amount));
	}
	
	public void recordGetTransaction(int neighbor, int amount) {
		getHistory.compute(neighbor, new MapValueUpdater(amount));
	}
}
