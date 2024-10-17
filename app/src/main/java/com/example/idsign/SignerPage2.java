package com.example.idsign;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.example.idsign.Utilities.MyHostApduService;
import com.example.idsign.Utilities.Utils;
import com.example.idsign.operations.PDFSignatureIntegrator;
import com.example.idsign.operations.SignatureCreation;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

public class SignerPage2 extends AppCompatActivity {

    private static final String TAG = "SignerAPP";
    private static final String APP_NAME = "SignerAppBluetooth";
    private static final UUID MY_UUID = UUID.fromString("e8e10f95-1a70-4b27-9ccf-02010264e9c8");
    private ActivityResultLauncher<Intent> discoverableIntentLauncher;
    private BluetoothAdapter bluetoothAdapter;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    TextView progressText;
    Boolean isReceived = false;
    Button openDoc,signDoc,sendSign,viewSignedDocSigner,signMoreFiles;
    String pathToReceivedFile;
    BluetoothDevice remoteDeviceName;
    private byte[] encryptedSignatures;
    byte[] signatures;
    String destinationPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signer_page2);

        // Fetching Progress Text
        progressText = findViewById(R.id.progressText);

        // Fetching Open Document button
        openDoc = findViewById(R.id.openDoc);

        // Fetching Sign Document button
        signDoc = findViewById(R.id.signDoc);
        sendSign = findViewById(R.id.sendSign);
        viewSignedDocSigner = findViewById(R.id.viewSignedDocSigner);
        signMoreFiles = findViewById(R.id.signMoreFiles);

        // Handle the back button press with the OnBackPressedDispatcher
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Custom back button behavior
                // Do nothing or add your own logic here
                // For example, show a toast or close the app
                // Toast.makeText(MainActivity.this, "Back button pressed!", Toast.LENGTH_SHORT).show();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);

        // Click listener to open the received document
        openDoc.setOnClickListener(view -> {
            if(isReceived && pathToReceivedFile!=null){
                File file = new File(pathToReceivedFile);

                Uri contentUri = FileProvider.getUriForFile(this,  "com.example.idsign.provider", file);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(contentUri, "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);  // Add this flag
                startActivity(intent);
            }else {
                Toast.makeText(this, "No path to the file received", Toast.LENGTH_SHORT).show();
            }
        });

        // Click listener to open the received document
        signDoc.setOnClickListener(view -> {
            if(isReceived && pathToReceivedFile!=null){
//                String docData = Utils.readPDFFileAsHexString(pathToReceivedFile);

                try {
                    Log.d(TAG,"Going for Signatures");
                    byte[] fileHash = Utils.calculateHash(pathToReceivedFile);
                    Log.d(TAG,"Original File Hash : "+Arrays.toString(fileHash));
                    SignatureCreation ob = new SignatureCreation(fileHash,SignerActivity.signerIdentity);
                    signatures = ob.signMessage(MyHostApduService.SKs);
                    Log.d(TAG,"Signatures Received, length : "+signatures.length);
                    Log.d(TAG,"Original Signatures : "+ Arrays.toString(signatures));
                    encryptedSignatures = Utils.encryptByteArray(signatures, MyHostApduService.HTK);
                    Log.d(TAG,"Encrypted Signatures length : "+ encryptedSignatures.length);
                    Log.d(TAG,"Encrypted Signatures : "+ Arrays.toString(encryptedSignatures));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                Toast.makeText(this,"Signatures Generated",Toast.LENGTH_SHORT).show();
                integrateAndSaveSignedDoc(signatures);
                progressText.setVisibility(View.GONE);
                signDoc.setVisibility(View.INVISIBLE);
                sendSign.setVisibility(View.VISIBLE);
                acceptThread.cancel();
            }else {
                Toast.makeText(this,"Document Not Received Yet",Toast.LENGTH_SHORT).show();
            }
        });

        viewSignedDocSigner.setOnClickListener(view -> {
            File fil = new File(destinationPath);

            Uri contentUri = FileProvider.getUriForFile(this,  "com.example.idsign.provider", fil);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);  // Add this flag
            startActivity(intent);
        });

        signMoreFiles.setOnClickListener(view -> {
            Intent intent = new Intent(SignerPage2.this, SignerPage2.class);
            startActivity(intent);
        });

        sendSign.setOnClickListener(view -> {
            connectToBluetoothDevice(remoteDeviceName);
        });



        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Make this device discoverable (using Activity Result API)
        discoverableIntentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        Toast.makeText(this, "Bluetooth discoverability not enabled", Toast.LENGTH_SHORT).show();
                    } else {
                        @SuppressLint("MissingPermission") boolean isDiscovering = bluetoothAdapter.startDiscovery();
                        Log.d("Bluetooth Discovery", "Discovery started: " + isDiscovering);
                    }
                }
        );

        // Lines to make the bluetooth discoverable and they should be put below the registerForActivityResult method
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600); // 10 minutes
        discoverableIntentLauncher.launch(discoverableIntent);  // Launch using the launcher

        acceptThread = new AcceptThread();
        acceptThread.start();


    }

    public File getUniqueFile(String baseFileName, File directory, String extension) {
        File file = new File(directory, baseFileName + extension);
        int fileCount = 0;

        // Check if the file already exists, and if it does, increment the counter
        while (file.exists()) {
            fileCount++;
            file = new File(directory, baseFileName + "(" + fileCount + ")" + extension);
        }

        return file;
    }

    private void integrateAndSaveSignedDoc(byte[] originalSignatures){
        // Get the public Downloads directory
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//        String fileName = "Digitally_Signed_Document.pdf";
//        File file = new File(downloadsDir, fileName);

        String baseFileName = "Digitally_Signed_Document";
        String fileExtension = ".pdf";

        // Get a unique file name
        File file = getUniqueFile(baseFileName, downloadsDir, fileExtension);

        destinationPath = file.getAbsolutePath();
        Log.d(TAG,"Destination path: " + destinationPath);

        // Integrating the signatures with the PDF
        try {
            Log.d(TAG,"Going for integration");
            // Fetching current Timestamp
            LocalDateTime currentTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTimestamp = currentTime.format(formatter);

            PDFSignatureIntegrator.embedSignatureWithField(pathToReceivedFile,destinationPath,originalSignatures,"Himanshu Sharma",formattedTimestamp);
//            PDFSignatureIntegrator.signPDF("/storage/emulated/0/Download/received_document.pdf",destinationPath,originalSignatures,signerIdentity);

//            byte[] fileHash = Utils.calculateHash(originalPdfPath);
//            Log.d(TAG,"Original File Hash : "+Arrays.toString(fileHash));
//
//            byte[] signedFileHash = Utils.calculateHash(destinationPath);
//            Log.d(TAG,"Signed File Hash : "+Arrays.toString(signedFileHash));
//            Log.d(TAG,"Integration Done");

//            assert fileHash != null;
//            if (Arrays.equals(fileHash, signedFileHash)){
//                Toast.makeText(this,"Digitally Signed PDF VERIFIED: Signatures are Valid",Toast.LENGTH_LONG).show();
//            }

            viewSignedDocSigner.setVisibility(View.VISIBLE);
        } catch (IOException e) {
            Log.d(TAG,"Error Occurred");
            throw new RuntimeException(e);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
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

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;
        private BluetoothSocket socket;

        @SuppressLint("MissingPermission")
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {

                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);

            } catch (Exception e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            serverSocket = tmp;
            Log.d("inside RUN IF","serverSocket initialised");
        }

        public void run() {
            Log.d("inside RUN","Just Started");
//            BluetoothSocket socket = null;
            while (!Thread.interrupted()) { // Keep running until interrupted
                try {
                    Log.d("inside RUN","Socket yet to accept");
                    socket = serverSocket.accept();
                    Log.d("inside RUN","Socket Accepted");
                } catch (Exception e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    Log.d("inside RUN IF","received something in socket");
                    manageConnectedSocketHCE_Card(socket);
                    try {
                        Log.d("inside RUN IF","going to close the socket");
                        //serverSocket.close();
                        Log.d("inside RUN IF","socket closed");
                    } catch (Exception e) {
                        Log.e(TAG, "Could not close the connect socket", e);
                    }
                    //break;
                }
            }
            Log.d("Accept thread run","Thread interrupted out of while loop");
        }

        @SuppressLint("MissingPermission")
        private void manageConnectedSocketHCE_Card(BluetoothSocket socket) {
            Log.d("inside ManageConnectedSocket","Just Started");

            // Storing the Remote device name for further connection
            remoteDeviceName = socket.getRemoteDevice();
            Log.d("Remote Device Name",remoteDeviceName.getName());

            // AsyncTask.execute(()->{
                InputStream inputStream = null;
                FileOutputStream fileOutputStream = null;
                OutputStream outputStream = null;
                try {

                    Log.d("inside ManageConnectedSocket","Try block");

                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();

                    Log.d("inside ManageConnectedSocket InputStream","Received something in Input Stream");

                    // Get the public Downloads directory
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

                    // Create a unique file name to avoid conflicts
//                    String fileName = "received_document_" + System.currentTimeMillis() + ".pdf";
                    String fileName = "received_document.pdf";
                    File file = new File(downloadsDir, fileName);

                    // Fetching the absolute path of the file received to use it later for opening it.
                    pathToReceivedFile = file.getAbsolutePath();
                    Log.d("File Path", pathToReceivedFile); // Log the path for debugging

//                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "received_document.pdf");
                    fileOutputStream = new FileOutputStream(file);

                    Log.d("inside ManageConnectedSocket","Saving the file");

                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    // Tracking the Progress
                    DataInputStream dataInputStream = new DataInputStream(inputStream);
                    int fileSize = dataInputStream.readInt(); // Read file size as an integer
                    Log.d("Received File Size","Expected FIle size: "+fileSize);
                    long totalBytesRead = 0;

                    Log.d("Manage Connected SOcket","going to read inputstream");

//                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                    while (true) {
                        bytesRead = inputStream.read(buffer);
                        fileOutputStream.write(buffer, 0, bytesRead);
                        if (bytesRead == -1 || new String(buffer, 0, bytesRead).contains("EOF")) {
                            break;
                        }

                        // Tracking the progress
                        totalBytesRead += bytesRead;

                        final int progress = (int) ((totalBytesRead * 100)/fileSize);
                        runOnUiThread(()->{
                            progressText.setText("Receiving Doc: "+progress+"%");
                        });

                        Log.d("Manage Connected Socket", "Reading bytes: " + bytesRead);

                    }
                    Log.d("inside ManageConnectedSocket", "Out of While loop");

                    fileOutputStream.flush();

                    outputStream.write("ready".getBytes());

                    Log.d("inside ManageConnectedSocket", "Sent ready signal");

                    runOnUiThread(() -> {
                        isReceived = true;
                        openDoc.setEnabled(true);
                        signDoc.setEnabled(true);
                        progressText.setText("Document Received");
                        Toast.makeText(SignerPage2.this, "File received in Downloads Folder", Toast.LENGTH_LONG).show();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error occurred when managing the connected socket", e);
                } finally {
                    try {
                        Log.d("Finally Block","Started Closing things");
                        if (inputStream != null) inputStream.close();
                        if (fileOutputStream != null) fileOutputStream.close();
                        if (outputStream != null) outputStream.close();

//                         if (socket != null) socket.close();
                        Log.d("Finally Block", "Closed all streams and socket");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
//            });
            Log.d("Manage Connected Socket", "out of Async task");
        }

        @SuppressLint("MissingPermission")
        public void cancel() {
            try {
//                // Unpair using reflection
//                if (socket != null && socket.getRemoteDevice() != null) {
//                    try {
//                        Method removeBondMethod = socket.getRemoteDevice().getClass().getMethod("removeBond");
//
//                        boolean result = false;
//                        Object returnValue = removeBondMethod.invoke(socket.getRemoteDevice());
//                        if (returnValue instanceof Boolean) {
//                            result = (Boolean) returnValue;
//                        }
//
//                        if (result) {
//                            Log.d(TAG, "Successfully unpaired device from receiver");
//                            runOnUiThread(() -> {
//                                Toast.makeText(SignerPage2.this, "Unpaired", Toast.LENGTH_SHORT).show();
//                            });
//                        } else {
//                            Log.e(TAG, "Failed to unpair device from receiver");
//                        }
//                    } catch (NoSuchMethodException e) {
//                        Log.e(TAG, "Method removeBond not found (receiver)", e);
//                    } catch (Exception e) {
//                        Log.e(TAG, "Error occurred while unpairing (receiver)", e);
//                    }
//                } else {
//                    Log.e(TAG, "Cannot unpair: socket or remote device is null (receiver)");
//                }

                Log.d("inside RUN IF","serverSocket Cancelled");
                serverSocket.close();
                Log.d("inside RUN IF","serverSocket closed");
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            Log.d("inside ConnectThread Constructor", "just started");
            BluetoothSocket tmp = null;

            try {
                Log.d("Inside Connect Thread ", "Got inside");
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            Log.d("Inside Connect Thread", "Outside Try Block");
            socket = tmp;
            Log.d("Inside Connect Thread", "Method Finished");
        }

        public void run() {
            if (ActivityCompat.checkSelfPermission(SignerPage2.this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.d("INSIDE IF BLOCK TO CHECK PERMISSIONS", "Will call check bluetooth method");
                // checkBluetoothPermissions();
            }
            bluetoothAdapter.cancelDiscovery();
            try {
                socket.connect();
                manageConnectedSocketSendByteArray(socket);
            } catch (Exception e) {
                Log.e(TAG, "Unable to connect; closing the socket", e);
                try {
                    socket.close();
                } catch (Exception ex) {
                    Log.e(TAG, "Could not close the client socket", ex);
                }
            }
            // Unpair and close the socket
            Log.d("run function","Called Cancel from run method");
            cancel();
            runOnUiThread(() -> {
                Log.d("changing UI","adding button");
                sendSign.setVisibility(View.INVISIBLE);
                signMoreFiles.setVisibility(View.VISIBLE);
            });
        }

        private void manageConnectedSocketSendByteArray(BluetoothSocket socket) {
            Log.d("Manage Connected Socket", "got inside");

            OutputStream outputStream = null;
            InputStream inputStream = null;
            try {
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();

                // Here, you need to prepare the byte array to be sent
                // For example, you can load a byte array from a file or generate it programmatically

                byte[] byteArrayToSend = encryptedSignatures; // Your byte array to send

//                // Sending the total byte array size so as to track the received progress on the other side
//                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
//                dataOutputStream.writeInt(byteArrayToSend.length);
//                dataOutputStream.flush();

                // Send the byte array
                outputStream.write(byteArrayToSend);
                outputStream.write("EOF".getBytes());
                outputStream.flush();

                // Wait for "ready" signal
                byte[] readyBuffer = new byte[5];
                int readyBytes = inputStream.read(readyBuffer); // Wait for "ready" from receiver
                String readySignal = new String(readyBuffer, 0, readyBytes);
                if ("ready".equals(readySignal)) {
                    Log.d("ManageConnectedSocket HCE Reader", "Received 'ready' signal");
                    runOnUiThread(() -> {
//                        progressBar.setVisibility(View.GONE);
//                        progressText.setText("Data Sent :)");
                        Toast.makeText(SignerPage2.this, "Data sent and acknowledged", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Log.d("ManageConnectedSocket HCE Reader", "Didn't receive 'ready' signal: " + readySignal); // If no "ready" is received
                }
            } catch (Exception e) {
                Log.e(TAG, "Error occurred when managing the connected socket", e);
            } finally {
                try {
                    Log.d("ManageConnectedSocket HCE Reader", "Closing streams and socket");
                    if (outputStream != null) outputStream.close();
                    if (inputStream != null) inputStream.close();
                    // if (socket != null) socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @SuppressLint("MissingPermission")
        public void cancel() {
            try {
//                // Unpair Device using Reflection
//                if (remoteDeviceName != null) {
//                    Method removeBondMethod = remoteDeviceName.getClass().getMethod("removeBond");
//                    boolean result = (boolean) removeBondMethod.invoke(remoteDeviceName);
//                    if (result) {
//                        Log.d(TAG, "Successfully unpaired device");
//                        runOnUiThread(() -> Toast.makeText(SignerPage2.this, "Unpaired", Toast.LENGTH_SHORT).show());
//                    } else {
//                        Log.e(TAG, "Failed to unpair device");
//                    }
//                }

                // Closing Socket
                Log.d("inside RUN IF", "socket Cancelled");
                if (socket != null) socket.close();
                Log.d("inside RUN IF", "socket closed");
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (connectThread!=null){
            Log.d("onSTOP","Called Cancel from OnStop");
            connectThread.cancel();
        }
    }
}