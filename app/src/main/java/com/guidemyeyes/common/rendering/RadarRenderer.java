package com.guidemyeyes.common.rendering;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.guidemyeyes.Coordinate;

public class RadarRenderer extends View {
    //Orientation
    private final int ROTATION_0 = 0;  //^
    private final int ROTATION_90 = 1; //<-
    private final int ROTATION_180 = 2;//v
    private final int ROTATION_270 = 3;//->

    // defines paint and canvas
    private Paint drawPaint;
    //Coordinate of the closest point
    private Coordinate coor;
    //Orientation of the device
    private int orientation;

    public RadarRenderer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setupPaint();
        this.setBackgroundColor(Color.TRANSPARENT);
        this.coor = new Coordinate((getWidth() / 2), (getHeight() / 2), getWidth(), getHeight(), (short) 0);
        this.orientation = 1; //Default portrait, head upward
    }

    // Setup paint with color and stroke styles
    private void setupPaint() {
        drawPaint = new Paint();
        // setup initial color
        int paintColor = Color.TRANSPARENT;
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
            switch (orientation) {
                case ROTATION_0: {
                    //In Portrait, head face upward
                    drawX = getWidth() - (float) coor.getY() * getWidth() / coor.getHeight();
                    drawY = (float) coor.getX() * getHeight() / coor.getWidth();
                    break;
                }
                case ROTATION_90: {
                    //In Landscape, head face to left
                    drawX = (float) coor.getX() * getWidth() / coor.getWidth();
                    drawY = (float) coor.getY() * getHeight() / coor.getHeight();
                    break;
                }
                case ROTATION_180: {
                    //In Portrait, head face downward (Not surport)
                    drawX = (float) getWidth() / 2;
                    drawY = (float) getHeight() / 2;
                    break;
                }
                case ROTATION_270: {
                    //In Landscape, head face to right
                    drawX = getWidth() - (float) coor.getX() * getWidth() / coor.getWidth();
                    drawY = getHeight() - (float) coor.getY() * getHeight() / coor.getHeight();
                    break;
                }
                default: {
                    return;
                }
            }

            canvas.drawCircle(drawX, drawY, 10, drawPaint);
            drawPaint.setColor(Color.RED);
            canvas.drawCircle((float) getWidth() / 2, (float) getHeight() / 2, 10, drawPaint);
            drawPaint.setColor(Color.BLUE);
            drawPaint.setTextSize(30);
            drawPaint.setStrokeWidth(3);
            canvas.drawText("Depth: " + coor.getDepth(), drawX + 40, drawY, drawPaint);
            canvas.drawText("X: " + drawX, drawX + 40, drawY + 40, drawPaint);
            canvas.drawText("Y: " + drawY, drawX + 40, drawY + 80, drawPaint);
        }
    }

    public void setCoordinate(Coordinate coor, int orientation) {
        if (coor != null) {
            this.coor = coor;
            this.orientation = orientation;
        }
    }
}
