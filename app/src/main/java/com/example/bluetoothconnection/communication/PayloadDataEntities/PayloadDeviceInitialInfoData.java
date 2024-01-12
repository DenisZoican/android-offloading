package com.example.bluetoothconnection.communication.PayloadDataEntities;

import static com.example.bluetoothconnection.communication.Utils.Common.MessageContentType;

import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;

public class PayloadDeviceInitialInfoData extends PayloadData{
    private DeviceInitialInfo deviceInitialInfo;

    public PayloadDeviceInitialInfoData(DeviceInitialInfo deviceInitialInfo) {
        super(MessageContentType.InitialDeviceInfo);
        this.deviceInitialInfo = deviceInitialInfo;
    }

    public DeviceInitialInfo getDeviceInitialInfo() {
        return deviceInitialInfo;
    }
}