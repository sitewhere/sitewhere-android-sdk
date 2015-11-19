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
package com.sitewhere.androidsdk.mqtt;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings.Secure;
import android.util.Log;

import com.sitewhere.androidsdk.messaging.IFromSiteWhere;
import com.sitewhere.androidsdk.messaging.ISiteWhereMessaging;
import com.sitewhere.androidsdk.messaging.IToSiteWhere;
import com.sitewhere.androidsdk.mqtt.preferences.IMqttServicePreferences;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Tracer;
import org.fusesource.mqtt.codec.MQTTFrame;

import java.net.URISyntaxException;
import java.util.Formatter;
import java.util.Locale;

/**
 * Service that provides MQTT connectivity to external apps.
 * 
 * @author Derek
 */
public class MqttService extends Service {

	/** Log tag */
	public static final String TAG = "MQTTService";

	/** Application package prefix */
	public static final String APP_ID = "com.sitewhere.mqtt";

	/** MQTT client connection state */
	public MqttConnectionState connectionState = MqttConnectionState.Disconnected;

	/** MQTT client */
	private MQTT mqtt;

	/** MQTT connection */
	private BlockingConnection connection;

	/** Manages interactions with MQTT pub/sub */
	private IMqttInteractionManager mqttManager;

	/** Manages client registration and notification */
	private RegistrationManager registrationManager;

	/** MQTT configuration */
	private IMqttServicePreferences configuration;

	/** Network availability monitor */
	private NetworkMonitor networkMonitor;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		// Reset connection state.
		connectionState = MqttConnectionState.Disconnected;

		// Start up management entities.
		mqttManager = new DefaultMqttInteractionManager();
		registrationManager = new RegistrationManager();
		mqttManager.setCallback(registrationManager);

		prepareMqtt();
		startMonitoringNetwork();
	}

	/**
	 * Configures the MQTT top-level settings. No connection is created until the service is
	 * requested.
	 */
	protected void prepareMqtt() {
		this.mqtt = new MQTT();
		mqtt.setConnectAttemptsMax(1);
		mqtt.setReconnectAttemptsMax(1);
		mqtt.setKeepAlive((short) 300);
		mqtt.setTracer(new Tracer() {

			@Override
			public void debug(String message, Object... args) {
				super.debug(message, args);
				StringBuilder sb = new StringBuilder();
				Formatter formatter = new Formatter(sb, Locale.US);
				try {
					Log.d(TAG, formatter.format(message, args).toString());
				} finally {
					formatter.close();
				}
			}

			@Override
			public void onReceive(MQTTFrame frame) {
				super.onReceive(frame);
				Log.d(TAG, frame.toString());
			}

			@Override
			public void onSend(MQTTFrame frame) {
				super.onSend(frame);
				Log.d(TAG, frame.toString());
			}
		});
	}

	/**
	 * Starts listening for network broadcasts to so we can shut down or start back up
	 * automatically.
	 */
	protected void startMonitoringNetwork() {
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		networkMonitor = new NetworkMonitor();
		this.registerReceiver(networkMonitor, filter);
		Log.d(TAG, "Now monitoring network state.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		stopMonitoringNetwork();
		disconnect();
	}

	/**
	 * Stop monitoring network state.
	 */
	protected void stopMonitoringNetwork() {
		if (networkMonitor != null) {
			this.unregisterReceiver(networkMonitor);
			Log.d(TAG, "No longer monitoring network state.");
		}
	}

	/**
	 * Reconnect to the MQTT broker.
	 */
	protected void reconnect() {
		if (!hasBeenConfigured()) {
			Log.d(TAG, "Reconnect called without client having configured settings. Ignoring.");
			return;
		}
		if (connectionState == MqttConnectionState.Connecting) {
			Log.d(TAG, "Connection request ignored. Already in process of connecting...");
			return;
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				Log.d(TAG, "Connecting to MQTT...");
				connectionState = MqttConnectionState.Connecting;

				disconnect();

				try {
					mqtt.setHost(configuration.getBrokerHostname(), configuration.getBrokerPort());
					connection = mqtt.blockingConnection();
					connection.connect();
					Log.d(TAG, "Connected to MQTT.");
					mqttManager.connect(configuration.getDeviceHardwareId(), connection);
					registrationManager.connected();
					connectionState = MqttConnectionState.Connected;
				} catch (URISyntaxException e) {
					Log.d(TAG, "Error setting MQTT host.", e);
					connectionState = MqttConnectionState.Disconnected;
				} catch (Exception e) {
					Log.d(TAG, "Error connecting to MQTT host.", e);
					connectionState = MqttConnectionState.Disconnected;
				}
			}
		}, TAG).start();
	}

	/**
	 * Disconnect from the MQTT broker.
	 */
	protected void disconnect() {
		if (isMqttConnected()) {
			try {
				Log.d(TAG, "Disconnecting from MQTT...");
				mqttManager.disconnect(configuration.getDeviceHardwareId(), connection);
				connection.disconnect();
				connection = null;
				connectionState = MqttConnectionState.Disconnected;
				registrationManager.disconnected();
			} catch (Exception e) {
				Log.d(TAG, "Error disconnecting from MQTT.", e);
			}
		}
	}

	/**
	 * Indicates if there is an active connection to the MQTT broker.
	 * 
	 * @return
	 */
	protected boolean isMqttConnected() {
		return ((connection != null) && (connection.isConnected()));
	}

	/**
	 * Indicates if a client has already sent in a configuration that can be used to start the
	 * service.
	 * 
	 * @return
	 */
	protected boolean hasBeenConfigured() {
		return (configuration != null);
	}

	/**
	 * Gets the unique id for a device.
	 * 
	 * @return
	 */
	protected String getUniqueDeviceId() {
		String id = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
		if (id == null) {
			throw new RuntimeException(
					"Running in context that does not have a unique id. Override getUniqueDeviceId() in subclass.");
		}
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStart(android.content.Intent, int)
	 */
	@Override
	public void onStart(final Intent intent, final int startId) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				handleStart(intent, startId);
			}
		}, TAG).start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(final Intent intent, int flags, final int startId) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				handleStart(intent, startId);
			}
		}, TAG).start();

		return Service.START_REDELIVER_INTENT;
	}

	/**
	 * Common handler for start request from application.
	 * 
	 * @param intent
	 * @param startId
	 */
	protected synchronized void handleStart(Intent intent, int startId) {
		IMqttServicePreferences inConfig = intent
				.getParcelableExtra(ISiteWhereMessaging.EXTRA_CONFIGURATION);
		if (inConfig == null) {
			return;
		}

		// Store hardware id for later use.
		boolean needsReconnect = true;
		if (this.configuration == null) {
			this.configuration = inConfig;
			Log.d(TAG,
					"Starting MQTT service for: host->" + configuration.getBrokerHostname()
							+ " port->" + configuration.getBrokerPort() + " hardwareId->"
							+ configuration.getDeviceHardwareId());
		} else if (!inConfig.equals(this.configuration)) {
			this.configuration = inConfig;
			Log.d(TAG,
					"Settings changed. Will reconnect with settings: host->"
							+ configuration.getBrokerHostname() + " port->"
							+ configuration.getBrokerPort() + " hardwareId->"
							+ configuration.getDeviceHardwareId());
		} else {
			needsReconnect = false;
		}

		if ((!isMqttConnected()) || (needsReconnect)) {
			if (isOnline()) {
				reconnect();
			} else {
				// Indicate that we are waiting for the network.
				connectionState = MqttConnectionState.WaitingForNetwork;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "Service received bind request...");
		return sitewhere.asBinder();
	}

	/** Implementation of {@link IToSiteWhere} service interface */
	private final IToSiteWhere.Stub sitewhere = new IToSiteWhere.Stub() {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sitewhere.android.messaging.IToSiteWhere#register(com.sitewhere.android.messaging
		 * .IFromSiteWhere)
		 */
		@Override
		public void register(IFromSiteWhere client) throws RemoteException {
			Log.d(TAG, "Sending register request...");
			registrationManager.addClient(client);

			// If we were already connected, make sure the client knows.
			if (isMqttConnected()) {
				registrationManager.connected();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.sitewhere.android.messaging.IToSiteWhere#send(byte[])
		 */
		@Override
		public void send(byte[] payload) throws RemoteException {
			try {
				mqttManager.send(payload);
			} catch (SiteWhereMqttException e) {
				Log.e(TAG, "Error sending message.", e);
				throw new RemoteException();
			}
		}

        @Override
        public void registerForEvents(String topic) throws RemoteException {
            try {
                mqttManager.subscribe(topic);
            } catch (SiteWhereMqttException e) {
                Log.e(TAG, "Error subscribing to topic " + topic, e);
                throw new RemoteException();
            }
        }

        /*
                 * (non-Javadoc)
                 *
                 * @see
                 * com.sitewhere.android.messaging.IToSiteWhere#unregister(com.sitewhere.android.messaging
                 * .IFromSiteWhere)
                 */
		@Override
		public void unregister(IFromSiteWhere client) throws RemoteException {
			Log.d(TAG, "Sending unregister request...");
			registrationManager.removeClient(client);
		}
	};

	/**
	 * Indicates whether the device is online.
	 * 
	 * @return
	 */
	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if ((cm.getActiveNetworkInfo() != null) && (cm.getActiveNetworkInfo().isAvailable())
				&& (cm.getActiveNetworkInfo().isConnected())) {
			return true;
		}
		return false;
	}

	/**
	 * Monitors broadcasts related to the network coming up or down.
	 * 
	 * @author Derek
	 */
	public class NetworkMonitor extends BroadcastReceiver {

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
		 * android.content.Intent)
		 */
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "Network status change detected.");
			if (!isOnline()) {
				if (isMqttConnected()) {
					disconnect();
					connectionState = MqttConnectionState.WaitingForNetwork;
				}
			} else {
				if ((!isMqttConnected()) && (hasBeenConfigured())) {
					reconnect();
				}
			}
		}
	}
}