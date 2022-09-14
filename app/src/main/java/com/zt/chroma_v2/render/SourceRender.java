package com.zt.chroma_v2.render;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import com.zt.chroma_v2.camera.Camera2Helper;
import com.zt.chroma_v2.egl.CoreProcess;
import com.zt.chroma_v2.egl.RenderThread;
import com.zt.chroma_v2.utils.ShaderUtils;

public class SourceRender extends BaseRender implements RenderThread.Renderer,SurfaceTexture.OnFrameAvailableListener{
    private Object mSurface;
    private EGLSurface mEGLSurface;
    private SurfaceTexture mInputSurfaceTexture;

    private Camera2Helper mCamera2Helper;
    private RenderThread mRenderThread;

    public SourceRender(Resources resources, Boolean isFboProgram, Surface outputSurface) {  // surfaceTexture
        super(resources, isFboProgram);
        TAG = "SourceRender";
        mTexType = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
        mSurface = outputSurface;
    }

    public void setCamera2Helper(Camera2Helper camera2Helper){
        mCamera2Helper = camera2Helper;
    }

    public void setRenderThread(RenderThread renderThread){
        mRenderThread = renderThread;
    }

    @Override
    public void setInputFboID(int[] inputFboID){
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
        createVBO();
        //创建VAO
        createVAO();
        //初始化纹理
        initInputTexture();
        //初始化FBO纹理
        initFBOTexture(false,0);  // error 设置为true时候黑屏

        // ==========输入源 SurfaceView与输入纹理链接=======================
        if(mTexture>0){
            mCamera2Helper.setSurfaceTexture(mTexture);
            mInputSurfaceTexture = mCamera2Helper.getSurfaceTexture();
            mCamera2Helper.startPreview();
            mInputSurfaceTexture.setOnFrameAvailableListener(this);
        }else {
            Log.d(TAG,"Get mInputTextureID error");
        }
        Log.d(TAG,"onSurfaceCreated finish");
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if(mRenderThread!=null){
            mRenderThread.requestRender();
        }
    }

    @Override
    public void onSurfaceChanged(CoreProcess coreProcess, int width, int height) {
        Log.d(TAG,"onSurfaceChanged ===>  mWidth = " + width + " mHeight = " + height);
        // mMVPMatrix 计算
//        computeMVPMatrix(width,height);
    }

    @Override
    public void onDrawFrame(CoreProcess coreProcess) {
        coreProcess.makeCurrent(mEGLSurface);
        if(mInputSurfaceTexture!=null){
            mInputSurfaceTexture.updateTexImage();
//            mInputSurfaceTexture.getTransformMatrix(mMVPMatrix); // 更新的matrix作用于纹理aTextureCoord
        }

        // 更新的matrix作用于gl_position
        Matrix.setIdentityM(mMVPMatrix, 0);
        if (mCamera2Helper.getCurrentFace()==0){
            Matrix.rotateM(mMVPMatrix,0,180F,0F,1F,0F); // angel旋转角度,x,y,z绕哪个转哪个置为1
            Matrix.rotateM(mMVPMatrix,0,90F,0F,0F,1F); // angel旋转角度,x,y,z绕哪个转哪个置为1
        }else if (mCamera2Helper.getCurrentFace()==1){
            Matrix.rotateM(mMVPMatrix,0,-90F,0F,0F,1F); // angel旋转角度,x,y,z绕哪个转哪个置为1
        }


        // 绘制
        drawPrepare(true,1);
        draw(false);
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
    public void createProgram() {
        String vertexShaderStr1st = ShaderUtils.parseShader(mRes, "shader/vertex_source_oes_shader.glsl");
        int vertexShaderId1st = ShaderUtils.compileVertexShader(vertexShaderStr1st);
        String fragmentShaderStr1st= ShaderUtils.parseShader(mRes, "shader/fragment_source_oes_shader.glsl");
        int fragmentShaderId1st = ShaderUtils.compileFragmentShader(fragmentShaderStr1st);
        //渲染程序1
        mProgram = ShaderUtils.linkProgram(vertexShaderId1st, fragmentShaderId1st);
    }

    @Override
    void onDraw() {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(mTexType,mTexture);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(mProgram,"cameraSource"),0);

        GLES30.glBindVertexArray(mVaoids[0]);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES,VERTEX_INDEX.length,GLES30.GL_UNSIGNED_SHORT,0);

        GLES30.glBindVertexArray(GLES30.GL_NONE);
        GLES30.glBindTexture(mTexType,GLES30.GL_NONE);
        GLES30.glUseProgram(0);
        GLES30.glDisableVertexAttribArray(mMatrixLocation);

//        Bitmap bmp = readPixel(1);
//        int w = bmp.getWidth();
    }
}
