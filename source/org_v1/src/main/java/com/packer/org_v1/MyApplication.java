package com.packer.org_v1;

import android.app.Application;
import android.util.Log;

public class MyApplication extends Application
{
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("source", "[Application]=>i am source apk" );
    }
}