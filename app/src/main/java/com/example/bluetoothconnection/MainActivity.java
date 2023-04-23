package com.example.bluetoothconnection;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String SERVICE_ID = "com.example.nearbytest";
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private ConnectionsClient connectionsClient;
    private String endpointId;

    private static final int MY_PERMISSIONS_REQUEST_NEARBY_WIFI_DEVICES = 2929;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        System.out.println("GRRRRRRRRRRRR");
        if (requestCode == MY_PERMISSIONS_REQUEST_NEARBY_WIFI_DEVICES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
            } else {
                // Permission is not granted, show a message or disable the feature that requires the permission
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("GRRRRRRRRRRRR");
        // Initialize the Nearby Connections client
        connectionsClient = Nearby.getConnectionsClient(this);

        /// Asking user to grant multiple permission. Should refactor. Too many if/else. Maybe some permissions are already granted and we shouldn't check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    MY_PERMISSIONS_REQUEST_NEARBY_WIFI_DEVICES);
        } else
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN},
                    MY_PERMISSIONS_REQUEST_NEARBY_WIFI_DEVICES);
        } else
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_ADVERTISE},
                    MY_PERMISSIONS_REQUEST_NEARBY_WIFI_DEVICES);
        } else
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.NEARBY_WIFI_DEVICES},
                    MY_PERMISSIONS_REQUEST_NEARBY_WIFI_DEVICES);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_NEARBY_WIFI_DEVICES);
        } else {
            // Permission is already granted
            System.out.println("GRRRRRRRRRRRR  GRANTED");
            if(Build.BRAND.toString().equals("google")) {
                System.out.println("STTTTTTTTTTTTART ADV");
                // Start advertising the endpoint
                startAdvertising();
            } else {
                System.out.println("STTTTTTTTTTTTART DISC");
                // Start discovering nearby endpoints
                startDiscovery();
            }
        }

        setContentView(R.layout.activity_main);


    }

    private void startAdvertising() {
        System.out.println("GRRRRRRRR BEGIN ADVER");

        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startAdvertising(
                        getLocalUserName(), SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // We're advertising!
                            System.out.println("GRRRRRRRR SUCCESS ADVER");
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We were unable to start advertising.
                            System.out.println("GRRRRRRRR FAILED ADVER"+e.toString());
                        });
    }

    private void startDiscovery() {
        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startDiscovery(
                        SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // We're discovering nearby endpoints!
                            System.out.println("GRRRRRRRR SUCCESS DISCOVERY");
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We were unable to start discovering.
                            System.out.println("GRRRRRRRR FAILED DISCOVERY"+e.toString());
                        });
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    System.out.println("GRRRRRR INITIATED");
                    // Automatically accept the connection on both devices.
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                    // Store the endpoint ID for later use.
                    MainActivity.this.endpointId = endpointId;
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        // We're connected!
                        System.out.println("GRRRRRR CONNECTED");

                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                sendMessage("Hello from here. Now we are connected <3");
                            }
                        }, 100);

                    } else {
                        // We were unable to connect.
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    // We're disconnected!
                }
            };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    System.out.println("We found endpoint "+endpointId);
                    // We found an endpoint!
                    connectionsClient.requestConnection(getLocalUserName(), endpointId, connectionLifecycleCallback)
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
                }
            };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            // We received a payload!
            String receivedPayload = new String(payload.asBytes(), StandardCharsets.UTF_8);
            Log.d("Payload", receivedPayload);
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            // Payload transfer status updated.
        }
    };

    private String getLocalUserName() {
        // Return a unique name for the local user.
        return "user" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Call this method to send a message to the other endpoint.
    private void sendMessage(String message) {
        Payload payload = Payload.fromBytes(message.getBytes(StandardCharsets.UTF_8));
        connectionsClient.sendPayload(endpointId, payload);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Disconnect from the endpoint.
        connectionsClient.disconnectFromEndpoint(endpointId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop advertising and discovering.
        connectionsClient.stopAdvertising();
        connectionsClient.stopDiscovery();
    }

}