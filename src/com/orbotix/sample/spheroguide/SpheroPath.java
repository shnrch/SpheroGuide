package com.orbotix.sample.spheroguide;

import java.util.Collection;
import java.util.LinkedList;

public class SpheroPath<K extends Number> implements PathProvider<K> {
	
	private int curId = 0;
	private LinkedList<PositionAbsolute<K>> waypoints = 
			new LinkedList<PositionAbsolute<K>>();
	
	public SpheroPath(Collection<PositionAbsolute<K>> points) {
		waypoints.addAll(points);
		
	}
	
	public SpheroPath(LinkedList<PositionPolar<K>> points) {
//		waypoints.addAll(points);
		// FIXME CONVERT
	}

	@Override
	public PositionAbsolute<K> getNextWaypoint() {
		if (hasNext()) {
			return waypoints.get(curId++);
		} else {
			return null;
		}
	}

	@Override
	public boolean hasNext() {
		return waypoints.size() > curId;
	}

	public LinkedList<PositionAbsolute<K>> getListOfPoints() {
		return waypoints;
	}
}
