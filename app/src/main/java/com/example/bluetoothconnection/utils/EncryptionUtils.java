package com.example.bluetoothconnection.utils;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.AlgorithmParameters;
import java.security.KeyStore;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;

public class EncryptionUtils {
    public static final String SECRET_AUTHENTICATION_TOKEN = "AuthenticationToken";
    private static final String COMMON_TEXT = "IAmCommonText";
    private static final byte[] FIXED_SALT = "MyFixedSalt".getBytes(); // Use a consistent salt

    public static SecretKey getSecretKey() throws Exception {
        int iterationCount = 10000; // Adjust this based on your security requirements
        int keyLength = 256; // Key length in bits

        KeySpec keySpec = new PBEKeySpec(COMMON_TEXT.toCharArray(), FIXED_SALT, iterationCount, keyLength);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return keyFactory.generateSecret(keySpec);
    }

    public static String encrypt(String plainText) throws Exception {
        SecretKey secretKey = getSecretKey();
        System.out.println("Secret key ss "+secretKey);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        byte[] iv = cipher.getIV();

        // Combine IV and encrypted text
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

        return Base64.encodeToString(combined, Base64.DEFAULT);
    }

    public static String decrypt(String encryptedText) throws Exception {
        SecretKey secretKey = getSecretKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // Extract IV and encrypted text from combined byte array
        byte[] combined = Base64.decode(encryptedText, Base64.DEFAULT);
        byte[] iv = new byte[12];
        byte[] encryptedBytes = new byte[combined.length - 12];
        System.arraycopy(combined, 0, iv, 0, 12);
        System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.length);

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes);
    }
}
