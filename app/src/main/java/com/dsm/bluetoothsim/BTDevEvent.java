package com.dsm.bluetoothsim;

/**
 * Created by dsm on 12/4/17.
 */

public enum BTDevEvent {
    STATUS(0),
    SIM_READY(1),
    INCOMING_CALL(2),
    CALL_CONNECTED(3),
    CALL_DISCONNECTED(4),
    VOICE_OPEN(5),
    VOICE_CLOSE(6),
    VOICE_RCVD(7), //TODO RCVD?
    SEND_SMS_CNF(8), //TODO rename sms sent?
    NEW_SMS(9),
    SIGNAL_STRENGTH(11),
    DEVICE_INFO(12),
    NET_OPERATOR(14),

    UNKNOWN(-1);

    public int value;

    BTDevEvent(int value) {
        this.value = value;
    }

    public static BTDevEvent getEvent(int value) {
        for (BTDevEvent event : values()) {
            if (event.value == value)
                return event;
        }
        return UNKNOWN;
    }
}