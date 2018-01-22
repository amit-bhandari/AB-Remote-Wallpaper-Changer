package in.thetechguru.walle.remote.abremotewallpaperchanger.fcm;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.FirebaseUtil;

/**
 * Created by AB on 2017-10-17.
 */

public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d("MyFirebaseInstanceId", "onTokenRefresh: Refreshed token :" + refreshedToken);
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String token){
        MyApp.getPref().edit().putString(getString(R.string.notification_token),token).apply();
        if(MyApp.getUser()!=null) {
            String userId = MyApp.getUser().username;
            if (userId == null || userId.equals("")) return;
            FirebaseUtil.getNotificationTokenRef().child(userId).setValue(token);
        }
    }
}
