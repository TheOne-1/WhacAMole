package com.example.alan.a0811whacamole;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

public class TrackService extends Service {


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

    private Integer nextHole;
    private int mState = StartGame.STATE_RESTING;



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
        public void setHandler(Handler handler) {
            TrackService.this.mHandler = handler;
        }

        public void startTrackingThread() {

        }

        public void setNextHole(int nextHole) {
            TrackService.this.nextHole = nextHole;
        }

        public void notifyTrackingThread() {
            synchronized (nextHole) {
                nextHole.notifyAll();
            }
        }
    }

    private class TrialThread extends Thread {

        @Override
        public void run() {
            while (mState == StartGame.STATE_PLAYING) {
                synchronized (nextHole) {
                    try {
                        nextHole.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

}

















