package com.luoye.dpt;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class fake_ThisApplication extends Application
{
    private static final String TAG = fake_ThisApplication.class.getSimpleName();
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"dpt onCreate");
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.d(TAG,"dpt attachBaseContext");
    }
}
