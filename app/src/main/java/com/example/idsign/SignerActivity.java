package com.example.idsign;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import com.example.idsign.Utilities.MyHostApduService;
import com.example.idsign.Utilities.PKG_Setup;
import com.example.idsign.recycleView.*;

import java.util.ArrayList;
import java.util.List;

public class SignerActivity extends AppCompatActivity {

    public static String signerIdentity = "signerapp@hcecard.com";

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
        defaultSignerId.setText("Default EmailID : "+signerIdentity);

        //Calling PKG Setup to generate keys
        try {
            PKG_Setup.setup(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Intent intent = new Intent(this, MyHostApduService.class);
        startService(intent);
    }

    public void intentToSignerPage2(){
            // On HCE Connection Lost this method will run via the MyHostApduService class onDeactivated Method
            Intent intent = new Intent(this, SignerPage2.class);
            startActivity(intent);
    }
}