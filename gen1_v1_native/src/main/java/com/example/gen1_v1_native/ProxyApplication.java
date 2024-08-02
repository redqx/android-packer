package com.example.gen1_v1_native;

import android.app.Application;
import android.content.Context;


public class ProxyApplication extends Application
{

    public static final String TAG="ithuiyilu";

    static {
        try{
            System.loadLibrary("gen1_v1_native");
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {

        super.attachBaseContext(base);

        try {
            loadApp(getClassLoader(),base);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public native void loadApp(ClassLoader clsLoader,Context base);

}
