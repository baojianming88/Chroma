package com.zt.chroma_v2.render;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.EGLExt;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.zt.chroma_v2.R;
import com.zt.chroma_v2.utils.ImageUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * author : Tan Lang
 * e-mail : 2715009907@qq.com
 * date   : 2022/2/24 12:49
 * desc   :
 * version: 1.0
 */
public abstract class BaseRender {

    protected static String TAG = "baseRender";

    protected final Resources mRes;
    // 判断是否渲染到FBO
    private boolean mIsFboProgram;

    private FloatBuffer vertexBuffer, mTexVertexBuffer, mFBOTexVertexBuffer;
    private ShortBuffer mVertexIndexBuffer;

    // Program id
    protected int mProgram;

    // The input source texture id
    protected int mTexture = 0;
    protected int mTextureBackgroundId;

    // The type of texture
    protected int mTexType = GLES30.GL_TEXTURE_2D;

    // The transform matrix location;
    protected   int mMatrixLocation;

    // VAO id
    protected int[] mVaoids = new int[2];
    // VBO id
    protected int[] mVboids = new int[4];
    // FBO id
    protected int[] mFbo;
    protected int[] mFboTexture;

    private boolean mOut2FBO = false;

    // The render image size
    protected int mWidth;
    protected int mHeight;

    // The texture size
    protected int vTexSizeLocation;

    //相机矩阵
    protected float[] mViewMatrix = new float[16];
    //投影矩阵
    protected float[] mProjectMatrix = new float[16];
    //变换矩阵
    protected float[] mMVPMatrix = new float[16];

    /**
     * 顶点坐标
     * (x,y,z)
     */
    protected static final float[] POSITION_VERTEX = new float[]{
            -1.0f, -1.0f,       //顶点坐标V0
            1.0f, -1.0f,        //顶点坐标V1
            -1.0f, 1.0f,        //顶点坐标V2
            1.0f, 1.0f,         //顶点坐标V3
    };

    /**
     * 纹理坐标
     * (s,t)
     */
    protected static final float[] TEX_VERTEX = {
            0.0f, 0.0f,     //纹理坐标V0
            1.0f, 0.0f,     //纹理坐标V1
            0.0f, 1.0f,     //纹理坐标V2
            1.0f, 1.0f,     //纹理坐标V3
    };

    /**
     * FBO纹理坐标
     * (s,t)
     */
    protected static final float[] FBO_TEX_VERTEX = {
            0.0f, 1.0f,  //纹理坐标V0
            1.0f, 1.0f,  //纹理坐标V1
            0.0f, 0.0f,  //纹理坐标V2
            1.0f, 0.0f,  //纹理坐标V3
    };


    /**
     * 绘制顺序索引
     */
    protected static final short[] VERTEX_INDEX = {
            0, 1, 2,  //V0,V1,V2 三个顶点组成一个三角形
            1, 2, 3,  //V1,V2,V3 三个顶点组成一个三角形
    };

    /**
     * 附着数组
     */
    protected static int attachments[] = {
            GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_COLOR_ATTACHMENT1,
    };


    public BaseRender(Resources resources,Boolean isFboProgram){
        mRes = resources;
        mIsFboProgram = isFboProgram;
        // 初始化顶点和纹理buffer
        initBuffer();
    }

    public void initBuffer(){
        vertexBuffer = ByteBuffer.allocateDirect(POSITION_VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(POSITION_VERTEX);
        vertexBuffer.position(0);

        mTexVertexBuffer = ByteBuffer.allocateDirect(TEX_VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(TEX_VERTEX);
        mTexVertexBuffer.position(0);

        mFBOTexVertexBuffer = ByteBuffer.allocateDirect(FBO_TEX_VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(FBO_TEX_VERTEX);
        mFBOTexVertexBuffer.position(0);

        mVertexIndexBuffer = ByteBuffer.allocateDirect(VERTEX_INDEX.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(VERTEX_INDEX);
        mVertexIndexBuffer.position(0);
    }

    abstract void createProgram();

    protected void getAttributeLocation(){
        mMatrixLocation = GLES30.glGetUniformLocation(mProgram, "u_Matrix");
        vTexSizeLocation = GLES30.glGetUniformLocation(mProgram,"vTextureSize");

    }

    protected void createVBO(){
        // 生成VBO，加载顶点，索引数据到gpu内存
        GLES30.glGenBuffers(4, mVboids,0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,mVboids[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,POSITION_VERTEX.length*4,vertexBuffer,GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,mVboids[1]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,TEX_VERTEX.length*4,mTexVertexBuffer,GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,mVboids[2]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,FBO_TEX_VERTEX.length*4,mFBOTexVertexBuffer,GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,mVboids[3]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,VERTEX_INDEX.length*2,mVertexIndexBuffer,GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0);
    }

    protected void createVAO(){

        GLES30.glGenVertexArrays(2,mVaoids,0);
        // Fbo纹理
        GLES30.glBindVertexArray(mVaoids[0]);

        GLES30.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboids[0]);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0,2,GLES30.GL_FLOAT,false, 0,0);
        GLES30.glBindBuffer(GLES20.GL_ARRAY_BUFFER, GLES30.GL_NONE);

        GLES30.glBindBuffer(GLES20.GL_ARRAY_BUFFER,mVboids[2]);
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1,2,GLES20.GL_FLOAT,false,0,0);
        GLES30.glBindBuffer(GLES20.GL_ARRAY_BUFFER, GLES30.GL_NONE);

        GLES30.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER,mVboids[3]);

        // 非Fbo纹理
        GLES30.glBindVertexArray(mVaoids[1]);

        GLES30.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboids[0]);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0,2,GLES30.GL_FLOAT,false, 0,0);
        GLES30.glBindBuffer(GLES20.GL_ARRAY_BUFFER, GLES30.GL_NONE);

        GLES30.glBindBuffer(GLES20.GL_ARRAY_BUFFER,mVboids[1]);
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1,2,GLES20.GL_FLOAT,false,0,0);
        GLES30.glBindBuffer(GLES20.GL_ARRAY_BUFFER, GLES30.GL_NONE);

        GLES30.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER,mVboids[3]);

    }

    public void initInputTexture(){
        // 初始化输入纹理
        mTexture = createTexture(mTexType);
    }

    public void initFBOTexture(boolean out2FBO, int FBOTextureSize){
        // 初始化FBO
        mOut2FBO = out2FBO;
        if(mOut2FBO){
            mFboTexture = createFBOTexture(FBOTextureSize);
            mFbo = createFBO(mFboTexture,0);
        }

    }

    public void createBackground(Bitmap bitmap){
        // 初始化背景图像和纹理
        if (bitmap==null){
            bitmap = loadImage2Bitmap(mRes, R.drawable.default_background);
        }
        mTextureBackgroundId = loadTexture(bitmap);
    }

    protected Bitmap loadImage2Bitmap(Resources resources, int resourceId){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        //这里需要加载原图未经缩放的数据
        options.inScaled = false;
        Bitmap mBitmap= BitmapFactory.decodeResource(resources, resourceId, options);
        if (mBitmap == null) {
            Log.e(TAG, "Resource ID " + resourceId + " could not be decoded.");
            return null;
        }
        mBitmap = ImageUtils.cropImage(mBitmap,mWidth,mHeight,true);

        return mBitmap;
    }

    protected int loadTexture(Bitmap mBitmap) {

        int[] mTextureId = new int[1];
        GLES30.glGenTextures(1,mTextureId,0);
        if (mTextureId[0] == 0) {
            Log.e(TAG, "Could not generate a new OpenGL textureId object.");
            return 0;
        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,mTextureId[0]);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_WRAP_S,GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_WRAP_T,GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MAG_FILTER,GLES30.GL_LINEAR);

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, mBitmap, 0);
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);

        //回收bitmap
//        mBitmap.recycle();

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

        return mTextureId[0];
    }

    private int[] createFBOTexture(int textureNum){
        int[] mFboTextureId = new int[textureNum];
        GLES30.glGenTextures(textureNum,mFboTextureId,0);

        for(int i =0;i<textureNum;i++){
            GLES30.glBindTexture(GLES20.GL_TEXTURE_2D,mFboTextureId[i]);
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, mWidth, mHeight,
                    0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_WRAP_S,GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_WRAP_T,GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_LINEAR);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MAG_FILTER,GLES30.GL_LINEAR);

        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE, GLES30.GL_NONE);

        return mFboTextureId;
    }

    public int createTexture(int textureType){
        int[] mTextureId = new int[1];
        GLES30.glGenTextures(1,mTextureId,0);

        GLES30.glBindTexture(textureType,mTextureId[0]);
        GLES30.glTexParameterf(textureType,GLES30.GL_TEXTURE_WRAP_S,GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(textureType,GLES30.GL_TEXTURE_WRAP_T,GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(textureType,GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_LINEAR);
        GLES30.glTexParameterf(textureType,GLES30.GL_TEXTURE_MAG_FILTER,GLES30.GL_LINEAR);

        GLES30.glBindTexture(textureType,GLES20.GL_NONE);
        return mTextureId[0];
    }

    private int[] createFBO(int[] mFBOTextureId, int offset){

        // 创建并初始化 FBO
        int[] m_FboId = new int[1];
        GLES30.glGenFramebuffers(1,m_FboId,0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,m_FboId[0]);

        for(int i=0;i<mFBOTextureId.length;i++){
            // mFBOTextureId.length-1-i+offset
            GLES30.glFramebufferTexture2D(GLES30.GL_DRAW_FRAMEBUFFER,attachments[i+offset],
                    GLES30.GL_TEXTURE_2D,mFBOTextureId[i],0);
        }

        int result = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)!= GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG,"FBO texture error"+result);
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,GLES30.GL_NONE);

        return m_FboId;
    }

    public void setRenderSize(int width, int height){
        mWidth = width;
        mHeight = height;
    }

    public void draw(boolean needSize){

        GLES30.glViewport(0, 0, mWidth, mHeight);
        // update the transform matrix
        updateMatrix();
        // update related params
        updateParams(needSize);
        // draw the image
        onDraw();

    }

    public void drawPrepare(boolean clearBuffer, int attachmentsSize) {
        if (mOut2FBO) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFbo[0]);
        }else {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,0);
        }
        GLES30.glDrawBuffers(attachmentsSize,attachments,0);

        onClear(clearBuffer);
        GLES20.glUseProgram(mProgram);
    }

    public void onClear(boolean clearBuffer){
        // 设置清屏颜色为黑色
        GLES30.glClearColor(0,0,0,1);
        if(clearBuffer){
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        }
    }

    public void updateMatrix(){
        GLES30.glUniformMatrix4fv(mMatrixLocation,1,false,mMVPMatrix,0);
    }

    public void computeMVPMatrix(int width, int height){

        float sWH = (float)mWidth / (float)mHeight;
        float sWidthHeight = (float)width / (float)height;

        if(width>height){
            if(sWH>sWidthHeight){
                Matrix.orthoM(mProjectMatrix, 0, -sWidthHeight*sWH,sWidthHeight*sWH, -1,1, 3, 7);
            }else{
                Matrix.orthoM(mProjectMatrix, 0, -sWidthHeight/sWH,sWidthHeight/sWH, -1,1, 3, 7);
            }
        }else{
            if(sWH>sWidthHeight){
                Matrix.orthoM(mProjectMatrix, 0, -1, 1, -1/sWidthHeight*sWH, 1/sWidthHeight*sWH,3, 7);
            }else{
                Matrix.orthoM(mProjectMatrix, 0, -1, 1, -sWH/sWidthHeight, sWH/sWidthHeight,3, 7);
            }
        }
        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        //计算变换矩阵
        Matrix.multiplyMM(mMVPMatrix,0,mProjectMatrix,0,mViewMatrix,0);
    }

    public void updateParams(boolean needSize){
        if(needSize){
            GLES30.glUniform2f(vTexSizeLocation,mWidth,mHeight);
        }
    }

    abstract void onDraw();

    public void release() {

        if (mOut2FBO) {
            GLES30.glDeleteFramebuffers(1, mFbo, 0);
            GLES30.glDeleteTextures(mFboTexture.length, mFboTexture, 0);
        }

        GLES20.glDeleteBuffers(mVboids.length, mVboids, 0);
        if (mTexture > 0) {
            int[] textures = new int[1];
            textures[0] = mTexture;
            GLES20.glDeleteTextures(1, textures,0);
        }

        if (mProgram > 0) {
            GLES20.glDeleteProgram(mProgram);
            mProgram = 0;
        }
    }

    protected Bitmap readPixel(int i){
        Bitmap bitmap=Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        ByteBuffer mBuffer = ByteBuffer.allocate(mWidth * mHeight * 4);
        GLES30.glReadBuffer(GLES30.GL_COLOR_ATTACHMENT0+i);
        GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, mBuffer);

        bitmap.copyPixelsFromBuffer(mBuffer);
        return bitmap;
    }
}
