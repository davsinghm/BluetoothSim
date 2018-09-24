package com.dsm.bluetoothsim.device;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.dsm.bluetoothsim.NotificationHelper;
import com.tedcall.sdk.BleDevice;

import java.io.IOException;
import java.io.InputStream;


class SocketThread extends Thread {

    private final String TAG = SocketThread.class.getSimpleName();
    private BTDevice btDevice = BTDevice.getInstance();
    private static BluetoothDevice device;
    private static BluetoothSocket socket;

    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final int STATE_DISCONNECTED = 0;
    static final int STATE_MTU_CHANGED = 4;

    private static int state = STATE_DISCONNECTED;

    SocketThread(BluetoothDevice device) {
        SocketThread.device = device;
    }

    static void write(byte[] data) {
        if (socket != null)
            try {
                socket.getOutputStream().write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    static boolean isConnected() {
        //return(SocketThread.getConnectionState()&0x2)==STATE_CONNECTED;
        return socket != null && socket.isConnected() && state == STATE_CONNECTED;
    }

    static boolean isConnecting() {
        return state == STATE_CONNECTING;
    }

    static void close() {
        if (socket != null)
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        state = STATE_DISCONNECTED;
    }

    @Override
    public void run() {
        if (state != STATE_DISCONNECTED) {
            NotificationHelper.notifyLog("Error", "Socket Thread is already running. Attempt to create a new thread. Terminating");
            return;
        }

        state = STATE_CONNECTING;

        try (BluetoothSocket bluetoothSocket = (SocketThread.socket = device.createRfcommSocketToServiceRecord(BLEDevice.UUID_SPP))) {
            byte[] buffer = new byte[0x800];
            btDevice.cancelBtDiscovery();
            if (bluetoothSocket != null) {
                bluetoothSocket.connect();
                state = STATE_CONNECTED;
                Log.d(TAG, "calling notifyConnectionChange()");
                btDevice.notifyConnectionChange();

                InputStream inputStream = bluetoothSocket.getInputStream();
                int n;
                while ((n = inputStream.read(buffer)) > 0) {
                    byte[] bytes = new byte[n];
                    System.arraycopy(buffer, 0, bytes, 0, n);
                    BleDevice.printHexString("spp recv:", bytes);
                    btDevice.notifyData(BleDevice.CHANNEL_ID_BCSP_MUX, bytes);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (state == STATE_CONNECTING)
            btDevice.onDeviceDisconnected("SocketThread"); //TODO merge with bluetooth_connection_change_notify

        state = STATE_DISCONNECTED;
        socket = null;
        btDevice.notifyConnectionChange();
    }

}
