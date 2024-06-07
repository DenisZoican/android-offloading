package com.example.bluetoothconnection.communication.PayloadDataEntities;

import static com.example.bluetoothconnection.communication.Utils.Common.MessageContentType;

import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;
import com.example.bluetoothconnection.communication.Entities.DeviceNode;

import java.util.List;
import java.util.Set;

public class PayloadDeviceNodeData extends PayloadData{
    private DeviceNode deviceNode;
    private String destinationEndpointId;
    private Set<String> visitedNodes;

    public PayloadDeviceNodeData(DeviceNode deviceNode, String destinationEndpointId, Set<String> visitedNodes) {
        super(MessageContentType.DeviceNode);
        this.deviceNode = deviceNode;
        this.destinationEndpointId = destinationEndpointId;
        this.visitedNodes = visitedNodes;
    }

    public DeviceNode getDeviceNode() {
        return deviceNode;
    }

    public String getDestinationEndpointId() {
        return destinationEndpointId;
    }

    public Set<String> getVisitedNodes() {
        return visitedNodes;
    }
}