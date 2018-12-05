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
package com.sitewhere.androidsdk.preferences;

/**
 * Provides constants for connectivity preferences.
 * 
 * @author Derek
 */
public interface IConnectivityPreferences {

	/** Preference for base URI used to access SiteWhere APIs */
	String PREF_SITEWHERE_API_URI = "sw_api_uri";

	/** Preference tenant used to access SiteWhere APIs */
	String PREF_SITEWHERE_API_TENANT = "sw_api_tenant";

	/** Preference for username used to access SiteWhere APIs */
    String PREF_SITEWHERE_API_USERNAME = "sw_api_username";

	/** Preference for password used to access SiteWhere APIs */
	String PREF_SITEWHERE_API_PASSWORD = "sw_api_password";

    /** Preference for protocol used to access SiteWhere APIs */
    String PREF_SITEWHERE_API_SCHEMA = "sw_api_protocol";

	/** Preference for hostname used to access SiteWhere APIs */
    String PREF_SITEWHERE_API_HOSTNAME = "sw_api_hostname";

    /** Preference for port used to access SiteWhere APIs */
    String PREF_SITEWHERE_API_PORT = "sw_api_port";

    /** Preference tenant to access SiteWhere APIs */
    String PREF_SITEWHERE_TENANT = "sw_tenant";

    /** Preference tenant auth to access SiteWhere APIs */
    String PREF_SITEWHERE_TENANT_AUTH = "sw_tenant_auth";
}