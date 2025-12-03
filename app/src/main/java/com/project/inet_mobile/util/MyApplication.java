package com.project.inet_mobile.util;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

import com.project.inet_mobile.data.remote.SupabaseApiClient;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Force light theme and prevent dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Initialize SupabaseApiClient
        SupabaseApiClient.init(this); // Initialize the Supabase API client here
    }
}