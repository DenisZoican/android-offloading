package com.example.bluetoothconnection.communication.PayloadDataEntities;

import static com.example.bluetoothconnection.communication.Utils.Common.MessageContentType;

import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;
import com.example.bluetoothconnection.communication.Entities.DeviceNode;

public class PayloadDeviceNodeData extends PayloadData{
    private DeviceNode deviceNode;

    public PayloadDeviceNodeData(DeviceNode deviceNode) {
        super(MessageContentType.DeviceNode);
        this.deviceNode = deviceNode;
    }

    public DeviceNode getDeviceNode() {
        return deviceNode;
    }
}