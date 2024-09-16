package com.example.idsign.Utilities;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.example.idsign.SigneePage2;

import java.util.HashSet;
import java.util.Set;

public class BluetoothDeviceFoundReceiver extends BroadcastReceiver {
    private static final String TAG = "BluetoothDeviceFoundReceiver";

    private SigneePage2 signeePage2Activity;
    private String targetDeviceName;
    private BluetoothAdapter bluetoothAdapter;
    private Set<String> discoveredDevices = new HashSet<>();

    // No-argument constructor
    public BluetoothDeviceFoundReceiver() {
    }

    public void setHceReaderActivity(SigneePage2 activity) {
        this.signeePage2Activity = activity;
    }

    public void setTargetDeviceName(String deviceName) {
        this.targetDeviceName = deviceName;
    }

    public void setBluetoothAdapter(BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter; // Set the BluetoothAdapter
    }

    @SuppressLint("MissingPermission")
    @Override
//    public void onReceive(Context context, Intent intent) {
//        String action = intent.getAction();
//        Log.d(TAG, "Inside Broad Cast Receiver");
//        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//            Log.d(TAG, "Finding Devices");
//            if (device != null && device.getName() != null) {
//                String deviceName = device.getName();
//                Log.d(TAG, "Discovered Device: " + deviceName);
//                if (deviceName.equals(targetDeviceName)) {
//                    // Notify the activity that the device was found
//                    if (signeePage2Activity != null) {
//                        signeePage2Activity.onDeviceFound(device); // Fixed this line
//                    }
//                }
//            } else {
//                Log.d(TAG, "Unnamed device found");
//            }
//        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//            Log.d(TAG, "Discovery finished, restarting discovery");
//            bluetoothAdapter.startDiscovery(); // Restart discovery after it finishes
//        }
//    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Inside Broad Cast Receiver");

        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.d(TAG, "Finding Devices");
            if (device != null && device.getName() != null) {
                String deviceName = device.getName();
                String deviceAddress = device.getAddress(); // Get the device address (MAC)

                if (!discoveredDevices.contains(deviceAddress)) { // Check if device is already discovered
                    discoveredDevices.add(deviceAddress); // Add the new device to the set

                    Log.d(TAG, "Discovered Device: " + deviceName + " - " + deviceAddress);

                    // If it's the target device, notify the activity
                    if (deviceName.equals(targetDeviceName) && signeePage2Activity != null) {
                        signeePage2Activity.onDeviceFound(device);
                    }
                } else {
                    Log.d(TAG, "Device already discovered: " + deviceName);
                }
            } else {
                Log.d(TAG, "Unnamed device found");
            }
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            Log.d(TAG, "Discovery finished, restarting discovery");
            bluetoothAdapter.startDiscovery(); // Restart discovery after it finishes
        }
    }

    // Optionally, you can clear discovered devices if needed when starting a fresh search
    public void clearDiscoveredDevices() {
        discoveredDevices.clear();
    }
}
