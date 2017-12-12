package com.tedcall.sdk;

import android.util.Log;

import com.dsm.bluetoothsim.BTDevEvent;

public class BleDevice {

    private static final String TAG = BleDevice.class.getSimpleName();

    public final int BTDEV_NOT_CONNECT = -2;
    public final int BTDEV_NOT_OPEN = -1;
    public final int BTDEV_RESULT_OK = 0;

    public static final int EVENT_STATUS = 0;
    public static final int EVENT_SIM_READY = 1;
    public static final int EVENT_INCOMING_CALL = 2;
    public static final int EVENT_CALL_CONNECTED = 3;
    public static final int EVENT_CALL_DISCONNECTED = 4;
    public static final int EVENT_VOICE_OPEN = 5;
    public static final int EVENT_VOICE_CLOSE = 6;
    public static final int EVENT_VOICE_RCVD = 7;
    public static final int EVENT_SEND_SMS_CNF = 8;
    public static final int EVENT_NEW_SMS = 9;
    public static final int EVENT_SIGNAL_STRENGTH = 11;
    public static final int EVENT_DEVICE_INFO = 12;
    public static final int EVENT_NET_OPERATOR = 14;
    public final int CHANNEL_ID_AT = 0;
    public final int CHANNEL_ID_CTRL = 1;
    public final int CHANNEL_ID_VOICE = 2;
    public final int CHANNEL_ID_SMS = 3;
    public final byte CHANNEL_ID_BCSP_MUX = 64;

    static {
        System.loadLibrary("btdev");
    }

    public native void open();

    public native int answer();

    public native int dial(String phoneNumber);

    public native int hangup();

    public native void ble_notify_data(byte b, byte[] data);

    public native void bluetooth_connection_change_notify();

    public native String getIMEI();

    public native String getMSISDN();

    public native String getVersion();

    public native int queryDeviceInfo();

    public native int querySignal();

    public native int sendDTMF(char c);

    public native int sendSMS(String phoneNumber, String content);

    public native int voiceWrite(byte[] data);

    public int jni_callback_ble_exist_sms_channel() {
        Log.d(TAG, "jni_callback_ble_exist_sms_channel() is called");
        return 1;
    }

    public String jni_callback_ble_get_device_password() {
        Log.d(TAG, "jni_callback_ble_get_device_password() is called");
        return "0000";
    }

    public int jni_callback_ble_is_connected() {
        Log.d(TAG, "jni_callback_ble_is_connected() is called");
        return 0;
    }

    public int jni_callback_ble_is_mtkdevice() {
        Log.d(TAG, "jni_callback_ble_is_mtkdevice() is called");
        return 1;
    }

    public void jni_callback_ble_write_data(byte channel, byte[] data) {
        Log.d(TAG, "jni_callback_ble_write_data() is called");
        printHexString("base print channel: " + channel + ", data:", data);
    }

    public void onDeviceInfo(int level, int charging) {
        Log.d(TAG, "onDeviceInfo: " + level + ',' + charging);
    }

    public void onDeviceKey(int keyStatus, int keyCode) {
        Log.d(TAG, "onDeviceKey: " + keyStatus + ',' + keyCode);
    }

    public void onDeviceStatus(int status) {
        Log.d(TAG, "onDeviceStatus:" + status);
    }

    public void onEvent(int event) {
        Log.d(TAG, "onEvent: " + BTDevEvent.getEvent(event).name());
    }

    public void onIncomingCall(String phoneNumber) {
        Log.d(TAG, "onIncomingCall:" + phoneNumber);
    }

    public void onNetOperator(int stat, String opName, String opNumber) {
        Log.d(TAG, "onNetOperator:" + stat + "," + opName + ',' + opNumber);
    }

    public void onNewSMS(String phoneNumber, String content) {
        Log.d(TAG, "onNewSMS:" + phoneNumber + "," + content);
    }

    public void onSendSMSCnf(int result) {
        Log.d(TAG, "onSendSMSCnf:" + result);
    }

    public void onSignalStrength(int signalValue) {
        Log.d(TAG, "onSignalStrength:" + signalValue);
    }

    public void onSimPinResult(int result) {
        Log.d(TAG, "onSimPinResult:" + result);
    }

    public void onSimStatus(int status) {
        Log.d(TAG, "onSimStatus:" + status);
    }

    public void onTCAuthResult(int result) {
        Log.d(TAG, "onTCAuthResult:" + result);
    }

    public void onVoiceCallback(byte[] data) {
        Log.d(TAG, "onVoiceCallback");
    }

    public static void printHexString(String hint, byte[] bytes) {
        System.out.print("printHexString: hint: " + hint + ", ");
        for (byte b : bytes) {
            String str = Integer.toHexString(b & 0xFF);
            if (str.length() == 1)
                str = '0' + str;
            System.out.print(str.toUpperCase() + " ");
        }
        System.out.println("");
    }
}
