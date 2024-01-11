package com.example.bluetoothconnection.communication;

import static com.example.bluetoothconnection.communication.Common.SERVICE_ID;
import static com.example.bluetoothconnection.communication.Common.STRATEGY;
import static com.example.bluetoothconnection.communication.Common.convertMatToPayload;
import static com.example.bluetoothconnection.communication.Common.convertPayloadToMat;
import static com.example.bluetoothconnection.opencv.ImageProcessing.convertImageToBitmap;
import static com.example.bluetoothconnection.opencv.ImageProcessing.replaceMat;
import static com.example.bluetoothconnection.utils.EncryptionUtils.SECRET_AUTHENTICATION_TOKEN;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.ArraySet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothconnection.R;
import com.example.bluetoothconnection.opencv.ImageProcessing;
import com.example.bluetoothconnection.utils.EncryptionUtils;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Discovery extends Device{
    public static final int PICK_IMAGE_REQUEST = 1;
    private Set<String> allDevicesIds = new ArraySet();
    private Map<String,Integer> devicesUsedInCurrentCommunication;
    private Map<Integer, Mat> partsNeededFromImage;
    private Mat matImageFromGallery;
    private String urlPathImageFromGallery;

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

    public void setUrlPathImageFromGallery(String urlPathImageFromGallery){
        this.urlPathImageFromGallery = urlPathImageFromGallery;
    }

    public void setMatImageFromGallery(Mat matImageFromGallery){
        Mat resizedMat = new Mat(500, 500, CvType.CV_8UC4);
        Imgproc.resize(matImageFromGallery, resizedMat, new Size(500, 500), 0, 0, Imgproc.INTER_LINEAR);
        /////////////// MUST RESIZE. FIND MAX SIZE FOR PAYLOAD

        this.matImageFromGallery = resizedMat;
    }

    private void sendMessage(Mat image) {
        boolean useCloud = true;

        if(useCloud){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    uploadImageToAPI();
                }
            }).start();
            return;
        }
        /// Simulate multiple devices when we have only one
        int numberOfParts = 3;
        initializeImageValues(numberOfParts);

        List<String> allDevicesArray = new ArrayList<>(allDevicesIds);
        String endpointId = allDevicesArray.get(0);

        sendMessageToSingleEndpoint(endpointId, 0);

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

    private void sendMessageToSingleEndpoint(String endpointId, int imagePart){
        devicesUsedInCurrentCommunication.put(endpointId,imagePart);
        Payload payload = convertMatToPayload(partsNeededFromImage.get(imagePart));
        connectionsClient.sendPayload(endpointId, payload);
    }

    private void initializeImageValues(int numberOfParts){
        List<Mat> divideImages = ImageProcessing.divideImages(matImageFromGallery,numberOfParts);
        this.partsNeededFromImage =  new HashMap<>();
        for(int i=0;i<numberOfParts;i++){
            this.partsNeededFromImage.put(i,divideImages.get(i));
        }

        this.devicesUsedInCurrentCommunication = new HashMap<>();
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

                    System.out.println(info.getServiceId()); ////////// Check if we need to check this or if it is checked automaticall

                    String authenticationTokenAsName;
                    // We request connections
                    try {
                        authenticationTokenAsName = EncryptionUtils.encrypt(SECRET_AUTHENTICATION_TOKEN);
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
            Toast.makeText(activity, "Received", Toast.LENGTH_SHORT).show();

            Integer imagePartIndex = devicesUsedInCurrentCommunication.get(endpointId);
            Mat receivedMat = convertPayloadToMat(payload);

            System.out.println("Zoicanel RECEIVER DISCOVERY "+imagePartIndex);
            matImageFromGallery = replaceMat(matImageFromGallery, receivedMat, imagePartIndex);
            ImageView imageView = activity.findViewById(R.id.imageView); ////////// RECEIVES JUST 499/499/1
            Bitmap receivedImageBitmap = convertImageToBitmap(matImageFromGallery);
            imageView.setImageBitmap(receivedImageBitmap);

            devicesUsedInCurrentCommunication.remove(endpointId); ///// This may be a problem. IF we remove an id from different threads, we may have inconsticency.
            partsNeededFromImage.remove(imagePartIndex);

            if(!partsNeededFromImage.isEmpty()){
                Integer firstNeededPartIndex = partsNeededFromImage.keySet().iterator().next();
                System.out.println("Zoicanel"+firstNeededPartIndex);
                sendMessageToSingleEndpoint(endpointId,firstNeededPartIndex);
            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            // Payload transfer status updated.
            System.out.println("endpoint " + endpointId + update.toString());
        }
    };

    private void uploadImageToAPI() {
        try {
            File imageFile = new File(urlPathImageFromGallery);

            // Open a connection to the API endpoint
            URL url = new URL("http://localhost:5118/api/images/grayscale");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            // Set request headers if needed
            connection.setRequestProperty("Content-Type", "image/jpeg");

            // Write the image data to the request body
            OutputStream outputStream = connection.getOutputStream();
            FileInputStream fileInputStream = new FileInputStream(imageFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            fileInputStream.close();

            // Get the response from the server
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Toast.makeText(activity,"We received image from cloud",Toast.LENGTH_SHORT).show();
            } else {
                // Image processing failed
                // Handle the error
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                sendMessage(matImageFromGallery);

                ImageView imageView = activity.findViewById(R.id.imageView);
                imageView.setImageBitmap(convertImageToBitmap(matImageFromGallery));
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
