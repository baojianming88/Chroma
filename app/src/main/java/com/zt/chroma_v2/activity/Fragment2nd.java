package com.zt.chroma_v2.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.zt.chroma_v2.R;
import com.zt.chroma_v2.camera.Camera2Helper;
import com.zt.chroma_v2.functions.ChromaManager;
import com.zt.chroma_v2.functions.SourceManager;
import com.zt.chroma_v2.utils.ImageUtils;

import java.io.IOException;
import java.io.InputStream;

public class Fragment2nd extends Fragment implements View.OnClickListener {

    private final static String TAG = "Fragment2nd";
    private View view;
    private SurfaceView mChromaView;
    private SurfaceView mSourceView;

    private Bitmap mBgBitmap;

    // Buttons
    private Button chromaMBtn;
    private Button sourceMBtn;
    private View turnBtn;
    private View backBtn;
    private ImageButton takePictureMBtn;
    private ImageButton recordMBtn;
    private ImageButton takePictureBtn;
    private ImageButton recordBtn;
    private ImageButton beautyBtn;
    private ImageButton filterBtn;
    private ImageButton LiveBtn;

    // Camera
    private Camera2Helper mCamera2Helper_chroma;
    private Camera2Helper mCamera2Helper_source;
    private ChromaManager mChromaManager;
    private SourceManager mSourceManager;
    private Size mRenderSize;
    private Bundle mBundle = null;

    public Fragment2nd() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.second_fragment, container, false);
        // ????????????
        chromaMBtn = view.findViewById(R.id.chromaMode);
        sourceMBtn = view.findViewById(R.id.sourceMode);
        // ????????????
        backBtn = view.findViewById(R.id.back2home);
        // ??????????????????
        turnBtn = view.findViewById(R.id.turnCamera);
        // ????????????
        takePictureMBtn = view.findViewById(R.id.takePhotoMode);
        takePictureBtn = view.findViewById(R.id.take_photo_click);
        // ????????????
        recordMBtn = view.findViewById(R.id.recMode);
        recordBtn = view.findViewById(R.id.rec_click);
        // ????????????
        beautyBtn = view.findViewById(R.id.beauty);
        // ????????????
        filterBtn = view.findViewById(R.id.filter);
        // ????????????
        LiveBtn = view.findViewById(R.id.live);

        // ??????surfaceView
        // ??????????????????surfaceView
        mChromaView = view.findViewById(R.id.ChromaView);
        mSourceView = view.findViewById(R.id.SourceView);

        chromaMBtn.setOnClickListener(this);
        sourceMBtn.setOnClickListener(this);
        backBtn.setOnClickListener(this);
        turnBtn.setOnClickListener(this);
        takePictureMBtn.setOnClickListener(this);
        takePictureBtn.setOnClickListener(this);
        recordMBtn.setOnClickListener(this);
        recordBtn.setOnClickListener(this);
        beautyBtn.setOnClickListener(this);
        filterBtn.setOnClickListener(this);
        LiveBtn.setOnClickListener(this);

        chromaMBtn.setSelected(false);
        sourceMBtn.setSelected(true);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Bundle??????
        setBundle();
        // ???????????????
        initCamera();
        // ChromaManager ?????????
        initChromaManager();
        // SourceManager?????????
        initSourceManager();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.sourceMode:
                if(recordBtn.isSelected()){
                    Toast.makeText(getContext(), "????????????????????????????????????", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (!sourceMBtn.isSelected()){
                    mCamera2Helper_chroma.releaseCamera();
                    mChromaManager.freezeRender();
                    mSourceManager.unFreezeRender();
                    mCamera2Helper_source.startPreview();

                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    RelativeLayout.LayoutParams layoutParams
                            = new RelativeLayout.LayoutParams(mRenderSize.getWidth(),mRenderSize.getHeight());
                    mSourceView.setLayoutParams(layoutParams);

                    sourceMBtn.setSelected(true);
                    chromaMBtn.setSelected(false);
                }
                break;
            case R.id.chromaMode:
                if(recordBtn.isSelected()){
                    Toast.makeText(getContext(), "????????????????????????????????????", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (!chromaMBtn.isSelected()) {
                    // sourceView???????????????????????????????????????????????????
                    mCamera2Helper_source.releaseCamera();
                    mSourceManager.freezeRender();
                    mChromaManager.unFreezeRender();

                    mCamera2Helper_chroma.startPreview();
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(1,1);
                    mSourceView.setLayoutParams(layoutParams);

                    chromaMBtn.setSelected(true);
                    sourceMBtn.setSelected(false);
                }
                break;
            case R.id.back2home:
                if (recordBtn.isSelected()){
                    Toast.makeText(getContext(), "????????????????????????????????????", Toast.LENGTH_SHORT).show();
                    break;
                }
                Navigation.findNavController(v).navigate(R.id.action_SecondFragment_to_FirstFragment,mBundle);   // ???fragment1 ???imageView??????
                release();
                break;
            case R.id.turnCamera:
                // ????????????
                turnCamera();
                break;
            case R.id.takePhotoMode:
                if(!takePictureMBtn.isSelected()){
                    if(recordBtn.isSelected()){
                        Toast.makeText(getContext(), "????????????????????????????????????", Toast.LENGTH_SHORT).show();
                    }else {
                        recordBtn.setVisibility(View.INVISIBLE);
                        takePictureBtn.setVisibility(View.VISIBLE);
                        takePictureMBtn.setSelected(true);
                        recordMBtn.setSelected(false);
                    }
                }else {
                    // ????????????
                    takePictureBtn.setVisibility(View.INVISIBLE);
                    takePictureMBtn.setSelected(false);
                }
                break;
            case R.id.take_photo_click:
                // ????????????
                Log.d(TAG,"Touch take photo button");
                takePictures();
                break;
            case R.id.recMode:
                if(!recordMBtn.isSelected()){
                    takePictureBtn.setVisibility(View.INVISIBLE);
                    recordBtn.setVisibility(View.VISIBLE);
                    takePictureMBtn.setSelected(false);
                    recordMBtn.setSelected(true);
                }else {
                    if(recordBtn.isSelected()){
                        Toast.makeText(getContext(), "????????????????????????????????????", Toast.LENGTH_SHORT).show();
                    }else {
                        // ????????????
                        recordBtn.setVisibility(View.INVISIBLE);
                        recordMBtn.setSelected(false);
                    }
                }
                break;
            case R.id.rec_click:
                // ????????????
                if(!recordBtn.isSelected()){
                    // ????????????
                    try {
                        startRecord();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    recordBtn.setSelected(true);
                }else {
                    // ????????????
                    recordBtn.setSelected(false);
                    stopRecord();
                }
                break;
            case R.id.beauty:
                beautyOn();
                break;
            case R.id.filter:
                filterOn();
                break;
            case R.id.live:
                popLiveWindow();
                break;
        }
    }

    private void setBundle() {
        mBundle = getArguments();
        // ?????????????????????
        initCamera();

        if(mBundle!=null){
            // ??????????????????uri
            Uri imageUri = mBundle.getParcelable("bgBitmapUri");
            if(imageUri!=null){
                try {
                    setBackground(imageUri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // ??????assets??????path
            String imagePath = mBundle.getString("imagePath");
            if (imagePath!=null){
                setBackground(imagePath);
            }
        }
    }

    private void setBackground(Uri imageUri) throws Exception {
        if (imageUri!=null){
            // ???????????????????????????
            byte[] content = FragmentPreviewBackground.readStream(getActivity().getContentResolver().openInputStream(Uri.parse(imageUri.toString())));
            // ?????????????????????ImageView????????????Bitmap??????
            mBgBitmap = FragmentPreviewBackground.getPicFromBytes(content,null);
            // bitmap?????? ????????????????????????
            mBgBitmap = ImageUtils.cropImage(mBgBitmap,mRenderSize.getWidth(),mRenderSize.getHeight(),true);
        }
    }

    private void setBackground(String path){
        // ??????????????????
        InputStream assetImage = null;
        Bitmap imageBitmap = null;
        try {
            assetImage = getResources().getAssets().open(path);
            imageBitmap = BitmapFactory.decodeStream(assetImage);
            imageBitmap = ImageUtils.cropImage(imageBitmap,mRenderSize.getWidth(),mRenderSize.getHeight(),true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mBgBitmap = imageBitmap;
    }

    private void initCamera(){
        mCamera2Helper_chroma = new Camera2Helper(getActivity());
        mRenderSize = mCamera2Helper_chroma.getScreenSize();
        mCamera2Helper_chroma.setTargetSize(mRenderSize.getWidth(),mRenderSize.getHeight());
        mCamera2Helper_chroma.setCurrentFacing(mBundle.getInt("cameraSelector"));

        mCamera2Helper_source = new Camera2Helper(getActivity());
        mRenderSize = mCamera2Helper_source.getScreenSize();
        mCamera2Helper_source.setTargetSize(mRenderSize.getWidth(),mRenderSize.getHeight());
        mCamera2Helper_source.setCurrentFacing(mBundle.getInt("cameraSelector"));
    }

    private void initChromaManager(){
        mChromaManager = new ChromaManager(getContext(), mChromaView, mRenderSize, mBgBitmap);
        // ??????mCamera2Helper???chromaManager,??????????????????surfaceTexture????????? render1stage ???inputTexture
        mChromaManager.setCamera2Helper(mCamera2Helper_chroma);
        mChromaManager.startPreview();
    }

    private void initSourceManager(){
        mSourceManager = new SourceManager(getContext(), mSourceView, mRenderSize);
        // ??????mCamera2Helper???chromaManager,??????????????????surfaceTexture????????? render1stage ???inputTexture
        mSourceManager.setCamera2Helper(mCamera2Helper_source);
        mSourceManager.startPreview();
    }

    private void popLiveWindow() {

    }

    private void filterOn() {

    }

    private void beautyOn() {

    }

    private void turnCamera() {
        mCamera2Helper_chroma.turnCamera();
        mCamera2Helper_source.turnCamera();
        if (chromaMBtn.isSelected()){
            mCamera2Helper_chroma.startPreview();
        }else if (sourceMBtn.isSelected()){
            mCamera2Helper_source.startPreview();
        }
    }

    private void takePictures() {
        if (chromaMBtn.isSelected()){
            mChromaManager.capturePicture();
        }else if (sourceMBtn.isSelected()){
            mCamera2Helper_source.takePhoto();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startRecord() throws IOException {
        if (chromaMBtn.isSelected()){
            mCamera2Helper_chroma.startRecordingVideo(mChromaManager.getSharedContext(),2,mChromaManager.getOutputFboTexture());
        }else if (sourceMBtn.isSelected()){
            mCamera2Helper_source.startRecordingVideo();
        }
    }

    private void stopRecord() {
        if (chromaMBtn.isSelected()){
            mCamera2Helper_chroma.stopRecordingVideo();
        }else if (sourceMBtn.isSelected()){
            mCamera2Helper_source.endRecordVideo();
        }
    }

    private void release(){
        if (mChromaManager.isStart){
            mChromaManager.stopPreview();
            mChromaManager = null;
        }
        if (mSourceManager.isStart){
            mSourceManager.stopPreview();
            mSourceManager = null;
        }
        if (mCamera2Helper_chroma!=null){
            mCamera2Helper_chroma.releaseCamera();
            mCamera2Helper_chroma = null;
        }
        if (mCamera2Helper_source!=null){
            mCamera2Helper_source.releaseCamera();
            mCamera2Helper_source = null;
        }
        if (mBundle!=null){
            mBundle.clear();
            mBundle = null;
        }

    }

}