package com.dsm.bluetoothsim;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class DeviceActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        findViewById(R.id.button_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DeviceActivity.this, BluetoothService.class);
                startService(intent);
            }
        });

        findViewById(R.id.button_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DeviceActivity.this, BluetoothService.class);
                stopService(intent);
            }
        });


        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BTDevice.getInstance().queryDeviceInfo();
                BTDevice.getInstance().querySignal();
            }
        });

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BTDevice.getInstance().dial("+919999999999");
            }
        });
    }
}