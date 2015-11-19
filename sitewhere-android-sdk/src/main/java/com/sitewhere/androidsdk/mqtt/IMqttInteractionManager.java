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

import org.fusesource.mqtt.client.BlockingConnection;

/**
 * Allows interaction with MQTT implementation to be customized.
 * 
 * @author Derek
 */
public interface IMqttInteractionManager {

	/**
	 * Handle topic-related connect logic.
	 * 
	 * @param hardwareId
	 * @param connection
	 * @throws SiteWhereMqttException
	 */
	public void connect(String hardwareId, BlockingConnection connection)
			throws SiteWhereMqttException;

	/**
	 * Send a message payload to SiteWhere.
	 * 
	 * @param payload
	 * @throws SiteWhereMqttException
	 */
	public void send(byte[] payload) throws SiteWhereMqttException;

    /**
     * Subscribes to a SiteWhere event message topic
     * @param topic
     * @throws SiteWhereMqttException
     */
	public void subscribe(String topic) throws SiteWhereMqttException;

	/**
	 * Handle topic-related disconnect logic.
	 * 
	 * @param hardwareId
	 * @param connection
	 * @throws SiteWhereMqttException
	 */
	public void disconnect(String hardwareId, BlockingConnection connection)
			throws SiteWhereMqttException;

	/**
	 * Set callback for MQTT message notifications.
	 * 
	 * @param callback
	 */
	public void setCallback(IMqttCallback callback);
}