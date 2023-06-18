package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.utils.Common.getUniqueName;

import com.google.android.gms.nearby.connection.ConnectionsClient;

public abstract class Device {

    ConnectionsClient connectionsClient;
    final String uniqueName = getUniqueName();
    private DeviceType deviceType;

    public Device(ConnectionsClient connectionsClient, DeviceType deviceType){
        this.connectionsClient = connectionsClient;
        this.deviceType = deviceType;
    }

    abstract public void start();
    abstract public void sendMessage(String message);
    abstract public void disconnect();
    abstract public void destroy();
}
