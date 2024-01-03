package com.example.bluetoothconnection.communication;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

public class Common {
    public static final Strategy STRATEGY = Strategy.P2P_STAR;
    public static final String SERVICE_ID = "com.example.nearbytest";
    public static Payload convertMatToPayload(Mat image){
        // Convert the Mat to a byte array
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode("jpg", image, matOfByte); /////////// We can specify the extension. Now is empty
        byte[] byteArray = matOfByte.toArray();
        return Payload.fromBytes(byteArray);
    }

    public static Mat convertPayloadToMat(Payload payload, int imageHeight, int imageWidth){
        Mat reconstructedImage = new Mat(new Size(imageWidth, imageHeight), CvType.CV_8UC3);
        reconstructedImage.put(0, 0, payload.asBytes());

        return reconstructedImage;
    }
}
