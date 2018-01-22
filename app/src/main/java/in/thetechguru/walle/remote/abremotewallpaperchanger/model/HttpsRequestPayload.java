package in.thetechguru.walle.remote.abremotewallpaperchanger.model;

/**
 * Created by abami on 1/18/2018.
 */

public class HttpsRequestPayload {
    public String toUserName;
    public String fromUserName;
    public String statusCode;

    //can be null
    public String wallpaperUrl;

    public HttpsRequestPayload(String toUserName, String fromUserName, String statusCode, String wallpaperUrl){
        this.toUserName = toUserName;
        this.fromUserName = fromUserName;
        this.statusCode = statusCode;
        this.wallpaperUrl = wallpaperUrl;
    }

    public interface STATUS_CODE {
        String FRIEND_REQUEST = "0";
        String FRIEND_ADDED = "1";
        String CHANGE_WALLPAPER = "2";
        String WALLPAPER_CHANGED = "3";
    }

}
