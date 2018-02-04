package in.thetechguru.walle.remote.abremotewallpaperchanger.history;

import android.arch.persistence.room.Room;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;

/**
 * Created by abami on 04-Feb-18.
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
                HistoryDB.class, "database-name").build();
        historyDAO=db.historyDAO();
    }


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

}
