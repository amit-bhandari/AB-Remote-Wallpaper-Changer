package in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.FirebaseUtil;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryItem;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryRepo;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.HttpsRequestPayload;
import in.thetechguru.walle.remote.abremotewallpaperchanger.tasks.SendHttpsRequest;

import static android.app.Activity.RESULT_OK;

/**
 * Created by abami on 1/17/2018.
 */

public class FragmentProfile extends Fragment {

    @BindView(R.id.username)
    TextView username;

    @BindView(R.id.email_id)
    TextView emailid;

    @BindView(R.id.display_name)
    TextView display_name;

    @BindView(R.id.image_view_profile)
    ImageView profile_photo;

    public FragmentProfile(){}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("FragmentProfile", "onActivityResult: ");
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri mFileUri = result.getUri();
                uploadPhoto(mFileUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                try {
                    throw result.getError();
                } catch (Exception e) {
                    Log.d("FragmentProfile", "onActivityResult: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private void uploadPhoto(Uri mFileUri){
        final String randomId = FirebaseUtil.getCurrentUser().getUid();
        StorageReference uploadedFile = FirebaseUtil.getProfilePhotoRef().child(randomId);
        final UploadTask uploadTask = uploadedFile.putFile(mFileUri);

        Toast.makeText(MyApp.getContext(), "Updating profile photo...", Toast.LENGTH_SHORT).show();

        uploadTask
                .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(MyApp.getContext(), "Profile photo update failed.", Toast.LENGTH_SHORT).show();
                }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        final Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        if (downloadUrl != null) {
                            Log.d("FragmentProfile", "onSuccess: " + downloadUrl.toString());
                            FirebaseUtil.getPhotoUrlRef(FirebaseUtil.getCurrentUser().getUid())
                                    .setValue(downloadUrl.toString())
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.getException()==null) {
                                                Toast.makeText(MyApp.getContext(), "Profile photo changed successfully", Toast.LENGTH_SHORT).show();
                                                if(getActivity()!=null) {
                                                    Glide.with(getActivity())
                                                            .load(downloadUrl)
                                                            .placeholder(R.drawable.person_blue)
                                                            .into(profile_photo);
                                                }
                                            }else {
                                                Toast.makeText(MyApp.getContext(), "Error changing profile photo", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_profile, container, false);
        ButterKnife.bind(this, layout);

        profile_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getActivity()==null) return;
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1,1)
                        .setOutputCompressQuality(5)
                        .start(getActivity());
            }
        });

        display_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(getActivity()==null) return;

                new MaterialDialog.Builder(getActivity())
                        .title("Change Display Name")
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .input("", MyApp.getUser().display_name, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, final CharSequence input) {

                                if(input.toString().equals("") || input.toString().length()<6){
                                    Toast.makeText(MyApp.getContext(), R.string.error_invalid_display_name, Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Do something
                                FirebaseUtil.getDisplayNameRef(FirebaseUtil.getCurrentUser().getUid())
                                        .setValue(input.toString())
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.getException()==null) {
                                                    display_name.setText(input.toString());
                                                    Toast.makeText(getContext(), "Display Name changed successfully", Toast.LENGTH_SHORT).show();
                                                }else {
                                                    Toast.makeText(getContext(), "Error changing display name", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        }).show();
            }
        });

        if(FirebaseUtil.getCurrentUser()!=null && MyApp.getUser().username!=null){
            display_name.setText(MyApp.getUser().display_name);
            username.setText( MyApp.getUser().username);
            emailid.setText(FirebaseUtil.getCurrentUser().getEmail());
            Glide.with(this)
                    .load(MyApp.getUser().pic_url)
                    .placeholder(R.drawable.person_blue)
                    .override(200,200)
                    .into(profile_photo);
        }

        return layout;
    }
}
