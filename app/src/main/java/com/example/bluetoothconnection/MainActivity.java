package com.example.bluetoothconnection;

import static com.example.bluetoothconnection.opencv.ImageProcessing.convertInputStreamToMat;

import com.example.bluetoothconnection.communication.Advertise;
import com.example.bluetoothconnection.communication.Device;
import com.example.bluetoothconnection.communication.Discovery;
import com.example.bluetoothconnection.permissions.Permissions;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
    private Context context;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
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

        context = getApplicationContext();

        OpenCVLoader.initDebug();

        initializeConfigValues();

        // Initialize the Nearby Connections client
        connectionsClient = Nearby.getConnectionsClient(this); //may be deleted because it's used just here

        Permissions permissions = new Permissions(this);
        permissions.checkAllPermissions(); //// !!!! When it throws an error, where do you go to check the error?
        // Permission is already granted

        boolean isAdvertise = !Build.MODEL.equals("Pixel 7");
        try {
            device = isAdvertise ? new Advertise(this.context, this, connectionsClient) : new Discovery(this.context, this, connectionsClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            device.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if(isAdvertise) {
            // Start advertising the endpoint
            Toast.makeText(MainActivity.this, "Start adverting", Toast.LENGTH_SHORT).show();  ////////// !!!!!!! Refactor this IF. Maybe make method in class Advertise/Discovery
        } else {
            initializeUploadButton();
            // Start discovering nearby endpoints
            Toast.makeText(MainActivity.this, "Start discovery", Toast.LENGTH_SHORT).show();
        }
    }

    private float getDeviceFreeMemory(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);  //// Move this up. We don't need to initialize all the time. Just at the beginning
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }

    private void initializeConfigValues(){
        AppConfig.initialize(this);
        AppConfig.setShouldEncryptData(true);
        AppConfig.setShouldCreateHash(true);
    }
    ////!!!!!!!!!!!!!!!!Aida: nu putem muta asta in Discovery din moment ce luam context din MainActivity
    //si il pasam ca param pentru Device??????????????????
    private void initializeUploadButton(){
        ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Uri imageUri = result.getData().getData();

                        ((Discovery)device).setUrlPathImageFromGallery(getRealPathFromURI(imageUri));

                        ImageView imageView = findViewById(R.id.imageView);
                        imageView.setImageURI(imageUri);

                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            if (inputStream != null) {
                                Mat originalImage = convertInputStreamToMat(inputStream); ////////// Maybe find a simple solution for converting from Uri to mat (not extra step Inputstream)

                                ((Discovery)device).setMatImageFromGallery(originalImage);
                            }

                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Toast.makeText(MainActivity.this,"Picked imaged", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        Button uploadButton = findViewById(R.id.uploadPhoto);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
                resultLauncher.launch(intent);
                //sendMessage(imageFromGallery);
            }
        });
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String filePath = cursor.getString(column_index);
        cursor.close();
        return filePath;
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