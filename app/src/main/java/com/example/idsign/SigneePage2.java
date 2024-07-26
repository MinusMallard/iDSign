package com.example.idsign;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.idsign.Utilities.BluetoothDeviceFoundReceiver;
import com.example.idsign.Utilities.Utils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class SigneePage2 extends AppCompatActivity {

    private static final String TAG = "SigneeAPP";
    private static final UUID MY_UUID = UUID.fromString("e8e10f95-1a70-4b27-9ccf-02010264e9c8");
    BluetoothDeviceFoundReceiver receiver;
    private BluetoothAdapter bluetoothAdapter;
    private ActivityResultLauncher<Intent> discoverableIntentLauncher;
    private ActivityResultLauncher<Intent> pickPdfLauncher;
    private String pdfPath;
    private BluetoothDevice foundDevice;
    private boolean isReadyToSend = false;
    private Button sendButton;
    private ProgressBar progressBar;
    private TextView progressText;
    private String targetDeviceName;
    private boolean isReceiverRegistered = false;
    private ConnectThread connectThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signee_page2);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Fetching Chooser Button
        Button chooseDocument = findViewById(R.id.buttonView);
        TextView textView = findViewById(R.id.textView);
        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.progressBar);

        // Fetching Send Button
        sendButton = findViewById(R.id.sendButton);

        // On Chooser button clicked
        chooseDocument.setOnClickListener(v -> {
            pickPdf();
        });

        // On Send Button clicked
        sendButton.setOnClickListener(view -> {
            if (isReadyToSend && pdfPath != null) {
                progressBar.setVisibility(View.VISIBLE);
                progressText.setText("Sending Document..");
                connectToBluetoothDevice(foundDevice); // Use the foundDevice stored in pairDevice
            } else {
                Toast.makeText(this, "Not ready to send yet or no file selected", Toast.LENGTH_SHORT).show();
            }
        });

        // Intent to make this device discoverable (using Activity Result API)
        discoverableIntentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        Toast.makeText(this, "Bluetooth discoverability not enabled", Toast.LENGTH_SHORT).show();
                    } else {
                        @SuppressLint("MissingPermission")
                        boolean isDiscovering = bluetoothAdapter.startDiscovery();
                        Log.d("Bluetooth Discovery", "Discovery started: " + isDiscovering);
                    }
                }
        );

        // Intent to launch media picker
        pickPdfLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri uri = data.getData();
                            if (uri != null) {
                                pdfPath = Utils.getPathFromUri(this,uri);
                                textView.setText("Doc Path: "+pdfPath);
                                Log.d("MainActivity", "Selected PDF Path: " + pdfPath);
                                Toast.makeText(this, "Selected PDF Path: " + pdfPath, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
        pairDevice(SigneeActivity.deviceName);

    }

    private void pickPdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        pickPdfLauncher.launch(intent);
    }

    private void pairDevice(String deviceName) {
        targetDeviceName = deviceName;

        // Making device discoverable
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600); // 10 minutes
        discoverableIntentLauncher.launch(discoverableIntent);  // Launch using the launcher

        runOnUiThread(()->{
            // Make progressBar visible with text
            progressBar.setVisibility(View.VISIBLE);
            progressText.setVisibility(View.VISIBLE);
            progressText.setText("Started Pairing Process");
        });

        Log.d(TAG,"Starting Braodcast Receiver");
        receiver = new BluetoothDeviceFoundReceiver();
        receiver.setHceReaderActivity(this);
        receiver.setTargetDeviceName(targetDeviceName);

    }

    @SuppressLint("MissingPermission")
    public void onDeviceFound(BluetoothDevice device) {
        Log.d(TAG, "Device found: " + device.getName());
//        bluetoothAdapter.cancelDiscovery();
        unregisterReceiver(receiver);
        isReceiverRegistered = false;

        foundDevice = device; // Store the discovered device
        isReadyToSend = true;
        Log.d("inside Broadcast", "Device paired and ready to send.");
        runOnUiThread(() ->{
            // Alternate Code in order to send document when the send button is clicked
            progressText.setText("Device found: " + targetDeviceName);
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(SigneePage2.this,"Device is Ready to Send the Document",Toast.LENGTH_SHORT).show();
            sendButton.setEnabled(true);
        } ); // Enable the send button
    }

    @Override
    protected void onResume() {
        super.onResume();
        // These lines should be placed below BroadCast Receiver so that after discovering first device it again run these lines and after discover more devices
        if (!isReceiverRegistered && receiver != null && targetDeviceName != null) {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(receiver, filter);
            isReceiverRegistered = true; // Mark the receiver as registered
        }
    }

    private void connectToBluetoothDevice(BluetoothDevice device) {
        if (device != null) {
            connectThread = new ConnectThread(device);
            connectThread.start();
        } else {
            Log.d(TAG, "No device selected for connection.");
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            Log.d("inside ConnectThread Constructor","just started");
            BluetoothSocket tmp = null;

            try {
                Log.d("Inside Connect Thread ","Got inside");
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            Log.d("Inside Connect Thread","Outside Try Block");
            socket = tmp;
            Log.d("Inside Connect Thread","Method Finished");
        }

        public void run() {
            if (ActivityCompat.checkSelfPermission(SigneePage2.this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.d("INSIDE IF BLOCK TO CHECK PERMISSIONS","Will call check bluetooth method");
                // checkBluetoothPermissions();
            }
            bluetoothAdapter.cancelDiscovery();
            try {
                socket.connect();
                manageConnectedSocketHCE_Reader(socket);
            } catch (Exception e) {
                Log.e(TAG, "Unable to connect; closing the socket", e);
                try {
                    socket.close();
                } catch (Exception ex) {
                    Log.e(TAG, "Could not close the client socket", ex);
                }
            }
        }

        private void manageConnectedSocketHCE_Reader(BluetoothSocket socket) {
            Log.d("Manage Connected SOcket","got inside");

//            AsyncTask.execute(()->{
                OutputStream outputStream = null;
                FileInputStream fileInputStream = null;
                InputStream inputStream = null;
                try {

                    outputStream = socket.getOutputStream();
                    inputStream = socket.getInputStream();

                    File file = new File(pdfPath);

                    if (!file.exists()) {
                        Log.e(TAG, "File does not exist: " + file.getAbsolutePath());
                        return;
                    }

                    Log.d("Manage Connected SOcket","file selected"+file.getAbsolutePath());

                    fileInputStream = new FileInputStream(file);

                    byte[] buffer = new byte[1024*2];
                    int bytesRead;

                    // Sending the total file size so as to track the received progress on the other side
                    DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                    dataOutputStream.writeInt((int) file.length());
                    dataOutputStream.flush();
                    // **************************************

                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        Log.d("Manage Connected Socket", "Writing bytes: " + bytesRead);
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    Log.d("Inside manage connected socket","out of while loop in HCE Reader");

                    outputStream.write("EOF".getBytes());
                    outputStream.flush();

                    // Wait for "ready" signal
                    byte[] readyBuffer = new byte[5];
                    int readyBytes = inputStream.read(readyBuffer); // Wait for "ready" from HCE Card
                    String readySignal = new String(readyBuffer, 0, readyBytes);
                    if ("ready".equals(readySignal)) {
                        Log.d("ManageConnectedSocket HCE Reader", "Received 'ready' signal");
                        runOnUiThread(() ->{
                            progressBar.setVisibility(View.GONE);
                            progressText.setText("Document Sent :)");
                            Toast.makeText(SigneePage2.this, "File sent and acknowledged", Toast.LENGTH_SHORT).show();
                        } );
                    } else {
                        Log.d("ManageConnectedSocket HCE Reader", "Didn't receive 'ready' signal: " + readySignal); // If no "ready" is received
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error occurred when managing the connected socket", e);
                } finally {
                    try {
                        Log.d("ManageConnectedSocket HCE Reader", "Closing streams and socket");
                        if (fileInputStream != null) fileInputStream.close();
                        if (outputStream != null) outputStream.close();
                        if(inputStream!= null) inputStream.close();

                        //if (socket != null) socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
//            });

        }
        @SuppressLint("MissingPermission")
        public void cancel() {
            try {
                // Unpair Device using Reflection
                if(foundDevice != null) {
                    Method removeBondMethod = foundDevice.getClass().getMethod("removeBond");
                    boolean result = (boolean) removeBondMethod.invoke(foundDevice);
                    if (result) {
                        Log.d(TAG, "Successfully unpaired device");
                        runOnUiThread(() -> Toast.makeText(SigneePage2.this, "Unpaired", Toast.LENGTH_SHORT).show());
                    } else {
                        Log.e(TAG, "Failed to unpair device");
                    }
                }

                Log.d("inside RUN IF","socket Cancelled");
                socket.close();
                Log.d("inside RUN IF","socket closed");
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e){
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isReceiverRegistered && receiver != null) { // Unregister only if registered
            unregisterReceiver(receiver);
            Log.d("Unregister Receiver","Successfully Unregistered");
        }
        if(connectThread!=null){
            connectThread.cancel();
        }
    }

}