package com.dsm.bluetoothsim.device;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dsm.bluetoothsim.Application;
import com.dsm.bluetoothsim.BluetoothService;
import com.dsm.bluetoothsim.NotificationHelper;
import com.dsm.bluetoothsim.util.TextUtils;
import com.tedcall.sdk.BleDevice;

import java.util.UUID;


/**
 * Created by dsm on 12/19/17.
 */

public abstract class BLEDevice {
    private static BleDevice bleDevice;

    static void init() {
        bleDevice = new BleDevice();
        Application.getAppContext().registerReceiver(mReceiver, makeGlobalIntentFilter());
    }

    private static int RESULT_OK = BleDevice.RESULT_OK;
    private boolean isDeviceConnected = false;
    private boolean isOffHook;
    private boolean isRinging;

    static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public final int CHANNEL_ID_AT = 0;
    public final int CHANNEL_ID_CTRL = 1;
    public final int CHANNEL_ID_VOICE = 2;
    public final int CHANNEL_ID_SMS = 3;

    private BluetoothDevice device;

    private static final String TAG = BLEDevice.class.getSimpleName();

    void initialize(BluetoothDevice device) {
        this.device = device;
    }

    public void connect() {
        if (isConnected()) {
            NotificationHelper.notifyLog("Error", "connect(): already connected");
            return;
        }
        new SocketThread(device).start();
    }


    private void disconnectSocket() {
        SocketThread.close();
    }

    public void disconnect() {
        if (isOffHook) endCall();
        disconnectSocket();
        //TODO why unregister ever, or do it when destroyed? Application.getAppContext().unregisterReceiver(mReceiver);
    }

    public boolean isConnected() {
        return isSocketConnected() && isDeviceConnected;
    }

    public boolean isConnecting() {
        return SocketThread.isConnecting();
    }

    public boolean isSocketConnected() {
        return SocketThread.isConnected();
    }

    public void onSocketWrite(byte[] data) {
        SocketThread.write(data);
    }

    public void onGattWrite(byte channel, byte[] data) {
        Log.e(TAG, "onGattWrite: need to write gatt characteristics");
        //TODO enable this when using writeCharacteristic(mGattService.getCharacteristic(toCharacteristic(channel)), data);
    }

    public void onDeviceConnected() {
        isDeviceConnected = true;
    }

    public void onDeviceDisconnected(String error) {
        isDeviceConnected = false;
        if (isOffHook || isRinging)
            onCallDisconnected();
    }

    public abstract void onCallConnected();

    public void onCallDisconnected() {
        isOffHook = false;
        isRinging = false;
    }

    public boolean call(@NonNull String phoneNumber) {
        boolean ret = bleDevice.dial(phoneNumber) == RESULT_OK;
        if (ret) isOffHook = true;
        return ret;
    }

    public boolean answerRingingCall() {
        boolean ret = bleDevice.answer() == RESULT_OK;
        if (ret) isOffHook = true;
        return ret;
    }

    public boolean endCall() {
        return bleDevice.hangup() == RESULT_OK;
    }

    abstract void cancelBtDiscovery();

    public void onCallRinging(@Nullable String phoneNumber) {
        isRinging = true;
    }

    public abstract void onVoiceOpened();

    public abstract void onVoiceReceived(byte[] data);

    public abstract void onVoiceClosed();

    public abstract void onBatteryInfo(int level, boolean isCharging);

    public abstract void onKeyUp(int keyCode);

    public abstract void onNetOperator(int stat, @Nullable String opName, @Nullable String opNumber);

    public abstract void onNewSMS(@Nullable String phoneNumber, @Nullable String content);

    public abstract void onSendSMSResult(boolean sent);

    public abstract void onSignalStrength(int signalValue);

    public abstract void onSimPinResult(int result);

    public abstract void onSimStatus(int status);

    public abstract void onTCAuthResult(int result);

    boolean writeVoice(byte[] data) {
        return bleDevice.voiceWrite(data) == RESULT_OK; //TODO confirm
    }

    void notifyData(byte channel, byte[] data) {
        bleDevice.ble_notify_data(channel, data);
    }

    void notifyConnectionChange() {
        Log.d(TAG, "notifyConnectionChange()");
        bleDevice.bluetooth_connection_change_notify();
    }

    private static void open() {
        bleDevice.open();
    }

    //null before calling queryDeviceInfo (0)
    @Nullable
    public String getIMEI() {
        return TextUtils.clean(bleDevice.getIMEI());
    }

    @Nullable
    public String getMSISDN() {
        return TextUtils.clean(bleDevice.getMSISDN());
    }

    @Nullable
    public String getVersion() {
        return TextUtils.clean(bleDevice.getVersion());
    }

    public boolean queryBatteryInfo() {
        return bleDevice.queryDeviceInfo() == RESULT_OK;
    }

    public boolean querySignal() {
        return bleDevice.querySignal() == RESULT_OK;
    }

    public boolean sendDTMF(char c) {
        return bleDevice.sendDTMF(c) == RESULT_OK; //TODO confirm
    }

    public boolean sendSMS(String phoneNumber, String content) {
        return bleDevice.sendSMS(TextUtils.clean(phoneNumber), TextUtils.clean(content)) == RESULT_OK;
    }

    private static final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null && BluetoothService.mDeviceAddress.equals(device.getAddress())) {
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    open();
                }
            }

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    BTDevice.getInstance().connect();
                }
            }
        }
    };

    private static IntentFilter makeGlobalIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return filter;
    }
}
