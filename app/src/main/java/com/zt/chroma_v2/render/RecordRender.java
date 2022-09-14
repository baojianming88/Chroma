package com.zt.chroma_v2.render;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGLSurface;
import android.opengl.GLES30;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.zt.chroma_v2.egl.CoreProcess;
import com.zt.chroma_v2.egl.RenderThread;
import com.zt.chroma_v2.utils.ShaderUtils;

/**
 * author : Tan Lang
 * e-mail : 2715009907@qq.com
 * date   : 2022/3/31-20:10
 * desc   :
 * version: 1.0
 */
public class RecordRender extends BaseRender implements RenderThread.Renderer{
    private Resources res;
    private Surface mSurface;
    private EGLSurface mEGLSurface;
    private int[] mInputFboID;

    public RecordRender(Resources resources, Boolean isFboProgram, Surface surface) {
        super(resources, isFboProgram);
        TAG = "Stage3rdRender";
        mSurface = surface;
        res = resources;
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
        //初始化FBO纹理
        initFBOTexture(false,0);   // initFBOTexture(false,0);
        Log.d(TAG,"onSurfaceCreated finish");
    }

    @Override
    public void onSurfaceChanged(CoreProcess coreProcess, int width, int height) {

    }

    @Override
    public void onDrawFrame(CoreProcess coreProcess) {
        // 绘制
        coreProcess.makeCurrent(mEGLSurface);
        drawPrepare(true,1);
        draw(true);
        coreProcess.swapBuffers(mEGLSurface);
    }

    @Override
    public void onSurfaceDestroyed(CoreProcess coreProcess) {
        coreProcess.makeCurrent(mEGLSurface);
        release();
        coreProcess.releaseWindowSurface(mEGLSurface);
        mEGLSurface = null;
    }

    @Override
    public void setInputFboID(int[] inputFboID) {
        mInputFboID = inputFboID;
    }

    @Override
    public int[] getOutputFboID() {
        return null;
    }

    @Override
    void createProgram() {
        // 直接输出
        String vertexShaderStrStraight = ShaderUtils.parseShader(res, "shader/vertex_in_straight_out_shader.glsl");
        int vertexShaderIdStraight = ShaderUtils.compileVertexShader(vertexShaderStrStraight);
        String fragmentShaderStrStraight = ShaderUtils.parseShader(res, "shader/fragment_in_straight_out_shader.glsl");
        int fragmentShaderIdStraight = ShaderUtils.compileFragmentShader(fragmentShaderStrStraight);
        //渲染程序3
        mProgram = ShaderUtils.linkProgram(vertexShaderIdStraight, fragmentShaderIdStraight);
    }

    @Override
    void onDraw() {
        // 输出到屏幕
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mInputFboID[0]);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(mProgram,"vTexture"), 0);

        GLES30.glBindVertexArray(mVaoids[0]);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, VERTEX_INDEX.length, GLES30.GL_UNSIGNED_SHORT,0);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES30.GL_NONE);
        GLES30.glBindVertexArray(GLES30.GL_NONE);
        GLES30.glUseProgram(0);

    }
}
