package com.example.idsign.Utilities;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import com.example.idsign.SignerActivity;
import com.example.idsign.SignerPage2;
import com.example.idsign.recycleView.*;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.plaf.jpbc.util.io.Base64;

public class MyHostApduService extends HostApduService {

    private String TAG = "MyHostApduService";
    public static Element PKs,SKs,PKd,EKd,x;
    public static Element PKs_check,SKs_check;
    public static List<Task> taskList = new ArrayList<>();
    private TaskAdapter taskAdapter;
    public static RecyclerView recyclerView;
    private String deviceName ;
    private byte[] responseAPDU;
    public static byte[] HTK;
    // Signer's Identity
    String IDs = "signerapp@hcecard.com";


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            String base64PublicKey = intent.getStringExtra("hash");
            Log.d("BASE64",base64PublicKey);
            String base64PrivateKey = intent.getStringExtra("privateKey");
            PKG_Setup.setup(this);

            // Convert the Base64-encoded public key to a byte array
            byte[] publicKeyBytes = Base64.decode(base64PublicKey);
//            byte[] publicKeyBytes = base64PublicKey.getBytes("UTF-8");

            // Convert the byte array to an Element
            PKs = PKG_Setup.pairing.getG1().newElementFromBytes(publicKeyBytes).getImmutable();
            PKs_check = PKG_Setup.getPublicKey(IDs);

            // Convert the Base64-encoded private key to a byte array
            byte[] privateKeyBytes = Base64.decode(base64PrivateKey);
//            byte[] privateKeyBytes = base64PrivateKey.getBytes("UTF-8");

            // Convert the byte array to an Element
            SKs = PKG_Setup.pairing.getG1().newElementFromBytes(privateKeyBytes).getImmutable();
            SKs_check = PKG_Setup.getPrivateKey(PKs_check);

        } catch (Exception e) {
            // Handle any errors that might occur
            e.printStackTrace();
        }
        Log.d(TAG+" PKs : ",PKs.toString());
        Log.d(TAG+" SKs : ",SKs.toString());
        Log.d(TAG+" PKs_check : ",PKs_check.toString());
        Log.d(TAG+" SKs_check : ",SKs_check.toString());
        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressLint("MissingPermission")
    public void init(){
        taskAdapter = new TaskAdapter(this,taskList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(taskAdapter);
        deviceName = BluetoothAdapter.getDefaultAdapter().getName();
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {

        Log.d("HOST APDU SERVICE 1","INSIDE process command apdu");
        if (Arrays.equals(commandApdu, Utils.SELECT_APDU)){
            init();
            taskAdapter.updateTask(0,true);

            // Sending RESPONSE_APDU = 0x9000 as status word in the first two mandatory bytes and REST BYTES EMPTY
            Log.d("HOST APDU SERVICE 2","Received 1st APDU");

            // GENERATING EKs
            // generate x , randomly chosen ephimeral secret
            x = PKG_Setup.pairing.getZr().newRandomElement().getImmutable();

            // EKs = x.P
            Element EKs = PKG_Setup.P.duplicate().mulZn(x).getImmutable();

            taskAdapter.updateTask(1,true);

            Log.d("EKs generated EKs : ", EKs.toString());
            byte EKs_Length = (byte) EKs.toBytes().length; // original length = 128 but after converting to byte it became -128

            Log.d("EKs in Bytes :",Arrays.toString(EKs.toBytes()));
            try {
                Log.d("IDs in Bytes :",Arrays.toString(Utils.sha256(IDs)));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            Log.d("EKs_Length",Integer.toString(EKs_Length));

            // ResponseAPDU = SelectOK([0x90][0x00])+LengthsOf EKs([EKs_Length])+[IDs]+[EKs_Bytes]
            // Length of PKs and EKs is same as both are from G1 curve
            try {
                responseAPDU = Utils.concatArrays(Utils.SELECT_OK_SW, new byte[]{EKs_Length}, Utils.sha256(IDs), EKs.toBytes());
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            Log.d("HOST APDU SERVICE 3 ResponseAPDU",Arrays.toString(responseAPDU));

            return responseAPDU;

        }else if (commandApdu[0] == (byte) 0x80 && commandApdu[1] == (byte) 0x01){

            Log.d("HOST APDU SERVICE 4","Second APDU received");

            int encryptedMessageLength = commandApdu[3] & 0xFF;

            Log.d("Check CommandAPDU length",Integer.toString(commandApdu.length));
            Log.d("check encrypLength",Integer.toString(encryptedMessageLength));
            Log.d("check length bit",Integer.toString(commandApdu[4] & 0xFF));

            byte[] PKd_Hash_Bytes = Arrays.copyOfRange(commandApdu,5, 5+32);

            Log.d("PKd Hash",PKd_Hash_Bytes.toString());
            byte[] EKd_bytes = Arrays.copyOfRange(commandApdu,5+32,PKs.getLengthInBytes()+5+32);
            Log.d("EKd bytes",EKd_bytes.toString());
            byte[] encryptedMessage = Arrays.copyOfRange(commandApdu, 5+32+PKs.getLengthInBytes(),commandApdu.length);

            Log.d("encrypted Message",Utils.ByteArrayToHexString(encryptedMessage));

            PKd = PKG_Setup.pairing.getG1().newElementFromHash(PKd_Hash_Bytes,0,PKd_Hash_Bytes.length).getImmutable();
            EKd = PKG_Setup.pairing.getG1().newElementFromBytes(EKd_bytes).getImmutable();

            taskAdapter.updateTask(2,true);

            Log.d("HOST APDU SERVICE 5 PKd : ",PKd.toString());
            Log.d("HOST APDU SERVICE 6 EKd : ",EKd.toString());

            // To compute SSs = x.EKd || e(x.Ppub + SKs , EKd + H(IDd))

            // Step - 1
            Element x_EKd = EKd.duplicate().mulZn(x);

            Element x_Ppub_plus_SKs = PKG_Setup.Ppub.duplicate().mulZn(x).add(SKs);
            Element EKd_plus_H_IDd = EKd.duplicate().add(PKd);
            Element SSs_PairingResult = PKG_Setup.pairing.pairing(x_Ppub_plus_SKs, EKd_plus_H_IDd);

            // Step - 2 : Converting the result into byte arrays
            byte[] x_EKd_bytes = x_EKd.toBytes();
            byte[] SSs_PairingResult_bytes = SSs_PairingResult.toBytes();

            // Step - 3 : Concatenating
            byte[] SSs = new byte[x_EKd_bytes.length + SSs_PairingResult_bytes.length];
            System.arraycopy(x_EKd_bytes, 0, SSs, 0, x_EKd_bytes.length);
            System.arraycopy(SSs_PairingResult_bytes, 0, SSs, x_EKd_bytes.length, SSs_PairingResult_bytes.length);

            HTK = generateHTK(SSs,PKG_Setup.Ppub.toBytes());

            taskAdapter.updateTask(3,true);

            Log.d("HTK GENERATED",Arrays.toString(HTK));

            // Decrypting the received encrypted message and modifying it and again encrypting it to send it back
            String testMessage;
            try {
                Log.d("HOST APDU SERVICE 7 ","Going for decryption");
                testMessage = Utils.decrypt(encryptedMessage, HTK);
                testMessage = testMessage+"SUCCESS";
                encryptedMessage = Utils.encrypt(testMessage, HTK);
                Log.d("HOST APDU SERVICE 8 Decryption completed",testMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Loading ResponseAPDU with encrypted message for authentication
            responseAPDU = Utils.concatArrays(Utils.SELECT_OK_SW,encryptedMessage);

            taskAdapter.updateTask(4,true);
            taskAdapter.updateTask(5,true);

            return responseAPDU;
        }else if (commandApdu[0] == (byte) 0x70 && commandApdu[1] == (byte) 0x02){

            try {
                responseAPDU = Utils.encrypt(deviceName,HTK);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Log.d("Final Log from HCE","device name sent");

            switchActivityWithDelay();
            return responseAPDU;
        }
        return new byte[0];
    }

    private void switchActivityWithDelay() {
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MyHostApduService.this, SignerPage2.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }, 500); // Delay for 500ms (or adjust the delay as needed)
    }

    @Override
    public void onDeactivated(int reason) {
//        Intent intent = new Intent(this, SignerPage2.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
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


}
