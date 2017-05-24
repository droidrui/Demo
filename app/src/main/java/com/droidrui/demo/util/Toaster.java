package com.droidrui.demo.util;

import android.text.TextUtils;
import android.widget.Toast;

import com.droidrui.demo.App;

/**
 * Created by lingrui on 2017/5/24.
 */

public class Toaster {

    public static void show(String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        Toast.makeText(App.getContext(), msg, Toast.LENGTH_SHORT).show();
    }

}
