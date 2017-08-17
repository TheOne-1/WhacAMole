package com.example.alan.a0811whacamole;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.MainThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

/**
 * This Activity lists any paired devices and devices detected in the area
 * after discovery. When a device is chosen by the user, the MAC address
 * of the device is sent back to the parent Activity in the result Intent.
 * <p>
 * Note from Tian:
 * Android 6.0 or higher version needs location permission when using
 * bluetooth. Location permission needs to be acquired by RunTime Permission.
 */

public class Controller extends AppCompatActivity {

    //Tag for Log
    private static final String TAG = "TAG";
    public static final int REQUEST_ENABLE_BT = 1;

    //Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    //Name of the connected device
    private String mConnectedDeviceName = null;

    //Member fields
    private BluetoothAdapter mBtAdapter;

    //Newly discovered devices
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    private ProgressBar progressBar;
    private BtService mBtService;
    private Button scanButton;
    BluetoothDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);


        //button for scanning other bt device
        scanButton = (Button) findViewById(R.id.search_device);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //check and get location permission
                if (ContextCompat.checkSelfPermission(Controller.this, Manifest.permission
                        .ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(Controller.this, new String[]
                            {Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    //1 is used for the switch in on RequestPermissionResult
                } else if (ContextCompat.checkSelfPermission(Controller.this, Manifest.permission
                        .ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(Controller.this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION}, 2);
                } else {
                    //Only do the discovery when permitted.
                    doDiscovery();
                    view.setVisibility(View.GONE);
                }
            }
        });

        //Initialize array adapters. One for already paired devices
        //and one for newly discovered devices
        ArrayAdapter<String> pairedDevicesArrayAdapter =
                new ArrayAdapter<String>(this, R.layout.device_item);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_item);

        // Find and set up the ListView for devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices_listview);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
        ListView newDevicesListView = (ListView) findViewById(R.id.available_devices_listview);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedDevicesArrayAdapter.add("No Devices Have Been Paired");
        }
        mBtService = new BtService(mHandler);
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBtService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBtService.getState() == BtService.STATE_NONE) {
                // Start the Bluetooth chat services
                mBtService.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permission, int[] grantResult) {
        switch (requestCode) {
            case 1:
                if (!(grantResult.length > 0 && grantResult[0] == PackageManager.PERMISSION_GRANTED))
                    Toast.makeText(Controller.this, "Permission denied.", Toast.LENGTH_SHORT).show();
            case 2:
                if (grantResult.length > 0 && grantResult[0] == PackageManager.PERMISSION_GRANTED) {
                    doDiscovery();
                    findViewById(R.id.search_device).setVisibility(View.GONE);
                } else {
                    Toast.makeText(Controller.this, "Permission denied.", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");

        //clear the new device list so that no duplicate devices will be displayed
        if (!mNewDevicesArrayAdapter.isEmpty())
            mNewDevicesArrayAdapter.clear();

        progressBar = (ProgressBar) findViewById(R.id.searching);
        progressBar.setVisibility(View.VISIBLE);
        TextView newDevicesText = (TextView) findViewById(R.id.other_device_text);
        newDevicesText.setVisibility(View.VISIBLE);
        ListView newDevicesListView = (ListView) findViewById(R.id.available_devices_listview);
        newDevicesListView.setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    //Updates the status on the right left corner
    private void setStatus(CharSequence state) {
        TextView currentState = (TextView) findViewById(R.id.current_state_text);
        currentState.setText(state);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode != Activity.RESULT_OK) {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Bluetooth is not enabled. Leaving...",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private void connectDevice(BluetoothDevice device, boolean secure) {
        if (secure == Constants.REQUEST_CONNECT_DEVICE_SECURE)
            mBtService.connect(device, Constants.REQUEST_CONNECT_DEVICE_SECURE);
        else
            mBtService.connect(device, Constants.REQUEST_CONNECT_DEVICE_INSECURE);
    }


    /**
     * The on-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Get the BluetoothDevice object
            device = mBtAdapter.getRemoteDevice(address);
            // Attempt to connect to the device
            connectDevice(device, Constants.REQUEST_CONNECT_DEVICE_SECURE);
        }
    };

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//                    mNewDevicesArrayAdapter.notifyDataSetChanged();
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                progressBar.setVisibility(View.INVISIBLE);
                scanButton.setVisibility(View.VISIBLE);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    mNewDevicesArrayAdapter.add("No Devices Found");
//                    mNewDevicesArrayAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BtService.STATE_CONNECTED:
                            /**
                             * In the connect thread, connected was invoked so that
                             * mConnectedDeviceName is initialized and can be quoted here.
                             */
                            setStatus("Connected to " + mConnectedDeviceName);
                            break;
                        case BtService.STATE_CONNECTING:
                            setStatus("Connecting");
                            break;
                        case BtService.STATE_LISTEN:
                        case BtService.STATE_NONE:
                            setStatus("Not connected.");
                            break;
                    }
                    break;

                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(Controller.this, "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;

                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    float[] posData = byteToFloat(readBuf);
                    Toast.makeText(Controller.this,
                            posData[0] + " " +
                            posData[1] + " " +
                            posData[2] + " " +
                            posData[3] + " " +
                            posData[4] + " " +
                            posData[5] + " " +
                            posData[6] + " " +
                            posData[7] + " " +
                            posData[8], Toast.LENGTH_SHORT).show();
                    break;

                case Constants.MESSAGE_TOAST:
                    if (null != Controller.this) {
                        Toast.makeText(Controller.this, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }


        private static final int dataSize = 9;      //size of the pose data
        public float[] byteToFloat(byte[] bArray) {
            float[] result = new float[dataSize];

            for (int i = 0; i < dataSize; i++) {
                int fInt = bArray[4 * i + 3] & 0xFF |
                        (bArray[4 * i + 2] & 0xFF) << 8 |
                        (bArray[4 * i + 1] & 0xFF) << 16 |
                        (bArray[4 * i + 0] & 0xFF) << 24;
                result[i] = Float.intBitsToFloat(fInt);
            }
            return result;
        }
    };
}









