package com.catadmirer.infuseSMP.util;

public class MessageUtil {
    public static String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (minutes == 0) {
            return String.valueOf(seconds);
        }

        return minutes + ":" + String.format("%02d", seconds);
    }
}
