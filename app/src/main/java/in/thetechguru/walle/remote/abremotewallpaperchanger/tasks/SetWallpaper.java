package in.thetechguru.walle.remote.abremotewallpaperchanger.tasks;

import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;

import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.FirebaseUtil;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryItem;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryRepo;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.HttpsRequestPayload;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.User;

/**
 * Created by AB on 2017-10-26.
 * task to set wallpaper given path of image
 * download it from storage and then set it as wallpaper
 * and then delete it from sever
 */

public class SetWallpaper extends Thread {

    public SetWallpaper(String id, String fromUser){
        super(getRunnable(id, fromUser));
    }

    private static Runnable getRunnable(final String id, final String fromUser){
        return new Runnable() {
            @Override
            public void run() {

                final StorageReference uploadedFile = FirebaseUtil.getStorage().child(id);
                File localFile;
                try {

                    String directory_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AB_Wall/";
                    String file_name = fromUser + "_" + id + ".jpg";
                    //create directory if not present
                    File file = new File(directory_path);
                    if(!file.exists() && !file.mkdir()){
                        localFile = File.createTempFile("photo", "jpg");
                    }else {
                        localFile = new File(directory_path + file_name);
                    }
                    Log.d("SetWallpaper", "save path: " + localFile.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                    FirebaseCrash.report(e);
                    return;
                }


                final File finalLocalFile = localFile;
                uploadedFile.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        // Local temp file has been created
                        Log.d("SetWallpaper", "onSuccess: downloaded file in local : " + taskSnapshot.toString());
                        setWallpaper(finalLocalFile);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                        FirebaseCrash.report(exception);
                        Log.d("SetWallpaper", "onFailure: Error downloading photo from firebase storage : " + exception.getMessage());
                    }
                }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        System.out.println("Download is " + progress + "% done");
                    }
                }).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                        //delete the file
                        if(task.isSuccessful()) {
                            uploadedFile.delete();
                        }
                    }
                });
            }

            private void setWallpaper(File localFile){
                Bitmap photo = getBitmapFromFile(localFile);
                WallpaperManager myWallpaperManager
                        = WallpaperManager.getInstance(MyApp.getContext());

                if(myWallpaperManager!=null){
                    try {
                        myWallpaperManager.setBitmap(photo);
                    } catch (Exception e){
                        e.printStackTrace();
                        FirebaseCrash.report(e);
                        return;
                    }

                    //changed successfully
                    HistoryItem item = new HistoryItem(id, fromUser, "self",System.currentTimeMillis(), Uri.fromFile(localFile).toString());
                    HistoryRepo.getInstance().putHistoryItem(item);

                    User user = MyApp.getUser();
                    if(user!=null){
                        user = new Gson().fromJson(MyApp.getPref().getString(MyApp.getContext().getString(R.string.pref_user_obj),""), User.class);
                    }

                    if(user==null) return;

                    //notify firebase function for sending fcm to userName
                    HttpsRequestPayload payload = new HttpsRequestPayload(fromUser
                            , user.username
                            , HttpsRequestPayload.STATUS_CODE.WALLPAPER_CHANGED
                            , id);
                    new SendHttpsRequest(payload).start();

                    Log.d("SetWallpaper", "setWallpaper: successfully changed wallpaper");
                }
            }

            private Bitmap getBitmapFromFile(File photo){
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeFile(photo.getAbsolutePath(), options);

                if(bitmap==null){
                    Log.d("SetWallpaper", "getBitmapFromFile: failed to create bitmap from file : " +  photo.getAbsolutePath());
                }

                return bitmap;
            }
        };
    }
}

