package in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.FirebaseUtil;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.UtilityFun;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryItem;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryRepo;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.HttpsRequestPayload;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.User;
import in.thetechguru.walle.remote.abremotewallpaperchanger.tasks.SendHttpsRequest;

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

/**
 * this activity launches when user tries to set up photo as wallpaper directly from gallery
 */
public class ActivitySetAsWallpaper extends AppCompatActivity {

    int clickedPosition;
    FriendListAdapter adapter;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.recycler_view) RecyclerView recyclerView;
    @BindView(R.id.progress_bar) ProgressBar progressBar;
    @BindView(R.id.status_text) TextView statusText;
    @BindView(R.id.swipe_to_refresh)
    SwipeRefreshLayout swipeRefreshLayout;

    Uri receivedUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent().getData()==null){
            Log.d("ActivitySetAsWallpaper", "onCreate: No extras");
            finish();
        }

        Log.d("ActivitySetAsWallpaper", "onCreate: URI" + getIntent().getData());
        receivedUri = getIntent().getData();

        activitySetup();

        if(MyApp.getUser()==null){
            final String user_json = MyApp.getPref().getString(getString(R.string.pref_user_obj),"");
            User user = new Gson().fromJson(user_json, User.class);
            MyApp.setUser(user);
        }

        if(MyApp.getUser().block_status){
            Toast.makeText(this, "You are in block mode in AB Wallpaper. Please turn it off by opening AB Wallpaper and clicking on switch on right top side.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        adapter = new FriendListAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void activitySetup() {
        setContentView(R.layout.activity_set_as_wallapaper);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        setTitle(getString(R.string.choose_friend));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("FragmentFriends", "onActivityResult: ");
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                //upload photo
                Uri mFileUri = result.getUri();
                adapter.uploadPhoto(mFileUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                try {
                    throw result.getError();
                } catch (Exception e) {
                    Log.d("FragmentFriends", "onActivityResult: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.MyViewHolder> {

        private List<User> users = new ArrayList<>();
        FriendListAdapter.GetFriendList getFriendListThread;
        private Activity activity;

        FriendListAdapter(Activity activity) {
            this.activity = activity;
            //handler = new Handler(Looper.getMainLooper());
            getFriendListThread = new FriendListAdapter.GetFriendList();
            refreshList();
        }

        private void refreshList() {
            users.clear();
            new FriendListAdapter.GetFriendList().start();
        }

        @NonNull
        @Override
        public FriendListAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_added_friend, parent, false);
            return new FriendListAdapter.MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FriendListAdapter.MyViewHolder holder, int position) {
            //String mainText = users.get(position).display_name + " ("  + users.get(position).username +")";
            holder.textView.setText(users.get(position).display_name);
            if(users.get(position).block_status){
                holder.blockModeActive.setVisibility(View.VISIBLE);
            }else {
                holder.blockModeActive.setVisibility(View.INVISIBLE);
            }
            //@todo profile photo
            Glide.with(MyApp.getContext())
                    .load(users.get(position).pic_url)
                    .override(200,200)
                    .placeholder(R.drawable.person_blue)
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return users.size();
        }


        //@todo use it to compress picture for free users
        //have to pay for setting original picture

        private Uri getCompressedFileUri(Uri originalUri){

            String compressedFilePath = activity.getExternalCacheDir() +"/"+ UUID.randomUUID().toString();
            Uri compressedUri = Uri.fromFile(new File(compressedFilePath));

            try {
                InputStream imageStream = null;

                imageStream = getContentResolver().openInputStream(
                        originalUri);

                Bitmap bmp = BitmapFactory.decodeStream(imageStream);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);

                OutputStream outputStream = new FileOutputStream(compressedFilePath);

                stream.writeTo(outputStream);

                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }catch (Exception e){
                Log.d("FriendListAdapter", "getCompressedFileUri: " + e.getLocalizedMessage());
                return  null;
            }

            Log.d("FriendListAdapter", "getCompressedFileUri: "
                    + new File(activity.getExternalCacheDir() +"/"+ UUID.randomUUID().toString()).length());

            return compressedUri;

        }

        private void uploadPhoto(final Uri mFileUri){

            final String randomId = UUID.randomUUID().toString();
            StorageReference uploadedFile = FirebaseUtil.getStorage().child(randomId);
            Log.d("FragmentFriends", "uploadPhoto: "+ uploadedFile.getPath());
            final UploadTask uploadTask = uploadedFile.putFile(mFileUri);

            MaterialDialog mDialog = null;
            if(activity!=null) {
                MaterialDialog.Builder builder = new MaterialDialog.Builder(activity)
                        .title(R.string.photo_upload_title)
                        .content(R.string.photo_upload_content)
                        .autoDismiss(false)
                        .cancelable(false)
                        .negativeText(R.string.cancel)
                        .onNegative((dialog, which) -> {
                            uploadTask.cancel();
                            dialog.dismiss();
                        })
                        .progress(true, 0);

                mDialog = builder.build();
            }

            final MaterialDialog dialog = mDialog;
            uploadTask
                    .addOnProgressListener(taskSnapshot -> {
                        int progress = (int) ((100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                        if(dialog!=null)
                            dialog.setContent(getString(R.string.photo_upload_content) + " " + progress + " %");
                        Log.d("FragmentFriends", "onProgress: " + progress);
                    }).addOnFailureListener(exception -> {
                        if(dialog!=null && dialog.isShowing()) dialog.dismiss();
                        Toast.makeText(MyApp.getContext(), "File upload failure", Toast.LENGTH_SHORT).show();
                    }).addOnSuccessListener(taskSnapshot -> {
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        if (downloadUrl != null) {
                            Log.d("FragmentFriends", "onSuccess: " + downloadUrl.toString());
                        }
                        Toast.makeText(MyApp.getContext(), "Uploaded successfully", Toast.LENGTH_SHORT).show();

                        //add history item in
                        HistoryItem item = new HistoryItem(randomId, "self", users.get(clickedPosition).username,System.currentTimeMillis(), mFileUri.toString());
                        HistoryRepo.getInstance().putHistoryItem(item);

                        //notify firebase function for sending fcm to userName
                        HttpsRequestPayload payload = new HttpsRequestPayload(users.get(clickedPosition).username
                                , MyApp.getUser().username
                                , HttpsRequestPayload.STATUS_CODE.CHANGE_WALLPAPER
                                , randomId);
                        new SendHttpsRequest(payload).start();

                        //update overall count of wallpaper changes(being tried via app), just for show off.
                        FirebaseUtil.getOverallChangeCountRef().addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                try {
                                    if (dataSnapshot.getValue() == null) {
                                        FirebaseUtil.getOverallChangeCountRef().setValue(1L);
                                    } else {
                                        FirebaseUtil.getOverallChangeCountRef().setValue((Long) dataSnapshot.getValue() + 1L);
                                    }
                                } catch (Exception ignored) {
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                        if(dialog!=null && dialog.isShowing()) dialog.dismiss();
                        finish();
                    });

            if(dialog!=null)
                dialog.show();
        }

        class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

            @BindView(R.id.friend_item_display_name)
            TextView textView;
            @BindView(R.id.friend_item_display_picture)
            ImageView imageView;
            @BindView(R.id.menu_popup) ImageView popup;
            @BindView(R.id.blockModeActive) TextView blockModeActive;

            MyViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this,itemView);
                itemView.setOnClickListener(this);
                popup.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onClick(View view) {
                //FriendListAdapter.this.onClick(view, getLayoutPosition());
                clickedPosition = getLayoutPosition();
                Uri myUri = Uri.fromFile(new File(activity.getExternalCacheDir(), UUID.randomUUID().toString()));
                CropImage.activity(receivedUri).setOutputUri(myUri).start(ActivitySetAsWallpaper.this);
            }
        }

        class GetFriendList extends Thread {

            @Override
            public void run() {
                getRunnable().run();
            }

            private Runnable getRunnable(){
                return () -> {

                    if(!UtilityFun.isConnectedToInternet()){
                        new Handler(Looper.getMainLooper()).post(() -> {
                            progressBar.setVisibility(View.INVISIBLE);
                            statusText.setVisibility(View.VISIBLE);
                            statusText.setText(R.string.error_no_network_swipe_down);
                            swipeRefreshLayout.setRefreshing(false);
                        });
                        return;
                    }

                    FirebaseUtil.getConfirmedRef().addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.getChildrenCount()<=0){
                                progressBar.setVisibility(View.INVISIBLE);
                                statusText.setVisibility(View.VISIBLE);
                                swipeRefreshLayout.setRefreshing(false);
                                statusText.setText(R.string.error_zero_friends);
                            }
                            for(DataSnapshot snap : dataSnapshot.getChildren()){
                                final String user_name  = snap.getKey();
                                FirebaseUtil.getUsernamesReference().child(user_name).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        String uid = dataSnapshot.getValue(String.class);
                                        if(uid==null){
                                            return;
                                        }
                                        FirebaseUtil.getUsersReference().child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                User user = dataSnapshot.getValue(User.class);
                                                users.add(user);
                                                statusText.setVisibility(View.INVISIBLE);
                                                recyclerView.setVisibility(View.VISIBLE);
                                                progressBar.setVisibility(View.INVISIBLE);
                                                swipeRefreshLayout.setRefreshing(false);
                                                notifyDataSetChanged();
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                progressBar.setVisibility(View.INVISIBLE);
                                                statusText.setVisibility(View.VISIBLE);
                                                swipeRefreshLayout.setRefreshing(false);
                                                statusText.setText(R.string.friend_list_fetch_unknown_error);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        progressBar.setVisibility(View.INVISIBLE);
                                        statusText.setVisibility(View.VISIBLE);
                                        swipeRefreshLayout.setRefreshing(false);
                                        statusText.setText(R.string.friend_list_fetch_unknown_error);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.d("GetBlockedList", "onCancelled: Error getting friend list");
                            progressBar.setVisibility(View.INVISIBLE);
                            statusText.setVisibility(View.VISIBLE);
                            swipeRefreshLayout.setRefreshing(false);
                            statusText.setText(R.string.friend_list_fetch_unknown_error);
                        }
                    });

                };
            }
        }
    }

}
