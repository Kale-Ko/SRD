#include <jni.h>

inline jfieldID getFieldId(JNIEnv* env, jobject object, const char* name, const char* signature)
{
    jclass clazz = env->GetObjectClass(object);
    jfieldID fieldId = env->GetFieldID(clazz, name, signature);
    env->DeleteLocalRef(clazz);
    return fieldId;
}

inline jfieldID getStaticFieldId(JNIEnv* env, jclass clazz, const char* name, const char* signature)
{
    jfieldID fieldId = env->GetStaticFieldID(clazz, name, signature);
    return fieldId;
}

inline jmethodID getMethodId(JNIEnv* env, jobject object, const char* name, const char* signature)
{
    jclass clazz = env->GetObjectClass(object);
    jmethodID methodId = env->GetMethodID(clazz, name, signature);
    env->DeleteLocalRef(clazz);
    return methodId;
}

inline jmethodID getStaticMethodId(JNIEnv* env, jclass clazz, const char* name, const char* signature)
{
    jmethodID methodId = env->GetStaticMethodID(clazz, name, signature);
    return methodId;
}

inline jmethodID getConstructorId(JNIEnv* env, jclass clazz, const char* name, const char* signature)
{
    jmethodID methodId = env->GetMethodID(clazz, name, signature);
    return methodId;
}

inline void throwException(JNIEnv* env, const char* message)
{
    jclass clazz = env->FindClass("java/lang/RuntimeException");
    jstring messageObj = env->NewStringUTF(message);
    jthrowable exception = (jthrowable)env->NewObject(clazz, getConstructorId(env, clazz, "<init>", "(Ljava/lang/String;)V"), messageObj);
    env->DeleteLocalRef(clazz);
    env->Throw(exception);
}