package com.guidemyeyes;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.Image;
import android.media.SoundPool;
import android.util.Log;

import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.NotYetAvailableException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

public class RadarHandler {
    //Orientation
    final int ORIENTATION_PORTRAIT = 1;
    final int ORIENTATION_LANDSCAPE = 2;

    Context context;

    //Soundpool and all Sound effect for radar
    private SoundPool soundPool;
    private int near, medium, far;

    private boolean isPlaying = false;

    /**
     * Constructor
     *
     * @param context The context of recent activity
     * @return None
     */
    public RadarHandler(Context context) {
        this.context = context;

        //initialize Soundpool
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();

        //Load all radar sound effect
        near = soundPool.load(context, R.raw.near, 3);
        medium = soundPool.load(context, R.raw.medium, 2);
        far = soundPool.load(context, R.raw.far, 1);
    }

    /**
     * Looking for closest point on depth map and play sound base on point coordinate
     *
     * @param frame The frame get from an AR Instance
     * @return Coordinate of the closest point on depth map
     */
    public Coordinate renderPosition(Frame frame, int orientation) {
        if (isPlaying == false) {
            try {
                //get Image and its size from frame
                Image depthImage = frame.acquireDepthImage();
                int height = depthImage.getHeight();
                int width = depthImage.getWidth();

                //Init closest image
                Coordinate coor = new Coordinate(width / 2, height / 2, width, height, Short.MAX_VALUE);

                /* Scan for closet pixel of the depth image
                 * AR Core Coordinate System is a bit funny
                    y (Height)
                    <-----------------------
                                            |
                                            |
                                            |
                                            |
                                            |
                                            | x (Width)
                                            v
                */
                Image.Plane plane = depthImage.getPlanes()[0];
                if (orientation == ORIENTATION_LANDSCAPE) {
                    //In Landscape
                    for (int i = 0; i < width; i++) {
                        for (int j = 0; j < height / 3 * 2; j++) {
                            //i === x
                            //j === y
                            int byteIndex = i * plane.getPixelStride() + j * plane.getRowStride();
                            ByteBuffer buffer = plane.getBuffer().order(ByteOrder.nativeOrder());
                            short depthSample = buffer.getShort(byteIndex);

                            //Check if this depth is closer
                            if (depthSample < coor.getDepth()) {
                                //Update new position
                                coor.setDepth(depthSample);
                                coor.setX(i);
                                coor.setY(j);
                            }
                        }
                    }
                } else {
                    //In Portrait
                    for (int i = 0; i < width / 3 * 2; i++) {
                        for (int j = 0; j < height; j++) {
                            //i === x
                            //j === y
                            int byteIndex = i * plane.getPixelStride() + j * plane.getRowStride();
                            ByteBuffer buffer = plane.getBuffer().order(ByteOrder.nativeOrder());
                            short depthSample = buffer.getShort(byteIndex);

                            //Check if this depth is closer
                            if (depthSample < coor.getDepth()) {
                                //Update new position
                                coor.setDepth(depthSample);
                                coor.setX(i);
                                coor.setY(j);
                            }
                        }
                    }
                }
                playSound(coor, orientation);
                return coor;
            } catch (NotYetAvailableException e) {
                //AR is not yet available. Playing loading sound
//                Timer timer = new Timer();
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        soundPool.play(far, 1, 1, 0, 0, 0.5f);
//                        isPlaying = false;
//                    }
//                }, 1000);
//                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Play sound to make person have immersion of location of the point on depth map
     *
     * @param coor coordinate of the point want to render sound
     * @return None
     */
    public void playSound(Coordinate coor, int orientation) {
        if (isPlaying == false && coor.getDepth() > 0) {
            float leftVolume = 0.5f, rightVolume = 0.5f;
            if (orientation == ORIENTATION_LANDSCAPE) {
                //In Landscape
                int width = coor.getWidth();
                isPlaying = true;
                //Get the closest distant on frame
                float offset = 0.5f * (coor.getX() - width / 2) / (width / 2);
                leftVolume -= offset;
                rightVolume += offset;
            } else {
                //In Portrait
                int height = coor.getHeight();
                isPlaying = true;
                //Get the closest distant on frame
                float offset = 0.5f * (coor.getY() - height / 2) / (height / 2);
                leftVolume += offset;
                rightVolume -= offset;
            }
            //Playing radar sound
            int finalSoundId = far;
            float finalLeftVolume = leftVolume;
            float finalRightVolume = rightVolume;
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    soundPool.play(finalSoundId, finalLeftVolume, finalRightVolume, 0, 0, 0.7f);
                    isPlaying = false;
                }
            }, Math.min(coor.getDepth() + 50, 2000));
            return;
        }
    }

    /**
     * Call this when RadarHandler is not used anymore
     *
     * @return None
     */
    public void destroy() {
        soundPool.release();
        soundPool = null;
    }
}
