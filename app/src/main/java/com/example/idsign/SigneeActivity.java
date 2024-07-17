package com.example.idsign;

import android.Manifest;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.idsign.recycleView.Task;
import com.example.idsign.recycleView.TaskAdapter;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import it.unisa.dia.gas.jpbc.Element;

public class SigneeActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    static Element SKd, PKd, EKs, PKs;
    String IDd = "signeeapp@hcereader.com";
    RecyclerView recyclerView;
    List<Task> taskList;
    TaskAdapter taskAdapter;
    private static final UUID MY_UUID = UUID.fromString("00001111-0000-1000-8000-00825F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signee);
        recyclerView = findViewById(R.id.recyclerViewSignee);

        taskList = new ArrayList<>();
        taskList.add(new Task("Device registered with the PKG"));
        taskList.add(new Task("Temporary Keys Generated"));
        taskList.add(new Task("Exchanged Temporary Keys and Identities"));
        taskList.add(new Task("HandShake Traffic Key Generated Successfully"));
        taskList.add(new Task("Exchanged Encrypted Messages With Each Other"));
        taskList.add(new Task("Mutual Authentication Completed Successfully"));


        Log.d("Please WORK", "Reader mode");

        TextView defaultSigneeId = findViewById(R.id.emailID);

        defaultSigneeId.setText("Default EmailID: " + IDd);


        // Generating Signee App Key Pair
        try {
            PKG_Setup.setup(this);
            PKd = PKG_Setup.getPublicKey(IDd);
            SKd = PKG_Setup.getPrivateKey(PKd);
            Log.d("SIGNEE APP PKd : ", PKd.toString());
            Log.d("SIGNEE APP SKd : ", SKd.toString());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        adapter.enableReaderMode(
                this,
                this,
                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
        );

    }


    @Override
    protected void onResume() {
        super.onResume();
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        adapter.enableReaderMode(
                this,
                this,
                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("data finished", "done");
        NfcAdapter.getDefaultAdapter(this).disableReaderMode(this);
    }

    public void init(Context context) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                taskAdapter = new TaskAdapter(context, taskList);
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
                recyclerView.setAdapter(taskAdapter);
            }
        });
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        init(this);
        Log.d("FOUND IT FINALLY 1", "TAG FOUND");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                taskAdapter.updateTask(0, true);
            }
        });
        IsoDep isoDep = IsoDep.get(tag);

        Log.d("FOUND IT FINALLY 2", "TAG FOUND");

        if (isoDep != null) {
            Log.d("FOUND IT FINALLY 3", "TAG FOUND");
            try {
                Log.d("FOUND IT FINALLY 4", "TAG FOUND");
                isoDep.connect();
                Log.d("FOUND IT FINALLY 5", "TAG FOUND");
                // ************************* 1st COMMAND APDU -> SELECT AID whether he matches with the application id or not ************************** //
                byte[] result = isoDep.transceive(Utils.SELECT_APDU);
                // ************************************************************************************************************************************* //

                // Log to verify the result that is received.
                Log.d("FOUND IT FINALLY 6", Utils.ByteArrayToHexString(result));

                // Created a variable which will always contain the received status
                byte[] status = new byte[]{result[0], result[1]};

                Log.d("FOUND IT FINALLY 7", Utils.ByteArrayToHexString(status));

                // ************************* 1st RESPONSE APDU -> RECEIVED SELECT_OK and PKs, EKs
                if (Arrays.equals(status, Utils.SELECT_OK_SW)) {
                    // Recover Lengths from result/responseAPDU
                    int EKs_Length = result[2] & 0xFF;

                    Log.d("FOUND IT FINALLY 8", Integer.toString(EKs_Length));

                    // Recover ByteArrays
                    byte[] PKs_Hash_Bytes = Arrays.copyOfRange(result, 3, 3 + 32);
                    byte[] EKs_Bytes = Arrays.copyOfRange(result, 3 + 32, result.length);

                    // Recover original point on elliptic curve
                    PKs = PKG_Setup.pairing.getG1().newElementFromHash(PKs_Hash_Bytes, 0, PKs_Hash_Bytes.length).getImmutable();
                    EKs = PKG_Setup.pairing.getG1().newElementFromBytes(EKs_Bytes).getImmutable();

                    Log.d("FOUND IT FINALLY 9 PKs : ", PKs.toString());
                    Log.d("FOUND IT FINALLY 10 EKs : ", EKs.toString());

                    // generate y, randomly chosen ephimeral secret
                    Element y = PKG_Setup.pairing.getZr().newRandomElement().getImmutable();

                    // Ephemeral Public key
                    Element EKd = PKG_Setup.P.duplicate().mulZn(y).getImmutable();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            taskAdapter.updateTask(1, true);
                            taskAdapter.updateTask(2, true);
                        }
                    });

                    // To compute SSd = y.EKs || e(EKs + H(IDs) , y.Ppub + SKd)

                    // Step - 1
                    Element y_EKs = EKs.duplicate().mulZn(y);

                    Element EKs_plus_H_IDs = EKs.duplicate().add(PKs);
                    Element y_Ppub_plus_SKd = PKG_Setup.Ppub.duplicate().mulZn(y).add(SKd);
                    Element SSd_PairingResult = PKG_Setup.pairing.pairing(EKs_plus_H_IDs, y_Ppub_plus_SKd);

                    // Step - 2 : Converting the result into byte arrays
                    byte[] y_EKs_bytes = y_EKs.toBytes();
                    byte[] SSd_PairingResult_bytes = SSd_PairingResult.toBytes();

                    // Step - 3 : Concatenating
                    byte[] SSd = new byte[y_EKs_bytes.length + SSd_PairingResult_bytes.length];
                    System.arraycopy(y_EKs_bytes, 0, SSd, 0, y_EKs_bytes.length);
                    System.arraycopy(SSd_PairingResult_bytes, 0, SSd, y_EKs_bytes.length, SSd_PairingResult_bytes.length);

                    // Generating HTK = HKDF(SSd,Ppub)
                    byte[] HTK = generateHTK(SSd, PKG_Setup.Ppub.toBytes());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            taskAdapter.updateTask(3, true);
                        }
                    });

                    Log.d("HTK GENERATED", Arrays.toString(HTK));

                    Log.d("FOUND IT FINALLY 11", "HTK GENERATED SUCCESSFULLLY");

                    // Generating TEST MESSAGE to check whether both parties have computed same Handshake Traffic Key
                    String testMessageToSend = generateRandomString(20);
                    byte[] encryptedMessage = encrypt(testMessageToSend, HTK);
                    byte encryptedMessageLength = (byte) encryptedMessage.length;

                    Log.d("FOUND IT FINALLY 12", "Message encrypted successfully");


                    // (Sending IDd as PKd is of 128 bytes which will go out of bound of commandAPDU)
                    // Preparing CommandAPDU = SIGNEE_HELLO [3 bytes] + LengthOfEncryptedMessage[1 byte] + Length bit[1 byte] + IDd[32 byte] + EKd[128 byte] + encryptedMessage[32 byte]
                    byte lengthBIT = (byte) (32 + EKd.toBytes().length + encryptedMessageLength);
                    byte[] commandAPDU = Utils.concatArrays(Utils.SEND_SIGNEE_HELLO, new byte[]{encryptedMessageLength}, new byte[]{lengthBIT}, Utils.sha256(IDd), EKd.toBytes(), encryptedMessage);
                    Log.d("FOUND IT FINALLY 13", commandAPDU.toString());


                    // ************************* 2nd COMMAND APDU -> Sending SIGNEE HELLO ****************************************************************** //
                    result = isoDep.transceive(commandAPDU);
                    // ************************************************************************************************************************************* //

                    status = new byte[]{result[0], result[1]};

                    if (Arrays.equals(status, Utils.SELECT_OK_SW)) {

                        encryptedMessageLength = (byte) (result[3] & 0xFF);
                        encryptedMessage = Arrays.copyOfRange(result, 3, result.length);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                taskAdapter.updateTask(4, true);

                            }
                        });

                        String testMessageReceived = decrypt(encryptedMessage, HTK);

                        if (testMessageToSend.equals(testMessageReceived)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    taskAdapter.updateTask(5, true);
                                    Toast.makeText(SigneeActivity.this, "MUTUAL AUTHENTICATION SUCCESSFULL", Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        commandAPDU = Utils.BLUETOOTH_REQUEST;

                        // ************************* 3rd COMMAND APDU -> Sending Bluetooth Connection Request ************************************************** //
                        result = isoDep.transceive(commandAPDU);
                        // ************************************************************************************************************************************* //


                        String bluetoothAddress = parseBluetoothAddress(result);
                        Log.d("BLUETOOTH ADDRESS OF HCE CARD: ",bluetoothAddress);
                        initiateBluetoothConnection(bluetoothAddress);
                        Log.d("Bluetooth initiated : ",Arrays.toString(result));

                    }

                }

            } catch (IOException e) {
                Log.d("error isodep", "Not Working");
                Log.e(Utils.TAG, "Error communicating with card: " + e);
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


        }
    }

    private String parseBluetoothAddress(byte[] result) {
        return String.format("%02X:%02X:%02X:%02X:%02X:%02X", result[0], result[1], result[2], result[3], result[4], result[5]);
    }

    private void initiateBluetoothConnection(String bluetoothAddress) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(bluetoothAddress);
        checkAndRequestPermissions();
        Log.d("Inside Bluetooth SOCKET","before thread starts");
        new Thread(() -> {
            BluetoothSocket socket = null;
            Log.d("Inside Bluetooth SOCKET","after thread starts");
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Log.d("Inside Bluetooth SOCKET","BUILD VERSION");
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        Log.d("Inside Bluetooth SOCKET","starting");
                        socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                        Log.d("Inside Bluetooth SOCKET","middle");
                        socket.connect();
                        Log.d("Inside Bluetooth SOCKET","YESSSSSSSSSSS");
                        Toast.makeText(this, "CAN NOW SEND AND RECEIVE", Toast.LENGTH_LONG).show();
                        Log.d("Inside Bluetooth SOCKET","YESSSSSSSSSSS");

                        // Connection established, ready to send/receive data
                    } else {
                        Log.e("BLUETOOTH PERMISSION NOT GRANTED", "Bluetooth connect permission not granted");
                    }
                } else {
                    socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    socket.connect();
                    Log.d("Inside Bluetooth SOCKET","YESSSSSSSSSSS");
                    Toast.makeText(this, "CAN NOW SEND AND RECEIVE", Toast.LENGTH_LONG).show();
                    Log.d("Inside Bluetooth SOCKET","YESSSSSSSSSSS");
                    // Connection established, ready to send/receive data
                }
            } catch (IOException e) {
                Log.d("Inside Bluetooth SOCKET","there is an exception");
                e.printStackTrace();
            }
        }).start();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                        },
                        REQUEST_PERMISSIONS);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ){

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN,
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
                Log.d("Inside Request Permission","YESSSSSSSSSSS");
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();

                // Call initiateBluetoothConnection here if the permissions are granted
            } else {
                // Permissions denied
                Toast.makeText(this, "Bluetooth permissions are mandatory. Please grant the permissions.", Toast.LENGTH_LONG).show();
                checkAndRequestPermissions(); // Request the permissions again
            }
        }
    }

    // Encrypt a string using AES and HTK
    public static byte[] encrypt(String data, byte[] HTK) throws Exception {
        SecretKey secretKey = new SecretKeySpec(HTK, 0, 16, "AES"); // Use first 16 bytes for AES-128, to use AES256 just replace the value 16 with 32
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    // Generate Handshake Traffic Key (HTK) using HKDF
    public static byte[] generateHTK(byte[] SS_D, byte[] P_Pub_bytes) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        HKDFParameters params = new HKDFParameters(SS_D, null, P_Pub_bytes);
        hkdf.init(params);

        // byte[] HTK = new byte[32]; // Length of output key (256 bits) if AES256 to be used , but AES256 is a little slower due to longer key length
        byte[] HTK = new byte[16]; // Here Length will be 128 bits as in this project I am going to use AES 128 which will give enough level of security and will be faster than AES256
        hkdf.generateBytes(HTK, 0, HTK.length);
        return HTK;
    }

    // Decrypt a byte array using AES and HTK
    public static String decrypt(byte[] encryptedData, byte[] HTK) throws Exception {
        SecretKey secretKey = new SecretKeySpec(HTK, 0, 16, "AES"); // Use first 16 bytes for AES-128
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedData = cipher.doFinal(encryptedData);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    public static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            sb.append(randomChar);
        }

        return sb.toString();
    }


}