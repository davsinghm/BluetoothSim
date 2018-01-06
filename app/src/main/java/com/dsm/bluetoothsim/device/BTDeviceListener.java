package com.dsm.bluetoothsim.device;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by dsm on 12/18/17.
 */

public interface BTDeviceListener {

    void onDeviceConnected();

    void onDeviceDisconnected(String error);

    void onNewSMS(@Nullable String phoneNumber, @Nullable String content);

    void onCallRinging(@Nullable String phoneNumber);

    void onCallDialed(@NonNull String phoneNumber);

    void onCallConnected(@Nullable String phoneNumber);

    void onCallDisconnected(CallStats stats);

    void onBatteryInfo(int level, boolean isCharging);

    void onNetOperator(int stat, @Nullable String opName, @Nullable String opNumber);

    void onSignalStrength(int signal);

    void onSimStatus(int status);

    void cancelBtDiscovery();
}
