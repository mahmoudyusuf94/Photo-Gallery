package com.example.blink22.photogallery;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import junit.framework.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by blink22 on 14/06/18.
 */

public class PhotoGalleryFragment extends VisibleFragment {

    private static final int IMAGE_WIDTH = 400;
    private int cols ;
    private int mCurrentPage = 1;
    private boolean loading = true;
    private static final String TAG = "PhotoGalleryFragment";
    private RecyclerView mPhotoRecyclerView;
    private GridLayoutManager mLayoutManager;
    private PhotoAdapter mAdapter;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

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
                            updateItems();
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
        updateItems();

//        PollService.setServiceAlarm(getActivity(), true);
//        TestService.setServiceAlarm(getActivity(), true);

        setHasOptionsMenu(true);
        Handler responseHandler = new Handler();

        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);

        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                photoHolder.bindPhoto(drawable);
            }
        });

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();

        Log.i(TAG, "Background Thread started");
    }

    private class FetchItemTask extends AsyncTask<Void, Void, List<GalleryItem> >{

        private String mQuery;

        public FetchItemTask(String query){
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            if(mQuery == null){
                Log.d("search", ": no search");

                return new FlickrFetchr().fetchRecentPhotos(mCurrentPage);
            }else{
                Log.d("search", ":    *****"+ mQuery);
                return new FlickrFetchr().searchPhotos(mQuery, mCurrentPage);
            }
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

    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private ImageView mItemImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView){
            super(itemView);
            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
            itemView.setOnClickListener(this);
        }

        public void bindPhoto(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);
        }

        public void bindGalleryItem(GalleryItem galleryItem){
            mGalleryItem = galleryItem;
            Picasso.get().load(galleryItem.getUrl())
                    .placeholder(R.drawable.pl)
                    .into(mItemImageView);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onClick(View view) {
//            Intent i = new Intent(Intent.ACTION_VIEW, mGalleryItem.getPhotoPageUri());
            Intent i = PhotoPageActivity
                    .newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
            startActivity(i, ActivityOptions.makeSceneTransitionAnimation(getActivity()).toBundle());
        }
    }

    private  class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems =  galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            holder.bindGalleryItem(galleryItem);
//            Drawable placeholder = getResources().getDrawable(R.drawable.pl);
//            holder.bindPhoto(placeholder);
//            String imageUrl = galleryItem.getUrl();
//            mThumbnailDownloader.queueThumbnail(holder, imageUrl);

        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread stopped");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView)  searchItem.getActionView();


        MenuItem alarmItem = menu.findItem(R.id.menu_item_polling);
//        if(PollService.isServiceAlarmOn(getActivity())){
//            alarmItem.setTitle(R.string.stop_polling);
//        }else{
//            alarmItem.setTitle(R.string.start_polling);
//        }
         if(PollServiceJobScheduler.isServiceOn(getActivity())){
            alarmItem.setTitle(R.string.stop_polling);
        }else{
            alarmItem.setTitle(R.string.start_polling);
        } //challenge JobService


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d("search", ":    "+query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                clear();
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });
    }

    private  void updateItems(){
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemTask(query).execute();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                clear();
                updateItems();
                return true;
            case R.id.menu_item_polling:
//                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
//                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                boolean shouldStartAlarm = !PollServiceJobScheduler.isServiceOn(getActivity());
                PollServiceJobScheduler.schedulePolling(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void clear(){
//        mItems = new ArrayList<>();
//        mAdapter = new PhotoAdapter(mItems);
//        mPhotoRecyclerView.setAdapter(mAdapter);
        mItems.clear();
        mAdapter.notifyDataSetChanged();
    }
}
