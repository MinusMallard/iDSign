package com.example.idsign.Utilities;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.idsign.SigneePage2;

public class BluetoothDeviceFoundReceiver extends BroadcastReceiver {
    private static final String TAG = "BluetoothDeviceFoundReceiver";

    private SigneePage2 signeePage2Activity;
    private String targetDeviceName;

    // No-argument constructor
    public BluetoothDeviceFoundReceiver() {
    }

    public void setHceReaderActivity(SigneePage2 activity) {
        this.signeePage2Activity = activity;
    }

    public void setTargetDeviceName(String deviceName) {
        this.targetDeviceName = deviceName;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Inside Broad Cast Receiver");
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.d(TAG, "Finding Devices");
            if (device != null && device.getName() != null) {
                String deviceName = device.getName();
                Log.d(TAG, "Discovered Device: " + deviceName);
                if (deviceName.equals(targetDeviceName)) {
                    // Notify the activity that the device was found
                    if (signeePage2Activity != null) {
                        signeePage2Activity.onDeviceFound(device); // Fixed this line
                    }
                }
            } else {
                Log.d(TAG, "Unnamed device found");
            }
        }
    }
}
