package com.example.bluetoothconnection.communication.PayloadDataEntities;

import static com.example.bluetoothconnection.communication.Utils.Common.MessageContentType;

import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;
import com.example.bluetoothconnection.communication.Entities.DeviceNode;

public class PayloadDeviceNodeData extends PayloadData{
    private DeviceNode deviceNode;

    private String destinationEndpointId;

    public PayloadDeviceNodeData(DeviceNode deviceNode, String destinationEndpointId) {
        super(MessageContentType.DeviceNode);
        this.deviceNode = deviceNode;
        this.destinationEndpointId = destinationEndpointId;
    }

    public DeviceNode getDeviceNode() {
        return deviceNode;
    }

    public String getDestinationEndpointId() {
        return destinationEndpointId;
    }
}