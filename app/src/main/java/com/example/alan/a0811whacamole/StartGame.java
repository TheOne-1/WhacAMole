package com.example.alan.a0811whacamole;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class StartGame extends AppCompatActivity {
    private final String TAG = "TAG";

    //duration per game, given in seconds
    private final int gameDuration = 30;
    
    private int screenWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_game);


        ProgressBar remainingTimeBar = (ProgressBar) findViewById(R.id.remaining_time_bar);
        remainingTimeBar.setMax(gameDuration);
        remainingTimeBar.setProgress(gameDuration);

    }



}











