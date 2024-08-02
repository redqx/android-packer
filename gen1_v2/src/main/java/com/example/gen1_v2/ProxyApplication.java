package com.example.gen1_v2;


import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dalvik.system.DexClassLoader;

public class ProxyApplication extends Application {
    String TAG = "demo";
    public String apkFileName;
    public String libPath;
    public String dexPath;



    @Override
    protected void attachBaseContext(Context base)
    {

        super.attachBaseContext(base);



        try {
            //在应用程序的数据存储目录下创建文件夹，具体路径为data/user/0/包名/app_payload_dex(怎么多了app_?)
            File dex = getDir("payload_dex", MODE_PRIVATE);
            File lib = getDir("payload_lib", MODE_PRIVATE);
            dexPath = dex.getAbsolutePath();
            libPath = lib.getAbsolutePath();
            apkFileName = dex.getAbsolutePath() + File.separator + "Source.apk";
            // 根据文件路径创建File对象 和以前分析的差不多
            File dexFile = new File(apkFileName);
            if (!dexFile.exists())
            {
                // 根据路径创建文件，即在payload_dex目录下创建Source.apk文件
                //读取Classes.dex文件
                //从中分理处源apk文件
                splitPayLoadFromDex("app-debug.apk");
            }

            //配置加载源程序的动态环境,即替换mClassLoader
            replaceClassLoaderInLoadedApk();

        } catch (Exception e) {
            Log.e(TAG, "attachBaseContext: " + Log.getStackTraceString(e));

        }
    }

    //替换LoadedApk中的mClassLoader
    private void replaceClassLoaderInLoadedApk() throws Exception
    {
        // 获取应用程序当前的classloader
        ClassLoader classLoader = this.getClassLoader();
        Log.d(TAG, "classLoader get: " + classLoader.toString());
        Log.d(TAG, "parent classLoader get: " + classLoader.getParent().toString());
        // 获取ActivityThread类
        Class<?> cls_ActivityThread = classLoader.loadClass("android.app.ActivityThread");

        // ActivityThread已经实例化了，我们需要通过反射currentActivityThread()方法获取实例，而不是通过类反射创建实例（他都不是同一个实例，创建没屁用）
        // 1.通过反射获取方法，进一步获取ActivityThread实例
//            Method currentActivityThreadMethod = ActivityThreadClass.getDeclaredMethod("currentActivityThread");
//            Log.d(TAG, "currentActivityThreadMethod: " + currentActivityThreadMethod.toString());
//            currentActivityThreadMethod.setAccessible(true);
//            Object sCurrentActivityThreadObj = currentActivityThreadMethod.invoke(null);//为什么这里可以设置为null
//            Log.d(TAG, "反射获取方法，进一步获取ActivityThread实例: " + sCurrentActivityThreadObj.toString());
        // 2.直接反射获取ActivityThread字段
        Field f_sCurrentActivityThread = cls_ActivityThread.getDeclaredField("sCurrentActivityThread");
        f_sCurrentActivityThread.setAccessible(true);
        Object obj_sCurrentActivityThread = f_sCurrentActivityThread.get(null);//为什么这里可以设置为null

        //获取mPackages,类型为ArrayMap<String, WeakReference<LoadedApk>>, 里面存放了当前应用的LoadedApk对象
        Field f_mPackages = cls_ActivityThread.getDeclaredField("mPackages");
        f_mPackages.setAccessible(true);
        //获取当前ActivityThread实例的mPackages字段
        ArrayMap obj_mPackages = (ArrayMap) f_mPackages.get(obj_sCurrentActivityThread);

        //获取mPackages中的当前应用包名
        String currentPackageName = this.getPackageName();

        // 获取loadedApk实例也有好几种,mInitialApplication mAllApplications mPackages
        // 通过包名获取当前应用的loadedApk实例
        WeakReference weakReference = (WeakReference) obj_mPackages.get(currentPackageName);
        Object obj_loadedApk = weakReference.get();

        //动态加载源程序的dex文件,以当前classloader的父加载器作为parent
        DexClassLoader dexClassLoader = new DexClassLoader(apkFileName,dexPath,libPath, classLoader.getParent());
        //替换loadedApk实例中的mClassLoader字段
        Class<?> cls_LoadedApk = classLoader.loadClass("android.app.LoadedApk");
        Field f_mClassLoader = cls_LoadedApk.getDeclaredField("mClassLoader");
        f_mClassLoader.setAccessible(true);
        f_mClassLoader.set(obj_loadedApk, dexClassLoader);

        //加载源程序的类
        //可有可无，只是测试看看有没有这个类
        try{
            dexClassLoader.loadClass("com.example.pack.MainActivity");
            Log.d(TAG, "com.example.sourceapk.MainActivity: 类加载成功");
        }catch (ClassNotFoundException e){
            Log.d(TAG, "com.example.sourceapk.MainActivity: " + Log.getStackTraceString(e));
        }

    }
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "SourceApk Application onCreate: " + this);

        //Application实例存在于: LoadedApk中的mApplication字段
        // 以及ActivityThread中的mInitialApplication和mAllApplications和mBoundApplication字段
        //替换Application

        loadResources(apkFileName);//好像并没有什么乱用

        String appClassName = null;
        try //读取自身xml元素
        {
            //获取AndroidManifest.xml 文件中的 <meta-data> 元素
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = applicationInfo.metaData;
            //获取xml文件声明的Application类
            if (metaData != null && metaData.containsKey("APPLICATION_CLASS_NAME")){
                appClassName = metaData.getString("APPLICATION_CLASS_NAME");
            }
            else
            {
                Log.e(TAG, "xml文件中没有声明Application类名");
                //是因为没有自定义application就不好动态加载源程序的application吗?
                return;
            }
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        //xml文件中存在自定义application类
        //开始替换

        //获取ActivityThread实例
        ClassLoader classLoader = this.getClassLoader();
        try
        {
            /*
             * 把ActivityThreadClass.sCurrentActivityThread.mBoundApplication(AppBindData).info(LoadedApk).mApplication设置为null
             * */


            //获取ActivityThread类
            Class<?> cls_ActivityThreadClass = classLoader.loadClass("android.app.ActivityThread");
            //反射获取sCurrentActivityThread实例
            Field f_sCurrentActivityThread = cls_ActivityThreadClass.getDeclaredField("sCurrentActivityThread");
            f_sCurrentActivityThread.setAccessible(true);
            Object obj_sCurrentActivityThread = f_sCurrentActivityThread.get(null);//为什么这里可以设置为null

            //获取mBoundApplication字段 (AppBindData对象)
            Field mBoundApplication = cls_ActivityThreadClass.getDeclaredField("mBoundApplication");
            mBoundApplication.setAccessible(true);
            Object obj_mBoundApplicationObj = mBoundApplication.get(obj_sCurrentActivityThread);

            //获取mBoundApplication对象中的info (LoadedApk对象)
            //所以这个和之前通过mPackages字段获取LoadedApk有什么不同???
            //首先获取AppBindData类,它位于ActivityThread类内部
            Class<?> cls_AppBindData = classLoader.loadClass("android.app.ActivityThread$AppBindData");
            Field f_info = cls_AppBindData.getDeclaredField("info");
            f_info.setAccessible(true);
            Object obj_info = f_info.get(obj_mBoundApplicationObj);

            //把infoObj (LoadedApk对象)中的mApplication设置为null,这样后续才能调用makeApplication()!!!
            Class<?> cls_LoadedApk = classLoader.loadClass("android.app.LoadedApk");
            Field f_mApplication = cls_LoadedApk.getDeclaredField("mApplication");
            f_mApplication.setAccessible(true);
            Log.d(TAG, "mApplication: " + f_mApplication.get(obj_info).toString());
            f_mApplication.set(obj_info, null);




            /*
             * 从ActivityThreadClass.sCurrentActivityThread.mAllApplications移除ActivityThreadClass.sCurrentActivityThread.mInitialApplication
             * */

            //获取ActivityThread实例中的mInitialApplication字段,拿到旧的Application(对于要加载的Application来讲)
            //为什么不直接通过刚才的info获取???
            Field f_mInitialApplication = cls_ActivityThreadClass.getDeclaredField("mInitialApplication");
            f_mInitialApplication.setAccessible(true);
            Object obj_mInitialApplication = f_mInitialApplication.get(obj_sCurrentActivityThread);


            //获取ActivityThread实例中的mAllApplications字段,然后删除里面的mInitialApplication,也就是旧的application
            Field f_mAllApplications = cls_ActivityThreadClass.getDeclaredField("mAllApplications");
            f_mAllApplications.setAccessible(true);
            ArrayList<Application> obj_mAllApplications = (ArrayList<Application>)f_mAllApplications.get(obj_sCurrentActivityThread);
            obj_mAllApplications.remove(obj_mInitialApplication);
            Log.d(TAG, "mInitialApplication 从 mAllApplications 中移除成功");


            /*
             * 修改把ActivityThreadClass.sCurrentActivityThread.mBoundApplication(AppBindData).info(LoadedApk).mApplicationInfo为源apk的ApplicationInfo
             * 修改把ActivityThreadClass.sCurrentActivityThread.mBoundApplication(AppBindData).appInfo为源apk的ApplicationInfo
             * */

            //这是要干嘛???
            //获取LoadedApk的mApplicationInfo字段
            Field f_mApplicationInfo = cls_LoadedApk.getDeclaredField("mApplicationInfo");
            f_mApplicationInfo.setAccessible(true);
            ApplicationInfo obj_mApplicationInfo = (ApplicationInfo) f_mApplicationInfo.get(obj_info);

            //获取mBoundApplication对象中的appInfo
            Field f_appInfo = cls_AppBindData.getDeclaredField("appInfo");
            f_appInfo.setAccessible(true);
            ApplicationInfo obj_appinfo = (ApplicationInfo) f_appInfo.get(obj_mBoundApplicationObj);

            //设置两个appinfo的classname为源程序的application类名,以便后续调用makeApplication()创建源程序的application
            obj_mApplicationInfo.className = appClassName;
            obj_appinfo.className = appClassName;
            //appClassName就是源apk的Application


            /*
             * 调用ActivityThreadClass.sCurrentActivityThread.mBoundApplication(AppBindData).info(LoadedApk).makeApplicatio()
             * 创建新的源apk的application
             * */

            //反射调用makeApplication方法创建源程序的application
            Method makeApplicationMethod = cls_LoadedApk.getDeclaredMethod("makeApplication", boolean.class, Instrumentation.class);
            makeApplicationMethod.setAccessible(true);
            Application org_Application = (Application) makeApplicationMethod.invoke(obj_info, false, null);
            Log.d(TAG, "创建源程序application成功");
            //

            /*
             * 把ActivityThreadClass.sCurrentActivityThread.mInitialApplication设置为我们的新创建的org_Application
             * */
            //将刚创建的Application设置到ActivityThread的mInitialApplication字段
            f_mInitialApplication.set(obj_sCurrentActivityThread, org_Application);
            Log.d(TAG, "源程序的application成功设置到mInitialApplication字段");

            /*
             * 下面仍然是赋值org_Application
             * 把ActivityThreadClass.sCurrentActivityThread.mProviderMap
             * */
            //ContentProvider会持有代理的Application,需要特殊处理一下
            Field f_mProviderMap = cls_ActivityThreadClass.getDeclaredField("mProviderMap");
            f_mProviderMap.setAccessible(true);
            ArrayMap mProviderMapObj = (ArrayMap) f_mProviderMap.get(obj_sCurrentActivityThread);

            //获取所有provider,装进迭代器中遍历
            Iterator iterator = mProviderMapObj.values().iterator();
            while(iterator.hasNext())
            {
                Object providerClientRecord = iterator.next();
                //获取ProviderClientRecord中的mLocalProvider字段
                Class<?> cls_ProviderClientRecordClass = classLoader.loadClass("android.app.ActivityThread$ProviderClientRecord");
                Field f_mLocalProvider = cls_ProviderClientRecordClass.getDeclaredField("mLocalProvider");
                f_mLocalProvider.setAccessible(true);

                Object mLocalProviderObj = f_mLocalProvider.get(providerClientRecord);
                //mLocalProviderObj可能为空
                if(mLocalProviderObj != null)
                {
                    //获取ContentProvider中的mContext字段,设置为新建的Application
                    Class<?> cls_ContentProviderClass = classLoader.loadClass("android.content.ContentProvider");
                    Field f_mContext = cls_ContentProviderClass.getDeclaredField("mContext");
                    f_mContext.setAccessible(true);
                    f_mContext.set(mLocalProviderObj,org_Application);
                }

            }
            //开始Application的创建,源程序启动!
            org_Application.onCreate();

        } catch (ClassNotFoundException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void splitPayLoadFromDex(String asset_apkName) throws IOException
    {
        InputStream is= this.getAssets().open(asset_apkName);//我们就先指定加载的对象
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


    //以下是加载资源, 好像并没有什么乱用.....
    protected AssetManager mAssetManager;//资源管理器
    protected Resources mResources;//资源
    protected Resources.Theme mTheme;//主题

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
    public Resources.Theme getTheme() {
        return mTheme == null ? super.getTheme() : mTheme;
    }
}
