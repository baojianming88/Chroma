package com.zt.chroma_v2.functions;

import static com.zt.chroma_v2.egl.RenderThread.RENDERMODE_CONTINUOUSLY;
import static com.zt.chroma_v2.egl.RenderThread.RENDERMODE_WHEN_DIRTY;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.zt.chroma_v2.camera.Camera2Helper;
import com.zt.chroma_v2.egl.RenderThread;
import com.zt.chroma_v2.render.Stage1stRender;
import com.zt.chroma_v2.render.Stage2ndRender;
import com.zt.chroma_v2.render.Stage3rdRender;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * author : Tan Lang
 * e-mail : 2715009907@qq.com
 * date   : 2022/2/289:59
 * desc   :
 * version: 1.0
 */
public class ChromaManager implements SurfaceHolder.Callback{
    private static final String TAG = "ComposerManager";

    public boolean isStart;
    private final Context mContext;
    private SurfaceView mMainOutputView;
    private Camera2Helper mCamera2Helper;
    private Size mRenderSize;
    private SurfaceTexture mStage2ndSurfaceTexture;
    private SurfaceTexture mStage1stSurfaceTexture;

    private Stage1stRender stage1stRender;
    private Stage2ndRender stage2ndRender;
    private Stage3rdRender stage3rdRender;

    private RenderThread renderThreadStage1st;
    private RenderThread renderThreadStage2nd;
    private RenderThread renderThreadStage3rd;

    private int[] stage2ndInputFboID;
    private int[] stage3rdInputFboID;

    private EGLContext mShareEGLContext;

    private Object setFinshLock = new Object();
    private Object stage1stCreateLock = new Object();
    private Object stage2ndCreateLock = new Object();
    private Object stage3rdCreateLock = new Object();

    private boolean mChangedTurn = true;
    private Bitmap mBackground;

    private final Map<Object, RenderThread> mRenderThreadMap;
    private final Map<SurfaceTexture, RenderThread> mInputTextureMap;

    public ChromaManager(Context context, SurfaceView mainOutputView, Size renderSize, Bitmap background){
        mContext = context;
        mMainOutputView = mainOutputView;
        mRenderSize = renderSize;
        mBackground = background;

        //Map initialize
        mRenderThreadMap = new ConcurrentHashMap<>();
        mInputTextureMap = new ConcurrentHashMap<>();
    }

    public void startPreview(){
        isStart = true;
        threadsManager(mMainOutputView,mRenderSize.getWidth(),mRenderSize.getHeight());
    }

    public void stopPreview(){
        isStart = false;
        renderThreadStage1st.setSurfaceDestroyed();
        renderThreadStage2nd.setSurfaceDestroyed();
        renderThreadStage3rd.setSurfaceDestroyed();

    }

    public void freezeRender(){
        renderThreadStage3rd.freezeRender();
        renderThreadStage2nd.freezeRender();
        renderThreadStage1st.freezeRender();
    }

    public void unFreezeRender(){
        renderThreadStage3rd.unFreezeRender();
        renderThreadStage2nd.unFreezeRender();
        renderThreadStage1st.unFreezeRender();
    }

    public void setCamera2Helper(Camera2Helper camera2Helper){
        mCamera2Helper = camera2Helper;
    }

    public void capturePicture(){
        stage3rdRender.getCaptureImage();
        Toast.makeText(mContext, "拍照完成，已保存 ", Toast.LENGTH_SHORT).show();
    }

    //Functions for output begin-----------------------------------------------
    public void createOutputThread(SurfaceView surfaceView,int renderWidth,int renderHeight) {
        //Add callback to get the surfaceView's lifecycle
        surfaceView.getHolder().addCallback(this);
        //New a OutputRender
        stage3rdRender = new Stage3rdRender(mContext,false, surfaceView.getHolder().getSurface());
        stage3rdRender.setRenderSize(renderWidth,renderHeight);
        //Create render thread
        renderThreadStage3rd = createRenderThread(surfaceView.getHolder().getSurface(), stage3rdRender,
                RENDERMODE_CONTINUOUSLY,"stage3rd",stage3rdCreateLock);
    }
    //Functions for output end-------------------------------------------------

    //Functions for middle stage begin-----------------------------------------
    public void createMiddleThread(int renderWidth,int renderHeight,Object lock){

        synchronized (lock){
            try {
                if(!renderThreadStage3rd.isCreateSurface()){
                    Log.d(TAG," stage2nd onSurfaceCreated waits");
                    lock.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(stage3rdRender !=null){
            // create output surfaceTexture
            mStage2ndSurfaceTexture = stage3rdRender.genSurfaceTexture();
            mStage2ndSurfaceTexture.setDefaultBufferSize(renderWidth,renderHeight);
            //New a OutputRender
            stage2ndRender = new Stage2ndRender(mContext.getResources(),true,mStage2ndSurfaceTexture);
            stage2ndRender.setmBackground(mBackground);
            stage2ndRender.setRenderSize(renderWidth,renderHeight);
            //Create render thread
            renderThreadStage2nd = createRenderThread(mStage2ndSurfaceTexture, stage2ndRender,
                    RENDERMODE_CONTINUOUSLY,"stage2nd",stage2ndCreateLock);
            renderThreadStage2nd.setSurfaceCreated();
        }

    }
    //Functions for middle stage end-------------------------------------------

    //Functions for input begin------------------------------------------------
    public void createInputThread(int renderWidth, int renderHeight,Object lock){
        synchronized (lock){
            try {
                if(!renderThreadStage2nd.isCreateSurface()){
                    Log.d(TAG,"stage1st onSurfaceCreated waits");
                    lock.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(stage2ndRender != null){
            // create output surfaceTexture
            mStage1stSurfaceTexture = stage2ndRender.genSurfaceTexture();
            mStage1stSurfaceTexture.setDefaultBufferSize(renderWidth,renderHeight);
            //New a OutputRender
            stage1stRender = new Stage1stRender(mContext.getResources(),true,mStage1stSurfaceTexture);
            stage1stRender.setRenderSize(renderWidth,renderHeight);
            stage1stRender.setCamera2Helper(mCamera2Helper);

            //Create render thread
            renderThreadStage1st = createRenderThread(mStage1stSurfaceTexture,stage1stRender,RENDERMODE_WHEN_DIRTY,"stage1st",stage1stCreateLock);
            renderThreadStage1st.setSurfaceCreated();

            // 在stage1stRender中设置frameAvailable监听器 监听输入源surfaceTexture,帧可获取时候，开始渲染
            stage1stRender.setRenderThread(renderThreadStage1st);
        }
    }
    //Functions for input end--------------------------------------------------

    //Functions for setting all threads start---------------------------------------
    public void setAllThread(Object lock){
        synchronized (lock){
            try {
                if(!renderThreadStage1st.isCreateSurface()){
                    Log.d(TAG,"Setting for all threads waits");
                    lock.wait();
                    Log.d(TAG,"Setting for all threads start");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // create surface
        if(renderThreadStage1st != null){
            renderThreadStage1st.setLastRenderThread(null);
            stage2ndInputFboID = renderThreadStage1st.getOutFboID();
        }
        if(renderThreadStage2nd != null){
            renderThreadStage2nd.setLastRenderThread(renderThreadStage1st);
            renderThreadStage2nd.setInputFboID(stage2ndInputFboID);
            stage3rdInputFboID = renderThreadStage2nd.getOutFboID();
        }
        if(renderThreadStage3rd!=null){
            renderThreadStage3rd.setLastRenderThread(renderThreadStage2nd);
            renderThreadStage3rd.setInputFboID(stage3rdInputFboID);
        }
    }
    //Functions for setting all threads start---------------------------------------

    //Function for thread manager begin------------------------------------------
    public void threadsManager(SurfaceView mainOutputView,int renderWidth, int renderHeight){

        //Add main output view
        createOutputThread(mainOutputView,renderWidth,renderHeight);

        //create work thread
        Thread generateInputTask = new Thread(){
            @Override
            public void run() {
                setName("Manager thread-" + getId());

                //Add middle stage render
                createMiddleThread(renderWidth,renderHeight,stage3rdCreateLock);

                //Add input stage render
                createInputThread(renderWidth,renderHeight,stage2ndCreateLock);

                // Trigger and setSurfaceChanged
                setAllThread(stage1stCreateLock);

                // start draw
                renderThreadStage1st.setSurfaceChanged(renderWidth,renderHeight);
                renderThreadStage2nd.setSurfaceChanged(renderWidth,renderHeight);

                synchronized (setFinshLock){
                    setFinshLock.notify();
                }
            }
        };
        generateInputTask.start();

    }
    //Function for thread manager end------------------------------------------

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

    public EGLContext getSharedContext(){
        return mShareEGLContext;
    }

    public int[] getOutputFboTexture(){
        return renderThreadStage3rd.getOutFboID();
    }

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
        if(mChangedTurn){
            synchronized (setFinshLock){
                try {
                    Log.d(TAG, "Stage3rd surfaceChanged waits");
                    setFinshLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mChangedTurn = false;
        }

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
