package com.orbotix.sample.spheroguide;

import java.util.EnumMap;
import java.util.Map;

import orbotix.robot.base.RGBLEDOutputCommand;
import orbotix.robot.base.Robot;
import orbotix.robot.base.RollCommand;
import orbotix.robot.sensor.LocatorData;
import android.util.Log;

import com.orbotix.sample.spheroguide.PathProvider.PositionPolar;
import com.orbotix.sample.spheroguide.PathProvider.PositionRelative;

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
    private PositionRelative<Double> oldWp;
		
	private boolean mIsRunning = true;

	private boolean mHasUpdatedLocPos = false;

	private PositionPolar<Double> mCurGoalWP;

	private double mControlFreq = 20.; // Hz
	
	private static double posError = 3.;
	
	
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
		
		mCurGoalWP = (PositionPolar<Double>) mPath.getNextWaypoint();
		oldWp      = new PositionRelative<Double>(0., 0.);
		
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
	public void update() throws InterruptedException {
		
		boolean transitionCheck = false;
		
		if (!mHasUpdatedLocPos)
			return ;
		
		// set LED according to current state
		updateSpheroLED();
		
		switch (mState) {
		
		case STATE_INITIAL:
			transitionCheck = init();
			mState = (transitionCheck) ? SpheroState.STATE_MOVING : SpheroState.STATE_ERROR;
			break;
			
		case STATE_MOVING:
			transitionCheck = move();
			mState = (transitionCheck) ? SpheroState.STATE_WP_REACHED : SpheroState.STATE_MOVING;
			break;
		case STATE_WP_REACHED: // <!- successfully reached WP
			transitionCheck = prepareNextMove();
			mState = (transitionCheck) ? SpheroState.STATE_GOAL_REACHED : SpheroState.STATE_MOVING;
			break;
			
		case STATE_GOAL_REACHED:
			transitionCheck = endOfMotion();
			mState = (transitionCheck) ? SpheroState.STATE_GOAL_REACHED : SpheroState.STATE_ERROR;
			break;
			
		case STATE_IAM_LOST: // <!- Sphero got off track
			
		case STATE_ERROR: // <!- random error - This should never be reached!
			mIsRunning = false;
		
		default:
			mState = SpheroState.STATE_ERROR;
			
		}		
	}

	

	private boolean move() {
		// quad. error
		double distX = lData.getPositionX() - oldWp.mX;
		double distY = lData.getPositionY() - oldWp.mY;
		double dist  = Math.sqrt(distX * distX + distY * distY);
		
		double err_2 = Math.pow(mCurGoalWP.distance.doubleValue() - dist, 2.0);
		double qErr   = Math.sqrt(err_2);
		
		// slow down before reaching WP
//		float dVel = (qErr > 50) ? 1.0f : (float) (qErr / 75.0f);
				
		if (qErr < posError) { // TODO OR error is increasing 
			RollCommand.sendStop(mRobot);
			return true;
		} else {
			RollCommand.sendCommand(mRobot, mCurGoalWP.angle.floatValue(), 0.5f);
			return false;
		}
	}

	private boolean prepareNextMove() throws InterruptedException {
		
		if (mPath.hasNext()) {
			mCurGoalWP = (PositionPolar<Double>) mPath.getNextWaypoint();
			oldWp = new PositionRelative<Double>((double) lData.getPositionX(), (double)lData.getPositionY());
			Thread.sleep(1000);
			
			return false;			
		} else {
			
			mCurGoalWP = null;
			Thread.sleep(3000);
			return true;
		}
			
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
		Thread.sleep(2000);
		
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

	public void stop() {
		mIsRunning = false;
		mState = SpheroState.STATE_ERROR;
		updateSpheroLED();
	}
	

	public synchronized void updateLocalisationData(LocatorData lData) {
		
		this.lData = lData;
		mHasUpdatedLocPos  = true;
	}
}
