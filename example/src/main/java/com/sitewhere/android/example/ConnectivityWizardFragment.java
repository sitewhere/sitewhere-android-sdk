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

import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.sitewhere.androidsdk.mqtt.preferences.IMqttServicePreferences;
import com.sitewhere.androidsdk.mqtt.preferences.MqttServicePreferences;
import com.sitewhere.androidsdk.preferences.IConnectivityPreferences;
import com.sitewhere.rest.client.SiteWhereClient;
import com.sitewhere.rest.model.system.Version;
import com.sitewhere.spi.ISiteWhereClient;
import com.sitewhere.spi.SiteWhereException;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Fragment that allows the user to choose the host/port of the SiteWhere instance to interact with.
 * 
 * @author Derek
 */
public class ConnectivityWizardFragment extends Fragment {

	/** Tag for logging */
	private static final String TAG = "SiteWhereConnectivity";

	/** Color for error messages */
	private static final String ERROR_COLOR = "#550000";

	/** Color for success messages */
	private static final String SUCCESS_COLOR = "#005500";

    /** SiteWhere default tenant */
    private static final String DEFAULT_SITEWHERE_TENANT = "default";

    /** SiteWhere default tenant auth token*/
	private static final String DEFAULT_SITEWHERE_TENANT_AUTH = "SiteWhere1234567890";

	/** SiteWhere default Username */
    private static final String DEFAULT_SITEWHERE_USERNAME = "admin";

    /** SiteWhere default Password */
    private static final String DEFAULT_SITEWHERE_PASSWORD = "password";

    /** SiteWhere default schema */
    private static final String DEFAULT_SITEWHERE_SCHEMA = "http";

    /** SiteWhere default Hostname */
    private static final String DEFAULT_SITEWHERE_HOSTNAME = "localhost";

    /** SiteWhere default port */
    private static final int DEFAULT_SITEWHERE_PORT = 8080;

    /** SiteWhere default path */
    private static final String DEFAULT_SITEWHERE_PATH = "sitewhere/api/";

    /** SiteWhere default URI */
    private static final String DEFAULT_SITEWHERE_URI;

	static {
        DEFAULT_SITEWHERE_URI = buildURI(DEFAULT_SITEWHERE_SCHEMA,
                DEFAULT_SITEWHERE_HOSTNAME,
                DEFAULT_SITEWHERE_PORT,
                DEFAULT_SITEWHERE_PATH);
    }

    /** Swith that holds if use the default tenant */
    private Switch useDefaultTenant;

    /** Text field that holds tenant */
    private EditText tenant;

    /** Layout tenant name */
    private LinearLayout tenantGroup;

	/** Text field that holds tenant auth token */
	private EditText tenantAuth;

    /** Layout tenant name */
    private LinearLayout tenantAuthGroup;

    /** Text field that holds Username */
	private EditText username;

	/** Text field that holds Password */
	private EditText password;

	/** Text field that holds Hostanme */
	private EditText hostname;

	/** Text field that holds PortNumber */
	private EditText portNumber;

	/** Switch field that holds the HTTP(S) */
	private Switch useHttps;

	/** Button for verifying API */
	private Button apiVerifyButton;

	/** Layout for verification info */
	private LinearLayout apiVerifyGroup;

	/** Message indicating we are verifying API URL */
	private TextView apiVerifyMessage;

	/** Check mark indicating server verified */
	private ImageView apiVerifyCheck;

	/** Spinner progress for API verify */
	private ProgressBar apiVerifyProgress;

	/** Layout for MQTT hostname info */
	private View mqttDivider;

	/** Layout for MQTT hostname info */
	private LinearLayout mqttHostGroup;

	/** MQTT title */
	private TextView mqttTitle;

	/** MQTT label */
	private TextView mqttLabel;

	/** Text field that MQTT Port Number */
	private EditText mqttPortNumber;

	/** Button for verifying MQTT */
	private Button mqttVerifyButton;

	/** Layout for MQTT verification info */
	private LinearLayout mqttVerifyGroup;

	/** Message indicating we are verifying MQTT URL */
	private TextView mqttVerifyMessage;

	/** Check mark indicating MQTT verified */
	private ImageView mqttVerifyCheck;

	/** Spinner progress for MQTT verify */
	private ProgressBar mqttVerifyProgress;

	/** Button that passes control to main application */
	private Button wizardComplete;

	/** Used to handle thread execution */
	private ExecutorService executor = Executors.newSingleThreadExecutor();

	/** Listens for wizard to complete */
	private IConnectivityWizardListener listener;


	public ConnectivityWizardFragment() {
	}

	public void setWizardListener(IConnectivityWizardListener listener) {
		this.listener = listener;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup,
	 * android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.sitewhere_connectivity, container, false);
		return rootView;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setupApiFields();
		setupMqttFields();

		// Get reference to 'verify' button.
		wizardComplete = (Button) getActivity().findViewById(R.id.sitewhere_wizard_complete);
		wizardComplete.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				onWizardCompleteClicked(v);
			}
		});
	}

	/**
	 * Set up field for verifying API access.
	 */
	protected void setupApiFields() {
		// Get reference to API hostname text field.

		useDefaultTenant = (Switch) getActivity().findViewById(R.id.use_default_tenant);
        useDefaultTenant.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onUseDefaultTenantChanged(buttonView, isChecked);
            }
        });
        tenant = (EditText) getActivity().findViewById(R.id.tenant);
        tenantAuth = (EditText) getActivity().findViewById(R.id.tenant_auth);
        tenantGroup = (LinearLayout) getActivity().findViewById(R.id.tenant_gpr);
        tenantAuthGroup = (LinearLayout) getActivity().findViewById(R.id.tenant_auth_gpr);

		username = (EditText) getActivity().findViewById(R.id.username);
		password = (EditText) getActivity().findViewById(R.id.password);
		hostname = (EditText) getActivity().findViewById(R.id.hostname);
		portNumber = (EditText) getActivity().findViewById(R.id.portNumber);
		useHttps = (Switch) getActivity().findViewById(R.id.https);


        // Load URI from preferences if available.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

		String prefSchema = prefs.getString(IConnectivityPreferences.PREF_SITEWHERE_API_SCHEMA, DEFAULT_SITEWHERE_SCHEMA);
		String prefHost = prefs.getString(IConnectivityPreferences.PREF_SITEWHERE_API_HOSTNAME, DEFAULT_SITEWHERE_HOSTNAME);
		int prefPort = prefs.getInt(IConnectivityPreferences.PREF_SITEWHERE_API_PORT, DEFAULT_SITEWHERE_PORT);
		String prefUsername = prefs.getString(IConnectivityPreferences.PREF_SITEWHERE_API_USERNAME, DEFAULT_SITEWHERE_USERNAME);
		String prefPassword = prefs.getString(IConnectivityPreferences.PREF_SITEWHERE_API_PASSWORD, DEFAULT_SITEWHERE_PASSWORD);
		String prefTenant = prefs.getString(IConnectivityPreferences.PREF_SITEWHERE_TENANT, DEFAULT_SITEWHERE_TENANT);
		String prefTenantAuth = prefs.getString(IConnectivityPreferences.PREF_SITEWHERE_TENANT_AUTH, DEFAULT_SITEWHERE_TENANT_AUTH);

		//String prefApiUri = prefs.getString(IConnectivityPreferences.PREF_SITEWHERE_API_URI, DEFAULT_SITEWHERE_URI);

		String prefApiUri = buildURI(prefSchema, prefHost, prefPort, DEFAULT_SITEWHERE_PATH);

		if (prefApiUri != null) {
			try {
				URI uri = new URI(prefApiUri);

                useHttps.setChecked("https".equalsIgnoreCase(uri.getScheme()));
				hostname.setText(uri.getHost());
                portNumber.setText(String.valueOf(uri.getPort()));
                username.setText(prefUsername);
                password.setText(prefPassword);
				tenant.setText(prefTenant);
				tenantAuth.setText(prefTenantAuth);
			} catch (URISyntaxException e) {
			}
		}


		// Get reference to 'verify' button.
		apiVerifyButton = (Button) getActivity().findViewById(R.id.sitewhere_api_submit);
		apiVerifyButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				onHostVerifyButtonClicked(v);
			}
		});

		// Get reference API verify group.
		apiVerifyGroup = (LinearLayout) getActivity().findViewById(R.id.sitewhere_api_verify_grp);

		// Get reference 'verify message' text view.
		apiVerifyMessage = (TextView) getActivity().findViewById(R.id.sitewhere_api_verify);

		// Get reference to check indicator for API verify.
		apiVerifyCheck = (ImageView) getActivity().findViewById(
				R.id.sitewhere_host_api_verify_check);

		// Get reference to progress indicator for API verify.
		apiVerifyProgress = (ProgressBar) getActivity().findViewById(
				R.id.sitewhere_api_verify_progress);
	}

    /**
     * Called when Use Default Tenant changes.
     * @param buttonView
     * @param isChecked
     */
    private void onUseDefaultTenantChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked) {
            this.tenantGroup.setVisibility(View.GONE);
            this.tenantAuthGroup.setVisibility(View.GONE);
        } else {
            this.tenantGroup.setVisibility(View.VISIBLE);
            this.tenantAuthGroup.setVisibility(View.VISIBLE);
        }
    }

    /**
	 * Set up field for verifying API access.
	 */
	protected void setupMqttFields() {
		// Get reference MQTT title.
		mqttDivider = (View) getActivity().findViewById(R.id.sitewhere_mqtt_divider);

		// Get reference MQTT title.
		mqttTitle = (TextView) getActivity().findViewById(R.id.sitewhere_mqtt_title);

		// Get reference MQTT title.
		mqttLabel = (TextView) getActivity().findViewById(R.id.sitewhere_mqtt_label);

		// Get reference MQTT host group.
		mqttHostGroup = (LinearLayout) getActivity().findViewById(R.id.sitewhere_mqtt_host_grp);

		// Get reference to MQTT host text field.
		mqttPortNumber = (EditText) getActivity().findViewById(R.id.sitewhere_mqtt_port);

		// Load URI from preferences if available.
		IMqttServicePreferences prefs = MqttServicePreferences.read(getActivity());
//		String prefMqttUri = prefs.getBrokerHostname() + ":" + prefs.getBrokerPort();

		mqttPortNumber.setText(String.valueOf(prefs.getBrokerPort()));

		// Get reference to 'verify' button.
		mqttVerifyButton = (Button) getActivity().findViewById(R.id.sitewhere_mqtt_submit);
		mqttVerifyButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				onMqttVerifyButtonClicked(v);
			}
		});

		// Get reference API verify group.
		mqttVerifyGroup = (LinearLayout) getActivity().findViewById(R.id.sitewhere_mqtt_verify_grp);

		// Get reference 'verify message' text view.
		mqttVerifyMessage = (TextView) getActivity().findViewById(R.id.sitewhere_mqtt_verify);

		// Get reference to check indicator for MQTT verify.
		mqttVerifyCheck = (ImageView) getActivity().findViewById(R.id.sitewhere_mqtt_verify_check);

		// Get reference to progress indicator for MQTT verify.
		mqttVerifyProgress = (ProgressBar) getActivity().findViewById(
				R.id.sitewhere_mqtt_verify_progress);
	}

	/**
	 * Verifies connection by creating the API URL and testing a call via the client.
	 * 
	 * @param v
	 */
	protected void onHostVerifyButtonClicked(View v) {
		if (!NetworkUtils.isOnline(getActivity())) {
			InterfaceUtils.showAlert(getActivity(), R.string.sitewhere_no_network_message,
					R.string.sitewhere_no_network_title);
			return;
		}

		// Disable button until processing is complete.
		apiVerifyButton.setEnabled(false);

		String apiUri = buildSiteWhereAPIUri();
		if (apiUri.length() == 0) {
            useHttps.setError("Select the schema for the remote SiteWhere server.");
		    hostname.setError("Enter the hostname for the remote SiteWhere server.");
		    portNumber.setError("Enter the port number for the remote SiteWhere server.");
            username.setError("Enter the username for the remote SiteWhere server.");
            password.setError("Enter the password for the remote SiteWhere server.");
			apiVerifyGroup.setVisibility(View.GONE);
			return;
		}

		apiVerifyMessage.setTextColor(Color.parseColor(SUCCESS_COLOR));
		apiVerifyMessage.setText("Verifying SiteWhere API available for URI: '" + apiUri + "' ...");

		// Make the group visible.
		apiVerifyGroup.setVisibility(View.VISIBLE);
		apiVerifyProgress.setVisibility(View.VISIBLE);

		String sitewhereUsername = username.getText().toString();
		String sitewherePassword = password.getText().toString();

		Future<ISiteWhereClient> clienteFuture =
                executor.submit(
		            new SiteWhereClientConnector(getAPISchema(),
                        getAPIHostname(),
                        getAPIPort(),
                        sitewhereUsername,
                        sitewherePassword));

		ISiteWhereClient client = null;

		try {
			client = clienteFuture.get();
			Log.i(TAG, "Client acquired!");
		} catch (ExecutionException e) {
			Log.e(TAG, e.getMessage());
            e.printStackTrace();
		} catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }

        if (client != null) {
            executor.submit(new HostVerifier(client));
        } else {
		    handleVerifyError("Invalid Authentication", null);
        }
    }

	private String buildSiteWhereAPIUri() {
	    return buildURI(getAPISchema(), getAPIHostname(), getAPIPort(), DEFAULT_SITEWHERE_PATH);
    }

    private static String buildURI(String schema, String hostname, int port) {
	    return buildURI(schema, hostname, port, null);
    }

    private static String buildURI(String schema, String hostname, int port, String path) {
        StringBuilder builder = new StringBuilder();

        builder.append(schema);
        builder.append("://");
        builder.append(hostname);

        if (80 != port) {
            builder.append(":");
            builder.append(port);
        }
        if (path != null && !path.isEmpty()){
            builder.append("/");
            builder.append(path);
        }
        return builder.toString();
    }

    private String getAPISchema() {
	    return this.useHttps.isChecked() ? "https":"http";
    }

    private String getAPIHostname() {
        if (this.hostname != null && this.hostname.getText().length() > 0) {
            return this.hostname.getText().toString();
        }
        return DEFAULT_SITEWHERE_HOSTNAME;
    }

    private int getAPIPort() {
        if (this.portNumber != null){
            try {
                return Integer.parseInt(this.portNumber.getText().toString());
            } catch (NumberFormatException nfe) {
                Log.e(TAG, nfe.getMessage());
            }

        }
        return DEFAULT_SITEWHERE_PORT;
    }



    /**
	 * Show error message encountered while verifying SiteWhere server access.
	 * 
	 * @param error
	 * @param t
	 */
	protected void handleVerifyError(String error, Throwable t) {
		apiVerifyButton.setEnabled(true);
		apiVerifyProgress.setVisibility(View.GONE);
		apiVerifyMessage.setTextColor(Color.parseColor(ERROR_COLOR));
		apiVerifyMessage.setText("Server verify failed: " + error);
		Log.e(TAG, error, t);
	}

	/**
	 * Show success message for verifying SiteWhere server access.
	 * 
	 * @param version
	 */
	protected void handleVerifySuccess(Version version) {
		apiVerifyProgress.setVisibility(View.GONE);
		apiVerifyCheck.setVisibility(View.VISIBLE);
		apiVerifyMessage.setTextColor(Color.parseColor(SUCCESS_COLOR));
		apiVerifyMessage.setText("SiteWhere server (version " + version.getVersionIdentifier()
				+ ") verified.");
		Log.d(TAG, "SiteWhere server reported version " + version.getVersionIdentifier() + ".");

		// Update preferences with new value.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		SharedPreferences.Editor editor = prefs.edit();

		String api = buildSiteWhereAPIUri();
		editor.putString(IConnectivityPreferences.PREF_SITEWHERE_API_URI, api);
        editor.putString(IConnectivityPreferences.PREF_SITEWHERE_API_USERNAME, username.getText().toString());
        editor.putString(IConnectivityPreferences.PREF_SITEWHERE_API_PASSWORD, password.getText().toString());
		editor.putString(IConnectivityPreferences.PREF_SITEWHERE_API_TENANT, tenant.getText().toString());

		editor.commit();

		showMqttHostFields();
	}

	/**
	 * Show fields for entering MQTT host.
	 */
	protected void showMqttHostFields() {
		mqttDivider.setVisibility(View.VISIBLE);
		mqttTitle.setVisibility(View.VISIBLE);
		mqttLabel.setVisibility(View.VISIBLE);
		mqttHostGroup.setVisibility(View.VISIBLE);

		mqttPortNumber.setText("1883");
	}

	/**
	 * Verifies MQTT connection by trying to establish a connection.
	 * 
	 * @param v
	 */
	protected void onMqttVerifyButtonClicked(View v) {
		if (!NetworkUtils.isOnline(getActivity())) {
			InterfaceUtils.showAlert(getActivity(),
					R.string.sitewhere_no_network_message,
					R.string.sitewhere_no_network_title);
			return;
		}

		// Disable button until processing is complete.
		mqttVerifyButton.setEnabled(false);

		Integer mqttPort = Integer.parseInt(mqttPortNumber.getText().toString());

		if (mqttPort.intValue() == 0) {
			mqttPortNumber.setError("Enter a value for the remote MQTT broker address.");
			mqttVerifyGroup.setVisibility(View.GONE);
			return;
		}

		// Calculate API URL and create client for testing.
		String api = buildURI("mqtt", getAPIHostname(), mqttPort);

		mqttVerifyMessage.setTextColor(Color.parseColor(SUCCESS_COLOR));
		mqttVerifyMessage.setText("Verifying MQTT broker available for URI: '" + api + "' ...");

		// Make the group visible.
		mqttVerifyGroup.setVisibility(View.VISIBLE);
		mqttVerifyProgress.setVisibility(View.VISIBLE);

		String broker = getAPIHostname();

		if (broker == null) {
			mqttVerifyMessage.setTextColor(Color.parseColor(ERROR_COLOR));
			mqttVerifyMessage.setText("Invalid MQTT broker hostname.");
			return;
		}

		executor.submit(new MqttVerifier(broker, mqttPort));
	}

	/**
	 * Show error message encountered while verifying MQTT broker access.
	 * 
	 * @param error
	 * @param t
	 */
	protected void handleMqttError(String error, Throwable t) {
		mqttVerifyButton.setEnabled(true);
		mqttVerifyProgress.setVisibility(View.GONE);
		mqttVerifyMessage.setTextColor(Color.parseColor(ERROR_COLOR));
		mqttVerifyMessage.setText("MQTT verify failed: " + error);
		Log.e(TAG, error, t);
	}

	/**
	 * Show success message for verifying MQTT broker access.
	 */
	protected void handleMqttSuccess() {
		mqttVerifyProgress.setVisibility(View.GONE);
		mqttVerifyCheck.setVisibility(View.VISIBLE);
		mqttVerifyMessage.setTextColor(Color.parseColor(SUCCESS_COLOR));
		mqttVerifyMessage.setText("MQTT broker connectivity verified.");

		// Update preferences with new values.
        try {
            Integer mqttPort = Integer.parseInt(mqttPortNumber.getText().toString());
            MqttServicePreferences updated = new MqttServicePreferences();
            updated.setBrokerHostname(getAPIHostname());
            updated.setBrokerPort(mqttPort);
            MqttServicePreferences.update(updated, getActivity());
            // Make "finish" button visible.
            wizardComplete.setVisibility(View.VISIBLE);
        }catch (NumberFormatException nfe) {
            Log.e(TAG, nfe.getMessage());
        }
	}

	/**
	 * Called when button for ending wizard is pressed.
	 * 
	 * @param view
	 */
	protected void onWizardCompleteClicked(View view) {
		if (listener != null) {
			listener.onWizardComplete();
		} else {
			wizardComplete.setText("Nowhere to go!");
			wizardComplete.setBackgroundColor(Color.parseColor("#cc0000"));
		}
	}

	/**
	 * Thread that verifies that the SiteWhere server is available.
	 * 
	 * @author Derek
	 */
	private class HostVerifier implements Runnable {

		private ISiteWhereClient client;

		public HostVerifier(ISiteWhereClient client) {
			this.client = client;
		}

		@Override
		public void run() {
			try {
				final Version version = client.getSiteWhereVersion();
				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						handleVerifySuccess(version);
					}
				});
			} catch (final SiteWhereException e) {
				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						handleVerifyError("SiteWhere version call failed.", e);
					}
				});
			} catch (final RuntimeException e) {
				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						handleVerifyError("Unable to connect to server.", e.getCause());
					}
				});
			} catch (final Throwable t) {
				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						handleVerifyError("Unexpected exception.", t);
					}
				});
			}
		}
	}

	/**
	 * Thread that verifies that the MQTT broker is available.
	 * 
	 * @author Derek
	 */
	private class MqttVerifier implements Runnable {

		/** MQTT broker hostname */
		private String hostname;

		/** MQTT broker port */
		private int port;

		public MqttVerifier(String hostname, int port) {
			this.hostname = hostname;
			this.port = port;
		}

		@Override
		public void run() {
			try {
				MQTT mqtt = new MQTT();
				mqtt.setConnectAttemptsMax(1);
				mqtt.setReconnectAttemptsMax(0);
				mqtt.setKeepAlive((short) 300);
				Log.d(TAG, "Connecting to MQTT...");

				mqtt.setHost(this.hostname, this.port);
				BlockingConnection connection = mqtt.blockingConnection();
				connection.connect();
				connection.disconnect();
				Log.d(TAG, "Connected to MQTT.");

				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						handleMqttSuccess();
					}
				});
			} catch (final URISyntaxException e) {
				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						handleMqttError("MQTT broker hostname invalid.", e);
					}
				});
			} catch (final Throwable t) {
				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						handleMqttError("Unable to connect to MQTT broker.", t);
					}
				});
			}
		}
	}

	/**
	 * Creates a <code>ISiteWhereClient</code>.
	 */
	private class SiteWhereClientConnector implements Callable<ISiteWhereClient> {

		private final String schema;

        private final String hostname;

		private final int port;

		private final String username;

		private final String password;

        public SiteWhereClientConnector(String schema, String hostname, int port, String username, String password) {
            this.schema = schema;
            this.hostname = hostname;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        @Override
		public ISiteWhereClient call() throws Exception {
			ISiteWhereClient client = null;
			try {
				client = SiteWhereClient.newBuilder()
						.withConnectionTo(this.schema, this.hostname, this.port)
						.forUser(this.username, this.password)
						.build();
				client.initialize();

				return client;
			} catch (SiteWhereException e) {
				throw e;
			}
		}
	}
}