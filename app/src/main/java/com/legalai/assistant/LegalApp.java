package com.legalai.assistant;

import android.app.Application;

public class LegalApp extends Application {
    private static LegalApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static LegalApp getInstance() {
        return instance;
    }
}
