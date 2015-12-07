package com.sitewhere.androidsdk;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.protobuf.AbstractMessageLite;
import com.sitewhere.androidsdk.messaging.IFromSiteWhere;
import com.sitewhere.androidsdk.messaging.ISiteWhereMessaging;
import com.sitewhere.androidsdk.messaging.IToSiteWhere;
import com.sitewhere.androidsdk.messaging.SiteWhereMessagingException;
import com.sitewhere.androidsdk.mqtt.MqttService;
import com.sitewhere.device.communication.protobuf.proto.Sitewhere;
import com.sitewhere.spi.device.event.IDeviceEventOriginator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;

//import com.sitewhere.spi.device.event.IDeviceEventOriginator;

/**
 * Created by CBICK on 10/9/15.
 */
public class SiteWhereMessageClient {

    private final static String TAG = SiteWhereMessageClient.class.getCanonicalName();

    /**
     * Indicates if bound to service
     */
    protected boolean mBound = false;

    /* Android application context */
    protected Context mContext;

    /**
     * Proxy to send messages
     */
    protected IToSiteWhere mSitewhere;
    protected SiteWhereResponseProcessor mResponseProcessor = new SiteWhereResponseProcessor();
    protected SiteWhereMessageClientCallback mCallback;

    /* Convenient singleton */
    protected static SiteWhereMessageClient sClient;

    /**
     * Handles responses sent from the message service
     */
    protected class SiteWhereResponseProcessor extends IFromSiteWhere.Stub {

        /*
         * (non-Javadoc)
         *
         * @see com.sitewhere.android.messaging.IFromSiteWhere#connected()
         */
        @Override
        public void connected() throws RemoteException {
            if (mCallback != null)
                mCallback.onConnectedToSiteWhere();
        }

        /*
         * (non-Javadoc)
         *
         * @see com.sitewhere.android.messaging.IFromSiteWhere#receivedCustomCommand(byte[])
         */
        @Override
        public void receivedCustomCommand(byte[] payload) throws RemoteException {
            onReceivedCustomCommand(payload);

            if (mCallback != null)
                mCallback.onReceivedCustomCommand(payload);
        }

        /*
         * (non-Javadoc)
         *
         * @see com.sitewhere.android.messaging.IFromSiteWhere#receivedSystemCommand(byte[])
         */
        @Override
        public void receivedSystemCommand(byte[] payload) throws RemoteException {
            if (mCallback != null)
                mCallback.onReceivedSystemCommand(payload);
        }

        @Override
        public void receivedEventMessage(String topic, byte[] message) throws RemoteException {
            if (mCallback != null)
                mCallback.onReceivedEventMessage(topic, message);
        }

        /*
                 * (non-Javadoc)
                 *
                 * @see com.sitewhere.android.messaging.IFromSiteWhere#disconnected()
                 */
        @Override
        public void disconnected() throws RemoteException {
            if (mCallback != null)
                mCallback.onDisconnectedFromSiteWhere();
        }
    }

    /**
     * Handles connection to message service
     */
    protected ServiceConnection serviceConnection = new ServiceConnection() {

        /*
         * (non-Javadoc)
         *
         * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName,
         * android.os.IBinder)
         */
        public void onServiceConnected(ComponentName className, IBinder service) {
            mSitewhere = IToSiteWhere.Stub.asInterface(service);
            try {
                mSitewhere.register(mResponseProcessor);
                mBound = true;
                Log.d(TAG, "Registered with SiteWhere messaging service.");
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to register with SiteWhere messaging service.");
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        public void onServiceDisconnected(ComponentName className) {
            mBound = false;
            try {
                mSitewhere.unregister(mResponseProcessor);
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to disconnect from messaging.");
            }
            mSitewhere = null;
            mResponseProcessor = null;
            if (mCallback != null)
                mCallback.onDisconnectedFromSiteWhere();

            Log.d(TAG, "Disconnected from service.");
        }
    };

    /**
     * Callback interface for listeners to receive events from Sitewhere
     */
    public interface SiteWhereMessageClientCallback {

        /**
         * Called after connection to underlying messaging service is complete.
         */
        public void onConnectedToSiteWhere();

        /**
         * Called when a custom command payload is received.
         */
        public void onReceivedCustomCommand(byte[] payload);

        /**
         * Called when a custom command payload is received.
         */
        public void onReceivedSystemCommand(byte[] payload);

        /**
         * Called when a event message is received.
         *
         * @param payload
         */
        public void onReceivedEventMessage(String topic, byte[] payload);

        /**
         * Called when connection to SiteWhere is disconnected.
         */
        public void onDisconnectedFromSiteWhere();
    }

    public SiteWhereMessageClient(Context context) {
        mContext = context;
        sClient = SiteWhereMessageClient.this;
    }

    public static SiteWhereMessageClient getInstance() {
        return sClient;
    }

    public void connect(Parcelable servicePreferences) {

        connectToSiteWhereLocal(servicePreferences);
    }

    /**
     * Disconnect from the underlying messaging service.
     */
    public void disconnect() {
        if ((serviceConnection != null) && (mBound)) {
            if (mSitewhere != null) {
                try {
                    mSitewhere.unregister(mResponseProcessor);
                    Log.d(TAG, "No longer registered with SiteWhere messaging service.");
                } catch (RemoteException e) {
                    Log.e(TAG, "Unable to unregister from response processor.", e);
                }
            }
            mContext.unbindService(serviceConnection);
            serviceConnection = null;
            sClient = null;
        }
    }

    public void registerForEvents(String topic) {
        if ((serviceConnection != null) && (mBound)) {
            if (mSitewhere != null) {
                try {
                    mSitewhere.registerForEvents(topic);
                } catch (RemoteException e) {
                    Log.e(TAG, "Unable to register for events from response processor.", e);
                }
            }
        }
    }

    public void setCallback(SiteWhereMessageClientCallback cb) {
        this.mCallback = cb;
    }

    /**
     * Creates a connection to SiteWhere
     */
    protected void connectToSiteWhereLocal(Parcelable servicePreferences) {
        if (!mBound) {
            Intent intent = new Intent(mContext, getServiceClass());
            intent.putExtra(ISiteWhereMessaging.EXTRA_CONFIGURATION, servicePreferences);
            mContext.startService(intent);
            mContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Convenient method to the unique id for this device.  Useful for hardware id.
     */
    public String getUniqueDeviceId() {
        String id = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (id == null) {
            throw new RuntimeException(
                    "Running in context that does not have a unique id. Override getUniqueDeviceId() in subclass.");
        }
        return id;
    }

    public void sendDeviceMeasurements(String hardwareId, Map<String, Double> measurements, @Nullable Date eventDate) throws SiteWhereMessagingException {
        if (eventDate == null)
            eventDate = new Date();

        sendMeasurement(hardwareId, "", measurements, eventDate);
    }

    public void sendDeviceLocation(String hardwareId, double latitude, double longitude, double elevation, @Nullable Date eventDate) throws SiteWhereMessagingException {
        if (eventDate == null)
            eventDate = new Date();

        sendLocation(hardwareId, "", latitude, longitude, elevation, eventDate);
    }

    public void sendDeviceAlert(String hardwareId, String type, String message, @Nullable Date eventDate) throws SiteWhereMessagingException {
        if (eventDate == null)
            eventDate = new Date();

        sendAlert(hardwareId, "", type, message, eventDate);
    }

    public void sendDeviceRegistration(String hardwareId, String specificationToken, String originator, @Nullable String siteToken) throws SiteWhereMessagingException {
        Sitewhere.SiteWhere.RegisterDevice.Builder rb = Sitewhere.SiteWhere.RegisterDevice.newBuilder();
        rb.setHardwareId(hardwareId).setSpecificationToken(specificationToken);
        if (siteToken != null) {
            rb.setSiteToken(siteToken);
        }
        sendMessage(Sitewhere.SiteWhere.Command.SEND_REGISTRATION, rb.build(), originator, "registration");
    }

    /**
     * Send an acknowledgement event to SiteWhere.
     *
     * @param hardwareId
     * @param originator
     * @param message
     * @throws SiteWhereMessagingException
     */
    public void sendAck(String hardwareId, String originator, String message)
            throws SiteWhereMessagingException {
        Sitewhere.SiteWhere.Acknowledge.Builder builder = Sitewhere.SiteWhere.Acknowledge.newBuilder();
        Sitewhere.SiteWhere.Acknowledge ack = builder.setHardwareId(hardwareId).setMessage(message).build();
        sendMessage(Sitewhere.SiteWhere.Command.SEND_ACKNOWLEDGEMENT, ack, originator, "ack");
    }

    /**
     * Send a measurement event to SiteWhere.
     *
     * @param hardwareId
     * @param originator
     * @param measurements
     * @param eventDate
     * @throws SiteWhereMessagingException
     */
    private void sendMeasurement(String hardwareId, String originator, Map<String, Double> measurements, Date eventDate)
            throws SiteWhereMessagingException {
        Sitewhere.Model.DeviceMeasurements.Builder mxsb = Sitewhere.Model.DeviceMeasurements.newBuilder();
        mxsb.setEventDate(eventDate.getTime());
        mxsb.setHardwareId(hardwareId);

        Sitewhere.Model.Measurement.Builder mxb = Sitewhere.Model.Measurement.newBuilder();
        for (String key : measurements.keySet()) {
            mxb.setMeasurementId(key).setMeasurementValue(measurements.get(key));
            mxsb.setHardwareId(hardwareId).addMeasurement(mxb.build());
        }

        sendMessage(Sitewhere.SiteWhere.Command.SEND_DEVICE_MEASUREMENTS, mxsb.build(), originator, "measurement");
    }

    /**
     * Send a location event to SiteWhere.
     *
     * @param hardwareId
     * @param originator
     * @param latitude
     * @param longitude
     * @param elevation
     * @throws SiteWhereMessagingException
     */
    private void sendLocation(String hardwareId, String originator, double latitude, double longitude,
                              double elevation, Date eventDate) throws SiteWhereMessagingException {
        Sitewhere.Model.DeviceLocation.Builder lb = Sitewhere.Model.DeviceLocation.newBuilder();
        lb.setHardwareId(hardwareId).setLatitude(latitude).setLongitude(longitude).setElevation(elevation);
        lb.setEventDate(eventDate.getTime());
        sendMessage(Sitewhere.SiteWhere.Command.SEND_DEVICE_LOCATION, lb.build(), originator, "location");
    }

    /**
     * Send an alert event to SiteWhere.
     *
     * @param hardwareId
     * @param originator
     * @param alertType
     * @param message
     * @throws SiteWhereMessagingException
     */
    private void sendAlert(String hardwareId, String originator, String alertType, String message, Date eventDate)
            throws SiteWhereMessagingException {
        Sitewhere.Model.DeviceAlert.Builder ab = Sitewhere.Model.DeviceAlert.newBuilder();
        ab.setHardwareId(hardwareId).setAlertType(alertType).setAlertMessage(message);
        ab.setEventDate(eventDate.getTime());
        sendMessage(Sitewhere.SiteWhere.Command.SEND_DEVICE_ALERT, ab.build(), originator, "alert");
    }

    /**
     * Build message from header and message, then send it to the underlying delivery mechanism.
     *
     * @param command
     * @param message
     * @param originator
     * @param label
     * @throws SiteWhereMessagingException
     */
    protected void sendMessage(Sitewhere.SiteWhere.Command command, AbstractMessageLite message, String originator,
                               String label) throws SiteWhereMessagingException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Sitewhere.SiteWhere.Header.Builder builder = Sitewhere.SiteWhere.Header.newBuilder();
            builder.setCommand(command);
            if (originator != null) {
                builder.setOriginator(originator);
            }
            builder.build().writeDelimitedTo(out);
            message.writeDelimitedTo(out);
            byte[] encoded = out.toByteArray();
            StringBuffer hex = new StringBuffer();
            for (byte current : encoded) {
                hex.append(String.format("%02X ", current));
                hex.append(" ");
            }
            Log.d(TAG, hex.toString());
            sendCommand(encoded);
        } catch (IOException e) {
            throw new SiteWhereMessagingException("Problem encoding " + label + " message.", e);
        } catch (Exception e) {
            throw new SiteWhereMessagingException(e);
        }
    }

    /**
     * Send command to SiteWhere.
     */
    protected void sendCommand(byte[] payload) throws SiteWhereMessagingException {
        try {
            mSitewhere.send(payload);
        } catch (RemoteException e) {
            throw new SiteWhereMessagingException("Unable to send command.", e);
        }
    }

    protected void onReceivedCustomCommand(byte[] payload) {
        try {
            ByteArrayInputStream encoded = new ByteArrayInputStream(payload);
            ObjectInputStream in = new ObjectInputStream(encoded);

            String commandName = (String) in.readObject();
            Object[] parameters = (Object[]) in.readObject();
            Object[] parametersWithOriginator = new Object[parameters.length + 1];
            Class<?>[] types = new Class[parameters.length];
            Class<?>[] typesWithOriginator = new Class[parameters.length + 1];
            int i = 0;
            for (Object parameter : parameters) {
                types[i] = parameter.getClass();
                typesWithOriginator[i] = types[i];
                parametersWithOriginator[i] = parameters[i];
                i++;
            }
            IDeviceEventOriginator originator = (IDeviceEventOriginator) in.readObject();
            typesWithOriginator[i] = IDeviceEventOriginator.class;
            parametersWithOriginator[i] = originator;

            Method method = null;
            try {
                method = getClass().getMethod(commandName, typesWithOriginator);
                method.invoke(this, parametersWithOriginator);
            } catch (NoSuchMethodException e) {
                method = getClass().getMethod(commandName, types);
                method.invoke(this, parameters);
            }
        } catch (StreamCorruptedException e) {
            Log.e(TAG, "Unable to decode command in hybrid mode.", e);
        } catch (IOException e) {
            Log.e(TAG, "Unable to read command in hybrid mode.", e);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Unable to resolve parameter class.", e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Unable to find method signature that matches command.", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Not allowed to call method for command.", e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid argument for command.", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Unable to call method for command.", e);
        }
    }

    protected Class<? extends Service> getServiceClass() {
        return MqttService.class;
    }

}
