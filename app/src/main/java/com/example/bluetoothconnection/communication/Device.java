package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.utils.Common.getUniqueName;
import static com.example.bluetoothconnection.utils.EncryptionUtils.SECRET_AUTHENTICATION_TOKEN;

import android.app.Activity;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bluetoothconnection.utils.EncryptionUtils;
import com.google.android.gms.nearby.connection.ConnectionsClient;

import org.opencv.core.Mat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.function.Consumer;

public abstract class Device {
    // Key pair for signing and verifying messages
    private KeyPair keyPair;

    Activity activity;
    final ConnectionsClient connectionsClient;
    final String uniqueName = getUniqueName();
    static String payloadType = null;

    public Consumer<Mat> onPayloadReceivedCallbackFunction;

    public Device(Activity activity, ConnectionsClient connectionsClient){
        this.activity = activity;
        this.connectionsClient = connectionsClient;

        generateKeyPair();
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
    private void generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
