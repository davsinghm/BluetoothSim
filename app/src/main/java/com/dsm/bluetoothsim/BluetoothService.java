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
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.provider.CallLog;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.dsm.bluetoothsim.ui.PhoneActivity;
import com.tedcall.sdk.BleDevice;

import java.io.IOException;

public class BluetoothService extends Service {

    private final String TAG = BluetoothService.class.getSimpleName();
    private final String mDeviceAddress = "36:88:06:01:08:B7"; //TODO scan and update
    private final String mLeDeviceAddress = "CD:F0:5E:F8:01:CA";

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

    private static final String packageName = Application.class.getPackage().getName();
    public static final String ACTION_STOP_SERVICE = packageName + ".service.STOP_SERVICE";

    //broadcasts global
    public static final String ACTION_SMS_REPLY = packageName + ".service.ACTION_SMS_REPLY";
    //both
    public static final String ACTION_CALL_DECLINE = packageName + ".service.ACTION_CALL_DECLINE";
    public static final String ACTION_CALL_ANSWER = packageName + ".service.ACTION_CALL_ANSWER";
    public static final String ACTION_CALL_HANG_UP = packageName + ".service.ACTION_CALL_HANG_UP";
    //local
    public static final String ACTION_BT_CANCEL_DISCOVERY = packageName + ".service.ACTION_BT_CANCEL_DISCOVERY";

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "GATT New State " + (newState == BluetoothProfile.STATE_CONNECTED ? "STATE_CONNECTED" : "STATE_DISCONNECTED"));
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null && mDeviceAddress.equals(device.getAddress())) {
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    Log.w(TAG, "ACL_CONNECTED");
                    notificationHelper.notifyConnecting("ACL Connected");
                } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    Log.w(TAG, "ACTION_BOND_STATE_CHANGED");
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                    Log.w(TAG, "ACL_DISCONNECT_REQUESTED");
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    Log.w(TAG, "ACL_DISCONNECTED");
                    notificationHelper.notifyDisconnected("ACL Disconnected");
                    //connectDevice();
                }
            }

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    connectDevice();
                }
            }

            if (ACTION_SMS_REPLY.equals(action)) {
                String text = RemoteInput.getResultsFromIntent(intent).getString(NotificationHelper.KEY_SMS_REPLY);
                String phoneNumber = intent.getStringExtra("PHONE_NUMBER");
                Log.d(TAG, "Reply " + text + " to " + phoneNumber);
                //TODO
            }

            if (ACTION_CALL_DECLINE.equals(action))
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ACTION_CALL_DECLINE));
            else if (ACTION_CALL_ANSWER.equals(action))
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ACTION_CALL_ANSWER));
            else if (ACTION_CALL_HANG_UP.equals(action))
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ACTION_CALL_HANG_UP));
        }
    };


    private final BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /*
            filter.addAction(BTDevice.ACTION_SEND_SMS);
            filter.addAction(BTDevice.ACTION_SIM_STATUS);*/
            String action = intent.getAction();

            if (BTDevice.ACTION_NET_OPERATOR.equals(action)) {
                int status = intent.getIntExtra("STATUS", 0);
                String opName = intent.getStringExtra("OPERATOR_NAME");
                String opNumber = intent.getStringExtra("OPERATOR_NUMBER");
                notificationHelper.notifySMS("Net Operator", "Status: " + status + ", " + opName + ", OpNum: " + opNumber);
            }

            if (BTDevice.ACTION_SIGNAL_LENGTH.equals(action)) {
                int value = intent.getIntExtra("SIGNAL", 0);
                notificationHelper.notifySMS("Signal Length", "Signal: " + value);
            }

            if (BTDevice.ACTION_CONNECTED.equals(action)) {
                notificationHelper.notifyConnected("From Device");
            }

            if (BTDevice.ACTION_DISCONNECTED.equals(action)) {
                notificationHelper.notifyDisconnected("From Device");
            }

            if (BTDevice.ACTION_DEVICE_INFO.equals(action)) {
                int level = intent.getIntExtra("LEVEL", -1);
                boolean isCharging = intent.getBooleanExtra("IS_CHARGING", false);
                //notificationHelper.updateService(R.drawable.ic_bluetooth_connected_white, "Connected", null, level >= 0 ? "" + level + "%" + (isCharging ? " (Charging)" : "") : null);
            }

            if (BTDevice.ACTION_NEW_SMS.equals(action)) {
                String phoneNumber = intent.getStringExtra("PHONE_NUMBER");
                String content = intent.getStringExtra("CONTENT");
                notificationHelper.notifySMS(phoneNumber, content);
            }

            if (BTDevice.ACTION_INCOMING_CALL.equals(action)) {
                String phoneNumber = intent.getStringExtra("PHONE_NUMBER");
                if (mPhoneNumber != null && !phoneNumber.equals(mPhoneNumber) || mPhoneNumber == null) {
                    mCallType = CallType.INCOMING_CALL;

                    PhoneActivity.notifyIncomingCall(Application.getAppContext(), phoneNumber);
                    mPhoneNumber = phoneNumber;
                    mTimeInMillis = System.currentTimeMillis();
                }
            }

            if (BTDevice.ACTION_OUTGOING_CALL.equals(action)) {
                String phoneNumber = intent.getStringExtra("PHONE_NUMBER");
                if (mPhoneNumber != null && !phoneNumber.equals(mPhoneNumber) || mPhoneNumber == null) {
                    mCallType = CallType.OUTGOING_CALL;

                    PhoneActivity.notifyOutgoingCall(Application.getAppContext(), phoneNumber);
                    mPhoneNumber = phoneNumber;
                    mTimeInMillis = System.currentTimeMillis();
                }
            }

            if (ACTION_CALL_ANSWER.equals(action)) {
                BTDevice.getInstance().answer();
                PhoneActivity.ongoingIncomingCallNotification(Application.getAppContext(), mPhoneNumber);
                mAnswered = true;
            }

            if (ACTION_CALL_DECLINE.equals(action)) {
                BTDevice.getInstance().hangup();
                mDeclined = true;
            }

            if (ACTION_CALL_HANG_UP.equals(action)) {
                BTDevice.getInstance().hangup();
            }

            if (BTDevice.ACTION_EVENT.equals(action)) {
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


    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        notificationHelper = new NotificationHelper(getApplicationContext());
        startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notificationHelper.getServiceNotification());

        registerReceiver(mReceiver, makeGlobalIntentFilter());
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalBroadcastReceiver, makeLocalIntentFilter());

        if (!initializeBluetooth()) {
            notificationHelper.notifyDisconnected("Unable to initialize BluetoothManager");
            return;
        }

        connectDevice();
    }

    private void connectDevice() {
        notificationHelper.notifyConnecting();

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        device.createBond();

        BluetoothDevice leDevice = mBluetoothAdapter.getRemoteDevice(mLeDeviceAddress);
        leDevice.createBond();
        //leDevice.connectGatt(this, true, mGattCallback);

        try {
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(BTDevice.UUID_SPP);

            BTDevice.connect(socket);
            BTDevice.getInstance().open();

            BTDevice.getInstance().queryDeviceInfo();

        } catch (IOException e) {
            e.printStackTrace();
            notificationHelper.notifyDisconnected("Cannot create Serial port");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (BluetoothService.ACTION_STOP_SERVICE.equals(intent.getAction())) {
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BTDevice.disconnect();

        unregisterReceiver(mReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalBroadcastReceiver);
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

    public IntentFilter makeGlobalIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        filter.addAction(ACTION_SMS_REPLY);
        filter.addAction(ACTION_CALL_ANSWER);
        filter.addAction(ACTION_CALL_HANG_UP);
        filter.addAction(ACTION_CALL_DECLINE);
        return filter;
    }

    public IntentFilter makeLocalIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BTDevice.ACTION_CONNECTED);
        filter.addAction(BTDevice.ACTION_DISCONNECTED);
        filter.addAction(BTDevice.ACTION_DEVICE_INFO);
        filter.addAction(BTDevice.ACTION_EVENT);
        filter.addAction(BTDevice.ACTION_INCOMING_CALL);
        filter.addAction(BTDevice.ACTION_OUTGOING_CALL);
        filter.addAction(BTDevice.ACTION_NET_OPERATOR);
        filter.addAction(BTDevice.ACTION_NEW_SMS);
        filter.addAction(BTDevice.ACTION_SEND_SMS);
        filter.addAction(BTDevice.ACTION_SIGNAL_LENGTH);
        filter.addAction(BTDevice.ACTION_SIM_STATUS);

        filter.addAction(ACTION_BT_CANCEL_DISCOVERY);
        filter.addAction(ACTION_CALL_ANSWER);
        filter.addAction(ACTION_CALL_HANG_UP);
        filter.addAction(ACTION_CALL_DECLINE);
        return filter;
    }
}