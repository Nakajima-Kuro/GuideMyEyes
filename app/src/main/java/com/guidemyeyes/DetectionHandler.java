package com.guidemyeyes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;

import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.guidemyeyes.common.helpers.YuvToRgbConverter;

import org.jetbrains.annotations.NotNull;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.List;

public class DetectionHandler {

    private ObjectDetector objectDetector;
    private TensorImage tensorImage = new TensorImage();
    private YuvToRgbConverter yuvToRgbConverter;

    private long currentTimestamp;

    public DetectionHandler(Context context) {
        //Set up TF Lite Object Detection
        try {
            currentTimestamp = 0;
            ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder().setMaxResults(10).build();
            objectDetector = ObjectDetector.createFromFileAndOptions(context, "ssd_mobilenet_v1_1_metadata_1.tflite", options);
            yuvToRgbConverter = new YuvToRgbConverter(context);
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
                Bitmap bitmapImage = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
                yuvToRgbConverter.yuvToRgb(image, bitmapImage);
                
                //Pre-processing Image
                int width = bitmapImage.getWidth();
                int height = bitmapImage.getHeight();

                int size = Math.min(height, width);
                ImageProcessor imageProcessor =
                        new ImageProcessor.Builder()
                            // Center crop the image to the largest square possible
                            .add(new ResizeWithCropOrPadOp(size, size))
                            // Resize using Bilinear or Nearest neighbour
                            .add(new ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR))
                            // Rotation counter-clockwise in 90 degree increments
                            .build();

                //Load bitmap to Tensor Image
                tensorImage.load(bitmapImage);
                tensorImage = imageProcessor.process(tensorImage);

                return objectDetector.detect(tensorImage);
            }
        }
        return null;
    }
}
