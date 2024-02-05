package com.example.bluetoothconnection.communication.Entities;

public class CommunicationDetails {
    private int failedAttempts;
    private int imagePart;

    public CommunicationDetails(int imagePart) {
        this.imagePart = imagePart;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public int getImagePart() {
        return imagePart;
    }

    public void setImagePart(int imagePart) {
        this.imagePart = imagePart;
    }

    public void incrementFailedAttempts() {
        this.failedAttempts ++;
    }
}
