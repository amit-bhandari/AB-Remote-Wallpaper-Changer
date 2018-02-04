package in.thetechguru.walle.remote.abremotewallpaperchanger.fcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments.ActivityMain;
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
                    postWallpaperChanged(fromUser);
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
                        .setSmallIcon(R.drawable.person_blue)
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
                        .setSmallIcon(R.drawable.person_blue)
                        .setContentTitle("Request Accepted")
                        .setContentText(fromUser + " is your friend now.")
                        .setContentIntent(pendingIntent);

        int mNotificationId = 2;
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (mNotifyMgr != null) {
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
    }

    private void postWallpaperChanged(String fromUser){

        PendingIntent pendingIntent;
        Intent notificationIntent = new Intent(this, ActivityMain.class);
        notificationIntent.putExtra("tab", "friends");
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, new Random().nextInt(),
                notificationIntent, 0);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                        .setSmallIcon(R.drawable.person_blue)
                        .setContentTitle("Wallpaper changed")
                        .setContentText("Successfully changed wallpaper for " + fromUser )
                        .setContentIntent(pendingIntent);

        int mNotificationId = 3;
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (mNotifyMgr != null) {
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
    }

}
