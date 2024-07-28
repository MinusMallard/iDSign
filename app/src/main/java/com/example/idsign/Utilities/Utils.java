package com.example.idsign.Utilities;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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

    // Decrypt a byte array using AES and HTK
    public static String decrypt(byte[] encryptedData, byte[] HTK) throws Exception {
        SecretKey secretKey = new SecretKeySpec(HTK, 0, 16, "AES"); // Use first 16 bytes for AES-128
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedData = cipher.doFinal(encryptedData);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    // Encrypt a string using AES and HTK
    public static byte[] encrypt(String data, byte[] HTK) throws Exception {
        SecretKey secretKey = new SecretKeySpec(HTK, 0, 16, "AES"); // Use first 16 bytes for AES-128, to use AES256 just replace the value 16 with 32
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    // Method to Encrypt Byte Array
    public static byte[] encryptByteArray(byte[] data, byte[] HTK) throws Exception {
        SecretKey secretKey = new SecretKeySpec(HTK, "AES"); // No provider specified
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // No provider specified
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    // Method to Decrypt Byte Array
    public static byte[] decryptByteArray(byte[] encryptedData, byte[] HTK) throws Exception {
        SecretKey secretKey = new SecretKeySpec(HTK, "AES"); // No provider specified
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // No provider specified
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(encryptedData);
    }

    // Method to return Absolute path of the document from URI
    public static String getPathFromUri(Context context, Uri uri) {
        String path = null;

        // Check if the URI is a content URI
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // Handle document URIs
                String documentId = DocumentsContract.getDocumentId(uri);
                if (documentId.startsWith("raw:")) {
                    path = documentId.replaceFirst("raw:", "");
                } else {
                    String[] split = documentId.split(":");
                    String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        // Primary storage
                        path = Environment.getExternalStorageDirectory() + "/" + split[1];
                    } else {
                        // Handle other types if necessary
                        path = getFilePathFromContentUri(context, uri);
                    }
                }
            } else {
                // For other content URIs
                path = getFilePathFromContentUri(context, uri);
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            // Direct file URI
            path = uri.getPath();
        }
        return path;
    }

    private static String getFilePathFromContentUri(Context context, Uri uri) {
        String[] projection = {MediaStore.Files.FileColumns.DATA};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            try {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(columnIndex);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

//    public static String readPDFFileAsHexString(String filePath) {
//        File file = new File(filePath);
//        StringBuilder hexString = new StringBuilder();
//
//        try (FileInputStream fis = new FileInputStream(file)) {
//            byte[] buffer = new byte[1024];
//            int bytesRead;
//
//            while ((bytesRead = fis.read(buffer)) != -1) {
//                for (int i = 0; i < bytesRead; i++) {
//                    hexString.append(String.format("%02x", buffer[i]));
//                }
//            }
//        } catch (IOException e) {
//            System.err.println("Error reading file: " + e.getMessage());
//            return null;
//        }
//
//        Log.d("Inside UTILS","Doc in String with Length: "+hexString.toString().length()+" and data: "+hexString.toString());
//
//        return hexString.toString();
//    }

    public static byte[] calculateHash(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            return digest.digest();
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
