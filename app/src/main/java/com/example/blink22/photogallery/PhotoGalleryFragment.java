package com.example.blink22.photogallery;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by blink22 on 14/06/18.
 */

public class PhotoGalleryFragment extends Fragment {

    private static final int IMAGE_WIDTH = 400;
    private int cols ;
    private int mCurrentPage = 1;
    private boolean loading = true;
    private static final String TAG = "PhotoGalleryFragment";
    private RecyclerView mPhotoRecyclerView;
    private GridLayoutManager mLayoutManager;
    private PhotoAdapter mAdapter;

    private int pastVisibleItems, visibleItemCount, totalItemCount;

    private List<GalleryItem> mItems  = new ArrayList<>();

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = v.findViewById(R.id.fragment_photo_gallery_recycler_view);

        //challenge 1
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0){
                    visibleItemCount = mLayoutManager.getChildCount();
                    totalItemCount = mLayoutManager.getItemCount();
                    pastVisibleItems = mLayoutManager.findFirstVisibleItemPosition();

                    if(loading){
                        if((visibleItemCount + pastVisibleItems) >= totalItemCount){
                            loading = false;
                            mCurrentPage ++;
                            new FetchItemTask().execute();
                            Log.i(TAG, "Page: "+ Integer.toString(mCurrentPage));
                            Log.i(TAG, "visible Item Count: "+ Integer.toString(visibleItemCount));
                            Log.i(TAG, "total Item Count: "+ Integer.toString(totalItemCount));
                            Log.i(TAG, "past Item: "+ Integer.toString(pastVisibleItems));

                        }
                    }
                }
            }
        });
        //end challenge 1

        //challenge 2
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        cols = displayMetrics.widthPixels/IMAGE_WIDTH;
        //end challenge 2

        mLayoutManager = new GridLayoutManager(getActivity(), cols);
//      mLayoutManager = new GridLayoutManager(getActivity(), 3);
        mPhotoRecyclerView.setLayoutManager(mLayoutManager);

        setupAdapter();
        return v;
    }

    private void setupAdapter(){
        if(isAdded()){
            mAdapter = new PhotoAdapter(mItems);
            mPhotoRecyclerView.setAdapter(mAdapter);
        }
    }

    private void updateUi(){
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemTask().execute();
    }

    private class FetchItemTask extends AsyncTask<Void, Void, List<GalleryItem> >{

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            return new FlickrFetchr().fetchItems(mCurrentPage);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
//            mItems = galleryItems;
//            setupAdapter();

            mItems.addAll(galleryItems);
            updateUi();
            loading = true;
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder{
        private TextView mTitleTextView;

        public PhotoHolder(View itemView){
            super(itemView);
            mTitleTextView = (TextView) itemView;
        }

        public void bindPhoto(GalleryItem item){
            mTitleTextView.setText(item.getCaption());
        }
    }

    private  class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems =  galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(getActivity());
            return new PhotoHolder(textView);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            holder.bindPhoto(galleryItem);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }


}
