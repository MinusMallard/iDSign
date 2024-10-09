package com.example.idsign;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.idsign.auth.LoginPage;

public class SplashScreen extends AppCompatActivity {

    boolean isRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        if (isRegistered){
            Intent intent = new Intent(SplashScreen.this, LoginPage.class);
            startActivity(intent);
        }else{
            Intent intent = new Intent(SplashScreen.this, LoginPage.class);
            startActivity(intent);
        }

    }
}