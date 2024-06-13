package com.example.bluetoothconnection.communication.Entities;

import org.opencv.core.Mat;

public class ImageDetails {
    private Mat image;
    private int imageIdentifier;

    public Mat getImage() {
        return image;
    }

    public int getImageIdentifier() {
        return imageIdentifier;
    }

    public ImageDetails(Mat image, int imageIdentifier) {
        this.image = image;
        this.imageIdentifier = imageIdentifier;
    }
}
