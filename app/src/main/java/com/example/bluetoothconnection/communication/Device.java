package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.utils.Common.getUniqueName;

import com.google.android.gms.nearby.connection.ConnectionsClient;

import java.util.EventListener;
import java.util.Observable;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Device {

    final ConnectionsClient connectionsClient;
    final String uniqueName = getUniqueName();

    public Consumer<String> onPayloadReceivedCallbackFunction;

    public Device(ConnectionsClient connectionsClient){
        this.connectionsClient = connectionsClient;
    }

    public void setOnPayloadReceivedCallbackFunction(Consumer<String> onPayloadReceivedCallbackFunction){
        this.onPayloadReceivedCallbackFunction = onPayloadReceivedCallbackFunction;
    }

    abstract public void start();
    abstract public void sendMessage(String message);
    abstract public void disconnect();
    abstract public void destroy();
}
