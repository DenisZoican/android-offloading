package com.example.bluetoothconnection.communication.Utils;

import static com.example.bluetoothconnection.communication.Utils.Encrypting.decryptWithAES;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.decryptWithCommonKey;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.decryptRSAWithPrivateKey;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.encryptRSAWithPublicKey;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.encryptWithAES;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.encryptWithCommonKey;
import java.nio.charset.StandardCharsets;

import com.example.bluetoothconnection.AppConfig;
import com.example.bluetoothconnection.communication.Entities.DeviceNode;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadDeviceNodeData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadRequestMatData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadResponseMatData;
import com.google.android.gms.nearby.connection.Payload;
import com.google.gson.Gson;

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
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Common {
    private final static Gson gson = new Gson();

    public enum MessageContentType {
        DeviceNode, ResponseImage, RequestImage, Error, UndefinedType
    }
    public static final String SERVICE_ID = "com.example.nearbytest";
    public static final int ENCRYPTED_SECRET_KEY_LENGTH = 256;
    public static final int MESSAGE_CONTENT_TYPE_LENGTH = 4;
    public static final int HASH_LENGTH = 32; //bytes
    public static final int IMAGE_SIZE_BYTE_LENGTH = 4; //bytes
    public static final int PROCESSOR_NODE_UNIQUE_NAME_LENGTH = 15; //bytes
    public static final int DESTINATION_ENDPOINT_ID_BYTES_LENGTH = 4; //bytes
    public static final int LINE_POSITION_FOR_IMAGE_PART_LENGTH = 4; //bytes
    public static final short LIST_OF_VISITED_NODES_SIZE = 2; //bytes

    public static Payload createPayloadFromRequestMat(Mat image, int linePositionForImagePart, DeviceNode treeNode, PublicKey publicKey, SecretKey secretKey) throws Exception {
        // Convert enum to byte array
        byte[] enumBytes = convertMessageContentTypeToByteArray(MessageContentType.RequestImage.ordinal());

        // Convert line position to byte array
        byte[] linePositionForImagePartBytes =  ByteBuffer.allocate(LINE_POSITION_FOR_IMAGE_PART_LENGTH).putInt(linePositionForImagePart).array();

        // Convert image (Mat) to byte array
        byte[] imageBytes = convertMatToByteArray(image);

        // Convert image size to byte array
        int imageSize = imageBytes.length;
        byte[] imageSizeBytes =  ByteBuffer.allocate(IMAGE_SIZE_BYTE_LENGTH).putInt(imageSize).array();

        // Convert treeNode to byte array
        byte[] treeNodeBytes = serializeObject(treeNode);


        // Combine enum and image bytes into a single byte array
        byte[] combinedBytes = new byte[enumBytes.length + LINE_POSITION_FOR_IMAGE_PART_LENGTH + imageBytes.length + IMAGE_SIZE_BYTE_LENGTH + treeNodeBytes.length];

        System.arraycopy(enumBytes, 0, combinedBytes, 0, enumBytes.length);
        System.arraycopy(linePositionForImagePartBytes, 0, combinedBytes, enumBytes.length, LINE_POSITION_FOR_IMAGE_PART_LENGTH);
        System.arraycopy(imageSizeBytes, 0, combinedBytes, enumBytes.length + LINE_POSITION_FOR_IMAGE_PART_LENGTH, IMAGE_SIZE_BYTE_LENGTH);
        System.arraycopy(imageBytes, 0, combinedBytes, enumBytes.length + LINE_POSITION_FOR_IMAGE_PART_LENGTH + IMAGE_SIZE_BYTE_LENGTH, imageBytes.length);
        System.arraycopy(treeNodeBytes, 0, combinedBytes,enumBytes.length + LINE_POSITION_FOR_IMAGE_PART_LENGTH + IMAGE_SIZE_BYTE_LENGTH + imageBytes.length, treeNodeBytes.length);

        return createPayLoadWithBytes(combinedBytes, publicKey, secretKey);
    }

    public static Payload createPayloadFromResponseMat(Mat image, int linePositionForImagePart, String processorNodeUniqueName, PublicKey publicKey, SecretKey secretKey) throws Exception {
        // Convert enum to byte array
        byte[] enumBytes = convertMessageContentTypeToByteArray(MessageContentType.ResponseImage.ordinal());

        // Convert line position to byte array
        byte[] linePositionForImagePartBytes =  ByteBuffer.allocate(LINE_POSITION_FOR_IMAGE_PART_LENGTH).putInt(linePositionForImagePart).array();

        // Convert image (Mat) to byte array
        byte[] imageBytes = convertMatToByteArray(image);

        // Convert processorNodeUniqueName to byte array
        byte[] processorNodeUniqueNameBytes = processorNodeUniqueName.getBytes();

        // Combine enum and image bytes into a single byte array
        byte[] combinedBytes = new byte[enumBytes.length + LINE_POSITION_FOR_IMAGE_PART_LENGTH + imageBytes.length + processorNodeUniqueNameBytes.length];

        System.arraycopy(enumBytes, 0, combinedBytes, 0, enumBytes.length);
        System.arraycopy(linePositionForImagePartBytes, 0, combinedBytes, enumBytes.length, LINE_POSITION_FOR_IMAGE_PART_LENGTH);
        System.arraycopy(processorNodeUniqueNameBytes, 0, combinedBytes, enumBytes.length + LINE_POSITION_FOR_IMAGE_PART_LENGTH, PROCESSOR_NODE_UNIQUE_NAME_LENGTH);
        System.arraycopy(imageBytes, 0, combinedBytes, enumBytes.length + LINE_POSITION_FOR_IMAGE_PART_LENGTH + PROCESSOR_NODE_UNIQUE_NAME_LENGTH, imageBytes.length);

        return createPayLoadWithBytes(combinedBytes, publicKey, secretKey);
    }

    public static Payload createPayloadFromDeviceNode(DeviceNode deviceNode, String destinationEndpointId, Set<String> visitedNodesEndpointIds) throws Exception {
        String visitedNodesEndpointIdsString = gson.toJson(visitedNodesEndpointIds);
        byte[] visitedNodesEndpointIdsBytes = visitedNodesEndpointIdsString.getBytes();

        short visitedNodesEndpointIdsBytesLength = (short)visitedNodesEndpointIdsBytes.length;
        byte[] listOfVisitedNodesSizeBytes = ByteBuffer.allocate(LIST_OF_VISITED_NODES_SIZE).putShort(visitedNodesEndpointIdsBytesLength).array();

        // Convert DeviceNode to byte array
        byte[] deviceNodeBytes = serializeObject(deviceNode);
        byte[] destinationEndpointIdBytes = destinationEndpointId.getBytes();
        
        byte[] combinedBytes = new byte[deviceNodeBytes.length + DESTINATION_ENDPOINT_ID_BYTES_LENGTH + visitedNodesEndpointIdsBytes.length + LIST_OF_VISITED_NODES_SIZE];
        System.arraycopy(destinationEndpointIdBytes, 0, combinedBytes, 0, DESTINATION_ENDPOINT_ID_BYTES_LENGTH);
        System.arraycopy(listOfVisitedNodesSizeBytes, 0, combinedBytes, DESTINATION_ENDPOINT_ID_BYTES_LENGTH, LIST_OF_VISITED_NODES_SIZE);
        System.arraycopy(visitedNodesEndpointIdsBytes, 0, combinedBytes, DESTINATION_ENDPOINT_ID_BYTES_LENGTH + LIST_OF_VISITED_NODES_SIZE, visitedNodesEndpointIdsBytes.length);
        System.arraycopy(deviceNodeBytes, 0, combinedBytes, DESTINATION_ENDPOINT_ID_BYTES_LENGTH + LIST_OF_VISITED_NODES_SIZE + visitedNodesEndpointIdsBytes.length, deviceNodeBytes.length);

        return createPayloadWithEncryptedBytesUsingCommonKey(combinedBytes);
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
        byte[] payloadBytes = Arrays.copyOfRange(payload.asBytes(), 1, payload.asBytes().length);
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
            case RequestImage:
                return extractRequestMatPayloadData(messageBytes);
            case ResponseImage:
                return extractResponseMatPayloadData(messageBytes);
            default:
                return new PayloadData(MessageContentType.UndefinedType);
        }
    }

    public static PayloadData extractDeviceNodeFromPayload(Payload payload) throws Exception {
        // Extract byte array from the payload
        byte[] payloadBytes = Arrays.copyOfRange(payload.asBytes(), 1, payload.asBytes().length);
        byte[] decryptedBytes = decryptWithCommonKey(payloadBytes);

        byte[] listOfVisitedNodesSizeBytes = new byte[LIST_OF_VISITED_NODES_SIZE];
        System.arraycopy(decryptedBytes, DESTINATION_ENDPOINT_ID_BYTES_LENGTH, listOfVisitedNodesSizeBytes, 0, LIST_OF_VISITED_NODES_SIZE);
        short listOfVisitedNodesLength = ByteBuffer.wrap(listOfVisitedNodesSizeBytes).getShort();

        byte[] destinationEndpointIdBytes =  new byte[DESTINATION_ENDPOINT_ID_BYTES_LENGTH];
        byte[] deviceNodeBytes = new byte[decryptedBytes.length - DESTINATION_ENDPOINT_ID_BYTES_LENGTH - LIST_OF_VISITED_NODES_SIZE - listOfVisitedNodesLength];
        byte[] visitedNodesEndpointIdsBytes = new byte[listOfVisitedNodesLength];

        System.arraycopy(decryptedBytes, 0, destinationEndpointIdBytes, 0, DESTINATION_ENDPOINT_ID_BYTES_LENGTH);
        System.arraycopy(decryptedBytes, DESTINATION_ENDPOINT_ID_BYTES_LENGTH + LIST_OF_VISITED_NODES_SIZE, visitedNodesEndpointIdsBytes ,0 , listOfVisitedNodesLength);
        System.arraycopy(decryptedBytes, DESTINATION_ENDPOINT_ID_BYTES_LENGTH + LIST_OF_VISITED_NODES_SIZE + listOfVisitedNodesLength, deviceNodeBytes ,0 , deviceNodeBytes.length);


        DeviceNode deviceNode = deserializeObject(deviceNodeBytes, DeviceNode.class);
        String destinationEndpointId = new String(destinationEndpointIdBytes, StandardCharsets.UTF_8);

        String jsonStringFromBytes = new String(visitedNodesEndpointIdsBytes, StandardCharsets.UTF_8);
        Set<String> visitedNodes = gson.fromJson(jsonStringFromBytes, Set.class);

        return new PayloadDeviceNodeData(deviceNode, destinationEndpointId, visitedNodes);
    }

    private static  byte[] getContentBytes(byte[] payloadBytes, PrivateKey privateKey) throws Exception {
        return Boolean.parseBoolean(AppConfig.getShouldEncryptData())
                ? getDecryptedContentBytes(payloadBytes, privateKey)
                : payloadBytes;
    }

    private static byte[] addPayloadTypeByte(boolean isDeviceNode, byte[] payloadContent) {

        byte[] finalBytes = new byte[payloadContent.length + 1];
        finalBytes[0] = (byte) (isDeviceNode ? 1:0);
        System.arraycopy(payloadContent,0, finalBytes, 1, payloadContent.length);

        return finalBytes;
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
        byte[] finalBytes = addPayloadTypeByte(true, encryptedBytes);
        return Payload.fromBytes(finalBytes);
    }

    private static byte[] getEncryptedBytes(byte[] bytes, PublicKey publicKeyUsedForAESSecretKey, SecretKey AESSecretKey) throws Exception {
        // Encrypt data with AES using the secret key
        byte[] encryptedBytes = encryptWithAES(bytes, AESSecretKey);

        // Encrypt the secret key with the recipient's public key (RSA)
        byte[] encryptedSecretKey = encryptRSAWithPublicKey(AESSecretKey.getEncoded(), publicKeyUsedForAESSecretKey);

        // Combine encrypted bytes and encrypted secret key into a single byte array
        return combineArrays(encryptedSecretKey, encryptedBytes);
    }

    private static Payload createPayLoadWithBytes(byte[] bytes, PublicKey publicKey, SecretKey secretKey) throws Exception {
        byte[] payloadContentBytes = Boolean.parseBoolean(AppConfig.getShouldEncryptData())
                ? getEncryptedBytes(bytes, publicKey, secretKey)
                : bytes;

        byte[] finalBytes = addPayloadTypeByte(false, payloadContentBytes);
        if(Boolean.parseBoolean(AppConfig.getShouldCreateHash())) {
            byte[] combinedPayloadsBytes = combineArrays(finalBytes, calculateHash(payloadContentBytes));
            return Payload.fromBytes(combinedPayloadsBytes);
        }
        return Payload.fromBytes(finalBytes);
    }

    private static PayloadRequestMatData extractRequestMatPayloadData(byte[] byteArray){
        byte[] linePositionBytes = new byte[LINE_POSITION_FOR_IMAGE_PART_LENGTH];
        System.arraycopy(byteArray,0, linePositionBytes, 0, LINE_POSITION_FOR_IMAGE_PART_LENGTH);
        int linePosition = ByteBuffer.wrap(linePositionBytes).getInt();

        byte[] imageLengthBytes = new byte[IMAGE_SIZE_BYTE_LENGTH];
        System.arraycopy(byteArray, LINE_POSITION_FOR_IMAGE_PART_LENGTH, imageLengthBytes, 0, IMAGE_SIZE_BYTE_LENGTH);
        int imageLength = ByteBuffer.wrap(imageLengthBytes).getInt();

        byte[] imageBytes = new byte[imageLength];
        System.arraycopy(byteArray , LINE_POSITION_FOR_IMAGE_PART_LENGTH + IMAGE_SIZE_BYTE_LENGTH, imageBytes, 0, imageLength);
        // Convert bytes back to image
        Mat image = convertByteArrayToMat(imageBytes);

        int treeNodeLength =  byteArray.length - IMAGE_SIZE_BYTE_LENGTH - imageLength - LINE_POSITION_FOR_IMAGE_PART_LENGTH;
        byte[] treeNodeBytes = new byte[treeNodeLength];
        System.arraycopy(byteArray, LINE_POSITION_FOR_IMAGE_PART_LENGTH + IMAGE_SIZE_BYTE_LENGTH + imageLength, treeNodeBytes, 0, treeNodeLength);
        DeviceNode treeNode = deserializeObject(treeNodeBytes, DeviceNode.class);

        return new PayloadRequestMatData(image, treeNode, linePosition);
    }

    private static PayloadResponseMatData extractResponseMatPayloadData(byte[] byteArray){
        byte[] linePositionLengthBytes = new byte[LINE_POSITION_FOR_IMAGE_PART_LENGTH];
        System.arraycopy(byteArray,0, linePositionLengthBytes, 0, LINE_POSITION_FOR_IMAGE_PART_LENGTH);
        int linePositionLength = ByteBuffer.wrap(linePositionLengthBytes).getInt();

        byte[] processorUniqueNameBytes = new byte[PROCESSOR_NODE_UNIQUE_NAME_LENGTH];
        System.arraycopy(byteArray, LINE_POSITION_FOR_IMAGE_PART_LENGTH, processorUniqueNameBytes, 0, PROCESSOR_NODE_UNIQUE_NAME_LENGTH);

        int imageBytesLength = byteArray.length - PROCESSOR_NODE_UNIQUE_NAME_LENGTH - LINE_POSITION_FOR_IMAGE_PART_LENGTH;
        byte[] imageBytes = new byte[imageBytesLength];
        System.arraycopy(byteArray , PROCESSOR_NODE_UNIQUE_NAME_LENGTH + LINE_POSITION_FOR_IMAGE_PART_LENGTH, imageBytes, 0, imageBytesLength);
        // Convert bytes back to image
        Mat image = convertByteArrayToMat(imageBytes);

        return new PayloadResponseMatData(image, new String(processorUniqueNameBytes, StandardCharsets.UTF_8), linePositionLength);
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
