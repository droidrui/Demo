package com.droidrui.demo;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.droidrui.demo.gles.EglCore;
import com.droidrui.demo.gles.WindowSurface;
import com.droidrui.demo.util.Logger;
import com.droidrui.demo.util.Toaster;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class RecordActivity extends Activity {

    private static final int VIDEO_WIDTH = 1280;  // dimensions for 720p video
    private static final int VIDEO_HEIGHT = 720;
    private static final int DESIRED_PREVIEW_FPS = 30;

    private SurfaceView mSurfaceView;

    private EglCore mEglCore;
    private WindowSurface mDisplaySurface;
    private SurfaceTexture mCameraTexture;
    private WindowSurface mEncoderSurface;
    private int mTextureId;


    private Camera mCamera;
    private int mCameraPreviewThousandFps;

    private MainHandler mHandler;
    private File mOutputFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        init();
    }

    private void init() {
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        SurfaceHolder sh = mSurfaceView.getHolder();
        sh.addCallback(mCallback);

        mHandler = new MainHandler(this);
        mOutputFile = new File(Environment.getExternalStorageDirectory(), "live.mp4");
    }

    private SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Logger.e("surfaceCreated holder=" + holder);
            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);
            mDisplaySurface = new WindowSurface(mEglCore, holder.getSurface(), false);
            mDisplaySurface.makeCurrent();
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            mTextureId = textures[0];
            mCameraTexture = new SurfaceTexture(mTextureId);
            mCameraTexture.setOnFrameAvailableListener(mFrameAvailableListener);

            Logger.e("starting camera preview");
            try {
                mCamera.setPreviewTexture(mCameraTexture);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            mCamera.startPreview();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Logger.e("surfaceChanged fmt=" + format + " size=" + width + "x" + height
                    + " holder=" + holder);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Logger.e("surfaceDestroyed holder=" + holder);
        }
    };

    private SurfaceTexture.OnFrameAvailableListener mFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            mHandler.sendEmptyMessage(MainHandler.MSG_FRAME_AVAILABLE);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        openCamera(VIDEO_WIDTH, VIDEO_HEIGHT, DESIRED_PREVIEW_FPS);
    }

    private void openCamera(int desiredWidth, int desiredHeight, int desiredFps) {
        if (mCamera != null) {
            throw new RuntimeException("onResume camera not null");
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                break;
            }
        }
        if (mCamera == null) {
            Logger.e("No front-facing camera found; Open default camera");
            mCamera = Camera.open();
        }
        if (mCamera == null) {
            Toaster.show("打开摄像头失败");
            return;
        }
        Camera.Parameters parms = mCamera.getParameters();
        choosePreviewSize(parms, desiredWidth, desiredHeight);
        mCameraPreviewThousandFps = chooseFixedPreviewFps(parms, desiredFps * 1000);
        parms.setRecordingHint(true);
        mCamera.setParameters(parms);

        Camera.Size cameraPreviewSize = parms.getPreviewSize();
        String previewFacts = cameraPreviewSize.width + "x" + cameraPreviewSize.height
                + " @" + (mCameraPreviewThousandFps / 1000.0f) + "fps";
        Logger.e("Camera config: " + previewFacts);
    }

    private void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Logger.e("Camera prefered preview size for video is "
                    + ppsfv.width + "x" + ppsfv.height);
        }

        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }

        Logger.e("Unable to set preview size to " + width + "x" + height);
        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }
    }

    private int chooseFixedPreviewFps(Camera.Parameters parms, int desiredThousandFps) {
        List<int[]> supported = parms.getSupportedPreviewFpsRange();
        for (int[] entry : supported) {
            Logger.e("entry: " + entry[0] + "-" + entry[1]);
            if ((entry[0] == entry[1]) && (entry[0] == desiredThousandFps)) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                return entry[0];
            }
        }
        int[] tmp = new int[2];
        parms.getPreviewFpsRange(tmp);
        int guess;
        if (tmp[0] == tmp[1]) {
            guess = tmp[0];
        } else {
            guess = tmp[1] / 2;
        }
        Logger.e("Could't find match for " + desiredThousandFps + ", using " + guess);
        return guess;
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();

        if (mCameraTexture != null) {
            mCameraTexture.release();
            mCameraTexture = null;
        }

        if (mDisplaySurface != null) {
            mDisplaySurface.release();
            mDisplaySurface = null;
        }

        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        Logger.e("onPause() done");
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Logger.e("releaseCamera -- done");
        }
    }

    private static class MainHandler extends Handler {

        public static final int MSG_FRAME_AVAILABLE = 1;

        private WeakReference<RecordActivity> mWeakActivity;

        public MainHandler(RecordActivity activity) {
            mWeakActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            RecordActivity activity = mWeakActivity.get();
            if (activity == null) {
                Logger.e("Got message for dead activity");
                return;
            }
            switch (msg.what) {
                case MSG_FRAME_AVAILABLE: {
                    activity.drawFrame();
                    break;
                }
            }
        }
    }

    private void drawFrame() {
        if (mEglCore == null) {
            Logger.e("Skipping drawFrame after shutdown");
            return;
        }
        mDisplaySurface.makeCurrent();
        mCameraTexture.updateTexImage();

        int viewWidth = mSurfaceView.getWidth();
        int viewHeight = mSurfaceView.getHeight();
        GLES20.glViewport(0, 0, viewWidth, viewHeight);
        mDisplaySurface.swapBuffers();
    }
}
