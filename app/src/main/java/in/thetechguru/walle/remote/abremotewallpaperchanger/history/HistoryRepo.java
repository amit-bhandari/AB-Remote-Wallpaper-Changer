package in.thetechguru.walle.remote.abremotewallpaperchanger.history;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.migration.Migration;
import android.support.annotation.NonNull;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;

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

public class HistoryRepo {

    private HistoryDAO historyDAO;
    private Executor executor;
    private static HistoryRepo historyRepo;

    public static HistoryRepo getInstance(){
        if(historyRepo==null){
            historyRepo = new HistoryRepo();
        }
        return historyRepo;
    }

    private HistoryRepo(){
        executor = Executors.newSingleThreadExecutor();
        //get the room db object, and thus DAO
        HistoryDB db = Room.databaseBuilder(MyApp.getContext(),
                HistoryDB.class, "database-name")
                .addMigrations(MIGRATION_1_2)
                .build();
        historyDAO=db.historyDAO();
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    };

    public List<HistoryItem> getHistory(){
        return historyDAO.getHistoryItems();
    }

    public void putHistoryItem(final HistoryItem historyItem){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                historyDAO.putHistoryItem(historyItem);
            }
        });
    }

    public void updateHistoryItem(final String historyId, final int status){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                historyDAO.updateStatus(historyId, status);
            }
        });
    }

    public void nukeHistory(){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                historyDAO.nukeTable();
            }
        });
    }
}
