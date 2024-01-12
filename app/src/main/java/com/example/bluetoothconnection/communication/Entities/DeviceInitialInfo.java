package com.example.bluetoothconnection.communication.Entities;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.security.PublicKey;

import javax.crypto.SecretKey;

public class DeviceInitialInfo implements Serializable {
    private final PublicKey publicKey;
    @Nullable
    private final int batteryPercentage;

    public DeviceInitialInfo(PublicKey publicKey, int batteryPercentage) {
        this.publicKey = publicKey;
        this.batteryPercentage = batteryPercentage;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public int getBatteryPercentage() {
        return batteryPercentage;
    }
}
