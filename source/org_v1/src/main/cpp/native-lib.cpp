#include <jni.h>
#include <string>




extern "C"
JNIEXPORT jstring JNICALL
Java_com_packer_org_1v1_MainActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
    std::string hello = "[redqx]=>";
    return env->NewStringUTF(hello.c_str());
}