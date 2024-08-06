 package com.luoyesiqiu.shell.util;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

 public class FileUtils
 {
    public static void close(Closeable closeable){
        if(closeable != null){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static String readAppName(Context context){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        InputStream in = null;
        try {
            in = context.getAssets().open("app_name");
            byte[] buf = new byte[1024];
            int len = -1;
            while((len = in.read(buf)) != -1){
                byteArrayOutputStream.write(buf,0,len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            close(byteArrayOutputStream);
            close(in);
        }
        return byteArrayOutputStream.toString();
    }
    public static void asset_copyfile(Context base,String src,String dest)
    {
        try{
            InputStream inputStream = base.getAssets().open(src);
            File targetFile = new File(dest);
            // 将文件从 assets 目录复制到目录 A
            OutputStream outputStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0)
            {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
     public static void setFilePermission(String filePath)
     {
         File file = new File(filePath);
         if (file.exists())
         {
             file.setReadable(true, false);
             file.setWritable(false, false);
             file.setExecutable(false, false);
         }
     }
}
