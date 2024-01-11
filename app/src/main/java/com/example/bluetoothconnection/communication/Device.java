package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.utils.Common.getUniqueName;
import static com.example.bluetoothconnection.utils.EncryptionUtils.SECRET_AUTHENTICATION_TOKEN;

import android.app.Activity;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bluetoothconnection.utils.EncryptionUtils;
import com.google.android.gms.nearby.connection.ConnectionsClient;

import org.opencv.core.Mat;

import java.util.EventListener;
import java.util.Observable;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Device {

    Activity activity;
    final ConnectionsClient connectionsClient;

    public Consumer<Mat> onPayloadReceivedCallbackFunction;

    public Device(Activity activity, ConnectionsClient connectionsClient){
        this.activity = activity;
        this.connectionsClient = connectionsClient;
    }

    public void setOnPayloadReceivedCallbackFunction(Consumer<Mat> onPayloadReceivedCallbackFunction){
        this.onPayloadReceivedCallbackFunction = onPayloadReceivedCallbackFunction;
    }

    public boolean checkAuthenticationToken(String authenticationToken) throws Exception {
        return SECRET_AUTHENTICATION_TOKEN.equals(EncryptionUtils.decrypt(authenticationToken));
    }

    abstract public void start() throws Exception;
    abstract public void disconnect();
    abstract public void destroy();
}
