package com.luoyesiqiu.shell;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;

import com.luoyesiqiu.shell.util.EnvUtils;

import java.io.File;

/**
 * Created by luoyesiqiu
 */
@Keep
public class JniBridge
{
//    static {
//        System.loadLibrary("dpt");
//    }

    //static JNINativeMethod gMethods[] = {
//        {"craoc", "(Ljava/lang/String;)V",                               (void *) callRealApplicationOnCreate},
//        {"craa",  "(Landroid/content/Context;Ljava/lang/String;)V",      (void *) callRealApplicationAttach},
//        {"ia",    "()V", (void *) init_app},
//        {"gap",   "()Ljava/lang/String;",         (void *) getApkPathExport},
//        {"gdp",   "()Ljava/lang/String;",         (void *) getCompressedDexesPathExport},
//        {"rcf",   "()Ljava/lang/String;",         (void *) readAppComponentFactory},
//        {"rapn",   "()Ljava/lang/String;",         (void *) readApplicationName},
//        {"mde",   "(Ljava/lang/ClassLoader;)V",        (void *) mergeDexElements},
//        {"rde",   "(Ljava/lang/ClassLoader;Ljava/lang/String;)V",        (void *) removeDexElements},
//        {"ra", "(Ljava/lang/String;)Ljava/lang/Object;",                               (void *) replaceApplication}
//};
    private static final String TAG = "dpt_" + JniBridge.class.getSimpleName();
    public static native void /*craoc*/callRealApplicationOnCreate(String applicationClassName);
    public static native void /*craa*/callRealApplicationAttach(Context context, String applicationClassName);
    public static native void /*ia*/init_app();
    public static native String /*rcf*/readAppComponentFactory();
    public static native void /*mde*/mergeDexElements(ClassLoader targetClassLoader);
//    public static native void /*rde*/removeDexElements(ClassLoader classLoader,String elementName);
//    public static native String /*gap*/getApkPathExport();
//    public static native String /*gdp*/getCompressedDexesPathExport();
    public static native Object /*ra*/replaceApplication(String originApplicationClassName);
    public static native String /*rapn*/readApplicationName();

    public static void loadShellLibs(String workspacePath,String apkPath)
    {
        Log.d(TAG, "start  System.Libiary()");
        final String[] allowLibNames = {Global.SHELL_SO_NAME};
        try {

            String abiDirName = EnvUtils.getAbiDirName(apkPath);
            File shellLibsFile = new File(workspacePath + File.separator + Global.LIB_DIR + File.separator + abiDirName);
            File[] files = shellLibsFile.listFiles();
            if(files != null)
            {
                for(File shellLibPath : files)
                {
                    String fullLibPath = shellLibPath.getAbsolutePath();
                    for(String libName : allowLibNames)
                    {
                        String libSuffix = File.separator + libName;
                        if(fullLibPath.endsWith(libSuffix))
                        {
                            Log.d(TAG, "loadShellLibs: " + fullLibPath);
                            System.load(fullLibPath);
                        }
                    }
                }
            }
        }
        catch (Throwable e)
        {
            Log.w(TAG,e);
        }
    }

}
