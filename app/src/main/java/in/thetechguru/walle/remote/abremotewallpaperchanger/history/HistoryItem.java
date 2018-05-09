package in.thetechguru.walle.remote.abremotewallpaperchanger.history;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

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
