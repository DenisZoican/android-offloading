package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Utils.Common.extractDataFromPayload;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.checkAuthenticationToken;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.getEncryptedAuthenticationToken;
import static com.example.bluetoothconnection.opencv.ImageProcessing.getImagePart;
import static com.example.bluetoothconnection.opencv.ImageProcessing.processImage;
import static com.example.bluetoothconnection.opencv.ImageProcessing.replaceMat;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.bluetoothconnection.R;
import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;
import com.example.bluetoothconnection.communication.Entities.DeviceNode;
import com.example.bluetoothconnection.communication.Entities.DeviceUsedInProcessingDetails;
import com.example.bluetoothconnection.communication.Entities.ImagePartInterval;
import com.example.bluetoothconnection.communication.Extern.ExternCommunicationUtils;
import com.example.bluetoothconnection.communication.Extern.ExternUploadCallback;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadDeviceNodeData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadErrorProcessingMat;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;


public class Discovery extends Device{
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
                if(hasConnectedToAdvertise){
                    return;
                }

                //just to test; helps connect to a single device so that other device can connect to the other one and create a graph
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
                onEndpointLostBehaviour(endpointId);
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
                        List<String> foundNeighbour = new ArrayList<>();
                        foundNeighbour.add(endpointId);

                        Set<String> visitedNodes = new HashSet<>();
                        visitedNodes.add(endpointId);

                        hasConnectedToAdvertise = true;

                        sendDeviceNode(foundNeighbour,visitedNodes);
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

                boolean isDeviceNode = (payload.asBytes()[0] == 1);

                PayloadData payloadData = null;
                try {
                    payloadData = isDeviceNode ? Common.extractDeviceNodeFromPayload(payload) : extractDataFromPayload(payload, keyPairUsedForAESSecretKEy.getPrivate());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            switch (payloadData.getMessageContentType()){
                case ResponseImage:
                    responseMatReceivedBehavior((PayloadResponseMatData)payloadData, endpointId);
                    break;
                case ErrorProcessingImage:
                    errorProcessingImageReceivedBehaviour((PayloadErrorProcessingMat) payloadData);
                    break;
                case DeviceNode:
                    deviceNodeReceivedBehavior((PayloadDeviceNodeData)payloadData, endpointId);
                    break;
                case Heartbeat:
                    heartbeatReceivedBehaviour(endpointId);
                    break;
                case Error:
                    Toast.makeText(activity, "Hash didn't match", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

        }

        /*@Override
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
                        sendRequestImagePartToSingleEndpoint(endpointId, devicesUsedInCurrentCommunicationDetails.get(endpointId).getImagePart(), 0);
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
        }*/
    };
    public void setMatImageFromGallery(Mat matImageFromGallery){
        Mat resizedMat = new Mat(500, 500, CvType.CV_8UC4);
        Imgproc.resize(matImageFromGallery, resizedMat, new Size(500, 500), 0, 0, Imgproc.INTER_LINEAR);
        /////////////// MUST RESIZE. FIND MAX SIZE FOR PAYLOAD

        this.imageThatNeedsToBeProcessed = resizedMat;
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
        devicesUsedInProcessing = new HashMap();

        double totalValidNodesWeight = validNeighboursUsedInCurrentCommunication.values().stream()
                .map(DeviceNode::getTotalWeight)
                .reduce(0.0, Double::sum);

        double personalWeight = treeNode.getPersonalWeight();
        if(personalWeight != 0){
            totalValidNodesWeight += personalWeight;
        }

        if(totalValidNodesWeight > 0){
            int linePosition = 0;
            int imageHeight = imageThatNeedsToBeProcessed.height();

            for (String neighbourEndpointId : validNeighboursUsedInCurrentCommunication.keySet()) {
                double neighbourTotalWeight = validNeighboursUsedInCurrentCommunication.get(neighbourEndpointId).getTotalWeight();
                double percentageOfImageToBeProcessed = neighbourTotalWeight * 100 / totalValidNodesWeight;

                int heightOfImagePartThatNeedsToBeProcessed = (int) (percentageOfImageToBeProcessed * imageHeight / 100);

                DeviceUsedInProcessingDetails deviceUsedInProcessingDetails = new DeviceUsedInProcessingDetails(heightOfImagePartThatNeedsToBeProcessed,linePosition);
                this.devicesUsedInProcessing.put(neighbourEndpointId, deviceUsedInProcessingDetails);

                try {
                    sendRequestImageToSingleEndpoint(neighbourEndpointId, heightOfImagePartThatNeedsToBeProcessed, linePosition);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                linePosition+=heightOfImagePartThatNeedsToBeProcessed;
            }
            if(linePosition < imageHeight){
                int heightOfImagePart = imageHeight - linePosition;

                processImagePartMyself(heightOfImagePart, linePosition);
            }

            verifyHeartbeatTimestamps();
        } else {
            ExternCommunicationUtils.uploadMat(imageThatNeedsToBeProcessed, true, new ExternUploadCallback() {
                @Override
                public void onSuccess(Mat processedMat) {
                    // Handle the processed Mat (e.g., display it in an ImageView)
                    replacePartInImageFromGallery(imageThatNeedsToBeProcessed, processedMat, 0);
                }
                @Override
                public void onFailure(String errorMessage) {
                    // Handle the error
                    System.out.println(errorMessage);
                }
            });
        }
    }

    private void processImagePartMyself(int imagePartHeight, int imagePartLinePosition) {
        Handler handler = new Handler(Looper.getMainLooper());

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {

                DeviceUsedInProcessingDetails deviceUsedInProcessingDetails = new DeviceUsedInProcessingDetails(imagePartHeight, imagePartLinePosition);
                devicesUsedInProcessing.put("Aida", deviceUsedInProcessingDetails);

                Mat partOfImageThatNeedsProcessed = getImagePart(imageThatNeedsToBeProcessed, imagePartLinePosition, imagePartHeight);
                //Toast.makeText(activity, "Processing", Toast.LENGTH_SHORT).show();
                Mat processedMat = processImage(partOfImageThatNeedsProcessed, 10000);
                //Toast.makeText(activity, "NOT Processing", Toast.LENGTH_SHORT).show();

                replaceMat(imageThatNeedsToBeProcessed, processedMat, imagePartLinePosition);
                // replacePartInImageFromGallery(imageThatNeedsToBeProcessed, processedMat, imagePartLinePosition);

                devicesUsedInProcessing.remove("Aida");

                if (devicesUsedInProcessing.size() == 0) {
                    verifyHeartbeatTimestamp.cancel();
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Update your view here
                        updateImageView(imageThatNeedsToBeProcessed);
                    }
                });

            }
        });
        t1.start();
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

    private void responseMatReceivedBehavior(PayloadResponseMatData payloadResponseMatData, String endpointId){
        DeviceUsedInProcessingDetails deviceUsedInProcessingDetails = devicesUsedInProcessing.get(endpointId);

        if(deviceUsedInProcessingDetails != null) {
            int remainedHeightToBeProcessed = deviceUsedInProcessingDetails.getHeightNeededToBeProcessed() - payloadResponseMatData.getImage().height() - 1; //TO DO POSSIBLE PROBLEM: we dont divide the images correctly sin some cases, we receive one extra line
            deviceUsedInProcessingDetails.setHeightNeededToBeProcessed(remainedHeightToBeProcessed);

            int imagePartIntervalStart = payloadResponseMatData.getLinePosition();
            int imagePartIntervalEnd =payloadResponseMatData.getLinePosition()+payloadResponseMatData.getImage().height();
            ImagePartInterval imagePartInterval = new ImagePartInterval(imagePartIntervalStart, imagePartIntervalEnd);
            deviceUsedInProcessingDetails.getProcessedImagePartsInterval().add(imagePartInterval);

            if (remainedHeightToBeProcessed <= 0) {
                devicesUsedInProcessing.remove(endpointId);

                if (devicesUsedInProcessing.size() == 0) {
                    verifyHeartbeatTimestamp.cancel();
                }
            }
        }
        Mat receivedMat = payloadResponseMatData.getImage();

        replacePartInImageFromGallery(imageThatNeedsToBeProcessed, receivedMat, payloadResponseMatData.getLinePosition());

        //devicesUsedInCurrentCommunicationDetails.remove(endpointId); ///// This may be a problem. IF we remove an id from different threads, we may have inconsticency.
        //partsNeededFromImage.remove(imagePartIndex);

        /*if (!partsNeededFromImage.isEmpty()) {
            Integer firstNeededPartIndex = partsNeededFromImage.keySet().iterator().next();
            sendRequestImagePartToSingleEndpoint(endpointId, firstNeededPartIndex, 0);
        }*/
        familiarNodesUniqueNames.add(payloadResponseMatData.getProcessorNodeUniqueName());
    }

    private void errorProcessingImageReceivedBehaviour(PayloadErrorProcessingMat payloadErrorProcessingMat){
        Mat partOfImageThatNeedsProcessed = payloadErrorProcessingMat.getImage();

        Toast.makeText(activity, "Processing", Toast.LENGTH_SHORT).show();
        Mat processedMat = processImage(partOfImageThatNeedsProcessed, 10000);
        Toast.makeText(activity, "NOT Processing", Toast.LENGTH_SHORT).show();

        replacePartInImageFromGallery(imageThatNeedsToBeProcessed, processedMat, payloadErrorProcessingMat.getLinePosition());
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
    protected void onEndpointLostBehaviour(String endpointId) {
        DeviceUsedInProcessingDetails neighbourLostDetails = devicesUsedInProcessing.get(endpointId);

        getNode().getNeighbours().remove(endpointId);
        devicesUsedInProcessing.remove(endpointId);
        validNeighboursUsedInCurrentCommunication.remove(endpointId);

        if(neighbourLostDetails != null) {
            List<ImagePartInterval> remainingImagePartsToBeProcessed = getRemainingImageParts(neighbourLostDetails.getProcessedImagePartsInterval(),
                    neighbourLostDetails.getLinePositionOfImagePart(),
                    neighbourLostDetails.getHeightOfImagePart());

            remainingImagePartsToBeProcessed.forEach(processedImagePartsInterval->{

                List<String> sortedEndpointsThatAreNotUsedInProcessing = validNeighboursUsedInCurrentCommunication.entrySet().stream()
                        .filter(entry-> !devicesUsedInProcessing.containsKey(entry.getKey()))
                        .sorted((previous, current)-> (int) (current.getValue().getTotalWeight() - previous.getValue().getTotalWeight()))
                        .map(entry->entry.getKey()).collect(Collectors.toList());

                if(sortedEndpointsThatAreNotUsedInProcessing.size() == 0) {
                    processImagePartMyself(processedImagePartsInterval.getEnd()-processedImagePartsInterval.getStart(), processedImagePartsInterval.getStart());
                } else {
                    String availableNodeEndpointId = sortedEndpointsThatAreNotUsedInProcessing.get(0);

                    DeviceUsedInProcessingDetails deviceUsedInProcessingDetails = new DeviceUsedInProcessingDetails(
                            processedImagePartsInterval.getEnd()-processedImagePartsInterval.getStart(),
                            processedImagePartsInterval.getStart()
                    );
                    this.devicesUsedInProcessing.put(availableNodeEndpointId, deviceUsedInProcessingDetails);

                    try {
                        sendRequestImageToSingleEndpoint(availableNodeEndpointId,
                                processedImagePartsInterval.getEnd()-processedImagePartsInterval.getStart(),
                                processedImagePartsInterval.getStart());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            if (devicesUsedInProcessing.size() == 0) {
                verifyHeartbeatTimestamp.cancel();
            }

            List<String> endpointsIds = new ArrayList<>(getNode().getNeighbours().keySet());
            sendDeviceNode(endpointsIds, new HashSet<>());

            updateAllDevicesTextView();
        }
    }

    private void updateAllDevicesTextView(){
        TextView allDevicesTextView = activity.findViewById(R.id.allDevices);

        String allDevicesIdString = "";
        Map<String,DeviceNode> neighbours = this.getNode().getNeighbours();
        for (String endpointId : neighbours.keySet())
        {
            DeviceInitialInfo deviceInfo = neighbours.get(endpointId).getDeviceInitialInfo();
            float batteryPercentage = deviceInfo.getBatteryPercentage();
            DeviceNode neighbour = neighbours.get(endpointId);
            allDevicesIdString += endpointId + " - Battery level: " + batteryPercentage + "% " + neighbour.getNeighbours().size() + "\t";
        }

        allDevicesTextView.setText(allDevicesIdString);
    }

}
