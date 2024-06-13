package com.example.bluetoothconnection.communication.Entities;

import android.os.Build;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeviceNode implements Serializable {
    private DeviceInitialInfo deviceInitialInfo;
    private Map<String,DeviceNode> neighbours;
    private double totalWeight;

    private double personalWeight;
    private String uniqueName = getHardwareID();

    public DeviceNode() {
        this.neighbours = new HashMap<>();
    }
    public DeviceInitialInfo getDeviceInitialInfo() {
        return deviceInitialInfo;
    }
    public Map<String,DeviceNode> getNeighbours() {
        return neighbours;
    }

    public void setDeviceInitialInfo(DeviceInitialInfo deviceInitialInfo) {
        this.deviceInitialInfo = deviceInitialInfo;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public DeviceNode createNodeCopyWithoutNeighbours() {
        DeviceNode newDeviceNode = new DeviceNode();
        newDeviceNode.setDeviceInitialInfo(this.deviceInitialInfo);
        newDeviceNode.setUniqueName(uniqueName);

        return newDeviceNode;
    }
    public static String getHardwareID() {
        String devIDShort = "35" +
                Build.BOARD.length() % 10 +
                Build.BRAND.length() % 10 +
                Build.CPU_ABI.length() % 10 +
                Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 +
                Build.HOST.length() % 10 +
                Build.ID.length() % 10 +
                Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 +
                Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 +
                Build.TYPE.length() % 10 +
                Build.USER.length() % 10; // 13 digits
        return devIDShort;
    }
    public double getTotalWeight() {
        return totalWeight;
    }
    public void setTotalWeight(double totalWeight) {
        this.totalWeight = totalWeight;
    }
    public double getPersonalWeight() {
        return personalWeight;
    }

    public void setPersonalWeight(double personalWeight) {
        this.personalWeight = personalWeight;
    }
}
