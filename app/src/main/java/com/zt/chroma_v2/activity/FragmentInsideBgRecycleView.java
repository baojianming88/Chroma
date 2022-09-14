package com.zt.chroma_v2.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zt.chroma_v2.R;
import com.zt.chroma_v2.utils.ImageUtils;
import com.zt.chroma_v2.utils.SimpleDividerItemDecoration;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * author : Tan Lang
 * e-mail : 2715009907@qq.com
 * date   : 2022/3/2515:24
 * desc   :
 * version: 1.0
 */
public class FragmentInsideBgRecycleView extends Fragment implements View.OnClickListener {

    private final static String TAG = "FragmentInsideBg";

    private View view;
    private RecyclerView mRecyclerView;
    private MyAdapter mMyAdapter;
    private List<String> mImagePathList = new ArrayList<>();

    // buttons
    private ImageButton mBack;
    private ImageButton mSubmit;

    private Bundle mBundle;

    public FragmentInsideBgRecycleView(){}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.preview_bg_inside_recycle_view,container,false);
        mRecyclerView = view.findViewById(R.id.inside_bg_preview);
        mBundle = new Bundle();
        mBundle.putString("choose_bg","inside");

        // 获取image 的uri数据放入mImagePathList
        try {
            mImagePathList = loadImageRes("background/");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 设置RecyclerView
        setRecyclerView();
        // 设置UI布局参数
        setUIParams();
        return view;
    }

    // image dictionary path 转 images path list
    private List<String> loadImageRes(String dirPath) throws IOException {

        String[] file_names = getResources().getAssets().list(dirPath);
        for(int i=0; i<file_names.length; i++){
            mImagePathList.add(dirPath+file_names[i]);
        }

        return mImagePathList;
    }

    private void setRecyclerView(){
        mMyAdapter = new MyAdapter();
        mRecyclerView.setAdapter(mMyAdapter);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(),1);
        gridLayoutManager.setOrientation(RecyclerView.VERTICAL);
        mRecyclerView.setLayoutManager(gridLayoutManager);

        // 设置间隔线
        SimpleDividerItemDecoration mDivider = new SimpleDividerItemDecoration(getContext(),5,5);
        mRecyclerView.addItemDecoration(mDivider);

    }

    private void setUIParams(){
        mBack = view.findViewById(R.id.back2last);
        mSubmit = view.findViewById(R.id.submit);

        mBack.setOnClickListener(this);
        mSubmit.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.back2last:
                Navigation.findNavController(v).navigate(R.id.action_chooseBg_inside_to_inside_preview,mBundle);
                break;
//            case R.id.submit:
//                // 传递图像uri
//                Navigation.findNavController(v).navigate(R.id.action_chooseBg_inside_to_inside_preview,mBundle);
//                break;
        }
    }

    class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = View.inflate(getContext(),R.layout.item_list_bg_preview,null);
            MyViewHolder myViewHolder = new MyViewHolder(view);
            return myViewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

            String imagePath = mImagePathList.get(position);

            // 展示数据加载
            InputStream assetImage = null;
            Bitmap imageBitmap = null;
            try {
                assetImage = getResources().getAssets().open(imagePath);
                imageBitmap = BitmapFactory.decodeStream(assetImage);
                imageBitmap = ImageUtils.cropImage(imageBitmap,3,4,true);
//                imageBitmap = ImageUtils.resizeImage(imageBitmap,300,400);
            } catch (IOException e) {
                e.printStackTrace();
            }

            holder.imageView.setImageBitmap(imageBitmap);
            holder.itemRoot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.setEnabled(false);
                    // 携带bundler传递数据
                    mBundle.putString("imagePath",imagePath);
                    Navigation.findNavController(v).navigate(R.id.action_chooseBg_inside_to_inside_preview,mBundle);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mImagePathList.size();
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        RelativeLayout itemRoot;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_preview);
            itemRoot= itemView.findViewById(R.id.item_view);
        }
    }
}
