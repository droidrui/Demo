package com.droidrui.demo;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.droidrui.demo.util.Logger;
import com.droidrui.demo.util.Toaster;

import java.io.File;
import java.util.List;

public class RecordActivity extends Activity {

    private static final int VIDEO_WIDTH = 1280;  // dimensions for 720p video
    private static final int VIDEO_HEIGHT = 720;
    private static final int DESIRED_PREVIEW_FPS = 30;

    private Camera mCamera;
    private int mCameraPreviewThousandFps;
    private SurfaceTexture mCameraTexture;

    private File mOutputFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        init();
    }

    private void init() {
        SurfaceView sv = (SurfaceView) findViewById(R.id.surface_view);
        SurfaceHolder sh = sv.getHolder();
        sh.addCallback(mCallback);

        mOutputFile = new File(Environment.getExternalStorageDirectory(), "live.mp4");
    }

    private SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Logger.e("surfaceCreated holder=" + holder);

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

    @Override
    protected void onResume() {
        super.onResume();

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
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Logger.e("releaseCamera -- done");
        }
    }
}
