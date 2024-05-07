#include "desktopCapture-common.cpp"

#include "io_github_kale_ko_srd_cpp_DesktopCapture.h"
#include "io_github_kale_ko_srd_cpp_DesktopCapture_Screen.h"

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/extensions/Xrandr.h>

#include <vector>

/*
 * Class:     io_github_kale_ko_srd_cpp_DesktopCapture
 * Method:    create
 * Signature: ()Lio/github/kale_ko/srd/cpp/DesktopCapture;
 */
JNIEXPORT jobject JNICALL Java_io_github_kale_1ko_srd_cpp_DesktopCapture_n_1create(JNIEnv* env, jclass selfClazz) {
    Display* display = XOpenDisplay(NULL);
    if (display == NULL) {
        throwException(env, "Failed to open X11 display.");
        return NULL;
    }

    char* displayName = XDisplayString(display);
    if (displayName == NULL) {
        throwException(env, "Failed to fetch display name.");
        return NULL;
    }

    jstring displayNameObj = env->NewStringUTF(displayName);
    jobject object = env->NewObject(selfClazz, getConstructorId(env, selfClazz, "<init>", "(JLjava/lang/String;)V"), (jlong)display, displayNameObj);
    env->DeleteLocalRef(displayNameObj);
    return object;
}

/*
 * Class:     io_github_kale_ko_srd_cpp_DesktopCapture
 * Method:    destroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_github_kale_1ko_srd_cpp_DesktopCapture_n_1close(JNIEnv* env, jobject self) {
    Display* display = (Display*)env->GetLongField(self, getFieldId(env, self, "handle", "J"));

    XCloseDisplay(display);
}

/*
 * Class:     io_github_kale_ko_srd_cpp_DesktopCapture
 * Method:    n_getScreens
 * Signature: ()[Lio/github/kale_ko/srd/cpp/DesktopCapture/Screen;
 */
JNIEXPORT jobjectArray JNICALL Java_io_github_kale_1ko_srd_cpp_DesktopCapture_n_1getScreens(JNIEnv* env, jobject self) {
    Display* display = (Display*)env->GetLongField(self, getFieldId(env, self, "handle", "J"));

    std::vector<jobject> screens;
    jclass screenClazz = env->FindClass("io/github/kale_ko/srd/cpp/DesktopCapture$Screen");

    int xScreenCount = XScreenCount(display);
    for (int i = 0; i < xScreenCount; i++) {
        Screen* xScreen = XScreenOfDisplay(display, i);
        Window xWindow = XRootWindowOfScreen(xScreen);

        XRRScreenResources* xScreenResources = XRRGetScreenResources(display, xWindow);

        for (int j = 0; j < xScreenResources->noutput; j++) {
            XRROutputInfo* xOutputInfo = XRRGetOutputInfo(display, xScreenResources, xScreenResources->outputs[j]);

            bool isPrimary = XRRGetOutputPrimary(display, xWindow) == xScreenResources->outputs[j];

            if (xOutputInfo->connection == RR_Connected && xOutputInfo->crtc > 0) {
                XRRCrtcInfo* xCrtcInfo = XRRGetCrtcInfo(display, xScreenResources, xOutputInfo->crtc);

                jstring screenNameObj = env->NewStringUTF(xOutputInfo->name);
                jobject screenObj = env->NewObject(screenClazz, getConstructorId(env, screenClazz, "<init>", "(Lio/github/kale_ko/srd/cpp/DesktopCapture;JLjava/lang/String;IIIIZ)V"), self, (jlong)xScreen, screenNameObj, xCrtcInfo->x, xCrtcInfo->y, xCrtcInfo->width, xCrtcInfo->height, isPrimary);
                env->DeleteLocalRef(screenNameObj);
                screens.push_back(screenObj);

                XRRFreeCrtcInfo(xCrtcInfo);
            }

            XRRFreeOutputInfo(xOutputInfo);
        }

        XRRFreeScreenResources(xScreenResources);
    }

    jobjectArray screensArray = env->NewObjectArray(screens.size(), screenClazz, NULL);
    for (int i = 0; i < screens.size(); i++) {
        env->SetObjectArrayElement(screensArray, i, screens[i]);
    }
    env->DeleteLocalRef(screenClazz);
    return screensArray;
}

/*
 * Class:     io_github_kale_ko_srd_cpp_DesktopCapture_Screen
 * Method:    n_capture
 * Signature: ()Lio/github/kale_ko/srd/cpp/DesktopCapture/Screen/ScreenCapture;
 */
JNIEXPORT jobject JNICALL Java_io_github_kale_1ko_srd_cpp_DesktopCapture_00024Screen_n_1capture(JNIEnv* env, jobject self) {
    jobject desktopObj = env->GetObjectField(self, getFieldId(env, self, "desktop", "Lio/github/kale_ko/srd/cpp/DesktopCapture;"));
    Display* display = (Display*)env->GetLongField(desktopObj, getFieldId(env, desktopObj, "handle", "J"));
    Screen* screen = (Screen*)env->GetLongField(self, getFieldId(env, self, "handle", "J"));
    Window window = XRootWindowOfScreen(screen);

    jint x = env->GetIntField(self, getFieldId(env, self, "x", "I"));
    jint y = env->GetIntField(self, getFieldId(env, self, "y", "I"));
    jint width = env->GetIntField(self, getFieldId(env, self, "width", "I"));
    jint height = env->GetIntField(self, getFieldId(env, self, "height", "I"));

    XImage* image = XGetImage(display, window, x, y, width, height, AllPlanes, ZPixmap);
    jint imageSize = image->width * image->height * (image->bits_per_pixel / 8);

    jint dataSize = image->width * image->height * 4;
    jbyte* data = new jbyte[dataSize];
    switch ((image->bits_per_pixel / 8)) {
    case 4:
        for (int i = 0; i < imageSize; i += 4) {
            data[i] = image->data[i + 2];
            data[i + 1] = image->data[i + 1];
            data[i + 2] = image->data[i];
            data[i + 3] = image->data[i + 3];
        }
        break;
    case 3:
        for (int i = 0, j = 0; i < imageSize; i += 3, j += 4) {
            data[j] = image->data[i + 2];
            data[j + 1] = image->data[i + 1];
            data[j + 2] = image->data[i];
            data[j + 3] = 0;
        }
        break;
    default:
        throwException(env, "X11 returned invalid image bit-depth!");
    }

    XFree(image);

    jbyteArray dataObj = env->NewByteArray(dataSize);
    env->SetByteArrayRegion(dataObj, 0, dataSize, data);

    jclass captureClazz = env->FindClass("io/github/kale_ko/srd/cpp/DesktopCapture$Capture");
    jobject captureObj = env->NewObject(captureClazz, getConstructorId(env, captureClazz, "<init>", "(II[B)V"), width, height, dataObj);
    env->DeleteLocalRef(captureClazz);
    return captureObj;
}