package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Utils.Common.SERVICE_ID;
import static com.example.bluetoothconnection.communication.Utils.Common.createPayloadFromDeviceInitialInfo;
import static com.example.bluetoothconnection.communication.Utils.Common.extractDataFromPayload;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.checkAuthenticationToken;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.generateAESKey;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.generateRSAKeyPair;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.getEncryptedAuthenticationToken;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadDeviceInitialInfoData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadMatData;
import com.example.bluetoothconnection.communication.Utils.Common;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

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
    public Context context;

    public Device(Context context, Activity activity, ConnectionsClient connectionsClient) throws Exception {
        this.activity = activity;
        this.connectionsClient = connectionsClient;
        this.context = context;

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

    protected void startAdvertise() throws Exception {
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();

        String authenticationTokenAsName = getEncryptedAuthenticationToken();
        connectionsClient.startAdvertising(authenticationTokenAsName, SERVICE_ID, getConnectionLifecycleCallback(), advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Toast.makeText(activity, "Started advertising", Toast.LENGTH_SHORT).show();
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Toast.makeText(activity, "Failed to advertise - "+e.toString(), Toast.LENGTH_SHORT).show();
                        });
    }

    protected void startDiscovery(){
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
        connectionsClient.startDiscovery(
                        SERVICE_ID, getEndpointDiscoveryCallback(), discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // We're discovering nearby endpoints!
                            Toast.makeText(activity, "We are discovering.", Toast.LENGTH_SHORT).show();
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We were unable to start discovering.
                            Toast.makeText(activity, "Failed to discover - "+e.toString(), Toast.LENGTH_SHORT).show();
                        });
    }

    protected EndpointDiscoveryCallback getEndpointDiscoveryCallback(){
        return null;
    }

    protected ConnectionLifecycleCallback getConnectionLifecycleCallback(){
        return null;
    }
}
