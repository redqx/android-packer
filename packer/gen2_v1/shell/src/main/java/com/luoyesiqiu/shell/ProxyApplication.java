package com.luoyesiqiu.shell;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import com.luoyesiqiu.shell.util.FileUtils;

/**
 * Created by luoyesiqiu
 */
public class ProxyApplication extends Application
{
    private static final String TAG = ProxyApplication.class.getSimpleName();
    private String realApplicationName = "";
    private Application realApplication = null;



    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        Log.d(TAG,"dpt attachBaseContext classloader = " + base.getClassLoader());

        //从(asset/app_acf)读取原始ApplicationName
        realApplicationName = FileUtils.readAppName(this);
        if(!Global.sIsReplacedClassLoader)
        {
            ApplicationInfo applicationInfo = base.getApplicationInfo();
            if(applicationInfo == null)
            {
                throw new NullPointerException("application info is null");
            }
            //从(asset/vwwwwwvwww)读取libdpt.so文件到指定路径/data/user/0/com.luoye.dpt/dpt-libs/x86_64
            FileUtils.unzipLibs(applicationInfo.sourceDir,applicationInfo.dataDir);

            //从指定路径加 加载libdpt.so文件,同时会触发init_array的函数执行, 还会加载原始apk自身的so
            JniBridge.loadShellLibs(applicationInfo.dataDir,applicationInfo.sourceDir);
            //init_array函数: 解密bitcode节, hook mmap, hook DefineClass. 同时调用一些保护策略(frida检测,fork+ptace,...)


            Log.d(TAG,"ProxyApplication init");
            JniBridge.ia();//执行JniBridge.init_app(), 主要是读取asset目录下的OoooooOooo和i11111i111.zip

            ClassLoader targetClassLoader = base.getClassLoader();
            JniBridge.mde(targetClassLoader);//把i11111i111.zip添加到dexElements列表中,用于后期dex的加载
            Global.sIsReplacedClassLoader = true;
        }
    }


    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "dpt onCreate");
        replaceApplication();//和一代壳类似的操作,涉及application的创建替换...
    }


    private void replaceApplication()
    {
        if (Global.sNeedCalledApplication && !TextUtils.isEmpty(realApplicationName)) {
            realApplication = (Application) JniBridge.ra(realApplicationName);
            Log.d(TAG, "applicationExchange: " + realApplicationName+"  realApplication="+realApplication.getClass().getName());

            JniBridge.craa(getApplicationContext(), realApplicationName);
            JniBridge.craoc(realApplicationName);
            Global.sNeedCalledApplication = false;
        }
    }


    @Override
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException
    {
        Log.d(TAG, "createPackageContext: " + realApplicationName);
        if(!TextUtils.isEmpty(realApplicationName)){
            replaceApplication();
            return realApplication;
        }
        return super.createPackageContext(packageName, flags);
    }

    @Override
    public String getPackageName() {
        if(!TextUtils.isEmpty(realApplicationName)){
            return "";
        }
        return super.getPackageName();
    }


}
