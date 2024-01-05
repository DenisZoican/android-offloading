package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.utils.Common.getUniqueName;

import android.app.Activity;

import com.google.android.gms.nearby.connection.ConnectionsClient;

import org.opencv.core.Mat;

import java.util.function.Consumer;

public abstract class Device {

    Activity activity;
    final ConnectionsClient connectionsClient;
    final String uniqueName = getUniqueName();
    static String payloadType = null;

    public Consumer<Mat> onPayloadReceivedCallbackFunction;

    public Device(Activity activity, ConnectionsClient connectionsClient){
        this.activity = activity;
        this.connectionsClient = connectionsClient;
    }

    public void setOnPayloadReceivedCallbackFunction(Consumer<Mat> onPayloadReceivedCallbackFunction){
        this.onPayloadReceivedCallbackFunction = onPayloadReceivedCallbackFunction;
    }

    abstract public void start();
    abstract public void disconnect();
    abstract public void destroy();
}
