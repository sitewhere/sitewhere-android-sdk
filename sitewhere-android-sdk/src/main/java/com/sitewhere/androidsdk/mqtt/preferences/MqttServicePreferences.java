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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

import com.sitewhere.androidsdk.mqtt.MqttService;

/**
 * Wraps actions related to preferences for {@link MqttService}.
 * 
 * @author Derek
 */
public class MqttServicePreferences implements IMqttServicePreferences {

	/** Default MQTT broker port */
	public static final int DEFAULT_BROKER_PORT = 1883;

	/** Default tenant */
	private static final String DEFAULT_TENANT = "default";

	/** MQTT broker host name */
	private String brokerHostname;

	/** MQTT broker port */
	private Integer brokerPort = DEFAULT_BROKER_PORT;

	/** Device token */
	private String deviceToken;

	/** Tenant */
	private String tenant = DEFAULT_TENANT;

	public MqttServicePreferences() {
	}

	public MqttServicePreferences(Parcel parcel) {
		this.brokerHostname = parcel.readString();
		this.brokerPort = parcel.readInt();
		this.deviceToken = parcel.readString();
		this.tenant = parcel.readString();
	}

	/**
	 * Read current values of preferences.
	 * 
	 * @param context
	 * @return
	 */
	public static IMqttServicePreferences read(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return MqttServicePreferences.loadFrom(prefs);
	}

	/**
	 * Update selected preference values. (Fields with null values are not set).
	 * 
	 * @param updated
	 * @param context
	 * @return
	 */
	public static IMqttServicePreferences update(MqttServicePreferences updated, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return MqttServicePreferences.saveTo(updated, prefs);
	}

	/**
	 * Load values from {@link SharedPreferences}.
	 * 
	 * @param prefs
	 * @return
	 */
	protected static MqttServicePreferences loadFrom(SharedPreferences prefs) {
		MqttServicePreferences mqtt = new MqttServicePreferences();
		mqtt.setBrokerHostname(prefs.getString(
				IMqttServicePreferences.PREF_SITEWHERE_MQTT_BROKER_HOSTNAME, null));
		mqtt.setBrokerPort(prefs.getInt(IMqttServicePreferences.PREF_SITEWHERE_MQTT_BROKER_PORT,
				DEFAULT_BROKER_PORT));
		mqtt.setDeviceToken(prefs.getString(
				IMqttServicePreferences.PREF_SITEWHERE_DEVICE_TOKEN, null));
		mqtt.setTenant(prefs.getString(
		        IMqttServicePreferences.PREF_SITEWHERE_TENANT, null));
		return mqtt;
	}

	/**
	 * Save values to {@link SharedPreferences}.
	 * 
	 * @param updated
	 * @param prefs
	 * @return
	 */
	protected static MqttServicePreferences saveTo(MqttServicePreferences updated,
			SharedPreferences prefs) {
		Editor editor = prefs.edit();
		if (updated.getBrokerHostname() != null) {
			editor.putString(IMqttServicePreferences.PREF_SITEWHERE_MQTT_BROKER_HOSTNAME,
					updated.getBrokerHostname());
		}
		if (updated.getBrokerPort() != null) {
			editor.putInt(IMqttServicePreferences.PREF_SITEWHERE_MQTT_BROKER_PORT,
					updated.getBrokerPort());
		}
		if (updated.getDeviceToken() != null) {
			editor.putString(IMqttServicePreferences.PREF_SITEWHERE_DEVICE_TOKEN,
					updated.getDeviceToken());
		}
		if (updated.getTenant() != null) {
			editor.putString(IMqttServicePreferences.PREF_SITEWHERE_TENANT,
					updated.getTenant());
		}

		editor.apply();
		return MqttServicePreferences.loadFrom(prefs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.android.mqtt.preferences.IMqttConnectivityPreferences#getBrokerHostname()
	 */
	public String getBrokerHostname() {
		return brokerHostname;
	}

	public void setBrokerHostname(String brokerHostname) {
		this.brokerHostname = brokerHostname;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.android.mqtt.preferences.IMqttServicePreferences#getBrokerPort()
	 */
	public Integer getBrokerPort() {
		return brokerPort;
	}

	public void setBrokerPort(Integer brokerPort) {
		this.brokerPort = brokerPort;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.android.mqtt.preferences.IMqttServicePreferences#getDeviceHardwareId()
	 */
	public String getDeviceToken() {
		return deviceToken;
	}

	public void setDeviceToken(String deviceToken) {
		this.deviceToken = deviceToken;
	}

	public String getTenant() {
		return this.tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof MqttServicePreferences) {
			MqttServicePreferences other = (MqttServicePreferences) o;
			if ((brokerHostname == null) || (!brokerHostname.equals(other.getBrokerHostname()))) {
				return false;
			}
			if ((brokerPort == null) || (!brokerPort.equals(other.getBrokerPort()))) {
				return false;
			}
			if ((deviceToken == null)
					|| (!deviceToken.equals(other.getDeviceToken()))) {
				return false;
			}
			return true;
		} else {
			return super.equals(o);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.Parcelable#describeContents()
	 */
	@Override
	public int describeContents() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(getBrokerHostname());
		parcel.writeInt(getBrokerPort());
		parcel.writeString(getDeviceToken());
		parcel.writeString(getTenant());
	}

	public static final Parcelable.Creator<MqttServicePreferences> CREATOR = new Parcelable.Creator<MqttServicePreferences>() {
		public MqttServicePreferences createFromParcel(Parcel in) {
			return new MqttServicePreferences(in);
		}

		public MqttServicePreferences[] newArray(int size) {
			return new MqttServicePreferences[size];
		}
	};
}