package com.example.bluetoothconnection.communication.Entities;

public class ImagePartInterval {
    private int start;
    private int end;

    public ImagePartInterval(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
