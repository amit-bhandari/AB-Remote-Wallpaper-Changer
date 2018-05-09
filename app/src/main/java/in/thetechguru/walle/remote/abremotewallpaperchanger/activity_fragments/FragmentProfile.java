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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.FirebaseUtil;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryItem;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryRepo;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.HttpsRequestPayload;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.User;
import in.thetechguru.walle.remote.abremotewallpaperchanger.tasks.SendHttpsRequest;

import static android.app.Activity.RESULT_OK;

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

public class FragmentProfile extends Fragment {

    @BindView(R.id.username)
    TextView username;

    @BindView(R.id.email_id)
    TextView emailid;

    @BindView(R.id.display_name)
    TextView display_name;

    @BindView(R.id.image_view_profile)
    ImageView profile_photo;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

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
        progressBar.setVisibility(View.VISIBLE);

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
                                            final String user_json = MyApp.getPref().getString(getString(R.string.pref_user_obj),"");
                                            if(task.getException()==null && !user_json.equals("")) {
                                                User user = new Gson().fromJson(user_json, User.class);
                                                user.pic_url = downloadUrl.toString();
                                                MyApp.setUser(user);
                                                MyApp.getPref().edit().putString(getString(R.string.pref_user_obj),new Gson().toJson(user)).apply();
                                                Toast.makeText(MyApp.getContext(), "Profile photo changed successfully", Toast.LENGTH_SHORT).show();
                                                progressBar.setVisibility(View.INVISIBLE);
                                                if(getActivity()!=null) {
                                                    Glide.with(getActivity())
                                                            .load(downloadUrl)
                                                            .placeholder(R.drawable.person_blue)
                                                            .into(profile_photo);
                                                }
                                            }else {
                                                progressBar.setVisibility(View.INVISIBLE);
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

    @OnClick(R.id.display_name)
    void onDisplayNameClick(){
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
                                        final String user_json = MyApp.getPref().getString(getString(R.string.pref_user_obj),"");
                                        if(task.getException()==null && !user_json.equals("") ) {
                                            User user = new Gson().fromJson(user_json, User.class);
                                            user.display_name = input.toString();
                                            display_name.setText(input.toString());
                                            MyApp.setUser(user);
                                            MyApp.getPref().edit().putString(getString(R.string.pref_user_obj),new Gson().toJson(user)).apply();
                                            Toast.makeText(getContext(), "Display Name changed successfully", Toast.LENGTH_SHORT).show();
                                        }else {
                                            Toast.makeText(getContext(), "Error changing display name", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                }).show();
    }

    @OnClick(R.id.image_view_profile)
    void onClickProfilePhoto(){
        if(getActivity()==null) return;
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1,1)
                .setOutputCompressQuality(5)
                .start(getActivity());
    }
}
