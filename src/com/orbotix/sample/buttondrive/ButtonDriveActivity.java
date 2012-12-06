package com.orbotix.sample.buttondrive;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import orbotix.robot.base.*;
import orbotix.robot.sensor.DeviceSensorsData;
import orbotix.robot.sensor.LocatorData;
import orbotix.robot.widgets.calibration.CalibrationView;
import orbotix.view.connection.SpheroConnectionView;
import orbotix.view.connection.SpheroConnectionView.OnRobotConnectionEventListener;

/**
 * Activity for controlling the Sphero with five control buttons.
 */
public class ButtonDriveActivity extends Activity implements OnItemSelectedListener
{
    /**
     * Data Streaming Packet Counts
     */
    private final static int TOTAL_PACKET_COUNT = 200;
    private final static int PACKET_COUNT_THRESHOLD = 50;
    private int mPacketCounter;

    /**
     * Robot to control
     */
    private Robot mRobot;

    /**
     * The Sphero Connection View
     */
    private SpheroConnectionView mSpheroConnectionView;
    
    private Handler mHandler = new Handler();

    private String mDest = "A";

    /**
     * AsyncDataListener that will be assigned to the DeviceMessager, listen for streaming data, and then do the
     *
     */
    private DeviceMessenger.AsyncDataListener mDataListener = new DeviceMessenger.AsyncDataListener() {
        @Override
        public void onDataReceived(DeviceAsyncData data) {

            if(data instanceof DeviceSensorsAsyncData){
            	
            	// If we are getting close to packet limit, request more
            	mPacketCounter++;
            	if( mPacketCounter > (TOTAL_PACKET_COUNT - PACKET_COUNT_THRESHOLD) ) {
            		requestDataStreaming();
            	}

                //get the frames in the response
                List<DeviceSensorsData> data_list = ((DeviceSensorsAsyncData)data).getAsyncData();
                if(data_list != null){

                    // Iterate over each frame, however we set data streaming as only one frame
                    for(DeviceSensorsData datum : data_list){

                        LocatorData locatorData = datum.getLocatorData();
                        if( locatorData != null ) {
                            ((TextView)findViewById(R.id.txt_locator_x)).setText(locatorData.getPositionX() + " cm");
                            ((TextView)findViewById(R.id.txt_locator_y)).setText(locatorData.getPositionY() + " cm");
                            ((TextView)findViewById(R.id.txt_locator_vx)).setText(locatorData.getVelocityX() + " cm/s");
                            ((TextView)findViewById(R.id.txt_locator_vy)).setText(locatorData.getVelocityY() + " cm/s");
                        }
                    }
                }
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        findViewById(R.id.back_layout).requestFocus();
        
        mSpheroConnectionView = (SpheroConnectionView)findViewById(R.id.sphero_connection_view);
        // Set the connection event listener 
        mSpheroConnectionView.setOnRobotConnectionEventListener(new OnRobotConnectionEventListener() {
        	// If the user clicked a Sphero and it failed to connect, this event will be fired
			@Override
			public void onRobotConnectionFailed(Robot robot) {}
			// If there are no Spheros paired to this device, this event will be fired
			@Override
			public void onNonePaired() {}
			// The user clicked a Sphero and it successfully paired.
			@Override
			public void onRobotConnected(Robot robot) {
				mRobot = robot;
				// Skip this next step if you want the user to be able to connect multiple Spheros
				mSpheroConnectionView.setVisibility(View.GONE);
				
				// This delay post is to give the connection time to be created
				mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Start streaming Locator values
                        requestDataStreaming();
                        
                        //Set the AsyncDataListener that will process each response.
                        DeviceMessenger.getInstance().addAsyncDataListener(mRobot, mDataListener); 

                        // Let Calibration View know which robot we are connected to
                        CalibrationView calibrationView = (CalibrationView)findViewById(R.id.calibration_widget);
                        calibrationView.setRobot(mRobot);
                    }
                }, 1000);
			}
		});

        Spinner spinner = (Spinner) findViewById(R.id.choose_node_spinner);
        spinner.setOnItemSelectedListener(this);
        // Create an ArrayAdapter using the string array and a default spinner layout

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.nodes_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
    }

    /**
     * When the user clicks "STOP", stop the Robot.
     * @param v The View that had been clicked
     */
    public void onStopClick(View v){

        if(mRobot != null){
            //Stop robot
            RollCommand.sendCommand(mRobot, 0f, 0f);
        }
    }

    /**
     * When the user clicks a control button, roll the Robot in that direction
     * @param v The View that had been clicked
     */
    public void onControlClick(View v){
        
        //Find the heading, based on which button was clicked
        final float heading;
        switch (v.getId()){
/*            
            case R.id.ninety_button:
                heading = 90f;
                break;
            
            case R.id.one_eighty_button:
                heading = 180f;
                break;
            
            case R.id.two_seventy_button:
                heading = 270f;
                break;
*/
            default:
                heading = 0f;
                break;
        }
        
        //Set speed. 60% of full speed
        final float speed = 0.6f;

        //Roll robot
        RollCommand.sendCommand(mRobot, heading, speed);
    }

    /**
     * Disconnect from the robot when the Activity stops
     */
    @Override
    protected void onStop() {
        super.onStop();

        //disconnect robot
        mSpheroConnectionView.shutdown();
        RobotProvider.getDefaultProvider().disconnectControlledRobots();
    }

    private void requestDataStreaming(){

        if(mRobot == null) return;

        // Set up a bitmask containing the sensor information we want to stream, in this case locator
        // with which only works with Firmware 1.20 or greater.
        final long mask = SetDataStreamingCommand.DATA_STREAMING_MASK_LOCATOR_ALL;

        //Specify a divisor. The frequency of responses that will be sent is 400hz divided by this divisor.
        final int divisor = 1;

        //Specify the number of frames that will be in each response. You can use a higher number to "save up" responses
        //and send them at once with a lower frequency, but more packets per response.
        final int packet_frames = 20;

        // Reset finite packet counter
        mPacketCounter = 0;
        
        // Count is the number of async data packets Sphero will send you before
        // it stops.  You want to register for a finite count and then send the command
        // again once you approach the limit.  Otherwise data streaming may be left
        // on when your app crashes, putting Sphero in a bad state 
        final int response_count = TOTAL_PACKET_COUNT;


        // Send this command to Sphero to start streaming.  
        // If your Sphero is on Firmware less than 1.20, Locator values will display as 0's
        SetDataStreamingCommand.sendCommand(mRobot, divisor, packet_frames, mask, response_count);
    }

    /**
     * Calibrate Sphero when a two finger event occurs
     */
    public boolean dispatchTouchEvent(MotionEvent event) {
        CalibrationView calibrationView = (CalibrationView)findViewById(R.id.calibration_widget);
        // Notify Calibration widget of a touch event
        calibrationView.interpretMotionEvent(event);
        return super.dispatchTouchEvent(event);
    }

    /**
     * When the user clicks the configure button, it calls this function
     * @param v
     */
    public void onCalibratePressed(View v) {

        if( mRobot == null ) return;

        int newX = 0;   // The locator's current X position value will be set to this value
        int newY = 0;   // The locator's current Y position value will be set to this value
        int newYaw = 0; // The yaw value you set this to, will represent facing down the +y_axis

        // Flag will be true if the check box is clicked, false if it is not
        // When the flag is off (default behavior) the x, y locator grid is rotated with the calibration
        // When the flag is on the x, y locator grid is fixed and Sphero simply calibrates within it
        /*int flag = ((CheckBox)findViewById(R.id.checkbox_flag)).isChecked() ?
                        ConfigureLocatorCommand.ROTATE_WITH_CALIBRATE_FLAG_ON :
                        ConfigureLocatorCommand.ROTATE_WITH_CALIBRATE_FLAG_OFF;*/
        // TODO: which flag do we want???
//        int flag = ConfigureLocatorCommand.ROTATE_WITH_CALIBRATE_FLAG_ON;
        int flag = ConfigureLocatorCommand.ROTATE_WITH_CALIBRATE_FLAG_OFF;

        ConfigureLocatorCommand.sendCommand(mRobot, flag, newX, newY, newYaw);
    }

    /**
     * When the user clicks the go button, it calls this function
     * @param v
     */
    public void onGoPressed(View v) {
        String tempDest = mDest;
        if (mDest.equals("A")) {
            Log.d("Sphero", "Going to " + mDest);
            // TODO: implement
            //RollCommand.sendCommand(mRobot, 315f, 0.5f);
        } else if (mDest.equals("B")) {
            Log.d("Sphero", "Going to " + mDest);
        } else if (mDest.equals("C")) {
            Log.d("Sphero", "Going to " + mDest);
        } else if (mDest.equals("D")) {
            Log.d("Sphero", "Going to " + mDest);
        } else if (mDest.equals("E")) {
            Log.d("Sphero", "Going to " + mDest);
        } else if (mDest.equals("F")) {
            Log.d("Sphero", "Going to " + mDest);
        } else if (mDest.equals("G")) {
            Log.d("Sphero", "Going to " + mDest);
        } else if (mDest.equals("H")) {
            Log.d("Sphero", "Going to " + mDest);
        } else if (mDest.equals("I")) {
            Log.d("Sphero", "Going to " + mDest);
        } else if (mDest.equals("J")) {
            Log.d("Sphero", "Going to " + mDest);
        } else if (mDest.equals("K")) {
            Log.d("Sphero", "Going to " + mDest);
        }
    }

    @Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
        Log.d("Sphero", "before User selected " + mDest + ", pos=" + pos);
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        switch (pos) {
        case 0:
            mDest = "A";
            break;
        case 1:
            mDest = "B";
            break;
        case 2:
            mDest = "C";
            break;
        case 3:
            mDest = "D";
            break;
        case 4:
            mDest = "E";
            break;
        case 5:
            mDest = "F";
            break;
        case 6:
            mDest = "G";
            break;
        case 7:
            mDest = "H";
            break;
        case 8:
            mDest = "I";
            break;
        case 9:
            mDest = "J";
            break;
        case 10:
            mDest = "K";
            break;
        }
        Parser.setDestination(mDest);
        Log.d("Sphero", "after User selected " + mDest + ", pos=" + pos);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub
		
	}
}
