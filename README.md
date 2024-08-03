

# 工程项目介绍

学习安卓加壳的原理, 主要是学习如何动态加载,不涉及加密的处理

```
app(com.example.pack)
gen1_v1
gen1_v1_1
gen1_v1_native
gen1_v2
```



>  app 

原始的待加壳项目, 主要功能是activity显示一个图片, 布局中有个按钮

![image-20240803145115909](https://raw.githubusercontent.com/redqx/pack/master/img/image-20240803145115909.png)

MainActivity内容如下

```java
package com.example.pack;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;



public class MainActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Log.i("source", "[source-onCreate] =>i am source apk" );
        findViewById(R.id.bt1).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(MainActivity.this, "功德无量", Toast.LENGTH_SHORT).show();
        }
    });

    }
}

```



其中 com.example.pack 项目自定义了一个application类,并在AndroidManifest.xml中注册了

```java
package com.example.pack;

import android.app.Application;
import android.util.Log;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("source", "[source-Application]=>i am source apk" );
    }
}
/*
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Pack"
        tools:targetApi="31"
        android:name="com.example.pack.MyApplication"> 注册
        <activity
            android:name="com.example.pack.MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
*/
```



##  一代壳学习: 动态加载dex



### 1), **gen1_v1**

来源于项目[simplepack:一个简单完整的Android apk壳程序](https://github.com/dreamxgn/simplepack)的学习, 大多数是copy代码,然后理解代码

项目用native实现的,我转成了java实现



核心原理|步骤:

写一个gen1_v1的项目, 功能及代码和com.example.pack项目差不多.

然后生成apk文件,提取其中的dex文件, 有很多dex, 只提取我们写的逻辑对应的dex, 也就是MainActivity的classes.dex

然后删除gen1_v1\src\main\java\com\example\gen1_v1所有java文件,, 添加ProxyApplication.java, 并实现ProxyApplication的内容

在gen1_v1的AndroidManifest.xml中添加 `android:name=".ProxyApplication"`

![image-20240803150200067](https://raw.githubusercontent.com/redqx/pack/master/img/image-20240803150200067.png)



ProxyApplication是自定义的Application, 会在很早的时机执行

我们重写Application.attachBaseContext(Context **base**), 实现自定义的dex文件加载

核心步骤是替换ContextImpl.mPackageInfo.mClassLoader成员

其中ContextImpl类继承于Context 

所以我们要做的就是替换**base**.mPackageInfo.mClassLoader=dexClassLoader. 

```
dexClassLoader=new InMemoryDexClassLoader( //安卓8.0引入的api,允许从内存加载dex内容
        dexBuffer_Arr, //含有dex文件内容的数组
        searchDir,//so的搜索路径
        this.getClassLoader());//父类的ClassLoader
```

然后就完成了.  结果: 实验成功, 可以达到原始效果,

缺点之一: 

- 原始的`com.example.pack.MyApplication`没有得到执行





### 2), **gen1_v1_native**

仍然来源于项目[simplepack:一个简单完整的Android apk壳程序](https://github.com/dreamxgn/simplepack)的学习,  

项目是native实现的,我就copy代码, 理解代码. 修改了一下代码.

结果: 实验成功, 可以达到原始效果,

### 3), **gen1_v1_1**

来源于项目[apkjiagu-对apk进行加固](https://github.com/zhang-hai/apkjiagu)的学习, 理解代码并转换为自己的工程

在**gen1_v1**项目中, 我们只提取并加载了一个dex文件, 于是**gen1_v1_1**是提取并加载原始的所有dex文件

首先和之前操作一样,写一个和com.example.pack项目类似的**gen1_v1_1**, 生成apk文件. 把原始的所有dex文件打包为dex.zip

后续步骤:  (有点多,有点复杂) , 原理建立于之前的项目以及知识点之上



A). 在**gen1_v1_1**添加2个内容, 一个是自定义ProxyApplication类的注册, 一个是子类的Application信息添加

![image-20240803153726749](https://raw.githubusercontent.com/redqx/pack/master/img/image-20240803153726749.png)

B),  首先定位到对象  ActivityThreadClass.sCurrentActivityThread, 我们暂时把它命名为Y

ps: 括号的意思表示当前类的类型, 比如mBoundApplication(AppBindData)表示mBoundApplication的类型是AppBindData

然后就是2个大步骤

```

一), 重写Application.attachBaseContext

拿到Y.mPackages(ArrayMap<String, WeakReference<LoadedApk>>)
通过mPackages拿到的currentPackageName(String)的weakReference, weakReference是一个LoadApk类型的弱引用
Object obj_loadedApk = weakReference.get();
然后修改obj_loadedApk.mClassLoader为自定义的dexClassLoader

二), 重写Application.onCreate

1.把Y.mBoundApplication(AppBindData).info(LoadedApk).mApplication(Application)设置为null
2.从Y.mAllApplications(ArrayList<Application>)中移除Y.mInitialApplication(Application)
3.修改把Y.mBoundApplication(AppBindData).info(LoadedApk).mApplicationInfo(ApplicationInfo).classname为源apk的ApplicationInfo的name
4.把Y.mBoundApplication(AppBindData).appInfo(ApplicationInfo).classname为源apk的ApplicationInfo的name
5.把Y.mInitialApplication(Application)设置为我们的新创建的new_org_Application(Application)
6.定位到Y.mProviderMap, 把mProviderMap里的每一个providerClientRecord.f_mLocalProvider.mContext设置为new_org_Application
7.执行new_org_Application.onCreate();

```

ps: 

在DexClassLoader dLoader = new DexClassLoader(dexFilePath, odexPath, libPath, mClassLoader);

dexFilePath是dex.zip的相关路径,比如是/data/user/0/com.example.gen1_v1_1/app_payload_odex/dex.zip

odexPath是/data/user/0/com.example.gen1_v1_1/app_payload_odex

libPath是当前apk的so路径, 比如是/data/app/~~Ox_xqQ1MVFd4RhrEVU11nw==/com.example.gen1_v1_1-2LnyLhAg4VWyVgdTQkUJzg==/lib/x86_64



步骤流程差不多就这样, 原理什么的和apk加载流程有关, 不怎么懂 , 但是按部就班抄流程还是会的

然后就完成了.  结果: 实验成功, 可以达到原始效果, 原始apk的Application得到执行



### 4), **gen1_v2**

这是加载完整的apk了, 不是加载纯粹的dex.zip或者dex文件

来源于项目 [一个很老的项目: 找不到原始项目,这个应该是别人fork的](https://github.com/tangsilian/SecurityPage/tree/master/%E7%A0%B4%E8%A7%A3%E5%BA%94%E7%94%A8%E7%9A%84%E5%AD%A6%E4%B9%A0/%E5%8A%A0%E5%A3%B3%E5%89%8D%E5%90%8E%E7%9A%84%E6%96%87%E4%BB%B6/Android%E4%B8%ADApk%E5%8A%A0%E5%9B%BA), 好像是来自书籍 "Android软件安全与逆向分析"

这个项目很老, 以至于我猜测,( gen1_v1, gen1_v1_1,gen1_v1_native)的相关原理都来自该项目, 



为什么之前的项目中我都要在当前项目**(命名为A)**写一个和com.example.pack类似的项目,然后生成apk, 提取dex.

之后删除项目A的所有java文件,并添加ProxyApplication?, 命名为项目B.

ps: 项目A和项目B都是相同的项目,只不过内容不同

因为之后的项目B生成的apk自带原来项目A的apk所需要的资源. 所有之后加载dex时,不需要再做资源处理.

貌似资源处理是一个很麻烦的东西, 网上没看到没人实现并公布出来, 或者我自己没搜到.



于是gen1_v2和之前的项目类似又不一样, 

比如gen1_v2直接拿着com.example.pack生成的apk加载,  之后的处理流程和以前的项目一样

在Application.onCreate()中多了资源的加载, 只不过这个操作貌似现在看来,无效了....

2018的项目到2024年现在来说, 该资源加载的代码好像不起作用了



所以本项目我在实验时,以失败告终.

失败表现为: 加载com.example.pack项目的apk时,运行它的activity找不到资源.



一开始我也处理了一些报错, 比如我把com.example.pack项目的MainActivity继承于`extends Activity`, 而不是`extends AppCompact`

然后取消com.example.pack项目中资源的调用, 比如findviewbyid就是加载资源,

于是把com.example.pack项目修改为了

```java
package com.example.pack;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Log.i("source", "[source-onCreate] =>i am source apk" );
    }
}

```

然后再去加载,就发现出现了一个莫名其妙的activity布局,(还包含一个按钮,不知道哪里来的activity)

![image-20240803163322917](https://raw.githubusercontent.com/redqx/pack/master/img/image-20240803163322917.png)