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

/**
 * Interface that allows en entity to receive callbacks upon arrival of MQTT messages.
 * 
 * @author Derek
 */
public interface IMqttCallback {

	/**
	 * Called when a client is connected to SiteWhere.
	 */
	public void connected();

	/**
	 * Called when a system command is received.
	 * 
	 * @param topic
	 * @param payload
	 */
	public void onSystemCommandReceived(String topic, byte[] payload);

	/**
	 * Called when a custom command is received.
	 * 
	 * @param topic
	 * @param payload
	 */
	public void onCustomCommandReceived(String topic, byte[] payload);

	/**
	 * Called when a event message is received.
	 *
	 * @param topic
	 * @param payload
	 */
	public void onEventMessageReceived(String topic, byte[] payload);

	/**
	 * Called when a client is disconnected from SiteWhere.
	 */
	public void disconnected();
}