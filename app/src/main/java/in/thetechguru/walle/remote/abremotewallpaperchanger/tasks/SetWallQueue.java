package in.thetechguru.walle.remote.abremotewallpaperchanger.tasks;

import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
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

import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.FirebaseUtil;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryItem;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryRepo;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.HttpsRequestPayload;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.User;

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

public class SetWallQueue extends Job {

    private static final int PRIORITY = 1;
    private String id;
    private String fromUser;

    private int RETRY_LIMIT = 5;

    enum JOB_STATUS{
        RUNNING, SUCCESS, FAILURE
    }

    private JOB_STATUS status = JOB_STATUS.RUNNING;

    public SetWallQueue(String id, String fromUser) {
        super(new Params(PRIORITY).requireNetwork().persist());
        this.id = id;
        this.fromUser = fromUser;
    }

    @Override
    protected int getRetryLimit() {
        return RETRY_LIMIT;
    }

    @Override
    public void onAdded() {
        Log.d("SetWallQueue", "onAdded: Job added to queue for changing wallpaper");
    }

    @Override
    public void onRun() throws Throwable {
        Log.d("SetWallQueue", "onRun: starting again");

        status = JOB_STATUS.RUNNING;

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
            Log.d("SetWallQueue", "Error : ");
            status = JOB_STATUS.FAILURE;
            e.printStackTrace();
            FirebaseCrash.report(e);
            return;
        }

        final File finalLocalFile = localFile;
        uploadedFile.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
            // Local temp file has been created
            Log.d("SetWallpaper", "onSuccess: downloaded file in local : " + taskSnapshot.toString());
            setWallpaper(finalLocalFile);
        }).addOnFailureListener(exception -> {
            // Handle any errors
            FirebaseCrash.report(exception);
            Log.d("SetWallpaper", "onFailure: Error downloading photo from firebase storage : " + exception.getMessage());
            status = JOB_STATUS.FAILURE;
        }).addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            System.out.println("Download is " + progress + "% done");
        }).addOnCompleteListener(task -> {
            //uploadedFile.delete();
            status = JOB_STATUS.FAILURE;
        });

        //while running, keep on checking status after every 1 second (because firebase don't give synchronous download option. Why would they do that.
        //if status fail, throw exception
        //is this ugly? Please tell me some alternative for this
        while (status==JOB_STATUS.RUNNING) {
            Thread.sleep(1000);

            Log.d("SetWallQueue", "polling status: " + status);

            if(status==JOB_STATUS.FAILURE) {
                throw new RuntimeException("Fail");
            }

            if(status==JOB_STATUS.SUCCESS) {
                break;
            }
        }

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
            if(user==null){
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

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {
        Log.d("SetWallQueue", "onCancel: ");
        //notify firebase function for sending fcm to userName
        HttpsRequestPayload payload = new HttpsRequestPayload(fromUser
                , MyApp.getUser().username
                , HttpsRequestPayload.STATUS_CODE.WALLPAPER_CHANGE_FAILED
                , id);
        new SendHttpsRequest(payload).start();
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        Log.d("SetWallQueue", "shouldReRunOnThrowable: " + runCount);
        return RetryConstraint.createExponentialBackoff(runCount, 1000);
    }
}
