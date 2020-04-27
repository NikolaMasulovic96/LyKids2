package app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import servent.message.SnapshotIndicator;

/**
 * This class contains all the global application configuration stuff.
 * @author bmilojkovic
 *
 */
public class AppConfig {


	public static ServentInfo myServentInfo;	
	private static List<ServentInfo> serventInfoList = new ArrayList<>();
	public static boolean IS_CLIQUE;
	
	//novo -> cuvanje snap istorije
	public static Map<Integer,SnapshotIndicator> snapshotIndicators = Collections.synchronizedMap(new HashMap<Integer,SnapshotIndicator>());
	
	public static Map<Integer, SnapshotGlobalInfo> globalSnapshotsInfos = Collections.synchronizedMap(new HashMap<Integer,SnapshotGlobalInfo>());
	public static List<ServentStatusInfo> currSnapshotResults = Collections.synchronizedList(new ArrayList<>());
	
	public static AtomicInteger currentSnapshotId = new AtomicInteger(-1);
	public static Integer currentSnapshotInitiator = -1;
	
	public static AtomicBoolean isWhite = new AtomicBoolean(true);
	public static Object colorLock = new Object();
	

	public static void timestampedStandardPrint(String message) {
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Date now = new Date();
		
		System.out.println(timeFormat.format(now) + " - " + message);
	}
	
	public static void timestampedErrorPrint(String message) {
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Date now = new Date();
		
		System.err.println(timeFormat.format(now) + " - " + message);
	}
	
	/**
	 * Reads a config file. Should be called once at start of app.
	 * The config file should be of the following format:
	 * <br/>
	 * <code><br/>
	 * servent_count=3 			- number of servents in the system <br/>
	 * clique=false 			- is it a clique or not <br/>
	 * fifo=false				- should sending be fifo
	 * servent0.port=1100 		- listener ports for each servent <br/>
	 * servent1.port=1200 <br/>
	 * servent2.port=1300 <br/>
	 * servent0.neighbors=1,2 	- if not a clique, who are the neighbors <br/>
	 * servent1.neighbors=0 <br/>
	 * servent2.neighbors=0 <br/>
	 * 
	 * </code>
	 * <br/>
	 * So in this case, we would have three servents, listening on ports:
	 * 1100, 1200, and 1300. This is not a clique, and:<br/>
	 * servent 0 sees servent 1 and 2<br/>
	 * servent 1 sees servent 0<br/>
	 * servent 2 sees servent 0<br/>
	 * 
	 * @param configName name of configuration file
	 */
	public static void readConfig(String configName){
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(new File(configName)));
			
		} catch (IOException e) {
			timestampedErrorPrint("Couldn't open properties file. Exiting...");
			System.exit(0);
		}
		
		int serventCount = -1;
		try {
			serventCount = Integer.parseInt(properties.getProperty("servent_count"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading servent_count. Exiting...");
			System.exit(0);
		}
		
		IS_CLIQUE = Boolean.parseBoolean(properties.getProperty("clique", "false"));
		String snapshotType = properties.getProperty("snapshot");
		if (snapshotType == null) {
			snapshotType = "none";
		}
				
		for (int i = 0; i < serventCount; i++) {
			String portProperty = "servent"+i+".port";
			
			int serventPort = -1;
			
			try {
				serventPort = Integer.parseInt(properties.getProperty(portProperty));
			} catch (NumberFormatException e) {
				timestampedErrorPrint("Problem reading " + portProperty + ". Exiting...");
				System.exit(0);
			}
			
			//************************* trazim ko su mi inicijatori
			String initiators = properties.getProperty("initiators");
			if(initiators == null) {
				timestampedErrorPrint("Initiators are not provided!");
			}else {
				String[] initiatorsIds = initiators.split(",");
				for (String ind : initiatorsIds) {
					SnapshotIndicator snapshopIndicator = new SnapshotIndicator(Integer.parseInt(ind),0);
					snapshotIndicators.putIfAbsent(Integer.parseInt(ind), snapshopIndicator);
				}
			}
			
			
			
			List<Integer> neighborList = new ArrayList<>();
			if (IS_CLIQUE) {
				for(int j = 0; j < serventCount; j++) {
					if (j == i) {
						continue;
					}
					
					neighborList.add(j);
				}
			} else {
				String neighborListProp = properties.getProperty("servent"+i+".neighbors");
				
				if (neighborListProp == null) {
					timestampedErrorPrint("Warning: graph is not clique, and node " + i + " doesnt have neighbors");
				} else {
					String[] neighborListArr = neighborListProp.split(",");
					
					try {
						for (String neighbor : neighborListArr) {
							neighborList.add(Integer.parseInt(neighbor));
						}
					} catch (NumberFormatException e) {
						timestampedErrorPrint("Bad neighbor list for node " + i + ": " + neighborListProp);
					}
				}
			}
			
			ServentInfo newInfo = new ServentInfo("localhost", i, serventPort, neighborList);
			serventInfoList.add(newInfo);
			//************************** svakom inicijatoru hocu da napravim globalSnaphots info 
			//da svaki inicijator za snapshot ima pojam o svakom cvoru kakvo je stanje
			if(serventInfoList.size() == serventCount) {
				for (Map.Entry<Integer,SnapshotIndicator> entry : AppConfig.snapshotIndicators.entrySet()) {
					SnapshotGlobalInfo globalInfo = new SnapshotGlobalInfo(entry.getValue().getSnapshotId());
					globalSnapshotsInfos.putIfAbsent(entry.getKey(), globalInfo);
				}
				AppConfig.timestampedErrorPrint("222222222+++"+globalSnapshotsInfos.toString());
			}
		}
	}
	
	/**
	 * Get info for a servent selected by a given id.
	 * @param id id of servent to get info for
	 * @return {@link ServentInfo} object for this id
	 */
	public static ServentInfo getInfoById(int id) {
		if (id >= getServentCount()) {
			throw new IllegalArgumentException(
					"Trying to get info for servent " + id + " when there are " + getServentCount() + " servents.");
		}
		return serventInfoList.get(id);
	}
	
	/**
	 * Get number of servents in this system.
	 */
	public static int getServentCount() {
		return serventInfoList.size();
	}
	public static List<ServentInfo> getServenInfoList(){
		return serventInfoList;
	}
	public static ServentStatusInfo getSnapshotGlobalInfo(Integer initiatorId, int snapshot, int serventId) {
		for (Map.Entry<Integer,SnapshotGlobalInfo> entry : AppConfig.globalSnapshotsInfos.entrySet()) {
			if(entry.getKey() == initiatorId) {
				SnapshotGlobalInfo info = entry.getValue();
				if(info.getSnapshotId().get() == snapshot) {
					for (ServentStatusInfo serventStatusInfo : info.getServentStatusInfos()) {
						if(serventStatusInfo.getServentId() == serventId) {
							return serventStatusInfo;
						}
					}
				}
			}
		}
		return null;
	}	
}
