package com.dsm.bluetoothsim;

import com.dsm.bluetoothsim.util.TextUtils;

/**
 * Created by dsm on 12/18/17.
 */

public class NetworkOperators {

    public static String getName(String opCode) {
        if (opCode == null)
            return null;

        switch (TextUtils.toInteger(opCode, 0)) {
            case 27201:
                return "vodafone IE";
            case 27202:
                return "Lycamobile";
            case 27203:
                return "Meteor";
            case 27204:
                return "Access Telecom";
            case 27205:
                return "Three";
            case 27207:
                return "Eircom";
            case 27209:
                return "Clever Communications";
            default:
                return null;
        }
    }
}
