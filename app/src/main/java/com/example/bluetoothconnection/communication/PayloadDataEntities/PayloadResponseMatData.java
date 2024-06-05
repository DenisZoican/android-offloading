package com.example.bluetoothconnection.communication.PayloadDataEntities;

import com.example.bluetoothconnection.communication.Utils.Common;

import org.opencv.core.Mat;

public class PayloadResponseMatData extends PayloadData{

    private final Mat image;
    private final String processorNodeUniqueName;

    private final int linePosition;

    public PayloadResponseMatData(Mat image, String processorNodeUniqueName, int linePosition) {
        super(Common.MessageContentType.ResponseImage);
        this.image = image;
        this.processorNodeUniqueName = processorNodeUniqueName;
        this.linePosition = linePosition;
    }

    public Mat getImage() {
        return image;
    }

    public String getProcessorNodeUniqueName() {
        return processorNodeUniqueName;
    }

    public int getLinePosition() {
        return linePosition;
    }
}
