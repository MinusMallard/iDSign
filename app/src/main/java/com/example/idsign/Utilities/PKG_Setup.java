package com.example.idsign.Utilities;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

public class PKG_Setup {

    public static Pairing pairing;
    private static Element s;
    public static Element Ppub, P;

    public static void setup(Context context) throws IOException {
        Log.d("Inside Setup 1 ","Lets Begin");

        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open("com/example/idsign/params/a.properties");

        File tempFile = File.createTempFile("jpbc_params", ".properties");
        FileUtils.copyInputStreamToFile(inputStream, tempFile);

        PairingParameters params = PairingFactory.getInstance().loadParameters(tempFile.getAbsolutePath());

        pairing = PairingFactory.getPairing(params);
        tempFile.delete();

        Log.d("Inside Setup 2 ","Lets Begin");
        checkSymmetric(pairing);
        Log.d("Check","Mate");

        // Generator = P
        byte[] P_bytes = {-95,-104,116,74,-104,-66,82,-89,-100,-49,83,-42,-41,72,17,123,-68,40,-53,81,-123,95,-65,-95,80,127,-100,124,-99,-7,126,5,-94,-44,-55,30,-81,102,7,109,98,-108,100,-63,95,-110,76,-2,9,63,-91,-89,86,123,-59,-97,11,109,118,79,59,20,-19,15,17,-73,-51,-21,43,112,-92,31,-8,19,-127,29,120,111,123,-38,83,-1,-12,11,-65,-28,21,100,-72,74,-28,-78,45,-103,26,0,100,-44,-75,-101,113,-13,90,-44,-88,-8,-49,13,101,107,17,8,101,38,29,37,-96,-14,-86,66,90,-49,-16,5,87,26,-75,-104};
        P = pairing.getG1().newElementFromBytes(P_bytes).getImmutable();

        Log.d("Inside Setup 3 P :",P.toString());

        // Master Secret Key = s
        byte[] s_bytes = {76,16,124,60,96,-127,-38,32,73,-55,72,84,12,-76,10,3,-14,-61,-64,77};
        s = pairing.getZr().newElementFromBytes(s_bytes).getImmutable();

        Log.d("Inside Setup 4 s :",s.toString());

        // Master Public Key = Ppub
        Ppub = P.duplicate().mulZn(s).getImmutable();

        Log.d("Inside Setup 5 Ppub :",Ppub.toString());
    }

    public static Element getPublicKey(String emailId) throws NoSuchAlgorithmException {
        byte[] hashedID = sha256(emailId);
        Element publicKey = pairing.getG1().newElementFromHash(hashedID, 0, hashedID.length).getImmutable();
        return publicKey;
    }

    public static Element getPrivateKey(Element publicKey){
        Element privateKey = publicKey.duplicate().mulZn(s).getImmutable();
        return privateKey;
    }

    private static byte[] sha256(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data.getBytes());
    }

    private static void checkSymmetric(Pairing pairing){
        if(pairing.isSymmetric()){
            Log.i("Check Symmetry","Nice Work It is Symmetric");
        }else {
            throw new RuntimeException("Pairing is not Symmetric");
        }
    }
}
