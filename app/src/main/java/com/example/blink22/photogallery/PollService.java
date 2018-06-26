package com.example.blink22.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

/**
 * Created by blink22 on 24/06/18.
 */

public class PollService extends IntentService {

    public static final String NOTIFICATION_CHANNEL_ID = "PollServiceChannel";
    private static final String TAG = "PollService";
//    private static final long POLL_INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    private static final long POLL_INTERVAL = 15*1000;
    public static final String PERM_PRIVATE = "android.permission.RECEIVE_BOOT_COMPLETED";
    public static final String ACTION_SHOW_NOTIFICATION =
            "com.example.blink22.photogallery.SHOW_NOTIFICATION";
    public static final String NOTIFICATION = "NOTIFICATION";
    public static final String REQUEST_CODE= "REQUEST_CODE";

    public PollService(){
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(!isNetworkAvailableAndConnected()){
            return;
        }
        List<GalleryItem> items;
        String lastResultId = QueryPreferences.getLastResultId(this);
        String query = QueryPreferences.getStoredQuery(this);
        if(query == null){
            items = new FlickrFetchr().fetchRecentPhotos(1);
        }else{
            items = new FlickrFetchr().searchPhotos(query, 1);
        }

        String resultId = items.get(0).getId();

        if(resultId.equals(lastResultId)){
            Log.i(TAG, "Got an old result" + resultId);
        }else{
            Log.i(TAG, "Got a new result" + resultId);

            Resources resources = getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID )
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentIntent(pi)
                    .setChannelId(NOTIFICATION_CHANNEL_ID)
                    .setAutoCancel(true);

            Notification notification = notificationBuilder.build();
            showBackgroundNotification(0, notification);
        }

        QueryPreferences.setLastResultId(this, resultId);
    }

    private void showBackgroundNotification(int requestCode, Notification notification){
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE, requestCode);
        i.putExtra(NOTIFICATION, notification);
        sendOrderedBroadcast(i,PERM_PRIVATE, null, null,
                Activity.RESULT_OK, null, null);
        Log.i(TAG, "Sent SHOW_NOTIFICATION Broadcast...");
    }

    public static Intent newIntent(Context context){
        return new Intent(context, PollService.class);
    }

    private boolean isNetworkAvailableAndConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable &&
                cm.getActiveNetworkInfo().isConnected();
        return isNetworkConnected;
    }

    public static void setServiceAlarm(Context context, boolean isOn){
        Intent i = newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if(isOn){
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL, pi);
        }else{
            alarmManager.cancel(pi);
            pi.cancel();
        }
        QueryPreferences.setAlarmOn(context, isOn)  ;
    }

    public static boolean isServiceAlarmOn(Context context){
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }


}
