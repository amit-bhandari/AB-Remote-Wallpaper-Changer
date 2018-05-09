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

public class User {
    public String username;
    public String pic_url;
    public String display_name;

    public boolean block_status = false;

    public User(String display_name,String username, String pic_url){
        this.display_name = display_name;
        this.username = username;
        this.pic_url = pic_url;
    }

    public User(String username, String pic_url){
        this.display_name = "";
        this.username = username;
        this.pic_url = pic_url;
    }

    public User(){}
}
