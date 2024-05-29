package com.example.bluetoothconnection.communication.Extern;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import okhttp3.OkHttpClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ExternCommunicationUtils {
    private static final String FOG_URL = "http://192.168.1.104:61984/Processing"; /// Must always change url and port
    private static final String CLOUD_URL = "https://image-processing-cloud.azurewebsites.net/Processing";

    private static final OkHttpClient CLOUD_CLIENT = new OkHttpClient();
    private static final OkHttpClient FOG_CLIENT = getUnsafeOkHttpClient();

    public static void uploadMat(Mat mat, Boolean isCloud, ExternUploadCallback externUploadCallback) {

        OkHttpClient usedClient = isCloud ? CLOUD_CLIENT : FOG_CLIENT;
        String usedUrl = isCloud ? CLOUD_URL : FOG_URL;

        // Convert Mat to byte array
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mat, buffer);
        byte[] byteArray = buffer.toArray();

        RequestBody fileBody = RequestBody.create(byteArray, MediaType.parse("image/png"));
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "image.png", fileBody)
                .build();

        Request request = new Request.Builder()
                .url(usedUrl)
                .post(requestBody)
                .build();

        // Execute the request
        usedClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle failure
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    byte[] imageBytes = response.body().bytes();
                    MatOfByte mob = new MatOfByte(imageBytes);
                    Mat processedMat =  Imgcodecs.imdecode(mob, Imgcodecs.IMREAD_UNCHANGED);
                    externUploadCallback.onSuccess(processedMat);
                } else {
                    String errorString = response.body().string();
                    // Handle error
                    externUploadCallback.onFailure("Upload failed: " + errorString);
                }
            }
        });
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Create an ssl socket factory with our all-trusting manager
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            OkHttpClient okHttpClient = builder.build();
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
