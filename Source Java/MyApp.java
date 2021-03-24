package com.example.easywedding;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

/**
 * For settings safe persistence on offline mode for the DB.
 * Manifest updated accordingly.
 */
public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
