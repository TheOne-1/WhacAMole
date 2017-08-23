package com.example.alan.a0811whacamole;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.animation.TranslateAnimation;

import java.io.Serializable;
import java.util.Random;

/**
 * Created by Alan on 2017/8/21.
 */

public class BallService extends Service implements Constants {



    private CatchState catchState = new CatchState(false);

    //coordinates of four holes
    private int holeX0 = 0;
    private int holeX1 = 0;
    private int holeY0 = 0;
    private int holeY1 = 0;
    private Handler mHandler;
    private BallMovementBinder mBallBinder = new BallMovementBinder();

    private int mState = StartGame.STATE_RESTING;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBallBinder;
    }



    //In this binder class, all the methods could be invoked by the StartGame Activity.
    public class BallMovementBinder extends Binder {
        //to set the handler for this service
        public void setParams(Handler handler, int[] positions) {
            BallService.this.mHandler = handler;
            holeX0 = positions[0];
            holeX1 = positions[1];
            holeY0 = positions[2];
            holeY1 = positions[3];
        }

        //to start a trial
        public void startATrial(int duration) {
            Log.d("TAG", "start a trial");
            TrialThread trialThread = new TrialThread(duration);
            trialThread.start();
        }

        //wake up the thread that the ball is in the hole and is catchable
        public void notifyTrialThread() {
            synchronized (catchState) {
                catchState.setCatchable(false);
                catchState.notify();
            }
        }

        public void setState(int state) {
            BallService.this.mState = state;
        }

    }

    private class TrialThread extends Thread {
//        private int duration;
//        private long startTime;
        //to indicate the current round in the loop
        private int currentRound;
        Random rand = new Random();
        //In every movement, the ball starts from the last
        //hole. The first hole is always the hole_0.
        int lastHole = 0;
        int lastTwoHole = 0;
        int moveDuration = EASY_MOVE_DURATION;
//        int waitDuration = EASY_WAIT_DURATION;
        public TrialThread(int duration) {
            currentRound = 0;
//            this.duration = duration;
        }

        @Override
        public void run() {
//            startTime = System.currentTimeMillis();
//            //control the duration of the game############################
            while (mState == StartGame.STATE_PLAYING) {
                int nextHole = rand.nextInt(4);
                //To prevent next hole equal to the last hole
                while (nextHole == lastHole || nextHole == lastTwoHole)
                    nextHole = rand.nextInt(4);

                //set the time of one round
                if (currentRound >= EASY_ROUND) {
                    if (currentRound >= (EASY_ROUND + HARD_ROUND)) {
                        moveDuration = CRAZY_MOVE_DURATION;
//                        waitDuration = CRAZY_WAIT_DURATION;
                    } else {
                        moveDuration = HARD_MOVE_DURATION;
//                        waitDuration = HARD_WAIT_DURATION;
                    }
                }

                move(lastHole, nextHole, moveDuration);
                Message msg = mHandler.obtainMessage(MESSAGE_NEXT_HOLE);
                Bundle bundle = new Bundle();
                bundle.putInt("next_hole", nextHole);
                msg.setData(bundle);
                mHandler.sendMessage(msg);

                synchronized (catchState) {
                    try {
                        catchState.setCatchable(true);
                        catchState.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                lastTwoHole = lastHole;
                lastHole = nextHole;
                currentRound++;
            }
//            Message msg = mHandler.obtainMessage(Constants.MESSAGE_TIME_UP);
//            mHandler.sendMessage(msg);
        }
    }

    //duration is given in seconds
    public void move(int startHole, int endHole, int duration) {
        //start hole cannot equal to end hole
        if (startHole == endHole)
            return;
        float fromX = 0;
        float fromY = 0;
        float toX = 0;
        float toY = 0;
        switch (startHole) {
            case 0:
                fromX = holeX0; fromY = holeY0; break;
            case 1:
                fromX = holeX1; fromY = holeY0; break;
            case 2:
                fromX = holeX0; fromY = holeY1; break;
            case 3:
                fromX = holeX1; fromY = holeY1; break;
        }
        switch (endHole) {
            case 0:
                toX = holeX0; toY = holeY0; break;
            case 1:
                toX = holeX1; toY = holeY0; break;
            case 2:
                toX = holeX0; toY = holeY1; break;
            case 3:
                toX = holeX1; toY = holeY1; break;
        }
        SerialAnimation animation = new SerialAnimation(fromX, toX, fromY, toY);
        //duration is given in seconds
        animation.setDuration(1000 * duration);
        Message msg = mHandler.obtainMessage(MESSAGE_BALL_ANIMATION);
        Bundle bundle = new Bundle();
        bundle.putSerializable("animation", animation);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    //for wait and notify
    private class CatchState {
        private boolean catchable;

        public CatchState(boolean catchable) {
            this.catchable = catchable;
        }

        public boolean isCatchable() {
            return catchable;
        }

        public void setCatchable(boolean catchable) {
            this.catchable = catchable;
        }
    }

    //define this class to implement serializable class
    private class SerialAnimation extends TranslateAnimation implements Serializable {
        SerialAnimation(float fromX, float toX, float fromY, float toY) {
            super(fromX, toX, fromY, toY);
        }
    }
}
