package com.example.alan.a0811whacamole;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
 */

public class Controller extends AppCompatActivity {

    //Tag for Log
    private static final String TAG = "DeviceListActivity";
    public static final int REQUEST_ENABLE_BT = 1;

    //Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    //Member fields
    private BluetoothAdapter mBtAdapter;

    //Newly discovered devices
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    private ProgressBar progressBar;

    Button scanButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        scanButton = (Button) findViewById(R.id.search_device);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doDiscovery();
                view.setVisibility(View.GONE);
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
                pairedDevicesArrayAdapter.add(device.getName() + "/n" + device.getAddress());
            }
        } else {
            pairedDevicesArrayAdapter.add("No Devices Have Been Paired");
        }
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
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
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


    /**
     * The on-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

//            // Get the device MAC address, which is the last 17 chars in the View
//            String info = ((TextView) v).getText().toString();
//            String address = info.substring(info.length() - 17);
//
//            // Create the result Intent and include the MAC address
//            Intent intent = new Intent();
//            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
//
//            // Set result and finish this Activity
//            setResult(Activity.RESULT_OK, intent);
//            finish();
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
                } break;
        }
    }
}
