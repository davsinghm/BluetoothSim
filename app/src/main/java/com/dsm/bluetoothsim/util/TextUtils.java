package com.dsm.bluetoothsim.util;

/**
 * Created by dsm on 12/15/17.
 */

public class TextUtils {

    public static String clean(String string) {
        if (string != null) {
            string = string.trim();
            if (string.isEmpty())
                return null;
        }

        return string;
    }

    public static int toInteger(String string, int def) {
        try {
            return Integer.valueOf(string);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static boolean isNumeric(String string) {
        return string.matches("\\+?-?\\d+(\\.\\d+)?");
    }
}
