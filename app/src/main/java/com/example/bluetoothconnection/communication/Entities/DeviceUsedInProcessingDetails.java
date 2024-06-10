package com.example.bluetoothconnection.communication.Entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DeviceUsedInProcessingDetails {
    private int heightNeededToBeProcessed;

    private final int heightOfImagePart;

    private final int linePositionOfImagePart;
    private LocalDateTime lastHeartbeatReceivedTimestamp;

    private List<ImagePartInterval> imagePartIntervals = new ArrayList<>();

    public DeviceUsedInProcessingDetails(int heightOfImagePart, int linePositionOfImagePart) {
        this.heightOfImagePart = heightOfImagePart;
        this.linePositionOfImagePart = linePositionOfImagePart;
        this.heightNeededToBeProcessed = heightOfImagePart;
    }

    public int getHeightNeededToBeProcessed() {
        return heightNeededToBeProcessed;
    }

    public List<ImagePartInterval> getImagePartIntervals() {
        return imagePartIntervals;
    }

    public void setHeightNeededToBeProcessed(int heightNeededToBeProcessed) {
        this.heightNeededToBeProcessed = heightNeededToBeProcessed;
    }

    public int getHeightOfImagePart() {
        return heightOfImagePart;
    }

    public int getLinePositionOfImagePart() {
        return linePositionOfImagePart;
    }
    public void setLastHeartbeatReceivedTimestamp(LocalDateTime  lastHeartbeatReceivedTimestamp) {
        this.lastHeartbeatReceivedTimestamp = lastHeartbeatReceivedTimestamp;
    }

    public LocalDateTime getLastHeartbeatReceivedTimestamp() {
        return lastHeartbeatReceivedTimestamp;
    }
}
