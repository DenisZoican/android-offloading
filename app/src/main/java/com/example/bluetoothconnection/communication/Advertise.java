package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Utils.Common.createPayloadFromDeviceNode;
import static com.example.bluetoothconnection.communication.Utils.Common.createPayloadFromResponseMat;
import static com.example.bluetoothconnection.communication.Utils.Common.extractDataFromPayload;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.checkAuthenticationToken;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.getEncryptedAuthenticationToken;
import static com.example.bluetoothconnection.opencv.ImageProcessing.convertImageToBitmap;
import static com.example.bluetoothconnection.opencv.ImageProcessing.processImage;

import android.app.Activity;
import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothconnection.R;
import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;
import com.example.bluetoothconnection.communication.Entities.DeviceNode;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadDeviceNodeData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadRequestMatData;
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
import java.util.Map;
import java.util.stream.Collectors;

public class Advertise extends Device {
    public Advertise(Context context, Activity activity, ConnectionsClient connectionsClient) throws Exception {
        super(context, activity, connectionsClient);
    }
    public void start() throws Exception {
        activity.setContentView(R.layout.activity_advertise_main);

        startDiscovery();
        startAdvertise();
    }

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
                case RequestImage:
                    try {
                        requestMatReceivedBehavior((PayloadRequestMatData)payloadData, endpointId);
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

    public void sendMessage(Mat image, String processorUniqueName, String endpointId) throws Exception {
        PublicKey senderPublicKey = getNode().getNeighbours().get(endpointId).getDeviceInitialInfo().getPublicKey();
        if(senderPublicKey == null){
            Toast.makeText(activity, "No public key found for discovery device.", Toast.LENGTH_SHORT).show();
            return;
        }

        Payload processedPayload = createPayloadFromResponseMat(image, processorUniqueName, senderPublicKey, AESSecretKeyUsedForMessages);
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


    private void requestMatReceivedBehavior(PayloadRequestMatData payloadRequestMatData, String endpointId) throws Exception {
        Mat receivedMat = payloadRequestMatData.getImage();

        DeviceNode currentNode = payloadRequestMatData.getTreeNode();
        /// Simulate multiple devices when we have only one
        validNeighboursUsedInCurrentCommunication  = currentNode.getNeighbours().entrySet().stream().filter(entry-> entry.getValue().getTotalWeight() > 10000)
                                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        int numberOfParts = validNeighboursUsedInCurrentCommunication.size();
        if(currentNode.getPersonalWeight() != 0) {
            numberOfParts++;
        }
        initializeImageValues(receivedMat, numberOfParts);

        // Send image to another device
        //String endpointId = this.getNode().getNeighbours().keySet().iterator().next();
        int index = 0;
        for (String neighbourEndpointId : validNeighboursUsedInCurrentCommunication.keySet()) {
            try {
                sendRequestImagePartToSingleEndpoint(neighbourEndpointId, index);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            index ++;
        }

        if(validNeighboursUsedInCurrentCommunication.keySet().size() < numberOfParts){
            int imagePartIndex = validNeighboursUsedInCurrentCommunication.keySet().size();

            Mat partOfImageThatNeedsProcessed = partsNeededFromImage.get(imagePartIndex);
            Mat processedMat = processImage(partOfImageThatNeedsProcessed);
            partsNeededFromImage.remove(imagePartIndex);

            sendResponseImagePartToSingleEndpoint(endpointId, processedMat, getNode().getUniqueName());
            ImageView imageView = activity.findViewById(R.id.imageView);
            imageView.setImageBitmap(convertImageToBitmap(processedMat));
        }

        /*Mat processedMat = processImage(receivedMat);


        sendMessage(processedMat, getNode().getUniqueName(), endpointId);*/
    }
    private void sendDeviceNode(String endpointId) {
        /////////////should not send all neighbours, just calculate the weight and send it
        /////////////it should be enough
        DeviceNode node = getNode();
        DeviceInitialInfo deviceInitialInfo = new DeviceInitialInfo(keyPairUsedForAESSecretKEy.getPublic(),getBatteryLevel(),getCpuUsage(),getCpuCores());

        DeviceNode copil1 = new DeviceNode();
        DeviceNode copil2 = new DeviceNode();
        DeviceNode copil3 = new DeviceNode();
        DeviceNode copil1_copil1= new DeviceNode();
        DeviceNode copil2_copil1 = new DeviceNode();
        DeviceNode copil2_copil1_copil1 = new DeviceNode();

        DeviceInitialInfo copil1_deviceInitialInfo = new DeviceInitialInfo(keyPairUsedForAESSecretKEy.getPublic(), 55, 0.73, 8);
        DeviceInitialInfo copil2_deviceInitialInfo = new DeviceInitialInfo(keyPairUsedForAESSecretKEy.getPublic(), 27, 0.01, 8);
        DeviceInitialInfo copil3_deviceInitialInfo = new DeviceInitialInfo(keyPairUsedForAESSecretKEy.getPublic(), 49, 0.56, 16);
        DeviceInitialInfo copil1_copil1_deviceInitialInfo = new DeviceInitialInfo(keyPairUsedForAESSecretKEy.getPublic(), 50, 0.79, 6);
        DeviceInitialInfo copil2_copil1_deviceInitialInfo = new DeviceInitialInfo(keyPairUsedForAESSecretKEy.getPublic(), 23, 0.88, 8);
        DeviceInitialInfo copil2_copil1_copil1_deviceInitialInfo = new DeviceInitialInfo(keyPairUsedForAESSecretKEy.getPublic(), 98, 0.53, 8);

        copil1.setDeviceInitialInfo(copil1_deviceInitialInfo);
        copil2.setDeviceInitialInfo(copil2_deviceInitialInfo);
        copil3.setDeviceInitialInfo(copil3_deviceInitialInfo);
        copil1_copil1.setDeviceInitialInfo(copil1_copil1_deviceInitialInfo);
        copil2_copil1.setDeviceInitialInfo(copil2_copil1_deviceInitialInfo);
        copil2_copil1_copil1.setDeviceInitialInfo(copil2_copil1_copil1_deviceInitialInfo);


        node.getNeighbours().put("1", copil1);
        node.getNeighbours().put("2", copil2);
        node.getNeighbours().put("3", copil3);
        node.getNeighbours().put("211", copil2_copil1_copil1);

        copil1.getNeighbours().put("11", copil1_copil1);
        copil1.getNeighbours().put("2", copil2);
        copil1.getNeighbours().put(getNode().getUniqueName(), node);

        copil1_copil1.getNeighbours().put("1", copil1);
        copil1_copil1.getNeighbours().put("2", copil2);


        copil2.getNeighbours().put(getNode().getUniqueName(), node);
        copil2.getNeighbours().put("1", copil1);
        copil2.getNeighbours().put("11", copil1_copil1);
        copil2.getNeighbours().put("21", copil2_copil1);

        copil2_copil1.getNeighbours().put("2", copil2);
        copil2_copil1.getNeighbours().put("211", copil2_copil1_copil1);

        copil2_copil1_copil1.getNeighbours().put("21", copil2_copil1);
        copil2_copil1_copil1.getNeighbours().put("3", copil3);
        copil2_copil1_copil1.getNeighbours().put(getNode().getUniqueName(), node);

        copil3.getNeighbours().put(getNode().getUniqueName(), node);
        copil3.getNeighbours().put("211", copil2_copil1_copil1);


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
