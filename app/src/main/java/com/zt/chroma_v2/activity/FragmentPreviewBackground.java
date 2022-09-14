package com.zt.chroma_v2.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.zt.chroma_v2.R;
import com.zt.chroma_v2.utils.ImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * author : Tan Lang
 * e-mail : 2715009907@qq.com
 * date   : 2022/3/2414:22
 * desc   :
 * version: 1.0
 */
public class FragmentPreviewBackground extends Fragment implements View.OnClickListener {

    private final String TAG = "FragmentGallery";

    private static final int REQUEST_CODE_GALLERY = 0X10; // 图库选择图像请求码
    private static final int CROP_PHOTO = 0X12;         // 裁剪图像请求码
    private static final int WRITE_READ_PERMISSION = 0X20;     // 动态权限申请 请求码

    private File mImageFile = null;
    private Uri mCropImageUri = null;  // 裁剪后图像uri
    private Uri mImageUri = null;
    private String path = "";
    private Bitmap backgroundBitmap;
    private Size mScreenSize;

    private Bundle mLastBundle;
    private Bundle mBundle;
    private String mDestination;

    private View mView;
    private ImageView mImageView;

    // Buttons
    private ImageButton backBtn;
    private Button submitBtn;
    private Button chooseBtn;

    public FragmentPreviewBackground(){};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.preview_bg, container, false);
        mScreenSize = getScreenSize();
        mBundle = new Bundle();
        setUIParams();
        return mView;
    }

    private void setUIParams(){
        mImageView = mView.findViewById(R.id.cropPreview);

        backBtn = mView.findViewById(R.id.back2last);
        submitBtn = mView.findViewById(R.id.submit);
        chooseBtn = mView.findViewById(R.id.chooseBg);

        mLastBundle = getArguments();
        mDestination = mLastBundle.getString("choose_bg");
        if(mDestination=="inside"){
            chooseBtn.setText("内置选取图像");
            setInsideBgParam();
        }

        backBtn.setOnClickListener(this);
        submitBtn.setOnClickListener(this);
        chooseBtn.setOnClickListener(this);
    }

    public void setInsideBgParam(){
        String imagePath = mLastBundle.getString("imagePath");
        if(imagePath!=null){
            mBundle.putString("imagePath", imagePath);
            // 展示数据加载
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
    }

    @Override
    public void onClick(View v) {
        if(mDestination=="gallery") {
            switch (v.getId()) {
                case R.id.back2last:
                    Navigation.findNavController(v).navigate(R.id.action_chooseBg_gallery_to_FirstFragment);
                    break;
                case R.id.submit:
                    mBundle.putParcelable("bgBitmapUri", mImageUri);
                    Navigation.findNavController(v).navigate(R.id.action_chooseBg_gallery_to_FirstFragment, mBundle);
                    break;
                case R.id.chooseBg:
                    chooseFromGallery();
                    break;
            }
        }else {
            switch (v.getId()) {
                case R.id.back2last:
                    Navigation.findNavController(v).navigate(R.id.action_inside_bg_preview_to_firstFragment);
                    break;
                case R.id.submit:
                    Navigation.findNavController(v).navigate(R.id.action_inside_bg_preview_to_firstFragment, mBundle);
                    break;
                case R.id.chooseBg:
                    Navigation.findNavController(v).navigate(R.id.action_inside_bg_preview_to_inside_choose);
                    break;
            }
        }
    }

    private void chooseFromGallery(){
        // 申请权限
        String write = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        String read = Manifest.permission.READ_EXTERNAL_STORAGE;

        String[] write_read_permission = new String[] {write,read};

        int checkWrite = ContextCompat.checkSelfPermission(getContext(),write);
        int checkRead = ContextCompat.checkSelfPermission(getContext(),read);
        int ok = PackageManager.PERMISSION_GRANTED;

        if(checkWrite!=ok && checkRead!=ok){
            // 重新申请权限
            ActivityCompat.requestPermissions(this.getActivity(),write_read_permission,WRITE_READ_PERMISSION);
        }
        openGallery();
    }

    private void openGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // 显示所有照片
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
        startActivityForResult(intent,REQUEST_CODE_GALLERY);
    }

    public static Bitmap getPicFromBytes(byte[] bytes, BitmapFactory.Options opts) {
        if (bytes != null)
            if (opts != null)
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length,opts);
            else
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return null;
    }

    public static byte[] readStream(InputStream inStream) throws Exception {
        byte[] buffer = new byte[1024];
        int len = -1;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        byte[] data = outStream.toByteArray();
        outStream.close();
        inStream.close();
        return data;
    }

    private void pictureCropping(Uri uri) {
        // 调用系统中自带的图片剪裁
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        // 下面这个crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        // 缩放
        intent.putExtra("scale",true);
        // 返回裁剪后的数据
        intent.putExtra("return-data", true);

        startActivityForResult(intent, CROP_PHOTO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==WRITE_READ_PERMISSION&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)
            openGallery();
        else Toast.makeText(this.getContext(), "你拒绝了打开相册的权限", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ContentResolver resolver = getActivity().getContentResolver();

        switch (requestCode){
            case REQUEST_CODE_GALLERY:
                try {
                    //当选择完相片，就会回到这里，然后相片的相关信息会保存在data中，后面想办法取出来
                    //通过getData方法取得图像uri
                    mImageUri=data.getData();

                    // 图片解析为字节数组
                    byte[] content = readStream(resolver.openInputStream(Uri.parse(mImageUri.toString())));
                    // 字节数组转换为ImageView可调用的Bitmap对象
                    backgroundBitmap = getPicFromBytes(content,null);
                    // bitmap裁剪 替补系统裁剪功能
                    backgroundBitmap = ImageUtils.cropImage(backgroundBitmap,mScreenSize.getWidth(),mScreenSize.getHeight(), true);
                    mImageView.setImageBitmap(backgroundBitmap);

//                    mImageView.setImageURI(mImageUri);
                    // 图形裁剪
//                    pictureCropping(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case CROP_PHOTO:
                // 裁剪图像返回
                Bundle bundle = data.getExtras();
                if(bundle!=null){
                    Bitmap image_crop = bundle.getParcelable("data");
                    mImageView.setImageBitmap(image_crop);
                }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"--onDestroy:");
//        backgroundBitmap.recycle();
        mBundle.clear();
    }

    public Size getScreenSize(){
        DisplayMetrics dm = getActivity().getApplicationContext().getResources().getDisplayMetrics();
        return new Size(dm.widthPixels,dm.heightPixels);
    }

}
