package com.example.gen1_v1_1;

import static com.example.gen1_v1_1.util.LoadDexUtil.getCurrentActivityThread;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;

import com.example.gen1_v1_1.util.LoadDexUtil;
import com.example.gen1_v1_1.util.RefInvoke;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import dalvik.system.DexClassLoader;

public class ProxyApplication extends Application {

    private static final String APP_KEY = "APPLICATION_CLASS_NAME";

    private Application org_app;
    public String apkFileName;
    public String libPath;
    public String odexPath;

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);

        //在应用程序的数据存储目录下创建文件夹，具体路径为data/user/0/包名/app_payload_dex(怎么多了app_?)
        File odex = getDir("payload_odex", MODE_PRIVATE);
        //File lib = getDir("payload_lib", MODE_PRIVATE); //lib目录暂时不用,不涉及so文件的移动

        odexPath = odex.getAbsolutePath();
        libPath = base.getApplicationInfo().nativeLibraryDir;//当前运行apk的so路径

        String dexFilePath = odexPath+"/dex.zip";//含有dex的zip文件路径,是DexClassLoader的参数1

        try
        {
            //1),文件复制
            InputStream inputStream = base.getAssets().open("dex.zip");
            // 创建目标文件
            File targetFile = new File(odexPath, "dex.zip");
            // 将文件从 assets 目录复制到目录 A
            OutputStream outputStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            // 2), 加载附录的dex,然后new DexClassLoader
            //配置动态加载环境,获取主线程对象
            Object currentActivityThread = getCurrentActivityThread();
            String packageName = base.getPackageName();//当前apk的包名
            //下面两句不是太理解
            ArrayMap mPackages = (ArrayMap) RefInvoke.getFieldOjbect( "android.app.ActivityThread", currentActivityThread, "mPackages");
            WeakReference wr = (WeakReference) mPackages.get(packageName);
            ClassLoader mClassLoader = (ClassLoader) RefInvoke.getFieldOjbect("android.app.LoadedApk", wr.get(), "mClassLoader");
            //创建被加壳apk的DexClassLoader对象  加载apk内的类和本地代码（c/c++代码）
            DexClassLoader dLoader = new DexClassLoader(dexFilePath, odexPath, libPath, mClassLoader);
            //base.getClassLoader(); 是不是就等同于 (ClassLoader) RefInvoke.getFieldOjbect()? 有空验证下//?
            //把当前进程的DexClassLoader 设置成了被加壳apk的DexClassLoader  ----有点c++中进程环境的意思~~
            RefInvoke.setFieldOjbect("android.app.LoadedApk", "mClassLoader", wr.get(), dLoader);
            
            org_app = LoadDexUtil.makeApplication(getSrcApplicationClassNameFromXML());

            //加载源程序的类
            //可有可无，只是测试看看有没有这个类
            try{
                dLoader .loadClass("com.example.gen1_v1_1.MainActivity");
                Log.d("demo", "com.example.gen1_v1_1.MainActivity: 类加载成功");
            }catch (ClassNotFoundException e){
                Log.d("demo", "com.example.gen1_v1_1.MainActivity: " + Log.getStackTraceString(e));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    public void onCreate() {
        super.onCreate();

        //create main Apk's Application and replace with it.
        LoadDexUtil.replaceAndRunMainApplication(org_app);
    }
    /**
     * 获取原application的类名
     * @return 返回类名
     */
    private String getSrcApplicationClassNameFromXML()
    {
        try {
            ApplicationInfo ai = this.getPackageManager()
                    .getApplicationInfo(this.getPackageName(),
                            PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            if (bundle != null && bundle.containsKey(APP_KEY)) {
                return bundle.getString(APP_KEY);//className 是配置在xml文件中的。
            } else {
                return "";
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

}
