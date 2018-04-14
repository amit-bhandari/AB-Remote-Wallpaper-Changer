package in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.FirebaseUtil;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.UtilityFun;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.Constants;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.HttpsRequestPayload;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.User;
import in.thetechguru.walle.remote.abremotewallpaperchanger.tasks.SendHttpsRequest;

/**
 * Created by abami on 1/17/2018.
 */

public class FragmentRequests extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    @BindView(R.id.recycler_view)    RecyclerView recyclerView;
    @BindView(R.id.progress_bar) ProgressBar progressBar;
    @BindView(R.id.status_text) TextView statusText;
    @BindView(R.id.swipe_to_refresh) SwipeRefreshLayout swipeRefreshLayout;
    private int clickedPosition;
    private FriendsRequestsAdapter adapter;

    BroadcastReceiver refreshReceiver;

    public FragmentRequests(){
        refreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(adapter!=null) adapter.refreshList();
                Log.d("FragmentRequests", "onReceive: ");
            }
        };
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
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(refreshReceiver, new IntentFilter(Constants.ACTIONS.REFRESH_REQUESTS));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_requests, container, false);
        ButterKnife.bind(this, layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);
        adapter = new FriendsRequestsAdapter(getActivity());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        return layout;
    }

    @Override
    public void onRefresh() {
        if(adapter!=null) {
            swipeRefreshLayout.setRefreshing(true);
            adapter.refreshList();
        }
    }


    class FriendsRequestsAdapter extends RecyclerView.Adapter<FriendsRequestsAdapter.MyViewHolder>
            implements PopupMenu.OnMenuItemClickListener{

        private List<User> users = new ArrayList<>();
        private Handler handler;
        private Activity activity;

        FriendsRequestsAdapter(Activity activity) {
            this.activity = activity;
            handler = new Handler(Looper.getMainLooper());

            refreshList();
        }

        void refreshList() {
            users.clear();
            progressBar.setVisibility(View.VISIBLE);
            statusText.setVisibility(View.INVISIBLE);
            new GetRequestList().start();
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend_request, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            holder.textView.setText( users.get(position).display_name);

            //@todo profile photo
            Glide.with(MyApp.getContext())
                    .load(users.get(position).pic_url)
                    .placeholder(R.drawable.person_blue)
                    .override(200,200)
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_accept:
                    addFriend();
                    break;

                case R.id.action_block:
                    new MaterialDialog.Builder(activity)
                            .title(getString(R.string.block_warn, users.get(clickedPosition).display_name) )
                            .positiveText(R.string.block)
                            .negativeText(getString(R.string.cancel))
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    block();
                                }
                            })
                            .show();
                    break;

                case R.id.action_reject:
                    new MaterialDialog.Builder(activity)
                            .title(getString(R.string.reject_friend_warn, users.get(clickedPosition).display_name) )
                            .positiveText(R.string.reject)
                            .negativeText(getString(R.string.cancel))
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    rejectFriend();
                                }
                            })
                            .show();
                    break;
            }
            return false;
        }

        private void addFriend() {
            String userName = users.get(clickedPosition).username;
            //add user name to confirmed path for both the users
            FirebaseUtil.getConfirmedRef()
                    .child(userName).setValue(true);
            FirebaseUtil.getConfirmedRef(userName)
                    .child(MyApp.getUser().username).setValue(true).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(getContext()!=null)
                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(Constants.ACTIONS.REFRESH_FRIEND_LIST));
                }
            });

            //remove from pending and requests sections
            FirebaseUtil.getRequestsRef()
                    .child(userName).removeValue();
            FirebaseUtil.getPendingRef(userName)
                    .child(MyApp.getUser().username).removeValue();

            //notify firebase function for sending fcm to userName
            HttpsRequestPayload payload = new HttpsRequestPayload(userName
                    , MyApp.getUser().username
                    ,HttpsRequestPayload.STATUS_CODE.FRIEND_ADDED
                    ,null);
            new SendHttpsRequest(payload).start();


            Toast.makeText(activity, getString(R.string.friend_added_toast, users.get(clickedPosition).display_name), Toast.LENGTH_SHORT).show();
            users.remove(clickedPosition);
            notifyItemRemoved(clickedPosition);
            //refreshList();
        }

        private void rejectFriend() {
            String userName = users.get(clickedPosition).username;
            //remove from pending and requests sections
            FirebaseUtil.getRequestsRef()
                    .child(userName).removeValue();
            FirebaseUtil.getPendingRef(userName)
                    .child(MyApp.getUser().username).removeValue();

            users.remove(clickedPosition);
            notifyItemRemoved(clickedPosition);
            //refreshList();
        }

        private void block() {
            String userName = users.get(clickedPosition).username;
            //remove from pending and requests sections
            FirebaseUtil.getRequestsRef()
                    .child(userName).removeValue();
            FirebaseUtil.getPendingRef(userName)
                    .child(MyApp.getUser().username).removeValue();

            FirebaseUtil.getBlockedRef()
                    .child(userName).setValue(true).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (getContext() != null) {
                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(Constants.ACTIONS.REFRESH_BLOCK_LIST));
                    }
                }
            });

            Toast.makeText(MyApp.getContext(), getString(R.string.blocked_toast, users.get(clickedPosition).display_name), Toast.LENGTH_SHORT).show();
            users.remove(clickedPosition);
            notifyItemRemoved(clickedPosition);
            //refreshList();
        }

        private void onClick(View view , int position){
            clickedPosition = position;
            switch (view.getId()){
                case R.id.card_view:
                case R.id.menu_popup:
                    PopupMenu popup=new PopupMenu(getContext(),view, Gravity.RIGHT);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.menu_request_item, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(this);
                    break;
            }}

        class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            @BindView(R.id.friend_request_display_name) TextView textView;
            @BindView(R.id.friend_request_display_picture) ImageView imageView;
            @BindView(R.id.menu_popup) ImageView popup;

            MyViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this,itemView);
                popup.setOnClickListener(this);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                FriendsRequestsAdapter.this.onClick(view, getLayoutPosition());
            }
        }

        class GetRequestList extends Thread {
            @Override
            public void run() {
                getRunnable().run();
            }

            private Runnable getRunnable(){
                return new Runnable() {
                    @Override
                    public void run(){

                        if(!UtilityFun.isConnectedToInternet()) {
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
                        FirebaseUtil.getRequestsRef().addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(dataSnapshot.getChildrenCount()<=0){
                                    progressBar.setVisibility(View.INVISIBLE);
                                    statusText.setVisibility(View.VISIBLE);
                                    swipeRefreshLayout.setRefreshing(false);
                                    statusText.setText(R.string.error_no_requests);
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
                                                    swipeRefreshLayout.setRefreshing(false);
                                                    statusText.setVisibility(View.VISIBLE);
                                                    statusText.setText(R.string.requests_fetch_unknown_error);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            progressBar.setVisibility(View.INVISIBLE);
                                            swipeRefreshLayout.setRefreshing(false);
                                            statusText.setVisibility(View.VISIBLE);
                                            swipeRefreshLayout.setRefreshing(false);
                                            statusText.setText(R.string.requests_fetch_unknown_error);
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.d("GetRequestList", "onCancelled: Error reading requests");
                                progressBar.setVisibility(View.INVISIBLE);
                                statusText.setVisibility(View.VISIBLE);
                                swipeRefreshLayout.setRefreshing(false);
                                statusText.setText(R.string.requests_fetch_unknown_error);
                            }
                        });

                    }
                };
            }
        }

    }

}
