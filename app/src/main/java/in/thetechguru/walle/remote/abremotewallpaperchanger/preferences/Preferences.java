package in.thetechguru.walle.remote.abremotewallpaperchanger.preferences;

import android.content.Context;
import android.preference.PreferenceManager;

public class Preferences {

    public static boolean isAdsRemoved(Context context){
        return get(context, "is_ad_removed",false);
    }

    public static void setAdsRemoved(Context context, boolean value){
        set(context, "is_ad_removed",value);
    }

    private static String get(Context context, String key, String defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(key, defaultValue);
    }

    private static boolean get(Context context, String key, boolean defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(key, defaultValue);
    }

    private static void set(Context context, String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(key, value)
                .apply();
    }

    private static void set(Context context, String key, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(key, value)
                .apply();
    }

}
