package com.example.idsign;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.idsign.Utilities.PKG_Setup;
import com.example.idsign.Utilities.Utils;
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
    public static String deviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signee);

        // Handling Recycler View
        recyclerView = findViewById(R.id.recyclerViewSignee);
        taskList = new ArrayList<>();
        taskList.add(new Task("Device registered with the PKG"));
        taskList.add(new Task("Temporary Keys Generated"));
        taskList.add(new Task("Exchanged Temporary Keys and Identities"));
        taskList.add(new Task("HandShake Traffic Key Generated Successfully"));
        taskList.add(new Task("Exchanged Encrypted Messages With Each Other"));
        taskList.add(new Task("Mutual Authentication Completed Successfully"));

        // Fetching Text View to set the default identity of Signee
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

        // Handling NFC Reader Mode
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

        if (isoDep != null) {
            try {

                isoDep.connect();

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

                    // Generating random TEST MESSAGE and encrypting it to check whether both parties have computed same Handshake Traffic Key
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

                        // Fetching received encrypted message
                        encryptedMessage = Arrays.copyOfRange(result, 2, result.length);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                taskAdapter.updateTask(4, true);
                            }
                        });

                        // Decrypting the received message with HTK
                        String testMessageReceived = decrypt(encryptedMessage, HTK);

                        // Checking the received encrypted message for authentication purpose
                        if (testMessageReceived.equals(testMessageToSend+"SUCCESS")) {
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

                        deviceName = decrypt(result,HTK);
                        Log.d("Remote Device Name: ",deviceName);

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