package app;

import java.util.function.BiFunction;

public class MapValueUpdater implements BiFunction<Integer, Integer, Integer>{
	private int valueToAdd;
	
	public MapValueUpdater(int valueToAdd) {
		this.valueToAdd = valueToAdd;
	}
	
	@Override
	public Integer apply(Integer key, Integer oldValue) {
		return oldValue + valueToAdd;
	}
}
