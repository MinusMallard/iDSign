package com.example.idsign.Utilities;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Utils {

    // TAG Name
    public static final String TAG = "iDSign";

    // AID for this application.
    public static final String OUR_APPLICATION_AID = "AAAAAAAA0000";

    // Select APDU that contains first 4 mandatory bytes and then the application aid
    public static final byte[] SELECT_APDU = {
            (byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x06,
            (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0x00, (byte) 0x00
    };

    // "OK" status word sent in response to SELECT AID command (0x9000)
    public static final byte[] SELECT_OK_SW = { (byte) 0x90, (byte) 0x00 };

    // Reader will be sending the SIGNEE HELLO , here i am going to assign the 4th byte in the reader class
    public static final byte[] SEND_SIGNEE_HELLO = HexStringToByteArray("800100");

    public static final byte[] BLUETOOTH_REQUEST = HexStringToByteArray("7002000000");

    public static String ByteArrayToHexString(byte[] bytes) {
        char[] hexArray = {
                '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] HexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static byte[] concatArrays(byte[] first, byte[]... rest) {
        int totalLength = first.length;
        for (byte[] array : rest) {
            totalLength += array.length;
        }
        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    public static byte[] sha256(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data.getBytes());
    }

}
