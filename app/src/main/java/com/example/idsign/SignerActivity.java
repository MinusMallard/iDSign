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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signer);

        // Recycler View
        MyHostApduService.recyclerView = findViewById(R.id.recyclerView);
        List<Task> taskList = new ArrayList<>();
        taskList.add(new Task("Device registered with the PKG"));
        taskList.add(new Task("Temporary Keys Generated"));
        taskList.add(new Task("Exchanged Temporary Keys and Identities"));
        taskList.add(new Task("HandShake Traffic Key Generated Successfully"));
        taskList.add(new Task("Exchanged Encrypted Messages With Each Other"));
        taskList.add(new Task("Mutual Authentication Completed Successfully"));
        MyHostApduService.taskList = taskList;

        // Fetching TextView to show the default identity of Signer
        TextView defaultSignerId = findViewById(R.id.emailID);
        defaultSignerId.setText("Default EmailID : signerapp@hcecard.com");

        //Calling PKG Setup to generate keys
        try {
            PKG_Setup.setup(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Intent intent = new Intent(this, MyHostApduService.class);
        startService(intent);


    }




}