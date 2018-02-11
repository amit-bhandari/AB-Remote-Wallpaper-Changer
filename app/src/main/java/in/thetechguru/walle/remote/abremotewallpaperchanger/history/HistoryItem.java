package in.thetechguru.walle.remote.abremotewallpaperchanger.history;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

/**
 * Created by abami on 04-Feb-18.
 */

@Entity
public class HistoryItem {

    public interface STATUS{
        int SUCCESS = 0;
        int WAITING = 1;
        int FAILURE = 2;
    }

    HistoryItem(){}

    @Ignore
    public HistoryItem(String historyId, String changedBy, String changedOf, long timestamp, String path){
        this.historyId = historyId;
        this.changedBy = changedBy;
        this.changedOf = changedOf;
        this.timestamp = timestamp;
        this.path = path;
    }

    public int status = STATUS.WAITING;

    @PrimaryKey
    @NonNull
    public String historyId = "";

    //username of person who changed wallpaper
    public String changedBy;

    //username whose wallpaper got changed
    public String changedOf;

    //time of changing
    public long timestamp;

    //path of image for thumbnail purpose
    public String path;
}
