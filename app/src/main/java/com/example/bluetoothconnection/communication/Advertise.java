package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Utils.Common.SERVICE_ID;
import static com.example.bluetoothconnection.communication.Utils.Common.STRATEGY;
import static com.example.bluetoothconnection.communication.Utils.Common.createPayloadFromMat;
import static com.example.bluetoothconnection.communication.Utils.Common.extractDataFromPayload;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.checkAuthenticationToken;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.getEncryptedAuthenticationToken;
import static com.example.bluetoothconnection.opencv.ImageProcessing.convertImageToBitmap;
import static com.example.bluetoothconnection.opencv.ImageProcessing.processImage;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothconnection.R;
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
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import org.opencv.core.Mat;

import java.security.PublicKey;

public class Advertise extends Device {
    private String discoveryDeviceId;
    private PublicKey discoveryDevicePublicKey;
    public Advertise(Activity activity, ConnectionsClient connectionsClient) throws Exception {
        super(activity, connectionsClient);
    }

    public void start() throws Exception {
        activity.setContentView(R.layout.activity_advertise_main);
        initializeUiElements();

        AdvertisingOptions advertisingOptions =
            new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();

        String authenticationTokenAsName = getEncryptedAuthenticationToken();
        connectionsClient.startAdvertising(authenticationTokenAsName, SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Toast.makeText(activity, "Started advertising", Toast.LENGTH_SHORT).show();
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Toast.makeText(activity, "Failed to advertise - "+e.toString(), Toast.LENGTH_SHORT).show();
                        });
    }

    public void sendMessage(Mat image) throws Exception {
        if(discoveryDevicePublicKey == null){
            Toast.makeText(activity, "No public key found for discovery device.", Toast.LENGTH_SHORT).show();
            return;
        }

        Payload processedPayload = createPayloadFromMat(image, discoveryDevicePublicKey, AESSecretKeyUsedForMessages);
        connectionsClient.sendPayload(discoveryDeviceId, processedPayload);
    }
    public void sendBatteryUsage(String batteryMessage) {
        System.out.println("Send batteryMessage from advertise");
        byte[] toSend = batteryMessage.getBytes();
        Payload payload = Payload.fromBytes(toSend);
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
                    // Automatically accept the connection on both devices.
                    try {
                        if(checkAuthenticationToken(connectionInfo.getEndpointName())){
                            connectionsClient.acceptConnection(endpointId, payloadCallback);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        discoveryDeviceId = endpointId;
                        updateAllDevicesTextView();

                        sendDeviceInitialInfo(endpointId); ////// SHOULD PUT BATTERY INFO HERE - with love for Aidel
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
            boolean isEndpointTheDiscoveryDevice = discoveryDeviceId == endpointId;
            if(isEndpointTheDiscoveryDevice){
                return;
            }

            /// Must delete discoveryDevicePublicKey if disconnected from discovery device
            boolean isDeviceInitialInfoPayload = discoveryDevicePublicKey == null;

            PayloadData payloadData = null;
            try {
                payloadData =  isDeviceInitialInfoPayload ? extractDataFromPayload(payload) : extractDataFromPayload(payload, keyPairUsedForAESSecretKEy.getPrivate());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            switch (payloadData.getMessageContentType()){
                case Image:
                    try {
                        matReceivedBehavior((PayloadMatData)payloadData);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case InitialDeviceInfo:
                    deviceInitialInfoReceivedBehavior((PayloadDeviceInitialInfoData) payloadData);
                    break;

            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            // Payload transfer status updated.
        }
    };

    private void deviceInitialInfoReceivedBehavior(PayloadDeviceInitialInfoData payloadDeviceInitialInfoData) {
        this.discoveryDevicePublicKey = payloadDeviceInitialInfoData.getDeviceInitialInfo().getPublicKey();
    }

    private void matReceivedBehavior(PayloadMatData payloadMatData) throws Exception {
        Mat receivedMat = payloadMatData.getImage();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Mat processedMat = processImage(receivedMat);

        ImageView imageView = activity.findViewById(R.id.imageView);
        imageView.setImageBitmap(convertImageToBitmap(receivedMat));

        sendMessage(processedMat);
    }

    private void sendDeviceInitialInfo(String endpointId){
        DeviceInitialInfo deviceInitialInfo = new DeviceInitialInfo(keyPairUsedForAESSecretKEy.getPublic(),29);
        try {
            sendDeviceInitialInfo(deviceInitialInfo, endpointId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    ////// UI stuff
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
