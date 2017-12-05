package com.dsm.bluetoothsim;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.dsm.bluetoothsim.ui.PhoneActivity;
import com.tedcall.sdk.BTDevice;


public class BTDeviceApi extends BTDevice {

    private final String TAG = ExtendCard.class.getSimpleName();

    public static final String ACTION_CONNECTED = Application.class.getPackage() + ".ACTION_CONNECTED";
    public static final String ACTION_DISCONNECTED = Application.class.getPackage() + ".ACTION_DISCONNECTED";
    public static final String ACTION_DEVICE_INFO = Application.class.getPackage() + ".ACTION_DEVICE_INFO";
    public static final String ACTION_EVENT = Application.class.getPackage() + ".EVENT";
    public static final String ACTION_OUTGOING_CALL = Application.class.getPackage() + ".ACTION_OUTGOING_CALL";
    public static final String ACTION_INCOMING_CALL = Application.class.getPackage() + ".ACTION_INCOMING_CALL";
    public static final String ACTION_NET_OPERATOR = Application.class.getPackage() + ".ACTION_NET_OPERATOR";
    public static final String ACTION_NEW_SMS = Application.class.getPackage() + ".ACTION_NEW_SMS";
    public static final String ACTION_SEND_SMS = Application.class.getPackage() + ".ACTION_SEND_SMS";
    public static final String ACTION_SIGNAL_LENGTH = Application.class.getPackage() + ".ACTION_SIGNAL_LENGTH";
    public static final String ACTION_SIM_STATUS = Application.class.getPackage() + ".ACTION_SIM_STATUS";
    public static final String ACTION_VOICE_BACK = Application.class.getPackage() + ".ACTION_VOICE_BACK";

    static {
        mInstance = new BTDeviceApi();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(Application.getAppContext());
    }

    private boolean voiceCodecRunning = false;
    private BluetoothSocket mSppSocket;
    private static BTDeviceApi mInstance;
    private static LocalBroadcastManager mLocalBroadcastManager;

    public static BTDeviceApi getInstance() {
        return mInstance;
    }

    public void setSocket(BluetoothSocket socket) {
        mSppSocket = socket;
    }

    @Override
    public int jni_callback_ble_exist_sms_channel() {
        return 1;
    }

    @Override
    public int jni_callback_ble_is_connected() {
        if ((ExtendCard.getInstance().mConnectionState & 0x2) == ExtendCard.STATE_CONNECTED)
            return 1;

        return 0;
    }

    @Override
    public void jni_callback_ble_write_data(byte channel, byte[] data) {
        Log.d(TAG, "jni_callback_ble_write_data");
        printHexString("channel:" + channel + " data:", data);
        if (channel == 64) {
            try {
                if (mSppSocket != null)
                    mSppSocket.getOutputStream().write(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        } else
            Log.e(TAG, "jni_callback_ble_write_data: need to write gatt characteristics");
        //TODO enable this when using writeCharacteristic(mGattService.getCharacteristic(toCharacteristic(channel)), data);
    }

    @Override
    public void onDeviceInfo(int level, int charging) {
        super.onDeviceInfo(level, charging);
        Intent intent = new Intent(ACTION_DEVICE_INFO);
        intent.putExtra("LEVEL", level);
        intent.putExtra("IS_CHARGING", charging == 1);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onDeviceStatus(int status) {
        super.onDeviceStatus(status);
        Intent intent = new Intent(status == 1 ? ACTION_CONNECTED : ACTION_DISCONNECTED);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public int dial(String phoneNumber) {
        Intent intent = new Intent(ACTION_OUTGOING_CALL);
        intent.putExtra("PHONE_NUMBER", phoneNumber);
        mLocalBroadcastManager.sendBroadcast(intent);
        return super.dial(phoneNumber);
    }

    @Override
    public void onEvent(int event) {
        super.onEvent(event);
        Log.i(TAG, "onEvent:" + BTDevEvent.getEvent(event));
        mLocalBroadcastManager.sendBroadcast(new Intent(ACTION_EVENT).putExtra("EVENT", event));

        switch (event) {
            case EVENT_CALL_CONNECTED /*3*/:
                break;
            case EVENT_CALL_DISCONNECTED /*4*/:
                break;
            case EVENT_VOICE_OPEN /*5*/:
                //added
                AudioManager audioManager = (AudioManager) Application.getAppContext().getSystemService(Context.AUDIO_SERVICE);
                // audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
                //int originalVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

                audioTrack.play();

                //moved from CALL_CONNECTED
                voiceCodecRunning = true; //TODO move to OPEN?
                new Thread(new Runnable() {
                    public void run() {
                        int bufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                        Log.i("AudioRecord", "bufferSize:" + bufferSize);
                        bufferSize += 320 - bufferSize % 320;
                        Log.i("AudioRecord", "bufferSize adjust:" + bufferSize);
                        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
                        audioRecord.startRecording();
                        byte[] buffer1 = new byte[bufferSize];
                        while (voiceCodecRunning) {
                            int readResult = audioRecord.read(buffer1, 0, bufferSize);
                            if (readResult > 0) {
                                Log.i("AudioRecord", "readResult:" + readResult);
                                voiceWrite(buffer1);
                            }
                        }
                        audioRecord.stop();
                        audioRecord.release();
                    }
                }).start();

                break;
            case EVENT_VOICE_CLOSE /*6*/:
                voiceCodecRunning = false;
                break;
        }

    }

    @Override
    public void onIncomingCall(String phoneNumber) {
        super.onIncomingCall(phoneNumber);
        /* if (blocked_number) {
            mExtendCardApi.hangup();
            return;
        }*/

        /*Intent intent = new Intent(ACTION_INCOMING_CALL);
        intent.putExtra("incoming_call", phoneNumber);
        Intent p = new Intent(mContext, AnswerPhone.class);
        mContext.startActivity(p);*/


        Intent intent = new Intent(ACTION_INCOMING_CALL);
        intent.putExtra("PHONE_NUMBER", phoneNumber);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onNetOperator(int stat, String opName, String opNumber) {
        super.onNetOperator(stat, opName, opNumber);
        /*Intent intent = new Intent(ACTION_NET_OPERATOR);
        intent.putExtra("status", stat);
        intent.putExtra("code", opNumber);*/
        PhoneActivity.notifySMS(Application.getAppContext(), "onNetOperator is Called", "Stat: " + stat + ", OpName: " + opName + ", OpNo: " + opNumber);
    }

    @Override
    public void onNewSMS(String phoneNumber, String content) {
        super.onNewSMS(phoneNumber, content);
        Intent intent = new Intent(ACTION_NEW_SMS);
        intent.putExtra("PHONE_NUMBER", phoneNumber);
        intent.putExtra("CONTENT", content);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onSendSMSCnf(int result) {
        super.onSendSMSCnf(result);
        /*Intent intent = new Intent(ACTION_SEND_SMS);
        intent.putExtra("SMS", result);*/
        if (result == 0)
            PhoneActivity.notifySMS(Application.getAppContext(), "SMS Sent Failed", "Result " + result);
    }

    @Override
    public void onSignalStrength(int signalValue) {
        super.onSignalStrength(signalValue);
        /*Intent localIntent = new Intent(ACTION_SIGNAL_LENGTH);
        localIntent.putExtra("signalValue", signalValue);*/
        PhoneActivity.notifySMS(Application.getAppContext(), "onSignalStrength is Called", "SignalValue " + signalValue);
    }

    @Override
    public void onSimStatus(int status) {
        super.onSimStatus(status);
        /*Intent localIntent = new Intent(ACTION_SIM_STATUS);
        localIntent.putExtra("ACTION_SIM_STATUS", status);*/
    }

    //TODO reset params, move
    private AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL /*0*/, 8000, AudioFormat.CHANNEL_OUT_MONO /*4*/, AudioFormat.ENCODING_PCM_16BIT /*2*/, 320, AudioTrack.MODE_STREAM /*1*/);

    @Override
    public void onVoiceCallback(byte[] data) {
        super.onVoiceCallback(data);

        audioTrack.getStreamType();
        audioTrack.write(data, 0, 320);

        /*Intent localIntent = new Intent(ACTION_VOICE_BACK);
        localIntent.putExtra("voice_data", data);*/
    }
}

