package com.orbotix.sample.spheroguide;

import java.util.Collection;
import java.util.LinkedList;

public class SpheroPath<K extends Number> implements PathProvider<K> {
	
	private int curId = 0;
	private LinkedList<PositionPolar<K>> waypoints = 
			new LinkedList<PositionPolar<K>>();
	
	public SpheroPath(Collection<com.orbotix.sample.spheroguide.PathProvider.Position<K>> points) {
		waypoints.add(new PositionPolar<K>(null, null));
		
		// FIXME CONVERT
	}
	
	public SpheroPath(LinkedList<PositionPolar<K>> points) {
		waypoints.addAll(points);
	}

	@Override
	public com.orbotix.sample.spheroguide.PathProvider.Position<K> getNextWaypoint() {
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

	public LinkedList<PositionPolar<K>> getListOfPoints() {
		return waypoints;
	}
}
