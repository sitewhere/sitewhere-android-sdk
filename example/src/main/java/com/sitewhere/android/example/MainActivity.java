/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.android.example;

import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.sitewhere.androidsdk.SiteWhereMessageClient;
import com.sitewhere.androidsdk.SiteWhereMessageClient.SiteWhereMessageClientCallback;
import com.sitewhere.androidsdk.messaging.SiteWhereMessagingException;
import com.sitewhere.androidsdk.mqtt.preferences.IMqttServicePreferences;
import com.sitewhere.androidsdk.preferences.IConnectivityPreferences;
import com.sitewhere.androidsdk.mqtt.preferences.MqttServicePreferences;
import com.sitewhere.communication.protobuf.proto.SiteWhere.Device.DeviceStreamAck;
import com.sitewhere.communication.protobuf.proto.SiteWhere.Device.Header;
import com.sitewhere.communication.protobuf.proto.SiteWhere.Device.RegistrationAck;
import com.sitewhere.android.Android;
import com.sitewhere.rest.model.device.event.DeviceMeasurement;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * SiteWhere 2.0 sample Main Activity.
 *
 * @author Derek
 * @author Jorge Villaverde
 */
public class MainActivity extends AppCompatActivity implements IConnectivityWizardListener, SiteWhereMessageClientCallback {

    /** Tag for logging */
    private static final String TAG = "SiteWhereExample";

    /** Default Area Token */
    public static final String DEFAULT_AREA_TOKEN = "southeast";

    /** Default Customer Token */
    public static final String DEFAULT_CUSTOMER_TOKEN = "acme";

    /** Default Device Type Token */
    public static final String DEFAULT_DEVICE_TYPE_TOKEN = "galaxytab3";

    /** Wizard shown to establish preferences */
    private ConnectivityWizardFragment wizard;

    /** Fragment with example application */
    private ExampleFragment example;

    /** SiteWhere tenant */
    private String tenant;

    /** Message client for sending events to SiteWhere */
    private SiteWhereMessageClient messageClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageClient = new SiteWhereMessageClient(getApplicationContext());

        // Verify that SiteWhere API location has been specified.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String apiUrl = prefs.getString(IConnectivityPreferences.PREF_SITEWHERE_API_URI, null);
        this.tenant = prefs.getString(IConnectivityPreferences.PREF_SITEWHERE_API_TENANT, null);

        // Push current device id into MQTT settings, then get current values.
        MqttServicePreferences updated = new MqttServicePreferences();
        updated.setDeviceToken(messageClient.getUniqueDeviceId());
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
     * Show the settings screen
     */
    private void  initSettinsScreen(){

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
            String topic = buildSiteWhereTopic();
            messageClient.registerForEvents(topic);

            String deviceToken = messageClient.getUniqueDeviceId();
            String originator = null;
            String areaToken = DEFAULT_AREA_TOKEN;
            String customerToken = DEFAULT_CUSTOMER_TOKEN;
            String deviceTypeToken = DEFAULT_DEVICE_TYPE_TOKEN;

            // This registers device with a specific site.
            messageClient.sendDeviceRegistration(deviceToken, originator, areaToken, customerToken, deviceTypeToken);
        } catch (SiteWhereMessagingException e) {
            Log.e(TAG, "Unable to send device registration to SiteWhere.", e);
        }
    }

    private String buildSiteWhereTopic() {
        StringBuilder builder = new StringBuilder();
        builder.append("SiteWhere/");
        builder.append(tenant);
        builder.append("input/protobuf");
        return builder.toString();
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
            Header header = Header.parseDelimitedFrom(stream);
            switch (header.getCommand()) {
                case REGISTRATION_ACK: {
                    RegistrationAck ack = RegistrationAck.parseDelimitedFrom(stream);
                    handleRegistrationAck(header, ack);
                    break;
                }
                case DEVICE_STREAM_ACK: {
                    DeviceStreamAck ack = DeviceStreamAck.parseDelimitedFrom(stream);
                    handleDeviceStreamAck(header, ack);
                    break;
                }
                case RECEIVE_DEVICE_STREAM_DATA: {
//                    // TODO
//                    Sitewhere.Model.DeviceStreamData chunk = Sitewhere.Model.DeviceStreamData.parseDelimitedFrom(stream);
//                    handleReceivedDeviceStreamData(header, chunk);
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to process system command.", e);
        }
    }

    /*
     * (non-Javadoc)
     * //TODO Check this code
     * @see SiteWhereMessageClientCallback
     */
    @Override
    public void onReceivedEventMessage(String topic, byte[] payload) {
        Log.d(TAG, "Received event message " + new String(payload));
        try {
            JsonNode eventNode = new ObjectMapper().readTree(payload);
            String eventType = eventNode.get("eventType").textValue();
            if ("Measurements".equals(eventType)) {
                DeviceMeasurement dm = new ObjectMapper().treeToValue(eventNode, DeviceMeasurement.class);
                StringBuilder sb = new StringBuilder();
                sb.append(dm.getName()).append(": ").append(dm.getValue());
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

//    public void handleReceivedDeviceStreamData(Header header, Sitewhere.Model.DeviceStreamData data) {
//    }

    /*
     * (non-Javadoc)
     *
     * @see SiteWhereMessageClientCallback
     */
    public void onReceivedCustomCommand(byte[] payload) {
        Log.d(TAG, "Received custom command.");
        ByteArrayInputStream stream = new ByteArrayInputStream(payload);
        try {
            Android.Spec_galaxytab3._Header header = Android.Spec_galaxytab3._Header.parseDelimitedFrom(stream);
            switch (header.getCommand()) {
                case CHANGEBACKGROUND: {
                    Android.Spec_galaxytab3.changeBackground cb = Android.Spec_galaxytab3.changeBackground.parseDelimitedFrom(stream);
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
            }
        } catch (IOException e) {
            Log.e(TAG, "IO exception processing custom command.", e);
        } catch (SiteWhereMessagingException e) {
            Log.e(TAG, "Messaging exception processing custom command.", e);
        }
    }

}
