package com.example.bluetoothconnection.permissions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Permissions {
    private Context context;
    private Activity activity;

    public Permissions(Context context){
        this.context = context;
        this.activity = (Activity)context;
    }

    public static final int MY_PERMISSIONS_REQUEST_NEARBY_WIFI_DEVICES = 2929; //// !!!!!!!!! Maybe we can delete
    private final String[] ALL_PERMISSIONS = {
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.BATTERY_STATS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    ///// !!!!!!!!! Check if we need all of these. Don't forget to try on a new phone where are no permissions !!!!!!!!!!! ////////////
    /// Asking user to grant multiple permission. Should refactor. Too many if/else. Maybe some permissions are already granted and we shouldn't check

    final public void checkAllPermissions(){ ///// !!!!!! When using this method, we pass (this, this). Not right. Find a way to pass just one this
        String[] ungrantedPermissions = this.getUngrantedPermissions();
        ActivityCompat.requestPermissions(activity,
                ungrantedPermissions,
                Permissions.MY_PERMISSIONS_REQUEST_NEARBY_WIFI_DEVICES);
    }

    private String[] getUngrantedPermissions(){
        return Arrays.stream(ALL_PERMISSIONS).filter((permission)-> !_isPermissionGranted(permission)).toArray(size -> new String[size]);
    }

    private boolean _isPermissionGranted(String permission){
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }
}
