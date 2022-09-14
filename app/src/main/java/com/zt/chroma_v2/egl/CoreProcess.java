package com.zt.chroma_v2.egl;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;

public class CoreProcess {
    private static final String TAG = "Livebox:CoreProcess";

    // Android-specific extension.
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private EGLDisplay mEglDisplay;
    private final EGLConfig mEglConfig;
    private EGLContext mEglContext;

    public CoreProcess(EGLContext shareEGLContext) {
        if (shareEGLContext == null) {
            shareEGLContext = EGL14.EGL_NO_CONTEXT;
        }

        //获取默认显示设备
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY){
            Log.e(TAG, "eglGetDisplay error = 0x" + Integer.toHexString(EGL14.eglGetError()));
        }

        //初始化显示设备
        //major：主版本 记录在 version[0]
        //minor : 子版本 记录在 version[1]
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            Log.e(TAG, "eglInitialize error = 0x" + Integer.toHexString(EGL14.eglGetError()));
        }

        //获取可用配置
        int[] config_attrib_list = {
                EGL14.EGL_BUFFER_SIZE,32,
                EGL14.EGL_RED_SIZE, 8, // 缓冲区中 红分量 位数
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR, //egl版本 3
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] num_config = new int[1];
        if (!EGL14.eglChooseConfig(mEglDisplay, config_attrib_list, 0,
                configs, 0, configs.length, num_config, 0)) {
            Log.e(TAG, "eglChooseConfig error = 0x" + Integer.toHexString(EGL14.eglGetError()));

        }
        mEglConfig = configs[0];

        //Create context
        int[] context_attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, //egl版本 3
                EGL14.EGL_NONE
        };
        mEglContext = EGL14.eglCreateContext(mEglDisplay, mEglConfig, shareEGLContext, context_attrib_list, 0);
        if (mEglContext == EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG, "eglCreateContext error = 0x" + Integer.toHexString(EGL14.eglGetError()));
        }

        //Confirm with query.
        final int[] values = new int[1];
        EGL14.eglQueryContext(mEglDisplay, mEglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0);
        Log.d(TAG, "EGLContext created, client version " + values[0]);
    }

    public void release() {
        if (mEglContext != null) {
            EGL14.eglDestroyContext(mEglDisplay, mEglContext);
            mEglContext = null;
        }

        if (mEglDisplay != null) {
            EGL14.eglTerminate(mEglDisplay);
            mEglDisplay = null;
        }
    }

    public void presentTime(EGLSurface eglSurface,long timestamp){
        EGLExt.eglPresentationTimeANDROID(mEglDisplay,eglSurface,timestamp);
    }

    public EGLSurface createWindowSurface(Object surface) {
        int[] surface_attrib_list = {
                EGL14.EGL_NONE
        };
        return EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, surface, surface_attrib_list, 0);
    }

    public void releaseWindowSurface(EGLSurface eglSurface) {
        if (eglSurface != null && eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(mEglDisplay, eglSurface);
        }
    }

    public EGLSurface createOffScreenSurface(int width, int height){
        //Surface属性设置
        int[] surfaceAttribs = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(mEglDisplay, mEglConfig, surfaceAttribs, 0); //创建离屏渲染Surface

        if (eglSurface == null) {
            throw new RuntimeException("surface was null");
        }
        return eglSurface;

    }

    public int getSurfaceWidth(EGLSurface eglSurface) {
        final int[] value = new int[1];
        EGL14.eglQuerySurface(mEglDisplay, eglSurface, EGL14.EGL_WIDTH, value, 0);
        Log.d(TAG, "getSurfaceWidth: " + value[0]);
        return value[0];
    }

    public int getSurfaceHeight(EGLSurface eglSurface) {
        final int[] value = new int[1];
        EGL14.eglQuerySurface(mEglDisplay, eglSurface, EGL14.EGL_HEIGHT, value, 0);
        Log.d(TAG, "getSurfaceHeight: " + value[0]);
        return value[0];
    }

    public void makeCurrent(EGLSurface eglSurface) {
        boolean result;
        if (eglSurface == null) {
            result = EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        } else {
            result = EGL14.eglMakeCurrent(mEglDisplay, eglSurface, eglSurface, mEglContext);
        }
        if (!result) {
            Log.e(TAG, "eglMakeCurrent error = 0x" +  Integer.toHexString(EGL14.eglGetError()));
        }
    }

    public void swapBuffers(EGLSurface eglSurface) {
        if (!EGL14.eglSwapBuffers(mEglDisplay, eglSurface)) {
            Log.e(TAG, "eglSwapBuffers error = 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
    }

    public EGLContext getContext() {
        return mEglContext;
    }
}
