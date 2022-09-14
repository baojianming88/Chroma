package com.zt.chroma_v2.functions;

import static com.zt.chroma_v2.egl.RenderThread.RENDERMODE_WHEN_DIRTY;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.zt.chroma_v2.camera.Camera2Helper;
import com.zt.chroma_v2.egl.RenderThread;
import com.zt.chroma_v2.render.SourceRender;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * author : Tan Lang
 * e-mail : 2715009907@qq.com
 * date   : 2022/2/289:59
 * desc   :
 * version: 1.0
 */
public class SourceManager implements SurfaceHolder.Callback{
    private static final String TAG = "SourceManager";

    public boolean isStart;
    private final Context mContext;
    private SurfaceView mMainOutputView;
    private Camera2Helper mCamera2Helper;
    private Size mRenderSize;

    private SourceRender mSourceRender;
    private RenderThread mRenderThread;
    private EGLContext mShareEGLContext;

    private Object InputCreateLock = new Object();

    private  Map<Object, RenderThread> mRenderThreadMap;
    private  Map<SurfaceTexture, RenderThread> mInputTextureMap;

    public SourceManager(Context context, SurfaceView mainOutputView, Size renderSize){
        mContext = context;
        mMainOutputView = mainOutputView;
        mRenderSize = renderSize;

        //Map initialize
        mRenderThreadMap = new ConcurrentHashMap<>();
        mInputTextureMap = new ConcurrentHashMap<>();

    }

    public void startPreview(){
        isStart = true;
        createSourceRenderThread(mMainOutputView.getHolder(), mRenderSize.getWidth(),mRenderSize.getHeight());
    }

    public void stopPreview(){
        isStart = false;
        mRenderThread.setSurfaceDestroyed();

    }

    public void freezeRender(){
        mRenderThread.freezeRender();
    }

    public void unFreezeRender(){
        mRenderThread.unFreezeRender();
    }

    public void setCamera2Helper(Camera2Helper camera2Helper){
        mCamera2Helper = camera2Helper;
    }

    public EGLContext getSharedContext(){
        return mShareEGLContext;
    }

    //Functions for source render begin------------------------------------------------
    public void createSourceRenderThread(SurfaceHolder surfaceHolder, int renderWidth, int renderHeight){
        surfaceHolder.addCallback(this);
        //New a OutputRender
        mSourceRender = new SourceRender(mContext.getResources(),false, surfaceHolder.getSurface());
        mSourceRender.setRenderSize(renderWidth,renderHeight);
        mSourceRender.setCamera2Helper(mCamera2Helper);

        //Create render thread
        mRenderThread = createRenderThread(surfaceHolder.getSurface(),mSourceRender,
                RENDERMODE_WHEN_DIRTY,"Input",InputCreateLock);

        // 在InputRender中设置frameAvailable监听器 监听输入源surfaceTexture,帧可获取时候，开始渲染
        mSourceRender.setRenderThread(mRenderThread);
    }
    //Functions for source render end--------------------------------------------------

    //Private interface--------------------------------------------------------
    private RenderThread createRenderThread(Object surface, RenderThread.Renderer renderer,int renderMode,String name,Object lock) {
        //Synchronized because all the RenderThread should share one context
        synchronized(this) {
            if (!mRenderThreadMap.containsKey(surface)) {
                RenderThread renderThread = new RenderThread(mShareEGLContext,lock);
                renderThread.setRender(renderer);
                renderThread.setRenderMode(renderMode);
                renderThread.setThreadName(name);
                renderThread.start();
                mRenderThreadMap.put(surface, renderThread);

                //After new first RenderThread, we need get it's EGLContext as the context of subsequent new threads
                if (mShareEGLContext == null) {
                    mShareEGLContext = renderThread.getContext();
                }
            }
        }
        return mRenderThreadMap.get(surface);
    }
    //Private interface--------------------------------------------------------
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        // OutputView's thread
        RenderThread renderThread = mRenderThreadMap.get(holder.getSurface());
        if (renderThread !=  null) {
            renderThread.setSurfaceCreated();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        //OutputView's thread
        RenderThread renderThread = mRenderThreadMap.get(holder.getSurface());
        if (renderThread !=  null) {
            renderThread.setSurfaceChanged(width, height);
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        //OutputView's thread
        RenderThread renderThread = mRenderThreadMap.get(holder.getSurface());
        if (renderThread != null) {
            renderThread.setSurfaceDestroyed();
            mRenderThreadMap.remove(holder.getSurface());
        }
    }
}
