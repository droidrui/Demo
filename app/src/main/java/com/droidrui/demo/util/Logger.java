package com.droidrui.demo.util;

import android.util.Log;

/**
 * Created by lingrui on 2017/5/24.
 */

public class Logger {

    private static final String TAG = "Logger";

    private static boolean sDebug = true;

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }

    public static void e(String msg) {
        if (sDebug) {
            Log.e(TAG, msg);
        }
    }

}
