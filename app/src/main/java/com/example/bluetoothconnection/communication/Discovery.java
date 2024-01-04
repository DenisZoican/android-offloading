package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Common.SERVICE_ID;
import static com.example.bluetoothconnection.communication.Common.STRATEGY;
import static com.example.bluetoothconnection.communication.Common.convertMatToPayload;
import static com.example.bluetoothconnection.communication.Common.convertPayloadToMat;
import static com.example.bluetoothconnection.opencv.ImageProcessing.convertImageToBitmap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.ArraySet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothconnection.R;
import com.example.bluetoothconnection.opencv.ImageProcessing;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Discovery extends Device{
    public static final int PICK_IMAGE_REQUEST = 1;
    private Set<String> allDevicesIds = new ArraySet();
    private Set<String> sentMessageDeviceIds = new ArraySet<>();
    private Mat imageFromGallery;

    public Discovery(Activity activity, ConnectionsClient connectionsClient){
        super(activity, connectionsClient);
    }

    public void start() {
        activity.setContentView(R.layout.activity_discover_main);

        initializeUiElements();

        System.out.println("STTTTTTTTTTTTART DISC");
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startDiscovery(
                        SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // We're discovering nearby endpoints!
                            System.out.println("SUCCESS DISCOVERY");
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We were unable to start discovering.
                            System.out.println("FAILED DISCOVERY"+e.toString());
                        });
    }

    public void setImageFromGallery(Mat imageFromGallery){
        this.imageFromGallery = imageFromGallery;
    }

    public void sendEmptyMessage() {
        List<String> allDevicesArray = new ArrayList<>(allDevicesIds);
        int allDevicesArrayLength = allDevicesArray.size();

        for(int i=0;i<allDevicesArrayLength;i++){
            String deviceId = allDevicesArray.get(i);

            sentMessageDeviceIds.add(deviceId);
            connectionsClient.sendPayload(deviceId,Payload.fromBytes("Test".getBytes()));
        }

        System.out.println("Sent all");
    }

    public void sendMessage(Mat image) {
        List<Mat> divideImages = ImageProcessing.divideImages(image,allDevicesIds.size());
        List<String> allDevicesArray = new ArrayList<>(allDevicesIds);
        int allDevicesArrayLength = allDevicesArray.size();

        for(int i=0;i<allDevicesArrayLength;i++){
            String deviceId = allDevicesArray.get(i);
            Payload payload = convertMatToPayload(divideImages.get(i));

            sentMessageDeviceIds.add(deviceId);
            connectionsClient.sendPayload(deviceId,Payload.fromBytes("Test".getBytes()));
        }

        System.out.println("Sent all");
    }


    public void disconnect() {
        allDevicesIds.stream().forEach((deviceId)->{
            connectionsClient.disconnectFromEndpoint(deviceId);
        });
    }

    public void destroy() {
        connectionsClient.stopDiscovery();
    }
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    // We found an endpoint!
                    System.out.println("GRRRRRRRR We found endpoint " + endpointId);

                    // We request connections
                    connectionsClient.requestConnection(uniqueName, endpointId, connectionLifecycleCallback)
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
                    System.out.println("We lost endpoint "+endpointId);
                    //////// !!!!!!!!!! verify if it throws exception when list doesn't contain endpointId !!!!!!!!!!!
                    allDevicesIds.remove(endpointId);
                    updateAllDevicesTextView();
                }
            };

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    System.out.println("GRRRRRR INITIATED");
                    // Automatically accept the connection on both devices.
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        // We're connected!
                        System.out.println("GRRRRRR CONNECTED");

                        allDevicesIds.add(endpointId);
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

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            System.out.println("RECEIVER DISCOVERY");
            Toast.makeText(activity, "Received", Toast.LENGTH_SHORT).show();

            ImageView imageView = activity.findViewById(R.id.imageView);

            Mat receivedMat = convertPayloadToMat(payload);
            Bitmap receivedImageBitmap = convertImageToBitmap(receivedMat);
            imageView.setImageBitmap(receivedImageBitmap);

            // We received a payload!
            /*
            Mat receivedImage = Common.convertPayloadToMat(payload, 500, 500);

            sentMessageDeviceIds.remove(endpointId); ///// This may be a problem. IF we remove an id from different threads, we may have inconsticency.
            if(sentMessageDeviceIds.isEmpty()){
                System.out.println("We have all parts");
            }

            onPayloadReceivedCallbackFunction.accept(receivedImage);
            */

        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            // Payload transfer status updated.
            System.out.println("endpoint " + endpointId + update.toString());
        }
    };

    /////////////// UI Elements

    private void initializeUiElements(){
        initializeSendButton();
    }

    private void initializeSendButton(){
        Button sendButton = activity.findViewById(R.id.button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> allDevicesArray = new ArrayList<>(allDevicesIds);

                Mat resizedMat = new Mat(500, 500, CvType.CV_8UC4);
                Imgproc.resize(imageFromGallery, resizedMat, new Size(500, 500), 0, 0, Imgproc.INTER_LINEAR);

                Payload payload = convertMatToPayload(resizedMat);
                connectionsClient.sendPayload(allDevicesArray.get(0),payload);

                ImageView imageView = activity.findViewById(R.id.imageView);
                imageView.setImageBitmap(convertImageToBitmap(resizedMat));
                //sendMessage(imageFromGallery);
            }
        });
    }

    private void updateAllDevicesTextView(){
        TextView allDevicesTextView = activity.findViewById(R.id.allDevices);

        String allDevicesIdString = "";
        for (String s : allDevicesIds)
        {
            allDevicesIdString += s + "\t";
        }

        allDevicesTextView.setText(allDevicesIdString);
    }
}
