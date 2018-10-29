package com.edgeon.faiq.synchor;

import android.app.Application;

import io.realm.Realm;

public class ApplicationClass extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(getApplicationContext());


    }
}
