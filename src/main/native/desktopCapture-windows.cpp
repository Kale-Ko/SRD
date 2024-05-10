#include "desktopCapture-common.cpp"

#include "io_github_kale_ko_srd_cpp_DesktopCapture.h"
#include "io_github_kale_ko_srd_cpp_DesktopCapture_Screen.h"

#include <windows.h>
#include <winuser.h>

#include <vector>

/*
 * Class:     io_github_kale_ko_srd_cpp_DesktopCapture
 * Method:    create
 * Signature: ()Lio/github/kale_ko/srd/cpp/DesktopCapture;
 */
JNIEXPORT jobject JNICALL Java_io_github_kale_1ko_srd_cpp_DesktopCapture_n_1create(JNIEnv* env, jclass selfClazz) {
    jstring displayNameObj = env->NewStringUTF("\\\\.");
    jobject object = env->NewObject(selfClazz, getConstructorId(env, selfClazz, "<init>", "(JLjava/lang/String;)V"), (jlong)1, displayNameObj);
    env->DeleteLocalRef(displayNameObj);
    return object;
}

/*
 * Class:     io_github_kale_ko_srd_cpp_DesktopCapture
 * Method:    destroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_github_kale_1ko_srd_cpp_DesktopCapture_n_1close(JNIEnv* env, jobject self) {
}

struct ScreenStruct {
    HMONITOR hMonitor;
    HDC hdcMonitor;
};

struct GetScreensProcStruct {
    std::vector<jobject>* screens;

    JNIEnv* env;
    jobject desktop;
    jclass screenClazz;
};

BOOL getScreensProc(HMONITOR hMonitor, HDC _hdcMonitor, LPRECT lprcMonitor, LPARAM dwData) {
    GetScreensProcStruct* procStruct = (GetScreensProcStruct*)dwData;

    MONITORINFOEX monitorInfo;
    monitorInfo.cbSize = sizeof(monitorInfo);
    GetMonitorInfo(hMonitor, &monitorInfo);

    HDC hdcMonitor = CreateDC("DISPLAY", monitorInfo.szDevice, NULL, NULL);
    bool isPrimary = (monitorInfo.dwFlags & 0x01) == 0x01;

    {
        ScreenStruct* screenStruct = new ScreenStruct;
        screenStruct->hMonitor = hMonitor;
        screenStruct->hdcMonitor = hdcMonitor;

        jstring screenNameObj = procStruct->env->NewStringUTF(monitorInfo.szDevice);
        jobject screenObj = procStruct->env->NewObject(procStruct->screenClazz, getConstructorId(procStruct->env, procStruct->screenClazz, "<init>", "(Lio/github/kale_ko/srd/cpp/DesktopCapture;JLjava/lang/String;IIIIZ)V"), procStruct->desktop, (jlong)screenStruct, screenNameObj, monitorInfo.rcMonitor.left, monitorInfo.rcMonitor.top, monitorInfo.rcMonitor.right - monitorInfo.rcMonitor.left, monitorInfo.rcMonitor.bottom - monitorInfo.rcMonitor.top, isPrimary);
        procStruct->env->DeleteLocalRef(screenNameObj);
        procStruct->screens->push_back(screenObj);
    }

    return TRUE;
}

/*
 * Class:     io_github_kale_ko_srd_cpp_DesktopCapture
 * Method:    n_getScreens
 * Signature: ()[Lio/github/kale_ko/srd/cpp/DesktopCapture/Screen;
 */
JNIEXPORT jobjectArray JNICALL Java_io_github_kale_1ko_srd_cpp_DesktopCapture_n_1getScreens(JNIEnv* env, jobject self) {
    std::vector<jobject> screens;
    jclass screenClazz = env->FindClass("io/github/kale_ko/srd/cpp/DesktopCapture$Screen");

    {
        GetScreensProcStruct procStruct = { .screens = &screens, .env = env, .desktop = self, .screenClazz = screenClazz };
        EnumDisplayMonitors(NULL, NULL, getScreensProc, (LPARAM)&procStruct);
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
    ScreenStruct* screenStruct = (ScreenStruct*)env->GetLongField(self, getFieldId(env, self, "handle", "J"));

    jint x = env->GetIntField(self, getFieldId(env, self, "x", "I"));
    jint y = env->GetIntField(self, getFieldId(env, self, "y", "I"));
    jint width = env->GetIntField(self, getFieldId(env, self, "width", "I"));
    jint height = env->GetIntField(self, getFieldId(env, self, "height", "I"));

    DWORD bmpSize = ((width * 24 + 31) / 32) * 4 * height;
    jbyte* bmpData = new jbyte[bmpSize];

    {
        HDC hdcMem = CreateCompatibleDC(screenStruct->hdcMonitor);
        HBITMAP hBitmap = CreateCompatibleBitmap(screenStruct->hdcMonitor, width, height);
        SelectObject(hdcMem, hBitmap);

        BitBlt(hdcMem, 0, 0, width, height, screenStruct->hdcMonitor, 0, 0, SRCCOPY);

        BITMAPINFOHEADER bmiHeader;
        bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
        bmiHeader.biWidth = width;
        bmiHeader.biHeight = height;
        bmiHeader.biPlanes = 1;
        bmiHeader.biBitCount = 24;
        bmiHeader.biCompression = BI_RGB;
        bmiHeader.biSizeImage = 0;

        GetDIBits(hdcMem, hBitmap, 0, height, bmpData, (BITMAPINFO*)&bmiHeader, DIB_RGB_COLORS);

        DeleteObject(hBitmap);
        DeleteDC(hdcMem);
    }

    jint dataSize = width * height * 4;
    jbyte* data = new jbyte[dataSize];
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            int srcIndex = ((height - 1 - y) * width + x) * 3;
            int destIndex = (y * width + x) * 4;

            data[destIndex] = bmpData[srcIndex + 2];
            data[destIndex + 1] = bmpData[srcIndex + 1];
            data[destIndex + 2] = bmpData[srcIndex];
            data[destIndex + 3] = 255;
        }
    }
    delete bmpData;

    jbyteArray dataObj = env->NewByteArray(dataSize);
    env->SetByteArrayRegion(dataObj, 0, dataSize, data);
    delete data;

    jclass captureClazz = env->FindClass("io/github/kale_ko/srd/cpp/DesktopCapture$Capture");
    jobject captureObj = env->NewObject(captureClazz, getConstructorId(env, captureClazz, "<init>", "(II[B)V"), width, height, dataObj);
    env->DeleteLocalRef(captureClazz);
    return captureObj;
}

JNIEXPORT void JNICALL Java_io_github_kale_1ko_srd_cpp_DesktopCapture_00024Screen_n_1close(JNIEnv* env, jobject self) {
    ScreenStruct* screenStruct = (ScreenStruct*)env->GetLongField(self, getFieldId(env, self, "handle", "J"));

    ReleaseDC(NULL, screenStruct->hdcMonitor);
    delete screenStruct;

    return;
}