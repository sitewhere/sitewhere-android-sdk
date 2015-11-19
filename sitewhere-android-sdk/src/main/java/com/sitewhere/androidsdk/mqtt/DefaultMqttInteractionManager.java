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

import android.util.Log;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Default implementation of {@link IMqttInteractionManager} that has a single outbound topic sending events
 * to SiteWhere and two inbound topics for system and custom messsages.
 * 
 * @author Derek
 */
public class DefaultMqttInteractionManager implements IMqttInteractionManager {

	/** Topic name for outbound messages */
	private static final String OUTBOUND_TOPIC = "SiteWhere/input/protobuf";

	/** Topic prefix for inbound system messages */
	private static final String SYSTEM_TOPIC_PREFIX = "SiteWhere/system/";

	/** Topic prefix for inbound command messages */
	private static final String COMMAND_TOPIC_PREFIX = "SiteWhere/commands/";

	/** Topic for receiving commands */
	private Topic commandTopic;

	/** Topic for system commands */
	private Topic systemTopic;

	/** Used to notify external listener of received messages */
	private IMqttCallback callback;

	/** MQTT connection */
	private BlockingConnection connection;

	/** Used to handle message processing */
	private ExecutorService executor;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.android.mqtt.IMqttInteractionManager#connect(java.lang.String,
	 * org.fusesource.mqtt.client.BlockingConnection)
	 */
	@Override
	public void connect(String hardwareId, BlockingConnection connection) throws SiteWhereMqttException {
		this.connection = connection;
		if ((executor != null) && (!executor.isShutdown())) {
			executor.shutdownNow();
		}
		executor = Executors.newSingleThreadExecutor();
		executor.submit(new MqttMessageProcessor());
		commandTopic = new Topic(getCommandTopicPrefix() + hardwareId, QoS.EXACTLY_ONCE);
		systemTopic = new Topic(getSystemTopicPrefix() + hardwareId, QoS.EXACTLY_ONCE);
		try {
			Log.d(MqttService.TAG, "System command topic: " + systemTopic.name());
			Log.d(MqttService.TAG, "Custom command topic: " + commandTopic.name());
			connection.subscribe(new Topic[] { commandTopic, systemTopic });
			Log.d(MqttService.TAG, "Subscribed to topics successfully.");
		} catch (Exception e) {
			throw new SiteWhereMqttException("Unable to subscribe to topics.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.android.mqtt.IMqttInteractionManager#send(byte[])
	 */
	@Override
	public void send(byte[] payload) throws SiteWhereMqttException {
		if (connection == null) {
			throw new SiteWhereMqttException("Attempting to send a message while disconnected.");
		}
		try {
			connection.publish(getOutboundTopic(), payload, QoS.EXACTLY_ONCE, false);
			Log.d(MqttService.TAG, "Sent message successfully to: " + getOutboundTopic());
		} catch (Exception e) {
			throw new SiteWhereMqttException("Unable to publish message.", e);
		}
	}

	@Override
	public void subscribe(String topic) throws SiteWhereMqttException {
		if (connection == null) {
            throw new SiteWhereMqttException("Attempting to subscribe to a topic while disconnected.");
        }
        try {
            Topic eventTopic = new Topic(topic, QoS.EXACTLY_ONCE);
            Log.d(MqttService.TAG, "Event topic: " + eventTopic.name());
            connection.subscribe(new Topic[]{eventTopic});
        } catch (Exception e) {
            throw new SiteWhereMqttException("Unable to subscribe to topic " + topic);
        }
	}

	/*
         * (non-Javadoc)
         *
         * @see com.sitewhere.android.mqtt.IMqttInteractionManager#disconnect(java.lang.String,
         * org.fusesource.mqtt.client.BlockingConnection)
         */
	@Override
	public void disconnect(String hardwareId, BlockingConnection connection) throws SiteWhereMqttException {
		try {
			connection.unsubscribe(new String[] { getCommandTopicPrefix() + hardwareId,
					getSystemTopicPrefix() + hardwareId });
			this.connection = null;
			this.executor.shutdownNow();
			Log.d(MqttService.TAG, "Unsubscribed from topics successfully.");
		} catch (Exception e) {
			throw new SiteWhereMqttException("Unable to subscribe to topics.", e);
		}
	}

	/**
	 * Handles message processing in a background thread.
	 * 
	 * @author Derek
	 */
	private class MqttMessageProcessor implements Runnable {

		@Override
		public void run() {
			Log.d(MqttService.TAG, "Started MQTT subscription processing thread.");
			while (true) {
				try {
					Message message = connection.receive();
					message.ack();
					Log.d(MqttService.TAG, "Received message from: " + message.getTopic());
					if (message.getTopic().startsWith(getCommandTopicPrefix())) {
						callback.onCustomCommandReceived(message.getTopic(), message.getPayload());
					} else if (message.getTopic().startsWith(getSystemTopicPrefix())) {
						callback.onSystemCommandReceived(message.getTopic(), message.getPayload());
					} else {
						callback.onEventMessageReceived(message.getTopic(), message.getPayload());
					}
				} catch (InterruptedException e) {
					Log.d(MqttService.TAG, "Device event processor interrupted.");
					break;
				} catch (Throwable e) {
					if (connection.isConnected()) {
						Log.e(MqttService.TAG, "Unhandled MQTT exception.", e);
					} else {
						Log.d(MqttService.TAG, "Ending message processing due to failed connection.");
						break;
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.android.mqtt.IMqttInteractionManager#setCallback(com.sitewhere.android.mqtt
	 * .IMqttCallback)
	 */
	@Override
	public void setCallback(IMqttCallback callback) {
		this.callback = callback;
	}

	/**
	 * Get topic for outbound messages.
	 * 
	 * @return
	 */
	protected String getOutboundTopic() {
		return OUTBOUND_TOPIC;
	}

	/**
	 * Return topic prefix for command messages.
	 * 
	 * @return
	 */
	protected String getCommandTopicPrefix() {
		return COMMAND_TOPIC_PREFIX;
	}

	/**
	 * Return topic prefix for system messages.
	 * 
	 * @return
	 */
	protected String getSystemTopicPrefix() {
		return SYSTEM_TOPIC_PREFIX;
	}
}