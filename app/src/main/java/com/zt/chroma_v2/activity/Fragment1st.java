package com.zt.chroma_v2.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.zt.chroma_v2.R;
import com.zt.chroma_v2.utils.ImageUtils;

import java.io.IOException;
import java.io.InputStream;

public class Fragment1st extends Fragment implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    private final static String TAG = "Fragment1st";

    private View mView;
    private Bundle mBundle;
    private PopupWindow popupWindow;
    private ImageView mImageView;
    private ImageView mMaskBackground;
    private Bitmap backgroundBitmap;
    private Size mScreenSize;
    // Buttons
    private Button chooseBg;
    private Button startChroma;
    private RadioGroup cameraSelector;

    public Fragment1st() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            mView = inflater.inflate(R.layout.first_fragment, container, false);
            mScreenSize = getScreenSize();
            setUIParams();
            setBundle();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mView;
    }

    private void setUIParams(){
        cameraSelector = mView.findViewById(R.id.cameraSelector);
        mImageView = mView.findViewById(R.id.imageView);
        chooseBg = mView.findViewById(R.id.chooseBackground);
        startChroma = mView.findViewById(R.id.startChroma);
        mMaskBackground = mView.findViewById(R.id.maskBackground);
        mMaskBackground.getForeground().setAlpha(150);

        cameraSelector.setOnCheckedChangeListener(this);
        chooseBg.setOnClickListener(this);
        startChroma.setOnClickListener(this);
    }

    private void setBundle() throws Exception {
        mBundle = new Bundle();
        mBundle.putInt("cameraSelector",1); // ????????????????????????????????????

        // ???????????????Bundle??????
        Bundle lastBundle = getArguments();
        Uri imageUri = null;
        String imagePath = null;
        if(lastBundle!=null){
            // ??????????????????uri
            imageUri = lastBundle.getParcelable("bgBitmapUri");
            if(imageUri!=null){
                setImageView(imageUri);
                mBundle.putParcelable("bgBitmapUri",imageUri);
            }
            // ??????assets??????uri
            imagePath = lastBundle.getString("imagePath");
            if(imagePath!=null){
                mBundle.putString("imagePath", imagePath);
                setImageView(imagePath);
            }
        }
        if(imageUri==null&&imagePath==null){
            setImageView("background/no18.png");
        }
    }

    private void setImageView(Uri imageUri) throws Exception {
        // ???????????????????????????
        byte[] content = FragmentPreviewBackground.readStream(getActivity().getContentResolver().openInputStream(Uri.parse(imageUri.toString())));
        // ?????????????????????ImageView????????????Bitmap??????
        backgroundBitmap = FragmentPreviewBackground.getPicFromBytes(content,null);
        // bitmap?????? ????????????????????????
        backgroundBitmap = ImageUtils.cropImage(backgroundBitmap,mScreenSize.getWidth(),mScreenSize.getHeight(),true);

        mImageView.setImageBitmap(backgroundBitmap);
    }

    private void setImageView(String imagePath){
        // ??????????????????
        InputStream assetImage = null;
        Bitmap imageBitmap = null;
        try {
            assetImage = getResources().getAssets().open(imagePath);
            imageBitmap = BitmapFactory.decodeStream(assetImage);
            imageBitmap = ImageUtils.cropImage(imageBitmap,mScreenSize.getWidth(),mScreenSize.getHeight(),true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mImageView.setImageBitmap(imageBitmap);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.chooseBackground:
                Log.d(TAG,"choose background onclick");
                // ????????????
                initPopWindow();
                break;
            case R.id.startChroma:
                Navigation.findNavController(mView).navigate(R.id.action_FirstFragment_to_SecondFragment,mBundle);
                break;
            case R.id.inside_background:
                //??????????????????
                mBundle.putString("choose_bg","inside");
                Navigation.findNavController(mView).navigate(R.id.action_FirstFragment_to_inside_b_preview,mBundle);
                dismissPopWindow();
                break;
            case R.id.gallery_background:
                //??????????????????
                mBundle.putString("choose_bg","gallery");
                Navigation.findNavController(mView).navigate(R.id.action_FirstFragment_to_chooseBg_gallery,mBundle);
                dismissPopWindow();
                break;
            case R.id.cancel:
                dismissPopWindow();
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId){
            case R.id.frontCamera:
                mBundle.putInt("cameraSelector",0);
                break;
            case R.id.backCamera:
                mBundle.putInt("cameraSelector",1);
                break;
        }
    }

    private void initPopWindow(){
        View view =  LayoutInflater.from(this.getContext()).inflate(R.layout.pop_choose_background,null,false);
        Button insideBgBtn = view.findViewById(R.id.inside_background);
        Button galleryBgBtn = view.findViewById(R.id.gallery_background);
        Button cancelBtn = view.findViewById(R.id.cancel);
        // ??????measure??????,?????????????????????0?????????linearLayout?????????????????????
        LinearLayout linearLayout = view.findViewById(R.id.choose_bg_layout);
        linearLayout.measure(0,0);

        // ??????PopupWindow
        popupWindow = new PopupWindow(view,ViewGroup.LayoutParams.MATCH_PARENT,linearLayout.getMeasuredHeight(),true);
        popupWindow.setAnimationStyle(R.style.pop_anim);  // ??????????????????

        // ????????????????????????popupWindow??????
        popupWindow.setTouchable(true);
        popupWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });

        // ????????????????????????
        popupWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
        // ??????????????????
        popupWindow.showAtLocation(mView, Gravity.BOTTOM|Gravity.CENTER,0,0);
        // ??????????????????
        insideBgBtn.setOnClickListener(this);
        galleryBgBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
    }

    // ??????popWindow
    private void dismissPopWindow(){
        if(popupWindow !=null && popupWindow.isShowing()){
            popupWindow.dismiss();
            popupWindow = null;
        }
    }

    public Size getScreenSize(){
        DisplayMetrics dm = getActivity().getApplicationContext().getResources().getDisplayMetrics();
        return new Size(dm.widthPixels,dm.heightPixels);
    }

}
