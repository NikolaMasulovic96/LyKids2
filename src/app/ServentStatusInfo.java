package app;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class ServentStatusInfo implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3191281875535676471L;
	
	private int serventId;
	private AtomicInteger currentAmount = new AtomicInteger(0);
	private Map<Integer, Integer> giveHistory = new ConcurrentHashMap<>();
	private Map<Integer, Integer> getHistory = new ConcurrentHashMap<>();
	
	public ServentStatusInfo(int serventId) {
		// TODO Auto-generated constructor stub
		this.serventId = serventId;
		ServentInfo serInfo = AppConfig.getInfoById(serventId);
		for(Integer neighbor : serInfo.getNeighbors()) {
			giveHistory.put(neighbor, 0);
			getHistory.put(neighbor, 0);
		}
	}
	
	public void updateGive(int neighbor, int amount) {
		giveHistory.compute(neighbor, new MapValueUpdater(amount));
	}
	
	public void updateGet(int neighbor, int amount) {
		getHistory.compute(neighbor, new MapValueUpdater(amount));
	}

	public int getServentId() {
		return serventId;
	}

	public void setServentId(int serventId) {
		this.serventId = serventId;
	}

	public AtomicInteger getCurrentAmount() {
		return currentAmount;
	}

	public void setCurrentAmount(AtomicInteger currentAmount) {
		this.currentAmount = currentAmount;
	}

	public Map<Integer, Integer> getGiveHistory() {
		return giveHistory;
	}

	public void setGiveHistory(Map<Integer, Integer> giveHistory) {
		this.giveHistory = giveHistory;
	}

	public Map<Integer, Integer> getGetHistory() {
		return getHistory;
	}

	public void setGetHistory(Map<Integer, Integer> getHistory) {
		this.getHistory = getHistory;
	}

	@Override
	public String toString() {
		return "ServentStatusInfo [serventId=" + serventId + ", currentAmount=" + currentAmount + ", giveHistory="
				+ giveHistory + ", getHistory=" + getHistory + "]";
	}
}
