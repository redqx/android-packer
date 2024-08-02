package com.example.by_java_packapk_v_2;



import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;

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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import dalvik.system.DexClassLoader;

/**
 * =============================================================================
 * Copyright (c) 2017 yuxin All rights reserved.
 * Packname com.jju.yuxin.reforceapk
 * Created by yuxin.
 * Created time 2017/6/18 0018 下午 5:03.
 * Version   1.0;
 * Describe :
 * History:
 * ==============================================================================
 */


//Applicaiton做为整个应用的上下文，会被系统第一时间调用，这也是应用开发者程序代码的第一执行点
public class ProxyApplication extends Application{

    private static String DEXFILENAME = "update.apk";

    private static String PARAMCONFIG_FileName = "paramConfig.json";

    private static final String appkey = "APPLICATION_CLASS_NAME";

    private static String cryptKey = "fuck all the android crackers";

    public static String PAYLOAD_ODEX = "my_payload_odex";

    public static String PAYLOAD_LIB = "my_payload_lib";

    private  static final String TAG = ProxyApplication.class.getSimpleName();

    private String srcDexFilePath = "";
    private String odexPath = "";
    private String libPath = "";

    private static String gIPstr = "";
    private static String gUserNameStr = "";

    private Context context = null;

    //以下是加载资源
    protected AssetManager mAssetManager = null;
    protected Resources mResources = null;
    protected Resources.Theme mTheme = null;


    //why run 2 times?
    @SuppressWarnings("rawtypes")
    @Override
    protected void attachBaseContext(Context base) {

        super.attachBaseContext(base);
        //getBaseContext()  返回由构造函数指定或setBaseContext()设置的上下文

        context = base;

        Log.e(TAG,"[notice] => Application.attachBaseContext");

        try {
//        	/data/user/0/com.apkunshell/app_payload_odex
            File odexPathFile = this.getDir(PAYLOAD_ODEX, MODE_PRIVATE);
//        	/data/user/0/com.apkunshell/app_payload_libs
            File libsPathFile = this.getDir(PAYLOAD_LIB, MODE_PRIVATE);

            //用于存放源apk释放出来的dex
            odexPath = odexPathFile.getAbsolutePath();
            //用于存放源Apk用到的so文件
            libPath = libsPathFile.getAbsolutePath();
            //用于存放解密后的apk
            srcDexFilePath = odexPathFile.getAbsolutePath() + "/" + DEXFILENAME;

//            String apppath = this.getFilesDir().getParent() + "/";
//            InputStream is = this.getAssets().open(APKFILENAME);
//            int size = is.available();
//            byte []buffer = new byte[size];
//            is.read(buffer);
//            is.close();
//            OutputStream os = new FileOutputStream(apppath + APKFILENAME);
//            os.write(buffer);
//            os.close();

            File srcDexFile = new File(srcDexFilePath);
            //第一次加载
            if (srcDexFile.exists() == false)
            {
                Log.e(TAG, "beFirstLoading");

                //srcDexFile.createNewFile();
                //拿到dex文件
                //byte[] dexdata = this.readDexFileFromApk();
                //取出源APK解密后放置在/payload.apk，及其so文件放置在payload_lib/下
                //this.splitPayLoadFromDex(dexdata);

                this.splitPayLoadFromDex2();
            }

            // 配置动态加载环境
            //反射获取主线程对象，并从中获取所有已加载的package信息，并中找到当前的LoadApk对象的弱引用
            //// 配置动态加载环境 获取主线程对象 http://blog.csdn.net/myarrow/article/details/14223493
            Object currentActivityThread = RefInvoke.invokeStaticMethod("android.app.ActivityThread", "currentActivityThread",new Class[] {}, new Object[] {});
            ArrayMap mPackages = (ArrayMap) RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread,"mPackages");
            String packageName = this.getPackageName();
            WeakReference wr = (WeakReference) mPackages.get(packageName);

            ///创建被加壳apk的DexClassLoader对象 加载apk内的类和本地代码（c/c++代码）
            //创建一个新的DexClassLoader用于加载源Apk,传入apk路径，dex释放路径，so路径，及父节点的DexClassLoader使其遵循双亲委托模型
            ClassLoader fathercl = (ClassLoader) RefInvoke.getFieldOjbect("android.app.LoadedApk", wr.get(), "mClassLoader");
            DexClassLoader dLoader = new DexClassLoader(srcDexFilePath, odexPath,libPath, fathercl);

            //base.getClassLoader(); 是不是就等同于 (ClassLoader) RefInvoke.getFieldOjbect()?
            //getClassLoader()等同于 (ClassLoader) RefInvoke.getFieldOjbect(),但是为了替换掉父节点我们需要通过反射来获取并修改其值

            //将父节点DexClassLoader替换
            //把当前进程的DexClassLoader 设置成了被加壳apk的DexClassLoader
            RefInvoke.setFieldOjbect("android.app.LoadedApk", "mClassLoader",wr.get(), dLoader);

            //调试时，测试类加载是否成功
            //Object actObj = dLoader.loadClass(LOADCLASSNAME);
            //Log.e(TAG, "get class object:" + actObj);

        } catch (Exception e) {
            Log.e(TAG, "error:"+Log.getStackTraceString(e));
            e.printStackTrace();
        }
    }



    //why run 2 times?
    @SuppressWarnings("rawtypes")
    public void onCreate() {
        Log.e(TAG,"[notice] => Application.onCreate");
        try {
            Log.e(TAG, "onCreate");

            Log.e(TAG,"Application:" + context + ",BaseContext:" + getBaseContext() + ",ApplicationContext:" + getApplicationContext() + ",Activity:" + this);

            if(context == null){
                context = this;
                if(context == null){
                    context = Utils.getContext();
                }
            }

            //加载源apk资源
            loadResources(srcDexFilePath);

            //获取配置在清单文件的源Apk的Application路径
            String appClassName = null;
            try {
                ApplicationInfo ai = this.getPackageManager().getApplicationInfo(this.getPackageName(),PackageManager.GET_META_DATA);
                Bundle bundle = ai.metaData;
                if (bundle != null && bundle.containsKey(appkey)) {
                    appClassName = bundle.getString(appkey);	//className 是配置在xml文件中的
                }else {
                    Log.e(TAG, "not found class name of application in bundle");
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "error:"+Log.getStackTraceString(e));
                e.printStackTrace();
                return;
            }

            //获取当前壳Apk的ApplicationInfo
            Object currentActivityThread = RefInvoke.invokeStaticMethod("android.app.ActivityThread", "currentActivityThread",new Class[] {}, new Object[] {});

            Object mBoundApplication = RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread,"mBoundApplication");

            Object loadedApkInfo = RefInvoke.getFieldOjbect("android.app.ActivityThread$AppBindData",mBoundApplication, "info");

            //将LoadedApk中的ApplicationInfo设置为null,what does this mean?
            RefInvoke.setFieldOjbect("android.app.LoadedApk", "mApplication",loadedApkInfo, null);

            //获取currentActivityThread中注册的Application
            Object oldApplication = RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread,"mInitialApplication");

            //获取ActivityThread中所有已注册的Application，并将当前壳Apk的Application从中移除
            @SuppressWarnings("unchecked")
            ArrayList<Application> mAllApplications = (ArrayList<Application>) RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mAllApplications");
            mAllApplications.remove(oldApplication);

            ApplicationInfo appinfo_In_LoadedApk = (ApplicationInfo) RefInvoke.getFieldOjbect("android.app.LoadedApk", loadedApkInfo,"mApplicationInfo");

            ApplicationInfo appinfo_In_AppBindData = (ApplicationInfo) RefInvoke.getFieldOjbect("android.app.ActivityThread$AppBindData",mBoundApplication, "appInfo");

            //替换原来的Application
            appinfo_In_LoadedApk.className = appClassName;
            appinfo_In_AppBindData.className = appClassName;

            //注册Application
            Application app = (Application) RefInvoke.invokeMethod("android.app.LoadedApk", "makeApplication",
                    loadedApkInfo,new Class[] { boolean.class, Instrumentation.class },new Object[] { false, null });

            //替换ActivityThread中的Application
            RefInvoke.setFieldOjbect("android.app.ActivityThread","mInitialApplication", currentActivityThread, app);

            ArrayMap mProviderMap = (ArrayMap) RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread,"mProviderMap");
            Iterator it = mProviderMap.values().iterator();
            while (it.hasNext()) {
                Object providerClientRecord = it.next();
                Object localProvider = RefInvoke.getFieldOjbect("android.app.ActivityThread$ProviderClientRecord", providerClientRecord, "mLocalProvider");
                RefInvoke.setFieldOjbect("android.content.ContentProvider", "mContext", localProvider, app);
            }

            app.onCreate();

            Log.e(TAG, "app:"+app);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void splitPayLoadFromDex(byte[] shelldexdata) throws IOException {
        //取被加壳apk的长度
        int sdlen = shelldexdata.length;
        byte[] bytedexlen = new byte[4];
        System.arraycopy(shelldexdata, sdlen - 4, bytedexlen, 0, 4);

        ByteArrayInputStream bais = new ByteArrayInputStream(bytedexlen);
        DataInputStream dis = new DataInputStream(bais);
        int readInt = dis.readInt();
        Log.d(TAG,"Integer.toHexString(readInt):"+Integer.toHexString(readInt));

        //取出apk
        byte[] encryptdata = new byte[readInt];
        System.arraycopy(shelldexdata, sdlen - 4 - readInt, encryptdata, 0, readInt);

        //对源程序Apk进行解密
        byte[] flatdata = xorcrypt(encryptdata);

        int offset = 0;
        byte [] byteunamelen = new byte[4];
        System.arraycopy(flatdata, offset, byteunamelen, 0, 4);
        offset += 4;

        int unamelen = Utils.bytesToInt(byteunamelen);
        byte[] username = new byte[unamelen];
        System.arraycopy(flatdata , offset, username, 0, unamelen);
        offset += unamelen;

        gUserNameStr = new String(username);

        byte [] byteiplen = new byte[4];
        System.arraycopy(flatdata, offset, byteiplen, 0, 4);
        offset += 4;

        int iplen = Utils.bytesToInt(byteiplen);
        byte[] ip = new byte[iplen];
        System.arraycopy(flatdata , offset, ip, 0, iplen);
        offset += iplen;

        gIPstr = new String(ip);

        Log.e(TAG,"username:"+ gUserNameStr + " ip:" + gIPstr);

        Utils.setValue(context,PARAMCONFIG_FileName,"username",gUserNameStr);
        Utils.setValue(context,PARAMCONFIG_FileName,"ip",gIPstr);

        //写入源apk文件
        File file = new File(srcDexFilePath);
        try {
            FileOutputStream localFileOutputStream = new FileOutputStream(file);
            localFileOutputStream.write(flatdata,offset,readInt - offset);
            localFileOutputStream.close();
        } catch (IOException localIOException) {
            throw new RuntimeException(localIOException);
        }

        int bits = Utils.getSystemBits();
        String libprefix = "lib/" + Build.CPU_ABI;

        //分析源apk文件
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)));
        while (true) {
            ZipEntry ze = zis.getNextEntry();
            if (ze == null) {
                break;
            }

            //依次取出被加壳apk用到的so文件，放到 libPath中（data/data/包名/payload_lib)
            String zfn = ze.getName();

            if (zfn.startsWith(libprefix) && zfn.endsWith(".so")) {
                File sofile = new File(libPath + zfn.substring(zfn.lastIndexOf('/')));
                sofile.createNewFile();
                FileOutputStream fos = new FileOutputStream(sofile);
                byte[] readbuf = new byte[0x4000];
                while (true) {
                    int readlen = zis.read(readbuf);
                    if (readlen == -1){
                        break;
                    }
                    fos.write(readbuf, 0, readlen);
                }
                fos.flush();
                fos.close();
                Log.e(TAG,"get lib:" + zfn );
            }
            zis.closeEntry();
        }
        zis.close();
    }

    private void splitPayLoadFromDex2() throws IOException
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
        File file = new File(srcDexFilePath);
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
     * 拿到自己apk文件中的dex文件
     * @return
     * @throws IOException
     */
    private byte[] readDexFileFromApk() throws IOException {

        ByteArrayOutputStream dexbaos = new ByteArrayOutputStream();

        //getApplicationInfo().sourceDir == /data/user/0/com.adobe.flashplayer/base.apk
        //BufferedInputStream会将该输入流数据分批读取，每次读取一部分到缓冲中；操作完缓冲中的这部分数据之后，再从输入流中读取下一部分的数据
        //无其他用途
        //ZipInputStream zis = new ZipInputStream(new FileInputStream(this.getApplicationInfo().sourceDir));

        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(this.getApplicationInfo().sourceDir)));

        while (true) {
            ZipEntry ze = zis.getNextEntry();
            if (ze == null) {
                break;
            }

            //拿到dex文件
            if (ze.getName().equals("classes.dex")) {
                byte[] readbuf = new byte[0x10000];
                while (true) {
                    int readlen = zis.read(readbuf);
                    if (readlen == -1){
                        zis.closeEntry();
                        break;
                    }

                    dexbaos.write(readbuf, 0, readlen);
                }
                zis.closeEntry();
                break;
            }else{
                zis.closeEntry();
            }
        }

        zis.close();
        return dexbaos.toByteArray();
    }


    private static byte[] xorcrypt(byte[] srcdata){
        byte[] key = cryptKey.getBytes();
        int keylen = cryptKey.length();
        for(int i = 0,j = 0; i<srcdata.length; i++){
            srcdata[i] = (byte)(key[j] ^ srcdata[i]);
            j ++;
            if(j >= keylen){
                j = 0;
            }
        }
        return srcdata;
    }


    protected void loadResources(String srcApkPath) {
        //创建一个AssetManager放置源apk的资源
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, srcApkPath);
            mAssetManager = assetManager;
        } catch (Exception e) {
            Log.i(TAG, "inject:loadResource error:"+Log.getStackTraceString(e));
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
