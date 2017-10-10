package com.example.alan.a0811whacamole;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class CaliService extends Service implements Constants {

    private int mState;
    private CaliBinder mCaliBinder = new CaliBinder();
    private Handler mCaliHandler;
    private BtService.BluetoothBinder mBtBinder;


    public CaliService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mCaliBinder;
    }

    public class CaliBinder extends Binder {
        public void startCalibration() {
            CaliService.this.startCalibration();
        }

        public void setParams(Handler handler, BtService.BluetoothBinder btBinder) {
            CaliService.this.mCaliHandler = handler;
            CaliService.this.mBtBinder = btBinder;
        }
    }

    private class CalibrationThread extends Thread {
        float[] posData = mBtBinder.getPosData();
        CheckTimeChange timeChecker = new CheckTimeChange();
        IMUMovementUtil movementUtil = new IMUMovementUtil();

        @Override
        public void run() {
            //stage 1, wait for movement
            while (mState == CalibrationActivity.STATE_WAITING) {
                posData = mBtBinder.getPosData();
                //do not check isStill unless the time stamp is changed
                if (timeChecker.isTimeChanged(posData[0])) {
                    Log.d("POS1", "" + posData[0] + "\t" + posData[1] + "\t" + posData[2] + "\t" + posData[3]);
                    if (movementUtil.isMoving(posData))
                        break;
                }
                try {
                    sleep(POS_DATA_INQUIRY_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Message msgUI = mCaliHandler.obtainMessage(CalibrationActivity.MESSAGE_UPDATE_UI);
                mCaliHandler.sendMessage(msgUI);
            }
            //transition between stage 1 and stage 2
            Message msgStart = mCaliHandler.obtainMessage(CalibrationActivity.MESSAGE_MOVED);
            Bundle bundle = new Bundle();
            bundle.putFloat("x0", posData[1]);
            bundle.putFloat("y0", posData[2]);
            msgStart.setData(bundle);
            mCaliHandler.sendMessage(msgStart);
            mState = CalibrationActivity.STATE_MOVING;
            //stage 2, wait for movement finished
            while (mState == CalibrationActivity.STATE_MOVING) {
                posData = mBtBinder.getPosData();
                //do not check isStill unless the time stamp is changed
                if (timeChecker.isTimeChanged(posData[0])) {
                    Log.d("POS2", "" + posData[0] + "\t" + posData[1] + "\t" + posData[2] + "\t" + posData[3]);
                    if (movementUtil.isStill(posData))
                        break;
                }
                try {
                    sleep(POS_DATA_INQUIRY_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Message msgUI = mCaliHandler.obtainMessage(CalibrationActivity.MESSAGE_UPDATE_UI);
                mCaliHandler.sendMessage(msgUI);
            }
            //transition between stage 2 and stage 3
            Message msgEnd = mCaliHandler.obtainMessage(CalibrationActivity.MESSAGE_STOPPED);
            bundle = new Bundle();
            bundle.putFloat("x1", posData[1]);
            msgEnd.setData(bundle);
            mCaliHandler.sendMessage(msgEnd);
            mState = CalibrationActivity.STATE_FINISHED;
        }
    }

    private class CheckTimeChange {
        private float lastTimeStamp = 0;

        public boolean isTimeChanged(float time) {
            if (time != lastTimeStamp) {
                lastTimeStamp = time;
                return true;
            } else {
                lastTimeStamp = time;
                return false;
            }
        }
    }

    private void startCalibration() {
        mState = CalibrationActivity.STATE_WAITING;
        CalibrationThread calibrationThread = new CalibrationThread();
        calibrationThread.start();
    }

/*    public double[] float2Double(float[] data) {
        int len = data.length;
        double[] dataTransition = new double[len];
        for (int i = 0; i < len; i++) {
            dataTransition[i] = (double) data[i];
        }
        return dataTransition;
    }*/


}










