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
package com.sitewhere.android.example;

import android.content.Context;
import android.net.ConnectivityManager;

/**
 * Network utility methods.
 * 
 * @author Derek
 */
public class NetworkUtils {


	/**
	 * Indicates whether the device is online.
	 * 
	 * @return
	 */
	public static boolean isOnline(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if ((cm.getActiveNetworkInfo() != null) && (cm.getActiveNetworkInfo().isAvailable())
				&& (cm.getActiveNetworkInfo().isConnected())) {
			return true;
		}
		return false;
	}
}