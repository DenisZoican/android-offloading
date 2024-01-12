package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Utils.Common.createPayloadFromDeviceInitialInfo;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.generateAESKey;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.generateRSAKeyPair;

import android.app.Activity;

import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Payload;

import org.opencv.core.Mat;

import java.security.KeyPair;
import java.util.function.Consumer;

import javax.crypto.SecretKey;

public abstract class Device {
    // Key pair for signing and verifying messages
    protected final KeyPair keyPairUsedForAESSecretKEy;
    protected final SecretKey AESSecretKeyUsedForMessages;
    protected Activity activity;
    protected final ConnectionsClient connectionsClient;

    public Device(Activity activity, ConnectionsClient connectionsClient) throws Exception {
        this.activity = activity;
        this.connectionsClient = connectionsClient;

        keyPairUsedForAESSecretKEy = generateRSAKeyPair();
        AESSecretKeyUsedForMessages = generateAESKey();
    }

    protected void sendDeviceInitialInfo(DeviceInitialInfo deviceInitialInfo, String endpointId) throws Exception {
        Payload payload = createPayloadFromDeviceInitialInfo(deviceInitialInfo);
        connectionsClient.sendPayload(endpointId, payload);
    }

    abstract public void start() throws Exception;
    abstract public void disconnect();
    abstract public void destroy();
}
