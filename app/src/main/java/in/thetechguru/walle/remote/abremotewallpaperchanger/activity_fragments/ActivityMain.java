package in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.FragmentManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.FirebaseUtil;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.Constants;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.HttpsRequestPayload;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.ViewPagerAdapter;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.User;
import in.thetechguru.walle.remote.abremotewallpaperchanger.tasks.SendHttpsRequest;

public class ActivityMain extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.drawer_layout) DrawerLayout drawer;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.nav_view) NavigationView navigationView;
    @BindView(R.id.viewpager) ViewPager viewPager;
    @BindView(R.id.tabs) TabLayout tabLayout;
    @BindView(R.id.fab)FloatingActionButton fab;
    @BindView(R.id.progress_bar) ProgressBar progressBar;
    @BindView(R.id.color_change_layout_view) View colorChangeView;
    @BindView(R.id.root_view_app_bar_main) View rootViewAppbarMain;

    private ViewPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setNotificationChannelForOreoPlus();

        problem();
        if(FirebaseUtil.getCurrentUser()==null){
            startActivity(new Intent(this, ActivityLoginSignup.class));
            finish();
            return;
        }

        FirebaseUtil.getUsersReference().child(FirebaseUtil.getCurrentUser().getUid()).
                addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null) {
                            MyApp.getPref().edit().putString("username",user.username).apply();
                        }
                        MyApp.setUser(user);
                        tabLayout.setVisibility(View.VISIBLE);
                        viewPager.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.INVISIBLE);
                        setupViewPager(viewPager);
                        viewPager.setOffscreenPageLimit(4);
                        tabLayout.setupWithViewPager(viewPager);
                        setUpDrawerHeader();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(ActivityMain.this, "Unknown Error, exiting application", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        Glide.with(this)
                .load("https://i2.wp.com/theprehabguys.com/wp-content/uploads/2016/12/black-background.jpg?ssl=1")
                //.centerCrop()
                //.crossFade(500)
                .into((ImageView)findViewById(R.id.full_background));


        colorChange();
    }

    void problem(){

        int[] array = new int[]{1,5,2,6,2};
        for(int i=1; i<array.length; i++){
            System.out.println(array[i]-array[i-1]);
        }

    }

    private void colorChange() {
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Log.d("ActivityMain", "onPageSelected: " + position);

                if(!isOpen) {
                    int x = tabLayout.getRight() / 2;
                    int y = tabLayout.getBottom() / 2;
                    int endRadius = (int) Math.hypot(rootViewAppbarMain.getWidth(), rootViewAppbarMain.getHeight());

                    Animator animator;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        animator = ViewAnimationUtils.createCircularReveal(colorChangeView, x, y, 0, endRadius);
                        //animator.setDuration(1000);
                        colorChangeView.setVisibility(View.VISIBLE);
                        animator.start();
                    }
                    isOpen = true;
                }else {
                    int x = tabLayout.getRight() / 2;
                    int y = tabLayout.getBottom() / 2;

                    int startRadius = Math.max(rootViewAppbarMain.getWidth(), rootViewAppbarMain.getHeight());
                    int endRadius = 0;

                    Animator anim = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        anim = ViewAnimationUtils.createCircularReveal(colorChangeView, x, y, startRadius, endRadius);
                        anim.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animator) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animator) {
                                colorChangeView.setVisibility(View.GONE);
                            }

                            @Override
                            public void onAnimationCancel(Animator animator) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animator) {

                            }
                        });
                        anim.start();
                    }
                    isOpen = false;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    boolean isOpen;

    private void setUpDrawerHeader(){
        TextView userName = navigationView.getHeaderView(0).findViewById(R.id.username);
        TextView emailId = navigationView.getHeaderView(0).findViewById(R.id.email_id);
        ImageView imageView = navigationView.getHeaderView(0).findViewById(R.id.imageView);
        Glide.with(this)
                .load(MyApp.getUser().pic_url)
                .placeholder(R.drawable.person_blue)
                .centerCrop()
                .into(imageView);
        userName.setText(MyApp.getUser().display_name);
        emailId.setText(FirebaseUtil.getCurrentUser().getEmail());
    }

    private void setNotificationChannelForOreoPlus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    /* Create or update. */
                NotificationChannel channel = new NotificationChannel(Constants.CHANNEL_ID,
                        "AB Remote Wallpaper",
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setSound(null, null);
                NotificationManager manager =  ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            }
        }catch (Exception ignored){}
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){
            case R.id.action_settings:
                //startActivity(new Intent(this, ActivityMain.class));
                //finish();
                Toast.makeText(this, "Nothing here yet", Toast.LENGTH_SHORT).show();
                break;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(getIntent().getExtras()!=null){
            String tab = getIntent().getExtras().getString("tab");
            if(tab!=null && viewPager!=null) {
                switch (tab) {
                    case "friends":
                        viewPager.setCurrentItem(0);
                        break;

                    case "requests":
                        viewPager.setCurrentItem(1);
                        break;
                }
                Log.d("ActivityMain", "onResume: switch to tab " + tab);
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id){
            case R.id.nav_signout:
                FirebaseUtil.getAuth().signOut();
                //remove token from server
                //FirebaseUtil.getNotificationTokenRef().child(MyApp.getUser().username).removeValue();
                startActivity(new Intent(this, ActivityLoginSignup.class));
                finish();
                break;

            case R.id.nav_history:
                startActivity(new Intent(this, ActivityHistory.class));
                break;

            case R.id.nav_rate:
                rateUs();
                break;

            case R.id.nav_settings:
                Toast.makeText(this, "Nothing here yet", Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_share:
                shareApp();
                break;

            case R.id.nav_write_me:
                feedbackEmail();
                break;
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void feedbackEmail() {
        String myDeviceModel = Build.MODEL;
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto",getString(R.string.au_email_id), null));
        String[] address = new String[]{getString(R.string.au_email_id)};
        emailIntent.putExtra(Intent.EXTRA_EMAIL, address);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for " + myDeviceModel);
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello AndroidDevs, \n\n");
        startActivity(Intent.createChooser(emailIntent, "Send Feedback"));
    }

    private void shareApp() {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            String sAux = getString(R.string.main_act_share_app_text);
            sAux = sAux + getString(R.string.share_app) + " \n\n";
            i.putExtra(Intent.EXTRA_TEXT, sAux);
            startActivity(Intent.createChooser(i, getString(R.string.main_act_share_app_choose)));
        } catch(Exception e) {
            //e.toString();
        }
    }

    private void rateUs(){
        new MaterialDialog.Builder(this)
                .title(R.string.main_act_rate_us_title)
                .content(getString(R.string.main_act_rate_us))
                .positiveText(getString(R.string.rate_now))
                .negativeText(getString(R.string.cancel))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                        } catch (ActivityNotFoundException anfe) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                        }
                    }
                })
                .show();
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new FragmentFriends(), "Friends");
        adapter.addFragment(new FragmentRequests(), "Requests");
        adapter.addFragment(new FragmentBlockList(), "Blocked");
        adapter.addFragment(new FragmentProfile(), "Profile");
        viewPager.setAdapter(adapter);
    }

    @OnClick(R.id.fab)
    void fabClicked(){
        //if (viewPager.getAdapter() != null
           //     && adapter.get(viewPager.getCurrentItem()) instanceof FragmentFriends) {
            //add friend dialog
            addFriendDialog();
        //}
    }

    private void addFriendDialog(){
        MaterialDialog.Builder builder = new MaterialDialog.Builder(this)
                .title(R.string.add_friend_title)
                .autoDismiss(false)
                .customView(R.layout.add_friend_dialog_custom_view, false)
                .positiveText(R.string.add_friend_pos)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull final MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                        View view = dialog.getCustomView();
                        if(view==null) return;
                        final EditText inputId = view.findViewById(R.id.add_friend_user_id);
                        final ProgressBar progressBar = view.findViewById(R.id.add_friend_progress);
                        progressBar.setVisibility(View.VISIBLE);

                        //@TODO validate user name
                        final String userName = inputId.getText().toString().trim();

                        if(userName.isEmpty()){
                            inputId.setError(getString(R.string.empty_username_error));
                            dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                            progressBar.setVisibility(View.INVISIBLE);
                            return;
                        }

                        if(userName.equals(MyApp.getUser().username)){
                            inputId.setError(getString(R.string.error_self_username));
                            dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                            progressBar.setVisibility(View.INVISIBLE);
                            return;
                        }

                        //check if already friend
                        if(((FragmentFriends)adapter.get(viewPager.getCurrentItem())).isFriend(userName)){
                            inputId.setError(getString(R.string.friend_already_added_error));
                            dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                            progressBar.setVisibility(View.INVISIBLE);
                            return;
                        }

                        FirebaseUtil.getUsernamesReference().child(userName).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()){
                                    Log.d("ActivityMain", "onDataChange: valid selfUser id : " + dataSnapshot.getKey());

                                    //Create request token
                                    FirebaseUtil.getTokenReference().child(MyApp.getUser().username).child("pending")
                                            .child(userName).setValue(true);
                                    FirebaseUtil.getTokenReference().child(userName).child("requests")
                                            .child(MyApp.getUser().username).setValue(true);

                                    //notify firebase function for sending fcm to userName
                                    HttpsRequestPayload payload = new HttpsRequestPayload(userName
                                            , MyApp.getUser().username
                                            ,HttpsRequestPayload.STATUS_CODE.FRIEND_REQUEST
                                            ,null);
                                    new SendHttpsRequest(payload).start();

                                    dialog.dismiss();
                                    Toast.makeText(ActivityMain.this, R.string.friend_request_sent_toast, Toast.LENGTH_SHORT).show();
                                }else {
                                    invalidUserId();
                                }
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                invalidUserId();
                            }

                            private void invalidUserId() {
                                Log.d("ActivityMain", "onCancelled: Invalid user id");
                                inputId.setError("Invalid user Id");
                                dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                                progressBar.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                });


        MaterialDialog dialog = builder.build();

        dialog.show();
    }
}
