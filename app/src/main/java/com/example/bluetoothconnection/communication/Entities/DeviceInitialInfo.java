package com.example.bluetoothconnection.communication.Entities;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.security.PublicKey;

import javax.crypto.SecretKey;

public class DeviceInitialInfo implements Serializable {
    private final PublicKey publicKey;
    @Nullable
    private final float batteryPercentage;
    private double cpuUsage;
    private int cpuCores;

    public DeviceInitialInfo(PublicKey publicKey, float batteryPercentage, double cpuUsage, int cpuCores) {
        this.publicKey = publicKey;
        this.batteryPercentage = batteryPercentage;
        this.cpuUsage = cpuUsage;
        this.cpuCores = cpuCores;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public float getBatteryPercentage() {
        return batteryPercentage;
    }

    public double getCpuUsage() {   return cpuUsage; }

    public double getCpuCores() {   return cpuCores; }
}
