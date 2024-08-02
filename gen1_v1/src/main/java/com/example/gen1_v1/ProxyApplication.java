package com.example.gen1_v1;


import android.app.Application;
import android.content.Context;


import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import dalvik.system.InMemoryDexClassLoader;



public class ProxyApplication extends Application
{
    public static final String TAG="pack_it";

    //@RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        try
        {
            byte[] dex_data=getDex();
            String searchDir=this.getApplicationInfo().sourceDir+"!/lib/x86_64";
            ByteBuffer[] dexBuffer_Arr=getDexBuffers(dex_data);
            InMemoryDexClassLoader dexClassLoader= null;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) //特定版本才才可与使用的InMemoryDexClassLoader
            {
                dexClassLoader = new InMemoryDexClassLoader(
                        dexBuffer_Arr,
                        searchDir,
                        this.getClassLoader());
            }
            entryApp(dexClassLoader,this.getClassLoader(),base);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public byte[] getDex() throws IOException
    {
        InputStream is= this.getAssets().open("classes3.dex");
        if(is==null)
        {
            return null;
        }
        int flen=is.available();
        byte[] data=new byte[flen];
        is.read(data);
        is.close();

//        for(int i=0;i<flen;i++){
//            data[i]= (byte) (data[i]^48);
//        }

        return data;
    }

    public ByteBuffer[] getDexBuffers(byte[] dex_data)
    {
        ByteBuffer[] byteBuff_Arr = new ByteBuffer[1];
        byteBuff_Arr[0]=ByteBuffer.wrap(dex_data);
        return byteBuff_Arr;
    }

    public void entryApp(InMemoryDexClassLoader dexClassLoader,ClassLoader parent_classloader,Context base)
    {
        try {
            //base.(ContextImpl.mPackageInfo).(LoadedApk.ClassLoader)
            //把base.mPackageInfo.ClassLoader替换为我们的dex

            Class<?> ContextImpl = parent_classloader.loadClass("android.app.ContextImpl");
            Field f_mPackageInfo = ContextImpl.getDeclaredField("mPackageInfo");

            Class<?> LoadedApk = parent_classloader.loadClass("android.app.LoadedApk");
            Field f_mClassLoader = LoadedApk.getDeclaredField("mClassLoader");

            f_mClassLoader.setAccessible(true);
            f_mPackageInfo.setAccessible(true);

            Object obj_mPackageInfo = f_mPackageInfo.get(base);
            //Object obj_mClassLoader = f_mClassLoader.get(obj_mPackageInfo);

            f_mClassLoader.set(obj_mPackageInfo, dexClassLoader);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return ;
    }
    //public native void loadApp(ClassLoader clsLoader,Context base);
}
