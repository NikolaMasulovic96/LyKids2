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
			((LaiYangBitcakeManager)bitcakeManager).markerEvent(AppConfig.myServentInfo.getId(), this,null);
			
			//2 wait for responses or finish
			boolean waiting = true;
			while (waiting) {
				if (collectedLYValues.size() == AppConfig.getServentCount()) {
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
			Collections.sort(AppConfig.currSnapshotResults);
			AppConfig.timestampedErrorPrint("LISY:" + AppConfig.currSnapshotResults);
			for (ServentStatusInfo infos : AppConfig.currSnapshotResults) {
				sum2 += infos.getCurrentAmount().get();
				AppConfig.timestampedErrorPrint("Recorded bitcake amount for " + infos.getServentId() + " = " + infos.getCurrentAmount().get());
			}
			
			for(int i = 0; i < AppConfig.getServentCount(); i++) {
				for (int j = 0; j < AppConfig.getServentCount(); j++) {
					if (i != j) {
//						if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
//							AppConfig.getInfoById(j).getNeighbors().contains(i)) {
							try {
								int ijAmount = AppConfig.currSnapshotResults.get(i).getGiveHistory().get(j);
								int jiAmount = AppConfig.currSnapshotResults.get(j).getGetHistory().get(i);
								if (ijAmount != jiAmount) {
									String outputString = String.format(
											"Unreceived bitcake amount: %d from servent %d to servent %d",
											ijAmount - jiAmount, i, j);
									AppConfig.timestampedErrorPrint(outputString);
									sum2 += ijAmount - jiAmount;
								}
							} catch (Exception e) {
								// TODO: handle exception
							}
							
						//}
					}
				}
			}
			
			
			AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
			AppConfig.timestampedErrorPrint("System bitcake count2: " + sum2);
			
			collectedLYValues.clear(); //reset for next invocation
			collecting.set(false);
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
