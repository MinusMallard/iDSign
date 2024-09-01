package com.example.idsign;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.idsign.Utilities.Utils;
import com.example.idsign.operations.PDFSignatureIntegrator;
import com.example.idsign.operations.SignatureCreation;
import com.example.idsign.operations.SignatureVerification;


import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class SigneePage3 extends AppCompatActivity {
    String TAG = "SigneePage3";
    String originalPdfPath;
    String signerIdentity;
    String destinationPath;
    byte[] originalSignatures;
    private ActivityResultLauncher<Intent> pickPdfLauncher;
    boolean validSignatures;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signee_page3);

        Button selectDocToVerify = findViewById(R.id.selectDocToVerify);
        TextView docPath = findViewById(R.id.docPath);
        TextView signResult = findViewById(R.id.signResult);
        SignatureVerification sv = new SignatureVerification();


        selectDocToVerify.setOnClickListener(view -> {
            pickPdf();
        });

        Button viewSignedDoc = findViewById(R.id.viewSignedDoc);

        // Setting the path of the original pdf
        originalPdfPath = SigneePage2.pdfPath;
        Log.d(TAG, "Original PDF Path: "+originalPdfPath);

        // TODO: Right now hardcoding the Signer's identity but later, it can be fetched from the signatures
        signerIdentity = "signerapp@hcecard.com";

        // Decrypting the received encrypted signatures
        try {
            originalSignatures = Utils.decryptByteArray(SigneePage2.receivedEncryptedSignatures,SigneeActivity.HTK);
            Log.d(TAG,"original Signatures Length: "+originalSignatures.length+ " , data: "+ Arrays.toString(originalSignatures));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Get the public Downloads directory
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String fileName = "Digitally_Signed_Document.pdf";
        File file = new File(downloadsDir, fileName);

        // Fetching the absolute path of the file to get the path
        destinationPath = file.getAbsolutePath();
        Log.d(TAG,"Destination path: " + destinationPath); // Log the path for debugging

        // Integrating the signatures with the PDF
        try {
            Log.d(TAG,"Going for integration");
            // Fetching current Timestamp
            LocalDateTime currentTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTimestamp = currentTime.format(formatter);

            PDFSignatureIntegrator.embedSignatureWithField(originalPdfPath,destinationPath,originalSignatures,"Himanshu Sharma",formattedTimestamp);
//            PDFSignatureIntegrator.signPDF("/storage/emulated/0/Download/received_document.pdf",destinationPath,originalSignatures,signerIdentity);

            byte[] fileHash = Utils.calculateHash(originalPdfPath);
            Log.d(TAG,"Original File Hash : "+Arrays.toString(fileHash));

            byte[] signedFileHash = Utils.calculateHash(destinationPath);
            Log.d(TAG,"Signed File Hash : "+Arrays.toString(signedFileHash));
            Log.d(TAG,"Integration Done");

            assert fileHash != null;
            if (Arrays.equals(fileHash, signedFileHash)){
                Toast.makeText(this,"Digitally Signed PDF VERIFIED: Signatures are Valid",Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Log.d(TAG,"Error Occurred");
            throw new RuntimeException(e);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }


        viewSignedDoc.setEnabled(true);
        viewSignedDoc.setOnClickListener(view -> {
            File fil = new File(destinationPath);

            Uri contentUri = FileProvider.getUriForFile(this,  "com.example.idsign.provider", fil);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);  // Add this flag
            startActivity(intent);

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