package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Utils.Common.createPayloadFromDeviceNode;
import static com.example.bluetoothconnection.communication.Utils.Common.createPayloadFromMat;
import static com.example.bluetoothconnection.communication.Utils.Common.extractDataFromPayload;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.checkAuthenticationToken;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.getEncryptedAuthenticationToken;
import static com.example.bluetoothconnection.opencv.ImageProcessing.convertImageToBitmap;
import static com.example.bluetoothconnection.opencv.ImageProcessing.processImage;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothconnection.R;
import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;
import com.example.bluetoothconnection.communication.Entities.DeviceNode;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadDeviceNodeData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadMatData;
import com.example.bluetoothconnection.communication.Utils.Common;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import org.opencv.core.Mat;

import java.security.PublicKey;

public class Advertise extends Device {

    protected EndpointDiscoveryCallback getEndpointDiscoveryCallback(){
        return new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                // We found an endpoint!
                System.out.println("Endpoint found" + info.getServiceId()); ////////// Check if we need to check this or if it is checked automatically

                String authenticationTokenAsName = null;
                try {
                    authenticationTokenAsName = getEncryptedAuthenticationToken();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                connectionsClient.requestConnection(authenticationTokenAsName, endpointId, getConnectionLifecycleCallback())
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
                //////// !!!!!!!!!! verify if it throws exception when list doesn't contain endpointId !!!!!!!!!!!
                //////////////////put it back, removed for when entering case PayloadTransferUpdate.Status.FAILURE//////////////////////////////////////
                //discoveredDevices.remove(endpointId);
                //updateAllDevicesTextView();
            }
        };
    }

    protected ConnectionLifecycleCallback getConnectionLifecycleCallback(){
        return new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                // Automatically accept the connection on both devices.
                System.out.println("Connection initiated");
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
                    System.out.println("Connection accepted");
                    //discoveryDeviceId = endpointId;
                    updateAllDevicesTextView();
                        //sendDeviceInitialInfo(endpointId); ////// SHOULD PUT BATTERY INFO HERE - with love for Aidel
                    sendDeviceNode(endpointId);
                } else {
                    // We were unable to connect.
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                // We're disconnected!
            }
        };
    }

    private final PayloadCallback payloadCallback = new PayloadCallback() {  ///// Isn't this the same with discovery???
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            /// Must delete discoveryDevicePublicKey if disconnected from discovery device
            boolean payloadHasDeviceNodeType = !getNode().getNeighbours().containsKey(endpointId);

            PayloadData payloadData = null;
            try {
                payloadData =  payloadHasDeviceNodeType ? Common.extractDeviceNodeFromPayload(payload) : extractDataFromPayload(payload, keyPairUsedForAESSecretKEy.getPrivate());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            switch (payloadData.getMessageContentType()){
                case Image:
                    try {
                        matReceivedBehavior((PayloadMatData)payloadData, endpointId);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case DeviceNode:
                    deviceNodeReceivedBehavior((PayloadDeviceNodeData) payloadData, endpointId);
                    break;
                case Error:
                    Toast.makeText(activity, "Hash didn't match", Toast.LENGTH_SHORT).show();

            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            // Payload transfer status updated.
        }
    };

    public Advertise(Context context, Activity activity, ConnectionsClient connectionsClient) throws Exception {
        super(context, activity, connectionsClient);
    }

    public void start() throws Exception {
        activity.setContentView(R.layout.activity_advertise_main);

        startDiscovery();
        startAdvertise();
    }

    public void sendMessage(Mat image, String endpointId) throws Exception {
        PublicKey senderPublicKey = getNode().getNeighbours().get(endpointId).getDeviceInitialInfo().getPublicKey();
        if(senderPublicKey == null){
            Toast.makeText(activity, "No public key found for discovery device.", Toast.LENGTH_SHORT).show();
            return;
        }

        Payload processedPayload = createPayloadFromMat(image, senderPublicKey, AESSecretKeyUsedForMessages);
        connectionsClient.sendPayload(endpointId, processedPayload);
    }
    public void disconnect() {
        for(String neighbourEndpointId : getNode().getNeighbours().keySet()) {
            connectionsClient.disconnectFromEndpoint(neighbourEndpointId);
        }
    }

    public void destroy() {
        connectionsClient.stopAdvertising();
    }

    private void deviceNodeReceivedBehavior(PayloadDeviceNodeData payloadDeviceNodeData, String endpointId) {
        System.out.println("mi-a dat pachet");
        this.getNode().getNeighbours().put(endpointId, payloadDeviceNodeData.getDeviceNode());
    }


    private void matReceivedBehavior(PayloadMatData payloadMatData, String endpointId) throws Exception {
        Mat receivedMat = payloadMatData.getImage();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Mat processedMat = processImage(receivedMat);

        ImageView imageView = activity.findViewById(R.id.imageView);
        imageView.setImageBitmap(convertImageToBitmap(receivedMat));

        sendMessage(processedMat, endpointId);
    }
    private void sendDeviceNode(String endpointId) {
        /////////////should not send all neighbours, just calculate the weight and send it
        /////////////it should be enough
        DeviceNode node = getNode();
        DeviceInitialInfo deviceInitialInfo = new DeviceInitialInfo(keyPairUsedForAESSecretKEy.getPublic(),getBatteryLevel(),getCpuUsage(),getCpuCores());

        DeviceNode node_copil1 = new DeviceNode();
        DeviceNode node_copil1_copil1 = new DeviceNode();
        DeviceNode node_copil1_copil2 = new DeviceNode();
        DeviceNode node_copil2= new DeviceNode();
        DeviceNode node_copil2_copil1 = new DeviceNode();

        node.getNeighbours().put("shakalaka1", node_copil1);
        node.getNeighbours().put("shakalaka2", node_copil2);
        node.getNeighbours().put("shakalaka3", new DeviceNode());

        node_copil1.getNeighbours().put("shakalaka1_copil1", node_copil1_copil1);
        node_copil1_copil1.getNeighbours().put("shakalaka1_copil2", node_copil1_copil2);

        node_copil2.getNeighbours().put("shakalaka2_copil1", node_copil2_copil1);

        node.setDeviceInitialInfo(deviceInitialInfo);
        try {
            Payload payload = createPayloadFromDeviceNode(node);
            connectionsClient.sendPayload(endpointId, payload);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    ////// UI stuff
    private void updateAllDevicesTextView(){
        TextView allDevicesTextView = activity.findViewById(R.id.allDevices);
        String allNeighboursText = getNode().getNeighbours().keySet().stream().reduce("", (previousElement, currentElement)->{
            return previousElement + "\n" + currentElement;
        });
        allDevicesTextView.setText(allNeighboursText);
    }

}
