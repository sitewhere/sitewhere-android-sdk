package com.sitewhere.androidsdk;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.sitewhere.androidsdk.mqtt.preferences.IMqttServicePreferences;
import com.sitewhere.androidsdk.mqtt.preferences.MqttServicePreferences;

import junit.framework.Assert;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }


    private boolean connected = false;
    private int SLEEP_CONNECTION_TIMEOUT = 10; // in seconds

    public void testOnConnectedToSiteWhere() {


        SiteWhereMessageClient client = new SiteWhereMessageClient(getContext());
        final SiteWhereMessageClient finalClient = client;

        client.setCallback(new SiteWhereMessageClient.SiteWhereMessageClientCallback() {
                               @Override
                               public void onConnectedToSiteWhere() {
                                   connected = true;
                               }

                               @Override
                               public void onReceivedCustomCommand(byte[] payload) {

                               }

                               @Override
                               public void onReceivedSystemCommand(byte[] payload) {

                               }

                                @Override
                                public void onReceivedEventMessage(String topic, byte[] payload) {

                                }

                                @Override
                               public void onDisconnectedFromSiteWhere() {
                                    connected = false;
                               }
                           });

        MqttServicePreferences updated = new MqttServicePreferences();
        updated.setDeviceHardwareId(client.getUniqueDeviceId());
        updated.setBrokerHostname("54.209.235.216");
        updated.setBrokerPort(1883);
        IMqttServicePreferences mqtt = MqttServicePreferences.update(updated, getContext());
        client.connect(updated);

        try {
            Thread.sleep(SLEEP_CONNECTION_TIMEOUT*1000);

            Assert.assertEquals(true, connected);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}