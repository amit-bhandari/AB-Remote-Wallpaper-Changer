package in.thetechguru.walle.remote.abremotewallpaperchanger.fcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments.ActivityMain;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryItem;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryRepo;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.Constants;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.HttpsRequestPayload;
import in.thetechguru.walle.remote.abremotewallpaperchanger.tasks.SetWallpaper;

/**
 * Created by AB on 2017-10-17.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Log.d("MyFirebaseMessaging", "onMessageReceived: received payload: "+remoteMessage.getData());
        if (remoteMessage.getData().size() > 0) {
            Log.d("MyFirebaseMessaging", "Message data payload: " + remoteMessage.getData());
            Log.d("MyFirebaseMessaging", "onMessageReceived: status :" + remoteMessage.getData().get("notif_status"));

            String status = remoteMessage.getData().get("notif_status");
            String fromUser = remoteMessage.getData().get("fromUser");
            String wallpaper_url = remoteMessage.getData().get("id");

            Log.d("MyFirebaseMessaging", "onMessageReceived: " + fromUser + " : " +status + " : " + wallpaper_url);

            switch (status){
                case HttpsRequestPayload.STATUS_CODE.FRIEND_REQUEST:
                    postFriendRequest(fromUser);
                    break;

                case HttpsRequestPayload.STATUS_CODE.FRIEND_ADDED:
                    postRequestAcceptedNotification(fromUser);
                    break;

                case HttpsRequestPayload.STATUS_CODE.CHANGE_WALLPAPER:
                    new SetWallpaper(wallpaper_url, fromUser).start();
                    break;

                case HttpsRequestPayload.STATUS_CODE.WALLPAPER_CHANGED:
                    postWallpaperChanged(wallpaper_url,fromUser);
                    break;
            }
        }
    }

    private void postFriendRequest(String fromUser){

        PendingIntent pendingIntent;
        Intent notificationIntent = new Intent(this, ActivityMain.class);
        notificationIntent.putExtra("tab", "requests");
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, new Random().nextInt(),
                notificationIntent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setAutoCancel(true)
                        .setContentTitle("Friend request")
                        .setContentText(fromUser + " wants to connect with you!")
                        .setContentIntent(pendingIntent);

        int mNotificationId = 1;
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
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Request Accepted")
                        .setAutoCancel(true)
                        .setContentText(fromUser + " is your friend now.")
                        .setContentIntent(pendingIntent);

        int mNotificationId = 2;
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

        int mNotificationId = 3;
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (mNotifyMgr != null) {
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
    }

}
