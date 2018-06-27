package com.example.blink22.photogallery;

import android.net.Uri;

/**
 * Created by blink22 on 14/06/18.
 */

public class GalleryItem {
    private String mCaption;
    private String mId;
    private String mUrl;
    private String mOwner;

    public String getCaption() {
        return mCaption;
    }

    public String getOwner(){
        return mOwner;
    }

    public void setOwner(String owner){
        mOwner = owner;
    }

    public Uri getPhotoPageUri(){
        return Uri.parse("http://www.flickr.com/photos/")
                .buildUpon()
                .appendPath(mOwner)
                .appendPath(mId)
                .build();
    }

    public void setCaption(String mCaption) {
        this.mCaption = mCaption;
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String mUrl) {
        this.mUrl = mUrl;
    }

    @Override
    public String toString() {
        return mCaption;
    }
}
