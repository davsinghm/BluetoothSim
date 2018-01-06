package com.dsm.bluetoothsim.device;

import android.os.Build;
import android.provider.CallLog;

/**
 * Created by dsm on 12/21/17.
 */

public class CallStats {

    private int callType;
    private String phoneNumber;
    private long startTime;
    private long connectedTime;
    private long disconnectedTime;
    boolean hangedUp;

    /**
     * CallStats log type for incoming calls.
     */
    public static final int INCOMING_TYPE = CallLog.Calls.INCOMING_TYPE;
    /**
     * CallStats log type for outgoing calls.
     */
    public static final int OUTGOING_TYPE = CallLog.Calls.OUTGOING_TYPE;
    /**
     * CallStats log type for missed calls.
     */
    public static final int MISSED_TYPE = CallLog.Calls.MISSED_TYPE;
    /**
     * CallStats log type for calls rejected by direct user action.
     */
    public static final int REJECTED_TYPE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? CallLog.Calls.REJECTED_TYPE : CallLog.Calls.REJECTED_TYPE;


    public CallStats(String phoneNumber, int callType) {
        this.phoneNumber = phoneNumber;
        this.callType = callType;
        this.startTime = System.currentTimeMillis();
    }

    public String getPhoneNumber() {
        return phoneNumber;


    }

    public int getCallType() {
        return callType;
    }

    /*void setStartTime() {
        this.startTime = System.currentTimeMillis();
    }*/

    void setConnectedTime() {
        this.connectedTime = System.currentTimeMillis();
    }

    void setDisconnectedTime() {
        this.disconnectedTime = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return startTime;
    }

    public int getDuration() {
        int ret = (int) ((disconnectedTime - connectedTime) / 1000);
        if (ret < 0 || connectedTime == 0)
            ret = 0;

        return ret;
    }

    void setCallType(int callType) {
        this.callType = callType;
    }

    public boolean isHangedUp() {
        return hangedUp;
    }

    void setHangedUp(boolean hangedUp) {
        this.hangedUp = hangedUp;
    }
}
