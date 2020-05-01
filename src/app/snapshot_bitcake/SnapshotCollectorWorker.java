package app.snapshot_bitcake;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import app.AppConfig;
import app.ServentStatusInfo;
import servent.message.SnapshotIndicator;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	
	private AtomicBoolean collecting = new AtomicBoolean(false);
	
	private Map<Integer, LYSnapshotResult> collectedLYValues = new ConcurrentHashMap<>();
	
	private BitcakeManager bitcakeManager;

	public SnapshotCollectorWorker() {
		bitcakeManager = new LaiYangBitcakeManager();
		for(Integer i : AppConfig.potentialInitiators) {
			SnapshotIndicator si = new SnapshotIndicator(i, 0);
			ServentStatusInfo snapStatus = new ServentStatusInfo(AppConfig.myServentInfo.getId());
			boolean has = false;
			for (Map.Entry<SnapshotIndicator,ServentStatusInfo> entry : AppConfig.snapshotIndicators.entrySet()) {
				if(entry.getKey().getInitiatorId() == si.getInitiatorId() && entry.getKey().getSnapshotId() == si.getSnapshotId()) {
					has = true;
				}
			}
			if(!has) {
				AppConfig.snapshotIndicators.putIfAbsent(si, snapStatus);
			}
			//AppConfig.timestampedErrorPrint("1-"+AppConfig.snapshotIndicators);
		}
	}
	
	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}
	
	@Override
	public void run() {
		while(working) {
			
			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (collecting.get() == false) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */
			
			//1 send asks
			AppConfig.timestampedStandardPrint("Zapocet info");
			((LaiYangBitcakeManager)bitcakeManager).markerEvent(AppConfig.myServentInfo.getId(), this,AppConfig.currentSnapshotIndicator);
			
			//2 wait for responses or finish
			boolean waiting = true;
			while (waiting) {
				if (AppConfig.currSnapshotResults.size() == AppConfig.getServentCount()) {
					waiting = false;
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			//print
			int sum;
			sum = 0;
			for (Entry<Integer, LYSnapshotResult> nodeResult : collectedLYValues.entrySet()) {
				sum += nodeResult.getValue().getRecordedAmount();
				AppConfig.timestampedStandardPrint("Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount());
			}
			for(int i = 0; i < AppConfig.getServentCount(); i++) {
				for (int j = 0; j < AppConfig.getServentCount(); j++) {
					if (i != j) {
						if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
							AppConfig.getInfoById(j).getNeighbors().contains(i)) {
							int ijAmount = collectedLYValues.get(i).getGiveHistory().get(j);
							int jiAmount = collectedLYValues.get(j).getGetHistory().get(i);
							
							if (ijAmount != jiAmount) {
								String outputString = String.format(
										"Unreceived bitcake amount: %d from servent %d to servent %d",
										ijAmount - jiAmount, i, j);
								AppConfig.timestampedStandardPrint(outputString);
								sum += ijAmount - jiAmount;
							}
						}
					}
				}
			}
			
			//moje razunanje
			int sum2;
			sum2 = 0;
//			Collections.sort(AppConfig.currSnapshotResults);
//			AppConfig.timestampedErrorPrint("LISY:" + AppConfig.currSnapshotResults);
			for (ServentStatusInfo infos : AppConfig.currSnapshotResults) {
				
				if(infos != null) {
					sum2 += infos.getCurrentAmount().get();
					AppConfig.timestampedErrorPrint("Recorded bitcake amount for " + infos.toString());
				}else {
					AppConfig.timestampedErrorPrint("greska");
				}
			}
			for(ServentStatusInfo info : AppConfig.currSnapshotResults) {
				List<Integer> neighbors = AppConfig.getInfoById(info.getServentId()).getNeighbors();
				for(int i = 0; i < neighbors.size(); i++) {
					//AppConfig.timestampedErrorPrint("MyINfo:"+ info);
					ServentStatusInfo neighborInfo = AppConfig.getResult(neighbors.get(i));
					//AppConfig.timestampedErrorPrint("MyNeig:"+neighborInfo);
					//AppConfig.timestampedErrorPrint("#" + info.getGiveHistory() + "-" + neighbors.get(i));
					//AppConfig.timestampedErrorPrint("%" + neighborInfo.getGetHistory() + "-" + info.getServentId());
					int amount1 = info.getGiveHistory().get(neighbors.get(i));
					int amount2 = neighborInfo.getGetHistory().get(info.getServentId());
					//AppConfig.timestampedErrorPrint("Razlika else"+amount1 +" " +amount2);
					if (amount1 != amount2) {
						String outputString = String.format(
								"Unreceived bitcake amount: %d from servent %d to servent %d",
								amount1 - amount2, info.getServentId(), neighborInfo.getServentId());
						AppConfig.timestampedErrorPrint(outputString);
						sum2 += amount1 - amount2;
					}else {
						//AppConfig.timestampedErrorPrint("Razlika else"+(amount1 - amount2));
					}
				}
			}
			
//			for(int i = 0; i < AppConfig.getServentCount(); i++) {
//				for (int j = 0; j < AppConfig.getServentCount(); j++) {
//					if (i != j) {
//						if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
//							AppConfig.getInfoById(j).getNeighbors().contains(i)) {
//							try {
//								int ijAmount = AppConfig.currSnapshotResults.get(i).getGiveHistory().get(j);
//								int jiAmount = AppConfig.currSnapshotResults.get(j).getGetHistory().get(i);
//								AppConfig.timestampedErrorPrint("Proveravam:"+i + "-"+j);
//								if (ijAmount != jiAmount) {
//									String outputString = String.format(
//											"Unreceived bitcake amount: %d from servent %d to servent %d",
//											ijAmount - jiAmount, i, j);
//									AppConfig.timestampedErrorPrint(outputString);
//									sum2 += ijAmount - jiAmount;
//								}
//							} catch (Exception e) {
//								// TODO: handle exception
//							}
//						}
//					}
//				}
//			}
			
			
			AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
			AppConfig.timestampedErrorPrint("System bitcake count2: " + sum2);
			
			collectedLYValues.clear(); //reset for next invocation
			collecting.set(false);
			//AppConfig.snapsotsInfo.clear();
			AppConfig.snapshotIndicators.clear();
			AppConfig.currSnapshotResults.clear();
		}

	}
	
	@Override
	public void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult) {
		collectedLYValues.put(id, lySnapshotResult);
	}
	
	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue == true) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}
	
	@Override
	public void stop() {
		working = false;
	}

}
