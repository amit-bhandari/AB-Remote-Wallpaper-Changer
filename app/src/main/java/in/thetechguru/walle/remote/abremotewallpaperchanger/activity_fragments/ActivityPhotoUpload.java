package in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.UUID;

import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.FirebaseUtil;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.HttpsRequestPayload;
import in.thetechguru.walle.remote.abremotewallpaperchanger.tasks.SendHttpsRequest;

/**
 * Created by AB on 2017-10-24.
 * this is invisible activity, used only for displaying uploading dialog
 * File is picked up using crop image library
 * @todo manager compression of images while uploading
 */

public class ActivityPhotoUpload extends Activity {

    private static final int TC_PICK_IMAGE = 101;
    private Uri mFileUri;
    private String toUserName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent()!=null){
            toUserName = getIntent().getStringExtra("userName");
        }else {
            finish();
        }

        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mFileUri = result.getUri();
                uploadPhoto();
                Log.d("InvisiblePhotoUpload", "onActivityResult: " + mFileUri.getPath());
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                try {
                    throw result.getError();
                } catch (Exception e) {
                    Log.d("InvisiblePhotoUpload", "onActivityResult: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
                finish();
            }else {
                finish();
            }
        }else {
            finish();
        }
    }

    private void uploadPhoto(){

        final String randomId = UUID.randomUUID().toString();
        StorageReference uploadedFile = FirebaseUtil.getStorage().child(randomId);
        Log.d("InvisiblePhotoUpload", "uploadPhoto: "+ uploadedFile.getPath());
        final UploadTask uploadTask = uploadedFile.putFile(mFileUri);

        MaterialDialog.Builder builder = new MaterialDialog.Builder(this)
                .title(R.string.photo_upload_title)
                .content(R.string.photo_upload_content)
                .autoDismiss(false)
                .cancelable(false)
                .negativeText(R.string.cancel)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        uploadTask.cancel();
                        dialog.dismiss();
                    }
                })
                .progress(true, 0);

        final MaterialDialog dialog = builder.build();

        uploadTask
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        int progress = (int) ((100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                        dialog.setContent(getString(R.string.photo_upload_content) + " " + progress + " %");
                        Log.d("InvisiblePhotoUpload", "onProgress: " + progress);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                if(dialog.isShowing()) dialog.dismiss();
                Toast.makeText(ActivityPhotoUpload.this, "File upload failure", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                if (downloadUrl != null) {
                    Log.d("InvisiblePhotoUpload", "onSuccess: " + downloadUrl.toString());
                }
                Toast.makeText(ActivityPhotoUpload.this, "Uploaded successfully", Toast.LENGTH_SHORT).show();

                //notify firebase function for sending fcm to userName
                HttpsRequestPayload payload = new HttpsRequestPayload(toUserName
                        , MyApp.getUser().username
                        , HttpsRequestPayload.STATUS_CODE.CHANGE_WALLPAPER
                        , randomId);
                new SendHttpsRequest(payload).start();

                if(dialog.isShowing()) dialog.dismiss();
                finish();
            }
        });

        dialog.show();
    }
}
