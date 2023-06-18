package com.example.bluetoothconnection;

import com.example.bluetoothconnection.communication.Advertise;
import com.example.bluetoothconnection.communication.Device;
import com.example.bluetoothconnection.communication.DeviceType;
import com.example.bluetoothconnection.communication.Discovery;
import com.example.bluetoothconnection.permissions.Permissions;

import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.*;

public class MainActivity extends AppCompatActivity {
      private ConnectionsClient connectionsClient;
      Device device;
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        System.out.println("GRRRRRRRRRRRR");
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
        System.out.println("GRRRRRRRRRRRR");
        // Initialize the Nearby Connections client
        connectionsClient = Nearby.getConnectionsClient(this); //may be deleted because it's used just here

        Permissions permissions = new Permissions(this);
        permissions.checkAllPermissions(); //// !!!! When it throws an error, where do you go to check the error?
        // Permission is already granted
        System.out.println("GRRRRRRRRRRRR  GRANTED");

        boolean isAdvertise = Build.BRAND.equals("google");
        device = isAdvertise ? new Advertise(connectionsClient) : new Discovery(connectionsClient);
        device.setOnPayloadReceivedCallbackFunction((message)-> {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
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

        Button myButton = findViewById(R.id.button);
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView insertedText = findViewById(R.id.editTextText);
                device.sendMessage(insertedText.getText().toString());
            }
        });
    }

    /*private void updateAllDevicesTextView(){
        TextView allDevicesTextView = findViewById(R.id.allDevices);

        String allDevicesIdString = "";
        for (String s : allDevicesIds)
        {
            allDevicesIdString += s + "\t";
        }

        allDevicesTextView.setText(allDevicesIdString);
    }*/

    private float getDeviceFreeMemory(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);  //// Move this up. We don't need to initialize all the time. Just at the beginning
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }

    // Call this method to send a message to the other endpoint.
   /* private void sendMessage(String message) {
        Payload payload = Payload.fromBytes(message.getBytes(StandardCharsets.UTF_8));
        connectionsClient.sendPayload(discoveryDeviceId, payload);
    }*/

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