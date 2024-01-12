package com.example.bluetoothconnection.communication.Utils;

import static com.example.bluetoothconnection.communication.Utils.Encrypting.decryptWithAES;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.decryptWithCommonKey;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.decryptRSAWithPrivateKey;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.encryptRSAWithPublicKey;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.encryptWithAES;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.encryptWithCommonKey;

import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadDeviceInitialInfoData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadMatData;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;

import static com.example.bluetoothconnection.utils.Common.combineArrays;
import static com.example.bluetoothconnection.utils.Common.deserializeObject;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Common {
    public enum MessageContentType {
        Image,
        InitialDeviceInfo
        // ... add more enum values as needed
    }
    public static final Strategy STRATEGY = Strategy.P2P_STAR; //// Use P2P_CLUSTER because in START the central one is the advertiser.
    public static final String SERVICE_ID = "com.example.nearbytest";

    //o sa le fac un enum
    public static final String PAYLOAD_TYPE_IMAGE = "image";
    public static final String PAYLOAD_TYPE_STRING= "message";
    public static final int ENCRYPTED_SECRET_KEY_LENGTH = 256;
    ////////// Delete the following 2
    public static Payload convertMatToPayload(Mat image){
        // Convert the Mat to a byte array
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", image, matOfByte); /////////// We can specify the extension. Now is empty
        byte[] byteArray = matOfByte.toArray();
        return Payload.fromBytes(byteArray);
    }

    public static Mat convertPayloadToMat(Payload payload){
        MatOfByte matOfByte = new MatOfByte(payload.asBytes());
        return Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_UNCHANGED);
    }

    public static Payload createPayloadFromMat(Mat image, PublicKey publicKey, SecretKey secretKey) throws Exception {
        // Convert enum to byte array
        byte[] enumBytes = convertIntToByteArray(MessageContentType.Image.ordinal());

        // Convert image (Mat) to byte array
        byte[] imageBytes = convertMatToByteArray(image);

        // Combine enum and image bytes into a single byte array
        byte[] combinedBytes = new byte[enumBytes.length + imageBytes.length];

        System.arraycopy(enumBytes, 0, combinedBytes, 0, enumBytes.length);
        System.arraycopy(imageBytes, 0, combinedBytes, enumBytes.length, imageBytes.length);

        return createPayloadWithEncryptedBytes(combinedBytes, publicKey, secretKey);
    }

    public static Payload createPayloadFromDeviceInitialInfo(DeviceInitialInfo deviceInitialInfo) throws Exception {
        // Convert enum to byte array
        byte[] enumBytes = convertIntToByteArray(MessageContentType.InitialDeviceInfo.ordinal());

        // Convert image (Mat) to byte array
        byte[] classBytes = serializeObject(deviceInitialInfo);

        // Combine enum and image bytes into a single byte array
        byte[] combinedBytes = new byte[enumBytes.length + classBytes.length];

        System.arraycopy(enumBytes, 0, combinedBytes, 0, enumBytes.length);
        System.arraycopy(classBytes, 0, combinedBytes, enumBytes.length, classBytes.length);

        return createPayloadWithEncryptedBytes(combinedBytes);
    }

    public static PayloadData extractPayloadData(Payload payload, PrivateKey privateKey) throws Exception {
        // Extract byte array from the payload
        byte[] combinedBytes = payload.asBytes();

        //// Get SECRET KEY and IMAGE
        int totalBytesLength = combinedBytes.length;
        byte[] encryptedSecretKey = new byte[ENCRYPTED_SECRET_KEY_LENGTH];
        byte[] encryptedBytes = new byte[totalBytesLength - ENCRYPTED_SECRET_KEY_LENGTH];
        System.arraycopy(combinedBytes, 0, encryptedSecretKey, 0, ENCRYPTED_SECRET_KEY_LENGTH);
        System.arraycopy(combinedBytes, ENCRYPTED_SECRET_KEY_LENGTH, encryptedBytes, 0, totalBytesLength-ENCRYPTED_SECRET_KEY_LENGTH);

        byte[] decryptedSecretKey = decryptRSAWithPrivateKey(encryptedSecretKey, privateKey);

        // Decrypt the original data using the decrypted AES secret key
        byte[] decryptedBytes = decryptWithAES(encryptedBytes, new SecretKeySpec(decryptedSecretKey, "AES"));

        //////////

        int enumValue = convertByteArrayToInt(decryptedBytes); /// Use just first 4 bytes from here
        MessageContentType messageContentType = MessageContentType.values()[enumValue];

        byte[] contentBytes = new byte[decryptedBytes.length - 4];
        System.arraycopy(decryptedBytes, 4, contentBytes, 0, contentBytes.length);

        switch (messageContentType) {
            case Image:
                return extractMatPayloadData(contentBytes);
            default:
                return new PayloadData(MessageContentType.InitialDeviceInfo);
        }
    }

    public static PayloadData extractPayloadData(Payload payload) throws Exception {
        // Extract byte array from the payload
        byte[] combinedBytes = payload.asBytes();
        byte[] decryptedBytes = decryptWithCommonKey(combinedBytes);

        byte[] contentBytes = new byte[decryptedBytes.length - 4];
        System.arraycopy(decryptedBytes, 4, contentBytes, 0, contentBytes.length);

        return extractInitialDeviceInfoData(contentBytes);
    }

    private static byte[] serializeObject(Serializable object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(object);
            return bos.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Payload createPayloadWithEncryptedBytes(byte[] bytes) throws Exception {
        byte[] encryptedBytes = encryptWithCommonKey(bytes);
        // Create a payload from the combined byte array
        return Payload.fromBytes(encryptedBytes);
    }

    private static Payload createPayloadWithEncryptedBytes(byte[] bytes, PublicKey publicKeyUsedForAESSecretKey, SecretKey AESSecretKey) throws Exception {
        // Encrypt data with AES using the secret key
        byte[] encryptedBytes = encryptWithAES(bytes, AESSecretKey);

        // Encrypt the secret key with the recipient's public key (RSA)
        byte[] encryptedSecretKey = encryptRSAWithPublicKey(AESSecretKey.getEncoded(), publicKeyUsedForAESSecretKey);

        // Combine encrypted bytes and encrypted secret key into a single byte array
        byte[] combinedData = combineArrays(encryptedSecretKey, encryptedBytes);

        // Create a payload from the combined byte array
        return Payload.fromBytes(combinedData);
    }

    private static PayloadDeviceInitialInfoData extractInitialDeviceInfoData(byte[] byteArray){
        DeviceInitialInfo deviceInitialInfo = deserializeObject(byteArray, DeviceInitialInfo.class);

        return new PayloadDeviceInitialInfoData(deviceInitialInfo);
    }

    private static PayloadMatData extractMatPayloadData(byte[] byteArray){
        // Convert bytes back to enum and image
        Mat image = convertByteArrayToMat(byteArray);

        return new PayloadMatData(image);
    }

    private static byte[] convertIntToByteArray(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }
    private static byte[] convertMatToByteArray(Mat image) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", image, matOfByte); /////////// We can specify the extension. Now is empty
        return matOfByte.toArray();
    }
    private static int convertByteArrayToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes, 0, 4).getInt();
    }
    private static Mat convertByteArrayToMat(byte[] bytes){
        MatOfByte matOfByte = new MatOfByte(bytes);
        return Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_UNCHANGED);
    }

}
