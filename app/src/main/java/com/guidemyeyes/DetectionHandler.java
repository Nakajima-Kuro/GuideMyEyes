package com.guidemyeyes;

import android.content.Context;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.renderscript.RenderScript;

import com.google.ar.core.exceptions.NotYetAvailableException;
import com.guidemyeyes.Coordinate;

import org.jetbrains.annotations.NotNull;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class DetectionHandler {

    private String TAG = "DetectionHandler";

    private ObjectDetector objectDetector;
    private TensorImage tensorImage = new TensorImage();
    private ImageProcessor imageProcessor = null;

    //Text To Speech
    String objectName = null;
    TextToSpeech textToSpeech;

    private long currentTimestamp;

    public DetectionHandler(Context context) {
        //Set up TF Lite Object Detection
        try {
            currentTimestamp = 0;
            ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                    .setScoreThreshold(0.6f)
                    .build();
            objectDetector = ObjectDetector.createFromFileAndOptions(context, "lite-model_efficientdet_lite0_detection_metadata_1.tflite", options);

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

    public Detection detect(@NotNull Bitmap image, @NotNull Coordinate coor) {
        Log.i(TAG, "detect: " + objectName);
//        Pre-processing Image
        int inputImageSize = 300;
        if (imageProcessor == null) {
            int width = image.getWidth();
            int height = image.getHeight();
            int size = Math.min(height, width);
            imageProcessor =
                    new ImageProcessor.Builder()
                            // Center crop the image to the largest square possible
//                            .add(new ResizeWithCropOrPadOp(size, size))
                            // Resize using Bilinear or Nearest neighbour
                            .add(new ResizeOp(inputImageSize, inputImageSize, ResizeOp.ResizeMethod.BILINEAR))
                            // Rotation counter-clockwise in 90 degree increments
                            .build();
        }
        //Load bitmap to Tensor Image
        tensorImage.load(image);
        tensorImage = imageProcessor.process(tensorImage);
        List<Detection> results = objectDetector.detect(tensorImage);

        //Get the best detection that contain the closet point
        Detection bestResult = null;
        for (Detection result : results) {
            //If the closet point also in the detection
            if (result.getBoundingBox().contains(
                    (float) coor.getX() / (float) coor.getWidth() * inputImageSize,
                    (float) coor.getY() / (float) coor.getHeight() * inputImageSize
            )
            ) {
                //If the new detection has higher score then the old one => more likely it the object
                if (bestResult == null || result.getCategories().get(0).getScore() > bestResult.getCategories().get(0).getScore()) {
                    bestResult = result;
                }
            }
        }

        if (bestResult != null) {
            //Re-coordinated the detection
            bestResult.getBoundingBox().set(
                    bestResult.getBoundingBox().left / inputImageSize * image.getWidth(),
                    bestResult.getBoundingBox().top / inputImageSize * image.getHeight(),
                    bestResult.getBoundingBox().right / inputImageSize * image.getWidth(),
                    bestResult.getBoundingBox().bottom / inputImageSize * image.getHeight()
            );

            //Using Text-To-Speech to read out loud that object name
            String label = bestResult.getCategories().get(0).getLabel();
            if(!label.equals(objectName)){
                objectName = label;
                textToSpeech.speak(label, TextToSpeech.QUEUE_FLUSH, null, null);
            }

        }
        return bestResult;
    }

}
