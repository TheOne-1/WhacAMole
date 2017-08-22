package com.example.alan.a0811whacamole;

/**
 * Created by Alan on 2017/8/11.
 */

public interface Constants {
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final int MESSAGE_BALL_ANIMATION = 6;
    public static final int MESSAGE_NEXT_HOLE = 7;
    public static final int MESSAGE_TRACKED = 8;
    public static final int MESSAGE_NOT_TRACKED = 9;
    public static final int MESSAGE_TRIAL_ENDED = 10;

    public static final boolean REQUEST_CONNECT_DEVICE_SECURE = true;
    public static final boolean REQUEST_CONNECT_DEVICE_INSECURE = false;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

}
