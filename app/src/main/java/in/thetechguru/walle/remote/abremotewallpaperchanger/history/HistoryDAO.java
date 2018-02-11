package in.thetechguru.walle.remote.abremotewallpaperchanger.history;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

/**
 * Created by abami on 04-Feb-18.
 */

@Dao
public interface HistoryDAO {

    @Query("SELECT * FROM historyitem ORDER BY timestamp DESC")
    List<HistoryItem> getHistoryItems();

    @Insert(onConflict = REPLACE)
    void putHistoryItem(HistoryItem historyItem);

    @Query("UPDATE historyitem SET status = :status WHERE historyId = :historyId")
    void updateStatus(String historyId, int status);
}
