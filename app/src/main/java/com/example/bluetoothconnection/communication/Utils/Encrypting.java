package com.example.bluetoothconnection.communication.Utils;

import static com.example.bluetoothconnection.utils.Common.getBytesBase64FromString;
import static com.example.bluetoothconnection.utils.Common.getStringBase64FromBytes;


import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;

public class Encrypting {
    public static final String SECRET_AUTHENTICATION_TOKEN = "AuthenticationToken";
    private static final String COMMON_TEXT = "IAmCommonText";
    private static final byte[] FIXED_SALT = "MyFixedSalt".getBytes(); // Use a consistent salt

    public static String getEncryptedAuthenticationToken() throws Exception {
        byte[] encryptedAuthenticationTokenAsBytes = encryptWithCommonKey(SECRET_AUTHENTICATION_TOKEN.getBytes());
        return getStringBase64FromBytes(encryptedAuthenticationTokenAsBytes);
    }

    public static boolean checkAuthenticationToken(String authenticationToken) throws Exception {
        byte[] decryptedAuthenticationTokenBytes = decryptWithCommonKey(getBytesBase64FromString(authenticationToken));
        String decryptedAuthenticationTokenString = new String(decryptedAuthenticationTokenBytes);
        return SECRET_AUTHENTICATION_TOKEN.equals(decryptedAuthenticationTokenString);
    }

    public static SecretKey getCommonSecretKey() throws Exception {
        int iterationCount = 10000; // Adjust this based on your security requirements
        int keyLength = 256; // Key length in bits

        KeySpec keySpec = new PBEKeySpec(COMMON_TEXT.toCharArray(), FIXED_SALT, iterationCount, keyLength);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return keyFactory.generateSecret(keySpec);
    }

    public static byte[] encryptWithCommonKey(byte[] content) throws Exception {
        SecretKey secretKey = getCommonSecretKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedBytes = cipher.doFinal(content);
        byte[] iv = cipher.getIV();

        // Combine IV and encrypted text
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

        return combined;
    }

    public static byte[] decryptWithCommonKey(byte[] content) throws Exception {
        SecretKey secretKey = getCommonSecretKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // Extract IV and encrypted text from combined byte array
        byte[] iv = new byte[12];
        byte[] encryptedBytes = new byte[content.length - 12];
        System.arraycopy(content, 0, iv, 0, 12);
        System.arraycopy(content, 12, encryptedBytes, 0, encryptedBytes.length);

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

        return cipher.doFinal(encryptedBytes);
    }

    public static KeyPair generateRSAKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] encryptRSAWithPublicKey(byte[] content, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(content);
    }

    // Decrypt with Private Key
    public static byte[] decryptRSAWithPrivateKey(byte[] encryptedBytes, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return decryptedBytes;
    }

    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256); // Use a suitable key size
        return keyGenerator.generateKey();
    }

    public static byte[] encryptWithAES(byte[] content, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(content);
    }

    public static byte[] decryptWithAES(byte[] encryptedData, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedData);
        return decryptedBytes;
    }
}
