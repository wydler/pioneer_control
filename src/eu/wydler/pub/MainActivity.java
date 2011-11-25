/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package eu.wydler.pub;

import java.net.URI;

import org.ros.message.geometry_msgs.Twist;
import org.ros.message.sensor_msgs.CompressedImage;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeRunner;
import org.ros.rosjava.android.BitmapFromCompressedImage;
import org.ros.rosjava.android.views.RosImageView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import eu.wydler.helper.Helper;

/**
 * @author michael.wydler@googlemail.com (Michael Wydler)
 */
public class MainActivity extends Activity implements SensorEventListener {

	/* Sensor Manager */
	private SensorManager mSensorManager;
    
	/* Sensorenwerte */
    private float mSensorX;
    private float mSensorY;
    private float mSensorZ;
    private TextView outputX;
  	private TextView outputY;
  	private TextView outputZ;
    
    /* Übertragung aktiv */
    private boolean accelerometerActive;
	
    /* ROS Noderunner */
	private final NodeRunner nodeRunner;
	AlertDialog.Builder builder;

	private RosImageView<CompressedImage> image;
	private Talker talker;
	
	String ROSMASTERURI;
    boolean VIDEOBACKGROUND;

	public MainActivity() {
		nodeRunner = NodeRunner.newDefault();
	}
  
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		/* UI-Element initialisieren */		
		/* Textfelder fuer Sensorenwerte */
	    outputX = (TextView) findViewById(R.id.acc_x);
	    outputY = (TextView) findViewById(R.id.acc_y);
	    outputZ = (TextView) findViewById(R.id.acc_z);
	    
	    /* Video als Hintergrund */
	    image = (RosImageView<CompressedImage>) findViewById(R.id.image);
	    image.setTopicName("/camera/rgb/image_color/compressed");
	    image.setMessageType("sensor_msgs/CompressedImage");
	    image.setMessageToBitmapCallable(new BitmapFromCompressedImage());
	    
	    /* Button für Uebertragung */
	    final Button button = (Button) findViewById(R.id.button);
		button.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent e) {
				switch(e.getAction()) {
				case MotionEvent.ACTION_DOWN:
					accelerometerActive = true;
					break;
				case MotionEvent.ACTION_UP:
					accelerometerActive = false;
					break;
				}		
				return false;
			}
		});

		/* Sensorservice abrufen */
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	}
  
	@Override
	protected void onPause() {
		super.onPause();
		/* Sensor abmelden */
		mSensorManager.unregisterListener(this, 
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
		
		/* Talker beenden */
		talker.shutdown();
		if(VIDEOBACKGROUND) {
			image.shutdown();
		}
	}
  
	@Override
	protected void onStop() {
		super.onStop();
		/* Sensor abmelden */
		mSensorManager.unregisterListener(this, 
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferences();
		try {
			/* Eigene Node */
			NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(Helper.getLocalIpAddress());
			nodeConfiguration.setMasterUri(new URI(ROSMASTERURI));
			
			/* Talker erstellen starten */
			talker = new Talker();
			nodeRunner.run(talker, nodeConfiguration);
			if(VIDEOBACKGROUND) {
				nodeRunner.run(image, nodeConfiguration);
			}

			/* Sensor anmelden */
			mSensorManager.registerListener(this, 
					mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
					SensorManager.SENSOR_DELAY_NORMAL);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.optionsmenu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.preferences:
	    	Intent settingsActivity = new Intent(getBaseContext(),
                    Preferences.class);
    		startActivity(settingsActivity);
	        return true;
	    case R.id.help:
	    	Toast.makeText(
					getApplicationContext(), 
					"TODO", 
					Toast.LENGTH_LONG).show();
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
  
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		/* keine Implementierung nötig */
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {            
            mSensorX = Helper.normalizeAccelerationValue(event.values[0]);
            mSensorY = Helper.normalizeAccelerationValue(event.values[1]);
            mSensorZ = Helper.normalizeAccelerationValue(event.values[2]);

            // Werte in Textfelder schreiben
            outputX.setText("x:"+mSensorX);
            outputY.setText("y:"+mSensorY);
            outputZ.setText("z:"+mSensorZ);
            
            Twist t = new Twist();
            
            // Übertragung erfolgt nur, wenn Button gedrückt ist
            if (accelerometerActive) {
            	t.linear.x = mSensorZ;
            	if((t.linear.x < 0.1) && (t.linear.x > -0.1)) {
            		t.linear.x = 0;
            	}
            	t.angular.z = -mSensorY;
            	if((t.angular.z < 0.1) && (t.angular.z > -0.1)) {
            		t.angular.z = 0;
            	}
            }
            else {
            	t.linear.x = 0;
            	t.angular.z = 0;
            }
            talker.publish(t);
        }
	}
	
	private void getPreferences() {
        SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());
        ROSMASTERURI = prefs.getString("rosmasteruri", "http://141.69.58.247:11311");
        VIDEOBACKGROUND = prefs.getBoolean("videobg", true);
	}
}

