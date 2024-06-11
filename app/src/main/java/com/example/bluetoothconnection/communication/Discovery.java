package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Utils.Common.createPayloadFromDeviceNode;
import static com.example.bluetoothconnection.communication.Utils.Common.extractDataFromPayload;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.checkAuthenticationToken;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.getEncryptedAuthenticationToken;
import static com.example.bluetoothconnection.opencv.ImageProcessing.convertImageToBitmap;
import static com.example.bluetoothconnection.opencv.ImageProcessing.processImage;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothconnection.R;
import com.example.bluetoothconnection.communication.Entities.CommunicationDetails;
import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;
import com.example.bluetoothconnection.communication.Entities.DeviceNode;
import com.example.bluetoothconnection.communication.Extern.ExternCommunicationUtils;
import com.example.bluetoothconnection.communication.Extern.ExternUploadCallback;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadDeviceNodeData;
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

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class Discovery extends Device{
    private Mat matImageFromGallery;
    private static final int NEIGHBOUR_TRANSPORT_PENALTY = 1; // s/ms
    private final int MAX_RETRIES = 10;
    private Set<String> familiarNodesUniqueNames = new HashSet<>();
    private boolean hasConnectedToAdvertise = false;

    public Discovery(Context context, Activity activity, ConnectionsClient connectionsClient) throws Exception {
        super(context, activity, connectionsClient);
    }

    public void start() {
        activity.setContentView(R.layout.activity_discover_main);
        initializeUiElements();

        startDiscovery();
    }

    protected EndpointDiscoveryCallback getEndpointDiscoveryCallback(){
        return new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                if(hasConnectedToAdvertise) {
                    return;
                }
                // We found an endpoint!
                System.out.println(info.getServiceId()); ////////// Check if we need to check this or if it is checked automatically
                String authenticationTokenAsName = null;
                try {
                    authenticationTokenAsName = getEncryptedAuthenticationToken();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                connectionsClient.requestConnection(authenticationTokenAsName, endpointId, connectionLifecycleCallback)
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
                getNode().getNeighbours().remove(endpointId);
                updateAllDevicesTextView();
                //updateAllDevicesTextView();
            }
        };
    }

    public void disconnect() {
        /*this.getNode().getNeighbours().keySet().forEach((endpointId)->{
            connectionsClient.disconnectFromEndpoint(endpointId);
        });*/
        this.getNode().getNeighbours().keySet().forEach(connectionsClient::disconnectFromEndpoint);
    }

    public void destroy() {
        connectionsClient.stopDiscovery();
    }
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    System.out.println("GRRRRRR INITIATED");
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
                        // We're connected!
                        System.out.println("GRRRRRR CONNECTED");
                        hasConnectedToAdvertise = true;
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

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            Toast.makeText(activity, "Received", Toast.LENGTH_SHORT).show();

            boolean isDeviceInitialInfoPayload = !getNode().getNeighbours().containsKey(endpointId);
                                                //!discoveredDevices.containsKey(endpointId);

            PayloadData payloadData = null;
            try {
                payloadData = isDeviceInitialInfoPayload ? Common.extractDeviceNodeFromPayload(payload) : extractDataFromPayload(payload, keyPairUsedForAESSecretKEy.getPrivate());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            switch (payloadData.getMessageContentType()){
                case ResponseImage:
                    Integer imagePartIndex = devicesUsedInCurrentCommunicationDetails.get(endpointId).getImagePart();
                    try {
                        responseMatReceivedBehavior((PayloadResponseMatData)payloadData, endpointId, imagePartIndex);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case DeviceNode:
                    deviceNodeReceivedBehavior((PayloadDeviceNodeData)payloadData, endpointId);
                    break;
                case Error:
                    Toast.makeText(activity, "Hash didn't match", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            // Payload transfer status updated.
            // Handle payload transfer updates
            if(devicesUsedInCurrentCommunicationDetails == null) {
                return;
            }

            if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                // Payload transfer successful, remove from the list of failed payloads
                CommunicationDetails comDetails = devicesUsedInCurrentCommunicationDetails.get(endpointId);
                //////////////////////de ce intra de mai multe ori, de ce comDetails e null
                if(comDetails != null) {
                    comDetails.setFailedAttempts(0);
                }
            } else if (update.getStatus() == PayloadTransferUpdate.Status.FAILURE) {
                // Payload transfer failed, check if it's retriable
                int failedAttempts = devicesUsedInCurrentCommunicationDetails.get(endpointId).getFailedAttempts();

                if (failedAttempts < MAX_RETRIES) {
                    // Retry the payload
                    try {
                        sendRequestImagePartToSingleEndpoint(endpointId, devicesUsedInCurrentCommunicationDetails.get(endpointId).getImagePart());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // Maximum retries reached, handle failure
                    devicesUsedInCurrentCommunicationDetails.remove(endpointId);
                    getNode().getNeighbours().remove(endpointId);
                    //image part should be sent to another endpoint
                }
            }
        }
    };
    public void setMatImageFromGallery(Mat matImageFromGallery){
        Mat resizedMat = new Mat(500, 500, CvType.CV_8UC4);
        Imgproc.resize(matImageFromGallery, resizedMat, new Size(500, 500), 0, 0, Imgproc.INTER_LINEAR);
        /////////////// MUST RESIZE. FIND MAX SIZE FOR PAYLOAD

        this.matImageFromGallery = resizedMat;
    }

    private DeviceNode convertGraphToTree(DeviceNode root, Set<String> visitedNodes) {
        Queue<DeviceNode> originalsNodesQueue = new LinkedList<>();
        Queue<DeviceNode> treeNodesQueue = new LinkedList<>();
        DeviceNode rootCopy = root.createNodeCopyWithoutNeighbours();
        originalsNodesQueue.add(root);
        treeNodesQueue.add(rootCopy);
        visitedNodes.add(root.getUniqueName());

        while(!treeNodesQueue.isEmpty()) {
            DeviceNode originalRoot = originalsNodesQueue.poll();
            DeviceNode treeNodeRoot = treeNodesQueue.poll();

            originalRoot.getNeighbours().keySet().forEach(endpointId->{
                DeviceNode neighbour = originalRoot.getNeighbours().get(endpointId);
                if (!visitedNodes.contains(neighbour.getUniqueName())) {
                    DeviceNode neighbourCopy = neighbour.createNodeCopyWithoutNeighbours();
                    originalsNodesQueue.add(neighbour);
                    treeNodesQueue.add(neighbourCopy);
                    visitedNodes.add(neighbour.getUniqueName());
                    treeNodeRoot.getNeighbours().put(endpointId, neighbourCopy);
                }
            });
        }

        return rootCopy;
    }

    private void setNodesWeight(DeviceNode treeNode) {
        DeviceInitialInfo deviceInitialInfo = treeNode.getDeviceInitialInfo();
        double formula = (deviceInitialInfo.getCpuCores()*(1-deviceInitialInfo.getCpuUsage())/100)*deviceInitialInfo.getBatteryPercentage();
        if (familiarNodesUniqueNames.contains(treeNode.getUniqueName())) {
            formula +=1;
        }
        treeNode.setPersonalWeight(formula);
        if (treeNode.getNeighbours().isEmpty()) {
            treeNode.setTotalWeight(formula);
            return;
        }

        treeNode.getNeighbours().keySet().forEach(endpointId->{
            DeviceNode neighbour = treeNode.getNeighbours().get(endpointId);
            setNodesWeight(neighbour);
        });

        double neighboursWeightSum = treeNode.getNeighbours().values().stream().map(DeviceNode::getTotalWeight).reduce(0.0, Double::sum);

        double currentNodeWeight = formula + neighboursWeightSum - treeNode.getNeighbours().size()*NEIGHBOUR_TRANSPORT_PENALTY;
        treeNode.setTotalWeight(currentNodeWeight);

    }
    private void sendMessage() {
        if(getNode().getDeviceInitialInfo() == null) {
            DeviceInitialInfo deviceInitialInfo = new DeviceInitialInfo(keyPairUsedForAESSecretKEy.getPublic(),getBatteryLevel(),getCpuUsage(),getCpuCores());
            getNode().setDeviceInitialInfo(deviceInitialInfo);
        }
        Set<String> visitedNodes = new HashSet<>();
        DeviceNode treeNode = convertGraphToTree(getNode(), visitedNodes);
        setNodesWeight(treeNode);
        validNeighboursUsedInCurrentCommunication  = treeNode.getNeighbours().entrySet().stream().filter(entry-> entry.getValue().getTotalWeight() > 0.5)
                                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        /// Simulate multiple devices when we have only one
        int numberOfParts = validNeighboursUsedInCurrentCommunication.size();

        if(treeNode.getPersonalWeight() > 10000) {
            numberOfParts++;
        }

        if (numberOfParts > 0) {
            initializeImageValues(matImageFromGallery, numberOfParts);
            int index = 0;
            for (String endpointId : validNeighboursUsedInCurrentCommunication.keySet()) {
                try {
                    sendRequestImagePartToSingleEndpoint(endpointId, index);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                index ++;
            }

            if(validNeighboursUsedInCurrentCommunication.keySet().size() < numberOfParts){
                int imagePartIndex = validNeighboursUsedInCurrentCommunication.keySet().size();

                Mat partOfImageThatNeedsProcessed = partsNeededFromImage.get(imagePartIndex);
                Mat processedMat = processImage(partOfImageThatNeedsProcessed);

                replacePartInImageFromGallery(matImageFromGallery, processedMat, imagePartIndex);

                partsNeededFromImage.remove(imagePartIndex);
            }
        } else {
            ExternCommunicationUtils.uploadMat(matImageFromGallery, true, new ExternUploadCallback() {
                @Override
                public void onSuccess(Mat processedMat) {
                    // Handle the processed Mat (e.g., display it in an ImageView)
                    replacePartInImageFromGallery(matImageFromGallery, processedMat, 0);
                }
                @Override
                public void onFailure(String errorMessage) {
                    // Handle the error
                    System.out.println(errorMessage);
                }
            });
        }
    }

    private void deviceNodeReceivedBehavior(PayloadDeviceNodeData payloadDeviceNodeData, String endpointId) {
        //DeviceNode neighbourDeviceNode = payloadDeviceNodeData.getDeviceNode();
        //neighbourDeviceNode.getNeighbours().put()
        this.getNode().getNeighbours().put(endpointId, payloadDeviceNodeData.getDeviceNode());
        updateAllDevicesTextView();
    }

    private void responseMatReceivedBehavior(PayloadResponseMatData payloadResponseMatData, String endpointId, int imagePartIndex) throws Exception {
        Mat receivedMat = payloadResponseMatData.getImage();

        replacePartInImageFromGallery(matImageFromGallery, receivedMat, imagePartIndex);

        devicesUsedInCurrentCommunicationDetails.remove(endpointId); ///// This may be a problem. IF we remove an id from different threads, we may have inconsticency.
        partsNeededFromImage.remove(imagePartIndex);

        if (!partsNeededFromImage.isEmpty()) {
            Integer firstNeededPartIndex = partsNeededFromImage.keySet().iterator().next();
            sendRequestImagePartToSingleEndpoint(endpointId, firstNeededPartIndex);
        }
        familiarNodesUniqueNames.add(payloadResponseMatData.getProcessorNodeUniqueName());
    }

    /////////////// UI Elements

    private void initializeUiElements(){
        initializeSendButton();
    }

    private void initializeSendButton(){
        Button sendButton = activity.findViewById(R.id.button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageView imageView = activity.findViewById(R.id.imageView);
                //imageView.setImageBitmap(convertImageToBitmap(matImageFromGallery));
                try {
                    sendMessage();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void updateAllDevicesTextView(){
        TextView allDevicesTextView = activity.findViewById(R.id.allDevices);

        String allDevicesIdString = "";
        Map<String,DeviceNode> neighbours = this.getNode().getNeighbours();
        for (String endpointId : neighbours.keySet())
        {
            DeviceInitialInfo deviceInfo = neighbours.get(endpointId).getDeviceInitialInfo();
            float batteryPercentage = deviceInfo.getBatteryPercentage();
            allDevicesIdString += endpointId + " - Battery level: " + batteryPercentage + "%" + "\t";
        }

        allDevicesTextView.setText(allDevicesIdString);
    }

    private void sendDeviceNode(String endpointId) {
        DeviceNode node = getNode();
        DeviceInitialInfo deviceInitialInfo = new DeviceInitialInfo(keyPairUsedForAESSecretKEy.getPublic(),getBatteryLevel(),getCpuUsage(),getCpuCores());
        node.setDeviceInitialInfo(deviceInitialInfo);
        try {
            Payload payload = createPayloadFromDeviceNode(node);
            connectionsClient.sendPayload(endpointId, payload);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
