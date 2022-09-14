package com.zt.chroma_v2.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Range;
import android.util.Size;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.VideoCaptureConfig;
import androidx.lifecycle.LifecycleOwner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * author : Tan Lang
 * e-mail : 2715009907@qq.com
 * date   : 2022/2/2221:30
 * desc   :
 * version: 1.0
 */

public class CameraXHelper implements Serializable {
    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Size mScreenSize;
    private HandlerThread handlerThread;

    private CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;
    private Preview.OnPreviewOutputUpdateListener listener;

    private ImageCapture mImageCapture;
    private Preview mPreview;
    private VideoCapture mVideoCapture;

    public CameraXHelper(Context context) throws CameraAccessException {
        this.mContext = context;
        this.mLifecycleOwner = (LifecycleOwner) context;
        this.mScreenSize = getScreenSize();

        // 获取相机参数
//        List<android.util.Size> tmp = getCameraSupportedSize();
//        android.util.Size opti = getOptimalSize(tmp,mScreenSize.getWidth(),mScreenSize.getHeight());
    }

    public void startPreview(Preview.OnPreviewOutputUpdateListener listener){
        this.listener = listener;
        setParams();
        mPreview.removePreviewOutputListener();
        mPreview.setOnPreviewOutputUpdateListener(listener);
        CameraX.unbindAll();
        CameraX.bindToLifecycle(mLifecycleOwner,mImageCapture,mPreview,mVideoCapture);
    }

    public void setCurrentFacing(int selector){
        currentFacing = selector==0 ? CameraX.LensFacing.BACK :  CameraX.LensFacing.FRONT;
    }

    @SuppressLint("RestrictedApi")
    private void setParams(){
        // 分辨率并不是最终分辨率，CameraX会自动根据设备支持情况，结合你的参数，设置一个最接近的分辨率
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetResolution(new Size(2160,1080)) // new Size(mScreenSize.getWidth(),mScreenSize.getHeight()
                .setLensFacing(currentFacing) // 前置或者后置摄像头
                .build();
        // 得到它的数据
        mPreview = new Preview(previewConfig);

        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setLensFacing(currentFacing)
                .setTargetResolution(new Size(mScreenSize.getWidth(), mScreenSize.getHeight()))
                .build();
        mImageCapture = new ImageCapture(imageCaptureConfig);

        VideoCaptureConfig videoCaptureConfig = new VideoCaptureConfig.Builder()
                .setLensFacing(currentFacing)
                .build();
        mVideoCapture = new VideoCapture(videoCaptureConfig);

    }

    public ImageCapture getImageCapture(){
        return mImageCapture;
    }

    public void turnCamera(int CAMERA_CODE){
        currentFacing = CAMERA_CODE==0 ? CameraX.LensFacing.BACK : CameraX.LensFacing.FRONT;

        CameraX.unbindAll();
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetResolution(new Size(mScreenSize.getWidth(),mScreenSize.getHeight()))
                .setLensFacing(currentFacing) // 前置或者后置摄像头
                .build();
        mPreview = new Preview(previewConfig);
        mPreview.setOnPreviewOutputUpdateListener(listener);

        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setTargetResolution(new Size(mScreenSize.getWidth(), mScreenSize.getHeight()))
                .setLensFacing(currentFacing)
                .build();
        mImageCapture = new ImageCapture(imageCaptureConfig);
        CameraX.bindToLifecycle(mLifecycleOwner,mImageCapture,mPreview);
    }

    public Size getScreenSize(){
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        return new Size(dm.widthPixels,dm.heightPixels);
    }

    private List<android.util.Size> getCameraSupportedSize() throws CameraAccessException {
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        List<android.util.Size> supportedUtilSize = new ArrayList<>();

        String[] s = cameraManager.getCameraIdList(); // 0 表示后置，1表示前置

        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(s[0]);
        StreamConfigurationMap configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        supportedUtilSize = Arrays.asList(configurationMap.getOutputSizes(ImageFormat.JPEG));

        Range<Integer>[] ranges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);

        return supportedUtilSize;
    }

    private static android.util.Size getOptimalSize(@NonNull List<android.util.Size> sizes, int w, int h) {

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;
        android.util.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (android.util.Size size : sizes) {
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (android.util.Size size : sizes) {
                if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - targetHeight);
                }
            }
        }

        return optimalSize;
    }


    private void updateTransform(TextureView textureView) {
        Matrix matrix = new Matrix();
        // Compute the center of the textureview
        float centerX = textureView.getWidth() / 2f;
        float centerY = textureView.getHeight() / 2f;

        float[] rotations = {0, 90, 180, 270};
        // Correct preview output to account for display rotation
        float rotationDegrees = rotations[textureView.getDisplay().getRotation()];

        matrix.postRotate(-rotationDegrees, centerX, centerY);

        // Finally, apply transformations to our TextureView
        textureView.setTransform(matrix);
    }
}
