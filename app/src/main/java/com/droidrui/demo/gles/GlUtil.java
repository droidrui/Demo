package com.droidrui.demo.gles;

import android.opengl.GLES20;

import com.droidrui.demo.util.Logger;

/**
 * Created by lingrui on 2017/5/25.
 */

public class GlUtil {

    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Logger.e(msg);
            throw new RuntimeException(msg);
        }
    }

}
