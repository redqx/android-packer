#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>

#define mmTAG "pack_it"

/***
 * 获取dex文件并解密返回dex文件数据
 * @param env
 * @param thiz
 * @return
 */
jbyteArray getDex(JNIEnv *env,jobject thiz);

/***
 * 获取lib文件搜索目录
 * @param env
 * @param thiz
 * @return
 */
jstring getSearchDir(JNIEnv *env,jobject thiz);

/****
 * 将dex文件数据封装成ByteBuffer数组
 * @param env
 * @param thiz
 * @param dex
 * @return
 */
jobjectArray getDexBuffers(JNIEnv *env,jobject thiz,jbyteArray dex);

/***
 * 创建DexClassLoader对象
 * @param env
 * @param thiz
 * @param dexBuffers dex文件数据
 * @param searchDir so库搜索目录
 * @param cls_loader 当前类加载器
 * @return
 */
jobject newDexClassLoader(JNIEnv *env,jobject thiz,jobjectArray dexBuffers,jstring searchDir,jobject cls_loader);

/****
 * 替换当前类加载器的dex文件
 * @param env
 * @param thiz
 * @param arg_dex
 * @param arg_classLoader
 * @param arg_base
 */
void entryApp(JNIEnv *env, jobject thiz, jobject arg_dex, jobject arg_classLoader, jobject arg_base);


extern "C"
JNIEXPORT void JNICALL
Java_com_example_gen1_1v1_1native_ProxyApplication_loadApp(JNIEnv *env, jobject thiz,
                                                           jobject cls_loader, jobject base) {

    jbyteArray dex_data=getDex(env,thiz);//从assets目录读取class.dex文件,然后解密它
    jstring searchDir= getSearchDir(env,thiz);//获取so库搜索目录
    jobjectArray dexBuffers= getDexBuffers(env,thiz,dex_data);
    jobject objDexClassLoader= newDexClassLoader(env,thiz,dexBuffers,searchDir,cls_loader);

    entryApp(env,thiz,objDexClassLoader,cls_loader,base);
}


/***
 * 获取dex文件并解密返回dex文件数据
 * @param env
 * @param thiz
 * @return
 */
jbyteArray getDex(JNIEnv *env,jobject thiz)
{
    jclass clsContextWrapper= env->FindClass("android/content/ContextWrapper");
    if(clsContextWrapper==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();

        __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                            "FindClass faild android/content/ContextWrapper");
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "FindClass android/content/ContextWrapper %p",clsContextWrapper);

    jmethodID  mthgetAssets=env->GetMethodID(clsContextWrapper,"getAssets",
                                             "()Landroid/content/res/AssetManager;");

    if(mthgetAssets==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "find func=>getAssets %p",mthgetAssets);

    jobject objAssets= env->CallObjectMethod(thiz,mthgetAssets);
    if(objAssets==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "objAssets %p",objAssets);

    jclass clsAssetManager= env->FindClass("android/content/res/AssetManager");
    if(clsAssetManager==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "clsAssetManager %p",clsAssetManager);

    jmethodID  m_open=env->GetMethodID(clsAssetManager,"open",
                                       "(Ljava/lang/String;)Ljava/io/InputStream;");
    if(m_open==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "m_open %p",objAssets);

    //打开classes.dex
    jobject  obj_inputstream= env->CallObjectMethod(objAssets,m_open,env->NewStringUTF("classes3.dex"));
    if(obj_inputstream==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "obj_inputstream %p",obj_inputstream);


    jclass cls_InputStream= env->FindClass("java/io/InputStream");
    if(cls_InputStream==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "cls_InputStream %p",clsAssetManager);

//    //java8的环境无法执行下面的代码
//    jmethodID  m_readNBytes=env->GetMethodID(cls_InputStream,"readNBytes",
//                                             "(I)[B");
//    if(m_readNBytes==NULL || env->ExceptionCheck())
//    {
//        env->ExceptionDescribe();
//        return nullptr;
//    }
//    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
//                        "m_readNBytes %p",m_readNBytes);

//    //读取class.dex
//    jbyteArray dexData= static_cast<jbyteArray>(env->CallObjectMethod(obj_inputstream, m_readNBytes,
//                                                                      (jint) 0x10000000));

//    jsize dexBuffSize= env->GetArrayLength(dexData);
//    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
//                        "dexBuffSize %d",dexBuffSize);

    jmethodID  m_read=env->GetMethodID(cls_InputStream,"read","([BII)I");
    if(m_read==NULL || env->ExceptionCheck()){
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,"m_read %p",m_read);

    jmethodID  m_available=env->GetMethodID(cls_InputStream,"available","()I");
    if(m_available==NULL || env->ExceptionCheck()){
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,"m_available %p",m_available);

    jint availableSize=env->CallIntMethod(obj_inputstream, m_available);
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,"availableSize %d",availableSize);

    jbyteArray dexData=env->NewByteArray(availableSize);
    jint dexBuffSize=env->CallIntMethod(obj_inputstream, m_read,dexData,0,availableSize);


    jbyte* data= env->GetByteArrayElements(dexData,JNI_FALSE);

//    for(int i=0;i<dexBuffSize;i++)
//    {
//        //*(data+i)=(*(data+i))^48;//数据解密
//        *(data+i)=(*(data+i));//数据解密
//    }
    env->SetByteArrayRegion(dexData,0,dexBuffSize,data);

    if(dexData==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "dexData %p",dexData);

    jmethodID  m_close=env->GetMethodID(cls_InputStream,"close",
                                        "()V");
    if(m_close==NULL || env->ExceptionCheck()){
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "m_close %p",m_close);

    env->CallVoidMethod(obj_inputstream,m_close);
    if( env->ExceptionCheck()){
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "m_close call ok");

    return dexData;
}

jstring getSearchDir(JNIEnv *env,jobject thiz)
{
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "Java_com_example_pack_PackApp_loadApp");

    jclass clsContextWrapper= env->FindClass("android/content/ContextWrapper");
    if(clsContextWrapper==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "clsContextWrapper %p",clsContextWrapper);

    jmethodID  m_getApplicationInfo=env->GetMethodID(clsContextWrapper,"getApplicationInfo",
                                                     "()Landroid/content/pm/ApplicationInfo;");

    if(m_getApplicationInfo==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return nullptr;
    }

    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "m_getApplicationInfo %p",m_getApplicationInfo);

    jobject obj_ApplicationInfo= env->CallObjectMethod(thiz,m_getApplicationInfo);
    if(obj_ApplicationInfo==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "obj_ApplicationInfo %p",obj_ApplicationInfo);


    jclass cls_ApplicationInfo= env->FindClass("android/content/pm/ApplicationInfo");
    if(cls_ApplicationInfo==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "cls_ApplicationInfo %p",cls_ApplicationInfo);

    jfieldID f_sourceDir=env->GetFieldID(cls_ApplicationInfo,"sourceDir",
                                         "Ljava/lang/String;");

    if(f_sourceDir==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return nullptr;
    }

    jstring  sourceDir= static_cast<jstring>(env->GetObjectField(obj_ApplicationInfo,f_sourceDir));
    if(sourceDir==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return nullptr;
    }
    const char* dir=env->GetStringUTFChars(sourceDir,JNI_FALSE);

    char searchDir[1000];
    memset(searchDir,0,1000);

    strcpy(searchDir,dir);

    const char* libDir="!/lib/x86_64";
    strcat(searchDir,libDir);

    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "nativeDir %p %s",sourceDir,searchDir);
    return env->NewStringUTF(searchDir);
}

jobjectArray getDexBuffers(JNIEnv *env,jobject thiz,jbyteArray dex)
{
    jclass cls_ByteBuffer = env->FindClass("java/nio/ByteBuffer");
    if(cls_ByteBuffer==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "cls_ByteBuffer %p",cls_ByteBuffer);

    jmethodID m_wrap =env->GetStaticMethodID(cls_ByteBuffer,"wrap", "([B)Ljava/nio/ByteBuffer;");
    if(m_wrap==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "m_wrap %p",m_wrap);

    jobject dexBuffer= env->CallStaticObjectMethod(cls_ByteBuffer,m_wrap,dex);
    if(dexBuffer==NULL || env->ExceptionCheck()){
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "dexBuffer %p",dexBuffer);

    jobjectArray dexBuffers= env->NewObjectArray(1,cls_ByteBuffer,0);
    env->SetObjectArrayElement(dexBuffers,0,dexBuffer);
    if(env->ExceptionCheck()){
        env->ExceptionDescribe();
        return nullptr;
    }
    return dexBuffers;
}

jobject newDexClassLoader(JNIEnv *env,jobject thiz,jobjectArray dexBuffers,jstring searchDir,jobject cls_loader)
{
    jclass  cls_InMemoryDexClassLoader= env->FindClass("dalvik/system/InMemoryDexClassLoader");
    if(cls_InMemoryDexClassLoader==NULL || env->ExceptionCheck()){
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "cls_InMemoryDexClassLoader %p",cls_InMemoryDexClassLoader);


    jmethodID m_InMemoryDexClassLoader= env->GetMethodID(cls_InMemoryDexClassLoader,"<init>",
                                                         "([Ljava/nio/ByteBuffer;Ljava/lang/String;Ljava/lang/ClassLoader;)V");

    jobject obj_InMemoryDexClassLoader= env->NewObject(cls_InMemoryDexClassLoader,m_InMemoryDexClassLoader,dexBuffers,
                                                       searchDir,cls_loader);
    if(obj_InMemoryDexClassLoader==NULL || env->ExceptionCheck()){
        env->ExceptionDescribe();
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "obj_InMemoryDexClassLoader %p",obj_InMemoryDexClassLoader);
    return obj_InMemoryDexClassLoader;
}

void entryApp(JNIEnv *env, jobject thiz, jobject arg_dex, jobject arg_classLoader, jobject arg_base){

    jclass cls_ClassLoader= env->FindClass("java/lang/ClassLoader");
    if(cls_ClassLoader==NULL || env->ExceptionCheck()){
        env->ExceptionDescribe();
        return;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "cls_ClassLoader %p",cls_ClassLoader);

    jmethodID m_loadClass= env->GetMethodID(cls_ClassLoader,"loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    if(m_loadClass==NULL || env->ExceptionCheck()){
        env->ExceptionDescribe();
        return;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "m_loadClass %p",m_loadClass);

    jclass cls_ContextImpl=(jclass)env->CallObjectMethod(arg_classLoader, m_loadClass,
                                                         env->NewStringUTF("android.app.ContextImpl"));
    if(cls_ContextImpl==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "cls_ContextImpl %p",cls_ContextImpl);


    jfieldID f_mPackageInfo= env->GetFieldID(cls_ContextImpl,"mPackageInfo","Landroid/app/LoadedApk;");
    if(f_mPackageInfo==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "f_mPackageInfo %p",f_mPackageInfo);


    jobject obj_mPackageInfo= env->GetObjectField(arg_base, f_mPackageInfo);
    if(obj_mPackageInfo==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "obj_mPackageInfo %p",obj_mPackageInfo);


    jclass cls_LoadedApk=(jclass)env->CallObjectMethod(arg_classLoader, m_loadClass,
                                                       env->NewStringUTF("android.app.LoadedApk"));
    if(cls_LoadedApk==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "cls_LoadedApk %p",cls_LoadedApk);

    jfieldID f_mClassLoader= env->GetFieldID(cls_LoadedApk,"mClassLoader","Ljava/lang/ClassLoader;");
    if(f_mClassLoader==NULL || env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        return;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "f_mClassLoader %p",f_mClassLoader);

    jobject obj_mClassLoader= env->GetObjectField(obj_mPackageInfo,f_mClassLoader);
    if(obj_mClassLoader==NULL || env->ExceptionCheck()){
        env->ExceptionDescribe();
        return;
    }
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "obj_mClassLoader %p",obj_mClassLoader);

    env->SetObjectField(obj_mPackageInfo, f_mClassLoader, arg_dex);
    __android_log_print(ANDROID_LOG_DEBUG,mmTAG,
                        "replace dex ok");
}
