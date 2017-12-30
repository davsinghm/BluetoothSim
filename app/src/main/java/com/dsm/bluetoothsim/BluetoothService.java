/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dsm.bluetoothsim;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.dsm.bluetoothsim.device.BTDevice;
import com.dsm.bluetoothsim.device.BTDeviceListener;
import com.dsm.bluetoothsim.device.CallStats;
import com.dsm.bluetoothsim.util.CallLogUtility;

public class BluetoothService extends Service implements BTDeviceListener {

    private final String TAG = BluetoothService.class.getSimpleName();
    public static final String mDeviceAddress = "36:88:06:01:08:B7"; //TODO scan and update
    private final String mLeDeviceAddress = "CD:F0:5E:F8:01:CA";

    private Context mContext;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private NotificationHelper notificationHelper;

    private static final String packageName = Application.class.getPackage().getName();
    public static final String ACTION_SERVICE_STOP = packageName + ".service.ACTION_STOP";
    public static final String ACTION_SERVICE_CONNECT = packageName + ".service.ACTION_CONNECT";
    public static final String ACTION_SERVICE_DISCONNECT = packageName + ".service.ACTION_DISCONNECT";
    public static final String ACTION_SERVICE_REFRESH = packageName + ".service.ACTION_REFRESH";

    public static final String ACTION_SMS_MARK_READ = packageName + ".service.ACTION_SMS_MARK_READ";
    public static final String ACTION_SMS_REPLY = packageName + ".service.ACTION_SMS_REPLY";
    public static final String ACTION_CALL = packageName + ".service.ACTION_CALL";
    public static final String ACTION_CALL_ANSWER = packageName + ".service.ACTION_CALL_ANSWER";
    public static final String ACTION_CALL_HANG_UP = packageName + ".service.ACTION_CALL_HANG_UP";

    //broadcasts
    public static final String ACTION_CALL_HANGED_UP = packageName + ".service.ACTION_CALL_HANGED_UP";
    public static final String ACTION_CALL_CONNECTED = packageName + ".service.ACTION_CALL_CONNECTED";
    public static final String ACTION_CALL_DISCONNECTED = packageName + ".service.ACTION_CALL_DISCONNECTED";

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "GATT New State " + (newState == BluetoothProfile.STATE_CONNECTED ? "STATE_CONNECTED" : "STATE_DISCONNECTED"));
        }
    };


    @Override
    public void onDeviceConnected() {
        notificationHelper.notifyConnected();
        Log.d(TAG, "ACTION_CONNECTED");

        BTDevice.getInstance().queryBatteryInfo(); //TODO remove
        BTDevice.getInstance().querySignal(); //TODO remove
    }

    @Override
    public void onDeviceDisconnected(String error) {
        notificationHelper.notifyDisconnected(error);
    }

    @Override
    public void onNewSMS(@Nullable String phoneNumber, @Nullable String content) {
        notificationHelper.notifySMS(phoneNumber, content);
    }

    @Override
    public void onCallRinging(@Nullable String phoneNumber) {

        notificationHelper.notifyIncomingCall(phoneNumber);

        startVibration();
    }

    @Override
    public void onCallDialed(@NonNull String phoneNumber) {
        notificationHelper.notifyOngoingCall(phoneNumber);
    }

    @Override
    public void onCallConnected(@Nullable String phoneNumber) {
        stopVibration();//TODO move
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_CALL_CONNECTED)); //TODO

        notificationHelper.notifyOngoingCall(phoneNumber);
    }

    @Override
    public void onCallDisconnected(CallStats stats) {
        notificationHelper.notifyCallDisconnected();

        if (stats.getCallType() == CallStats.MISSED_TYPE)
            notificationHelper.notifyMissedCall(stats.getPhoneNumber());

        CallLogUtility.putLog(mContext, getContentResolver(), stats.getPhoneNumber(), stats.getCallType(), stats.getTimestamp(), stats.getDuration());

        Intent intent = new Intent(stats.isHangedUp() ? ACTION_CALL_HANGED_UP : ACTION_CALL_DISCONNECTED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        stopVibration();//TODO move
    }

    @Override
    public void onBatteryInfo(int level, boolean isCharging) {
        notificationHelper.notifyBattery(level, isCharging);
    }

    @Override
    public void onNetOperator(int stat, @Nullable String opName, @Nullable String opNumber) {
        notificationHelper.notifyNetwork(stat, opName, opNumber);
    }

    @Override
    public void onSignalStrength(int signal) {
        notificationHelper.notifySignal(signal);
    }

    @Override
    public void cancelBtDiscovery() {
        mBluetoothAdapter.cancelDiscovery();
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        notificationHelper = new NotificationHelper(getApplicationContext());
        startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notificationHelper.getServiceNotification());

        if (!initializeBluetooth()) {
            NotificationHelper.notifyLog("Error", "Unable to initialize BluetoothManager");
        }
    }

    private void startBtDevice() {
        if (BTDevice.getInstance().isConnected() || BTDevice.getInstance().isConnecting()) {
            NotificationHelper.notifyLog("Service", "BTDevice is connected or connecting!");
            return;
        }

        notificationHelper.notifyConnecting();

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        device.createBond();

        BluetoothDevice leDevice = mBluetoothAdapter.getRemoteDevice(mLeDeviceAddress);
        leDevice.createBond();
//        leDevice.connectGatt(this, true, mGattCallback); //TODO move, needed to connect/pair for first time | 2: test gatt

        BTDevice.registerCallbacks(device, this);
    }

    /*TODO
            start this onAnswered, onCallMade
            Intent phoneIntent = new Intent(this, PhoneActivity.class);
            startActivity(phoneIntent);
     */

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_SERVICE_STOP.equals(action)) {
            stopSelf();
        } else if (ACTION_SERVICE_DISCONNECT.equals(action)) {
            BTDevice.getInstance().disconnect();
        } else if (ACTION_SERVICE_REFRESH.equals(action)) {
            BTDevice.getInstance().queryBatteryInfo();
            BTDevice.getInstance().querySignal();
        } else if (ACTION_CALL.equals(action)) {
            String phoneNumber = intent.getStringExtra("PHONE_NUMBER");
            if (phoneNumber != null) BTDevice.getInstance().call(phoneNumber);
        } else if (ACTION_CALL_ANSWER.equals(action)) {
            BTDevice.getInstance().answerRingingCall();
        } else if (ACTION_CALL_HANG_UP.equals(action)) {
            BTDevice.getInstance().endCall();
        } else if (ACTION_SMS_REPLY.equals(action)) {
            String text = RemoteInput.getResultsFromIntent(intent).getString(NotificationHelper.REMOTE_INPUT_KEY_SMS_REPLY);
            String phoneNumber = intent.getStringExtra("PHONE_NUMBER");
            Log.d(TAG, "Reply " + text + " to " + phoneNumber);
            //TODO
        }else if (ACTION_SMS_MARK_READ.equals(action)) {
            //TODO
        } else if (ACTION_SERVICE_CONNECT.equals(action)) {
            startBtDevice();
        } else {
            startBtDevice();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BTDevice.destroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public boolean initializeBluetooth() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    boolean shouldVibrate;
    private Thread vibrationThread;

    private void startVibration() { //TODO check if phone's vibration is enabled while phone call
        shouldVibrate = true;
        if (vibrationThread != null)
            vibrationThread.interrupt();

        vibrationThread = new Thread() {
            long timestamp;

            @Override
            public void run() {
                timestamp = System.currentTimeMillis();
                while (shouldVibrate && System.currentTimeMillis() - timestamp < 2 * 60 * 1000) {
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    vibrator.vibrate(1300);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        vibrator.cancel();
                    }
                }
                vibrationThread = null;
            }
        };
        vibrationThread.start();

    }

    private void stopVibration() {
        if (vibrationThread != null)
            vibrationThread.interrupt();

        shouldVibrate = false;
    }
}