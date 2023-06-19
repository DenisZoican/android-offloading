package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Common.SERVICE_ID;
import static com.example.bluetoothconnection.communication.Common.STRATEGY;
import static com.example.bluetoothconnection.utils.Common.getUniqueName;

import android.util.Log;

import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.nio.charset.StandardCharsets;

import javax.security.auth.Subject;

public class Advertise extends Device {
    private String discoveryDeviceId; ///// Rename it later
    public Advertise(ConnectionsClient connectionsClient){
        super(connectionsClient, DeviceType.Advertise);
    }

    public void start(){
        System.out.println("BEGIN ADVER");
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();

        connectionsClient.startAdvertising(this.uniqueName, SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // We're advertising!
                            System.out.println("SUCCESS ADVER");
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We were unable to start advertising.
                            System.out.println("FAILED ADVER"+e.toString());
                        });
    }

    public void sendMessage(String message) {
        Payload payload = Payload.fromBytes(message.getBytes(StandardCharsets.UTF_8));
        connectionsClient.sendPayload(discoveryDeviceId, payload);
    }
    public void disconnect() {
        connectionsClient.disconnectFromEndpoint(discoveryDeviceId);
    }

    public void destroy() {
        connectionsClient.stopAdvertising();
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    System.out.println("GRRRRRR INITIATED");
                    // Automatically accept the connection on both devices.
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                    // Store the endpoint ID for later use.
                    discoveryDeviceId = endpointId;
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        // We're connected!
                        System.out.println("GRRRRRR CONNECTED");

                        // We should send just if you do the offloading
                        sendMessage("info: memory usage");

                    } else {
                        // We were unable to connect.
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    // We're disconnected!
                }
            };
    private final PayloadCallback payloadCallback = new PayloadCallback() {  ///// Isn't this the same with discovery???
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            // We received a payload!
            String receivedPayload = new String(payload.asBytes(), StandardCharsets.UTF_8);
            Log.d("Payload", receivedPayload);
            onPayloadReceivedCallbackFunction.accept(receivedPayload);
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            // Payload transfer status updated.
        }
    };
}
