package com.dsm.bluetoothsim.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dsm.bluetoothsim.BluetoothService;
import com.dsm.bluetoothsim.R;
import com.dsm.bluetoothsim.device.BTDevice;
import com.dsm.bluetoothsim.ui.widget.DialPadLayout;
import com.dsm.bluetoothsim.util.CallLogUtility;

import net.frakbot.glowpadbackport.GlowPadView;

public class PhoneActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener, GlowPadView.OnTriggerListener, DialPadLayout.OnInputListener {
    private static final String TAG = PhoneActivity.class.getSimpleName();
    private static final int PROXIMITY_VALUE = 4;
    private static final int GLOW_PAD_ANIMATION_DURATION = 1350;
    private BTDevice btDevice;
    private Handler handler;
    private GlowPadView glowPadView;
    private DialPadLayout dialPadLayout;
    private TextView tvStatus;
    private AudioManager audioManager;
    private SensorManager sensorManager;
    private PowerManager.WakeLock wakeLock;
    private PowerManager powerManager;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothService.ACTION_CALL_DISCONNECTED.equals(action)) {
                finishActivityDelayed(1500);
            } else if (BluetoothService.ACTION_CALL_HANGED_UP.equals(action)) {
                finishActivityDelayed(0);
            } else if (BluetoothService.ACTION_CALL_CONNECTED.equals(action)) {
                tvStatus.setText(R.string.ongoing_call);
                updateViewVisibilities(true);
            }
        }
    };

    private void finishActivityDelayed(int delay) {
        tvStatus.setText(R.string.call_ended);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, delay);
    }

    private Runnable glowPadRunnable = new Runnable() {
        @Override
        public void run() {
            glowPadView.ping();
            handler.postDelayed(this, GLOW_PAD_ANIMATION_DURATION);
        }
    };

    private void showOnCallLayout() {
        findViewById(R.id.button_mic).setVisibility(View.VISIBLE);
        findViewById(R.id.button_pad).setVisibility(View.VISIBLE);
        findViewById(R.id.button_speaker).setVisibility(View.VISIBLE);
        findViewById(R.id.button_add_call).setVisibility(View.VISIBLE);
        findViewById(R.id.tv_mic).setVisibility(View.VISIBLE);
        findViewById(R.id.tv_pad).setVisibility(View.VISIBLE);
        findViewById(R.id.tv_speaker).setVisibility(View.VISIBLE);
        findViewById(R.id.tv_add_call).setVisibility(View.VISIBLE);
        findViewById(R.id.fab_end_call).setVisibility(View.VISIBLE);
    }

    private void hideGlowPad() {
        glowPadView.suspendAnimations();
        handler.removeCallbacks(glowPadRunnable);
        glowPadView.setVisibility(View.GONE);
    }

    private void updateViewVisibilities(boolean isOffHook) {
        if (isOffHook) {
            hideGlowPad();
            showOnCallLayout();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);
        btDevice = BTDevice.getInstance();
        handler = new Handler(Looper.getMainLooper());

        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON | LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        dialPadLayout = new DialPadLayout(this, this);
        tvStatus = findViewById(R.id.call_status);
        glowPadView = findViewById(R.id.glow_pad_view);
        glowPadView.setOnTriggerListener(this);
        findViewById(R.id.fab_end_call).setOnClickListener(this);
        findViewById(R.id.button_pad).setOnClickListener(this);
        findViewById(R.id.button_speaker).setOnClickListener(this);
        findViewById(R.id.button_mic).setOnClickListener(this);

        handler.post(glowPadRunnable);
        updateViewVisibilities(btDevice.isOffhook());
        setContactInfo();

        if (btDevice.isOffhook())
            tvStatus.setText(R.string.calling);
        else tvStatus.setText(R.string.call_from); //TODO
    }

    private void setContactInfo() {
        String phoneNumber = getIntent().getStringExtra("PHONE_NUMBER");
        String displayName = CallLogUtility.getContactDisplayName(this, phoneNumber);

        Bitmap contactPicture = CallLogUtility.getContactDisplayPhoto(this, phoneNumber);
        ((ImageView) findViewById(R.id.image_view)).setImageBitmap(contactPicture);

        if (phoneNumber == null)
            phoneNumber = "Unknown";

        TextView callerNameTV = findViewById(R.id.caller_name);
        if (displayName != null) {
            callerNameTV.setText(displayName);
            ((TextView) findViewById(R.id.caller_number)).setText(phoneNumber);
            ((TextView) findViewById(R.id.caller_number_label)).setText("Main"); //TODO
        } else {
            callerNameTV.setText(phoneNumber);
            findViewById(R.id.caller_number).setVisibility(View.GONE);
            findViewById(R.id.caller_number_label).setVisibility(View.GONE);
        }
    }

    @Override
    public void onInput(View buttonView, char c) {
        btDevice.sendDTMF(c);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, makeIntentFilter());
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);

        if (btDevice.isIdle()) {
            tvStatus.setText("Call state idle"); //TODO navigate up
        }
    }

    private IntentFilter makeIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_CALL_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_CALL_DISCONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_CALL_HANGED_UP);
        return intentFilter;
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        sensorManager.unregisterListener(this);

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_end_call:
                tvStatus.setText(R.string.hanging_up);
                if (!btDevice.endCall())
                    Toast.makeText(this, R.string.device_not_connected, Toast.LENGTH_LONG).show();
                break;
            case R.id.button_pad:
                dialPadLayout.openPage();
                break;
            case R.id.button_speaker:
                toggleSpeakerphone();
                break;
            case R.id.button_mic:
                toggleMic();
                break;
        }
    }

    private void toggleMic() {
        audioManager.setMicrophoneMute(!audioManager.isMicrophoneMute());
        Toast.makeText(this, "Is Mic Mute: " + audioManager.isMicrophoneMute(), Toast.LENGTH_SHORT).show();
    }

    private void toggleSpeakerphone() {
        audioManager.setSpeakerphoneOn(!audioManager.isSpeakerphoneOn());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float val = event.values[0];
        if (val >= -PROXIMITY_VALUE && val <= PROXIMITY_VALUE) {
            wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG);
            wakeLock.acquire();
        } else {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onGrabbed(View v, int handle) {
        handler.removeCallbacks(glowPadRunnable);
    }

    @Override
    public void onTrigger(View v, int target) {
        if (glowPadView.getResourceIdForTarget(target) == R.drawable.ic_lockscreen_answer) {
            if (!btDevice.answerRingingCall()) {
                Toast.makeText(this, R.string.device_not_connected, Toast.LENGTH_LONG).show();
                glowPadView.reset(false);
            }
        } else {
            if (!btDevice.endCall()) {
                Toast.makeText(this, R.string.device_not_connected, Toast.LENGTH_LONG).show();
                glowPadView.reset(false);
            }
        }
    }

    @Override
    public void onFinishFinalAnimation() {
        handler.postDelayed(glowPadRunnable, GLOW_PAD_ANIMATION_DURATION);
    }

    @Override
    public void onGrabbedStateChange(View v, int handle) {
    }

    @Override
    public void onReleased(View v, int handle) {
    }
}
