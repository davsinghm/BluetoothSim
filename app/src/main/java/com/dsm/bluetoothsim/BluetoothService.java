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
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.provider.CallLog;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.dsm.bluetoothsim.ui.PhoneActivity;
import com.tedcall.sdk.BleDevice;

import java.io.IOException;
import java.net.Socket;

public class BluetoothService extends Service {

    private final String TAG = BluetoothService.class.getSimpleName();
    private final String mDeviceAddress = "36:88:06:01:08:B7"; //TODO scan and update
    String mLeDeviceAddress = "CD:F0:5E:F8:01:CA";

    private Context mContext;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private NotificationHelper notificationHelper;

    /* used for call */
    CallType mCallType;

    enum CallType {INCOMING_CALL, OUTGOING_CALL}

    boolean mAnswered;
    boolean mDeclined;
    String mPhoneNumber;
    String mDisplayName;
    int mDuration;
    long mTimeInMillis;

    //used through notification
    public static final String PENDING_INTENT_HANG_UP = Application.class.getPackage().getName() + ".service.PENDING_INTENT_HANG_UP";
    public static final String PENDING_INTENT_DECLINE = Application.class.getPackage().getName() + ".service.PENDING_INTENT_DECLINE";
    public static final String PENDING_INTENT_ANSWER = Application.class.getPackage().getName() + ".service.PENDING_INTENT_ANSWER";

    public static final String ACTION_CALL_DECLINE = Application.class.getPackage().getName() + ".service.ACTION_CALL_DECLINE";
    public static final String ACTION_CALL_ANSWER = Application.class.getPackage().getName() + ".service.ACTION_CALL_ANSWER";
    public static final String ACTION_CALL_HANG_UP = Application.class.getPackage().getName() + ".service.ACTION_CALL_HANG_UP";

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null && mDeviceAddress.equals(device.getAddress())) {
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    Log.w(TAG, "CONNECTED");
                } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    Log.w(TAG, "ACTION_BOND_STATE_CHANGED");
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                    Log.w(TAG, "REQUESTED");
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    Log.w(TAG, "DISCONNECTED");
                    notificationHelper.updateService(R.drawable.ic_bluetooth_disabled_white, "Device disconnected", null, null);
                    //connectDevice();
                }
            }

            if (PENDING_INTENT_DECLINE.equals(action))
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ACTION_CALL_DECLINE));
            else if (PENDING_INTENT_ANSWER.equals(action))
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ACTION_CALL_ANSWER));
            else if (PENDING_INTENT_HANG_UP.equals(action))
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ACTION_CALL_HANG_UP));
        }
    };


    private final BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /*
            filter.addAction(BTDeviceApi.ACTION_CONNECTED);
            filter.addAction(BTDeviceApi.ACTION_DISCONNECTED);
            filter.addAction(BTDeviceApi.ACTION_NET_OPERATOR);
            filter.addAction(BTDeviceApi.ACTION_SEND_SMS);
            filter.addAction(BTDeviceApi.ACTION_SIGNAL_LENGTH);
            filter.addAction(BTDeviceApi.ACTION_SIM_STATUS);
            filter.addAction(BTDeviceApi.ACTION_VOICE_BACK);*/
            String action = intent.getAction();
            if (BTDeviceApi.ACTION_DEVICE_INFO.equals(action)) {
                int level = intent.getIntExtra("LEVEL", -1);
                boolean isCharging = intent.getBooleanExtra("IS_CHARGING", false);
                notificationHelper.updateService(R.drawable.ic_bluetooth_connected_white, "Connected", null, level >= 0 ? "" + level + "%" + (isCharging ? " (Charging)" : "") : null);
            }

            if (BTDeviceApi.ACTION_NEW_SMS.equals(action)) {
                String phoneNumber = intent.getStringExtra("PHONE_NUMBER");
                String content = intent.getStringExtra("CONTENT");
                PhoneActivity.notifySMS(Application.getAppContext(), phoneNumber, content);
            }

            if (BTDeviceApi.ACTION_INCOMING_CALL.equals(action)) {
                String phoneNumber = intent.getStringExtra("PHONE_NUMBER");
                if (mPhoneNumber != null && !phoneNumber.equals(mPhoneNumber) || mPhoneNumber == null) {
                    mCallType = CallType.INCOMING_CALL;

                    PhoneActivity.notifyIncomingCall(Application.getAppContext(), phoneNumber);
                    mPhoneNumber = phoneNumber;
                    mTimeInMillis = System.currentTimeMillis();
                }
            }

            if (BTDeviceApi.ACTION_OUTGOING_CALL.equals(action)) {
                String phoneNumber = intent.getStringExtra("PHONE_NUMBER");
                if (mPhoneNumber != null && !phoneNumber.equals(mPhoneNumber) || mPhoneNumber == null) {
                    mCallType = CallType.OUTGOING_CALL;

                    PhoneActivity.notifyOutgoingCall(Application.getAppContext(), phoneNumber);
                    mPhoneNumber = phoneNumber;
                    mTimeInMillis = System.currentTimeMillis();
                }
            }

            if (ACTION_CALL_ANSWER.equals(action)) {
                BTDeviceApi.getInstance().answer();
                PhoneActivity.ongoingIncomingCallNotification(Application.getAppContext(), mPhoneNumber);
                mAnswered = true;
            }

            if (ACTION_CALL_DECLINE.equals(action)) {
                BTDeviceApi.getInstance().hangup();
                mDeclined = true;
            }

            if (ACTION_CALL_HANG_UP.equals(action)) {
                BTDeviceApi.getInstance().hangup();
            }

            if (BTDeviceApi.ACTION_EVENT.equals(action)) {
                int event = intent.getIntExtra("EVENT", -1);
                switch (event) {
                    case BleDevice.EVENT_CALL_DISCONNECTED:
                        if (mCallType == CallType.INCOMING_CALL) {
                            PhoneActivity.dismissIncomingCallNotification(Application.getAppContext());

                            int type = mDeclined ? CallLog.Calls.REJECTED_TYPE : (mAnswered ? CallLog.Calls.INCOMING_TYPE : CallLog.Calls.MISSED_TYPE);
                            long when = type == CallLog.Calls.INCOMING_TYPE ? mTimeInMillis : System.currentTimeMillis();
                            if (type == CallLog.Calls.MISSED_TYPE)
                                PhoneActivity.notifyMissedCall(mContext, mPhoneNumber);

                            CallLogUtility.addNumber(mContext, getContentResolver(), mPhoneNumber, type, when, mDuration);
                        } else if (mCallType == CallType.OUTGOING_CALL) {
                            PhoneActivity.dismissOutgoingCallNotification(Application.getAppContext());

                            CallLogUtility.addNumber(mContext, getContentResolver(), mPhoneNumber, CallLog.Calls.OUTGOING_TYPE, mTimeInMillis, mDuration);
                        }

                        resetCallStats();
                        break;
                }
            }
        }
    };

    private void resetCallStats() {
        mCallType = null;
        mAnswered = false;
        mDeclined = false;
        mPhoneNumber = null;
        mDisplayName = null;
        mDuration = 0;
        mTimeInMillis = 0;
    }

    public IntentFilter makeGlobalIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        filter.addAction(PENDING_INTENT_ANSWER);
        filter.addAction(PENDING_INTENT_HANG_UP);
        filter.addAction(PENDING_INTENT_DECLINE);
        return filter;
    }

    public IntentFilter makeExtendCardIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BTDeviceApi.ACTION_CONNECTED);
        filter.addAction(BTDeviceApi.ACTION_DISCONNECTED);
        filter.addAction(BTDeviceApi.ACTION_DEVICE_INFO);
        filter.addAction(BTDeviceApi.ACTION_EVENT);
        filter.addAction(BTDeviceApi.ACTION_INCOMING_CALL);
        filter.addAction(BTDeviceApi.ACTION_OUTGOING_CALL);
        filter.addAction(BTDeviceApi.ACTION_NET_OPERATOR);
        filter.addAction(BTDeviceApi.ACTION_NEW_SMS);
        filter.addAction(BTDeviceApi.ACTION_SEND_SMS);
        filter.addAction(BTDeviceApi.ACTION_SIGNAL_LENGTH);
        filter.addAction(BTDeviceApi.ACTION_SIM_STATUS);
        filter.addAction(BTDeviceApi.ACTION_VOICE_BACK);

        filter.addAction(ACTION_CALL_ANSWER);
        filter.addAction(ACTION_CALL_HANG_UP);
        filter.addAction(ACTION_CALL_DECLINE);
        return filter;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        notificationHelper = new NotificationHelper();
        showServiceNotification();

        registerReceiver(mReceiver, makeGlobalIntentFilter());
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalBroadcastReceiver, makeExtendCardIntentFilter());

        if (!initializeBluetooth()) {
            notificationHelper.updateService(R.drawable.ic_bluetooth_disabled_white, "Unable to initialize BluetoothManager", null, null);
            return;
        }

        connectDevice();

    }

    private void connectDevice() {

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        device.createBond();

        BluetoothDevice leDevice = mBluetoothAdapter.getRemoteDevice(mLeDeviceAddress);
        leDevice.createBond();
        leDevice.connectGatt(this, true, mGattCallback);

        try {
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(ExtendCard.TEDCALL_SPP_UUID);
            // mBluetoothAdapter.cancelDiscovery();

            BTDeviceApi.connect(socket);
            BTDeviceApi.getInstance().open();


            notificationHelper.updateService(R.drawable.ic_bluetooth_connected_white, "Connected", null, null);
            BTDeviceApi.getInstance().queryDeviceInfo();

        } catch (IOException e) {
            e.printStackTrace();
            notificationHelper.updateService(R.drawable.ic_bluetooth_disabled_white, "Failed to create RFComm Socket", null, null);
        }
    }

    private void showServiceNotification() {
        startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notificationHelper.startService(getApplicationContext()));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String address = intent.getStringExtra("ADDRESS");
        if (address != null) ;
        //connect(address);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.w("BLEService", "onDestroy()");
        //close();
        BTDeviceApi.disconnect();

        unregisterReceiver(mReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalBroadcastReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initializeBluetooth() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
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
}