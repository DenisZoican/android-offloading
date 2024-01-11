package com.example.bluetoothconnection.communication.Utils.PayloadDataEntities;

import org.opencv.core.Mat;
import static com.example.bluetoothconnection.communication.Utils.Common.MessageContentType;

public class PayloadMatData extends PayloadData{
    private final Mat image;

    public PayloadMatData(Mat image) {
        super(MessageContentType.Image);
        this.image = image;
    }

    public Mat getImage() {
        return image;
    }
}