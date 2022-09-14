package com.zt.chroma_v2.egl;

import android.opengl.EGLContext;
import android.util.Log;

public class RenderThread extends Thread {
    private static final String TAG = "Livebox:RenderThread";
    private String threadName;
    private final Object mGetContextObject;
    private final Object mDrawFrameObject;
    private EGLContext mShareEGLContext;
    public Renderer mRenderer;
    private int mRenderMode;
    private boolean mExitThread;
    private boolean mSurfaceCreated;
    private boolean mSurfaceChanged;
    private boolean ismSurfaceCreatedFlag;
    private boolean mReadyToDraw;
    private int mRenderOutputWidth;
    private int mRenderOutputHeight;

    private CoreProcess coreProcess;

    public int[] mOutFboID;
    public int[] mInputFboID;

    public final static int RENDERMODE_WHEN_DIRTY = 0;
    public final static int RENDERMODE_CONTINUOUSLY = 1;

    public boolean isSetInputFBOReady=true;
    public RenderThread mLastRenderThread;
    public boolean drawFinish = false;
    public boolean lastDrawFinish = false;

    private Object mCreateLock;

    private boolean freeze = false;

    public RenderThread(EGLContext shareEGLContext, Object createLock) {
        super();
        mShareEGLContext = shareEGLContext;
        mGetContextObject = new Object();
        mDrawFrameObject = new Object();
        mExitThread = false;
        mCreateLock = createLock;
    }

    public EGLContext getContext() {
        synchronized (mGetContextObject) {
            while (mShareEGLContext == null) {
                try {
                    mGetContextObject.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return mShareEGLContext;
        }
    }

    public boolean isCreateSurface(){
        return ismSurfaceCreatedFlag;
    }

    public void setRender(Renderer renderer) {
        mRenderer = renderer;
    }

    public void setRenderMode(int mode) {
        mRenderMode = mode;
    }

    public void setThreadName(String name){
        threadName = name;
    }

    public synchronized void setSurfaceCreated(){
        Log.d(TAG, threadName + "setSurfaceCreated");
        mSurfaceCreated = true;
    }

    public synchronized void setSurfaceChanged(int width, int height){
        Log.d(TAG, threadName + "setSurfaceChanged");
        mRenderOutputWidth = width;
        mRenderOutputHeight = height;
        mSurfaceChanged = true;
    }

    public synchronized void setSurfaceDestroyed() {
        Log.d(TAG, threadName + "setSurfaceDestroyed");
        mReadyToDraw = false;
        mExitThread = true;
        requestRender();
        try {
            join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void requestRender() {
        // 同步锁打开释放控制run()
        synchronized (mDrawFrameObject) {
            mDrawFrameObject.notify();
        }
    }

    public void setLastRenderThread(RenderThread renderThread){
        mLastRenderThread = renderThread;
    }

    public void setInputFboID(int[] inputFboID){
        mInputFboID = inputFboID;
    }

    public int[] getOutFboID(){
        return mOutFboID;
    }

    public void freezeRender(){
        freeze = true;
    }

    public void unFreezeRender(){
        freeze = false;
    }

    @Override
    public void run() {
        setName("RenderT-" + threadName + getId());
        Log.d(TAG, "RenderThread " + getId() + " start!");
        coreProcess = new CoreProcess(mShareEGLContext);
        synchronized (mGetContextObject) {
            if (mShareEGLContext == null) {
                mShareEGLContext = coreProcess.getContext();
                mGetContextObject.notifyAll();
            }
        }

        while (!mExitThread) {
            if (freeze){
                continue;
            }
            if (mSurfaceCreated) {
                mRenderer.onSurfaceCreated(coreProcess);
                mOutFboID = mRenderer.getOutputFboID();
                mSurfaceCreated = false;
                ismSurfaceCreatedFlag = true;

                if (mCreateLock!=null){
                    synchronized (mCreateLock){
                        mCreateLock.notify();
                    }
                }
            }

            if (mSurfaceChanged) {
                if(isSetInputFBOReady){
                    mRenderer.setInputFboID(mInputFboID);
                    isSetInputFBOReady = false;
                }

                mRenderer.onSurfaceChanged(coreProcess, mRenderOutputWidth, mRenderOutputHeight);
                mSurfaceChanged = false;
                mReadyToDraw = true;
            }

            if(mLastRenderThread==null){
                lastDrawFinish = true;
            }else {
                lastDrawFinish = mLastRenderThread.drawFinish;
            }

            if (mReadyToDraw && lastDrawFinish){
                if(mLastRenderThread!=null){
                    mLastRenderThread.drawFinish = false;
                }
                //Draw start======================================
                if (mRenderMode == RENDERMODE_WHEN_DIRTY) {
                    synchronized (mDrawFrameObject) {
                        try {
                            mDrawFrameObject.wait();
                            mRenderer.onDrawFrame(coreProcess);
                        } catch(InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                } else {
                    mRenderer.onDrawFrame(coreProcess);
                }
                //Draw end=====================================
                drawFinish = true;
            }
        }
        synchronized (mDrawFrameObject) {
            mDrawFrameObject.notify();
        }
        mRenderer.onSurfaceDestroyed(coreProcess);
        coreProcess.release();
    }

    public interface Renderer {
        void onSurfaceCreated(CoreProcess coreProcess);
        void onSurfaceChanged(CoreProcess coreProcess, int width, int height);
        void onDrawFrame(CoreProcess coreProcess);
        void onSurfaceDestroyed(CoreProcess coreProcess);
        void setInputFboID(int[] inputFboID);
        int[] getOutputFboID();
    }
}
