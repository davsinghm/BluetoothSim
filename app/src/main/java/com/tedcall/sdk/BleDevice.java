package com.tedcall.sdk;

import android.util.Log;

import com.dsm.bluetoothsim.device.BLEDevice;
import com.dsm.bluetoothsim.device.BTDevice;
import com.dsm.bluetoothsim.util.TextUtils;

public class BleDevice {

    public static final int RESULT_OK = 0;
    public static final byte CHANNEL_ID_BCSP_MUX = 64;
    private static final String TAG = BleDevice.class.getSimpleName();
    private static final int EVENT_CALL_CONNECTED = 3;
    private static final int EVENT_CALL_DISCONNECTED = 4;
    private static final int EVENT_VOICE_OPEN = 5;
    private static final int EVENT_VOICE_CLOSE = 6;
    private static final int EVENT_STATUS = 0;
    private static final int EVENT_SIM_READY = 1;
    private static final int EVENT_INCOMING_CALL = 2;
    private static final int EVENT_VOICE_RCVD = 7;
    private static final int EVENT_SEND_SMS_CNF = 8;
    private static final int EVENT_NEW_SMS = 9;
    private static final int EVENT_SIGNAL_STRENGTH = 11;
    private static final int EVENT_DEVICE_INFO = 12;
    private static final int EVENT_NET_OPERATOR = 14;
    private static BLEDevice bleDevice;

    static {
        System.loadLibrary("btdev");
        bleDevice = BTDevice.getInstance();
    }

    public final int RESULT_NOT_CONNECT = -2;
    public final int RESULT_NOT_OPEN = -1;
    public final int CHANNEL_ID_AT = 0;
    public final int CHANNEL_ID_CTRL = 1;
    public final int CHANNEL_ID_VOICE = 2;
    public final int CHANNEL_ID_SMS = 3;

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

    public native void open();

    public native int answer();

    public native int dial(String phoneNumber);

    public native int hangup();

    public native void ble_notify_data(byte channel, byte[] data);

    public native void bluetooth_connection_change_notify();

    //empty before calling queryDeviceInfo (0)
    public native String getIMEI();

    public native String getMSISDN();

    public native String getVersion();

    public native int queryDeviceInfo();

    public native int querySignal();

    public native int sendDTMF(char c);

    public native int sendSMS(String phoneNumber, String content);

    public native int voiceWrite(byte[] data);

    public int jni_callback_ble_exist_sms_channel() {
        return 1;
    }

    public String jni_callback_ble_get_device_password() {
        return "0000";
    }

    public int jni_callback_ble_is_connected() {
        Log.d(TAG, "jni_callback_ble_is_connected() is called");
        return bleDevice.isSocketConnected() ? 1 : 0;
    }

    public int jni_callback_ble_is_mtkdevice() {
        return 1;
    }

    public void jni_callback_ble_write_data(byte channel, byte[] data) {
        Log.d(TAG, "jni_callback_ble_write_data() is called");
        printHexString("base print channel: " + channel + ", data:", data);
        if (channel == CHANNEL_ID_BCSP_MUX)
            bleDevice.onSocketWrite(data);
        else
            bleDevice.onGattWrite(channel, data);
    }

    public void onDeviceInfo(int level, int charging) {
        Log.d(TAG, "onDeviceInfo: " + level + ',' + charging);
        bleDevice.onBatteryInfo(level, charging == 1);
    }

    /**
     * @param keyStatus 0 = down, 2 = up
     * @param keyCode   1 = side (cam)
     */
    public void onDeviceKey(int keyStatus, int keyCode) {
        if (keyStatus == 2)
            bleDevice.onKeyUp(keyCode);
    }

    public void onDeviceStatus(int status) {
        Log.d(TAG, "onDeviceStatus:" + status);
        if (status == 1)
            bleDevice.onDeviceConnected();
        else
            bleDevice.onDeviceDisconnected("Ble Device");
    }

    public void onEvent(int event) {

        switch (event) {
            case EVENT_CALL_CONNECTED:
                Log.d(TAG, "onEvent: EVENT_CALL_CONNECTED");

                bleDevice.onCallConnected();
                break;
            case EVENT_CALL_DISCONNECTED:
                Log.d(TAG, "onEvent: EVENT_CALL_DISCONNECTED");

                bleDevice.onCallDisconnected();
                break;
            case EVENT_VOICE_OPEN:
                Log.d(TAG, "onEvent: EVENT_VOICE_OPEN");

                bleDevice.onVoiceOpened();
                break;
            case EVENT_VOICE_CLOSE:
                Log.d(TAG, "onEvent: EVENT_VOICE_CLOSE");

                bleDevice.onVoiceClosed();
                break;
            default:
                throw new RuntimeException("Unknown Event");
        }
    }

    public void onIncomingCall(String phoneNumber) {
        Log.d(TAG, "onIncomingCall:" + phoneNumber);
        phoneNumber = TextUtils.clean(phoneNumber);
        if (phoneNumber != null) {
            if (!phoneNumber.startsWith("+") && phoneNumber.matches("\\d+") && !phoneNumber.startsWith("0")) phoneNumber = "+" + phoneNumber;
        }
        bleDevice.onCallRinging(phoneNumber);
    }

    public void onNetOperator(int stat, String opName, String opNumber) {
        Log.d(TAG, "onNetOperator:" + stat + "," + opName + ',' + opNumber);
        opName = TextUtils.clean(opName);
        opNumber = TextUtils.clean(opNumber);
        bleDevice.onNetOperator(stat, opName, opNumber);
    }

    public void onNewSMS(String phoneNumber, String content) {
        Log.d(TAG, "onNewSMS:" + phoneNumber + "," + content);
        phoneNumber = TextUtils.clean(phoneNumber);
        if (phoneNumber != null) {
            if (!phoneNumber.startsWith("+") && phoneNumber.matches("\\d+") && !phoneNumber.startsWith("0")) phoneNumber = "+" + phoneNumber;
        }
        content = TextUtils.clean(content);
        bleDevice.onNewSMS(phoneNumber, content);
    }

    public void onSendSMSCnf(int result) {
        Log.d(TAG, "onSendSMSCnf:" + result);
        bleDevice.onSendSMSResult(result == 1); //0 = fail TODO confirm
    }

    public void onSignalStrength(int signalValue) {
        Log.d(TAG, "onSignalStrength:" + signalValue);
        bleDevice.onSignalStrength(signalValue);
    }

    public void onSimPinResult(int result) {
        Log.d(TAG, "onSimPinResult:" + result);
        bleDevice.onSimPinResult(result);
    }

    public void onSimStatus(int status) {
        Log.d(TAG, "onSimStatus:" + status);
        bleDevice.onSimStatus(status);
    }

    public void onTCAuthResult(int result) {
        Log.d(TAG, "onTCAuthResult:" + result);
        bleDevice.onTCAuthResult(result);
    }

    public void onVoiceCallback(byte[] data) {
        Log.d(TAG, "onVoiceCallback");
        bleDevice.onVoiceReceived(data);
    }
}
