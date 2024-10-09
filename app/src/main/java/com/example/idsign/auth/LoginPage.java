package com.example.idsign.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.idsign.MainActivity;
import com.example.idsign.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class LoginPage extends AppCompatActivity {

    EditText enterNumber,verifyOTPEditText;
    Button getOTP,verifyOTP;
    String verificationID_OTP;

    FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);

        enterNumber = findViewById(R.id.enterNumber);
        verifyOTPEditText = findViewById(R.id.verifyOTPEditText);
        getOTP = findViewById(R.id.getOTP);
        verifyOTP = findViewById(R.id.verifyOTP);


        getOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!enterNumber.getText().toString().trim().isEmpty()){
                    if ((enterNumber.getText().toString().trim()).length() == 10){

                        Log.d("Number is",enterNumber.getText().toString());

                        // Build the PhoneAuthOptions
                        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                                .setPhoneNumber("+91" + enterNumber.getText().toString())       // Phone number to verify
                                .setTimeout(60L, TimeUnit.SECONDS)                              // Timeout and unit
                                .setActivity(LoginPage.this)                                    // Activity (for callback binding)
                                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                    @Override
                                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {

                                    }

                                    @Override
                                    public void onVerificationFailed(@NonNull FirebaseException e) {
                                        Log.d("Verificaion Failer","Failed Verification");
                                        Toast.makeText(LoginPage.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onCodeSent(@NonNull String verificationId,
                                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                        verificationID_OTP = verificationId;
                                        getOTP.setVisibility(View.GONE);
                                        verifyOTP.setVisibility(View.VISIBLE);
                                        enterNumber.setVisibility(View.GONE);
                                        verifyOTPEditText.setVisibility(View.VISIBLE);
                                    }
                                })
                                .build();


                        PhoneAuthProvider.verifyPhoneNumber(options);

                    }else{
                        Toast.makeText(LoginPage.this,"Please enter correct number",Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(LoginPage.this,"Enter Mobile Number",Toast.LENGTH_SHORT).show();
                }
            }
        });

        verifyOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!verifyOTPEditText.getText().toString().trim().isEmpty()){
                    PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(verificationID_OTP,verifyOTPEditText.getText().toString());

                    FirebaseAuth.getInstance().signInWithCredential(phoneAuthCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }else{
                                Toast.makeText(LoginPage.this,"Enter the correct OTP",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }else{
                    Toast.makeText(LoginPage.this,"Enter the OTP",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}