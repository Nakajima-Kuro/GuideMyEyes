package com.guidemyeyes.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.guidemyeyes.Coordinate;
import com.guidemyeyes.DepthTextureHandler;
import com.guidemyeyes.DetectionHandler;
import com.guidemyeyes.R;
import com.guidemyeyes.RadarHandler;
import com.guidemyeyes.common.helpers.CameraPermissionHelper;
import com.guidemyeyes.common.helpers.DisplayRotationHelper;
import com.guidemyeyes.common.helpers.FullScreenHelper;
import com.guidemyeyes.common.rendering.BackgroundRenderer;
import com.guidemyeyes.common.rendering.DetectionRenderer;
import com.guidemyeyes.common.rendering.RadarRenderer;

import org.tensorflow.lite.task.vision.detector.Detection;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GuideActivity extends AppCompatActivity implements GLSurfaceView.Renderer {

    private static final String TAG = GuideActivity.class.getSimpleName();

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;

    private boolean installRequested;
    private boolean isDepthSupported;

    private Session session;
    private DisplayRotationHelper displayRotationHelper;

    private final DepthTextureHandler depthTexture = new DepthTextureHandler();
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private RadarHandler radarHandler;
    private RadarRenderer radarRenderer;

    private boolean showDepthMap = true;

    private DetectionHandler detectionHandler;
    private DetectionRenderer detectionRenderer;

    private boolean devMode = false;
    private boolean enableDetection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_guide);

        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        surfaceView.setWillNotDraw(false);

        //Set up RadarHandler
        radarHandler = new RadarHandler(this);
        radarRenderer = findViewById(R.id.radarRendererLayout);

        //Set up DetectionHandler
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        detectionHandler = new DetectionHandler(this, displayMetrics);
        detectionRenderer = findViewById(R.id.detectionRendererLayout);

        installRequested = false;

        final ImageButton toggleDepthButton = findViewById(R.id.toggle_depth_button);

        //Check for preferences
        checkPreferences();

        toggleDepthButton.setOnClickListener(
                view -> {
                    if (isDepthSupported) {
                        showDepthMap = !showDepthMap;
                        toggleDepthButton.setImageIcon(showDepthMap ? Icon.createWithResource(this, R.drawable.ic_visibility_off) : Icon.createWithResource(this, R.drawable.ic_visibility));
                    } else {
                        showDepthMap = false;
                        toggleDepthButton.setEnabled(false);
                    }
                });

        //Stop button click handler
        FloatingActionButton button = findViewById(R.id.stopButton);
        button.setOnClickListener(view -> {
            surfaceView.onPause();
            session.close();
            radarHandler.destroy();
            //Redirect back to Main Activity
            Intent intent = new Intent(GuideActivity.this, MainActivity.class);
            GuideActivity.this.startActivity(intent);
        });
    }

    private void checkPreferences() {
        devMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("dev_mode", false);
//        Get View for both Development View and Normal View
        if (devMode) {
            //Hide Logo
            findViewById(R.id.guide_logo).setVisibility(View.GONE);
        } else {
            findViewById(R.id.toggle_depth_button).setVisibility(View.GONE);
        }

        enableDetection = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("enable_detection", false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                        PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                            50);
                }

                // Creates the ARCore session.
                session = new Session(/* context= */ this);
                Config config = session.getConfig();
                isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
                if (isDepthSupported) {
                    config.setDepthMode(Config.DepthMode.AUTOMATIC);
                } else {
                    config.setDepthMode(Config.DepthMode.DISABLED);
                }
                session.configure(config);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            session = null;
            return;
        }

        surfaceView.onResume();
        displayRotationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application",
                    Toast.LENGTH_LONG).show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // The depth texture is used for object occlusion and rendering.
            depthTexture.createOnGlThread();

            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(/*context=*/ this);
            backgroundRenderer.createDepthShaders(/*context=*/ this, depthTexture.getDepthTexture());
        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera frame rate.
            Frame frame = session.update();
            // Retrieves the latest depth image for this frame.
            if (isDepthSupported) {
                if(devMode){
                    depthTexture.update(frame);

                    // If frame is ready, render camera preview image to the GL surface.
                    backgroundRenderer.draw(frame);

                    // Render the depth map
                    if (showDepthMap) {
                        backgroundRenderer.drawDepth(frame);
                    }
                }

                Coordinate coor = radarHandler.renderPosition(frame);
                if (coor != null) {
                    if(devMode){
                        //Render the coordinate for closest point on screen [Dev mode]
                        radarRenderer.setCoordinate(coor);
                        radarRenderer.invalidate();
                    }
                    if(enableDetection){
                        // Load new ARCore frame into detection
                        Detection bestResult = detectionHandler.detect(frame, coor);
                        if(devMode){
                            //Render detection result
                            detectionRenderer.setDetections(bestResult);
                            detectionRenderer.invalidate();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

}