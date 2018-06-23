package com.example.blink22.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by blink22 on 23/06/18.
 */

public class ThumbnailDownloader <T> extends HandlerThread{
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    private LruCache<String , Bitmap> mImageCache; //chapter  - challenge 1

    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;
    private ConcurrentMap<T,String> mRequestMap = new ConcurrentHashMap<>();

    public interface  ThumbnailDownloadListener<T>{
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }
    public void setThumbnailDownloadListener(ThumbnailDownloadListener listener){
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader (Handler responseHandler){
        super(TAG);
        mResponseHandler = responseHandler;
        final int maxMemory = (int) Runtime.getRuntime().maxMemory()/1024;

        mImageCache = new LruCache<String, Bitmap>(maxMemory/6);
    }

    public Bitmap getBitmapFromCache(String url){
        return mImageCache.get(url);
    }

    public void addBitmapToCache(String url, Bitmap bitmap){
        if(mImageCache.get(url) == null){
            mImageCache.put(url, bitmap);
        }
    }

    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    public ThumbnailDownloader() {
        super(TAG);
    }

    public void queueThumbnail(T target, String url){
        if(url == null){
            mRequestMap.remove(target);
        }
        else{
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
        Log.i(TAG, "Got a Url "+ url);
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler(){
            public void handleMessage(Message msg){
                if(msg.what == MESSAGE_DOWNLOAD){
                    T target = (T) msg.obj;
                    handleRequest(target);
                }
            }
        };
    }

    public void handleRequest(final T target){

        final String url = mRequestMap.get(target);
        if(url == null){
            return;
        }
        try {
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory
                    .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mRequestMap.get(target) != url){
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error downloading image", e);
        }
    }


}
