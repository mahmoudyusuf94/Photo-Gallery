package com.example.blink22.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by blink22 on 26/06/18.
 */

public class StartupReceiver extends BroadcastReceiver {
    private final static String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received Intent :" + intent.getAction());
        boolean isOn =  QueryPreferences.isAlarmOn(context);
        PollService.setServiceAlarm(context, isOn);
    }
}