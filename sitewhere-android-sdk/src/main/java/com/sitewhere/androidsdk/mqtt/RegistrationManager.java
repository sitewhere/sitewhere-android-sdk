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

import android.os.RemoteException;
import android.util.Log;

import com.sitewhere.androidsdk.messaging.IFromSiteWhere;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages list of registered clients and sends commands to them.
 * 
 * @author Derek
 */
public class RegistrationManager implements IMqttCallback {

	/** List of clients interested in data from SiteWhere */
	private List<IFromSiteWhere> clients = new ArrayList<IFromSiteWhere>();

	/**
	 * Add a new client to the list.
	 * 
	 * @param client
	 */
	public void addClient(IFromSiteWhere client) {
		if (!clients.contains(client)) {
			Log.d(MqttService.TAG, "Registration manager adding client.");
			clients.add(client);
		}
	}

	/**
	 * Remove an existing client from the list.
	 * 
	 * @param client
	 */
	public void removeClient(IFromSiteWhere client) {
		Log.d(MqttService.TAG, "Registration manager removing client.");
		clients.remove(client);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.android.mqtt.IMqttCallback#connected()
	 */
	@Override
	public void connected() {
		List<IFromSiteWhere> unreachable = new ArrayList<IFromSiteWhere>();
		for (IFromSiteWhere client : clients) {
			try {
				client.connected();
			} catch (RemoteException e) {
				Log.e(MqttService.TAG, "Unable to send message to client. Removing from list.", e);
				unreachable.add(client);
			}
		}
		for (IFromSiteWhere client : unreachable) {
			clients.remove(client);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.android.mqtt.IMqttCallback#onSystemCommandReceived(java.lang.String, byte[])
	 */
	@Override
	public void onSystemCommandReceived(String topic, byte[] payload) {
		Log.d(MqttService.TAG, "Notifying clients system command was received.");
		List<IFromSiteWhere> unreachable = new ArrayList<IFromSiteWhere>();
		for (IFromSiteWhere client : clients) {
			try {
				client.receivedSystemCommand(payload);
			} catch (RemoteException e) {
				Log.w(MqttService.TAG, "Unable to send message to client. Removing from list.", e);
				unreachable.add(client);
			}
		}
		for (IFromSiteWhere client : unreachable) {
			clients.remove(client);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.android.mqtt.IMqttCallback#onCustomCommandReceived(java.lang.String, byte[])
	 */
	@Override
	public void onCustomCommandReceived(String topic, byte[] payload) {
		Log.d(MqttService.TAG, "Notifying clients custom command was received.");
		List<IFromSiteWhere> unreachable = new ArrayList<IFromSiteWhere>();
		for (IFromSiteWhere client : clients) {
			try {
				client.receivedCustomCommand(payload);
			} catch (RemoteException e) {
				Log.w(MqttService.TAG, "Unable to send message to client. Removing from list.", e);
				unreachable.add(client);
			}
		}
		for (IFromSiteWhere client : unreachable) {
			clients.remove(client);
		}
	}

    @Override
    public void onEventMessageReceived(String topic, byte[] payload) {
        Log.d(MqttService.TAG, "Notifying clients event message was received.");
        List<IFromSiteWhere> unreachable = new ArrayList<IFromSiteWhere>();
        for (IFromSiteWhere client : clients) {
            try {
                client.receivedEventMessage(topic, payload);
            } catch (RemoteException e) {
                Log.w(MqttService.TAG, "Unable to send message to client. Removing from list.", e);
                unreachable.add(client);
            }
        }
        for (IFromSiteWhere client : unreachable) {
            clients.remove(client);
        }
    }

    /*
         * (non-Javadoc)
         *
         * @see com.sitewhere.android.mqtt.IMqttCallback#disconnected()
         */
	@Override
	public void disconnected() {
		List<IFromSiteWhere> unreachable = new ArrayList<IFromSiteWhere>();
		for (IFromSiteWhere client : clients) {
			try {
				client.disconnected();
			} catch (RemoteException e) {
				Log.w(MqttService.TAG, "Unable to send message to client. Removing from list.", e);
				unreachable.add(client);
			}
		}
		for (IFromSiteWhere client : unreachable) {
			clients.remove(client);
		}
	}
}