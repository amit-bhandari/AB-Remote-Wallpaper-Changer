package in.thetechguru.walle.remote.abremotewallpaperchanger.fcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.birbit.android.jobqueue.log.CustomLogger;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.Random;

import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments.ActivityMain;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryItem;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryRepo;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.Constants;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.HttpsRequestPayload;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.User;
import in.thetechguru.walle.remote.abremotewallpaperchanger.tasks.SetWallQueue;
import in.thetechguru.walle.remote.abremotewallpaperchanger.tasks.SetWallpaper;

/**
 Copyright 2017 Amit Bhandari AB
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    //priority queue job manager
    private JobManager mJobManager;

    private void createJobManager(){
        Configuration.Builder builder = new Configuration.Builder(this)
                .minConsumerCount(1) // always keep at least one consumer alive
                .maxConsumerCount(3) // up to 3 consumers at a time
                .loadFactor(3) // 3 jobs per consumer
                .consumerKeepAlive(120) // wait 2 minute
                .customLogger(new CustomLogger() {
                    private static final String TAG = "JOBS";
                    @Override
                    public boolean isDebugEnabled() {
                        return true;
                    }

                    @Override
                    public void d(String text, Object... args) {
                        Log.d(TAG, String.format(text, args));
                    }

                    @Override
                    public void e(Throwable t, String text, Object... args) {
                        Log.e(TAG, String.format(text, args), t);
                    }

                    @Override
                    public void e(String text, Object... args) {
                        Log.e(TAG, String.format(text, args));
                    }

                    @Override
                    public void v(String text, Object... args) {

                    }
                });

        mJobManager = new JobManager(builder.build());
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Log.d("MyFirebaseMessaging", "onMessageReceived: received payload: "+remoteMessage.getData());
        if (remoteMessage.getData().size() > 0) {
            Log.d("MyFirebaseMessaging", "Message data payload: " + remoteMessage.getData());
            Log.d("MyFirebaseMessaging", "onMessageReceived: status :" + remoteMessage.getData().get("notif_status"));

            String status = remoteMessage.getData().get("notif_status");
            String fromUser = remoteMessage.getData().get("fromUser");
            String wallpaper_url = remoteMessage.getData().get("id");    //i know, 'id' doesn't make sense, but I am too lazy to change it now. Maybe someday I will

            Log.d("MyFirebaseMessaging", "onMessageReceived: " + fromUser + " : " +status + " : " + wallpaper_url);

            switch (status){
                case HttpsRequestPayload.STATUS_CODE.FRIEND_REQUEST:
                    postFriendRequest(fromUser);
                    break;

                case HttpsRequestPayload.STATUS_CODE.FRIEND_ADDED:
                    postRequestAcceptedNotification(fromUser);
                    break;

                case HttpsRequestPayload.STATUS_CODE.CHANGE_WALLPAPER:
                    changeWallpaperRequest(fromUser, wallpaper_url);
                    break;

                case HttpsRequestPayload.STATUS_CODE.WALLPAPER_CHANGED:
                    postWallpaperChanged(wallpaper_url,fromUser);
                    break;

                case HttpsRequestPayload.STATUS_CODE.WALLPAPER_CHANGE_FAILED:
                    postWallpaperChangeFailure(wallpaper_url, fromUser);
                    break;
            }
        }
    }

    private void changeWallpaperRequest(String fromUser, String wallpaper_url) {
        User user = new Gson().fromJson(MyApp.getPref().getString(getString(R.string.pref_user_obj),""),User.class);
        if(user!=null && user.block_status) {
            FirebaseCrash.log("Wallpaper request came even in blocked mode for user : " + user.username);
            return;
        }
        //new SetWallpaper(wallpaper_url, fromUser).start();
        if(mJobManager==null) {
            createJobManager();
        }
        mJobManager.addJobInBackground(new SetWallQueue(wallpaper_url, fromUser));
    }

    //@todo better notifications

    private void postFriendRequest(String fromUser){

        PendingIntent pendingIntent;
        Intent notificationIntent = new Intent(this, ActivityMain.class);
        notificationIntent.putExtra("tab", "requests");
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, new Random().nextInt(),
                notificationIntent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_person_add_black_24dp)
                        .setAutoCancel(true)
                        .setContentTitle("Friend request from " + fromUser)
                        .setContentText(fromUser + " wants to connect with you! Once connected, you can change each others wallpaper remotely.")
                        .setContentIntent(pendingIntent);

        int mNotificationId = new Random().nextInt();
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (mNotifyMgr != null) {
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
    }

    private void postRequestAcceptedNotification(String fromUser){

        PendingIntent pendingIntent;
        Intent notificationIntent = new Intent(this, ActivityMain.class);
        notificationIntent.putExtra("tab", "friends");
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, new Random().nextInt(),
                notificationIntent, 0);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_person_black_24dp)
                        .setContentTitle(fromUser + " accepted your friend request")
                        .setAutoCancel(true)
                        .setContentText(fromUser + " is your friend now. You can change wallpaper for him/her.")
                        .setContentIntent(pendingIntent);

        int mNotificationId = new Random().nextInt();
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (mNotifyMgr != null) {
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
    }

    private void postWallpaperChanged(String wallpaperUrl, String fromUser){
        PendingIntent pendingIntent;
        Intent notificationIntent = new Intent(this, ActivityMain.class);
        notificationIntent.putExtra("tab", "friends");
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, new Random().nextInt(),
                notificationIntent, 0);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_wallpaper_black_24dp)
                        .setContentTitle("Wallpaper changed")
                        .setAutoCancel(true)
                        .setContentText("Successfully changed wallpaper for " + fromUser )
                        .setContentIntent(pendingIntent);

        //update history item for showing success
        HistoryRepo.getInstance().updateHistoryItem(wallpaperUrl, HistoryItem.STATUS.SUCCESS);

        int mNotificationId = new Random().nextInt();
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (mNotifyMgr != null) {
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
    }

    private void postWallpaperChangeFailure(String wallpaperUrl, String fromUser){
        PendingIntent pendingIntent;
        Intent notificationIntent = new Intent(this, ActivityMain.class);
        notificationIntent.putExtra("tab", "friends");
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, new Random().nextInt(),
                notificationIntent, 0);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_error_outline_black_24dp)
                        .setContentTitle("Wallpaper change failure")
                        .setAutoCancel(true)
                        .setContentText("Failed to change wallpaper for " + fromUser )
                        .setContentIntent(pendingIntent);

        //update history item for showing failure
        HistoryRepo.getInstance().updateHistoryItem(wallpaperUrl, HistoryItem.STATUS.FAILURE);

        int mNotificationId = new Random().nextInt();
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (mNotifyMgr != null) {
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
    }
}
