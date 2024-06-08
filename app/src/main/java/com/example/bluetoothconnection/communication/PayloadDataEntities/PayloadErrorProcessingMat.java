package com.example.bluetoothconnection.communication.PayloadDataEntities;

import com.example.bluetoothconnection.communication.Utils.Common;

import org.opencv.core.Mat;

public class PayloadErrorProcessingMat extends PayloadData{
    private int linePosition;
    private Mat image;
    public PayloadErrorProcessingMat(Mat image, int linePosition) {
        super(Common.MessageContentType.ErrorProcessingImage);
        this.image = image;
        this.linePosition = linePosition;
    }

    public int getLinePosition() {
        return linePosition;
    }

    public Mat getImage() {
        return image;
    }
}
