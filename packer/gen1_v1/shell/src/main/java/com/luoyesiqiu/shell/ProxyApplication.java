package com.luoyesiqiu.shell;

import com.luoyesiqiu.shell.util.FileUtils;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.luoyesiqiu.shell.util.LoadDexUtil;
import com.luoyesiqiu.shell.util.RefInvoke;

import java.io.File;
import java.lang.ref.WeakReference;
import dalvik.system.DexClassLoader;

/**
 * Created by redqx
 */
public class ProxyApplication extends Application {
    private static final String TAG = ProxyApplication.class.getSimpleName();
    private String org_Application_Name = "";
    private Application org_Application = null;

    //private Application org_app;
    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        Log.d(TAG, "[ProxyApplication] attachBaseContext");

        org_Application_Name = FileUtils.readAppName(this);//从asset/app_name读取原始的application名字

        String cache_path = getCacheDir().getPath();
        String dexzipName = "i11111i111.zip";
        String dexzip_loadpath = cache_path + "/" + dexzipName;

        //复制文件
        FileUtils.asset_copyfile(base, dexzipName, dexzip_loadpath);

        // 2), 加载附录的dex,然后new DexClassLoader
        //配置动态加载环境,获取主线程对象
        Object currentActivityThread = LoadDexUtil.getCurrentActivityThread();
        String packageName = base.getPackageName();//当前apk的包名
        //下面两句不是太理解
        ArrayMap mPackages = (ArrayMap) RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mPackages");
        WeakReference wr = (WeakReference) mPackages.get(packageName);
        ClassLoader mClassLoader = (ClassLoader) RefInvoke.getFieldOjbect("android.app.LoadedApk", wr.get(), "mClassLoader");
        //创建被加壳apk的DexClassLoader对象  加载apk内的类和本地代码（c/c++代码）

        //至少呢...在安卓14的模拟器上,DexClassLoader只能加载 0444属性的dex文件(只读)
        FileUtils.setFilePermission(dexzip_loadpath);
        DexClassLoader newDexLoader = new DexClassLoader(dexzip_loadpath, cache_path, base.getApplicationInfo().nativeLibraryDir, mClassLoader);
        RefInvoke.setFieldOjbect("android.app.LoadedApk", "mClassLoader", wr.get(), newDexLoader);

        org_Application = LoadDexUtil.makeApplication(org_Application_Name);//构造原Application

        //加载源程序的类
        //可有可无，只是测试看看有没有这个类
//        try {
//            newDexLoader.loadClass("com.example.gen1_v1_1.MainActivity");
//            Log.d("demo", "com.example.gen1_v1_1.MainActivity: 类加载成功");
//        } catch (ClassNotFoundException e) {
//            Log.d("demo", "com.example.gen1_v1_1.MainActivity: " + Log.getStackTraceString(e));
//        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "[ProxyApplication] onCreate");
        //create main Apk's Application and replace with it.
        LoadDexUtil.replaceAndRunMainApplication(org_Application);
    }

}