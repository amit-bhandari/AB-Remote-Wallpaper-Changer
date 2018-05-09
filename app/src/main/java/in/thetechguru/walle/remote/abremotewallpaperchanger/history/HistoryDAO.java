package in.thetechguru.walle.remote.abremotewallpaperchanger.history;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

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

@Dao
public interface HistoryDAO {

    @Query("SELECT * FROM historyitem ORDER BY timestamp DESC")
    List<HistoryItem> getHistoryItems();

    @Insert(onConflict = REPLACE)
    void putHistoryItem(HistoryItem historyItem);

    @Query("UPDATE historyitem SET status = :status WHERE historyId = :historyId")
    void updateStatus(String historyId, int status);

    @Query("DELETE FROM historyitem")
    void nukeTable();
}
