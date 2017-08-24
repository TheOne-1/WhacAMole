package com.example.alan.a0811whacamole;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;


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

    private Hole nextHole = new Hole(0);
    private int restSecond;
    private ImageView knotImage;

    private double screenDensity;

    private int maxMobileX;
    private int maxMobileY;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_game);
        ballImage = (ImageView) findViewById(R.id.ball_image);
        knotImage = (ImageView) findViewById(R.id.knot_image);


        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        screenDensity = displayMetrics.density;

        Button startButton = (Button) findViewById(R.id.start_game_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mState == STATE_RESTING) {
                    ballImage.setVisibility(View.VISIBLE);
                    mState = STATE_PLAYING;

                    mTimingBinder.setState(mState);
                    mTimingBinder.startCounting(GAME_DURATION);

                    mBallBinder.setState(mState);
                    mBallBinder.startATrial();

                    mTrackBinder.initialCatching();

                    restSecond = GAME_DURATION;
                    currentScore = 0;
                    resetUI();
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
                resetUI();
            }
        });

        //bind the bluetooth service
        Intent btBindIntent = new Intent(StartGame.this, BtService.class);
        bindService(btBindIntent, btServiceConnection, BIND_AUTO_CREATE);

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
            if (positions != null) {
                mTrackBinder.setParams(mHandler, mBtBinder, positions);
                mTrackBinder.setMax(maxMobileX, maxMobileY);
            }
            /*start tracking thread at the beginning;
            users are allowed to control the knot even
            when the game has not started*/
            mTrackBinder.startTrackingThread();
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

        //bind the timing service
        Intent timingIntent = new Intent(StartGame.this, TimingService.class);
        bindService(timingIntent, timingServiceConnection, BIND_AUTO_CREATE);

        mState = STATE_INITIALIZATION;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Intent stopBallIntent = new Intent(StartGame.this, BallService.class);
        stopService(stopBallIntent);
        Intent stopTrackIntent = new Intent(StartGame.this, TrackService.class);
        stopService(stopTrackIntent);
        Intent stopTimingIntent = new Intent(StartGame.this, TimingService.class);
        stopService(stopTimingIntent);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        //only be invoked when the activity is created
        if (mState == STATE_INITIALIZATION) {
            //get the position of holes
            ImageView hole0 = (ImageView) findViewById(R.id.hole_0);
            ImageView hole3 = (ImageView) findViewById(R.id.hole_3);

            holeX0 = hole0.getLeft();
            holeY0 = hole0.getTop();
            holeX1 = hole3.getLeft();
            holeY1 = hole3.getTop();

            ImageView knotImage = (ImageView) findViewById(R.id.knot_image);
            int knotWidth = knotImage.getWidth();
            int holeWidth = hole0.getWidth();
            maxMobileX = (holeX0 + holeX1) - (knotWidth - holeWidth);
            maxMobileY = (holeY0 + holeY1) - (knotWidth - holeWidth);

            positions = new int[]{holeX0, holeX1, holeY0, holeY1};

            //set params if binder is initialized, is the same as
            // functions in onServiceConnected
            if (mBallBinder != null)
                mBallBinder.setParams(mHandler, positions);
            if (mTrackBinder != null) {
                mTrackBinder.setParams(mHandler, mBtBinder, positions);
                mTrackBinder.setMax(maxMobileX, maxMobileY);
            }
            ballImage.setX(holeX0);
            ballImage.setY(holeY0);
        }
    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BtService.STATE_CONNECTED:
                            setBtStatus("Connected");
                            break;
                        case BtService.STATE_CONNECTING:
                            setBtStatus("Connecting");
                            break;
                        case BtService.STATE_NONE:
                            setBtStatus("Not connected.");
                            break;
                    }
                    break;

                case MESSAGE_BALL_ANIMATION:
                    TranslateAnimation animation = (TranslateAnimation)
                            msg.getData().getSerializable("animation");
//                    //set the ball at the final position of the animation
//                    animation.setFillAfter(false);
                    //TODO: wake up track thread at the end of animation
                    animation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            //to avoid flash of the ball image
                            ballImage.clearAnimation();
                            //Only set next hole and start catching when playing,
                            //if animation is ended by time_up, the following codes
                            //won't be executed
                            if (mState == STATE_PLAYING) {
                                ballImage.setX(nextHole.getHoleX());
                                ballImage.setY(nextHole.getHoleY());
                                mTrackBinder.setNextHole(nextHole);
                                mTrackBinder.startCatching();
                            }
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                    ballImage.startAnimation(animation);
                    break;

                case MESSAGE_NEXT_HOLE:
                    //set the next hole for track service
                    int nextHoleId = msg.getData().getInt("next_hole");
                    setNextHole(nextHoleId);
                    break;

                case MESSAGE_TRACKED:
                    currentScore++;

                    //successfully caught sound
                    try {
                        soundRing(StartGame.this);
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                    TextView scoreText = (TextView) findViewById(R.id.current_score_text);
                    scoreText.setText("" + currentScore);
                case MESSAGE_NOT_TRACKED:
                    mBallBinder.notifyTrialThread();
                    break;

                case MESSAGE_UPDATE_KNOT:
                    int[] mobilePos = msg.getData().getIntArray("mobile_pos");
                    knotImage.setLeft(mobilePos[0]);
                    knotImage.setTop(mobilePos[1]);
                    break;

                case MESSAGE_ONE_SECOND:
                    restSecond--;
                    updateTimerUI(restSecond);
                    break;

                case MESSAGE_TIME_UP:
                    mState = STATE_RESTING;

                    mBallBinder.stopPlaying();
                    mTrackBinder.stopPlaying();
//                    mTrackBinder.notifyTrackingThread();
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
    private void setBtStatus(CharSequence state) {
        TextView currentState = (TextView) findViewById(R.id.current_state_text_start_game);
        currentState.setText(state);
    }

    private void setNextHole(int nextHoleId) {
        switch (nextHoleId) {
            case 0:
                nextHole.setHoleX(holeX0);
                nextHole.setHoleY(holeY0);
                break;
            case 1:
                nextHole.setHoleX(holeX1);
                nextHole.setHoleY(holeY0);
                break;
            case 2:
                nextHole.setHoleX(holeX0);
                nextHole.setHoleY(holeY1);
                break;
            case 3:
                nextHole.setHoleX(holeX1);
                nextHole.setHoleY(holeY1);
                break;
        }
    }


    private void initialUI() {
        //set initial state as state connected
        setBtStatus("Connected");
        TextView remainingTimeText = (TextView) findViewById(R.id.remaining_time_text);
        remainingTimeText.setText("" + GAME_DURATION);
        ProgressBar remainingTimeBar = (ProgressBar) findViewById(R.id.remaining_time_bar);
        remainingTimeBar.setMax(GAME_DURATION);
        remainingTimeBar.setProgress(GAME_DURATION);
        TextView scoreText = (TextView) findViewById(R.id.current_score_text);
        scoreText.setText("" + 0);
        knotImage = (ImageView) findViewById(R.id.knot_image);
    }

    private void resetUI() {
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

    private void soundRing(Context context)
            throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
        MediaPlayer mp = new MediaPlayer();
        mp.reset();
        mp.setDataSource(context, Uri.fromFile(
                        new File("/system/media/audio/notifications/Bump.ogg")));
        mp.prepare();
        mp.start();
    }
}











