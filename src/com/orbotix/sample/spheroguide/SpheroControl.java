package com.orbotix.sample.spheroguide;

import orbotix.robot.base.Robot;
import orbotix.robot.base.RollCommand;
import orbotix.robot.sensor.LocatorData;
import android.util.Log;

/**
 * 
 * coordinate system definition of sphero
 *     Y // 0 Degrees
 *
 *     ^
 *     |
 *     |___>  x // 90 Degress
 * 
 * @author Steffen
 *
 */

public class SpheroControl implements Runnable {

	/**
     * Robot to control
     */
    private Robot mRobot;
    
    private LocatorData lData;
	
	double goalX, goalY;
	
	private boolean isRunning = true;
	boolean hasAdjustedHeading = false;

	private boolean hasUpdatedLocPos = false;
	
	static double posError = 20.0;
	
	public SpheroControl(Robot rbt, int x, int y) {
		goalX       = x;
		goalY       = y;
		mRobot      = rbt;
		hasAdjustedHeading = false;
		isRunning          = true;

	}
	
	/**
	 * 
	 * @return true, iff control point has been reached
	 */	
	public boolean control() {
		
		if (!hasUpdatedLocPos) // FIXME
			return false; // skip
		
		// quad. error
		double errX_2 = Math.pow(goalX - lData.getPositionX(), 2.0);
		double errY_2 = Math.pow(goalY - lData.getPositionY(), 2.0);
		double qErr   = Math.sqrt(errX_2 + errY_2);
		
		if (qErr < posError) {
			return true;
		} else {
			// more control needed
			
			double delta_x = goalX - lData.getPositionX();
			double delta_y = goalY - lData.getPositionY();
			float theta    = (float) (Math.atan2(delta_y, delta_x) * (180.0 / Math.PI)) + 180.0f;
			
//			theta = (float) Math.acos( (goalX * 0 + goalY * 100) 
//					/ (Math.sqrt(Math.pow(goalX,2.0) + Math.pow(goalY,2.0)) * Math.sqrt((Math.pow(0,2.0) + Math.pow(100,2.0)))));

			float dVel = (qErr > 100) ? 50.0f : (float) (qErr / 50.0f);

			String str = "Heading: " + theta;
			Log.e("Sphero", str);
			
			if (Math.abs(theta) > 5 && !hasAdjustedHeading) {
				try {
					RollCommand.sendCommand(mRobot, theta, 0.05f);
					hasAdjustedHeading = true;
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else
				RollCommand.sendCommand(mRobot, 0f, 0.5f * dVel);
			
			
//			System.out.println("Errors: " + (goalX - locData.getPositionX()) + " :: " 
			//							  + (goalY - locData.getPositionY()));

//			System.out.println("lData.x " + locData.getPositionX() + " lData.y " + locData.getPositionY());
			return false;
		}
		
		
	}

	@Override
	public void run() {
		boolean hasReachedGoalPos  = false;

		while (!hasReachedGoalPos && isRunning()) {
			hasReachedGoalPos = control();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		RollCommand.sendStop(mRobot);
		System.out.println("Controlling Motion has finished");
	}

	private boolean isRunning() {
		return isRunning ;
	}

	public void stop() {
		isRunning = false;
	}
	

	public synchronized void updateLocalisationData(LocatorData lData) {
		
		this.lData = lData;
		hasUpdatedLocPos  = true;
	}
}
