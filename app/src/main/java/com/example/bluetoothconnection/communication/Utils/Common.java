package com.example.bluetoothconnection.communication.Utils;

import com.example.bluetoothconnection.communication.Utils.PayloadDataEntities.PayloadData;
import com.example.bluetoothconnection.communication.Utils.PayloadDataEntities.PayloadMatData;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.nio.ByteBuffer;

public class Common {
    public enum MessageContentType {
        Image,
        BatteryInformation
        // ... add more enum values as needed
    }
    public static final Strategy STRATEGY = Strategy.P2P_STAR; //// Use P2P_CLUSTER because in START the central one is the advertiser.
    public static final String SERVICE_ID = "com.example.nearbytest";

    //o sa le fac un enum
    public static final String PAYLOAD_TYPE_IMAGE = "image";
    public static final String PAYLOAD_TYPE_STRING= "message";
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



    ///////////////////// New
    public static Payload createPayloadFromMat(Mat image) {
        // Convert enum to byte array
        byte[] enumBytes = convertIntToByteArray(MessageContentType.Image.ordinal());

        // Convert image (Mat) to byte array
        byte[] imageBytes = convertMatToByteArray(image);

        // Combine enum and image bytes into a single byte array
        byte[] combinedBytes = new byte[enumBytes.length + imageBytes.length];

        System.arraycopy(enumBytes, 0, combinedBytes, 0, enumBytes.length);
        System.arraycopy(imageBytes, 0, combinedBytes, enumBytes.length, imageBytes.length);

        // Create a payload from the combined byte array
        return Payload.fromBytes(combinedBytes);
    }

    public static PayloadData extractDataFromPayload(Payload payload) {
        // Extract byte array from the payload
        byte[] combinedBytes = payload.asBytes();

        // Extract the enum as a 4-byte integer
        int enumValue = convertByteArrayToInt(combinedBytes);
        MessageContentType messageContentType = MessageContentType.values()[enumValue];

        switch (messageContentType){
            case Image:
                return extractMatPayloadData(combinedBytes);
            default:
                return new PayloadData(MessageContentType.BatteryInformation);

        }
    }

    private static PayloadMatData extractMatPayloadData(byte[] byteArray){
        // Extract image bytes
        byte[] imageBytes = new byte[byteArray.length - 4];
        System.arraycopy(byteArray, 4, imageBytes, 0, imageBytes.length);

        // Convert bytes back to enum and image
        Mat image = convertByteArrayToMat(imageBytes);

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
