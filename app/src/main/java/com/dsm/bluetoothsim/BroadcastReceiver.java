package com.dsm.bluetoothsim;

import android.content.Context;
import android.content.Intent;

/**
 * Created by dsm on 12/18/17.
 */

public class BroadcastReceiver extends android.content.BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            context.startService(new Intent(context, BluetoothService.class));
        }
    }
}