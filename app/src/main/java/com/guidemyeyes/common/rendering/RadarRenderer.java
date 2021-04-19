package com.guidemyeyes.common.rendering;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.guidemyeyes.Coordinate;

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
        this.coor = new Coordinate((getWidth() / 2), (getHeight() / 2), getWidth(), getHeight(), (short) 0);
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
        if (coor.getHeight() != 0 && coor.getHeight() != 0) {
            /*
            * AR Core Coordinate System is a bit funny
            * y
            * <------------------------------------------
                                                        |
                                                        |
                                                        |
                                                        |
                                                        |
                                                        | x
                                                        v
            *
            *
            * While Canvas Coordinate System is like this
            *                                         x
            * ----------------------------------------->
            * |
            * |
            * |
            * |
            * |
            * | y
            * v
            *
            * So the equation will be hard to understand
            * */
            float drawX, drawY;
            //Check if phone in landscape or potrait
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // In landscape
                drawX = coor.getX() * getWidth() / coor.getWidth();
                drawY = coor.getY() * getHeight() / coor.getHeight();
            } else {
                // In portrait
                drawX = getWidth() - coor.getY() * getWidth() / coor.getHeight();
                drawY = coor.getX() * getHeight() / coor.getWidth();
            }

            canvas.drawCircle(drawX, drawY, 10, drawPaint);
            drawPaint.setColor(Color.RED);
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, 10, drawPaint);
        }
    }

    public void setCoordinate(Coordinate coor) {
        if (coor != null) {
            this.coor = coor;
        }
    }
}
