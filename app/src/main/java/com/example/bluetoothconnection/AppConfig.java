package com.example.bluetoothconnection;

import android.content.Context;
import android.content.SharedPreferences;

public class AppConfig {
    private static final String PREFS_NAME = "AppPrefs";

    private static final String SHOULD_ENCRYPT_DATA = "should-encrypt-data";

    private static SharedPreferences preferences = null;

    // Initialize the SharedPreferences instance
    public static void initialize(Context context) {
        if (preferences == null) {
            preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    public static void setShouldEncryptData(Boolean value) {
        preferences.edit().putString(SHOULD_ENCRYPT_DATA, value.toString()).apply();
    }

    public static String getShouldEncryptData() {
        return preferences.getString(SHOULD_ENCRYPT_DATA, null);
    }
}
