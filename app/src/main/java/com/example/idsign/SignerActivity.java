package com.example.idsign;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;

import com.example.idsign.recycleView.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

public class SignerActivity extends AppCompatActivity {

    private static final UUID MY_UUID = UUID.fromString("00001111-0000-1000-8000-00825F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_PERMISSIONS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signer);
        MyHostApduService.recyclerView = findViewById(R.id.recyclerView);


        List<Task> taskList = new ArrayList<>();
        taskList.add(new Task("Device registered with the PKG"));
        taskList.add(new Task("Temporary Keys Generated"));
        taskList.add(new Task("Exchanged Temporary Keys and Identities"));
        taskList.add(new Task("HandShake Traffic Key Generated Successfully"));
        taskList.add(new Task("Exchanged Encrypted Messages With Each Other"));
        taskList.add(new Task("Mutual Authentication Completed Successfully"));

        MyHostApduService.taskList = taskList;


        TextView defaultSignerId = findViewById(R.id.emailID);

        Log.d("Inside Signer 1 ", "Lets Begin");
        defaultSignerId.setText("Default EmailID : signerapp@hcecard.com");
        Log.d("Inside Signer 2 ", "Lets Begin");

        try {
            PKG_Setup.setup(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Log.d("Inside Signer 3 ", "Lets Begin");

        Intent intent = new Intent(this, MyHostApduService.class);
        startService(intent);


    }

    void initiateBluetoothConnection() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkAndRequestPermissions();

        new Thread(() -> {
            BluetoothSocket socket = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        BluetoothServerSocket serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("MyApp", MY_UUID);
                        socket = serverSocket.accept();
                        if (socket != null) {
                            // Connection established, ready to send/receive data
                            Toast.makeText(this, "CAN NOW SEND AND RECEIVE", Toast.LENGTH_LONG).show();
                            serverSocket.close();
                        }
                    } else {

                    }
                }else{
                    BluetoothServerSocket serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("MyApp", MY_UUID);
                    socket = serverSocket.accept();
                    if (socket != null) {
                        // Connection established, ready to send/receive data
                        Toast.makeText(this, "CAN NOW SEND AND RECEIVE", Toast.LENGTH_LONG).show();
                        serverSocket.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.BLUETOOTH,
                                android.Manifest.permission.BLUETOOTH_ADMIN,
                                android.Manifest.permission.BLUETOOTH_SCAN,
                                android.Manifest.permission.BLUETOOTH_CONNECT,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        REQUEST_PERMISSIONS);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.BLUETOOTH,
                                android.Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        REQUEST_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();

                // Call initiateBluetoothConnection here if the permissions are granted
            } else {
                // Permissions denied
                Toast.makeText(this, "Bluetooth permissions are mandatory. Please grant the permissions.", Toast.LENGTH_LONG).show();
                checkAndRequestPermissions(); // Request the permissions again
            }
        }
    }

}