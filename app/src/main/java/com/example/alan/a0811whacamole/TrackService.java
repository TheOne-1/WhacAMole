package com.example.alan.a0811whacamole;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

public class TrackService extends Service {

    //define the scale of distance between real distance
    // and pixel, given in dp/mm
    private final float scale = 1;

    //the difficult of the game is controlled by the duration
    //of movement & waiting
    public final int EASY_WAIT_DURATION = 4;
    public final int HARD_WAIT_DURATION = 3;
    public final int CRAZY_WAIT_DURATION = 2;

    //to determine how many easy and hard round will be
    //launched; crazy round will not stop until the end
    //of the trial
    public final int EASY_ROUND = 3;
    public final int HARD_ROUND = 3;

    //coordinates of four holes
    private int holeX0 = 0;
    private int holeX1 = 0;
    private int holeY0 = 0;
    private int holeY1 = 0;


    private TrackBinder mTrackBinder = new TrackBinder();
    private Handler mHandler;

    private BtService.BluetoothBinder mBtBinder;

    //initial the first hole
    private Hole nextHole = new Hole(0);
    private int mState = StartGame.STATE_RESTING;


    //to indicate whether reached the time limit
//    private boolean ballWaiting;

    public TrackService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mTrackBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int[] positions = intent.getIntArrayExtra("holes_position");
        holeX0 = positions[0];
        holeX1 = positions[1];
        holeY0 = positions[2];
        holeY1 = positions[3];
        return super.onStartCommand(intent, flags, startId);
    }

    public class TrackBinder extends Binder {
        //to set the handler for this service
        public void setParams(Handler handler, BtService.BluetoothBinder mBtBinder) {
            TrackService.this.mHandler = handler;
            TrackService.this.mBtBinder = mBtBinder;
        }

        public void startTrackingThread() {
            TrackingThread trackingThread = new TrackingThread();
            trackingThread.start();
        }

        public void setNextHole(int nextHole) {
            TrackService.this.nextHole.setHole(nextHole);
            switch (nextHole) {
                case 0:
                    TrackService.this.nextHole.setHoleX(holeX0);
                    TrackService.this.nextHole.setHoleY(holeY0);
                    break;
                case 1:
                    TrackService.this.nextHole.setHoleX(holeX1);
                    TrackService.this.nextHole.setHoleY(holeY0);
                    break;
                case 2:
                    TrackService.this.nextHole.setHoleX(holeX0);
                    TrackService.this.nextHole.setHoleY(holeY1);
                    break;
                case 3:
                    TrackService.this.nextHole.setHoleX(holeX1);
                    TrackService.this.nextHole.setHoleY(holeY1);
                    break;
            }
        }

        public void notifyTrackingThread() {
            synchronized (nextHole) {
                nextHole.notify();
            }
        }

        public void setState(int state) {
            TrackService.this.mState = state;
        }
    }

    private class TrackingThread extends Thread {

        int waitDuration = EASY_WAIT_DURATION;
        int currentRound;

        public TrackingThread() {
            currentRound = 0;
        }

        @Override
        public void run() {

            //to make sure the current time is within the waiting duration
            //as well as one trial duration
            while (mState == StartGame.STATE_PLAYING) {

                //set the time of one round
                if (currentRound >= EASY_ROUND) {
                    if (currentRound >= (EASY_ROUND + HARD_ROUND)) {
                        waitDuration = CRAZY_WAIT_DURATION;
                    } else {
                        waitDuration = HARD_WAIT_DURATION;
                    }
                }
                synchronized (nextHole) {
                    try {
                        nextHole.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
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
                    if (matchedTimes >= 3) {
                        tracked = true;
                        break;
                    }
                    //sleep to avoid noise; the duration should be determined by
                    //refresh rate of the Bluetooth transmission
                    try {
                        sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (tracked) {
                    mHandler.obtainMessage(Constants.MESSAGE_TRACKED).sendToTarget();
                } else {
                    mHandler.obtainMessage(Constants.MESSAGE_NOT_TRACKED).sendToTarget();
                }
                currentRound++;
            }
        }
    }

    public boolean positionMatched() {
        float[] mobilePos = mBtBinder.getPosData();
//        //mobileX and mobileY represents the transferred
//        //coordinates of mobile on the tablet
        int mobileX = 0;
        int mobileY = 0;

        return false;
    }


    private class Hole {
        private int holeId;
        private int holeX;
        private int holeY;

        public Hole(int hole) {
            this.holeId = hole;
        }

        public int getHole() {
            return holeId;
        }

        public void setHole(int hole) {
            this.holeId = hole;
        }

        public int getHoleX() {
            return holeX;
        }

        public void setHoleX(int holeX) {
            this.holeX = holeX;
        }

        public int getHoleY() {
            return holeY;
        }

        public void setHoleY(int holeY) {
            this.holeY = holeY;
        }
    }
}

















