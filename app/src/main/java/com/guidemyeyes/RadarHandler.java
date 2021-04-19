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
    //Init interval for
    private final static int NEAR_INTERVAL = 1000;
    private final static int MEDIUM_INTERVAL = 2500;
    private final static int FAR_INTERVAL = 4000;

    Context context;

    //Soundpool and all Sound effect for radar
    private SoundPool soundPool;
    private int near, medium, far;

    private boolean isPlaying = false;

    /**
     * Constructor
     *
     * @param context The context of recent activity
     *
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
     *
     * @return Coordinate of the closest point on depth map
     */
    public Coordinate renderPosition(Frame frame) {
        if (isPlaying == false) {
            try {
                //get Image and its size from frame
                Image depthImage = frame.acquireDepthImage();
                int height = depthImage.getHeight();
                int width = depthImage.getWidth();

                //Init closest image
                Coordinate coor = new Coordinate(width / 2, height / 2, Short.MAX_VALUE);

                //Scan for closet pixel of the depth image
                Image.Plane plane = depthImage.getPlanes()[0];
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height / 4 * 3; j++) {
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

                playSound(width, height, coor);
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
     * @param width width of the depth image
     * @param height height of the depth image
     * @param coor coordinate of the point want to render sound
     *
     * @return None
     */
    public void playSound(int width, int height, Coordinate coor) {
        if (isPlaying == false) {
            isPlaying = true;
            //Get the closest distant on frame
            float leftVolume = 0.5f, rightVolume = 0.5f;
            float offset = 0.5f / 100 * (coor.getX() - width / 2) / (width / 2);
            leftVolume += offset;
            rightVolume -= offset;
            int soundId;

            if (coor.getDepth() < 2000) {
                soundId = near;

            } else if (coor.getDepth() >= 2000 && coor.getDepth() < 3000) {
                soundId = medium;

            } else if (coor.getDepth() >= 3000) {
                soundId = far;

            } else {
                //SoundId not defined, can't continue
                isPlaying = false;
                return;
            }
            //Playing radar sound
            int finalSoundId = soundId;
            float finalLeftVolume = leftVolume;
            float finalRightVolume = rightVolume;
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    soundPool.play(finalSoundId, finalLeftVolume, finalRightVolume, 0, 0, 0.8f);
                    isPlaying = false;
                }
            }, Math.min(coor.getDepth() + 50, 2000));
            return;
        }
    }

    /**
     * Call this when RadarHandler is not used anymore
     * @return None
     */
    public void destroy() {
        soundPool.release();
        soundPool = null;
    }
}
