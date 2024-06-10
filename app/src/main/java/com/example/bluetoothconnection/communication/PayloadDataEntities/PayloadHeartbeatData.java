package com.example.bluetoothconnection.communication.PayloadDataEntities;

import com.example.bluetoothconnection.communication.Entities.DeviceNode;
import com.example.bluetoothconnection.communication.Utils.Common;

import org.opencv.core.Mat;

public class PayloadHeartbeatData extends PayloadData{
    public PayloadHeartbeatData() {
        super(Common.MessageContentType.Heartbeat);
    }
}
