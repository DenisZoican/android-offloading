package com.example.bluetoothconnection.communication.Utils;

import static com.example.bluetoothconnection.communication.Utils.Encrypting.decryptWithAES;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.decryptWithCommonKey;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.decryptRSAWithPrivateKey;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.encryptRSAWithPublicKey;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.encryptWithAES;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.encryptWithCommonKey;

import com.example.bluetoothconnection.AppConfig;
import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;
import com.example.bluetoothconnection.communication.Entities.DeviceNode;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadDeviceInitialInfoData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadMatData;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;

import static com.example.bluetoothconnection.communication.Utils.Hashing.calculateHash;
import static com.example.bluetoothconnection.utils.Common.combineArrays;
import static com.example.bluetoothconnection.utils.Common.deserializeObject;
import static com.example.bluetoothconnection.utils.Common.serializeObject;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Common {
    public enum MessageContentType {
        InitialDeviceInfo, Image, Error, UndefinedType
    }
    public static final String SERVICE_ID = "com.example.nearbytest";
    public static final int ENCRYPTED_SECRET_KEY_LENGTH = 256;
    public static final int MESSAGE_CONTENT_TYPE_LENGTH = 4;
    public static final int HASH_LENGTH = 32; //bytes

    public static Payload createPayloadFromMat(Mat image, PublicKey publicKey, SecretKey secretKey) throws Exception {
        // Convert enum to byte array
        byte[] enumBytes = convertMessageContentTypeToByteArray(MessageContentType.Image.ordinal());

        // Convert image (Mat) to byte array
        byte[] imageBytes = convertMatToByteArray(image);

        // Combine enum and image bytes into a single byte array
        byte[] combinedBytes = new byte[enumBytes.length + imageBytes.length];

        System.arraycopy(enumBytes, 0, combinedBytes, 0, enumBytes.length);
        System.arraycopy(imageBytes, 0, combinedBytes, enumBytes.length, imageBytes.length);

        return createPayLoadWithBytes(combinedBytes, publicKey, secretKey);
    }
    /////////de sters
    public static Payload createPayloadFromDeviceInitialInfo(DeviceInitialInfo deviceInitialInfo) throws Exception {
        byte[] deviceInitialInfoBytes = serializeObject(deviceInitialInfo);

        return createPayloadWithEncryptedBytesUsingCommonKey(deviceInitialInfoBytes);
    }

    public static Payload createPayloadFromDeviceNode(DeviceNode deviceNode) throws Exception {
        byte[] deviceNodeBytes = serializeObject(deviceNode);

        return createPayloadWithEncryptedBytesUsingCommonKey(deviceNodeBytes);
    }

    public static byte[] extractPayloadBytesWithoutHash (byte[] payloadWithHash) throws Exception {
        byte[] receivedHash = new byte[HASH_LENGTH];
        System.arraycopy(payloadWithHash, payloadWithHash.length-HASH_LENGTH, receivedHash, 0, receivedHash.length);

        byte[] payloadWithoutHashBytes = new byte[payloadWithHash.length-HASH_LENGTH];
        System.arraycopy(payloadWithHash, 0, payloadWithoutHashBytes, 0, payloadWithHash.length-HASH_LENGTH);
        byte[] recalculateHash = calculateHash(payloadWithoutHashBytes);
        if(!Arrays.equals(receivedHash, recalculateHash)) {
            System.out.println("The received hash doesn't match");
            return null;
        }
        return payloadWithoutHashBytes;
    }

    public static PayloadData extractDataFromPayload(Payload payload, PrivateKey privateKey) throws Exception {
        // Extract byte array from the payload
        byte[] payloadBytes = payload.asBytes();
        byte[] decryptedContentBytes;
        byte[] toBeDecryptedBytes;
        if(Boolean.parseBoolean(AppConfig.getShouldCreateHash())) {
            toBeDecryptedBytes = extractPayloadBytesWithoutHash(payloadBytes);
            if(toBeDecryptedBytes == null) {
                return new PayloadData(MessageContentType.Error);
            }
        } else {
            toBeDecryptedBytes = getContentBytes(payloadBytes, privateKey);
        }
        decryptedContentBytes = getContentBytes(toBeDecryptedBytes, privateKey);
        int messageContentTypeValue = convertByteArrayToMessageContentType(decryptedContentBytes); /// Use just first 4 bytes from here
        MessageContentType messageContentType = MessageContentType.values()[messageContentTypeValue];

        byte[] messageBytes = new byte[decryptedContentBytes.length - MESSAGE_CONTENT_TYPE_LENGTH];
        System.arraycopy(decryptedContentBytes, MESSAGE_CONTENT_TYPE_LENGTH, messageBytes, 0, messageBytes.length);

        switch (messageContentType) {
            case Image:
                return extractMatPayloadData(messageBytes);
            default:
                return new PayloadData(MessageContentType.UndefinedType);
        }
    }

    public static PayloadData extractDeviceInitialInfoFromPayload(Payload payload) throws Exception {
        // Extract byte array from the payload
        byte[] payloadBytes = payload.asBytes();
        byte[] decryptedBytes = decryptWithCommonKey(payloadBytes);

        return extractDeviceInitialInfoFromBytes(decryptedBytes);
    }

    private static  byte[] getContentBytes(byte[] payloadBytes, PrivateKey privateKey) throws Exception {
        return Boolean.parseBoolean(AppConfig.getShouldEncryptData())
                ? getDecryptedContentBytes(payloadBytes, privateKey)
                : payloadBytes;
    }

    private static byte[] getDecryptedContentBytes(byte[] payloadBytes, PrivateKey privateKey) throws Exception {
        int totalBytesLength = payloadBytes.length;
        byte[] encryptedSecretKey = new byte[ENCRYPTED_SECRET_KEY_LENGTH];
        byte[] encryptedBytes = new byte[totalBytesLength - ENCRYPTED_SECRET_KEY_LENGTH];
        System.arraycopy(payloadBytes, 0, encryptedSecretKey, 0, ENCRYPTED_SECRET_KEY_LENGTH);
        System.arraycopy(payloadBytes, ENCRYPTED_SECRET_KEY_LENGTH, encryptedBytes, 0, totalBytesLength-ENCRYPTED_SECRET_KEY_LENGTH);

        byte[] decryptedSecretKey = decryptRSAWithPrivateKey(encryptedSecretKey, privateKey);
        return decryptWithAES(encryptedBytes, new SecretKeySpec(decryptedSecretKey, "AES"));
    }

    private static Payload createPayloadWithEncryptedBytesUsingCommonKey(byte[] bytes) throws Exception {
        ////////////add if for encrypting/not encrypting using AppConfig
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

    private static Payload createPayLoadWithBytes(byte[] bytes, PublicKey publicKey, SecretKey secretKey) throws Exception {
        Payload payloadWithContent = Boolean.parseBoolean(AppConfig.getShouldEncryptData())
                ? createPayloadWithEncryptedBytes(bytes, publicKey, secretKey)
                : createPayloadWithBytesWithoutEncryption(bytes);

        if(Boolean.parseBoolean(AppConfig.getShouldCreateHash())) {
            byte[] combinedPayloadsBytes = combineArrays(payloadWithContent.asBytes(), calculateHash(payloadWithContent.asBytes()));
            return Payload.fromBytes(combinedPayloadsBytes);
        }
        return payloadWithContent;
    }
    private static Payload createPayloadWithBytesWithoutEncryption(byte[] bytes){
        return Payload.fromBytes(bytes);
    }

    private static PayloadDeviceInitialInfoData extractDeviceInitialInfoFromBytes(byte[] byteArray){
        DeviceInitialInfo deviceInitialInfo = deserializeObject(byteArray, DeviceInitialInfo.class);

        return new PayloadDeviceInitialInfoData(deviceInitialInfo);
    }

    private static PayloadMatData extractMatPayloadData(byte[] byteArray){
        // Convert bytes back to enum and image
        Mat image = convertByteArrayToMat(byteArray);

        return new PayloadMatData(image);
    }

    private static byte[] convertMessageContentTypeToByteArray(int value) {
        return ByteBuffer.allocate(MESSAGE_CONTENT_TYPE_LENGTH).putInt(value).array();
    }
    public static byte[] convertMatToByteArray(Mat image) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", image, matOfByte); /////////// We can specify the extension. Now is empty
        return matOfByte.toArray();
    }
    private static int convertByteArrayToMessageContentType(byte[] bytes) {
        return ByteBuffer.wrap(bytes, 0, MESSAGE_CONTENT_TYPE_LENGTH).getInt();
    }
    public static Mat convertByteArrayToMat(byte[] bytes){
        MatOfByte matOfByte = new MatOfByte(bytes);
        return Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_UNCHANGED);
    }

}
