package com.example.by_java_packapk_v_2;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.util.Log;



public class Utils {

    public static String TAG = "Utils";

    public static int bytesToInt(byte[] src) {
        int value = 0;
        value = (src[0] & 0xFF) | ((src[1] & 0xFF)<<8) | ((src[2] & 0xFF)<<16) | ((src[3] & 0xFF)<<24);
        return value;
    }


    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +((ip >> 8) & 0xFF) + "." +((ip >> 16) & 0xFF) + "." +(ip >> 24 & 0xFF);
    }


    public static byte[] intToBytes( int value )
    {
        byte[] src = new byte[4];
        src[3] =  (byte) ((value>>24) & 0xFF);
        src[2] =  (byte) ((value>>16) & 0xFF);
        src[1] =  (byte) ((value>>8) & 0xFF);
        src[0] =  (byte) (value & 0xFF);
        return src;
    }

    public static boolean writeParamsFile(Context context,String ip,String username){
        boolean ret = false;

        try {
            String paramfn = context.getFilesDir() + "/params.dat";
            File paramFile = new File(paramfn);
            if (paramFile.exists()) {
                return true;
            }else{
                FileOutputStream fout = new FileOutputStream(paramFile);
                String dataString = "ip:" + ip + "\r\nusername:" + username;
                fout.write(dataString.getBytes());
                fout.close();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }

    public static boolean setValue(Context context,String filename,String key,String value){

        Log.e("preference", "setValue context:" + context.toString() + ",filename:" + filename + ",key:" + key + ",value:" + value);
        if (value == null || value.equals("") == true) {
            return false;
        }

        try {
            SharedPreferences sp = context.getSharedPreferences(filename, Context.MODE_PRIVATE);

            Editor editor = sp.edit();

            String defvalue = "";
            String oldvalue = sp.getString(key, defvalue);
            if (oldvalue != null && oldvalue.equals("") == false) {
                Log.e(TAG, "old value:" + oldvalue + ",new value:" + value + " key:" + key);
            }

            editor.putString(key, value);
            editor.commit();
            Log.e(TAG, "set value:" + value + " key:" + key);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static Context getContext(){
        try {
            Class<?> ActivityThread = Class.forName("android.app.ActivityThread");
            Method methodcat = ActivityThread.getMethod("currentActivityThread");
            Object currentActivityThread = methodcat.invoke(ActivityThread);
            Method methodga = currentActivityThread.getClass().getMethod("getApplication");
            Context context =(Context)methodga.invoke(currentActivityThread);
            if (context == null) {
                Log.e(TAG, "context null");
            }else{
                Log.e(TAG, "get context ok,package name:" + context.getPackageName()+"/class name:" + context.getClass().getName());
                return context;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public static int getSystemBits(){
        String[] abis = Build.SUPPORTED_ABIS;
        int size = abis.length;
        for (int i = 0;i < size ;i ++){
            if (abis[i].equals("arm64-v8a")){
                return 64;
            }
        }
        return 32;
    }

}
