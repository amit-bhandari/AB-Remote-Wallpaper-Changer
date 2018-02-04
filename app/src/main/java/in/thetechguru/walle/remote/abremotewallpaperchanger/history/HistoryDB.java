package in.thetechguru.walle.remote.abremotewallpaperchanger.history;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

/**
 * Created by abami on 04-Feb-18.
 */

@Database(entities = {HistoryItem.class}, version = 1, exportSchema = false)
public abstract class HistoryDB extends RoomDatabase {

    public abstract HistoryDAO historyDAO();

}
