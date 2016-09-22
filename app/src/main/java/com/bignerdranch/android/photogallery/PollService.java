package com.bignerdranch.android.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.nfc.Tag;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by v.borovkov on 22.09.2016.
 */
public class PollService extends IntentService {
    private static final String TAG = "PollService";
    private static final int POLL_INTERVAL = 1000 * 60;//60 secs

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent intent = newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (isOn) {
            Log.i(TAG,"turned on the alarm");
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL, pi);
        } else {
            Log.i(TAG,"turned off the alarm");
            alarmManager.cancel(pi);
            pi.cancel();
        }

    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent i = newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
        Log.i(TAG,"service on : "+(pi!=null));
        return pi != null;

    }

    public PollService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "got intent for service : " + intent);
        if (!isNetwokAvailableAndConnected()) {
            return;
        }
        List<GalleryItem> items;
        String lastResultId = QueryPreferences.getLastResultId(this);
        String query = QueryPreferences.getStoredQuery(this);
        if (query == null) {
            items = new FlickrFetchr().fetchRecentPhotos();
        } else {
            items = new FlickrFetchr().searchPhotos(query);
        }
        if (items.size() == 0) {
            return;
        }
        String resultId = items.get(0).getId();
        if (resultId.equals(lastResultId)) {
            Log.i(TAG, "Got old result!" + " lastResultId: " + lastResultId + " resultId : " + resultId);
        } else {
            Log.i(TAG, "Got new result!" + " lastResultId: " + lastResultId + " resultId : " + resultId);
            Resources resources=getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi=PendingIntent.getActivity(this,0,i,0);

            Notification notification=new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_picture_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_picture_title))
                    .setContentText(resources.getString(R.string.new_picture_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();
            NotificationManagerCompat notificationManager=NotificationManagerCompat.from(this);
            notificationManager.notify(0,notification);
        }




        QueryPreferences.setLastResultId(this, resultId);


    }







    private boolean isNetwokAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = (cm.getActiveNetworkInfo() != null);
        if (isNetworkAvailable && cm.getActiveNetworkInfo().isConnected()) {
            Log.i(TAG, "Network available and connected");
            return true;
        } else {
            Log.i(TAG, "Network either not available or not connected");
            return false;
        }
    }

}
