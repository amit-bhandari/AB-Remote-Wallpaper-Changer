package in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
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
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
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
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.Constants;
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

public class FragmentFriends extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    FriendListAdapter adapter;
    @BindView(R.id.recycler_view)    RecyclerView recyclerView;
    @BindView(R.id.progress_bar)    ProgressBar progressBar;
    @BindView(R.id.status_text) TextView statusText;
    @BindView(R.id.swipe_to_refresh) SwipeRefreshLayout swipeRefreshLayout;
    int clickedPosition;

    BroadcastReceiver refreshReceiver;

    public FragmentFriends(){
        refreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(adapter!=null) adapter.refreshList();
                Log.d("FragmentFriends", "onReceive: ");
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_friends, container, false);
        ButterKnife.bind(this, layout);

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);

        adapter = new FriendListAdapter(getActivity());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        return layout;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(getContext()!=null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(refreshReceiver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getContext()!=null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(refreshReceiver, new IntentFilter(Constants.ACTIONS.REFRESH_FRIEND_LIST));
        }
    }

    public boolean isFriend(String userName){
        boolean isFriend = false;

        if(adapter!=null && adapter.getList()!=null){
            for(User user:adapter.getList()){
                if(user.username.equals(userName)){
                    isFriend = true;
                    break;
                }
            }
        }
        return isFriend;
    }

    @Override
    public void onRefresh() {
        if(adapter!=null) {
            swipeRefreshLayout.setRefreshing(true);
            adapter.refreshList();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("FragmentFriends", "onActivityResult: ");
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
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

    class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.MyViewHolder>
        implements PopupMenu.OnMenuItemClickListener{

        private List<User> users = new ArrayList<>();
        GetFriendList getFriendListThread;
        private Activity activity;

        FriendListAdapter(Activity activity) {
            this.activity = activity;
            //handler = new Handler(Looper.getMainLooper());
            getFriendListThread = new GetFriendList();
            refreshList();
        }

        private void refreshList() {
            users.clear();
            recyclerView.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            statusText.setVisibility(View.INVISIBLE);
            new GetFriendList().start();
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_added_friend, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
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

        private List<User> getList (){return users;}

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_change_wallpaper:
                    if(activity==null) return false;

                    if(users.get(clickedPosition).block_status){
                        Toast.makeText(activity, getString(R.string.error_user_in_blocked_mode, users.get(clickedPosition).display_name), Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    String fileName = MyApp.getUser().username + "--" + UUID.randomUUID().toString();
                    Uri myUri = Uri.fromFile(new File(activity.getExternalCacheDir(), fileName));
                    CropImage.activity()
                            .setGuidelines(CropImageView.Guidelines.ON)
                            .setOutputUri(myUri)
                            .setOutputCompressQuality(70)
                            //.setAspectRatio(1,1)
                            //.setOutputCompressQuality(5)
                            .start(activity);
                    break;

                case R.id.action_block_user:
                    new MaterialDialog.Builder(activity)
                            .title(getString(R.string.block_warn, users.get(clickedPosition).display_name) )
                            .positiveText(R.string.block)
                            .negativeText(getString(R.string.cancel))
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    Toast.makeText(MyApp.getContext(), getString(R.string.blocked_toast, users.get(clickedPosition).display_name), Toast.LENGTH_SHORT).show();

                                    //add token to blocked users list
                                    FirebaseUtil.getBlockedRef().child(users.get(clickedPosition).username).setValue(true);

                                    //remove from confirmed in both user directories i.e
                                    //from self --> blocked and user_getting_blocked --> blocked
                                    FirebaseUtil.getConfirmedRef(users.get(clickedPosition).username)
                                            .child(MyApp.getUser().username).removeValue();
                                    FirebaseUtil.getConfirmedRef().child(users.get(clickedPosition).username).removeValue();

                                    users.remove(clickedPosition);
                                    notifyItemRemoved(clickedPosition);
                                }
                            })
                            .show();
                    break;

                case R.id.action_remove_friend:
                    new MaterialDialog.Builder(activity)
                            .title(getString(R.string.remove_friend_warn, users.get(clickedPosition).display_name) )
                            .positiveText(R.string.remove)
                            .negativeText(getString(R.string.cancel))
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                                    String userName = users.get(clickedPosition).username;

                                    Toast.makeText(MyApp.getContext(), getString(R.string.friend_removed_toast, users.get(clickedPosition).display_name), Toast.LENGTH_SHORT).show();

                                    //remove from pending and requests sections
                                    FirebaseUtil.getConfirmedRef()
                                            .child(userName).removeValue();
                                    FirebaseUtil.getConfirmedRef(userName)
                                            .child(MyApp.getUser().username).removeValue();

                                    users.remove(clickedPosition);
                                    notifyItemRemoved(clickedPosition);
                                }
                            })
                            .show();
                    break;
            }
            return true;
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
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                uploadTask.cancel();
                                dialog.dismiss();
                            }
                        })
                        .progress(true, 0);

                mDialog = builder.build();
            }

            final MaterialDialog dialog = mDialog;
            uploadTask
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            int progress = (int) ((100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                            if(dialog!=null)
                                dialog.setContent(getString(R.string.photo_upload_content) + " " + progress + " %");
                            Log.d("FragmentFriends", "onProgress: " + progress);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    if(dialog!=null && dialog.isShowing()) dialog.dismiss();
                    Toast.makeText(MyApp.getContext(), "File upload failure", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();
                            if (downloadUrl != null) {
                                Log.d("FragmentFriends", "onSuccess: " + downloadUrl.toString());
                            }

                            if(getActivity() instanceof  ActivityMain){
                                ((ActivityMain)getActivity()).loadInterstial();
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

                            //update overall count of wallpaper changes, just for show off.
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
                        }
                    });

            if(dialog!=null)
                dialog.show();
        }

        void onClick(View view, int position) {
            clickedPosition = position;
            switch (view.getId()){

                case R.id.card_view:
                case R.id.menu_popup:
                    PopupMenu popup=new PopupMenu(getContext(),view, Gravity.END);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.menu_friend_item, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(this);
                    break;
            }
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
                popup.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                FriendListAdapter.this.onClick(view, getLayoutPosition());
            }
        }

        class GetFriendList extends Thread {

            @Override
            public void run() {
                getRunnable().run();
            }

            private Runnable getRunnable(){
                return new Runnable() {
                    @Override
                    public void run(){

                        if(!UtilityFun.isConnectedToInternet()){
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.INVISIBLE);
                                    statusText.setVisibility(View.VISIBLE);
                                    statusText.setText(R.string.error_no_network_swipe_down);
                                    swipeRefreshLayout.setRefreshing(false);
                                }
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

                    }
                };
            }
        }
    }

}
