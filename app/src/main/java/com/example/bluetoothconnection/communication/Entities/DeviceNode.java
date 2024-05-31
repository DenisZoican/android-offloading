package com.example.bluetoothconnection.communication.Entities;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DeviceNode implements Serializable {
    private DeviceInitialInfo deviceInitialInfo;
    private Map<String,DeviceNode> neighbours;

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

    public float getWeight() {
        float wait = -1;
        //logica pentru calcularea wait-ului unui nod pe baza deviceInitialInfo
        return wait;
    }
}
