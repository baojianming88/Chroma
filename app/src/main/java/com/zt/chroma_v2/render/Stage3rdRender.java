package com.zt.chroma_v2.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGLSurface;
import android.opengl.GLES30;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.zt.chroma_v2.egl.CoreProcess;
import com.zt.chroma_v2.egl.RenderThread;
import com.zt.chroma_v2.utils.ImageUtils;
import com.zt.chroma_v2.utils.ShaderUtils;

/**
 * author : Tan Lang
 * e-mail : 2715009907@qq.com
 * date   : 2022/2/2715:42
 * desc   :
 * version: 1.0
 */
public class Stage3rdRender extends BaseRender implements RenderThread.Renderer {

    private Object mSurface;
    private Context mContext;
    private EGLSurface mEGLSurface;
    private int[] mInputFboID;
    private boolean FLAG_CAPTURE = false;
    private int mProgramStraight;

    public Stage3rdRender(Context context, Boolean isFboProgram, Surface surface) {
        super(context.getResources(), isFboProgram);
        TAG = "Stage3rdRender";
        mSurface = surface;
        mContext = context;
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

    public void getCaptureImage(){
        FLAG_CAPTURE = true;
    }

    @Override
    public void setInputFboID(int[] inputFboID){
        mInputFboID = inputFboID;
    }

    @Override
    public int[] getOutputFboID(){
        return mFboTexture;
    }

    @Override
    public void onSurfaceCreated(@NonNull CoreProcess coreProcess) {
        Log.d(TAG,"onSurfaceCreated start");
        mEGLSurface = coreProcess.createWindowSurface(mSurface);
        coreProcess.makeCurrent(mEGLSurface);

        //创建程序
        createProgram();
        //得到着色器程序属性位置
        getAttributeLocation();
        //创建VBO
        createVBO();
        //创建VAO
        createVAO();
        //初始化输入纹理
        initInputTexture();
        // 初始化FBO纹理
        initFBOTexture(true,1);   // initFBOTexture(false,0);
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
        String vertexShaderStr3rd = ShaderUtils.parseShader(mRes, "shader/vertex_chromakey2nd.glsl");
        int vertexShaderId3rd = ShaderUtils.compileVertexShader(vertexShaderStr3rd);
//        String fragmentShaderStr3rd = ShaderUtils.parseShader(mRes, "shader/fragment_chromakey2nd(optimize).glsl");
        String fragmentShaderStr3rd_ = ShaderUtils.parseCipherShader(mRes, "shader/2nd_shader");
        int fragmentShaderId3rd = ShaderUtils.compileFragmentShader(fragmentShaderStr3rd_);
        //渲染程序3
        mProgram = ShaderUtils.linkProgram(vertexShaderId3rd, fragmentShaderId3rd);

        // 直接输出
        String vertexShaderStrStraight = ShaderUtils.parseShader(mRes, "shader/vertex_in_straight_out_shader.glsl");
        int vertexShaderIdStraight = ShaderUtils.compileVertexShader(vertexShaderStrStraight);
        String fragmentShaderStrStraight = ShaderUtils.parseShader(mRes, "shader/fragment_in_straight_out_shader.glsl");
        int fragmentShaderIdStraight = ShaderUtils.compileFragmentShader(fragmentShaderStrStraight);
        //渲染程序3
        mProgramStraight = ShaderUtils.linkProgram(vertexShaderIdStraight, fragmentShaderIdStraight);

    }

    @Override
    void onDraw() {
        // 输出到录制
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mInputFboID[0]);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(mProgram,"vTexture"), 0);

        GLES30.glBindVertexArray(mVaoids[0]);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, VERTEX_INDEX.length, GLES30.GL_UNSIGNED_SHORT,0);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES30.GL_NONE);
        GLES30.glBindVertexArray(GLES30.GL_NONE);

        if(FLAG_CAPTURE){
            Bitmap bmp = readPixel(0);
            ImageUtils.saveImageToGallery(mContext,bmp);
            FLAG_CAPTURE = false;
        }

        // 输出到屏幕
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,0);
        GLES30.glUseProgram(mProgramStraight);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mFboTexture[0]);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(mProgram,"vTexture"), 0);

        GLES30.glBindVertexArray(mVaoids[0]);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, VERTEX_INDEX.length, GLES30.GL_UNSIGNED_SHORT,0);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES30.GL_NONE);
        GLES30.glBindVertexArray(GLES30.GL_NONE);
        GLES30.glUseProgram(0);

    }
}
