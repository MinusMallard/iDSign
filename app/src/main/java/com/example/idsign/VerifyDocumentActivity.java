package com.example.idsign;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.idsign.Utilities.PKG_Setup;
import com.example.idsign.Utilities.Utils;
import com.example.idsign.operations.PDFSignatureIntegrator;
import com.example.idsign.operations.SignatureVerification;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class VerifyDocumentActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> pickPdfLauncher;
    boolean validSignatures;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verify_document);

        try {
            PKG_Setup.setup(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Button selectDocToVerify = findViewById(R.id.selectDocForVerification);
        TextView docPath = findViewById(R.id.docAbsolutePathh);
        TextView signResult = findViewById(R.id.signOutcome);
        SignatureVerification sv = new SignatureVerification();

        selectDocToVerify.setOnClickListener(view -> {
            pickPdf();
        });

        // Intent to launch media picker
        pickPdfLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri uri = data.getData();
                            if (uri != null) {
                                String pdfPath = Utils.getPathFromUri(this,uri);
                                docPath.setText("Doc Path: "+pdfPath);
                                try {
                                    byte[] signaturesFromPDF = PDFSignatureIntegrator.extractSignature(pdfPath);
                                    if (signaturesFromPDF == null){
                                        signResult.setText("PDF doesn't have signatures");
                                    }else {
                                        validSignatures = sv.verifyMessage(pdfPath, "signerapp@hcecard.com", signaturesFromPDF);
                                    }
                                } catch (IOException | NoSuchAlgorithmException e) {
                                    throw new RuntimeException(e);
                                }
                                if (validSignatures){
                                    signResult.setText("Valid Signatures");
                                }else{
                                    signResult.setText("Invalid Signatures");
                                }


                                Log.d("MainActivity", "Selected PDF Path: " + pdfPath);
                                Toast.makeText(this, "Selected PDF Path: " + pdfPath, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );



    }

    private void pickPdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        pickPdfLauncher.launch(intent);
    }
}