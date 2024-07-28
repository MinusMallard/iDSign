package com.example.idsign.operations;

import android.util.Log;

import com.example.idsign.Utilities.PKG_Setup;
import com.example.idsign.Utilities.Utils;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import it.unisa.dia.gas.jpbc.Element;

public class SignatureCreation {

    String identity;
    byte[] messageHash;


    public SignatureCreation(byte[] messageHash, String identity){
        this.messageHash = messageHash;
        this.identity = identity;
    }

    public byte[] signMessage(Element SKs) throws NoSuchAlgorithmException {
        // Hash the message
//        byte[] messageHash = Utils.sha256(docData);
        Log.d("Signature Creation","Hash of the message generated");

        // Hash of the identity
        byte[] identityHash = Utils.sha256(identity);
        Log.d("Signature Creation","Hash of the identity generated");

        Element k = PKG_Setup.pairing.getZr().newRandomElement().getImmutable();
        Element P1 = PKG_Setup.pairing.getG1().newRandomElement().getImmutable();
        Element r = PKG_Setup.pairing.pairing(P1, PKG_Setup.P).powZn(k);
        Element v = PKG_Setup.pairing.getZr().newElementFromHash(messageHash,0,messageHash.length).mul(r.toBigInteger()).getImmutable();
        Element u = SKs.duplicate().mulZn(v).add(P1.duplicate().mulZn(k));

        ByteBuffer signature = ByteBuffer.allocate(u.getLengthInBytes() + v.getLengthInBytes() + identityHash.length);
        signature.put(u.toBytes());
        signature.put(v.toBytes());
        signature.put(identityHash);

        return signature.array();
    }
}
