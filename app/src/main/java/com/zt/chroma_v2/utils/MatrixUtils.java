package com.zt.chroma_v2.utils;

import android.opengl.Matrix;

public enum MatrixUtils {
    ;
    public static final int TYPE_INSIDE_START = 0;
    public static final int TYPE_INSIDE_CENTER = 1;
    public static final int TYPE_INSIDE_END = 2;
    public static final int TYPE_FULL_BY_CROP = 3;
    public static final int TYPE_FULL_BY_FIT = 4;
    MatrixUtils(){}

    private static int mMatrixType;

    public static void getMatrixByType(float[] matrix, int type, int imgWidth, int imgHeight, int viewWidth, int viewHeight, boolean outputToFBO) {
        if(imgHeight>0 && imgWidth>0 && viewWidth>0 && viewHeight>0) {
            float[] projection = new float[16];
            float[] camera = new float[16];
            float imgRatio = (float)imgWidth / imgHeight;
            float viewRatio = (float)viewWidth / viewHeight;

            float left, right, bottom, top;
            switch (type) {
                case TYPE_INSIDE_START:
                    left = -1;
                    top = 1;
                    if (imgRatio > viewRatio) {
                        right = 1;
                        bottom = 1 - 2*imgRatio/viewRatio;
                    } else {
                        right = (-1 + 2*viewRatio/imgRatio);
                        bottom = -1;
                    }
                    break;
                case TYPE_INSIDE_CENTER:
                    if (imgRatio > viewRatio) {
                        left = -1;
                        right = 1;
                        bottom = -imgRatio/viewRatio;
                        top = -bottom;
                    } else {
                        left = -viewRatio/imgRatio;
                        right = -left;
                        bottom = -1;
                        top = 1;
                    }
                    break;
                case TYPE_INSIDE_END:
                    right = 1;
                    bottom = -1;
                    if (imgRatio > viewRatio) {
                        left = -1;
                        top = -1 + 2*imgRatio/viewRatio;
                    } else {
                        left = 1 - 2*viewRatio/imgRatio;
                        top = 1;
                    }
                    break;
                case TYPE_FULL_BY_CROP:
                    if (imgRatio > viewRatio) {
                        left = -viewRatio/imgRatio;
                        right = -left;
                        bottom = -1;
                        top = 1;
                    } else {
                        left = -1;
                        right = 1;
                        bottom = -imgRatio/viewRatio;
                        top = -bottom;
                    }
                    break;
                case TYPE_FULL_BY_FIT:
                default:
                    left = -1;
                    right = 1;
                    bottom = -1;
                    top = 1;
                    break;
            }
            mMatrixType = type;
            if (outputToFBO) {
                float wap = bottom;
                bottom = top;
                top = wap;
            }
            Matrix.orthoM(projection,0, left, right, bottom, top,1,3);
            Matrix.setLookAtM(camera,0,0,0,1,0,0,0,0,1,0);
            Matrix.multiplyMM(matrix,0,projection,0,camera,0);
        }
    }

    public static void rotate(float[] m, float angle) {
        if (angle == 0) {
            return;
        }
        Matrix.rotateM(m,0, angle, 0, 0, 1);
    }

    public static void flip(float[] m, boolean x, boolean y) {
        if (x || y) {
            Matrix.scaleM(m,0, x?-1:1, y?-1:1,1);
        }
    }

    public static void scale(float[] m, float x, float y){
        if (x == 0 && y == 0) {
            return;
        }
        Matrix.scaleM(m, 0, x, y, 1);
    }

    public static void translate(float[] m, int xOffset, int yOffset, int imgWidth, int imgHeight, int viewWidth, int viewHeight) {
        float x = 0, y = 0;
        float imgRatio = (float)imgWidth / imgHeight;
        float viewRatio = (float)viewWidth / viewHeight;

        switch (mMatrixType) {
            case TYPE_INSIDE_START:
            case TYPE_INSIDE_CENTER:
            case TYPE_INSIDE_END:
                if (imgRatio > viewRatio) {
                    x = 2.0f * xOffset / viewWidth;
                    y = 2.0f * yOffset / (viewWidth / imgRatio);
                } else {
                    x = 2.0f * xOffset / (viewHeight * imgRatio);
                    y = 2.0f * yOffset / viewHeight;
                }
                break;
            case TYPE_FULL_BY_FIT:
            case TYPE_FULL_BY_CROP:
                x = 2.0f * xOffset / viewWidth;
                y = 2.0f * yOffset / viewHeight;
                break;
        }

        Matrix.translateM(m, 0, x, y, 0);
    }

    public static float[] getOriginalMatrix(){
        return new float[]{
                1,0,0,0,
                0,1,0,0,
                0,0,1,0,
                0,0,0,1
        };
    }

}
