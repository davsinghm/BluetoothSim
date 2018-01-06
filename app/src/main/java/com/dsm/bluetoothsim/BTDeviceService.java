package com.dsm.bluetoothsim;

import android.app.Service;
import android.content.Context;

import com.dsm.bluetoothsim.device.BTDeviceListener;

/**
 * Created by dsm on 12/18/17.
 */

public class BTDeviceService {

    private Context context;
    private BTDeviceListener btDeviceListener;
    private static BTDeviceService mInstance;

    static {
        mInstance = new BTDeviceService();
    }

    private BTDeviceService() {
    }

    public static BTDeviceService getService(Context context) {
        mInstance.context = context;
        return mInstance;
    }

    public void startService(BTDeviceListener btDeviceListener) {
        this.btDeviceListener = btDeviceListener;
    }


}
