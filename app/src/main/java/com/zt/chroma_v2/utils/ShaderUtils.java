package com.zt.chroma_v2.utils;

import android.content.res.Resources;
import android.opengl.GLES30;
import android.util.Log;

import java.io.InputStream;

import javax.crypto.CipherInputStream;

/**
 * author : Tan Lang
 * e-mail : 2715009907@qq.com
 * date   : 2022/1/1516:05
 * desc   :
 * version: 1.0
 */

public class ShaderUtils {

    private static final String TAG = "ShaderUtils";

    /**
     *  解析Shader文件
     */
    public static String parseShader(Resources resources, String path) {
        StringBuilder stringBuilder = new StringBuilder();
        try{
            InputStream inputStream = resources.getAssets().open(path);
            int stringLength;
            byte[] buffer = new byte[1024];
            while (-1 != (stringLength = inputStream.read(buffer))) {
                stringBuilder.append(new String(buffer,0, stringLength));
            }
        }catch (Exception e){
            Log.e(TAG, "open " + path + " fail");
            return null;
        }
        return stringBuilder.toString().replaceAll("\\r\\n","\n");
    }

    /**
     * 解析加密shader文件
     */
    public static String parseCipherShader(Resources resources, String path){
        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStream inputStream = resources.getAssets().open(path);
            return AES.decryptionStreamGCM(inputStream);
        }catch (Exception e){
            Log.e(TAG,"open " + path + " fail");
            return null;
        }
    }

    /**
     * 编译顶点着色器
     * @param shaderCode
     */
    public static int compileVertexShader(String shaderCode) {
        return compileShader(GLES30.GL_VERTEX_SHADER, shaderCode);
    }

    /**
     * 编译片段着色器
     * @param shaderCode
     */
    public static int compileFragmentShader(String shaderCode) {
        return compileShader(GLES30.GL_FRAGMENT_SHADER, shaderCode);
    }

    /**
     * 编译
     * @param type       顶点着色器:GLES30.GL_VERTEX_SHADER
     *                   片段着色器:GLES30.GL_FRAGMENT_SHADER
     * @param shaderCode
     */
    private static int compileShader(int type, String shaderCode) {
        //创建一个着色器
        final int shaderId = GLES30.glCreateShader(type);
        if (shaderId != 0) {
            GLES30.glShaderSource(shaderId, shaderCode);
            GLES30.glCompileShader(shaderId);
            //检测状态
            final int[] compileStatus = new int[1];
            GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] == 0) {
                String logInfo = GLES30.glGetShaderInfoLog(shaderId);
                System.err.println(logInfo);
                //创建失败
                GLES30.glDeleteShader(shaderId);
                return 0;
            }
            return shaderId;
        } else {
            //创建失败
            return 0;
        }
    }

    /**
     * 链接小程序
     * @param vertexShaderId   顶点着色器
     * @param fragmentShaderId 片段着色器
     */
    public static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        //创建一个空的OpenGLES程序
        final int programId = GLES30.glCreateProgram();
        if (programId != 0) {
            //将顶点着色器加入到程序
            GLES30.glAttachShader(programId, vertexShaderId);
            //将片元着色器加入到程序中
            GLES30.glAttachShader(programId, fragmentShaderId);
            //链接着色器程序
            GLES30.glLinkProgram(programId);
            final int[] linkStatus = new int[1];

            GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0) {
                String logInfo = GLES30.glGetProgramInfoLog(programId);
                System.err.println(logInfo);
                GLES30.glDeleteProgram(programId);
                return 0;
            }
            return programId;
        } else {
            //创建失败
            return 0;
        }
    }

    /**
     * 验证程序片段是否有效
     * @param programObjectId
     */
    public static boolean validProgram(int programObjectId) {
        GLES30.glValidateProgram(programObjectId);
        final int[] programStatus = new int[1];
        GLES30.glGetProgramiv(programObjectId, GLES30.GL_VALIDATE_STATUS, programStatus, 0);
        return programStatus[0] != 0;
    }

}
