package com.example.blink22.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;

/**
 * Created by blink22 on 27/06/18.
 */

public class PhotoPageActivity extends SingleFragmentActivity {
    private PhotoPageFragment mFragment;

    public static Intent newIntent (Context context, Uri uri){
        Intent i = new Intent(context, PhotoPageActivity.class);
        i.setData(uri);
        return i;
    }

    @Override
    public Fragment createFragment() {
        mFragment = PhotoPageFragment.newInstance(getIntent().getData());
        return mFragment;
    }

    @Override
    public void onBackPressed() {
        if(mFragment.canGoBack()){
            mFragment.goBack();
        }
        else{
            super.onBackPressed();
        }
    }
}
