package com.example.bluetoothconnection.opencv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.TextView;

import com.example.bluetoothconnection.R;
import com.google.android.gms.nearby.connection.Payload;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessing {
    public  static Mat getImagePart(Mat image, int linePosition, int imagePartHeight){
        Rect roi = new Rect(0, linePosition, image.width(), imagePartHeight);
        return image.submat(roi);
    }

    public static Mat processImage(Mat originalImage, int delay){
        // Perform image processing operations using OpenCV on the imageMat
        // Convert the image to grayscale
        Mat grayscaleMat = new Mat();
        Imgproc.cvtColor(originalImage, grayscaleMat, Imgproc.COLOR_BGR2GRAY);

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return grayscaleMat;
    }

    /// destMat - CVU_4, subMat - CVU_1
    public static void replaceMat(Mat destMat, Mat subMat, int heightOffset){
        //Mat resultMat = destMat.clone();

        int destMatHeight = destMat.height();

        int subMatWidth = subMat.width();
        int subMatHeight = subMat.height();

        for(int i=heightOffset;i<heightOffset+subMatHeight && i<destMatHeight;i++){
            for(int j=0;j<subMatWidth;j++){
                double grayScaleValue = subMat.get(i - heightOffset, j)[0];
                double[] newPixel = new double[]{grayScaleValue, grayScaleValue, grayScaleValue, 255.0};

                destMat.put(i,j,newPixel);
            }
        }

        //return destMat;
    }

    public static double compareHistograms(Mat hist1, Mat hist2) {
        // Compare the histograms using correlation method
        return Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_CORREL);
    }

    public static Mat calculateHistogram(Mat img) {
        // Separate the image into its respective color channels (B, G, R)
        List<Mat> bgrPlanes = new ArrayList<>();
        Core.split(img, bgrPlanes);

        // Set histogram parameters
        MatOfInt histSize = new MatOfInt(256);
        MatOfFloat ranges = new MatOfFloat(0f, 256f);
        MatOfInt channels = new MatOfInt(0);

        // Calculate the histograms for each channel
        Mat bHist = new Mat();
        Mat gHist = new Mat();
        Mat rHist = new Mat();

        Imgproc.calcHist(bgrPlanes.subList(0, 1), channels, new Mat(), bHist, histSize, ranges, false);
        Imgproc.calcHist(bgrPlanes.subList(1, 2), channels, new Mat(), gHist, histSize, ranges, false);
        Imgproc.calcHist(bgrPlanes.subList(2, 3), channels, new Mat(), rHist, histSize, ranges, false);

        // Normalize the histograms
        Core.normalize(bHist, bHist, 0, 1, Core.NORM_MINMAX);
        Core.normalize(gHist, gHist, 0, 1, Core.NORM_MINMAX);
        Core.normalize(rHist, rHist, 0, 1, Core.NORM_MINMAX);

        // Concatenate the histograms into a single Mat
        List<Mat> histList = new ArrayList<>();
        histList.add(bHist);
        histList.add(gHist);
        histList.add(rHist);

        Mat hist = new Mat();
        Core.hconcat(histList, hist);

        return hist;
    }

    public static List<Mat> divideImages(Mat image, int divisionsCount){
        List<Mat> dividedImages = new ArrayList<Mat>();

        int imageWidth = image.width();
        int imageHeight = image.height();

        int subMatHeight = imageHeight / divisionsCount;

        for(int i=0;i<divisionsCount-1;i++){
                int startI = i * subMatHeight;
                int endI = (i+1) * subMatHeight;

                Range rowRange = new Range(startI, endI);
                Range colRange = new Range(0, imageWidth);

                Mat dividedMat = image.submat(rowRange, colRange);
                dividedImages.add(dividedMat);
        }

        int startI =  (divisionsCount - 1) * subMatHeight;

        Range rowRange = new Range(startI, imageHeight);
        Range colRange = new Range(0, imageWidth);

        Mat dividedMat = image.submat(rowRange, colRange);
        dividedImages.add(dividedMat);

        return dividedImages;
    }

    public static Mat convertInputStreamToMat(InputStream inputStream){
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        Mat image = new Mat();
        Utils.bitmapToMat(bitmap, image);

        return image;
    }

    public static Bitmap convertImageToBitmap(Mat image) {
        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap);

        return bitmap;
    }
}
