package com.example.by_java_packapk_v_1;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import dalvik.system.DexClassLoader;

public class ProxyApplication extends Application
{
    private static final String appkey = "APPLICATION_CLASS_NAME";
    private String apkFileName;
    private String odexPath;
    private String libPath;
    private Context f_context = null;
    //这是context 赋值
    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        f_context=base;
//        try {
//            //创建两个文件夹payload_odex，payload_lib 私有的，可写的文件目录
//            File odex = this.getDir("payload_odex", MODE_PRIVATE);
//            File libs = this.getDir("payload_lib", MODE_PRIVATE);
//            odexPath = odex.getAbsolutePath();
//            libPath = libs.getAbsolutePath();
//            apkFileName = odex.getAbsolutePath() + "/payload.apk";
//            File apkFile = new File(apkFileName);
//            Log.i("demo", "apk size:"+apkFile.length());
//            if (!apkFile.exists())
//            {
//                //dexFile.createNewFile();  //在payload_odex文件夹内，创建payload.apk
//                // 读取程序classes.dex文件
//                //byte[] dexdata = this.readDexFileFromApk();
//                // 分离出解壳后的apk文件已用于动态加载
//                //this.splitPayLoadFromDex(dexdata);
//                this.splitPayLoadFromDex();//把数据写入payload.apk
//            }
//            // 配置动态加载环境
//            Object currentActivityThread = RefInvoke.invokeStaticMethod("android.app.ActivityThread", "currentActivityThread", new Class[] {}, new Object[] {});//获取主线程对象
//            String packageName = this.getPackageName();//当前apk的包名
//
//            ArrayMap mPackages = (ArrayMap) RefInvoke.getFieldOjbect(
//                    "android.app.ActivityThread", currentActivityThread,
//                    "mPackages");
//
//            WeakReference wr = (WeakReference) mPackages.get(packageName);
//
//            //创建被加壳apk的DexClassLoader对象  加载apk内的类和本地代码（c/c++代码）
//            DexClassLoader dLoader = new DexClassLoader(
//                    apkFileName, odexPath, libPath,
//                    (ClassLoader) RefInvoke.getFieldOjbect("android.app.LoadedApk", wr.get(), "mClassLoader")
//            );
//            //base.getClassLoader(); 是不是就等同于 (ClassLoader) RefInvoke.getFieldOjbect()? 有空验证下//?
//            //把当前进程的DexClassLoader 设置成了被加壳apk的DexClassLoader  ----有点c++中进程环境的意思~~
//
//            RefInvoke.setFieldOjbect(
//                    "android.app.LoadedApk",
//                    "mClassLoader",
//                    wr.get(), dLoader);
//
//            Log.i("demo","classloader:"+dLoader);
//
//            try
//            {
//                //先尝试加载一下payload.apk的MainActivity
//                Class actObj = dLoader.loadClass("com.example.pack.MainActivity");//这里怎么感觉有是写死了的
//                Log.i("demo", "actObj:"+actObj);
//                Intent intent = new Intent(ProxyApplication.this, actObj);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
//            }
//            catch(Exception e)
//            {
//                Log.e("demo", "activity:"+Log.getStackTraceString(e));
//            }
//
//
//        } catch (Exception e)
//        {
//            Log.e("demo", "error:"+Log.getStackTraceString(e));
//            e.printStackTrace();
//        }
        ClassLoader pathClassLoader = MainActivity.class.getClassLoader();
        try {
            //创建两个文件夹payload_odex，payload_lib 私有的，可写的文件目录
            File odex = this.getDir("payload_odex", MODE_PRIVATE);
            File libs = this.getDir("payload_lib", MODE_PRIVATE);
            odexPath = odex.getAbsolutePath();
            libPath = libs.getAbsolutePath();
            apkFileName = odex.getAbsolutePath() + "/payload.apk";
            File apkFile = new File(apkFileName);
            Log.i("demo", "apk size:"+apkFile.length());
            if (!apkFile.exists())
            {
                //dexFile.createNewFile();  //在payload_odex文件夹内，创建payload.apk
                // 读取程序classes.dex文件
                //byte[] dexdata = this.readDexFileFromApk();
                // 分离出解壳后的apk文件已用于动态加载
                //this.splitPayLoadFromDex(dexdata);
                this.splitPayLoadFromDex();//把数据写入payload.apk
            }
            // apkFileName, odexPath, libPath,

            DexClassLoader dexClassLoader = new DexClassLoader(apkFileName, odexPath, libPath, ProxyApplication.class.getClassLoader());

            //1.获取ActivityThread实例
            Class ActivityThread = pathClassLoader.loadClass("android.app.ActivityThread");
            Method currentActivityThread = ActivityThread.getDeclaredMethod("currentActivityThread");
            Object activityThreadObj = currentActivityThread.invoke(null);
            //2.通过反射获得类加载器
            //final ArrayMap<String, WeakReference<LoadedApk>> mPackages = new ArrayMap<>();
            Field mPackagesField = ActivityThread.getDeclaredField("mPackages");
            mPackagesField.setAccessible(true);
            //3.拿到LoadedApk
            ArrayMap mPackagesObj = (ArrayMap) mPackagesField.get(activityThreadObj);
            String packagename = this.getPackageName();
            WeakReference wr = (WeakReference) mPackagesObj.get(packagename);
            Object LoadApkObj = wr.get();
            //4.拿到mclassLoader
            Class LoadedApkClass = pathClassLoader.loadClass("android.app.LoadedApk");
            Field mClassLoaderField = LoadedApkClass.getDeclaredField("mClassLoader");
            mClassLoaderField.setAccessible(true);
            Object mClassLoader =mClassLoaderField.get(LoadApkObj);
            Log.e("mClassLoader",mClassLoader.toString());
            //5.将系统组件ClassLoader给替换
            mClassLoaderField.set(LoadApkObj,dexClassLoader);


            //先尝试加载一下payload.apk的MainActivity
//            Class actObj = dexClassLoader.loadClass("com.example.pack.MainActivity");//这里怎么感觉有是写死了的
//            Log.i("demo", "actObj:"+actObj);
//            Intent intent = new Intent(ProxyApplication.this, actObj);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(intent);

        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //@Override
    @SuppressWarnings("rawtypes")
    public void onCreate()
    {
        super.onCreate();

        //this.loadResources(apkFileName);// 加载资源

        Log.i("demo", "onCreate");
        // 如果源应用配置有Appliction对象，则替换为源应用Applicaiton，以便不影响源程序逻辑。
        String appClassName = null;
        try
        {
            ApplicationInfo ai = this.getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            if (bundle != null && bundle.containsKey("APPLICATION_CLASS_NAME"))
            {
                appClassName = bundle.getString("APPLICATION_CLASS_NAME");//className 是配置在xml文件中的。
            }
            else
            {
                Log.i("demo", "have no application class name");
                return;
            }
        }
        catch (NameNotFoundException e)
        {
            Log.i("demo", "error:"+Log.getStackTraceString(e));
            e.printStackTrace();
        }
        //有值的话调用该Applicaiton
        //获取ActivityThread实例
        Object currentActivityThread = RefInvoke.invokeStaticMethod("android.app.ActivityThread", "currentActivityThread", new Class[] {}, new Object[] {});
        Object mBoundApplication = RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mBoundApplication");
        Object loadedApkInfo = RefInvoke.getFieldOjbect("android.app.ActivityThread$AppBindData", mBoundApplication, "info");
        //把当前进程的mApplication 设置成了null
        RefInvoke.setFieldOjbect("android.app.LoadedApk", "mApplication", loadedApkInfo, null);

        Object oldApplication = RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mInitialApplication");
        //http://www.codeceo.com/article/android-context.html
        ArrayList<Application> mAllApplications = (ArrayList<Application>) RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mAllApplications");
        mAllApplications.remove(oldApplication);//删除oldApplication

        ApplicationInfo appinfo_In_LoadedApk = (ApplicationInfo) RefInvoke.getFieldOjbect("android.app.LoadedApk", loadedApkInfo, "mApplicationInfo");
        ApplicationInfo appinfo_In_AppBindData = (ApplicationInfo) RefInvoke.getFieldOjbect("android.app.ActivityThread$AppBindData", mBoundApplication, "appInfo");
        appinfo_In_LoadedApk.className = appClassName;
        appinfo_In_AppBindData.className = appClassName;
        Application new_org_application = (Application) RefInvoke.invokeMethod("android.app.LoadedApk", "makeApplication", loadedApkInfo, new Class[] { boolean.class, Instrumentation.class }, new Object[] { false, null });//执行 makeApplication（false,null）
        RefInvoke.setFieldOjbect("android.app.ActivityThread", "mInitialApplication", currentActivityThread, new_org_application);


        ArrayMap mProviderMap = (ArrayMap) RefInvoke.getFieldOjbect(
                "android.app.ActivityThread", currentActivityThread,
                "mProviderMap");
        Iterator it = mProviderMap.values().iterator();
        while (it.hasNext())
        {
            Object providerClientRecord = it.next();
            Object localProvider = RefInvoke.getFieldOjbect
                    (
                    "android.app.ActivityThread$ProviderClientRecord",
                    providerClientRecord, "mLocalProvider");
            RefInvoke.setFieldOjbect("android.content.ContentProvider",
                    "mContext", localProvider, new_org_application);
        }

        Log.i("demo", "app:"+new_org_application);

        new_org_application.onCreate();

    }

    /**
     * 释放被加壳的apk文件，so文件
     * @param data
     * @throws IOException
     */
    //private void splitPayLoadFromDex(byte[] apkdata) throws IOException
    private void splitPayLoadFromDex() throws IOException
    {
        InputStream is= this.getAssets().open("app-debug.apk");//我们就先指定加载的对象
        int flen=is.available();
        byte[] new_apk=new byte[flen];
        is.read(new_apk);
        is.close();

//        int ablen = apkdata.length;
//        //取被加壳apk的长度   这里的长度取值，对应加壳时长度的赋值都可以做些简化
//        byte[] dexlen = new byte[4];
//        System.arraycopy(apkdata, ablen - 4, dexlen, 0, 4);
//        ByteArrayInputStream bais = new ByteArrayInputStream(dexlen);
//        DataInputStream in = new DataInputStream(bais);
//        int readInt = in.readInt();
//        System.out.println(Integer.toHexString(readInt));
//        byte[] newdex = new byte[readInt];
//        //把被加壳apk内容拷贝到newdex中
//        System.arraycopy(apkdata, ablen - 4 - readInt, newdex, 0, readInt);
//        //这里应该加上对于apk的解密操作，若加壳是加密处理的话
//        //?
//
//        //对源程序Apk进行解密
//        newdex = decrypt(newdex);

        //创建并写入apk文件
        File file = new File(apkFileName);
        file.createNewFile();
        try {
            FileOutputStream localFileOutputStream = new FileOutputStream(file);
            localFileOutputStream.write(new_apk);
            localFileOutputStream.close();
        } catch (IOException localIOException) {
            throw new RuntimeException(localIOException);
        }

        //分析被加壳的apk文件
        ZipInputStream localZipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)));
        while (true) {

            ZipEntry localZipEntry = localZipInputStream.getNextEntry();//不了解这个是否也遍历子目录，看样子应该是遍历的
            if (localZipEntry == null)
            {
                localZipInputStream.close();
                break;
            }
            //取出被加壳apk用到的so文件，放到 libPath中（data/data/包名/payload_lib)
            String name = localZipEntry.getName();
            if (name.startsWith("lib/") && name.endsWith(".so"))
            {
                File storeFile = new File(libPath + "/" + name.substring(name.lastIndexOf('/')));
                storeFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(storeFile);
                byte[] arrayOfByte = new byte[1024];
                while (true)
                {
                    int i = localZipInputStream.read(arrayOfByte);
                    if (i == -1)
                        break;
                    fos.write(arrayOfByte, 0, i);
                }
                fos.flush();
                fos.close();
            }
            localZipInputStream.closeEntry();
        }
        localZipInputStream.close();
    }

    /**
     * 从apk包里面获取dex文件内容（byte）
     * @return
     * @throws IOException
     */
//    private byte[] readDexFileFromApk() throws IOException {
//        ByteArrayOutputStream dexByteArrayOutputStream = new ByteArrayOutputStream();
//        ZipInputStream localZipInputStream = new ZipInputStream(
//                new BufferedInputStream(new FileInputStream(
//                        this.getApplicationInfo().sourceDir)));
//        while (true) {
//            ZipEntry localZipEntry = localZipInputStream.getNextEntry();
//            if (localZipEntry == null) {
//                localZipInputStream.close();
//                break;
//            }
//            if (localZipEntry.getName().equals("classes.dex")) {
//                byte[] arrayOfByte = new byte[1024];
//                while (true) {
//                    int i = localZipInputStream.read(arrayOfByte);
//                    if (i == -1)
//                        break;
//                    dexByteArrayOutputStream.write(arrayOfByte, 0, i);
//                }
//            }
//            localZipInputStream.closeEntry();
//        }
//        localZipInputStream.close();
//        return dexByteArrayOutputStream.toByteArray();
//    }


    // //直接返回数据，读者可以添加自己解密方法
//    private byte[] decrypt(byte[] srcdata) {
//        for(int i=0;i<srcdata.length;i++){
//            srcdata[i] = (byte)(0xFF ^ srcdata[i]);
//        }
//        return srcdata;
//    }


    //以下是加载资源
    protected AssetManager mAssetManager;//资源管理器
    protected Resources mResources;//资源
    protected Theme mTheme;//主题

    protected void loadResources(String dexPath)
    {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, dexPath);
            mAssetManager = assetManager;
        } catch (Exception e) {
            Log.i("inject", "loadResource error:"+Log.getStackTraceString(e));
            e.printStackTrace();
        }
        Resources superRes = super.getResources();
        superRes.getDisplayMetrics();
        superRes.getConfiguration();
        mResources = new Resources(mAssetManager, superRes.getDisplayMetrics(),superRes.getConfiguration());
        mTheme = mResources.newTheme();
        mTheme.setTo(super.getTheme());
    }

    @Override
    public AssetManager getAssets() {
        return mAssetManager == null ? super.getAssets() : mAssetManager;
    }

    @Override
    public Resources getResources() {
        return mResources == null ? super.getResources() : mResources;
    }

    @Override
    public Theme getTheme() {
        return mTheme == null ? super.getTheme() : mTheme;
    }

}