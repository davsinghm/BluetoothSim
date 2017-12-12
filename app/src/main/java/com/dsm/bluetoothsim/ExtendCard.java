package com.dsm.bluetoothsim;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by dsm on 10/29/17.
 */

public class ExtendCard {

    private static final String TAG = ExtendCard.class.getSimpleName();

    public static final String SPRD_TEDCALL_DEVICE_SERVICE = "04687561-7550-e279-ba20-cd39b7";

    public static final String TEDCALL_DEVICE_NAME = "36:88:06";
    public static final String TEDCALL_SERVICE_PREFIX = "04687561-7550-e279-ba20-";
    public static final UUID TEDCALL_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID channelATCharacteristics = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb");
    private static final UUID channelCtrlCharacteristics = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static final UUID channelSMSCharacteristics = UUID.fromString("0000fff8-0000-1000-8000-00805f9b34fb");
    private static final UUID channelVoiceCharacteristics = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb");
    private static final UUID[] ChannelCharacteristicUUIDs = {channelATCharacteristics, channelCtrlCharacteristics, channelVoiceCharacteristics, channelSMSCharacteristics};

    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattService mGattService;

//    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {//TODO check what this does in smali/java };

    private static byte toChannel(UUID characteristicUUID) {
        byte channelId = 0;
        UUID[] uuids = ChannelCharacteristicUUIDs;
        while (channelId < uuids.length) {
            if (uuids[channelId].equals(characteristicUUID))
                return channelId;

            channelId = (byte) (channelId + 1);
        }
        Log.e(TAG, "asd: toChannel: unknown uuid " + characteristicUUID.toString());
        return -1;
    }

    private static UUID toCharacteristic(byte channelID) {
        if (channelID >= ChannelCharacteristicUUIDs.length) {
            Log.e(TAG, "asd: " + "toCharacteristic: error channel id" + channelID);
            return null;
        }
        return ChannelCharacteristicUUIDs[channelID];
    }

   /* public boolean connect(BluetoothSocket socket) {
        if (mSppSocket != null) {
            Log.e(TAG, "Past SPP socket didn't close");
            return false;
        }
        mSppSocket = socket;
        mBTDeviceApi.setSocket(socket);

        Log.e(TAG, "Connecting to Socket");

        mConnectionState = STATE_CONNECTING *//*1*//*;
        new Thread(new Runnable() {
            public void run() {
                try {
                    //mBluetoothAdapter.cancelDiscovery();
                    mSppSocket.connect();
                    Log.w(TAG, "Connected Socket");
                    mConnectionState = STATE_CONNECTED *//*2*//*;
                    byte[] buffer = new byte[0x800];
                    Log.d(TAG, "calling bluetooth_connection_change_notify()");
                    mBTDeviceApi.bluetooth_connection_change_notify();
                    int n;
                    while ((n = mSppSocket.getInputStream().read(buffer)) > 0) {
                        byte[] temp = new byte[n];
                        System.arraycopy(buffer, 0, temp, 0, n);
                        BTDevice.printHexString("spp recv:", temp);
                        mBTDeviceApi.ble_notify_data((byte) 64, temp);
                        *//*try {
                            Thread.sleep(10L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }*//*
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    mSppSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                mConnectionState = STATE_DISCONNECTED *//*0*//*;
                Log.d(TAG, "calling bluetooth_connection_change_notify()");
                mBTDeviceApi.bluetooth_connection_change_notify();
                mSppSocket = null;
            }
        }).start();
        return true;
        *//*} catch (IOException e) {
            e.printStackTrace();
            mSppSocket = null;
        }*//*
        //return false;
    }
*/

    //TODO confirmed but, find where this is used? in smali
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothGatt != null)
            mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    //TODO confirmed but, find where this is used? in smali
    //TODO enable last line in jni_callback_ble_write_data when testing this
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
        characteristic.setValue(data);
        return mBluetoothGatt != null && mBluetoothGatt.writeCharacteristic(characteristic);
    }
}
