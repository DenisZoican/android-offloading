package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Utils.Common.createPayloadFromDeviceNode;
import static com.example.bluetoothconnection.communication.Utils.Common.createPayloadFromMat;
import static com.example.bluetoothconnection.communication.Utils.Common.extractDataFromPayload;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.checkAuthenticationToken;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.getEncryptedAuthenticationToken;
import static com.example.bluetoothconnection.opencv.ImageProcessing.convertImageToBitmap;
import static com.example.bluetoothconnection.opencv.ImageProcessing.replaceMat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothconnection.R;
import com.example.bluetoothconnection.communication.Extern.ExternCommunicationUtils;
import com.example.bluetoothconnection.communication.Extern.ExternUploadCallback;
import com.example.bluetoothconnection.communication.Entities.CommunicationDetails;
import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;
import com.example.bluetoothconnection.communication.Entities.DeviceNode;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadDeviceNodeData;
import com.example.bluetoothconnection.communication.PayloadDataEntities.PayloadMatData;
import com.example.bluetoothconnection.communication.Utils.Common;
import com.example.bluetoothconnection.opencv.ImageProcessing;
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

import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Discovery extends Device{
    //private Map<String,DeviceInitialInfo> discoveredDevices = new HashMap<>();
    //delete
    //private String batteryUsage = new String();
    private Map<String, CommunicationDetails> devicesUsedInCurrentCommunicationDetails;
    private Map<Integer, Mat> partsNeededFromImage;
    private Mat matImageFromGallery;

    protected EndpointDiscoveryCallback getEndpointDiscoveryCallback(){
        return new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
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
                case Image:
                    Integer imagePartIndex = devicesUsedInCurrentCommunicationDetails.get(endpointId).getImagePart();
                    try {
                        matReceivedBehavior((PayloadMatData)payloadData, endpointId, imagePartIndex);
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
                        sendImagePartToSingleEndpoint(endpointId, devicesUsedInCurrentCommunicationDetails.get(endpointId).getImagePart());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // Maximum retries reached, handle failure
                    devicesUsedInCurrentCommunicationDetails.remove(endpointId);
                    //image part should be sent to another endpoint
                }
            }
        }
    };
    private final int MAX_RETRIES = 10;

    public Discovery(Context context, Activity activity, ConnectionsClient connectionsClient) throws Exception {
        super(context, activity, connectionsClient);
    }

    public void start() {
        activity.setContentView(R.layout.activity_discover_main);
        initializeUiElements();

        startDiscovery();
    }

    public void setMatImageFromGallery(Mat matImageFromGallery){
        Mat resizedMat = new Mat(500, 500, CvType.CV_8UC4);
        Imgproc.resize(matImageFromGallery, resizedMat, new Size(500, 500), 0, 0, Imgproc.INTER_LINEAR);
        /////////////// MUST RESIZE. FIND MAX SIZE FOR PAYLOAD

        this.matImageFromGallery = resizedMat;
    }

    private void sendMessage(Mat image) throws Exception {
        /// Simulate multiple devices when we have only one
        int numberOfParts = 3;
        initializeImageValues(numberOfParts);

       /* ExternCommunicationUtils.uploadMat(matImageFromGallery, false, new ExternUploadCallback() {
            @Override
            public void onSuccess(Mat processedMat) {
                // Handle the processed Mat (e.g., display it in an ImageView)
                replacePartInImageFromGallery(processedMat, 0);
            }

            @Override
            public void onFailure(String errorMessage) {
                // Handle the error
                System.out.println(errorMessage);
            }
        });*/

        // Send image to another device
        //String endpointId = discoveredDevices.keySet().iterator().next();
        String endpointId = this.getNode().getNeighbours().keySet().iterator().next();
        sendImagePartToSingleEndpoint(endpointId, 0);

        /*
        //// Real dividing and sending images. Do not delete. Will be used in future
        List<Mat> divideImages = ImageProcessing.divideImages(imageFromGallery,allDevicesIds.size());
        List<String> allDevicesArray = new ArrayList<>(allDevicesIds);
        int allDevicesArrayLength = allDevicesArray.size();

        for(int i=0;i<allDevicesArrayLength;i++){
            String deviceId = allDevicesArray.get(i);
            Payload payload = convertMatToPayload(divideImages.get(i));

            devicesUsedInCurrentCommunication.put(deviceId, i);
            connectionsClient.sendPayload(deviceId, payload);
        }*/
    }

    ///////////// Send message only if we have public key. Add a check
    private void sendImagePartToSingleEndpoint(String endpointId, int imagePart) throws Exception {
        ///////////// Send message only if we have public key. Add a check
        if(devicesUsedInCurrentCommunicationDetails.containsKey(endpointId)) {
            CommunicationDetails communicationDetails = devicesUsedInCurrentCommunicationDetails.get(endpointId);
            communicationDetails.incrementFailedAttempts();
        } else {
            CommunicationDetails communicationDetails = new CommunicationDetails(imagePart);
            devicesUsedInCurrentCommunicationDetails.put(endpointId,communicationDetails);
        }

        //PublicKey endpointPublicKey = discoveredDevices.get(endpointId).getPublicKey();
        PublicKey endpointPublicKey = this.getNode().getNeighbours().get(endpointId).getDeviceInitialInfo().getPublicKey();

        Payload payload = createPayloadFromMat(partsNeededFromImage.get(imagePart), endpointPublicKey, AESSecretKeyUsedForMessages);
        connectionsClient.sendPayload(endpointId, payload);
    }

    private void initializeImageValues(int numberOfParts){
        List<Mat> divideImages = ImageProcessing.divideImages(matImageFromGallery,numberOfParts);
        this.partsNeededFromImage =  new HashMap<>();
        for(int i=0;i<numberOfParts;i++){
            this.partsNeededFromImage.put(i,divideImages.get(i));
        }

        this.devicesUsedInCurrentCommunicationDetails = new HashMap<>();
    }
    private void deviceNodeReceivedBehavior(PayloadDeviceNodeData payloadDeviceNodeData, String endpointId) {
        this.getNode().getNeighbours().put(endpointId, payloadDeviceNodeData.getDeviceNode());
        updateAllDevicesTextView();
    }

    private void matReceivedBehavior(PayloadMatData payloadMatData, String endpointId, int imagePartIndex) throws Exception {
        Mat receivedMat = payloadMatData.getImage();

        replacePartInImageFromGallery(receivedMat, imagePartIndex);

        devicesUsedInCurrentCommunicationDetails.remove(endpointId); ///// This may be a problem. IF we remove an id from different threads, we may have inconsticency.
        partsNeededFromImage.remove(imagePartIndex);

        if (!partsNeededFromImage.isEmpty()) {
            Integer firstNeededPartIndex = partsNeededFromImage.keySet().iterator().next();
            sendImagePartToSingleEndpoint(endpointId, firstNeededPartIndex);
        }
    }

    private void replacePartInImageFromGallery(Mat imagePart, int imagePartIndex){
        matImageFromGallery = replaceMat(matImageFromGallery, imagePart, imagePartIndex);
        ImageView imageView = activity.findViewById(R.id.imageView);
        Bitmap receivedImageBitmap = convertImageToBitmap(matImageFromGallery);
        imageView.setImageBitmap(receivedImageBitmap);
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
                try {
                    sendMessage(matImageFromGallery);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                ImageView imageView = activity.findViewById(R.id.imageView);
                imageView.setImageBitmap(convertImageToBitmap(matImageFromGallery));
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
