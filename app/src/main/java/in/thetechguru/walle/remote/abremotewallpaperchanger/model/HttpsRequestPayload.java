package in.thetechguru.walle.remote.abremotewallpaperchanger.model;

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
