package com.dsm.bluetoothsim.device;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.dsm.bluetoothsim.Application;
import com.dsm.bluetoothsim.NotificationHelper;

import java.io.IOException;
import java.io.InputStream;

public class BTDevice extends BLEDevice {

    private final String TAG = BTDevice.class.getSimpleName();

    private AudioTrack audioTrack;
    private boolean shouldMicOpen = false;

    private int AUDIO_BUFFER = 320;
    private int AUDIO_SAMPLE_RATE = 8000;

    /**
     * Device call state: No activity.
     */
    public static final int CALL_STATE_IDLE = 0;
    /**
     * Device call state: Ringing.
     * A new call arrived and is ringing or waiting. In the latter case, another call is already active.
     */
    public static final int CALL_STATE_RINGING = 1;
    /**
     * Device call state: Off-hook.
     * At least one call exists that is dialing, active, or on hold, and no calls are ringing or waiting.
     */
    public static final int CALL_STATE_OFFHOOK = 2;

    private static int phoneState;
    private static CallStats callStats;

    private static BTDevice mInstance;

    private static BTDeviceListener btDeviceListener;

    private AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
        }
    };

    public static void registerCallbacks(BluetoothDevice device, BTDeviceListener btListener) {
        btDeviceListener = btListener;
        getInstance().initialize(device);
        getInstance().connect();

        phoneState = CALL_STATE_IDLE;
    }

    public static void destroy() {
        getInstance().disconnect();
        btDeviceListener = null;

        phoneState = CALL_STATE_IDLE;
        callStats = null;

        //if (phoneState != CALL_STATE_IDLE) msgInvStWarning("unregisterCallbacks", phoneState, CALL_STATE_IDLE);
        //TODO call onCallDisconnected if not CALL_STATE_IDLE?
    }

    static {
        mInstance = new BTDevice();
        init();
    }

    public static BTDevice getInstance() {
        return mInstance;
    }

    public boolean isOffhook() {
        return phoneState == CALL_STATE_OFFHOOK;
    }

    public boolean isIdle() {
        return phoneState == CALL_STATE_IDLE;
    }

    @Override
    public void onDeviceConnected() {
        super.onDeviceConnected();
        if (btDeviceListener != null)
            btDeviceListener.onDeviceConnected();
    }

    @Override
    public void onDeviceDisconnected(String error) {
        super.onDeviceDisconnected(error);
        if (btDeviceListener != null)
            btDeviceListener.onDeviceDisconnected(error);
        Log.w("TEST", "onDeviceDisconnected()");
    }

    @Override
    public boolean call(@NonNull String phoneNumber) {
        boolean ret = super.call(phoneNumber);
        if (ret) { //TODO and !isSocketConnected()?
            btDeviceListener.onCallDialed(phoneNumber);
            callStats = new CallStats(phoneNumber, CallStats.OUTGOING_TYPE);

            phoneState = CALL_STATE_OFFHOOK;
        }
        return ret;
    }

    @Override
    public boolean answerRingingCall() {
        boolean ret = super.answerRingingCall();
        if (ret)
            phoneState = CALL_STATE_OFFHOOK;
        return ret;
    }

    @Override
    public boolean endCall() {
        if (callStats != null)
            callStats.setHangedUp(true);
        return super.endCall();
    }

    @Override
    public void onCallConnected() {
        openMic();
        callStats.setConnectedTime();

        btDeviceListener.onCallConnected(callStats.getPhoneNumber());
    }

    @Override
    public void onCallDisconnected() {
        super.onCallDisconnected();
        if (phoneState != CALL_STATE_IDLE) {
            if (phoneState == CALL_STATE_RINGING) {
                if (callStats.isHangedUp())
                    callStats.setCallType(CallStats.REJECTED_TYPE);
                else
                    callStats.setCallType(CallStats.MISSED_TYPE);
            } else {
                playHangup();
            }

            callStats.setDisconnectedTime();
            btDeviceListener.onCallDisconnected(callStats);
        }
//      else msgInvStError("onCallDisconnected", phoneState, CALL_STATE_OFFHOOK); //TODO remove

        phoneState = CALL_STATE_IDLE;
        callStats = null;
        Log.d(TAG, "onCallDisconnected() is called");
    }

    private long lastRinged = 0;

    @Override
    public void onCallRinging(@Nullable String phoneNumber) {
        super.onCallRinging(phoneNumber);
        if (lastRinged != 0 && System.currentTimeMillis() - lastRinged > 60 * 1000 && phoneState == CALL_STATE_RINGING) { //TODO remove
            msgInvStWarning("onCallRinging", CALL_STATE_RINGING, CALL_STATE_IDLE, "The Ringing state lasted > 1 min!!");
        }

        if (phoneState == CALL_STATE_IDLE) {
            callStats = new CallStats(phoneNumber, CallStats.INCOMING_TYPE);
            phoneState = CALL_STATE_RINGING;

            btDeviceListener.onCallRinging(phoneNumber);
        }

        if (phoneState == CALL_STATE_OFFHOOK) {//TODO remove warning
            msgInvStWarning("onCallRinging", CALL_STATE_OFFHOOK, CALL_STATE_IDLE);
            btDeviceListener.onCallRinging(phoneNumber);
            phoneState = CALL_STATE_RINGING;
        }

        lastRinged = System.currentTimeMillis();
    }

    @Override
    public void onBatteryInfo(int level, boolean charging) {
        btDeviceListener.onBatteryInfo(level, charging);
    }

    /**
     * @param stat   status. 4 = registered?, 2 = searching
     * @param opName operator name
     */
    @Override
    public void onNetOperator(int stat, @Nullable String opName, @Nullable String opNumber) {
        btDeviceListener.onNetOperator(stat, opName, opNumber);
    }

    private void openMic() {
        AudioManager audioManager = (AudioManager) Application.getAppContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMicrophoneMute(false);

        shouldMicOpen = true;
        new Thread(new Runnable() {
            public void run() {
                int bufferSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                bufferSize += AUDIO_BUFFER - bufferSize % AUDIO_BUFFER;
                byte[] buffer = new byte[bufferSize];
                AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
                audioRecord.startRecording();
                while (shouldMicOpen) {
                    int readResult = audioRecord.read(buffer, 0, bufferSize);
                    if (readResult > 0) {
                        boolean bool = writeVoice(buffer);
                        //if bool break;?
                    }
                }
                audioRecord.stop();
                audioRecord.release();
            }
        }).start();
    }

    private void closeMic() {
        shouldMicOpen = false;
    }

    private void playAudio() {
        AudioManager audioManager = (AudioManager) Application.getAppContext().getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            NotificationHelper.notifyLog("AudioManager", "Audio focus request not granted");
        }

        audioManager.setMode(AudioManager.MODE_IN_CALL);
        audioManager.setSpeakerphoneOn(false);
        audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, AUDIO_BUFFER, AudioTrack.MODE_STREAM);
        audioTrack.play();
    }

    private void stopAudio() {
        audioTrack.stop();
        audioTrack.release();
        AudioManager audioManager = (AudioManager) Application.getAppContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(afChangeListener);
    }

    @Override
    public void onVoiceOpened() {
        playAudio();
    }

    @Override
    public void onVoiceReceived(byte[] data) {
        audioTrack.write(data, 0, AUDIO_BUFFER);
    }

    @Override
    public void onVoiceClosed() {
        stopAudio();
        closeMic();
    }

    @Override
    public void onNewSMS(@Nullable String phoneNumber, @Nullable String content) {
        btDeviceListener.onNewSMS(phoneNumber, content);
    }

    @Override
    public void onSendSMSResult(boolean sent) {
        NotificationHelper.notifyLog("SMS Send Result", sent ? "Success" : "Fail");
    }

    @Override
    public void onSignalStrength(int signal) {
        btDeviceListener.onSignalStrength(signal);
    }

    @Override
    public void onKeyUp(int keyCode) {
        NotificationHelper.notifyLog("Key Pressed", "Key Code" + keyCode);
    }

    @Override
    public void onSimPinResult(int result) {
        NotificationHelper.notifyLog("Error", "Sim Pin result is not 1, = " + result);
    }

    // status = 1 good
    @Override
    public void onSimStatus(int status) {
        if (status != 1)
            NotificationHelper.notifyLog("SIM Status", "Sim status is not 1, status = " + status);
    }

    @Override
    public void onTCAuthResult(int result) {
        NotificationHelper.notifyLog("TC Auth Result", "Result" + result);
    }

    @Override
    void cancelBtDiscovery() {
        btDeviceListener.cancelBtDiscovery();
    }

    private void playHangup() {
        new Thread() {
            @Override
            public void run() {
                int minBufferSize = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                int bufferSize = 512;
                AudioTrack at = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
                int i;
                byte[] s = new byte[bufferSize];
                try {
                    InputStream dis = Application.getAppContext().getAssets().open("hangup.wav");

                    at.play();
                    while ((i = dis.read(s, 0, bufferSize)) > -1) {
                        at.write(s, 0, i);
                    }
                    at.stop();
                    at.release();
                    dis.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void msgInvStWarning(String function, int phoneState, int corr, String add) {
        String stat_incorr = gtState(phoneState);
        String stat_corr = gtState(corr);
        NotificationHelper.notifyLog("Warning", String.format("%s(): Invalid call state %s, should be: %s\nCorrected It!! %s", function, stat_incorr, stat_corr, add));
    }

    private void msgInvStWarning(String function, int phoneState, int corr) {
        msgInvStWarning(function, phoneState, corr, "");
    }

    private void msgInvStError(String function, int phoneState, int corr) {
        String stat_incorr = gtState(phoneState);
        String stat_corr = gtState(corr);
        NotificationHelper.notifyLog("Error", String.format("%s(): Invalid call state %s, should be: %s", function, stat_incorr, stat_corr));
    }

    private String gtState(int s) {
        switch (s) {
            case CALL_STATE_OFFHOOK:
                return "OFFHOCK";
            case CALL_STATE_RINGING:
                return "RINGING";
            case CALL_STATE_IDLE:
                return "IDLE";
            default:
                return "UNKNOWN";
        }
    }

}
