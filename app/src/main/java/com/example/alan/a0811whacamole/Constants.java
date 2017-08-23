package com.example.alan.a0811whacamole;

/**
 * Created by Alan on 2017/8/11.
 */

public interface Constants {

    //duration per game, given in seconds
    public final int GAME_DURATION = 10;
    //define the scale of distance between real distance
    // and pixel, given in dp/mm
    public final float SCALE = 1;

    //the difficult of the game is controlled by the duration
    //of movement & waiting
    public final int EASY_MOVE_DURATION = 4;
    public final int HARD_MOVE_DURATION = 3;
    public final int CRAZY_MOVE_DURATION = 2;
    //the difficult of the game is controlled by the duration
    //of movement & waiting
    public final int EASY_WAIT_DURATION = 4;
    public final int HARD_WAIT_DURATION = 3;
    public final int CRAZY_WAIT_DURATION = 2;

    //to determine how many easy and hard round will be
    //launched; crazy round will not stop until the end
    //of the trial
    public final int EASY_ROUND = 3;
    public final int HARD_ROUND = 3;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_DEVICE_NAME = 2;
    public static final int MESSAGE_TOAST = 3;

    public static final int MESSAGE_BALL_ANIMATION = 4;
    public static final int MESSAGE_NEXT_HOLE = 5;
    public static final int MESSAGE_TRACKED = 6;
    public static final int MESSAGE_NOT_TRACKED = 7;

    public static final int MESSAGE_ONE_SECOND = 8;
    public static final int MESSAGE_TIME_UP = 9;

    public static final boolean REQUEST_CONNECT_DEVICE_SECURE = true;
    public static final boolean REQUEST_CONNECT_DEVICE_INSECURE = false;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

}
