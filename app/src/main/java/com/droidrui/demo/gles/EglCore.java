package com.droidrui.demo.gles;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.GLUtils;

/**
 * Created by lingrui on 2017/5/24.
 */

public class EglCore {

    private static final String TAG = "EglCore";

    public static final int FLAG_RECORDABLE = 0x01;

    public static final int FLAG_TRY_GLES3 = 0x02;

    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLConfig mEGLConfig = null;
    private int mGlVersion = -1;

    public EglCore(){
        this(null, 0);
    }

    public EglCore(EGLContext sharedContext, int flags){
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY){
            throw new RuntimeException("EGL already set up");
        }
        if (sharedContext == null){
            sharedContext = EGL14.EGL_NO_CONTEXT;
        }
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY){
            throw new RuntimeException("unable to get EGL14 display");
        }
    }

}
