package com.zt.chroma_v2.render;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGLSurface;
import android.opengl.GLES30;
import android.util.Log;
import android.util.Size;

import com.zt.chroma_v2.egl.CoreProcess;
import com.zt.chroma_v2.egl.RenderThread;
import com.zt.chroma_v2.utils.ShaderUtils;

/**
 * author : Tan Lang
 * e-mail : 2715009907@qq.com
 * date   : 2022/2/2715:41
 * desc   :
 * version: 1.0
 */
public class Stage2ndRender extends BaseRender implements RenderThread.Renderer{

    private Object mSurface;
    private EGLSurface mEGLSurface;
    private int[] mInputFboID;
    private Bitmap mBackground;
    private Size mRenderSize;

    public Stage2ndRender(Resources resources, Boolean isFboProgram,SurfaceTexture surfaceTexture) {
        super(resources, isFboProgram);
        TAG = "Stage2ndRender";
        mSurface = surfaceTexture;
    }

    public synchronized SurfaceTexture genSurfaceTexture(){
        synchronized (mSurface){

            if(mTexture>0){
                Log.d(TAG,"Gen input texture2D id: " + mTexture);
                return new SurfaceTexture(mTexture);
            }else {
                Log.e(TAG,"Gen input texture2D fail");
                return null;
            }
        }
    }

    public void setmBackground(Bitmap bitmap){
        mBackground = bitmap;
    }

    @Override
    public void setInputFboID(int[] inputFboID){
        mInputFboID = inputFboID;
    }

    @Override
    public int[] getOutputFboID() {
        return mFboTexture;
    }

    @Override
    public void onSurfaceCreated(CoreProcess coreProcess) {
        Log.d(TAG,"onSurfaceCreated start");
        mEGLSurface = coreProcess.createWindowSurface(mSurface);
        coreProcess.makeCurrent(mEGLSurface);
        //创建程序
        createProgram();
        //得到着色器程序属性位置
        getAttributeLocation();
        //初始化VBO
        mVboids = new int[]{1, 2, 3, 4};
        //创建VAO
        createVAO();
        //初始化输入纹理
        initInputTexture();
        // 初始化背景
        createBackground(mBackground);
        //初始化FBO纹理
        initFBOTexture(true,1);
        Log.d(TAG,"onSurfaceCreated finish");
    }

    @Override
    public void onSurfaceChanged(CoreProcess coreProcess, int width, int height) {
        Log.d(TAG,"onSurfaceChanged ===>  mWidth = " + width + " mHeight = " + height);
        coreProcess.makeCurrent(mEGLSurface);
        // mMVPMatrix 计算
//        computeMVPMatrix(width,height);
    }

    float total = 100;
    float times = total;
    long duration = 0;
    @Override
    public void onDrawFrame(CoreProcess coreProcess) {
        long startTime = System.currentTimeMillis();
        // 绘制
        coreProcess.makeCurrent(mEGLSurface);
        drawPrepare(true,1);
        draw(true);
        coreProcess.swapBuffers(mEGLSurface);


        duration += System.currentTimeMillis() - startTime;
        times--;
        if (times < 0){
            Log.i("Stage2nd","渲染一次耗时"+String.valueOf(duration/total)+"ms");
            duration = 0;
            times = total;
        }

    }

    @Override
    public void onSurfaceDestroyed(CoreProcess coreProcess) {
        coreProcess.makeCurrent(mEGLSurface);
        release();
        coreProcess.releaseWindowSurface(mEGLSurface);
        mEGLSurface = null;
    }

    @Override
    public void createProgram() {
        String vertexShaderStr2nd = ShaderUtils.parseShader(mRes, "shader/vertex_chromakey1st.glsl");
        int vertexShaderId2nd = ShaderUtils.compileVertexShader(vertexShaderStr2nd);
//        String fragmentShaderStr2nd = ShaderUtils.parseShader(mRes, "shader/fragment_chromakey1st(optimize).glsl");
        String fragmentShaderStr2nd_ = ShaderUtils.parseCipherShader(mRes, "shader/1st_shader");
        int fragmentShaderId2nd = ShaderUtils.compileFragmentShader(fragmentShaderStr2nd_);
        //渲染程序2
        mProgram = ShaderUtils.linkProgram(vertexShaderId2nd, fragmentShaderId2nd);
    }

    @Override
    void onDraw() {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mInputFboID[0]);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(mProgram,"vTexture"),0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,mTextureBackgroundId);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(mProgram,"vTextureBackground"),1);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,mInputFboID[1]);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(mProgram,"relatedMask"),2);

        GLES30.glBindVertexArray(mVaoids[0]);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, VERTEX_INDEX.length, GLES30.GL_UNSIGNED_SHORT,0);

        GLES30.glBindVertexArray(GLES30.GL_NONE);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES30.GL_NONE);
        GLES30.glUseProgram(0);

//        Bitmap bmp = readPixel(0);
//        int w = bmp.getWidth();
    }
}
