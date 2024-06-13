package com.example.bluetoothconnection.communication.Entities;

import android.os.Build;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DeviceUsedInProcessingDetails {
    private int heightNeededToBeProcessed;

    private final int heightOfImagePart;

    private final int linePositionOfImagePart;
    private LocalDateTime lastHeartbeatReceivedTimestamp;

    private List<ImagePartInterval> processedImagePartsInterval = new ArrayList<>();

    public DeviceUsedInProcessingDetails(int heightOfImagePart, int linePositionOfImagePart) {
        this.heightOfImagePart = heightOfImagePart;
        this.linePositionOfImagePart = linePositionOfImagePart;
        this.heightNeededToBeProcessed = heightOfImagePart;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lastHeartbeatReceivedTimestamp = LocalDateTime.now();
        }
    }

    public int getHeightNeededToBeProcessed() {
        return heightNeededToBeProcessed;
    }

    public List<ImagePartInterval> getProcessedImagePartsInterval() {
        return processedImagePartsInterval;
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
