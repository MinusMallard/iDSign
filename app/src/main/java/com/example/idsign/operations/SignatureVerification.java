package com.example.idsign.operations;

import com.example.idsign.Utilities.PKG_Setup;
import com.example.idsign.Utilities.Utils;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import it.unisa.dia.gas.jpbc.Element;

public class SignatureVerification {

    public boolean verifyMessage(String pathToSignedFile, String identity, byte[] signature) throws NoSuchAlgorithmException {
        byte[] messageHash = Utils.calculateHash(pathToSignedFile);
        Element PKs = PKG_Setup.getPublicKey(identity);
        byte[] uBytes = Arrays.copyOfRange(signature, 0, PKG_Setup.P.getLengthInBytes());
        byte[] vBytes = Arrays.copyOfRange(signature, PKG_Setup.P.getLengthInBytes(), PKG_Setup.P.getLengthInBytes() + PKG_Setup.pairing.getZr().newRandomElement().getLengthInBytes());
        byte[] identityHash = Arrays.copyOfRange(signature, PKG_Setup.P.getLengthInBytes() + PKG_Setup.pairing.getZr().newRandomElement().getLengthInBytes(), signature.length);
        System.out.println("Identity hash in verify : " + Arrays.toString(identityHash));
        Element u = PKG_Setup.pairing.getG1().newElementFromBytes(uBytes).getImmutable();
        Element v = PKG_Setup.pairing.getZr().newElementFromBytes(vBytes).getImmutable();
        Element r_prime = PKG_Setup.pairing.pairing(u.duplicate(), PKG_Setup.P.duplicate()).mul(PKG_Setup.pairing.pairing(PKs.duplicate(), PKG_Setup.Ppub.duplicate().negate()).powZn(v.duplicate())).getImmutable();
        Element v_prime = PKG_Setup.pairing.getZr().newElementFromHash(messageHash, 0, messageHash.length).mul(r_prime.duplicate().toBigInteger()).getImmutable();
        return v_prime.isEqual(v);
    }

}
