package com.zt.chroma_v2.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SyncContext;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.opengl.EGLContext;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.TokenWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.zt.chroma_v2.egl.CoreProcess;
import com.zt.chroma_v2.render.RecordRender;
import com.zt.chroma_v2.utils.ShaderUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by lb6905 on 2017/7/20.
 */

public class Camera2Helper {

    public static final String TAG = "Camera2Helper";

    private Activity mActivity;
    private TextureView mPreviewView;
    private CameraDevice mCameraDevice;
    private String mCameraId;
    private Size mPreviewSize;
    private int mCurrentFacing;
    private ImageReader mImageReader;
    private Surface mImageSurface;
    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private int targetWidth;
    private int targetHeight;
    private CameraManager mCameraManager;
    private SurfaceTexture mSurfaceTexture;
    private CaptureRequest previewRequest;
    private CameraCaptureSession mCameraCaptureSession;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private Surface previewSurface;

    // 录制相关属性
    private CaptureRequest recordRequest;
    private MediaRecorder mediaRecorder;
    private Surface recorderSurface;
    private File mNextVideoAbsolutePath;
    private MediaCodec mediaCodec;
    private MediaMuxer mediaMuxer;
    private Handler eglCodecHandler;
    private CoreProcess coreProcess;
    private RecordRender recordRender;
    private float speed;
    private boolean flagDraw;
    private boolean flagCodecEnd;
    private int[] mRenderOutputFboTexture;
    private int avcIndex;
    private HandlerThread eglCodecThread;
    private MediaCodec.BufferInfo mBufferInfo;
    private Object mRecordLock;

    public Camera2Helper(Activity activity) {
        mActivity = activity;
        mCameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        startCameraThread();
    }

    public void setPreviewTexture(TextureView textureView) {
        mPreviewView = textureView;
        // 初始化预览 surface

        // 对textureView进行状态监听
        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                // textureView可用时
                mSurfaceTexture = textureView.getSurfaceTexture();
                startPreview();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        };
        mPreviewView.setSurfaceTextureListener(surfaceTextureListener);

    }

    public TextureView.SurfaceTextureListener getSurfaceTextureListener(){
        return surfaceTextureListener;
    }

    public void setCurrentFacing(int facing){
        mCurrentFacing = facing;
    }

    public int getCurrentFace(){
        return mCurrentFacing;
    }

    public void startPreview(){
        releaseCamera();
        setupCamera(targetWidth,targetHeight,mCurrentFacing);
        openCamera();
    }

    public void startCameraThread() {
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {

            }
        };
    }

    public void turnCamera(){
        if (mCurrentFacing==CameraCharacteristics.LENS_FACING_FRONT){
            mCurrentFacing = CameraCharacteristics.LENS_FACING_BACK;
        }else if(mCurrentFacing==CameraCharacteristics.LENS_FACING_BACK){
            mCurrentFacing = CameraCharacteristics.LENS_FACING_FRONT;
        }
    }

    /*
        功能：
            1.根据目标尺寸选择最优预览尺寸
            2.选择前置|后置摄像头
        参数：
            width: 目标宽度
            height: 目标高度
            FACING: 0前置，1后置
     */
    public void setupCamera(int width, int height,int FACING) {
        try {
            for (String id : mCameraManager.getCameraIdList()) {  // id 0后置，1前置
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == FACING) {
                    //获取相机输出格式/尺寸参数
                    StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    mPreviewSize = getOptimalSize(configs.getOutputSizes(SurfaceTexture.class), width, height);
                    mCameraId = id;
                    Log.i(TAG, "preview width = " + mPreviewSize.getWidth() + ", height = " + mPreviewSize.getHeight() + ", cameraId = " + mCameraId);
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private static Size getOptimalSize(@NonNull Size sizes[], int targetWidth, int targetHeight) {
        // 相机宽高和屏幕宽高是相反的
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) targetHeight / targetWidth;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Size size : sizes) {
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.getHeight() - targetWidth) + Math.abs(size.getWidth() - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - targetWidth) + Math.abs(size.getWidth() - targetHeight);
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - targetWidth) + Math.abs(size.getWidth() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - targetWidth) + Math.abs(size.getWidth() - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public boolean openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // 请求权限 未实现
                return false;
            }
            mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            mCameraDevice = camera;
                            // 创建相机 session
                            createSession(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {
                            releaseCamera();
                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {
                            releaseCamera();
                        }
                    },
                    mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public void createSession(int mWidth, int mHeight){
        // 初始化拍照用的ImageReader 和 surface
        initReaderAndSurface();
        try{
            mSurfaceTexture.setDefaultBufferSize(mWidth,mHeight);
            previewSurface =  new Surface(mSurfaceTexture);

            // 预览捕获请求
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//            builder.set(CaptureRequest.JPEG_ORIENTATION, 90);
            builder.addTarget(previewSurface);  // 作为预览数据的显示
            previewRequest = builder.build();

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface,mImageSurface),new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try{
                        mCameraCaptureSession = session;
                        // 设置重复请求，连续获取预览数据
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, null, mCameraHandler);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            },mCameraHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // TAKE PHOTO===========================
    public void initReaderAndSurface(){
        // 初始化拍照 ImageReader
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(),mPreviewSize.getHeight(), ImageFormat.JPEG,1);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                writeImage2File();
            }
        },mCameraHandler);

        mImageSurface = mImageReader.getSurface();
    }

    public void writeImage2File(){
        Image image = mImageReader.acquireNextImage();
        if(image==null){
            return;
        }

        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[byteBuffer.remaining()];
        byteBuffer.get(data);  // 字节buffer数据 转入 字节数组
        FileOutputStream fos = null;

        //创建文件
        // 新建目录appDir，并把图片存到其下
        String appDir = Environment.getExternalStorageDirectory()+"/DCIM/Camera";   // 插入到照相机专属图库里
        String fileName = System.currentTimeMillis() + ".jpg"; //
        File file = new File(appDir, fileName);
        try{
            fos = new FileOutputStream(file);
            fos.write(data);
        }catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                image.close();
            }
        }

        // 通知相册更新
        mActivity.getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        Toast.makeText(mActivity.getApplicationContext(), "拍照完成，已保存 ", Toast.LENGTH_SHORT).show();
    }

    public void takePhoto(){
        try {
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(mImageSurface);  // 作为预览数据的显示
            // 拍照图像旋转
            if (mCurrentFacing==1){
                builder.set(CaptureRequest.JPEG_ORIENTATION,90);
            }else if (mCurrentFacing==0){
                builder.set(CaptureRequest.JPEG_ORIENTATION,270);
            }
            // 使用AF模式
            builder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            CaptureRequest takePhotoRequest = builder.build();

            mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession.capture(takePhotoRequest, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    try {
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        Log.d(TAG, "相机访问异常");
                    }
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d(TAG, "相机访问异常");
        }
    }
    // TAKE PHOTO=============================

    // RECODE=============================
    // textureView录制

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setUpMediaRecorder() {
        if (null == mActivity) {
            return;
        }
        try {
            mediaRecorder = new MediaRecorder();

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);       // 录音
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);   // 录视频
            // 设置旋转，保持竖屏录制，前置相机默认输出逆时针旋转270，后置默认输出逆时针旋转90
            if (mCurrentFacing == 0) {
                // 前置
                mediaRecorder.setOrientationHint(270); // 设置顺时针旋转
            } else {
                // 后置
                mediaRecorder.setOrientationHint(90);
            }

            // 设置输出格式
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            // 设置存储路径
            String appDir = Environment.getExternalStorageDirectory() + "/DCIM/Camera";   // 插入到照相机专属图库里
            String fileName = System.currentTimeMillis() + ".mp4"; //
            mNextVideoAbsolutePath = new File(appDir, fileName);
            mediaRecorder.setOutputFile(mNextVideoAbsolutePath);
            mediaRecorder.setVideoEncodingBitRate(mPreviewSize.getWidth() * mPreviewSize.getHeight() * 30 / 5);

            // 设置音视频编码
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            // 设置帧率和输出尺寸
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.setVideoSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }


        recorderSurface = mediaRecorder.getSurface();
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startRecordingVideo(){
        // 初始化摄像用的 mediaRecorder和 surface
        setUpMediaRecorder();
        try {
            // 创建录制请求
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            // 为预览添加surface
            builder.addTarget(previewSurface);
            // 为MediaRecorder设置surface
            builder.addTarget(recorderSurface);

            recordRequest = builder.build();
            mCameraCaptureSession.stopRepeating();
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface,recorderSurface),new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try{
                        mCameraCaptureSession = session;
                        // 设置重复请求，连续获取预览数据
                        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                        mCameraCaptureSession.setRepeatingRequest(recordRequest, null, mCameraHandler);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG,"Start recordVideo ----- 开始录制");
                            Toast.makeText(mActivity.getApplicationContext(), "开始录制", Toast.LENGTH_SHORT).show();
                            mediaRecorder.start();
                        }
                    });
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            },mCameraHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    public void endRecordVideo(){
        mediaRecorder.stop();
        mediaRecorder.reset();
        createSession(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Toast.makeText(mActivity.getApplicationContext(), "录制完成，已保存", Toast.LENGTH_SHORT).show();
        // 通知相册更新
        mActivity.getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mNextVideoAbsolutePath)));
        mNextVideoAbsolutePath = null;

    }

    // surfaceView录制
    public void startRecordingVideo(EGLContext sharedContext, int SPEED_MODE,int[] renderOutputFboTexture){
        mRenderOutputFboTexture = renderOutputFboTexture;
        switch (SPEED_MODE){
            case 1:
                // 慢速
                speed = 0.5f;
                break;
            case 2:
                // 正常
                speed = 1.0f;
                break;
            case 3:
                // 快速
                speed = 1.5f;
                break;
        }
        mRecordLock = new Object();
        setMediaCodec();
        bindEGLSurface(sharedContext);
    }

    public void setMediaCodec(){
        // 设置存储路径
        String appDir = Environment.getExternalStorageDirectory()+"/DCIM/Camera";   // 插入到照相机专属图库里
        String fileName = System.currentTimeMillis() + ".mp4"; //
        mNextVideoAbsolutePath = new File(appDir, fileName);

        try {
            // 混合器，将编码的h264封装成mp4
            mediaMuxer = new MediaMuxer(mNextVideoAbsolutePath.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            // 生成mediaCodec配置
            MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,  mPreviewSize.getHeight(), mPreviewSize.getWidth());  // 和渲染尺寸宽高相反
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface); // 颜色空间从surface中获取
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mPreviewSize.getWidth() * mPreviewSize.getHeight() * 30 / 5);  //
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 20);

            // 创建编码器
            mediaCodec  = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            // 配置编码器
            mediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            // mediaCodec自动编码 inputSurface 中的图像
            recorderSurface = mediaCodec.createInputSurface();
            mediaCodec.start();
            // 从输出缓冲区中，获取编码后的数据,所以先获取到输出缓冲区。
            mBufferInfo = new MediaCodec.BufferInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void bindEGLSurface(EGLContext sharedContext){
        eglCodecThread = new HandlerThread("eglCodec");
        eglCodecThread.start();

        eglCodecHandler = new Handler(eglCodecThread.getLooper());
        eglCodecHandler.post(new Runnable() {
            @Override
            public void run() {
                eglConfigBase(recorderSurface,sharedContext);
            }
        });

    }

    /**
     * @param recorderSurface    MediaCodec创建的surface 我们需要将其贴到我们的虚拟屏幕上去
     * @param sharedContext GLThread的EGL上下文
     */
    public void eglConfigBase(Surface recorderSurface, android.opengl.EGLContext sharedContext) {
        coreProcess = new CoreProcess(sharedContext);
        recordRender = new RecordRender(mActivity.getResources(), false, recorderSurface);
        recordRender.setRenderSize(mPreviewSize.getHeight(),mPreviewSize.getWidth());  //
        recordRender.setInputFboID(mRenderOutputFboTexture);
        recordRender.onSurfaceCreated(coreProcess);
        recordTest();
    }

    public void recordTest(){

        eglCodecHandler.post(new Runnable() {
            @Override
            public void run() {
                flagDraw = true;
                while (flagDraw){
                    recordRender.onDrawFrame(coreProcess);
                }
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                flagCodecEnd  = false;
                while (true){
                    // 使用同步锁，防止停止录制时候，mediaMuxer 继续write outputBuffer
                    synchronized (mRecordLock){
                        if (flagCodecEnd){
                            break;
                        }
                        getCodec();
                    }
                }
            }
        }).start();
    }

    /*
        功能： 从MediaCodec的输出队列中，拿编码完成的数据，给mediaMuxer保存
     */
    private void getCodec() {

        //标记结束信号
        if (flagCodecEnd) {
            mediaCodec.signalEndOfInputStream();
            return;
        }

        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(mBufferInfo,10_000);
        //还需要更多数据才能编码，需要在等一会
        if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {

        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            //输出格式发生改变，开启混合器，第一次总会调用，
            MediaFormat outputFormat = mediaCodec.getOutputFormat();
            //设置视频轨
            avcIndex = mediaMuxer.addTrack(outputFormat);
            mediaMuxer.start();
        } else if (outputBufferIndex >= 0) {
            //获取输出缓冲区编码后的数据,正常情况下，encoderStatus表示缓冲区的下标
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);

            //如果当前的buffer是配置信息，不用写进去
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) !=0) {
                mBufferInfo.size = 0;
            }
            if (mBufferInfo.size > 0) {
                //调整时间戳,实现快速，慢速录像
                mBufferInfo.presentationTimeUs = (long)(mBufferInfo.presentationTimeUs / speed);
                //设置从哪里开始读数据（读出来就是编码后的数据）
                outputBuffer.position(mBufferInfo.offset);
                //设置可读数据的总长度
                outputBuffer.limit(mBufferInfo.size + mBufferInfo.offset);
                //写到mp4文件中
                mediaMuxer.writeSampleData(avcIndex, outputBuffer, mBufferInfo);

            }
            //释放缓冲区，后续可以存放新的编码后的数据
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            //结束信号
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                flagCodecEnd = true;
            }

        }
    }

    public void stopRecordingVideo(){

        synchronized (mRecordLock){
            flagDraw = false;
            flagCodecEnd = true;
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }

        eglCodecHandler.post(new Runnable() {
            @Override
            public void run() {
                recordRender.onSurfaceDestroyed(coreProcess);
                recordRender = null;
                recorderSurface.release();
                recorderSurface = null;
                eglCodecHandler.getLooper().quitSafely();
                eglCodecHandler = null;
                eglCodecThread.quit();
            }
        });
        //等待线程退出
        try{
            eglCodecThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Toast.makeText(mActivity.getApplicationContext(), "录制完成，已保存", Toast.LENGTH_SHORT).show();
        // 通知相册更新
        mActivity.getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mNextVideoAbsolutePath)));
        mNextVideoAbsolutePath = null;

    }
    // RECODE=============================

    public void setTargetSize(int width, int height){
        targetWidth = width;
        targetHeight = height;
    }

    public Size getScreenSize(){
        DisplayMetrics dm = mActivity.getApplicationContext().getResources().getDisplayMetrics();
        return new Size(dm.widthPixels,dm.heightPixels);
    }

    public Size getPreviewSize(){
        return mPreviewSize;
    }

    public void setSurfaceTexture(int textureId){
        mSurfaceTexture = new SurfaceTexture(textureId);
    }
    public SurfaceTexture  getSurfaceTexture(){
        return mSurfaceTexture;
    }

    public void releaseCamera(){
        if (mImageReader!=null){
            mImageReader.close();
            mImageReader = null;
        }
        if (mCameraCaptureSession!=null){
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if(mCameraDevice!=null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    public void release(){
        releaseCamera();
        // 关闭camera线程
        if (mCameraThread.isAlive()){
            try{
                mCameraThread.quitSafely();
                mCameraThread.join();
                mCameraThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
