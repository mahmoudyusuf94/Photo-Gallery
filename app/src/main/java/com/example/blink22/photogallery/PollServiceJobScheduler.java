package com.example.blink22.photogallery;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

/**
 * Created by blink22 on 24/06/18.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PollServiceJobScheduler extends JobService {

    private static final String TAG = "JobScheduler";
    private PollTask mCurrentTask;
    private final static int JOB_ID = 1;
    public static final String PERM_PRIVATE = "android.permission.RECEIVE_BOOT_COMPLETED";
    public static final String ACTION_SHOW_NOTIFICATION =
            "com.example.blink22.photogallery.SHOW_NOTIFICATION";
    public static final String NOTIFICATION = "NOTIFICATION";
    public static final String REQUEST_CODE= "REQUEST_CODE";
    public static final String NOTIFICATION_CHANNEL_ID = "PollServiceChannel";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "On start job......");
        mCurrentTask = new PollTask();
        mCurrentTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if(mCurrentTask != null){
            mCurrentTask.cancel(true);
        }
        Log.i(TAG, "On stop job......");
        return false;
    }

    private class PollTask extends AsyncTask<JobParameters, Void,List<GalleryItem>>{

        @Override
        protected List<GalleryItem> doInBackground(JobParameters ... jobParameters) {
            Log.i(TAG, "OOOOOOOOOOO  Black Hole OOOOOOOOOOOOO");
            List<GalleryItem> items;
            String query = QueryPreferences.getStoredQuery(getApplicationContext());
            if(query == null){
                items = new FlickrFetchr().fetchRecentPhotos(1);
            }else{
                items = new FlickrFetchr().searchPhotos(query, 1);
            }
            jobFinished(jobParameters[0], false);
            return items;
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            if(items.size()==0) return;
            String resultId = items.get(0).getId();
            String lastResultId = QueryPreferences.getLastResultId(PollServiceJobScheduler.this);

            if(resultId.equals(lastResultId)){
                Log.i(TAG, "Got an old result" + resultId);
            }else{
                Log.i(TAG, "Got a new result" + resultId);

                Resources resources = getResources();
                Intent i = PhotoGalleryActivity.newIntent(PollServiceJobScheduler.this);
                PendingIntent pi = PendingIntent.getActivity(PollServiceJobScheduler.this, 0, i, 0);
                NotificationCompat.Builder notificationBuilder =
                        new NotificationCompat.Builder(PollServiceJobScheduler.this, NOTIFICATION_CHANNEL_ID )
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
            QueryPreferences.setLastResultId(PollServiceJobScheduler.this, resultId);
        }
    }

    public static boolean isServiceOn(Context context){
        Log.i(TAG, "Called isServiceOn");
        JobScheduler scheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        for (JobInfo jobInfo: scheduler.getAllPendingJobs()){
            if(jobInfo.getId() == JOB_ID){
                Log.i(TAG, "scheduled -> return true (Service is on)");
                return true;
            }
        }
        Log.i(TAG, "not scheduled -> return false (Service is off)");
        return false;
    }

    public static void schedulePolling(Context context, boolean isOn){
        JobScheduler scheduler = (JobScheduler)context.getSystemService(JOB_SCHEDULER_SERVICE);
        if(isOn){
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID,
                    new ComponentName(context, PollServiceJobScheduler.class))
                    .setPeriodic(10*1000)
                    .setPersisted(true)
                    .build();
            Log.i(TAG, "Scheduling job ....");
            Log.i(TAG, jobInfo.toString());
            scheduler.schedule(jobInfo);
        }else{
            Log.i(TAG, "Cancelling Job");
            scheduler.cancel(JOB_ID);
        }
    }

    private void showBackgroundNotification(int requestCode, Notification notification){
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE, requestCode);
        i.putExtra(NOTIFICATION, notification);
        sendOrderedBroadcast(i,PERM_PRIVATE, null, null,
                Activity.RESULT_OK, null, null);
        Log.i(TAG, "Sent SHOW_NOTIFICATION Broadcast...");
    }
}
