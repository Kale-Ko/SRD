#include <jni.h>
#include "desktopCapture.h"

#include <X11/Xlib.h>
#include <X11/Xutil.h>

inline jfieldID getFieldId(JNIEnv* env, jobject object, const char* field, const char* signature) {
    jclass clazz = env->GetObjectClass(object);
    jfieldID feildId = env->GetFieldID(clazz, field, signature);
    env->DeleteLocalRef(clazz);
    return feildId;
}

inline jfieldID getStaticFieldId(JNIEnv* env, jclass clazz, const char* field, const char* signature) {
    jfieldID feildId = env->GetStaticFieldID(clazz, field, signature);
    return feildId;
}

inline jmethodID getMethodId(JNIEnv* env, jobject object, const char* method, const char* signature) {
    jclass clazz = env->GetObjectClass(object);
    jmethodID methodId = env->GetMethodID(clazz, method, signature);
    env->DeleteLocalRef(clazz);
    return methodId;
}

inline jmethodID getStaticMethodId(JNIEnv* env, jclass clazz, const char* method, const char* signature) {
    jmethodID methodId = env->GetStaticMethodID(clazz, method, signature);
    return methodId;
}

int getRefCount(JNIEnv* env, jobject self) {
    jclass clazz = env->GetObjectClass(self);
    jfieldID fieldId = getStaticFieldId(env, clazz, "refCount", "I");
    jint value = env->GetStaticIntField(clazz, fieldId);
    env->DeleteLocalRef(clazz);
    return value;
}

int incrRefCount(JNIEnv* env, jobject self) {
    jclass clazz = env->GetObjectClass(self);
    jfieldID fieldId = getStaticFieldId(env, clazz, "refCount", "I");
    jint value = env->GetStaticIntField(clazz, fieldId);
    env->SetStaticIntField(clazz, fieldId, value + 1);
    env->DeleteLocalRef(clazz);
    return value;
}

int decrRefCount(JNIEnv* env, jobject self) {
    jclass clazz = env->GetObjectClass(self);
    jfieldID fieldId = getStaticFieldId(env, clazz, "refCount", "I");
    jint value = env->GetStaticIntField(clazz, fieldId);
    env->SetStaticIntField(clazz, fieldId, value - 1);
    env->DeleteLocalRef(clazz);
    return value - 1;
}

struct DisplayStruct {
    Display* display;

    jint screensCount;
    jlong* screensCache;
};

struct ScreenStruct {
    int number;
    Screen* screen;
    Window rootWindow;
};

/*
 * Class:     io_github_kale_ko_srd_cpp_DesktopCapture
 * Method:    connectDisplay
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_io_github_kale_1ko_srd_cpp_DesktopCapture_connectDisplay(JNIEnv* env, jobject self) {
    if (incrRefCount(env, self) == 0) {
        XInitThreads();
    }

    Display* display = XOpenDisplay(NULL);

    DisplayStruct* displayStruct = new DisplayStruct;
    displayStruct->display = display;
    displayStruct->screensCount = -1;
    displayStruct->screensCache = NULL;
    return (jlong)displayStruct;
}

/*
 * Class:     io_github_kale_ko_srd_cpp_DesktopCapture
 * Method:    disconnectDisplay
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_io_github_kale_1ko_srd_cpp_DesktopCapture_disconnectDisplay(JNIEnv* env, jobject self, jlong displayHandle) {
    DisplayStruct* displayStruct = (DisplayStruct*)displayHandle;

    XCloseDisplay(displayStruct->display);

    if (displayStruct->screensCache != NULL) {
        for (int i = 0; i < displayStruct->screensCount; i++) {
            delete (ScreenStruct*)displayStruct->screensCache[i];
        }
        delete displayStruct->screensCache;
    }
    delete displayStruct;

    if (decrRefCount(env, self) == 0) {
        XFreeThreads();
    }
}

/*
 * Class:     io_github_kale_ko_srd_cpp_DesktopCapture
 * Method:    getScreens
 * Signature: (J)[J
 */
JNIEXPORT jlongArray JNICALL Java_io_github_kale_1ko_srd_cpp_DesktopCapture_getScreens(JNIEnv* env, jobject self, jlong displayHandle) {
    DisplayStruct* displayStruct = (DisplayStruct*)displayHandle;

    int screensCount;
    jlong* screens;

    if (displayStruct->screensCache == NULL) {
        screensCount = XScreenCount(displayStruct->display);
        screens = new jlong[screensCount];

        for (int i = 0; i < screensCount; i++) {
            ScreenStruct* screenStruct = new ScreenStruct;
            screenStruct->number = i;
            screenStruct->screen = XScreenOfDisplay(displayStruct->display, i);
            screenStruct->rootWindow = XRootWindowOfScreen(screenStruct->screen);
            screens[i] = (jlong)screenStruct;
        }

        displayStruct->screensCount = screensCount;
        displayStruct->screensCache = screens;
    }
    else {
        screensCount = displayStruct->screensCount;
        screens = displayStruct->screensCache;
    }

    jlongArray jScreens = env->NewLongArray(screensCount);
    env->SetLongArrayRegion(jScreens, 0, screensCount, screens);
    return jScreens;
}

/*
 * Class:     io_github_kale_ko_srd_cpp_DesktopCapture
 * Method:    captureScreen
 * Signature: (J)[B
 */
JNIEXPORT jbyteArray JNICALL Java_io_github_kale_1ko_srd_cpp_DesktopCapture_captureScreen__J(JNIEnv* env, jobject self, jlong displayHandle) {
    DisplayStruct* displayStruct = (DisplayStruct*)displayHandle;

    Screen* screen = XDefaultScreenOfDisplay(displayStruct->display);
    Window screenWindow = XRootWindowOfScreen(screen);

    XImage* image = XGetImage(displayStruct->display, screenWindow, 0, 0, screen->width, screen->height, AllPlanes, ZPixmap);

    int bufferSize = image->width * image->height * (image->bits_per_pixel / 8);
    jbyte* buffer = new jbyte[bufferSize];

    switch (image->bits_per_pixel) {
    case 32:
        for (int i = 0; i < bufferSize; i += 4) {
            buffer[i] = image->data[i + 2];
            buffer[i + 1] = image->data[i + 1];
            buffer[i + 2] = image->data[i];
            buffer[i + 3] = image->data[i + 3];
        }
        break;
    case 24:
        for (int i = 0; i < bufferSize; i += 3) {
            buffer[i] = image->data[i + 2];
            buffer[i + 1] = image->data[i + 1];
            buffer[i + 2] = image->data[i];
            buffer[i + 3] = 0;
        }
        break;
    default:
        return NULL;
    }

    jbyteArray array = env->NewByteArray(bufferSize);
    env->SetByteArrayRegion(array, 0, bufferSize, buffer);
    delete buffer;
    return array;
}

/*
 * Class:     io_github_kale_ko_srd_cpp_DesktopCapture
 * Method:    captureScreen
 * Signature: (JJ)[B
 */
JNIEXPORT jbyteArray JNICALL Java_io_github_kale_1ko_srd_cpp_DesktopCapture_captureScreen__JJ(JNIEnv* env, jobject self, jlong displayHandle, jlong screenHandle) {
    DisplayStruct* displayStruct = (DisplayStruct*)displayHandle;
    ScreenStruct* screenStruct = (ScreenStruct*)screenHandle;

    XImage* image = XGetImage(displayStruct->display, screenStruct->rootWindow, 0, 0, screenStruct->screen->width, screenStruct->screen->height, AllPlanes, ZPixmap);

    int bufferSize = image->width * image->height * (image->bits_per_pixel / 8);
    jbyte* buffer = new jbyte[bufferSize];

    switch (image->bits_per_pixel) {
    case 32:
        for (int i = 0; i < bufferSize; i += 4) {
            buffer[i] = image->data[i + 2];
            buffer[i + 1] = image->data[i + 1];
            buffer[i + 2] = image->data[i];
            buffer[i + 3] = image->data[i + 3];
        }
        break;
    case 24:
        for (int i = 0; i < bufferSize; i += 3) {
            buffer[i] = image->data[i + 2];
            buffer[i + 1] = image->data[i + 1];
            buffer[i + 2] = image->data[i];
            buffer[i + 3] = 0;
        }
        break;
    default:
        return NULL;
    }

    jbyteArray array = env->NewByteArray(bufferSize);
    env->SetByteArrayRegion(array, 0, bufferSize, buffer);
    delete buffer;
    return array;
}