package com.example.bluetoothconnection;

import static com.example.bluetoothconnection.opencv.ImageProcessing.convertImageToBitmap;
import static com.example.bluetoothconnection.opencv.ImageProcessing.convertInputStreamToMat;

import com.example.bluetoothconnection.communication.Advertise;
import com.example.bluetoothconnection.communication.Device;
import com.example.bluetoothconnection.communication.Discovery;
import com.example.bluetoothconnection.permissions.Permissions;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.*;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
      private ConnectionsClient connectionsClient;
      Device device;

    ImageView imageView;
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        System.out.println("GRRRRRRRRRRRR");
        /////////////////// !!!!!!!!!!!! SHOULD ADD HERE LOGIC BECAUSE HERE WE KNOW THAT WE HAVE PERMISSIONS
        if (requestCode == Permissions.MY_PERMISSIONS_REQUEST_NEARBY_WIFI_DEVICES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
            } else {
                // Permission is not granted, show a message or disable the feature that requires the permission
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("GRRRRRRRRRRRR OpenCV"+ OpenCVLoader.initDebug());
        System.out.println("GRRRRRRRRRRRR");
        // Initialize the Nearby Connections client
        connectionsClient = Nearby.getConnectionsClient(this); //may be deleted because it's used just here

        Permissions permissions = new Permissions(this);
        permissions.checkAllPermissions(); //// !!!! When it throws an error, where do you go to check the error?
        // Permission is already granted
        System.out.println("GRRRRRRRRRRRR  GRANTED");

        boolean isAdvertise = Build.MODEL.equals("Pixel 7");
        device = isAdvertise ? new Advertise(connectionsClient) : new Discovery(connectionsClient);
        device.setOnPayloadReceivedCallbackFunction((image)-> {
            Bitmap bitmap = convertImageToBitmap(image);

            imageView.setImageBitmap(bitmap);
        });

        if(isAdvertise) {
            // Start advertising the endpoint
            Toast.makeText(MainActivity.this, "Start adverting", Toast.LENGTH_SHORT).show();  ////////// !!!!!!! Refactor this IF. Maybe make method in class Advertise/Discovery
        } else {
            // Start discovering nearby endpoints
            Toast.makeText(MainActivity.this, "Start discovery", Toast.LENGTH_SHORT).show();
        }
        device.start();

        setContentView(R.layout.activity_main);

        initializeUiElements();
    }

    private float getDeviceFreeMemory(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);  //// Move this up. We don't need to initialize all the time. Just at the beginning
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }

    private void initializeUiElements(){
        initializeSendButton();
        initializeUploadPhotoButton();
    }

    private void initializeSendButton(){
        Button sendButton = findViewById(R.id.button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView insertedText = findViewById(R.id.editTextText);
                System.out.println(insertedText);
                //device.sendMessage(insertedText.getText().toString());
            }
        });
    }

    private void initializeUploadPhotoButton(){
        ActivityResultLauncher<Intent> processingImageLauncher = generateActivityLauncherForProcessedImage();

        Button uploadPhotoButton = findViewById(R.id.uploadPhoto);
        uploadPhotoButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            processingImageLauncher.launch(intent);
        });
    }
    private ActivityResultLauncher<Intent> generateActivityLauncherForProcessedImage(){
        imageView = findViewById(R.id.imageView);
        return registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();

                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            if (inputStream != null) {
                                // Read the image data and convert it to a Mat object
                                Mat originalImage = convertInputStreamToMat(inputStream);

                                // Convert the modified image back to a Bitmap for display or further use
                                // Bitmap processedBitmap = Bitmap.createBitmap(originalImage.cols(), originalImage.rows(), Bitmap.Config.ARGB_8888);
                                // Utils.matToBitmap(originalImage, processedBitmap);

                                device.sendMessage(originalImage);
                            }

                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("GRRRRRRRRRRRRRRRR Image URI"+result.getData());
                        // Handle the selected photo (e.g., upload it to a server)
                    }
                }
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        device.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        device.destroy();
    }

}