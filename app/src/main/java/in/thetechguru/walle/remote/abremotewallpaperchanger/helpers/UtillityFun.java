package in.thetechguru.walle.remote.abremotewallpaperchanger.helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;

/**
 * Created by abami on 1/18/2018.
 */

public class UtillityFun {

    public static boolean isConnectedToInternet(){
        ConnectivityManager
                cm = (ConnectivityManager) MyApp.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = null;
        if (cm != null) {
            activeNetwork = cm.getActiveNetworkInfo();
        }
        return activeNetwork != null
                && activeNetwork.isConnectedOrConnecting();
    }

}
