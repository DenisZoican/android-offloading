package com.example.bluetoothconnection.communication.Extern;

import org.opencv.core.Mat;

public interface ExternUploadCallback {
    void onSuccess(Mat processedMat);
    void onFailure(String errorMessage);
}