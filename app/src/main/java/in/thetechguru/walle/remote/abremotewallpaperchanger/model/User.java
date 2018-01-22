package in.thetechguru.walle.remote.abremotewallpaperchanger.model;

/**
 * Created by abami on 1/18/2018.
 * User object is for storing friends or requests data items
 * Simple POJO for binding data in recycler view class
 */

public class User {
    public String username;
    public String pic_url;
    public String display_name;

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
