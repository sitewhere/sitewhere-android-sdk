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

import android.app.AlertDialog;
import android.content.Context;

/**
 * Utility class for user interface operations.
 * 
 * @author Derek
 */
public class InterfaceUtils {

	/**
	 * Show an alert dialog (Uses codes for message and title).
	 * 
	 * @param context
	 * @param message
	 * @param title
	 */
	public static void showAlert(Context context, int message, int title) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message).setTitle(title);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	/**
	 * Show an alert dialog (Uses strings for message and title).
	 * 
	 * @param context
	 * @param message
	 * @param title
	 */
	public static void showAlert(Context context, String message, String title) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message).setTitle(title);
		AlertDialog dialog = builder.create();
		dialog.show();
	}
}