package com.zt.chroma_v2.utils;

/**
 * author : Tan Lang
 * e-mail : 2715009907@qq.com
 * date   : 2022/2/2113:09
 * desc   :
 * version: 1.0
 */

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;

public class ImageUtils {

    private static final String TAG = "ImageUtils";

    private static final String GALLERY_PATH = Environment.getExternalStoragePublicDirectory(Environment
            .DIRECTORY_DCIM) + File.separator + "Camera";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    public static Bitmap rotateBitmap(Bitmap source, int degree, boolean flipHorizontal, boolean recycle) {
        if (degree == 0) {
            return source;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        if (flipHorizontal) {
            matrix.postScale(-1, 1); // 前置摄像头存在水平镜像的问题，所以有需要的话调用这个方法进行水平镜像
        }
        Bitmap rotateBitmap = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
        if (recycle) {
            source.recycle();
        }
        return rotateBitmap;
    }



    private Bitmap readPixel(int i,int mWidth, int mHeight){
        Bitmap bitmap=Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        ByteBuffer mBuffer = ByteBuffer.allocate(mWidth * mHeight * 4);
        GLES30.glReadBuffer(GLES30.GL_COLOR_ATTACHMENT0+i);
        GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, mBuffer);

        bitmap.copyPixelsFromBuffer(mBuffer);
        return bitmap;
    }

    public static void saveImageToGallery(Context context, Bitmap bmp) {
        //检查有没有存储权限
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.e(TAG,"无权限");
        } else {
            // 新建目录appDir，并把图片存到其下
//            File appDir = new File(context.getExternalFilesDir(null).getPath()+ "BarcodeBitmap");
//            if (!appDir.exists()) {
//                appDir.mkdir();
//            }
//            String fileName = System.currentTimeMillis() + ".jpg";

            String appDir = Environment.getExternalStorageDirectory()+"/DCIM/Camera";
            String fileName = System.currentTimeMillis() + ".jpg"; //
            File file = new File(appDir, fileName);

            try {
                FileOutputStream fos = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 把file里面的图片插入到系统相册中
//            try {
//                MediaStore.Images.Media.insertImage(context.getContentResolver(),
//                        file.getAbsolutePath(), fileName, null);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }

            Log.d(TAG,"成功插入");

            // 通知相册更新
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        }
    }

    public static Bitmap cropImage(Bitmap bitmap, double wRatio, double hRatio, boolean middle) {
        // 只实现HRatio>WRatio情况
        if (wRatio>hRatio){
            Log.e(TAG,"Please sure wRatio <= Ratio");
            return null;
        }

        int startX = 0;
        int startY = 0;

        int bmpWidth = bitmap.getWidth();
        int bmpHeight = bitmap.getHeight();

        double imageRatio = (double)bmpHeight / (double)bmpWidth;
        double paramRatio = hRatio / wRatio;

        if (paramRatio>imageRatio){
            bmpWidth = (int) ((double) bmpHeight *  (wRatio/hRatio));
            startX = (bitmap.getWidth() - bmpWidth)/2;
        }else {
            bmpHeight = (int) ((double) (bmpWidth*hRatio) / wRatio);
            startY = (bitmap.getHeight() - bmpHeight)/2;
        }

        return middle? Bitmap.createBitmap(bitmap,startX,startY, bmpWidth, bmpHeight):
                Bitmap.createBitmap(bitmap,0,0, bmpWidth, bmpHeight);
    }

    public static Bitmap resizeImage(Bitmap bitmap, int newWidth, int newHeight){

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // 缩放比例
        float scaleWidth = (float) newWidth/width;
        float scaleHeight = (float) newHeight/height;
        // 缩放矩阵
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth,scaleHeight);

        return Bitmap.createBitmap(bitmap,0,0,width,height,matrix,true);
    }

}

