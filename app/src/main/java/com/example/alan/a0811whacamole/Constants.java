package com.example.alan.a0811whacamole;

/**
 * Created by Alan on 2017/8/11.
 */

public interface Constants {

    //duration per game, given in seconds 在这里设置游戏时间
    public final int GAME_DURATION = 60;

//    //define the scale of distance between real distance
//    // and pixel, given in pixel/mm, not dp 在这里设置尺度
//    public final float SCALE = 1;

    //minimum match distance on the screen在这里设置最小匹配距离
    public final int MIN_MATCH_DIS = 20;

    //interval between two position data inquiry查询位资数据间隔(ms)
    public final int POS_DATA_INQUIRY_INTERVAL = 50;
    //the difficult of the game is controlled by the duration
    //of movement & waiting
    public final int EASY_MOVE_DURATION = 4;
    public final int HARD_MOVE_DURATION = 3;
    public final int CRAZY_MOVE_DURATION = 2;

    //the difficult of the game is controlled by the duration
    //of movement & waiting 在这里设置等待时间
    public final int EASY_WAIT_DURATION = 15;
    public final int HARD_WAIT_DURATION = 10;
    public final int CRAZY_WAIT_DURATION = 5;

    //to determine how many easy and hard round will be
    //launched; crazy round will not stop until the end
    //of the trial
    public final int EASY_ROUND = 3;
    public final int HARD_ROUND = 3;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_CONNECTION_LOST = 0;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_DEVICE_NAME = 2;
    public static final int MESSAGE_TOAST = 3;
    public static final int MESSAGE_START_GAME = 4;

    public static final int MESSAGE_BALL_ANIMATION = 5;
    public static final int MESSAGE_NEXT_HOLE = 6;
    public static final int MESSAGE_TRACKED = 7;
    public static final int MESSAGE_NOT_TRACKED = 8;
    public static final int MESSAGE_UPDATE_KNOT = 9;
    public static final int MESSAGE_ONE_SECOND = 10;
    public static final int MESSAGE_TIME_UP = 11;

    public static final boolean REQUEST_CONNECT_DEVICE_SECURE = true;
    public static final boolean REQUEST_CONNECT_DEVICE_INSECURE = false;

    public final boolean CATCHABLE = true;
    public final boolean UNCATCHABLE = false;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";


}
