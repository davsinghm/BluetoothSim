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
                //ExtendCard.getInstance().getExtendCardApi().queryDeviceInfo();
                //ExtendCard.getInstance().getExtendCardApi().dial("00353834191001");
                //ExtendCard.getInstance().getExtendCardApi().answer();
                ExtendCard.getInstance().getExtendCardApi().queryDeviceInfo();
                ExtendCard.getInstance().getExtendCardApi().querySignal();
                //ExtendCard.getInstance().getExtendCardApi().sendSMS("+353834191001", "text sms");
            }
        });

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExtendCard.getInstance().getExtendCardApi().dial("00353834191001");

                /*ExtendCard extendCard = ExtendCard.getInstance();
                extendCard.disconnect();
                if (extendCard.initializeBluetooth(DeviceActivity.this)) {
                    extendCard.connect("36:88:06:01:08:B7");
                } else Log.e(TAG, "error while init extend card");*/

            }
        });
    }
}