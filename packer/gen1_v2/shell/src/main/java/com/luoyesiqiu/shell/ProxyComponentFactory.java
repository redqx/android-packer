 package com.luoyesiqiu.shell;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppComponentFactory;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.luoyesiqiu.shell.util.EnvUtils;
import com.luoyesiqiu.shell.util.FileUtils;

import java.lang.reflect.Method;
 @TargetApi(28)
public class ProxyComponentFactory extends AppComponentFactory
 {
    private static final String TAG = "dpt " + ProxyComponentFactory.class.getSimpleName();
    private static AppComponentFactory sAppComponentFactory;

    private String getTargetClassName(){
        return JniBridge.readAppComponentFactory();
    }


     @Override
     public Application instantiateApplication(@NonNull ClassLoader cl, @NonNull String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException
     {
         Log.d(TAG, "[ProxyComponentFactory](2) instantiateApplication() called with: cl = [" + cl + "], className = [" + className + "]");
         if(!Global.sIsReplacedClassLoader) {
             if(EnvUtils.getApplicationInfo() == null) {
                 throw new NullPointerException("application info is null");
             }
             String dataDir = EnvUtils.getApplicationInfo().dataDir;
             String sourceDir = EnvUtils.getApplicationInfo().sourceDir;
             FileUtils.unzipLibs(sourceDir,dataDir);
             JniBridge.loadShellLibs(dataDir,sourceDir);
         }
         JniBridge.init_app();

         AppComponentFactory targetAppComponentFactory = null;
         String applicationName = JniBridge.readApplicationName();
         if(!Global.sIsReplacedClassLoader){
             JniBridge.mergeDexElements(cl);
             Global.sIsReplacedClassLoader = true;
             targetAppComponentFactory = getTargetAppComponentFactory(cl);
         }
         else{
             targetAppComponentFactory = getTargetAppComponentFactory(cl);
         }

         Global.sNeedCalledApplication = false;

         if(targetAppComponentFactory != null) {
             try {
                 Method method = targetAppComponentFactory.getClass().getDeclaredMethod("instantiateApplication", ClassLoader.class, String.class);
                 Log.d(TAG, "[ProxyComponentFactory] instantiateApplication target applicationName = " + applicationName);

                 if(!TextUtils.isEmpty(applicationName)) {
                     Log.d(TAG, "[ProxyComponentFactory] instantiateApplication application name and AppComponentFactory specified");

                     return (Application) method.invoke(targetAppComponentFactory, cl, applicationName);
                 }
                 else{
                     Log.d(TAG, "[ProxyComponentFactory] instantiateApplication app does not specify application name");

                     return (Application) method.invoke(targetAppComponentFactory, cl,className);
                 }

             } catch (Exception e) {
                 Log.e(TAG,"instantiateApplication",e);
             }
         }

         //AppComponentFactory no specified
         if(!TextUtils.isEmpty(applicationName)) {

             Log.d(TAG, "[ProxyComponentFactory] instantiateApplication application name specified but AppComponentFactory no specified");
             return super.instantiateApplication(cl, applicationName);
         }
         else{

             Log.d(TAG, "[ProxyComponentFactory] instantiateApplication application name and AppComponentFactory no specified");
             return super.instantiateApplication(cl, className);
         }
     }
     @Override
     public Activity instantiateActivity(@NonNull ClassLoader cl, @NonNull String className, Intent intent) throws ClassNotFoundException, IllegalAccessException, InstantiationException
     {
         Log.d(TAG, "[ProxyComponentFactory] instantiateActivity() called with: cl = [" + cl + "], className = [" + className + "], intent = [" + intent + "]");

         AppComponentFactory targetAppComponentFactory = getTargetAppComponentFactory(cl);
         if(targetAppComponentFactory != null) {
             try {
                 Method method = AppComponentFactory.class.getDeclaredMethod("instantiateActivity", ClassLoader.class, String.class, Intent.class);
                 return (Activity) method.invoke(targetAppComponentFactory, cl, className, intent);

             } catch (Exception e) {
             }
         }
         return super.instantiateActivity(cl, className, intent);
     }

    private AppComponentFactory getTargetAppComponentFactory(ClassLoader appClassLoader)
    {
        if(sAppComponentFactory == null)
        {
            String targetClassName = getTargetClassName();
            Log.d(TAG,"[ProxyComponentFactory] targetClassName = " + targetClassName);
            if(!TextUtils.isEmpty(targetClassName)) {
                try {
                    sAppComponentFactory = (AppComponentFactory) Class.forName(targetClassName,true,appClassLoader).newInstance();
                    return sAppComponentFactory;
                } catch (Exception e) {
                }
            }
        }

        return sAppComponentFactory;
    }



    /**
     * This method add in Android 10
     */
    @Override
    public ClassLoader instantiateClassLoader(@NonNull ClassLoader cl, @NonNull ApplicationInfo aInfo)
    {
        Log.d(TAG, "[ProxyComponentFactory](1) instantiateClassLoader() called with: cl = [" + cl + "], aInfo = [" + aInfo + "]");
        FileUtils.unzipLibs(aInfo.sourceDir,aInfo.dataDir);
        JniBridge.loadShellLibs(aInfo.dataDir,aInfo.sourceDir);

        JniBridge.init_app();

        AppComponentFactory targetAppComponentFactory = getTargetAppComponentFactory(cl);

        JniBridge.mergeDexElements(cl);

        Global.sIsReplacedClassLoader = true;

        if(targetAppComponentFactory != null) {
            try {
                Method method = AppComponentFactory.class.getDeclaredMethod("instantiateClassLoader", ClassLoader.class, ApplicationInfo.class);
                return (ClassLoader) method.invoke(targetAppComponentFactory, cl, aInfo);

            } catch (Exception e) {
            }
        }
        return super.instantiateClassLoader(cl, aInfo);
    }

    @Override
    public BroadcastReceiver instantiateReceiver(@NonNull ClassLoader cl, @NonNull String className, Intent intent) throws ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        Log.d(TAG, "[ProxyComponentFactory] instantiateReceiver() called with: cl = [" + cl + "], className = [" + className + "], intent = [" + intent + "]");
        AppComponentFactory targetAppComponentFactory = getTargetAppComponentFactory(cl);

        if(targetAppComponentFactory != null) {
            try {
                Method method = AppComponentFactory.class.getDeclaredMethod("instantiateReceiver", ClassLoader.class, String.class, Intent.class);
                return (BroadcastReceiver) method.invoke(targetAppComponentFactory, cl, className, intent);

            } catch (Exception e) {
            }
        }
        return super.instantiateReceiver(cl, className, intent);
    }

    @Override
    public Service instantiateService(@NonNull ClassLoader cl, @NonNull String className, Intent intent)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        Log.d(TAG, "[ProxyComponentFactory] instantiateService() called with: cl = [" + cl + "], className = [" + className + "], intent = [" + intent + "]");
        AppComponentFactory targetAppComponentFactory = getTargetAppComponentFactory(cl);

        if(targetAppComponentFactory != null) {
            try {
                Method method = AppComponentFactory.class.getDeclaredMethod("instantiateService", ClassLoader.class, String.class, Intent.class);
                return (Service) method.invoke(targetAppComponentFactory, cl, className, intent);

            } catch (Exception e) {
            }
        }
        return super.instantiateService(cl, className, intent);
    }


    @Override
    public ContentProvider instantiateProvider(@NonNull ClassLoader cl, @NonNull String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        Log.d(TAG, "[ProxyComponentFactory] instantiateProvider() called with: cl = [" + cl + "], className = [" + className + "]");
        AppComponentFactory targetAppComponentFactory = getTargetAppComponentFactory(cl);
        if(targetAppComponentFactory != null) {
            try {
                Method method = AppComponentFactory.class.getDeclaredMethod("instantiateProvider", ClassLoader.class, String.class);
                return (ContentProvider) method.invoke(targetAppComponentFactory, cl, className);

            } catch (Exception e) {
            }
        }
        return super.instantiateProvider(cl, className);
    }

}
