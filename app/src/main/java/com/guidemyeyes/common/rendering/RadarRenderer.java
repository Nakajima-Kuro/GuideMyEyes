package com.guidemyeyes.common.rendering;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.guidemyeyes.Coordinate;

import static android.content.ContentValues.TAG;

public class RadarRenderer extends View {

    // setup initial color
    private final int paintColor = Color.TRANSPARENT;
    // defines paint and canvas
    private Paint drawPaint;
    //Coordinate of the closest point
    private Coordinate coor;

    public RadarRenderer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setupPaint();
        this.setBackgroundColor(Color.TRANSPARENT);
        this.coor = new Coordinate((getWidth()/2),(getHeight()/2), (short) 0);
    }

    // Setup paint with color and stroke styles
    private void setupPaint() {
        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(20);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawPaint.setColor(Color.BLUE);
        canvas.drawCircle(coor.getX(), coor.getY(), 10, drawPaint);
    }

    public void setCoordinate(Coordinate coor){
        if(coor != null){
            this.coor = coor;
            Log.i(TAG, "onDraw: " + coor.getX() + ", " + coor.getY());
            Log.i(TAG, "onDraw: " + (getWidth()/2) + ", " + (getHeight()/2));
        }
    }
}
