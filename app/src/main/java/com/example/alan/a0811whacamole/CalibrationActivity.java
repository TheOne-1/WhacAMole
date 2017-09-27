package com.example.alan.a0811whacamole;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class CalibrationActivity extends AppCompatActivity {

    //some states belongs to  calibration activity
    public static final int STATE_WAITING = 0;     //wait for the movement
    public static final int STATE_MOVING = 1;      //calibrating
    public static final int STATE_FINISHED = 2;     //calibrated, waiting for confirmation
    public static final int MESSAGE_MOVED = 0;
    public static final int MESSAGE_STOPPED = 1;

    private BtService.BluetoothBinder mBtBinder;
    private CaliService.CaliBinder mCaliBinder;

    private float scaleFactor;
    private float x0;
    private float x1;
    private float windowWidth;

    private TextView coordinatesText;
    private TextView stateText;
    private int mState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
        mState = STATE_WAITING;
        coordinatesText = (TextView) findViewById(R.id.coordinate_text);
        stateText = (TextView) findViewById(R.id.current_state_text);

        Button recaliButton = (Button) findViewById(R.id.recali_button);
        recaliButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mState != STATE_FINISHED)
                    return;
                initialUI();
                mCaliBinder.startCalibration();
                mState = STATE_WAITING;
            }
        });

        Button confirmButton = (Button) findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //do not start the game unless the calibration is finished
                if (mState != STATE_FINISHED)
                    return;
                Intent intent = new Intent(CalibrationActivity.this, StartGame.class);
                intent.putExtra("scaleFactor", scaleFactor);
                startActivity(intent);
            }
        });

        //bind the bluetooth service
        Intent btBindIntent = new Intent(CalibrationActivity.this, BtService.class);
        bindService(btBindIntent, btServiceConnection, BIND_AUTO_CREATE);

        // the service is started by bindService so it is finished after unbind
        Intent caliBindIntent = new Intent(CalibrationActivity.this, CaliService.class);
        bindService(caliBindIntent, caliServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        WindowManager wm = this.getWindowManager();
        windowWidth = wm.getDefaultDisplay().getWidth();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(btServiceConnection);
    }


    private void initialUI() {
        stateText.setText("waiting for movement");
        coordinatesText.setText(getPosText());
    }


    /**
     * update coordinates and state
     * only do updates when state changes
     */
    private void updateUI() {
        switch (mState) {
            case STATE_WAITING:
                stateText.setText("waiting for movement");
                break;
            case STATE_MOVING:
                stateText.setText("moving");
                break;
            case STATE_FINISHED:
                stateText.setText("waiting for confirmation");
                break;
        }
        coordinatesText.setText(getPosText());
    }

    private String getPosText() {
        float[] posData = mBtBinder.getPosData();
        StringBuilder builder = new StringBuilder();
        builder.append("X = ");
        builder.append("" + posData[1]);
        builder.append("\t\t");
        builder.append("Y = ");
        builder.append("" + posData[2]);
        builder.append("\t\t");
        builder.append("Z = ");
        builder.append("" + posData[3]);
        return builder.toString();
    }

    private ServiceConnection btServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBtBinder = (BtService.BluetoothBinder) iBinder;
            mBtBinder.setHandler(mCaliHandler);
            initialUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {}
    };

    private ServiceConnection caliServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mCaliBinder = (CaliService.CaliBinder) iBinder;
            mCaliBinder.setParams(mCaliHandler, mBtBinder);
            mCaliBinder.startCalibration();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };


    // handles message from mCaliHandler
    private Handler mCaliHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_MOVED:
                    x0 = msg.getData().getFloat("x0");
                    mState = STATE_MOVING;
                    updateUI();
                    break;
                case MESSAGE_STOPPED:
                    x1 = msg.getData().getFloat("x1");
                    scaleFactor = windowWidth / (x0 - x1);
                    Log.d("FACTOR", "" + scaleFactor);
                    mState = STATE_FINISHED;
                    updateUI();
                    break;
            }
        }
    };


}



















