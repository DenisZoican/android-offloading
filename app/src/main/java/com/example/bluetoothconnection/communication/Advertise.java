package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Utils.Common.createPayloadFromDeviceNode;
import static com.example.bluetoothconnection.communication.Utils.Common.extractDataFromPayload;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.checkAuthenticationToken;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.getEncryptedAuthenticationToken;
import static com.example.bluetoothconnection.opencv.ImageProcessing.convertImageToBitmap;
import static com.example.bluetoothconnection.opencv.ImageProcessing.processImage;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothconnection.R;
import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;
import com.example.bluetoothconnection.communication.Entities.DeviceNode;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadDeviceNodeData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadRequestMatData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadResponseMatData;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Advertise extends Device {

    String requestInitiatorEndpointId;
    Map<String, Integer> devicesUsedInProcessing;
    int requestImageBaseLinePosition;

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
                getNode().getNeighbours().remove(endpointId);
                List<String> sortedEndpointsThatAreNotUsedInProcessing = validNeighboursUsedInCurrentCommunication.entrySet().stream()
                            .filter(entry-> !devicesUsedInProcessing.containsKey(entry.getKey()))
                            .sorted((previous, current)-> (int) (current.getValue().getTotalWeight() - previous.getValue().getTotalWeight()))
                            .map(entry->entry.getKey()).collect(Collectors.toList());

                int imagePartThatNeedsToBeProcessedIndex = devicesUsedInProcessing.get(endpointId);
                devicesUsedInProcessing.remove(endpointId);

                if(sortedEndpointsThatAreNotUsedInProcessing.size() == 0) {
                    if(!devicesUsedInProcessing.containsKey("Aida")) {
                        try {
                            processImagePartMyself(imagePartThatNeedsToBeProcessedIndex, requestImageBaseLinePosition);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        //trimit inapoi la discovery
                    }
                } else {
                    String availableNodeEndpointId = sortedEndpointsThatAreNotUsedInProcessing.get(0);
                    //si trimit la availableNodeEndpointId
                }
                
                List<String> endpointsIds = new ArrayList<>(getNode().getNeighbours().keySet());
                sendDeviceNode(endpointsIds, new HashSet<>());

                updateAllDevicesTextView();
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
                        //sendDeviceInitialInfo(endpointId); ////// SHOULD PUT BATTERY INFO HERE - with love for Aidel
                    List<String> foundNeighbour = new ArrayList<>();
                    foundNeighbour.add(endpointId);

                    Set<String> visitedNodes = new HashSet<>();
                    visitedNodes.add(endpointId);

                    sendDeviceNode(foundNeighbour, visitedNodes);
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
            boolean isDeviceNode = (payload.asBytes()[0] == 1);

            PayloadData payloadData = null;
            try {
                payloadData =  isDeviceNode ? Common.extractDeviceNodeFromPayload(payload) : extractDataFromPayload(payload, keyPairUsedForAESSecretKEy.getPrivate());
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
                case ResponseImage:
                    responseMatReceivedBehavior((PayloadResponseMatData)payloadData, endpointId);
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
    public void disconnect() {
        for(String neighbourEndpointId : getNode().getNeighbours().keySet()) {
            connectionsClient.disconnectFromEndpoint(neighbourEndpointId);
        }
    }

    public void destroy() {
        connectionsClient.stopAdvertising();
    }

    private void deviceNodeReceivedBehavior(PayloadDeviceNodeData payloadDeviceNodeData, String endpointId) {
        DeviceNode receivedNode = payloadDeviceNodeData.getDeviceNode();
        receivedNode.getNeighbours().put(payloadDeviceNodeData.getDestinationEndpointId(), this.getNode());
        this.getNode().getNeighbours().put(endpointId, payloadDeviceNodeData.getDeviceNode());

        Set<String> visitedNodes = payloadDeviceNodeData.getVisitedNodes();
        visitedNodes.add(endpointId);

        List<String> neighboursThatNeedToBeUpdated = new ArrayList<>(getNode().getNeighbours().keySet()).stream()
                .filter(neighbourEndpointId-> !visitedNodes.contains(neighbourEndpointId))
                .collect(Collectors.toList());

        visitedNodes.addAll(neighboursThatNeedToBeUpdated);

        sendDeviceNode(neighboursThatNeedToBeUpdated, visitedNodes);

        updateAllDevicesTextView();
    }

    private void responseMatReceivedBehavior(PayloadResponseMatData payloadResponseMatData, String endpointId) {
        devicesUsedInProcessing.remove(endpointId);
        try {
            sendResponseImagePartToSingleEndpoint(
                    requestInitiatorEndpointId,
                    payloadResponseMatData.getImage(),
                    payloadResponseMatData.getLinePosition(),
                    payloadResponseMatData.getProcessorNodeUniqueName()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void requestMatReceivedBehavior(PayloadRequestMatData payloadRequestMatData, String endpointId) throws Exception {
        this.requestInitiatorEndpointId = endpointId;
        this.requestImageBaseLinePosition = payloadRequestMatData.getLinePosition();

        Mat receivedMat = payloadRequestMatData.getImage();

        DeviceNode currentNode = payloadRequestMatData.getTreeNode();
        /// Simulate multiple devices when we have only one
        validNeighboursUsedInCurrentCommunication  = currentNode.getNeighbours().entrySet().stream().filter(entry-> entry.getValue().getTotalWeight() != 0)
                                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        devicesUsedInProcessing = new HashMap();

        int numberOfParts = validNeighboursUsedInCurrentCommunication.size();
        if(currentNode.getPersonalWeight() != 0) {
            numberOfParts++;
        }
        initializeImageValues(receivedMat, numberOfParts);

        // Send image to another device
        int index = 0;
        for (String neighbourEndpointId : validNeighboursUsedInCurrentCommunication.keySet()) {
            try {
                sendRequestImagePartToSingleEndpoint(neighbourEndpointId, index, payloadRequestMatData.getLinePosition());
                devicesUsedInProcessing.put(neighbourEndpointId, index);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            index ++;
        }

        if(validNeighboursUsedInCurrentCommunication.keySet().size() < numberOfParts ) { //&& !Build.MODEL.equals("SM-G991B")){
            int imagePartIndex = validNeighboursUsedInCurrentCommunication.keySet().size();
            processImagePartMyself(imagePartIndex, payloadRequestMatData.getLinePosition());
        }
    }
    private void processImagePartMyself(int imagePartIndex, int baseLinePosition) throws Exception {
        devicesUsedInProcessing.put("Aida", imagePartIndex);
        Mat partOfImageThatNeedsProcessed = partsNeededFromImage.get(imagePartIndex);

        Toast.makeText(activity, "Processing", Toast.LENGTH_SHORT).show();
        Mat processedMat = processImage(partOfImageThatNeedsProcessed);
        Toast.makeText(activity, "NOT Processing", Toast.LENGTH_SHORT).show();
        //partsNeededFromImage.remove(imagePartIndex);
        int imageLinePartPosition = baseLinePosition + imagePartIndex*this.getImagePartHeight();
        sendResponseImagePartToSingleEndpoint(requestInitiatorEndpointId, processedMat, imageLinePartPosition, getNode().getUniqueName());
        ImageView imageView = activity.findViewById(R.id.imageView);
        imageView.setImageBitmap(convertImageToBitmap(processedMat));
        devicesUsedInProcessing.remove("Aida");
    }
    ////// UI stuff
    private void updateAllDevicesTextView(){
        TextView allDevicesTextView = activity.findViewById(R.id.allDevices);
        String allNeighboursText = getNode().getNeighbours().keySet().stream().reduce("", (previousElement, currentElement)->{
            return previousElement + " " + currentElement;
        });
        allDevicesTextView.setText(allNeighboursText);
    }

}
