package com.example.alan.a0811whacamole;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

public class TimingService extends Service implements Constants {

    private TimingBinder mTimingBinder = new TimingBinder();
    private Handler mHandler;
    private int gameDuration;
    private int mState;


    public TimingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mTimingBinder;
    }

    public class TimingBinder extends Binder {
        public void setHandler(Handler handler) {
            TimingService.this.mHandler = handler;
        }

        public void setState(int state) {
            mState = state;
        }

        public void startCounting(int duration) {
            TimingService.this.gameDuration = duration;
            TimingThread timingThread = new TimingThread();
            timingThread.start();
        }
    }

    private class TimingThread extends Thread {
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            int restDuration = gameDuration;
            while (restDuration > 0) {
//                try {
//                    sleep(950);     //sleep less than 1 second
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                if (mState == StartGame.STATE_RESTING)
//                    break;

                //wait until 1 second
                while (System.currentTimeMillis() - startTime <
                        1000 * (gameDuration - restDuration + 1)) {
                    if (mState == StartGame.STATE_RESTING)
                        break;
                    try {
                        sleep(10);     //sleep to save resource
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (mState == StartGame.STATE_RESTING)
                    break;
                Message oneSecondMsg = mHandler.obtainMessage(MESSAGE_ONE_SECOND);
                mHandler.sendMessage(oneSecondMsg);
                restDuration--;
            }
            Message timeUpMsg = mHandler.obtainMessage(MESSAGE_TIME_UP);
            mHandler.sendMessage(timeUpMsg);
            mTimingBinder.setState(StartGame.STATE_RESTING);
        }

    }

}
















