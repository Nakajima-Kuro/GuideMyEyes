package com.guidemyeyes.common.handlers;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.Image;
import android.media.SoundPool;

import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.guidemyeyes.Coordinate;
import com.guidemyeyes.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Timer;
import java.util.TimerTask;

public class RadarHandler {

    Context context;

    //Soundpool and all Sound effect for radar
    private SoundPool soundPool;
    private final int ping;

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
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();

        //Load all radar sound effect
        ping = soundPool.load(context, R.raw.far, 1);
    }

    /**
     * Looking for closest point on depth map and play sound base on point coordinate
     *
     * @param frame The frame get from an AR Instance
     * @return Coordinate of the closest point on depth map
     */
    public Coordinate renderPosition(Frame frame) {
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
                    <-----------------------                       ------------   Device Orientation (Portrait, head upward)
                                            |                     |    ====    |
                                            |                     |            |
                                            |                     |            |
                                            |                     |            |
                                            |                     |            |
                                            | x (Width)           |      O     |
                                            v                     --------------
                */
            Image.Plane plane = depthImage.getPlanes()[0];
            for (int i = 0; i < width / 4 * 3; i++) {
                for (int j = 0; j < height; j++) {
                    //i === x
                    //j === y
                    int byteIndex = i * plane.getPixelStride() + j * plane.getRowStride();
                    ByteBuffer buffer = plane.getBuffer().order(ByteOrder.nativeOrder());
                    short depthSample = buffer.getShort(byteIndex);
                    //Check if this depth is closer
                    Coordinate tempCoor = new Coordinate(i, j, width, height, depthSample);
                    if (getSensorValue(tempCoor) < getSensorValue(coor)) {
                        //Update new position
                        coor.setDepth(depthSample);
                        coor.setX(i);
                        coor.setY(j);
                    }
                }
            }
            depthImage.close();
            playSound(coor);
            return coor;
        } catch (NotYetAvailableException e) {
            return null;
        }
    }

    /**
     * Get the weight of that depth pixel base on location on screen. Further away from center of the screen, lower the weight
     *
     * @param coor coordinate of the point want to render sound
     */
    private float getSensorValue(Coordinate coor) {
        float halfWidth = (float) coor.getWidth() / 2;
        float halfHeight = (float) coor.getHeight() / 2;
        //Set a weight value so that further depth point from the centre of the image will be less important
        short weight = 300;
        return (float) coor.getDepth()
                + weight * (Math.abs(coor.getX() - halfWidth) / halfWidth)
                + weight * 1.5f * (Math.abs(coor.getY() - halfHeight) / halfHeight);
    }

    /**
     * Play sound to make person have immersion of location of the point on depth map
     *
     * @param coor coordinate of the point want to render sound
     */
    public void playSound(Coordinate coor) {
        if (coor.getDepth() > 0 && !isPlaying) {
            //Start processing sound to play
            soundPool.autoPause();
            isPlaying = true;
            //Init the volume will be when depth point is horizontal middle. The lower the value, the more vary the volume change
            float baseVolume = 0.5f;

            //Left, Right volume to immerse location in horizontal
            float leftVolume = baseVolume, rightVolume = baseVolume;

            //Pitch to immerse the distance
            float pitch = 1.0f;

            float halfHeight = (float) coor.getHeight() / 2;
            float offset = (1 - baseVolume) * (coor.getY() - halfHeight) / halfHeight;
            leftVolume += offset;
            rightVolume -= offset;
            //Change pitch to immerse the distance
            short maxDepth = 8000;
            pitch -= 1.5f * Math.min(((float) coor.getDepth() / maxDepth), 1);
            //Playing radar sound
            float finalLeftVolume = Math.min(Math.max(leftVolume * (1 - Math.max((float) coor.getDepth() / maxDepth, 0)), 0), 1);
            float finalRightVolume = Math.min(Math.max(rightVolume * (1 - Math.max((float) coor.getDepth() / maxDepth, 0)), 0), 1);
            float finalPitch = pitch;
            //Interval between sound to immerse the distance
            //Interval = Min(interval when near, interval when far)
            int interval = (int) Math.min(Math.max(coor.getDepth() * 0.3, 50), 800 + (1200 * (Math.min((float) coor.getDepth() / maxDepth, 1))));
            Timer timer = new Timer();
            soundPool.play(ping, finalLeftVolume, finalRightVolume, 10, 0, finalPitch);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    isPlaying = false;
                }
            }, interval);
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
