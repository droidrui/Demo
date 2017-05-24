package com.droidrui.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void demo(View v) {
        startActivity(new Intent(this, DemoActivity.class));
    }

    public void record(View v) {
        startActivity(new Intent(this, RecordActivity.class));
    }

    public void play(View v) {
        startActivity(new Intent(this, PlayActivity.class));
    }

    private void test() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new CameraToMpegTest().encodeCameraToMpeg();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
