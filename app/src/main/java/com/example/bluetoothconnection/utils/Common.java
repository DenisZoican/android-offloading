package com.example.bluetoothconnection.utils;

import java.util.UUID;

public class Common {
    static public String getUniqueName() {
        // Return a unique name for the local user.
        return "user" + UUID.randomUUID().toString().substring(0, 8);
    }
}
