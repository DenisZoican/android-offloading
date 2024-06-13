package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Utils.Common.SERVICE_ID;
import static com.example.bluetoothconnection.communication.Utils.Common.createPayloadFromDeviceNode;
import static com.example.bluetoothconnection.communication.Utils.Common.createPayloadFromRequestMat;
import static com.example.bluetoothconnection.communication.Utils.Common.createPayloadFromResponseMat;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.generateAESKey;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.generateRSAKeyPair;
import static com.example.bluetoothconnection.communication.Utils.Encrypting.getEncryptedAuthenticationToken;
import static com.example.bluetoothconnection.opencv.ImageProcessing.convertImageToBitmap;
import static com.example.bluetoothconnection.opencv.ImageProcessing.getImagePart;
import static com.example.bluetoothconnection.opencv.ImageProcessing.replaceMat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.bluetoothconnection.R;
import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;
import com.example.bluetoothconnection.communication.Entities.DeviceUsedInProcessingDetails;
import com.example.bluetoothconnection.communication.Entities.ImagePartInterval;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Debug;
import android.util.Log;

import com.example.bluetoothconnection.communication.Entities.DeviceNode;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;


import org.opencv.core.Mat;

import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.SecretKey;

public abstract class Device {

    protected Mat imageThatNeedsToBeProcessed;
    // Key pair for signing and verifying messages
    protected final KeyPair keyPairUsedForAESSecretKEy;
    protected final SecretKey AESSecretKeyUsedForMessages;
    protected Activity activity;
    protected final ConnectionsClient connectionsClient;
    public Context context;
    protected Timer verifyHeartbeatTimestamp;
    protected int sendHeartbeatInterval = 3000; //ms
    protected int verifyHeartbeatInterval = sendHeartbeatInterval*2; //ms

    // protected Map<String, CommunicationDetails> devicesUsedInCurrentCommunicationDetails;

    private float batteryLevel;
    private double cpuUsage;
    private int cpuCores;
    private DeviceNode node;
    protected Map<String, DeviceNode> validNeighboursUsedInCurrentCommunication = new HashMap<>();

    protected Map<String, DeviceUsedInProcessingDetails> devicesUsedInProcessing = new HashMap<>();

    public Device(Context context, Activity activity, ConnectionsClient connectionsClient) throws Exception {
        this.activity = activity;
        this.connectionsClient = connectionsClient;
        this.context = context;
        this.node = new DeviceNode();

        keyPairUsedForAESSecretKEy = generateRSAKeyPair();
        AESSecretKeyUsedForMessages = generateAESKey();

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(batteryReceiver, filter);
    }

    public float getBatteryLevel() {
        return batteryLevel;
    }

    public KeyPair getKeyPairUsedForAESSecretKEy() {
        return keyPairUsedForAESSecretKEy;
    }
    public double getCpuUsage() {
        return cpuUsage;
    }
    public int getCpuCores() {
        return cpuCores;
    }
    public DeviceNode getNode() {
        return node;
    }

    abstract public void start() throws Exception;
    abstract public void disconnect();
    abstract public void destroy();

    protected void startAdvertise() throws Exception {
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();

        String authenticationTokenAsName = getEncryptedAuthenticationToken();
        connectionsClient.startAdvertising(authenticationTokenAsName, SERVICE_ID, getConnectionLifecycleCallback(), advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Toast.makeText(activity, "Started advertising", Toast.LENGTH_SHORT).show();
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Toast.makeText(activity, "Failed to advertise - "+e.toString(), Toast.LENGTH_SHORT).show();
                        });
    }

    protected void startDiscovery(){
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
        connectionsClient.startDiscovery(
                        SERVICE_ID, getEndpointDiscoveryCallback(), discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // We're discovering nearby endpoints!
                            Toast.makeText(activity, "We are discovering.", Toast.LENGTH_SHORT).show();
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We were unable to start discovering.
                            Toast.makeText(activity, "Failed to discover - "+e.toString(), Toast.LENGTH_SHORT).show();
                        });
    }

    protected EndpointDiscoveryCallback getEndpointDiscoveryCallback(){
        return null;
    }

    protected ConnectionLifecycleCallback getConnectionLifecycleCallback() {
        return null;
    }

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                //if(batteryLevel < 0.0) {
                // Retrieve battery level
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

                // Retrieve battery scale (maximum level)
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                // Calculate battery percentage
                batteryLevel = (level / (float) scale) * 100;
                updateCPUInfo();
            }
        }
    };

    private void updateCPUInfo() {
        // Get CPU usage info
        cpuUsage = calculateCpuUsage();
        //cpuUsageTextView.setText("CPU Usage: " + cpuUsage + "%");
        Log.d("CPU Usage: ",cpuUsage + "%");

        System.out.println("Aida CPU USAGE=" + cpuUsage);

        // Get CPU capacity
        cpuCores = Runtime.getRuntime().availableProcessors();
        //cpuCapacityTextView.setText("CPU Capacity: " + cpuCores + " cores");
        Log.d("CPU Capacity: ",cpuCores + " cores");
        System.out.println("Aida CPU Capacity=" + cpuCores);
    }
    public static double calculateCpuUsage() {
        double cpuUsage = -1.0;
        try {
            // Get total CPU time in nanoseconds
            long totalCpuTimeNs = Debug.threadCpuTimeNanos();

            // Delay for a short period
            Thread.sleep(1000);

            // Get total CPU time again after a short delay
            long totalCpuTimeNs2 = Debug.threadCpuTimeNanos();

            // Calculate elapsed time in milliseconds
            long elapsedTimeMs = 1000;

            // Calculate CPU usage percentage
            double cpuTimeDiffMs = (totalCpuTimeNs2 - totalCpuTimeNs) / 1000000.0; // Convert nanoseconds to milliseconds
            cpuUsage = (cpuTimeDiffMs / elapsedTimeMs) * 100.0;

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return cpuUsage;
    }

    ///////////// Send message only if we have public key. Add a check
    protected void sendRequestImageToSingleEndpoint(String endpointId, int imagePartHeight, int imagePartLinePosition) throws Exception {
        ///////////// Send message only if we have public key. Add a check
        /*if(devicesUsedInCurrentCommunicationDetails.containsKey(endpointId)) {
            CommunicationDetails communicationDetails = devicesUsedInCurrentCommunicationDetails.get(endpointId);
            communicationDetails.incrementFailedAttempts();
        } else {
            CommunicationDetails communicationDetails = new CommunicationDetails(imagePart);
            devicesUsedInCurrentCommunicationDetails.put(endpointId,communicationDetails);
        }*/

        //PublicKey endpointPublicKey = discoveredDevices.get(endpointId).getPublicKey();
        PublicKey endpointPublicKey = this.getNode().getNeighbours().get(endpointId).getDeviceInitialInfo().getPublicKey();

        DeviceNode neighbourTreeNode = validNeighboursUsedInCurrentCommunication.get(endpointId);

        Mat imagePart = getImagePart(imageThatNeedsToBeProcessed, imagePartLinePosition, imagePartHeight);

        Payload payload = createPayloadFromRequestMat(imagePart, imagePartLinePosition, neighbourTreeNode, endpointPublicKey, AESSecretKeyUsedForMessages);
        connectionsClient.sendPayload(endpointId, payload);
    }

    protected void sendResponseImagePartToSingleEndpoint(String endpointId, Mat processedImage, int linePositionForImagePart, String processorUniqueName) throws Exception {
        PublicKey endpointPublicKey = this.getNode().getNeighbours().get(endpointId).getDeviceInitialInfo().getPublicKey();

        Payload payload = createPayloadFromResponseMat(processedImage, linePositionForImagePart, processorUniqueName, endpointPublicKey, AESSecretKeyUsedForMessages);
        connectionsClient.sendPayload(endpointId, payload);
    }

    protected void sendDeviceNode(List<String> endpointIds, Set<String> visitedNodes) {
        /////////////should not send all neighbours, just calculate the weight and send it
        /////////////it should be enough
        DeviceNode node = getNode();
        DeviceInitialInfo deviceInitialInfo = new DeviceInitialInfo(keyPairUsedForAESSecretKEy.getPublic(),getBatteryLevel(),getCpuUsage(),getCpuCores());
        node.setDeviceInitialInfo(deviceInitialInfo);

        endpointIds.forEach(endpointId->{
            Payload payload = null;
            try {
                payload = createPayloadFromDeviceNode(node, endpointId, visitedNodes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            connectionsClient.sendPayload(endpointId, payload);
        });
    }

    protected void replacePartInImageFromGallery(Mat image, Mat imagePart, int linePositionForImagePart){
        replaceMat(image, imagePart, linePositionForImagePart);
        updateImageView(image);
    }

    protected  void updateImageView(Mat image){
        ImageView imageView = activity.findViewById(R.id.imageView);
        Bitmap receivedImageBitmap = convertImageToBitmap(image);
        imageView.setImageBitmap(receivedImageBitmap);
    }

    protected void heartbeatReceivedBehaviour(String endpointId) {
        Toast.makeText(activity, "Heartbeat received", Toast.LENGTH_SHORT).show();
        DeviceUsedInProcessingDetails deviceUsedInProcessingDetails = devicesUsedInProcessing.get(endpointId);

        System.out.println("Set heartbeat start");
        if(deviceUsedInProcessingDetails != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                LocalDateTime currentTimestamp = LocalDateTime.now();
                deviceUsedInProcessingDetails.setLastHeartbeatReceivedTimestamp(currentTimestamp);
                System.out.println("Set heartbeat "+endpointId+" - "+currentTimestamp.toString());

            }
        }
    }
    protected void verifyHeartbeatTimestamps() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    LocalDateTime currentTimestamp = LocalDateTime.now();
                    Set<String> lostEndpoints = new HashSet<>();
                    devicesUsedInProcessing.keySet().stream().filter(endpointId->{
                        return !endpointId.equals("Aida");
                    }).forEach(deviceUsedInProcessingEndpointId->{
                        LocalDateTime lastHeartbeatTimestamp = devicesUsedInProcessing.get(deviceUsedInProcessingEndpointId).getLastHeartbeatReceivedTimestamp();
                        Duration duration = Duration.between(lastHeartbeatTimestamp, currentTimestamp);
                        int durationInMillis = (int) duration.toMillis();

                        if(durationInMillis > verifyHeartbeatInterval) {
                            lostEndpoints.add(deviceUsedInProcessingEndpointId);
                        }
                    });
                    lostEndpoints.forEach(lostEndpointId->{
                        onEndpointLostBehaviour(lostEndpointId);
                    });
                }
            }
        };
        verifyHeartbeatTimestamp = new Timer();
        verifyHeartbeatTimestamp.scheduleAtFixedRate(task, verifyHeartbeatInterval, verifyHeartbeatInterval);
    }
    abstract protected void onEndpointLostBehaviour(String endpointId);

    protected List<ImagePartInterval> getRemainingImageParts(List<ImagePartInterval> processedImagePartsIntervals, int linePosition, int imageHeight) {
        short[] intervalArray = new short[imageHeight];
        processedImagePartsIntervals.forEach(processedImagePartsInterval->{
            intervalArray[processedImagePartsInterval.getStart() - linePosition] = 1;
            intervalArray[processedImagePartsInterval.getEnd() - linePosition] = -1;
        });
        List<ImagePartInterval> resultImagePartsIntervals = new ArrayList<>();

        short sum = 0;
        for(int i = 0; i < imageHeight; i++) {
            sum += intervalArray[i];
            if(sum == 0){
                int start = i;
                i++;
                while(sum == 0 && i < imageHeight) {
                    sum += intervalArray[i];
                    i++;
                }
                ImagePartInterval imagePartInterval = new ImagePartInterval(linePosition + start, linePosition + i - 1);
                resultImagePartsIntervals.add(imagePartInterval);
            }
        }

        return resultImagePartsIntervals;
    }
}
