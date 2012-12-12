package com.orbotix.sample.spheroguide;

import java.util.EnumMap;
import java.util.Map;

import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;

import orbotix.robot.base.RGBLEDOutputCommand;
import orbotix.robot.base.Robot;
import orbotix.robot.base.RollCommand;
import orbotix.robot.sensor.LocatorData;
import android.util.Log;

import com.orbotix.sample.spheroguide.PathProvider.PositionPolar;
import com.orbotix.sample.spheroguide.PathProvider.PositionAbsolute;

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
 * TODO
 * - state change via synchronised method, check isRunning
 *
 */

public class SpheroControl implements Runnable {

	/**
     * Robot to control
     */
    private Robot mRobot;
    private SpheroState mState = SpheroState.STATE_INITIAL;
    private Map<LEDColor,int[]> mColorMap;
    
    private LocatorData lData;
    private SpheroPath<Double> mPath;
    private PositionAbsolute<Double> mOldWp;
    private PositionAbsolute<Double> mCurGoalWP;
    private float mWPAngle;
    private double mWPDistance;
		
	private boolean mIsRunning = true;

	private boolean mHasUpdatedLocPos = false;


	private double mControlFreq = 20.; // Hz
	
	private static double posError = 3.; // cm
	private final double brakeZone = 20; // cm
	private final float  maxSpeed  = 0.6f; // ratio of sphero's max. velocity
	
	
	enum LEDColor {
		COLOR_RED,
		COLOR_GREEN,
		COLOR_BLUE,
		COLOR_YELLOW,
		COLOR_MAGENTA,
		COLOR_CYAN,
		COLOR_WHITE,
		COLOR_OFF,
	}
	
	enum SpheroState {
		STATE_INITIAL,
		STATE_MOVING,
		STATE_WP_REACHED,
		STATE_GOAL_REACHED,
		STATE_IAM_LOST,
		STATE_ERROR
	}
	
	public SpheroControl(Robot robot, SpheroPath<Double> path) {
		mRobot              = robot;
		mIsRunning          = true;
		mPath               = path;
		
		mCurGoalWP = (PositionAbsolute<Double>) mPath.getNextWaypoint();
		mOldWp      = new PositionAbsolute<Double>(0., 0.);
		
		calculateDistanceAndAngle();
		
		// init color codes
		int[][] mStateColors = {{255,0,0},{0,255,0},{0,0,255},
								{255,255,0},{255,0,255}, {0,255,255},
								{255,255,255},{0,0,0}};
		
		// create color map
		mColorMap = new EnumMap<LEDColor,int[]>(LEDColor.class);
		mColorMap.put(LEDColor.COLOR_RED,     mStateColors[0]);
		mColorMap.put(LEDColor.COLOR_GREEN,   mStateColors[1]);
		mColorMap.put(LEDColor.COLOR_BLUE,    mStateColors[2]);
		mColorMap.put(LEDColor.COLOR_YELLOW,  mStateColors[3]);
		mColorMap.put(LEDColor.COLOR_MAGENTA, mStateColors[4]);
		mColorMap.put(LEDColor.COLOR_CYAN,    mStateColors[5]);
		mColorMap.put(LEDColor.COLOR_WHITE,   mStateColors[6]);
		mColorMap.put(LEDColor.COLOR_OFF,     mStateColors[7]);
	}
	
	private boolean init() throws InterruptedException {
		
		Thread.sleep(1000);
		
		return true;
	}

	/**
	 * 
	 * @throws InterruptedException 
	 */	
	public synchronized void update() throws InterruptedException {
		
		boolean transitionCheck = false;
		SpheroState transState  = SpheroState.STATE_INITIAL;
		
		// set LED according to current state
		updateSpheroLED();
		
		if (!mHasUpdatedLocPos)
			return;	
		
		switch (mState) {
		
		case STATE_INITIAL:
			transitionCheck = init();
			transState = (transitionCheck) ? SpheroState.STATE_MOVING : SpheroState.STATE_ERROR;
			break;
			
		case STATE_MOVING:
			transitionCheck = move();
			transState = (transitionCheck) ? SpheroState.STATE_WP_REACHED : SpheroState.STATE_MOVING;
			break;
		case STATE_WP_REACHED: // <!- successfully reached WP
			transitionCheck = prepareNextMove();
			transState = (transitionCheck) ? SpheroState.STATE_GOAL_REACHED : SpheroState.STATE_MOVING;
			break;
			
		case STATE_GOAL_REACHED:
			transitionCheck = endOfMotion();
			transState = (transitionCheck) ? SpheroState.STATE_GOAL_REACHED : SpheroState.STATE_ERROR;
			break;
			
		case STATE_IAM_LOST: // <!- Sphero got off track
			
		case STATE_ERROR: // <!- random error - This should never be reached!
			mIsRunning = false;
		
		default:
			transState = SpheroState.STATE_ERROR;
			
		}
		
		// proceed state change
		changeState(transState);
	}

	

	private boolean move() {
		// quad. error
		double distX = lData.getPositionX() - mOldWp.mX;
		double distY = lData.getPositionY() - mOldWp.mY;
		double dist  = Math.sqrt(distX * distX + distY * distY);
		
		double err_2 = Math.pow(mWPDistance - dist, 2.0);
		double qErr   = Math.sqrt(err_2);
		
		// slow down before reaching WP
		float dVel = (qErr > brakeZone) ? 1.0f : (float) (qErr / brakeZone);
				
		if (qErr < posError) { // TODO OR error is increasing 
			RollCommand.sendStop(mRobot);
			return true;
		} else {
			RollCommand.sendCommand(mRobot, mWPAngle, maxSpeed * dVel);
			return false;
		}
	}

	private boolean prepareNextMove() throws InterruptedException {
		
		if (mPath.hasNext()) {
			mCurGoalWP = (PositionAbsolute<Double>) mPath.getNextWaypoint();
			mOldWp = new PositionAbsolute<Double>((double) lData.getPositionX(), (double)lData.getPositionY());
			
			calculateDistanceAndAngle();
			
			Thread.sleep(1000);
			
			return false;			
		} else {
			
			mCurGoalWP = null;
			return true;
		}
			
	}


	private void calculateDistanceAndAngle() {
		double x2 = mCurGoalWP.mX - mOldWp.mX;
		double y2 = mCurGoalWP.mY - mOldWp.mY;
		mWPDistance = Math.sqrt(Math.pow(x2, 2.0) + Math.pow(y2, 2.0));
		mWPAngle    = (float) (((Math.atan2(y2, x2) - (Math.PI / 2.)) * -1.) * (180 / Math.PI)) % 360;
		
		/*for (int i = 0; i< 3; ++i) {
			
			Log.e("Sphero", "Angle:    " + mWPAngle);
			Log.e("Sphero", "Distance: " + mWPDistance);
			Log.e("Sphero", "atan2     " + (Math.atan2(y2, x2) + (Math.PI/2.0)));
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
		
	}

	/**
	 * set LED corresponding to the current controller state
	 */
	private void updateSpheroLED() {

		int[] color = mColorMap.get(LEDColor.COLOR_OFF);
		
		switch (mState) {
		case STATE_INITIAL:
			color = mColorMap.get(LEDColor.COLOR_GREEN);
			break;
			
		case STATE_MOVING:
			color = mColorMap.get(LEDColor.COLOR_CYAN);
			break;
			
		case STATE_WP_REACHED: // <!- successfully reached WP
			color = mColorMap.get(LEDColor.COLOR_MAGENTA);
			break;
			
		case STATE_GOAL_REACHED:
			color = mColorMap.get(LEDColor.COLOR_BLUE);
			break;
			
		case STATE_IAM_LOST: // <!- Sphero got off track
			color = mColorMap.get(LEDColor.COLOR_YELLOW);
			break;
			
		case STATE_ERROR: // <!- random error - This should never be reached!
			color = mColorMap.get(LEDColor.COLOR_RED);
			break;
		
		default:
		}
		
		RGBLEDOutputCommand.sendCommand(mRobot, color[0], color[1], color[2]);

	}

	private boolean endOfMotion() throws InterruptedException {
		Thread.sleep(3000);
		
		Log.e("Sphero","Sphero has arrived");
		// ends spheros journey
		
		return true;
	}
	
	@Override
	public void run() {

		while (isRunning() && mCurGoalWP != null) {
			try {
				// toggle a new controller step
				update();
				
				// run in the defined frequency
				Thread.sleep((int) (1000 * (1. / mControlFreq)));
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		RollCommand.sendStop(mRobot);
		Log.e("Sphero","Controlling Motion has finished");
	}

	private boolean isRunning() {
		return mIsRunning ;
	}

	public synchronized void stop() {
		changeState(SpheroState.STATE_ERROR);
		mIsRunning = false;
		updateSpheroLED();
	}
	
	public synchronized void changeState(SpheroState newState) {
		if (isRunning()) {
			mState = newState;
		}
	}
	

	public synchronized void updateLocalisationData(LocatorData lData) {
		
		this.lData = lData;
		mHasUpdatedLocPos  = true;
	}
}
