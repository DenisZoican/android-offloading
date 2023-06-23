package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Common.SERVICE_ID;
import static com.example.bluetoothconnection.communication.Common.STRATEGY;
import static com.example.bluetoothconnection.utils.Common.getUniqueName;

import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;

import com.example.bluetoothconnection.MainActivity;
import com.example.bluetoothconnection.utils.Common;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Discovery extends Device{

    private Set<String> allDevicesIds = new ArraySet();

    public Discovery(ConnectionsClient connectionsClient){
        super(connectionsClient, DeviceType.Discovery);
    }

    public void start() {
        System.out.println("STTTTTTTTTTTTART DISC");
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startDiscovery(
                        SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // We're discovering nearby endpoints!
                            System.out.println("SUCCESS DISCOVERY");
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We were unable to start discovering.
                            System.out.println("FAILED DISCOVERY"+e.toString());
                        });
    }

    public void sendMessage(String message) {
        Payload payload = Payload.fromBytes(message.getBytes(StandardCharsets.UTF_8));
        allDevicesIds.stream().forEach((deviceId)-> {
            System.out.println("GRRRRRRRRRRRR send message "+deviceId+" "+payload);
            connectionsClient.sendPayload(deviceId, payload);
        });
    }
    public void disconnect() {
        allDevicesIds.stream().forEach((deviceId)->{
            connectionsClient.disconnectFromEndpoint(deviceId);
        });
    }

    public void destroy() {
        connectionsClient.stopDiscovery();
    }
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    // We found an endpoint!
                    System.out.println("GRRRRRRRR We found endpoint "+endpointId);

                    // We request connections
                    connectionsClient.requestConnection(uniqueName, endpointId, connectionLifecycleCallback)
                            .addOnSuccessListener(
                                    (Void unused) -> {
                                        // We're connecting!
                                        System.out.println("We are connecting");
                                    })
                            .addOnFailureListener(
                                    (Exception e) -> {
                                        // We were unable to connect.
                                        System.out.println("We are unable connecting "+e.toString());
                                    });
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    // We lost an endpoint.
                    System.out.println("We lost endpoint "+endpointId);
                    //////// !!!!!!!!!! verify if it throws exception when list doesn't contain endpointId !!!!!!!!!!!
                    allDevicesIds.remove(endpointId);
                }
            };

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    System.out.println("GRRRRRR INITIATED");
                    // Automatically accept the connection on both devices.
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        // We're connected!
                        System.out.println("GRRRRRR CONNECTED");

                        allDevicesIds.add(endpointId);
                    } else {
                        // We were unable to connect.
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    // We're disconnected!
                }
            };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
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
