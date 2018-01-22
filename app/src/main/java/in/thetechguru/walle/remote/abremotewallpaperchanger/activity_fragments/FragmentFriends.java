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

import com.bumptech.glide.Glide;
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
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.Constants;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.User;

/**
 * Created by abami on 1/17/2018.
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
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(refreshReceiver, new IntentFilter(Constants.ACTIONS.REFRESH_BLOCK_LIST));
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


    class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.MyViewHolder>
        implements PopupMenu.OnMenuItemClickListener{

        private List<User> users = new ArrayList<>();
        private Handler handler;
        private Activity activity;
        GetFriendList getFriendListThread;

        FriendListAdapter(Activity activity) {
            this.activity = activity;
            handler = new Handler(Looper.getMainLooper());
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

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_added_friend, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            //String mainText = users.get(position).display_name + " ("  + users.get(position).username +")";
            holder.textView.setText(users.get(position).display_name);

            //@todo profile photo
            Glide.with(MyApp.getContext()).load(users.get(position).pic_url).placeholder(R.drawable.person_blue).into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        List<User> getList (){return users;}

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_change_wallpaper:
                    if(getActivity() instanceof ActivityMain){
                        Intent i = new Intent(getActivity(), ActivityPhotoUpload.class);
                        i.putExtra("userName", users.get(clickedPosition).username);
                        activity.startActivity(i);
                    }
                    break;

                case R.id.action_block_user:
                    //add token to blocked users list
                    FirebaseUtil.getBlockedRef().child(users.get(clickedPosition).username).setValue(true);

                    //remove from confirmed in both user directories i.e
                    //from self --> blocked and user_getting_blocked --> blocked
                    FirebaseUtil.getConfirmedRef(users.get(clickedPosition).username)
                            .child(MyApp.getUser().username).removeValue();
                    FirebaseUtil.getConfirmedRef().child(users.get(clickedPosition).username).removeValue();

                    break;

                case R.id.action_remove_friend:
                    String userName = users.get(clickedPosition).username;
                    //remove from pending and requests sections
                    FirebaseUtil.getConfirmedRef()
                            .child(userName).removeValue();
                    FirebaseUtil.getConfirmedRef(userName)
                            .child(MyApp.getUser().username).removeValue();

                    refreshList();
                    break;
            }
            return true;
        }

        void onClick(View view, int position) {
            clickedPosition = position;
            switch (view.getId()){
                        case R.id.menu_popup:
                            PopupMenu popup=new PopupMenu(getContext(),view, Gravity.RIGHT);
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

            MyViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this,itemView);
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
