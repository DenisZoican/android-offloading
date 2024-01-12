package com.example.bluetoothconnection.utils;

import android.util.Base64;

import com.example.bluetoothconnection.communication.Entities.DeviceInitialInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Common {
    public static String getStringBase64FromBytes(byte[] bytes){
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
    public static byte[] getBytesBase64FromString(String string){
        return Base64.decode(string, Base64.DEFAULT);
    }

    public static <E> byte[] serializeObject(E object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(object);
            return bos.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> T deserializeObject(byte[] serializedBytes, Class<T> clazz) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(serializedBytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {

            return clazz.cast(ois.readObject());

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] combineArrays(byte[] array1, byte[] array2) {
        byte[] combined = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, combined, 0, array1.length);
        System.arraycopy(array2, 0, combined, array1.length, array2.length);
        return combined;
    }

    private static byte[] serializeObject(Serializable object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(object);
            return bos.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
