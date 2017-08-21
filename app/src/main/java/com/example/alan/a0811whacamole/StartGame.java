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

    private int nextHole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_game);
        ballImage = (ImageView) findViewById(R.id.ball_image);



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
                }
            }
        });
    }

    private ServiceConnection connection = new ServiceConnection() {
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        ImageView hole0 = (ImageView) findViewById(R.id.hole_0);
        holeX0 = hole0.getLeft();
        holeY0 = hole0.getTop();
        ImageView hole1 = (ImageView) findViewById(R.id.hole_3);
        holeX1 = hole1.getLeft();
        holeY1 = hole1.getTop();
        Intent intent = new Intent(StartGame.this, BallService.class);
        intent.putExtra("holes_position", new int[] {holeX0, holeX1, holeY0, holeY1});
        startService(intent);
        Intent bindIntent = new Intent(this, BallService.class);
        //bind the service and StartGame class
        bindService(bindIntent, connection, BIND_AUTO_CREATE);

    }

    public static final int MESSAGE_BALL_ANIMATION = 0;
    public static final int MESSAGE_NEXT_HOLE = 1;
    public static final int MESSAGE_TRIAL_ENDED = 3;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_BALL_ANIMATION:
                    TranslateAnimation animation = (TranslateAnimation)
                            msg.getData().getSerializable("animation");
                    animation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {}

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mBallBinder.notifyTrialThread();
                            //startCatching. time should be controlled by this thread. and notify trial thread afterwards.
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });
                    ballImage.startAnimation(animation);
                    break;
                case MESSAGE_NEXT_HOLE:
                    //set the next hole for track service
                    nextHole = msg.getData().getInt("next_hole");
                    break;

                case MESSAGE_TRIAL_ENDED:
                    mState = STATE_RESTING;
                    ballImage.setVisibility(View.INVISIBLE);

            }
        }
    };

}











