package com.example.guidemyeyes;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.size.Size;

public class GuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_guide);

        //Stop button click handler
        FloatingActionButton button = (FloatingActionButton) findViewById(R.id.stopButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(GuideActivity.this, MainActivity.class);
                GuideActivity.this.startActivity(intent);
            }
        });

        //Camera init
        CameraView cameraView = findViewById(R.id.camera);
        cameraView.setLifecycleOwner(this);
        cameraView.setFocusable(false);
        cameraView.addFrameProcessor(frame -> {
            long time = frame.getTime();
            Size size = frame.getSize();
            int format = frame.getFormat();
            int userRotation = frame.getRotationToUser();
            int viewRotation = frame.getRotationToView();
            if (frame.getDataClass() == byte[].class) {
                byte[] data = frame.getData();
                // Process byte array...
            } else if (frame.getDataClass() == Image.class) {
                Image data = frame.getData();
                // Process android.media.Image...
            }
        });
    }
}