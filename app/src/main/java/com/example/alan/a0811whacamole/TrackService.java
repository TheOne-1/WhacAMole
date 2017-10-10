package com.example.alan.a0811whacamole;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import static java.lang.Math.abs;

public class TrackService extends Service implements Constants {

    int waitDuration;
    int currentRound;

    private int maxMobileX;
    private int maxMobileY;
    private float scaleFactor;
    private float xOffset;
    private float yOffset;
    private int windowWidth;
    private int windowHeight;

    private TrackBinder mTrackBinder = new TrackBinder();
    private Handler mHandler;

    private BtService.BluetoothBinder mBtBinder;

    //initial the first hole
    private Hole nextHole = new Hole(0);
    private int mState = StartGame.STATE_INITIALIZATION;
    private boolean isCatchable;

    //to indicate whether reached the time limit
//    private boolean ballWaiting;

    public TrackService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mTrackBinder;
    }


    public class TrackBinder extends Binder {
        //to set the handler for this service
        public void setParams(Handler handler, BtService.BluetoothBinder mBtBinder
                , float scaleFactor, float xOffset, float yOffset, int windowWidth, int windowHeight) {
            TrackService.this.mHandler = handler;
            TrackService.this.mBtBinder = mBtBinder;
            TrackService.this.scaleFactor = scaleFactor;
            TrackService.this.xOffset = xOffset;
            TrackService.this.yOffset = yOffset;
            TrackService.this.windowWidth = windowWidth;
        }

        public void startTrackingThread() {
            TrackingThread trackingThread = new TrackingThread();
            setCatchable(UNCATCHABLE);
            trackingThread.start();
        }

        public void setNextHole(Hole nextHole) {
            TrackService.this.nextHole.setHole(nextHole.getHoleId());
            TrackService.this.nextHole.setHoleX(nextHole.getHoleX());
            TrackService.this.nextHole.setHoleY(nextHole.getHoleY());
        }

        public void initialCatching() {
            setState(StartGame.STATE_PLAYING);
            waitDuration = EASY_WAIT_DURATION;
            currentRound = 0;
        }

        public void startCatching() {
            setCatchable(CATCHABLE);
        }

        public void stopPlaying() {
            setState(StartGame.STATE_RESTING);
            setCatchable(UNCATCHABLE);
        }

        public void setState(int state) {
            TrackService.this.mState = state;
        }

        public void setCatchable(Boolean isCatchable) {
            TrackService.this.isCatchable = isCatchable;
        }

        public void setMax(int maxMobileX, int maxMobileY) {
            TrackService.this.maxMobileX = maxMobileX;
            TrackService.this.maxMobileY = maxMobileY;
        }
    }

    private class TrackingThread extends Thread {

        //        public TrackingThread() {
//            currentRound = 0;
//        }
        @Override
        public void run() {
            //keep running when this service is alive
            while (true) {
                //update the position of knot
                updateKnot();
                if (isCatchable) {
                    //set the time of one round
                    if (currentRound >= EASY_ROUND) {
                        if (currentRound >= (EASY_ROUND + HARD_ROUND)) {
                            waitDuration = CRAZY_WAIT_DURATION;
                        } else {
                            waitDuration = HARD_WAIT_DURATION;
                        }
                    }
                    long startTime = System.currentTimeMillis();
                    int matchedTimes = 0;       //count the time of match to avoid noise
                    boolean tracked = false;

                    while ((System.currentTimeMillis() - startTime < 1000 * waitDuration)
                            && mState == StartGame.STATE_PLAYING) {
                        if (positionMatched()) {
                            matchedTimes++;
                        } else {
                            matchedTimes = 0;
                        }
                        //at least match twice to catch
                        if (matchedTimes >= 2) {
                            tracked = true;
                            break;
                        }
                        //sleep to avoid noise; the duration should be determined by
                        //refresh rate of the Bluetooth transmission
                        try {
                            sleep(POS_DATA_INQUIRY_INTERVAL);      //get position data every 20 ms
                            updateKnot();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    //only return the result when playing
                    if (mState == StartGame.STATE_PLAYING) {
                        if (tracked) {
                            mHandler.obtainMessage(MESSAGE_TRACKED).sendToTarget();
                        } else {
                            mHandler.obtainMessage(MESSAGE_NOT_TRACKED).sendToTarget();
                        }
                        currentRound++;
                        mTrackBinder.setCatchable(UNCATCHABLE);
                    }
                } else {
                    //sleep to save resource
                    try {
                        sleep(POS_DATA_INQUIRY_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    int mobileX = 0;
    int mobileY = 0;

    public void updateKnot() {
        float[] mobilePos = mBtBinder.getPosData();
//        //mobileX and mobileY represents the transferred
//        //coordinates of mobile on the tablet
        mobileX = (int) ((mobilePos[1] - xOffset) * scaleFactor) + maxMobileX / 2;
        mobileY = (int) ((mobilePos[2] - yOffset) * scaleFactor) + maxMobileY / 2;

        //to make sure the coordinates are within the range
        if (mobileX < 0)
            mobileX = 0;
        else if (mobileX > maxMobileX)
            mobileX = maxMobileX;
        if (mobileY < 0)
            mobileY = 0;
        else if (mobileY > maxMobileY)
            mobileY = maxMobileY;
        Message msg = mHandler.obtainMessage(MESSAGE_UPDATE_KNOT);
        Bundle bundle = new Bundle();
        bundle.putIntArray("mobile_pos", new int[]{mobileX, mobileY});
        msg.setData(bundle);
        mHandler.sendMessage(msg);

    }

    public boolean positionMatched() {

        int ballX = nextHole.getHoleX();
        int ballY = nextHole.getHoleY();

        //捕获逻辑
        if (abs(ballX - mobileX) < MIN_MATCH_DIS &&
                abs(ballY - mobileY) < MIN_MATCH_DIS)
            return true;
        else
            return false;
    }


}

















