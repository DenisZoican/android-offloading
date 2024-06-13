package com.example.bluetoothconnection.communication.Entities;

import org.opencv.core.Mat;

public class ImageComparison {
    private Mat image;
    private Mat histogram;
    private int count;

    public ImageComparison(Mat image, Mat histogram, int count) {
        this.image = image;
        this.histogram = histogram;
        this.count = count;
    }

    public Mat getHistogram() {
        return histogram;
    }

    public Mat getImage() {
        return image;
    }

    public void incrementCount() {
        count++;
    }

    public int getCount() {
        return count;
    }
}
