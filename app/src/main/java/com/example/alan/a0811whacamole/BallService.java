package com.example.alan.a0811whacamole;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
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
        public void startATrial() {
            TrialThread trialThread = new TrialThread();
            trialThread.start();
        }

        //wake up the thread that the ball is in the hole and is catchable
        public void notifyTrialThread() {
            synchronized (catchState) {
//                catchState.setCatchable(false);
                catchState.notify();
            }
        }

        public void setState(int state) {
            BallService.this.mState = state;
        }

        public void stopPlaying() {
            mBallBinder.setState(StartGame.STATE_RESTING);
            //TODO: wake up the ball thread so that it can stop
            mBallBinder.notifyTrialThread();
        }

    }

    private class TrialThread extends Thread {
        //to indicate the current round in the loop
        private int currentRound;
        Random rand = new Random();
        //In every movement, the ball starts from the last
        //hole. The first hole is always the hole_0.
        int lastHole;
        int lastTwoHole;
        int moveDuration;
//        int waitDuration = EASY_WAIT_DURATION;
        public TrialThread() {
            currentRound = 0;
            lastHole = 0;
            lastTwoHole = 0;
            moveDuration = EASY_MOVE_DURATION;
        }

        @Override
        public void run() {
            while (mState == StartGame.STATE_PLAYING) {
                int nextHoleId = rand.nextInt(4);
                //To prevent next hole equal to the last hole
                while (nextHoleId == lastHole || nextHoleId == lastTwoHole)
                    nextHoleId = rand.nextInt(4);

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

                move(lastHole, nextHoleId, moveDuration);
                Message msg = mHandler.obtainMessage(MESSAGE_NEXT_HOLE);
                Bundle bundle = new Bundle();
                bundle.putInt("next_hole", nextHoleId);
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
                lastHole = nextHoleId;
                currentRound++;
            }
        }
    }

    //duration is given in seconds
    public void move(int startHole, int endHole, int duration) {
        //start hole cannot equal to end hole
        if (startHole == endHole)
            return;
        float toX = 0;
        float toY = 0;
        switch (startHole) {
            case 0:
                switch (endHole) {
                    case 1:
                        toX = holeX1 - holeX0; break;
                    case 2:
                        toY = holeY1 - holeY0; break;
                    case 3:
                        toX = holeX1 - holeX0; toY = holeY1 - holeY0; break;
                } break;
            case 1:
                switch (endHole) {
                    case 0:
                        toX = holeX0 - holeX1; break;
                    case 2:
                        toX = holeX0 - holeX1; toY = holeY1 - holeY0; break;
                    case 3:
                        toY = holeY1 - holeY0; break;
                } break;
            case 2:
                switch (endHole) {
                    case 0:
                        toY = holeY0 - holeY1; break;
                    case 1:
                        toX = holeX1 - holeX0; toY = holeY0 - holeY1; break;
                    case 3:
                        toX = holeX1 - holeX0; break;
                } break;
            case 3:
                switch (endHole) {
                    case 0:
                        toX = holeX0 - holeX1; toY = holeY0 - holeY1; break;
                    case 1:
                        toY = holeY0 - holeY1; break;
                    case 2:
                        toX = holeX0 - holeX1; break;
                } break;
        }
        SerialAnimation animation = new SerialAnimation(0, toX, 0, toY);
        //duration is given in seconds
        animation.setDuration(1000 * duration);
        Message msg = mHandler.obtainMessage(MESSAGE_BALL_ANIMATION);
        Bundle bundle = new Bundle();
        bundle.putSerializable("animation", animation);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    //这个类并没有什么卵用，就是用来实现 wait and notify 的
    private class CatchState {
        private boolean catchable;

        public CatchState(boolean catchable) {
            this.catchable = catchable;
        }

//        public boolean isCatchable() {
//            return catchable;
//        }

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
