package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Utils.Common.createHeartbeatPayload;
import static com.example.bluetoothconnection.communication.Utils.Common.createPayloadFromErrorProcessingImage;
import static com.example.bluetoothconnection.communication.Utils.Common.extractDataFromPayload;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.checkAuthenticationToken;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.getEncryptedAuthenticationToken;
import static com.example.bluetoothconnection.opencv.ImageProcessing.convertImageToBitmap;
import static com.example.bluetoothconnection.opencv.ImageProcessing.getImagePart;
import static com.example.bluetoothconnection.opencv.ImageProcessing.processImage;

import android.app.Activity;
import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothconnection.R;
import com.example.bluetoothconnection.communication.Entities.DeviceNode;
import com.example.bluetoothconnection.communication.Entities.DeviceUsedInProcessingDetails;
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

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class Advertise extends Device {

    String requestInitiatorEndpointId;

    private int baseLinePositionForImageFromRequest = 0;

    private final Timer sendHeartbeatTimer = new Timer();

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
                onEndpointLostBehaviour(endpointId);
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
                case Heartbeat:
                    heartbeatReceivedBehaviour(endpointId);
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

        DeviceUsedInProcessingDetails deviceUsedInProcessingDetails = devicesUsedInProcessing.get(endpointId);
        int remainedHeightToBeProcessed = deviceUsedInProcessingDetails.getHeightNeededToBeProcessed() - payloadResponseMatData.getImage().height();
        deviceUsedInProcessingDetails.setHeightNeededToBeProcessed(remainedHeightToBeProcessed);

        if (remainedHeightToBeProcessed == 0) {
            devicesUsedInProcessing.remove(endpointId);

            if (devicesUsedInProcessing.size() == 0) {
                sendHeartbeatTimer.cancel();
                verifyHeartbeatTimestamp.cancel();
            }
        }
    }

    private void requestMatReceivedBehavior(PayloadRequestMatData payloadRequestMatData, String endpointId) throws Exception {
        this.requestInitiatorEndpointId = endpointId;

        this.imageThatNeedsToBeProcessed = payloadRequestMatData.getImage();

        DeviceNode treeNode = payloadRequestMatData.getTreeNode();
        /// Simulate multiple devices when we have only one
        validNeighboursUsedInCurrentCommunication  = treeNode.getNeighbours().entrySet().stream().filter(entry-> entry.getValue().getTotalWeight() != 0)
                                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        devicesUsedInProcessing = new HashMap();

        double totalValidNodesWeight = validNeighboursUsedInCurrentCommunication.values().stream()
                .map(DeviceNode::getTotalWeight)
                .reduce(0.0, Double::sum);

        double personalWeight = treeNode.getPersonalWeight();
        if(personalWeight > 0){
            totalValidNodesWeight += personalWeight;
        }

        baseLinePositionForImageFromRequest = payloadRequestMatData.getLinePosition();

        int linePosition = 0;
        int imageHeight = imageThatNeedsToBeProcessed.height();

        for (String neighbourEndpointId : validNeighboursUsedInCurrentCommunication.keySet()) {
            double neighbourTotalWeight = validNeighboursUsedInCurrentCommunication.get(neighbourEndpointId).getTotalWeight();
            double percentageOfImageToBeProcessed = neighbourTotalWeight * 100 / totalValidNodesWeight;

            int heightOfImagePartThatNeedsToBeProcessed = (int) (percentageOfImageToBeProcessed * imageHeight / 100);

            DeviceUsedInProcessingDetails deviceUsedInProcessingDetails = new DeviceUsedInProcessingDetails(heightOfImagePartThatNeedsToBeProcessed,linePosition);
            this.devicesUsedInProcessing.put(neighbourEndpointId, deviceUsedInProcessingDetails);

            sendRequestImageToSingleEndpoint(neighbourEndpointId, heightOfImagePartThatNeedsToBeProcessed, linePosition + baseLinePositionForImageFromRequest);

            linePosition += heightOfImagePartThatNeedsToBeProcessed;
        }

        sendHeartbeatToRequestInitiatorAtFixedInterval();
        verifyHeartbeatTimestamps();

        if(linePosition < imageHeight){
            int heightOfImagePart = imageHeight - linePosition;
            processImagePartMyself(heightOfImagePart, linePosition);
        }
    }

    private void processImagePartMyself(int imagePartHeight, int imagePartLinePosition) throws Exception {
        DeviceUsedInProcessingDetails deviceUsedInProcessingDetails = new DeviceUsedInProcessingDetails(imagePartHeight,imagePartLinePosition);
        this.devicesUsedInProcessing.put("Aida", deviceUsedInProcessingDetails);

        Mat partOfImageThatNeedsProcessed = getImagePart(imageThatNeedsToBeProcessed,imagePartLinePosition, imagePartHeight);

        Toast.makeText(activity, "Processing", Toast.LENGTH_SHORT).show();
        Mat processedMat = processImage(partOfImageThatNeedsProcessed,30000);
        Toast.makeText(activity, "NOT Processing", Toast.LENGTH_SHORT).show();

        //partsNeededFromImage.remove(imagePartIndex);
        sendResponseImagePartToSingleEndpoint(requestInitiatorEndpointId, processedMat, imagePartLinePosition + baseLinePositionForImageFromRequest, getNode().getUniqueName());

        ImageView imageView = activity.findViewById(R.id.imageView);
        imageView.setImageBitmap(convertImageToBitmap(processedMat));

        devicesUsedInProcessing.remove("Aida");

        if (devicesUsedInProcessing.size() == 0) {
            sendHeartbeatTimer.cancel();
            verifyHeartbeatTimestamp.cancel();
        }
    }
    ////// UI stuff
    private void updateAllDevicesTextView(){
        TextView allDevicesTextView = activity.findViewById(R.id.allDevices);
        String allNeighboursText = getNode().getNeighbours().keySet().stream().reduce("", (previousElement, currentElement)->{
            return previousElement + " " + currentElement;
        });
        allDevicesTextView.setText(allNeighboursText);
    }
    protected void onEndpointLostBehaviour(String endpointId) {
        devicesUsedInProcessing.remove(endpointId);

        getNode().getNeighbours().remove(endpointId);
        DeviceUsedInProcessingDetails neighbourLostDetails = devicesUsedInProcessing.get(endpointId);
        if(neighbourLostDetails != null) {
            List<String> sortedEndpointsThatAreNotUsedInProcessing = validNeighboursUsedInCurrentCommunication.entrySet().stream()
                    .filter(entry-> !devicesUsedInProcessing.containsKey(entry.getKey()))
                    .sorted((previous, current)-> (int) (current.getValue().getTotalWeight() - previous.getValue().getTotalWeight()))
                    .map(entry->entry.getKey()).collect(Collectors.toList());

            if(sortedEndpointsThatAreNotUsedInProcessing.size() == 0) {
                //if(!devicesUsedInProcessing.containsKey("Aida")) {
                if(false) {
                    try {
                        processImagePartMyself(
                                neighbourLostDetails.getHeightOfImagePart(),
                                neighbourLostDetails.getLinePositionOfImagePart()
                        ); //// Change this
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    //trimit inapoi la discovery
                    Mat partOfImageThatNeedsProcessed = getImagePart(
                            imageThatNeedsToBeProcessed,
                            neighbourLostDetails.getLinePositionOfImagePart(),
                            neighbourLostDetails.getHeightNeededToBeProcessed()
                    );
                    try {
                        Payload errorProcessingImagePayload = createPayloadFromErrorProcessingImage(
                                partOfImageThatNeedsProcessed,
                                neighbourLostDetails.getLinePositionOfImagePart(),
                                getNode().getNeighbours().get(requestInitiatorEndpointId).getDeviceInitialInfo().getPublicKey(),
                                AESSecretKeyUsedForMessages
                        );
                        connectionsClient.sendPayload(requestInitiatorEndpointId, errorProcessingImagePayload);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                }
            } else {
                String availableNodeEndpointId = sortedEndpointsThatAreNotUsedInProcessing.get(0);
                //si trimit la availableNodeEndpointId
            }

           if (devicesUsedInProcessing.size() == 0) {
                sendHeartbeatTimer.cancel();
                verifyHeartbeatTimestamp.cancel();
            }
        }


        List<String> endpointsIds = new ArrayList<>(getNode().getNeighbours().keySet());
        sendDeviceNode(endpointsIds, new HashSet<>());

        updateAllDevicesTextView();
    }

    private void sendHeartbeatToRequestInitiator() {

        PublicKey publicKey = getNode().getNeighbours().get(requestInitiatorEndpointId).getDeviceInitialInfo().getPublicKey();
        Payload heartbeatPayload = null;
        try {
            heartbeatPayload = createHeartbeatPayload(publicKey, AESSecretKeyUsedForMessages);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        connectionsClient.sendPayload(requestInitiatorEndpointId, heartbeatPayload);
        System.out.println("Heartbeat trimis");
    }

    private void sendHeartbeatToRequestInitiatorAtFixedInterval() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                sendHeartbeatToRequestInitiator();
            }
        };
        sendHeartbeatTimer.scheduleAtFixedRate(task, 0, sendHeartbeatInterval);
    }

}
