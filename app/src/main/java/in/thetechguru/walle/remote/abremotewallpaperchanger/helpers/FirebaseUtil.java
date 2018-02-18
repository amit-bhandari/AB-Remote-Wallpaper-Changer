package in.thetechguru.walle.remote.abremotewallpaperchanger.helpers;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;

/**
 * Created by abami on 1/18/2018.
 * Firebase data structure
 *
 * usernames ---> { username1 --> firebase_userid1 }
 *           ---> { username2 --> firebase_userid2}
 *
 * users     ---> { firebase_userid1 --> username1
 *                                   --> email1    }
 *           ---> { firebase_userid2 --> username2
 *                                   --> email2     }
 *
 * tokens    ---> username1  ---> requests   ---> { List of user names who sent requests }
 *                           ---> confirmed  ---> { List of user names who are friends   }
 *                           ---> blocked    ---> { List of user names  blocked by you   }
 *
 *           ---> username2  ---> requests   ---> { List of user names who sent requests }
 *                           ---> confirmed  ---> { List of user names who are friends   }
 *                           ---> blocked    ---> { List of user names  blocked by you   }
 */


public class FirebaseUtil {

    private static FirebaseDatabase firebaseDatabase;

    public static FirebaseDatabase getDatabase(){
        if(firebaseDatabase==null){
            firebaseDatabase = FirebaseDatabase.getInstance();
            //firebaseDatabase.setPersistenceEnabled(true);
        }
        return firebaseDatabase;
    }

    public static StorageReference getStorage(){
        return FirebaseStorage.getInstance().getReference();
    }
    
    public static FirebaseUser getCurrentUser(){
        return getAuth().getCurrentUser();
    }

    //Global Reference getters

    //get global token reference
    public static DatabaseReference getTokenReference(){
        return getDatabase().getReference().child("tokens");
    }

    //get global user names reference
    public static DatabaseReference getUsernamesReference(){
        return getDatabase().getReference().child("usernames");
    }

    //get global users reference
    public static DatabaseReference getUsersReference(){
        return getDatabase().getReference().child("users");
    }

    public static DatabaseReference getNotificationTokenRef(){
        return getDatabase().getReference().child("notificationTokens");
    }


    //Specific reference getters

    //get root -> users -> self --> reference
    public static DatabaseReference getSelfUserReference(){
        if(getCurrentUser()!=null) {
            return getDatabase().getReference().child("users").child(getCurrentUser().getUid());
        }else {
            return null;
        }
    }

    //get particular username reference from firebase user id
    public static DatabaseReference getUsernameRef(String uId){
        return getDatabase().getReference().child("users").child(uId).child("username");
    }

    //get particular username reference from firebase user id
    public static DatabaseReference getBlockStatusRef(String uId){
        return getDatabase().getReference().child("users").child(uId).child("block_status");
    }

    //get particular email reference from firebase user id
    public static DatabaseReference getEmailRef(String uId){
        return getDatabase().getReference().child("users").child(uId).child("email");
    }

    //get particular display name reference from firebase user id
    public static DatabaseReference getDisplayNameRef(String uId){
        return getDatabase().getReference().child("users").child(uId).child("display_name");
    }

    //get particular photo path reference from firebase user id
    public static DatabaseReference getPhotoUrlRef(String uId){
        return getDatabase().getReference().child("users").child(uId).child("pic_url");
    }

    //get requests pref for self
    public static DatabaseReference getRequestsRef(){
        return getTokenReference().child(MyApp.getUser().username).child("requests");
    }

    //get confirmed pref for self
    public static DatabaseReference getConfirmedRef(){
        return getTokenReference().child(MyApp.getUser().username).child("confirmed");
    }

    //get blocked pref for self
    public static DatabaseReference getBlockedRef(){
        return getTokenReference().child(MyApp.getUser().username).child("blocked");
    }

    //get pending pref for self
    public static DatabaseReference getPendingRef(){
        return getTokenReference().child(MyApp.getUser().username).child("pending");
    }

    //get requests pref for particular username
    public static DatabaseReference getRequestsRef(String user_name){
        return getTokenReference().child(user_name).child("requests");
    }

    //get confirmed pref for particular username
    public static DatabaseReference getConfirmedRef(String user_name){
        return getTokenReference().child(user_name).child("confirmed");
    }

    //get blocked pref for particular username
    public static DatabaseReference getBlockedRef(String user_name){
        return getTokenReference().child(user_name).child("blocked");
    }

    //get pending pref for particular username
    public static DatabaseReference getPendingRef(String user_name){
        return getTokenReference().child(user_name).child("pending");
    }

    public static DatabaseReference getOverallChangeCountRef(){
        return getDatabase().getReference().child("stats").child("wallpaper_change_requests");
    }

    //get auth instance
    public static FirebaseAuth getAuth(){
        return FirebaseAuth.getInstance();
    }

    //get profile photo storage refrence
    public static StorageReference getProfilePhotoRef(){
        return getStorage().child("profile_photos");
    }

}
