package com.droidrui.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

public class DemoActivity extends Activity {

    private SurfaceView mSurfaceView;
    private volatile boolean mRendering;
    private float mRed = 0.2f;
    private float mGreen = 0.3f;
    private float mBlue = 0.8f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mSurfaceView.getHolder().addCallback(mCallback);
        mSurfaceView.setOnTouchListener(mTouchListener);
    }

    private SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mRendering = true;
            new Thread(mTask).start();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mRendering = false;
        }
    };

    private Runnable mTask = new Runnable() {
        @Override
        public void run() {
            EGL10 egl = (EGL10) EGLContext.getEGL();
            EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            egl.eglInitialize(display, new int[2]);
            EGLConfig[] configs = new EGLConfig[1];
            egl.eglChooseConfig(display, new int[]{EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE}, configs, 1, new int[1]);
            EGLConfig config = configs[0];
            EGLSurface surface = egl.eglCreateWindowSurface(display, config, mSurfaceView.getHolder(), null);
            EGLContext context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, null);
            egl.eglMakeCurrent(display, surface, surface, context);
            GL10 gl = (GL10) context.getGL();
            while (true) {
                if (!mRendering) {
                    break;
                }
                gl.glClearColor(mRed, mGreen, mBlue, 1.0f);
                gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
                egl.eglSwapBuffers(display, surface);
            }
            egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            egl.eglDestroyContext(display, context);
            egl.eglDestroySurface(display, surface);
        }
    };

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mRed = event.getX() / v.getWidth();
            mGreen = event.getY() / v.getHeight();
            mBlue = 1.0f;
            return true;
        }
    };
}
