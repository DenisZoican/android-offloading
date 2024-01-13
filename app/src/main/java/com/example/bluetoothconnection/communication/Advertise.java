package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Common.PAYLOAD_TYPE_IMAGE;
import static com.example.bluetoothconnection.communication.Common.PAYLOAD_TYPE_STRING;
import static com.example.bluetoothconnection.communication.Common.SERVICE_ID;
import static com.example.bluetoothconnection.communication.Common.STRATEGY;
import static com.example.bluetoothconnection.communication.Common.convertMatToPayload;
import static com.example.bluetoothconnection.communication.Common.convertPayloadToMat;
import static com.example.bluetoothconnection.opencv.ImageProcessing.convertImageToBitmap;
import static com.example.bluetoothconnection.opencv.ImageProcessing.processImage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothconnection.R;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import org.opencv.core.Mat;

public class Advertise extends Device {
    private String discoveryDeviceId; ///// Rename it later
    private String payloadType;

    public Advertise(Context context, Activity activity, ConnectionsClient connectionsClient){
        super(context, activity, connectionsClient);

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(batteryReceiver, filter);
    }

    public void start(){
        activity.setContentView(R.layout.activity_advertise_main);
        initializeUiElements();

        System.out.println("BEGIN ADVER");
        AdvertisingOptions advertisingOptions =
            new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();

        connectionsClient.startAdvertising(this.uniqueName, SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // We're advertising!
                            System.out.println("SUCCESS ADVER "+this.uniqueName);
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We were unable to start advertising.
                            System.out.println("FAILED ADVER" + e.toString());
                        });
    }

    public void sendMessage(Mat image) {
        Payload processedPayload = convertMatToPayload(image);
        payloadType = PAYLOAD_TYPE_IMAGE;
        connectionsClient.sendPayload(discoveryDeviceId, processedPayload);
    }
    public void sendBatteryUsage(String batteryMessage) {

        System.out.println("Send batteryMessage from advertise");
        byte[] toSend = batteryMessage.getBytes();
        Payload payload = Payload.fromBytes(toSend);
        payloadType = PAYLOAD_TYPE_STRING;
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
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        // We're connected!
                        System.out.println("GRRRRRR CONNECTED");

                        // We should send just if you do the offloading
                        //sendMessage("info: memory usage");

                        discoveryDeviceId = endpointId;
                        updateAllDevicesTextView();

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
            System.out.println("DA DA");
            Toast.makeText(activity, "Received", Toast.LENGTH_SHORT).show();

            Mat receivedMat = convertPayloadToMat(payload);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Mat processedMat = processImage(receivedMat);

            ImageView imageView = activity.findViewById(R.id.imageView);
            imageView.setImageBitmap(convertImageToBitmap(receivedMat));

            sendMessage(processedMat);

            //Mat receivedImage = convertPayloadToMat(payload,500,500); ////////////// Dimensions are false 100%

            //sendMat(receivedImage);

            return;
            /*Mat processedImage;

            try { ////////// Simulate that we do something to the message
                Thread.sleep(2000);
                processedImage = ImageProcessing.processImage(receivedImage);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            sendMat(processedImage);

             */
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            // Payload transfer status updated.
        }
    };

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                // Retrieve battery level
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

                // Retrieve battery scale (maximum level)
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                // Calculate battery percentage
                float batteryPercentage = (level / (float) scale) * 100;

                // Send battery usage as payload to the connected device
                //sendPayload(String.valueOf(batteryPercentage));

                System.out.println("Battery Level: " + batteryPercentage + "%");
                Toast.makeText(activity, "Battery Level: " + batteryPercentage + "%", Toast.LENGTH_SHORT).show();
            }
        }
    };


    private void updateAllDevicesTextView(){
        TextView allDevicesTextView = activity.findViewById(R.id.allDevices);

        allDevicesTextView.setText(discoveryDeviceId);
    }

    private void initializeUiElements(){
        initializeSendButton();
        initializeSendBatteryButton();
    }

    private void initializeSendButton(){
        Button sendButton = activity.findViewById(R.id.button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectionsClient.sendPayload(discoveryDeviceId, Payload.fromBytes("Hello".getBytes()));
            }
        });
    }

    private void initializeSendBatteryButton() {
        Button sendButton = activity.findViewById(R.id.buttonBattery);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBatteryUsage("Baterie:50%");
            }
        });
    }
}
