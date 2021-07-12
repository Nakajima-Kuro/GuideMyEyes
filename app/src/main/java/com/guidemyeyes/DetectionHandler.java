package com.guidemyeyes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.renderscript.RenderScript;

import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.guidemyeyes.common.helpers.ColorConvertHelper;

import org.jetbrains.annotations.NotNull;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class DetectionHandler {

    private String TAG = "DetectionHandler";

    private ObjectDetector objectDetector;
    private TensorImage tensorImage = new TensorImage();
    private RenderScript renderScript;
    private ImageProcessor imageProcessor = null;

    private long currentTimestamp;

    public DetectionHandler(Context context) {
        //Set up TF Lite Object Detection
        try {
            currentTimestamp = 0;
            ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                    .setMaxResults(5)
                    .setScoreThreshold(0.5f)
                    .build();
            objectDetector = ObjectDetector.createFromFileAndOptions(context, "model.tflite", options);
            renderScript = RenderScript.create(context);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Detection> detect(@NotNull Frame frame) throws NotYetAvailableException {
        if (frame.getTimestamp() != 0 && frame.getTimestamp() > currentTimestamp) {
            try (Image image = frame.acquireCameraImage()) {
                currentTimestamp = frame.getTimestamp();
                if (image.getFormat() != ImageFormat.YUV_420_888) {
                    throw new IllegalArgumentException(
                            "Expected image in YUV_420_888 format, got format " + image.getFormat());
                }
                //Convert Android.media.Image to bitmap (On later version of TFLite, this will be redundant)
                Bitmap bitmapImage = ColorConvertHelper.YUV_420_888_toRGBIntrinsics(renderScript, image);

                Log.i(TAG, "detect: " + bitmapImage.getWidth() + ", " + bitmapImage.getHeight());

                //Pre-processing Image
                if (imageProcessor == null) {
                    int width = bitmapImage.getWidth();
                    int height = bitmapImage.getHeight();
                    int size = Math.min(height, width);
                    imageProcessor =
                            new ImageProcessor.Builder()
                                    // Center crop the image to the largest square possible
                                    .add(new ResizeWithCropOrPadOp(size, size))
                                    // Resize using Bilinear or Nearest neighbour
                                    .add(new ResizeOp(600, 600, ResizeOp.ResizeMethod.BILINEAR))
                                    // Rotation counter-clockwise in 90 degree increments
                                    .build();
                }
                //Load bitmap to Tensor Image
                tensorImage.load(bitmapImage);
//                tensorImage = imageProcessor.process(tensorImage);

                List<Detection> results = objectDetector.detect(tensorImage);
                for (Detection result : results){

//                    Log.i("TAG", "detect: " + result.toString());
                }
                return results;
            }
        }
        return null;
    }

}
