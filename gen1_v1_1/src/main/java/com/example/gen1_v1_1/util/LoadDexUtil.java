package com.example.gen1_v1_1.util;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;



import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import dalvik.system.DexClassLoader;

/**
 * 方式一：壳程序直接对APK进行加密方式
 *
 * 这里对APK解密及加载
 */
public class LoadDexUtil {



    /**
     * 构造原Application对象
     * @param srcApplicationClassName 原Application类名
     * @return 返回原application对象
     */
    public static Application makeApplication(String srcApplicationClassName)
    {
        if (TextUtils.isEmpty(srcApplicationClassName))
        {
            LogUtil.error("请配置原APK的Application ===== ");
            return null;
        }

        //调用静态方法android.app.ActivityThread.currentActivityThread获取当前activity所在的线程对象
        Object currentActivityThread = getCurrentActivityThread();
        //获取当前currentActivityThread的mBoundApplication属性对象，
        //该对象是一个AppBindData类对象，该类是ActivityThread的一个内部类
        Object mBoundApplication = getBoundApplication(currentActivityThread);
        //读取mBoundApplication中的info信息，info是LoadedApk对象
        Object loadedApkInfo = getLoadApkInfoObj(mBoundApplication);


        //先从LoadedApk中反射出mApplicationInfo变量，并设置其className为原Application的className
        //todo:注意：这里一定要设置，否则makeApplication还是壳Application对象，造成一直在attach中死循环
        ApplicationInfo mApplicationInfo = (ApplicationInfo) RefInvoke.getFieldOjbect("android.app.LoadedApk", loadedApkInfo, "mApplicationInfo");
        mApplicationInfo.className = srcApplicationClassName;
        //执行 makeApplication（false,null）
        Application app = (Application) RefInvoke.invokeMethod("android.app.LoadedApk", "makeApplication", loadedApkInfo, new Class[] { boolean.class, Instrumentation.class }, new Object[] { false, null });

        //由于源码ActivityThread中handleBindApplication方法绑定Application后会调用installContentProviders，
        //此时传入的context仍为壳Application，故此处进手动安装ContentProviders，调用完成后，清空原providers
        installContentProviders(app,currentActivityThread,mBoundApplication);

        return app;
    }


    /**
     * 手动安装ContentProviders
     * @param app 原Application对象
     * @param currentActivityThread 当前ActivityThread对象
     * @param boundApplication 当前AppBindData对象
     */
    private static void installContentProviders(Application app,Object currentActivityThread,Object boundApplication){
        if (app == null) return;
        LogUtil.info("执行installContentProviders =================");
        List providers = (List) RefInvoke.getFieldOjbect("android.app.ActivityThread$AppBindData",
                boundApplication, "providers");
        LogUtil.info( "反射拿到providers = " + providers);
        if (providers != null) {
            RefInvoke.invokeMethod("android.app.ActivityThread","installContentProviders",currentActivityThread,new Class[]{Context.class,List.class},new Object[]{app,providers});
            providers.clear();
        }
    }


    /**
     * Application替换并运行
     * @param app 原application对象
     */
    public static void replaceAndRunMainApplication(Application app)
    {
        if (app == null)
        {
            LogUtil.error(" 原APK的Application 创建失败=====");
            return;
        }

        LogUtil.info( "onCreate ===== 开始替换=====");
        // 如果源应用配置有Appliction对象，则替换为源应用Applicaiton，以便不影响源程序逻辑。
        final String appClassName = app.getClass().getName();

        //调用静态方法android.app.ActivityThread.currentActivityThread获取当前activity所在的线程对象
        Object currentActivityThread = getCurrentActivityThread();
        //获取当前currentActivityThread的mBoundApplication属性对象，
        //该对象是一个AppBindData类对象，该类是ActivityThread的一个内部类
        Object mBoundApplication = getBoundApplication(currentActivityThread);
        //读取mBoundApplication中的info信息，info是LoadedApk对象
        Object loadedApkInfo = getLoadApkInfoObj(mBoundApplication);
        //检测loadApkInfo是否为空
        if (loadedApkInfo == null){
            LogUtil.error( "loadedApkInfo ===== is null !!!!");
        }else {
            LogUtil.info( "loadedApkInfo ===== "+loadedApkInfo);
        }

        //把当前进程的mApplication 设置成了原application,
        RefInvoke.setFieldOjbect("android.app.LoadedApk", "mApplication", loadedApkInfo, app);
        Object oldApplication = RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mInitialApplication");
        LogUtil.info( "oldApplication ===== "+oldApplication);
        ArrayList<Application> mAllApplications = (ArrayList<Application>) RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mAllApplications");
        //将壳oldApplication从ActivityThread#mAllApplications列表中移除
        mAllApplications.remove(oldApplication);

        //将原Application赋值给mInitialApplication
        RefInvoke.setFieldOjbect("android.app.ActivityThread", "mInitialApplication", currentActivityThread, app);


//        ApplicationInfo appinfo_In_LoadedApk = (ApplicationInfo) RefInvoke.getFieldOjbect(
//                "android.app.LoadedApk", loadedApkInfo, "mApplicationInfo");
        ApplicationInfo appinfo_In_AppBindData = (ApplicationInfo) RefInvoke.getFieldOjbect(
                "android.app.ActivityThread$AppBindData", mBoundApplication, "appInfo");
//        appinfo_In_LoadedApk.className = appClassName;
        appinfo_In_AppBindData.className = appClassName;


        ArrayMap mProviderMap = (ArrayMap) RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mProviderMap");
        Iterator it = mProviderMap.values().iterator();
        while (it.hasNext())
        {
            Object providerClientRecord = it.next();
            Object localProvider = RefInvoke.getFieldOjbect("android.app.ActivityThread$ProviderClientRecord", providerClientRecord, "mLocalProvider");
            RefInvoke.setFieldOjbect("android.content.ContentProvider", "mContext", localProvider, app);
        }

        LogUtil.info( "app ===== "+app + "=====开始执行原Application");
        app.onCreate();
    }

    /**
     * 调用静态方法android.app.ActivityThread.currentActivityThread获取当前activity所在的线程对象
     * @return 当前ActivityThread对象
     */
    public static Object getCurrentActivityThread(){
        return RefInvoke.invokeStaticMethod("android.app.ActivityThread",
                "currentActivityThread", new Class[] {}, new Object[] {});
    }

    /**
     * 获取当前currentActivityThread的mBoundApplication属性对象，
     * 该对象是一个AppBindData类对象，该类是ActivityThread的一个内部类
     * @param currentActivityThread 当前ActivityThread对象
     * @return 返回AppBindData对象
     */
    private static Object getBoundApplication(Object currentActivityThread){
        if (currentActivityThread == null)
            return null;
        return RefInvoke.getFieldOjbect("android.app.ActivityThread",
                currentActivityThread, "mBoundApplication");
    }

    /**
     * 读取mBoundApplication中的info信息，info是LoadedApk对象
     * @param boundApplication AppBindData对象
     * @return LoadedApkInfo对象
     */
    private static Object getLoadApkInfoObj(Object boundApplication){
        return RefInvoke.getFieldOjbect("android.app.ActivityThread$AppBindData",
                boundApplication, "info");
    }
}
