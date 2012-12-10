package com.orbotix.sample.spheroguide;


public interface PathProvider<K extends Number> {
	
	public interface Position<T> {
		
	}
	
	public class PositionRelative<T> implements Position<T> {
		public T mX, mY;
		PositionRelative (T x, T y) {
			mX = x;
			mY = y;
		}
	}
	
	public class PositionPolar<T> implements Position<T> {
		public T angle;
		public T distance;
		
		public PositionPolar(T a, T d) {
			angle = a;
			distance = d;
		}
	}
	
	public Position<K> getNextWaypoint();
	
	public boolean hasNext();

}
