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

    //如果ProxyComponentFactory得到了执行, ProxyApplication将不会执行,这些项目的原理相关配置.
    //默认ProxyComponentFactory执行,而ProxyApplication不执行
    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        Log.d(TAG,"[ProxyApplication] attachBaseContext => classloader = " + base.getClassLoader());

        realApplicationName = FileUtils.readAppName(this);//从asset/app_name读取原始的application名字
        if(!Global.sIsReplacedClassLoader)
        {
            ApplicationInfo applicationInfo = base.getApplicationInfo();
            if(applicationInfo == null)
            {
                throw new NullPointerException("application info is null");
            }
            FileUtils.unzipLibs(applicationInfo.sourceDir,applicationInfo.dataDir);//提取so文件
            JniBridge.loadShellLibs(applicationInfo.dataDir,applicationInfo.sourceDir);//加载so文件

            Log.d(TAG,"ProxyApplication init");
            JniBridge.init_app();//调用JniBridge.init_app(), 主要是读取asset目录下的OoooooOooo和i11111i111.zip
            ClassLoader targetClassLoader = base.getClassLoader();
            JniBridge.mergeDexElements(targetClassLoader);//把i11111i111.zip添加到DexElements中
            Global.sIsReplacedClassLoader = true;
        }
    }
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "[ProxyApplication] onCreate");
        replaceApplication();
    }

    private void replaceApplication()
    {
        if (Global.sNeedCalledApplication && !TextUtils.isEmpty(realApplicationName))
        {
            realApplication = (Application) JniBridge.replaceApplication(realApplicationName);
            Log.d(TAG, "applicationExchange: " + realApplicationName+"  realApplication="+realApplication.getClass().getName());

            JniBridge.callRealApplicationAttach(getApplicationContext(), realApplicationName);
            JniBridge.callRealApplicationOnCreate(realApplicationName);
            Global.sNeedCalledApplication = false;
        }
    }

    @Override
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
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
