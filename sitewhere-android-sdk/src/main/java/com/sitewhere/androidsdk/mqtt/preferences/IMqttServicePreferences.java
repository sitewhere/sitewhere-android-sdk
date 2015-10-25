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
package com.sitewhere.androidsdk.mqtt.preferences;

import android.os.Parcelable;

/**
 * Provides constants for connectivity preferences.
 * 
 * @author Derek
 */
public interface IMqttServicePreferences extends Parcelable {

	/** Preference for MQTT broker hostname */
	public static final String PREF_SITEWHERE_MQTT_BROKER_HOSTNAME = "mqtt_hostname";

	/** Preference for MQTT broker port */
	public static final String PREF_SITEWHERE_MQTT_BROKER_PORT = "mqtt_port";

	/** Preference for device hardware id */
	public static final String PREF_SITEWHERE_DEVICE_HARDWARE_ID = "device_hardware_id";

	/**
	 * Get MQTT broker host name.
	 * 
	 * @return
	 */
	public String getBrokerHostname();

	/**
	 * Get MQTT broker port.
	 * 
	 * @return
	 */
	public Integer getBrokerPort();

	/**
	 * Get unique hardware id for device.
	 * 
	 * @return
	 */
	public String getDeviceHardwareId();
}