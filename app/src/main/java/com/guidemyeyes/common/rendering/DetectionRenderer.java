package com.guidemyeyes.common.rendering;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import org.tensorflow.lite.task.vision.detector.Detection;

import java.util.List;

public class DetectionRenderer extends View {

    // defines paint and canvas
    private Paint drawPaint;
    // list of detection
    private List<Detection> detections;

    public DetectionRenderer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        detections = null;
        setupPaint();
    }

    // Setup paint with color and stroke styles
    private void setupPaint() {
        drawPaint = new Paint();
        // setup initial color
        int paintColor = Color.TRANSPARENT;
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(3);
        drawPaint.setTextSize(30);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawPaint.setColor(Color.BLUE);
        if(detections != null){
            for(Detection detection : detections){
                //If the score if more than 0.5, to filter the false positive without leaving any false negative
                if(detection.getCategories().get(0).getScore() > 0.5f){
                    //Draw the rectangle surround object
                    canvas.drawRect(detection.getBoundingBox(), drawPaint);
                    //Draw the label on top left of object
                    canvas.drawText(detection.getCategories().get(0).getLabel(), detection.getBoundingBox().left, detection.getBoundingBox().top, drawPaint);
                }
            }
        }
    }

    public void setDetections(List<Detection> detections) {
        this.detections = detections;
    }
}
