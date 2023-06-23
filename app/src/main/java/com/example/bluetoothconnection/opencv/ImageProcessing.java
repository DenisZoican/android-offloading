package com.example.bluetoothconnection.opencv;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ImageProcessing {
    public static Mat processImage(Mat originalImage){
        // Perform image processing operations using OpenCV on the imageMat
        // Convert the image to grayscale
        Mat grayscaleMat = new Mat();
        Imgproc.cvtColor(originalImage, grayscaleMat, Imgproc.COLOR_BGR2GRAY);

        return grayscaleMat;
    }

    public static List<Mat> divideImages(Mat image, int divisionsCount){
        List<Mat> dividedImages = new ArrayList<Mat>();

        int imageWidth = image.width();
        int imageHeight = image.height();

        int subMatHeight = imageHeight / divisionsCount;

        for(int i=0;i<divisionsCount-1;i++){
                int startI = i * subMatHeight;
                int endI = (i+1) * subMatHeight - 1;

                Range rowRange = new Range(startI, endI);
                Range colRange = new Range(0, imageWidth-1);

                Mat dividedMat = image.submat(rowRange, colRange);
                dividedImages.add(dividedMat);
        }

        int startI =  (divisionsCount - 1) * subMatHeight;

        Range rowRange = new Range(startI, imageHeight-1);
        Range colRange = new Range(0, imageWidth-1);

        Mat dividedMat = image.submat(rowRange, colRange);
        dividedImages.add(dividedMat);

        return dividedImages;
    }
}
