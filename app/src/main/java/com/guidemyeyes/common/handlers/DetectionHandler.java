package com.guidemyeyes.common.handlers;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;

import com.google.ar.core.Frame;
import com.google.ar.core.ImageFormat;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.guidemyeyes.Coordinate;
import com.guidemyeyes.common.helpers.ColorConvertHelper;

import org.jetbrains.annotations.NotNull;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class DetectionHandler {

    private String TAG = "DetectionHandler";
    private final int INPUT_IMAGE_SIZE = 300;

    //Object Detection
    private ObjectDetector objectDetector;
    private TensorImage tensorImage = new TensorImage();
    private ImageProcessor imageProcessor = null;

    //Convert ARCore Image to Bitmap
    private ColorConvertHelper colorConvertHelper;
    private DisplayMetrics displayMetrics;

    //Text To Speech
    String objectName = null;
    TextToSpeech textToSpeech;

    private long currentTimestamp;

    public DetectionHandler(Context context, DisplayMetrics displayMetrics) {
        //Set up TF Lite Object Detection
        try {
            currentTimestamp = 0;
            imageProcessor =
                    new ImageProcessor.Builder()
                            // Resize using Bilinear or Nearest neighbour
                            .add(new ResizeOp(INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                            //Rotate 90 degrees
                            .add(new Rot90Op(3))
                            .build();
            ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                    .setScoreThreshold(0.6f)
                    .setNumThreads(4)
                    .build();
            objectDetector = ObjectDetector.createFromFileAndOptions(context, "lite-model_ssd_mobilenet_v1_1_metadata_2.tflite", options);
            colorConvertHelper = new ColorConvertHelper(context);
            this.displayMetrics = displayMetrics;
            //Create new TextToSpeech
            textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        textToSpeech.setLanguage(Locale.UK);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Detection detect(@NotNull Frame frame, @NotNull Coordinate coor) {
        if (frame.getTimestamp() != 0 && frame.getTimestamp() > currentTimestamp) {
            try (Image image = frame.acquireCameraImage()) {
                currentTimestamp = frame.getTimestamp();
                //Convert Image to Bitmap
                if (image.getFormat() != ImageFormat.YUV_420_888) {
                    throw new IllegalArgumentException(
                            "Expected image in YUV_420_888 format, got format " + image.getFormat());
                }
                //Convert Android.media.Image to bitmap (On later version of TFLite, this will be redundant)
                Bitmap bitmapImage = colorConvertHelper.YUV_420_888_toRGBIntrinsics(image);

                //Load bitmap to Tensor Image
                tensorImage.load(bitmapImage);
                tensorImage = imageProcessor.process(tensorImage);
                //Detect
                List<Detection> results = objectDetector.detect(tensorImage);

                //Get the best detection that contain the closet point
                Detection bestResult = null;
                for (Detection result : results) {
                    //If the closet point also in the detection
                    if (result.getBoundingBox().contains(
                            (float) coor.getX() / (float) coor.getWidth() * INPUT_IMAGE_SIZE,
                            (float) coor.getY() / (float) coor.getHeight() * INPUT_IMAGE_SIZE
                    )
                    ) {
                        //If the new detection has higher score then the old one => more likely it the object
                        if (bestResult == null || result.getCategories().get(0).getScore() > bestResult.getCategories().get(0).getScore()) {
                            bestResult = result;
                        }
                    }
                }

                if (bestResult != null) {
                    //Re-coordinated the detection to match with the screen size
                    bestResult.getBoundingBox().set(
                            bestResult.getBoundingBox().left / INPUT_IMAGE_SIZE * displayMetrics.widthPixels,
                            bestResult.getBoundingBox().top / INPUT_IMAGE_SIZE * displayMetrics.heightPixels,
                            bestResult.getBoundingBox().right / INPUT_IMAGE_SIZE * displayMetrics.widthPixels,
                            bestResult.getBoundingBox().bottom / INPUT_IMAGE_SIZE * displayMetrics.heightPixels
                    );

                    //Using Text-To-Speech to read out loud that object name if it never read before
                    String label = bestResult.getCategories().get(0).getLabel();
                    if (!label.equals(objectName)) {
                        objectName = label;
                        textToSpeech.speak(label, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
                return bestResult;
            } catch (NotYetAvailableException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void destroy(){
        this.textToSpeech.shutdown();
        this.objectDetector.close();
    }

}
