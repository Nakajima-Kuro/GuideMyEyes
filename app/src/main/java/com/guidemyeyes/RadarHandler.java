package com.guidemyeyes;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.Image;
import android.media.SoundPool;

import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.NotYetAvailableException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Timer;
import java.util.TimerTask;

public class RadarHandler {
    //Orientation
    private final int ROTATION_0 = 0;  //^
    private final int ROTATION_90 = 1; //<-
    private final int ROTATION_180 = 2;//v
    private final int ROTATION_270 = 3;//->

    Context context;

    //Soundpool and all Sound effect for radar
    private SoundPool soundPool;
    private final int near, medium, far;

    private boolean isPlaying = false;

    /**
     * Constructor
     *
     * @param context The context of recent activity
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
            switch (orientation) {
                case ROTATION_0: {
                    //In Portrait, head face upward
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
                    break;
                }
                case ROTATION_90:
                case ROTATION_270: {
                    //In Landscape, head face to left
                    for (int i = 0; i < width; i++) {
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
                    break;
                }
                case ROTATION_180:
                default: {
                    //In Portrait, head face downward (Not support)
                    //Or invalid orientation
                    return null;
                }
            }
            if (!isPlaying) {
                playSound(coor, orientation);
            }
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
            return null;
        }
    }

    /**
     * Play sound to make person have immersion of location of the point on depth map
     *
     * @param coor coordinate of the point want to render sound
     */
    public void playSound(Coordinate coor, int orientation) {
        if (!isPlaying && coor.getDepth() > 0) {
            float leftVolume = 0.4f, rightVolume = 0.4f;
            switch (orientation) {
                case ROTATION_0: {
                    //In Portrait, head face upward
                    int height = coor.getHeight();
                    isPlaying = true;
                    //Get the offset to immerse location
                    float offset = 0.6f * (coor.getY() - (float) height / 2) / ((float) height / 2);
                    leftVolume += offset;
                    rightVolume -= offset;
                    break;
                }
                case ROTATION_90: {
                    //In Landscape, head face to left
                    int width = coor.getWidth();
                    isPlaying = true;
                    //Get the offset to immerse location
                    float offset = 0.6f * (coor.getX() - (float) width / 2) / ((float) width / 2);
                    leftVolume -= offset;
                    rightVolume += offset;
                    break;
                }
                case ROTATION_270: {
                    //In Landscape, head face to right
                    int width = coor.getWidth();
                    isPlaying = true;
                    //Get the offset to immerse location
                    float offset = 0.6f * (coor.getX() - (float) width / 2) / ((float) width / 2);
                    leftVolume += offset;
                    rightVolume -= offset;
                    break;
                }
                case ROTATION_180:
                default: {
                    //In Portrait, head face downward (Not support)
                    return;
                }
            }
            //Playing radar sound
            float finalLeftVolume = Math.min(Math.max(leftVolume, 0), 1);
            float finalRightVolume = Math.min(Math.max(rightVolume, 0), 1);
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    soundPool.play(far, finalLeftVolume, finalRightVolume, 0, 0, 0.7f);
                    isPlaying = false;
                }
            }, Math.min(Math.max(coor.getDepth(), 50), 1500));
        }
    }

    /**
     * Call this when RadarHandler is not used anymore
     */
    public void destroy() {
        soundPool.release();
        soundPool = null;
    }
}
