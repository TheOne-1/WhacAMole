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

public class StartGame extends AppCompatActivity implements Constants {
    private final String TAG = "TAG";

    public static final int STATE_INITIALIZATION = 2;
    public static final int STATE_RESTING = 2;
    public static final int STATE_PLAYING = 1;

    private int mState;
    private int currentScore;


    //coordinates of four holes
    private int holeX0 = 0;
    private int holeX1 = 0;
    private int holeY0 = 0;
    private int holeY1 = 0;
    int[] positions;


    private ImageView ballImage;

    private BallService.BallMovementBinder mBallBinder;
    private TrackService.TrackBinder mTrackBinder;
    private TimingService.TimingBinder mTimingBinder;
    private BtService.BluetoothBinder mBtBinder;

    private int nextHole;
    private int restSecond;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_game);
        ballImage = (ImageView) findViewById(R.id.ball_image);

//        mBtService = (BtService) getIntent().getSerializableExtra("bluetooth_service");



        Button startButton = (Button) findViewById(R.id.start_game_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mState == STATE_RESTING) {
                    ballImage.setVisibility(View.VISIBLE);
                    mState = STATE_PLAYING;

                    mTimingBinder.setState(STATE_PLAYING);
                    mTimingBinder.startCounting(GAME_DURATION);

                    mBallBinder.setState(mState);
                    mBallBinder.startATrial(GAME_DURATION);

                    //set state as playing and start the thread
                    mTrackBinder.setState(mState);
                    mTrackBinder.startTrackingThread();

                    restSecond = GAME_DURATION;
                    currentScore = 0;
                }
            }
        });

        Button resetButton = (Button) findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mState == STATE_PLAYING) {
                    mTimingBinder.setState(STATE_RESTING);
                }
                initialUI();
            }
        });

        //bind the bluetooth service and StartGame class
        Intent btBindIntent = new Intent(StartGame.this, BtService.class);
        bindService(btBindIntent, btServiceConnection, BIND_AUTO_CREATE);

        Intent timingIntent = new Intent(StartGame.this, TimingService.class);
        bindService(timingIntent, timingServiceConnection, BIND_AUTO_CREATE);

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

    private ServiceConnection timingServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mTimingBinder = (TimingService.TimingBinder) iBinder;
            mTimingBinder.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    private ServiceConnection ballServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBallBinder = (BallService.BallMovementBinder) iBinder;
            //set params if positions is initialized
            if (positions != null)
                mBallBinder.setParams(mHandler, positions);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    private ServiceConnection trackServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mTrackBinder = (TrackService.TrackBinder) iBinder;
            if (positions != null)
                mTrackBinder.setParams(mHandler, mBtBinder, positions);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };


    @Override
    protected void onResume() {
        super.onResume();

        //bind the ball service and StartGame class
        Intent ballBindIntent = new Intent(StartGame.this, BallService.class);
        bindService(ballBindIntent, ballServiceConnection, BIND_AUTO_CREATE);

        Intent trackBindIntent = new Intent(StartGame.this, TrackService.class);
        bindService(trackBindIntent, trackServiceConnection, BIND_AUTO_CREATE);

        mState = STATE_INITIALIZATION;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Intent stopBallIntent = new Intent(StartGame.this, BallService.class);
        stopService(stopBallIntent);
        Intent stopTrackIntent = new Intent(StartGame.this, BallService.class);
        stopService(stopTrackIntent);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        //only be invoked when the activity is created
        if (mState == STATE_INITIALIZATION) {
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

            positions = new int[]{holeX0, holeX1, holeY0, holeY1};

            //set params if binder is initialized, is the same as
            // functions in onServiceConnected
            if (mBallBinder != null)
                mBallBinder.setParams(mHandler, positions);
            if (mTrackBinder != null)
                mTrackBinder.setParams(mHandler, mBtBinder, positions);


        }
    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
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
                case MESSAGE_BALL_ANIMATION:
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
                case MESSAGE_NEXT_HOLE:
                    //set the next hole for track service
                    nextHole = msg.getData().getInt("next_hole");
                    break;

                case MESSAGE_TRACKED:
                    currentScore++;
                    TextView scoreText = (TextView) findViewById(R.id.current_score_text);
                    scoreText.setText("" + currentScore);
                case MESSAGE_NOT_TRACKED:
                    mBallBinder.notifyTrialThread();
                    break;

                case MESSAGE_ONE_SECOND:
                    restSecond--;
                    updateTimerUI(restSecond);
                    break;

                case MESSAGE_TIME_UP:
                    mState = STATE_RESTING;
                    mBallBinder.setState(mState);
                    //TODO: wake up the ball thread so that it can stop
                    mBallBinder.notifyTrialThread();
                    mTrackBinder.setState(mState);
                    mTrackBinder.notifyTrackingThread();
                    //stop animation
                    ballImage.clearAnimation();
                    ballImage.setVisibility(View.INVISIBLE);
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
        remainingTimeText.setText("" + GAME_DURATION);
        ProgressBar remainingTimeBar = (ProgressBar) findViewById(R.id.remaining_time_bar);
        remainingTimeBar.setMax(GAME_DURATION);
        remainingTimeBar.setProgress(GAME_DURATION);
        TextView scoreText = (TextView) findViewById(R.id.current_score_text);
        scoreText.setText("" + 0);
    }

    private void updateTimerUI(int restSecond) {
        TextView remainingTimeText = (TextView) findViewById(R.id.remaining_time_text);
        remainingTimeText.setText("" + restSecond);
        ProgressBar remainingTimeBar = (ProgressBar) findViewById(R.id.remaining_time_bar);
        remainingTimeBar.setProgress(restSecond);

    }
}











