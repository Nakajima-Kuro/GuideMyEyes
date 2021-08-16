package com.guidemyeyes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.Image;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.NotYetAvailableException;

import org.jetbrains.annotations.NotNull;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;

public class DetectionHandler {

    private String TAG = "DetectionHandler";
    private final int INPUT_IMAGE_SIZE = 300;

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
            imageProcessor =
                    new ImageProcessor.Builder()
                            // Resize using Bilinear or Nearest neighbour
                            .add(new ResizeOp(INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                            .build();
            ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                    .setScoreThreshold(0.6f)
                    .setNumThreads(4)
                    .build();
            objectDetector = ObjectDetector.createFromFileAndOptions(context, "lite-model_ssd_mobilenet_v1_1_metadata_2.tflite", options);

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
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
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
                    //Re-coordinated the detection to match with the screen
                    bestResult.getBoundingBox().set(
                            bestResult.getBoundingBox().left / INPUT_IMAGE_SIZE * image.getWidth(),
                            bestResult.getBoundingBox().top / INPUT_IMAGE_SIZE * image.getHeight(),
                            bestResult.getBoundingBox().right / INPUT_IMAGE_SIZE * image.getWidth(),
                            bestResult.getBoundingBox().bottom / INPUT_IMAGE_SIZE * image.getHeight()
                    );

                    //Using Text-To-Speech to read out loud that object name if it never read before
                    String label = bestResult.getCategories().get(0).getLabel();
                    if (!label.equals(objectName)) {
                        objectName = label;
                        textToSpeech.speak(label, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                    Log.i(TAG, "detect: " + bestResult.getCategories().get(0).getLabel());
                }

                return bestResult;
            } catch (NotYetAvailableException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
