package com.example.alan.a0811whacamole;

/**
 * Created by Alan on 2017/8/11.
 */

public interface Constants {
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final boolean REQUEST_CONNECT_DEVICE_SECURE = true;
    public static final boolean REQUEST_CONNECT_DEVICE_INSECURE = false;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

}
