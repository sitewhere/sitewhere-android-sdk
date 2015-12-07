/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sitewhere.android.example;

import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitewhere.android.generated.Android;
import com.sitewhere.android.generated.Android.AndroidSpecification._Header;
import com.sitewhere.android.generated.Android.AndroidSpecification.changeBackground;
import com.sitewhere.androidsdk.SiteWhereMessageClient;
import com.sitewhere.androidsdk.messaging.SiteWhereMessagingException;
import com.sitewhere.androidsdk.mqtt.preferences.IMqttServicePreferences;
import com.sitewhere.androidsdk.mqtt.preferences.MqttServicePreferences;
import com.sitewhere.androidsdk.preferences.IConnectivityPreferences;
import com.sitewhere.device.communication.protobuf.proto.Sitewhere;
import com.sitewhere.device.communication.protobuf.proto.Sitewhere.Device.DeviceStreamAck;
import com.sitewhere.device.communication.protobuf.proto.Sitewhere.Device.Header;
import com.sitewhere.device.communication.protobuf.proto.Sitewhere.Device.RegistrationAck;
import com.sitewhere.rest.model.device.event.DeviceMeasurements;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * SiteWhere sample activity.
 * 
 * @author Derek
 */
public class SiteWhereExample extends AppCompatActivity implements IConnectivityWizardListener, SiteWhereMessageClient.SiteWhereMessageClientCallback {

	/** Tag for logging */
	private static final String TAG = "SiteWhereExample";

	/** Wizard shown to establish preferences */
	private ConnectivityWizardFragment wizard;

	/** Fragment with example application */
	private ExampleFragment example;

    /** Message client for sending events to SiteWhere */
	private SiteWhereMessageClient messageClient;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v7.app.ActionBarActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		messageClient = new SiteWhereMessageClient(getApplicationContext());

		// Verify that SiteWhere API location has been specified.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String apiUrl = prefs.getString(IConnectivityPreferences.PREF_SITEWHERE_API_URI, null);

		// Push current device id into MQTT settings, then get current values.
		MqttServicePreferences updated = new MqttServicePreferences();
		updated.setDeviceHardwareId(messageClient.getUniqueDeviceId());
		IMqttServicePreferences mqtt = MqttServicePreferences.update(updated, this);

		if ((apiUrl == null) || (mqtt.getBrokerHostname() == null)) {
			initConnectivityWizard();
		} else {
			initExampleApplication();
		}
	}

	/**
	 * Adds the connectivity wizard if preferences have not been set.
	 */
	protected void initConnectivityWizard() {
		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		wizard = new ConnectivityWizardFragment();
		wizard.setWizardListener(this);
		fragmentTransaction.replace(R.id.container, wizard);
		fragmentTransaction.commit();
		//getActionBar().setTitle("SiteWhere Device Setup");
	}

	/**
	 * Adds the connectivity wizard if preferences have not been set.
	 */
	protected void initExampleApplication() {
		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		example = new ExampleFragment();
		fragmentTransaction.replace(R.id.container, example);
		fragmentTransaction.commit();

		messageClient.setCallback(this);
		messageClient.connect(MqttServicePreferences.read(this));
		//getActionBar().setTitle("SiteWhere Example");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.android.mqtt.ui.IConnectivityWizardListener#onWizardComplete()
	 */
	@Override
	public void onWizardComplete() {
		initExampleApplication();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.example_app_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_setup_wizard:
			initConnectivityWizard();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		messageClient.disconnect();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see SiteWhereMessageClientCallback
	 */
	@Override
	public void onConnectedToSiteWhere() {
		Log.d(TAG, "Connected to SiteWhere.");

        try {
            // This registers to receive any event that is sent to SiteWhere with a specific site.
            // Filter and route configuration with groovy rules scripts will determine what events
            // get sent to this client.
            messageClient.registerForEvents("/bb105f8d-3150-41f5-b9d1-db04965668d3");

			// This registers device with a specific site.
			messageClient.sendDeviceRegistration(messageClient.getUniqueDeviceId(), "d2604433-e4eb-419b-97c7-88efe9b2cd41",
                    null, "bb105f8d-3150-41f5-b9d1-db04965668d3");
		} catch (SiteWhereMessagingException e) {
			Log.e(TAG, "Unable to send device registration to SiteWhere.", e);
		}
	}

    /*
	 * (non-Javadoc)
	 *
	 * @see SiteWhereMessageClientCallback
	 */
	@Override
	public void onDisconnectedFromSiteWhere() {
		Log.d(TAG, "Disconnected from SiteWhere.");

        try {
            messageClient.sendDeviceAlert(messageClient.getUniqueDeviceId(), "sitewhere.disconnected", "Disconnected to SiteWhere.", null);
        } catch (SiteWhereMessagingException e) {
            Log.e(TAG, "Unable to send device alert to SiteWhere.", e);
        }

        if (example != null) {
			example.onSiteWhereDisconnected();
		}
	}

    /*
	 * (non-Javadoc)
	 *
	 * @see SiteWhereMessageClientCallback
	 */
    @Override
    public void onReceivedSystemCommand(byte[] payload) {
        ByteArrayInputStream stream = new ByteArrayInputStream(payload);
        try {
            Header header = Sitewhere.Device.Header.parseDelimitedFrom(stream);
            switch (header.getCommand()) {
                case ACK_REGISTRATION: {
                    RegistrationAck ack = RegistrationAck.parseDelimitedFrom(stream);
                    handleRegistrationAck(header, ack);
                    break;
                }
                case ACK_DEVICE_STREAM: {
                    DeviceStreamAck ack = DeviceStreamAck.parseDelimitedFrom(stream);
                    handleDeviceStreamAck(header, ack);
                    break;
                }
                case RECEIVE_DEVICE_STREAM_DATA: {
                    Sitewhere.Model.DeviceStreamData chunk = Sitewhere.Model.DeviceStreamData.parseDelimitedFrom(stream);
                    // TODO
                    handleReceivedDeviceStreamData(header, chunk);
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to process system command.", e);
        }
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see SiteWhereMessageClientCallback
	 */
    @Override
    public void onReceivedEventMessage(String topic, byte[] payload) {
        Log.d(TAG, "Received event message " + new String(payload));
        try {
            JsonNode eventNode = new ObjectMapper().readTree(payload);
            String eventType = eventNode.get("eventType").textValue();
            if ("Measurements".equals(eventType)) {
                DeviceMeasurements dm = new ObjectMapper().treeToValue(eventNode, DeviceMeasurements.class);
                Map<String, Double> measurements = dm.getMeasurements();
                StringBuilder sb = new StringBuilder();
                for (String key : measurements.keySet()) {
                    sb.append(key).append(": ");
                    sb.append(measurements.get(key));
                    sb.append(" ");
                }
                Log.d(TAG, "Received measurements " + sb.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRegistrationAck(Header header, RegistrationAck ack) {
		switch (ack.getState()) {
			case REGISTRATION_ERROR: {
				Log.d(TAG, "Error registering device. " + ack.getErrorType().name() + ": " + ack.getErrorMessage());
				return;
			}
			case ALREADY_REGISTERED: {
				Log.d(TAG, "Device was already registered.");
				break;
			}
			case NEW_REGISTRATION: {
				Log.d(TAG, "Device was registered successfully.");
				break;
			}
		}

        try {
            // send connected alert to sitewhere
            messageClient.sendDeviceAlert(messageClient.getUniqueDeviceId(), "sitewhere.connected", "Connected to SiteWhere.", null);
        } catch (SiteWhereMessagingException e) {
            Log.e(TAG, "Unable to send device alert to SiteWhere.", e);
        }

		if (example != null) {
			example.onSiteWhereConnected();
		}
	}

	public void handleDeviceStreamAck(Header header, DeviceStreamAck ack) {
	}

    public void handleReceivedDeviceStreamData(Header header, Sitewhere.Model.DeviceStreamData data) {
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see SiteWhereMessageClientCallback
	 */
	public void onReceivedCustomCommand(byte[] payload) {
		Log.d(TAG, "Received custom command.");
		ByteArrayInputStream stream = new ByteArrayInputStream(payload);
		try {
			_Header header = Android.AndroidSpecification._Header.parseDelimitedFrom(stream);
			switch (header.getCommand()) {
			case CHANGEBACKGROUND: {
				final changeBackground cb = changeBackground.parseDelimitedFrom(stream);
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						getWindow().getDecorView().setBackgroundColor(Color.parseColor(cb.getColor()));
					}
				});
				messageClient.sendAck(messageClient.getUniqueDeviceId(), header.getOriginator(), "Updated background color.");
				Log.i(TAG, "Sent reponse to 'changeBackground' command.");
				break;
			}
			case PING: {
                messageClient.sendAck(messageClient.getUniqueDeviceId(), header.getOriginator(), "Acknowledged.");
				Log.i(TAG, "Sent reponse to 'ping' command.");
				break;
			}
			case TESTEVENTS: {
                Map<String, Double> measurements = new HashMap<>();
                measurements.put("engine.temp", 170.0);
				messageClient.sendDeviceMeasurements(messageClient.getUniqueDeviceId(), /*header.getOriginator(),*/ measurements, null);
                messageClient.sendDeviceLocation(messageClient.getUniqueDeviceId(), /*header.getOriginator(),*/ 33.7550, -84.3900, 0.0, null);
                messageClient.sendDeviceAlert(messageClient.getUniqueDeviceId(), /*header.getOriginator(),*/ "engine.overheat",
                        "Engine is overheating!", null);
				Log.i(TAG, "Sent reponse to 'testEvents' command.");
				break;
			}
			}
		} catch (IOException e) {
			Log.e(TAG, "IO exception processing custom command.", e);
		} catch (SiteWhereMessagingException e) {
			Log.e(TAG, "Messaging exception processing custom command.", e);
		}
	}
}
