package com.example.alan.a0811whacamole;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class StartGame extends AppCompatActivity {
    private final String TAG = "TAG";

    public static final int STATE_RESTING = 0;
    public static final int STATE_PLAYING = 1;

    private int mState = STATE_RESTING;
    //duration per game, given in seconds
    private final int gameDuration = 60;

    //coordinates of four holes
    private int holeX0 = 0;
    private int holeX1 = 0;
    private int holeY0 = 0;
    private int holeY1 = 0;

    private ImageView ballImage;

    private BallService.BallMovementBinder mBallBinder;
    private TrackService.TrackBinder mTrackBinder;

    private BtService.BluetoothBinder mBtBinder;

    private int nextHole;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_game);
        ballImage = (ImageView) findViewById(R.id.ball_image);

//        mBtService = (BtService) getIntent().getSerializableExtra("bluetooth_service");

        ProgressBar remainingTimeBar = (ProgressBar) findViewById(R.id.remaining_time_bar);
        remainingTimeBar.setMax(gameDuration);
        remainingTimeBar.setProgress(gameDuration);

        Button startButton = (Button) findViewById(R.id.start_game_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mState == STATE_RESTING) {
                    ballImage.setVisibility(View.VISIBLE);
                    mState = STATE_PLAYING;
                    mBallBinder.startATrial(gameDuration);

                    //set state as playing and start the thread
                    mTrackBinder.setState(mState);
                    mTrackBinder.startTrackingThread();
                }
            }
        });

        initialUI();
    }

    private ServiceConnection btServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBtBinder = (BtService.BluetoothBinder) iBinder;
            mBtBinder.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private ServiceConnection ballServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBallBinder = (BallService.BallMovementBinder) iBinder;
            //send the handler to the service
            mBallBinder.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    private ServiceConnection trackServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mTrackBinder = (TrackService.TrackBinder) iBinder;
            mTrackBinder.setParams(mHandler, mBtBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };


    @Override
    protected void onStart() {
        super.onStart();
        //bind the bluetooth service and StartGame class
        Intent btBindIntent = new Intent(StartGame.this, BtService.class);
        bindService(btBindIntent, btServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        //get the position of holes
        ImageView hole0 = (ImageView) findViewById(R.id.hole_0);
        ImageView hole1 = (ImageView) findViewById(R.id.hole_3);
        //get the margin on each side and cut those part
        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) hole0.getLayoutParams();
        int offset = params.topMargin;
        holeX0 = hole0.getLeft() - offset;
        holeY0 = hole0.getTop() - offset;
        holeX1 = hole1.getLeft() - offset;
        holeY1 = hole1.getTop() - offset;

        //start the ball service
        Intent ballIntent = new Intent(StartGame.this, BallService.class);
        ballIntent.putExtra("holes_position", new int[]{holeX0, holeX1, holeY0, holeY1});
        startService(ballIntent);
        //bind the ball service and StartGame class
        Intent ballBindIntent = new Intent(StartGame.this, BallService.class);
        bindService(ballBindIntent, ballServiceConnection, BIND_AUTO_CREATE);

        //start the track service
        Intent trackIntent = new Intent(StartGame.this, TrackService.class);
        trackIntent.putExtra("holes_position", new int[]{holeX0, holeX1, holeY0, holeY1});
        startService(trackIntent);
        Intent trackBindIntent = new Intent(StartGame.this, TrackService.class);
        bindService(trackBindIntent, trackServiceConnection, BIND_AUTO_CREATE);
    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BtService.STATE_CONNECTED:
                            setStatus("Connected");
                            break;
                        case BtService.STATE_CONNECTING:
                            setStatus("Connecting");
                            break;
                        case BtService.STATE_NONE:
                            setStatus("Not connected.");
                            break;
                    }
                    break;
                case Constants.MESSAGE_BALL_ANIMATION:
                    TranslateAnimation animation = (TranslateAnimation)
                            msg.getData().getSerializable("animation");
                    //set the ball at the final position of the animation
                    animation.setFillAfter(true);
                    animation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mTrackBinder.setNextHole(nextHole);
                            mTrackBinder.notifyTrackingThread();

                            //startCatching. time should be controlled by this thread. and notify trial thread afterwards.
//                            mBallBinder.notifyTrialThread();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                    ballImage.startAnimation(animation);
                    break;
                case Constants.MESSAGE_NEXT_HOLE:
                    //set the next hole for track service
                    nextHole = msg.getData().getInt("next_hole");
                    break;

                case Constants.MESSAGE_TRACKED:

                case Constants.MESSAGE_NOT_TRACKED:
                    mBallBinder.notifyTrialThread();
                    break;


                case Constants.MESSAGE_TRIAL_ENDED:
                    mState = STATE_RESTING;
                    ballImage.setVisibility(View.INVISIBLE);
                    mTrackBinder.setState(mState);
                    break;
                default:
                    break;
            }
        }
    };
    //Updates the status on the right left corner
    private void setStatus(CharSequence state) {
        TextView currentState = (TextView) findViewById(R.id.current_state_text_start_game);
        currentState.setText(state);
    }


    private void initialUI() {
        //set initial state as state connected
        setStatus("Connected");
        TextView remainingTimeText = (TextView) findViewById(R.id.remaining_time_text);
        remainingTimeText.setText("" + gameDuration);
    }

}











