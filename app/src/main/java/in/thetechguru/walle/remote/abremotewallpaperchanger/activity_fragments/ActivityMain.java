package in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments;

import android.animation.Animator;
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
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SwitchCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.FirebaseUtil;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.UtilityFun;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryRepo;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.Constants;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.HttpsRequestPayload;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.ViewPagerAdapter;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.User;
import in.thetechguru.walle.remote.abremotewallpaperchanger.preferences.Preferences;
import in.thetechguru.walle.remote.abremotewallpaperchanger.tasks.SendHttpsRequest;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

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
    @BindView(R.id.status_text) TextView statusText;
    SwitchCompat blockSwitch;

    private InterstitialAd mInterstitialAd;

    final static String INSTA_WEBSITE = "https://www.instagram.com/_amit_bhandari/?hl=en";
    final static String TERMS_USAGE__WEBSITE = "https://thetechguru.in/terms-usage-ab-remote-wallpaper-changer/";

    @BindView(R.id.adView) AdView mAdView;

    private ViewPagerAdapter adapter;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //call profile fragment
        Fragment fragment = adapter.get(viewPager.getCurrentItem());
        fragment.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setNotificationChannelForOreoPlus();

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if(FirebaseUtil.getCurrentUser()==null){
            startActivity(new Intent(this, ActivityLoginSignup.class));
            finish();
            return;
        }

        loadInitialScreen();
        setSupportActionBar(toolbar);

        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        showAd();
    }

    private void showAd(){
        if (!Preferences.isAdsRemoved(this) && UtilityFun.isConnectedToInternet()) {
            MobileAds.initialize(getApplicationContext(), getString(R.string.banner_main));
            AdRequest adRequest = new AdRequest.Builder()//.addTestDevice("F40E78AED9B7FE233362079AC4C05B61")
                    .build();
            if (mAdView != null) {
                mAdView.loadAd(adRequest);
                mAdView.setVisibility(View.VISIBLE);
            }
        } else {
            if (mAdView != null) {
                mAdView.setVisibility(View.GONE);
            }
        }
    }

    public void loadInterstial(){
        if(!Preferences.isAdsRemoved(this) && UtilityFun.isConnectedToInternet()) {
                mInterstitialAd = new InterstitialAd(this);
                mInterstitialAd.setAdUnitId(getString(R.string.inter_wall_change));

                mInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        mInterstitialAd.show();
                    }
                });
                requestNewInterstitial();
        }
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                //.addTestDevice("F40E78AED9B7FE233362079AC4C05B61")
                .build();

        mInterstitialAd.loadAd(adRequest);
    }

    private void loadInitialScreen() {
        if(!UtilityFun.isConnectedToInternet()){
            setErrorScreen(R.string.no_network_retry);
        }else {
            statusText.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);

            final String user_json = MyApp.getPref().getString(getString(R.string.pref_user_obj),"");
            if(user_json.equals("")){
                FirebaseUtil.getUsersReference().child(FirebaseUtil.getCurrentUser().getUid()).
                        addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                User user = dataSnapshot.getValue(User.class);
                                if (user == null) {
                                    setErrorScreen(R.string.unknown_error);
                                    return;
                                }
                                MyApp.getPref().edit().putString(getString(R.string.pref_user_obj), new Gson().toJson(user)).apply();
                                MyApp.setUser(user);

                                if(user.block_status) {
                                    if(blockSwitch!=null) blockSwitch.setChecked(true);
                                    setErrorScreen(R.string.error_block_mode);
                                }else {
                                    setMainScreen();
                                }
                                setUpDrawerHeader();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Toast.makeText(ActivityMain.this, "Unknown Error, exiting application", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
            }else {
                User user = new Gson().fromJson(user_json, User.class);
                MyApp.setUser(user);
                if(user.block_status) {
                    if(blockSwitch!=null) blockSwitch.setChecked(true);
                    setErrorScreen(R.string.error_block_mode);
                }else {
                    setMainScreen();
                }
                setUpDrawerHeader();
            }
        }
    }

    private void setErrorScreen(int string_resource) {
        fab.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText(string_resource);
        tabLayout.setVisibility(View.INVISIBLE);
        viewPager.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void setMainScreen() {
        //if first install, show info message
        if(!MyApp.getPref().getBoolean(getString(R.string.pref_usage_terms), false)){
            termsOfUsage();
        }

        fab.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.INVISIBLE);
        tabLayout.setVisibility(View.VISIBLE);
        viewPager.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        setupViewPager(viewPager);
        viewPager.setOffscreenPageLimit(4);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void revealBlockView(boolean reveal){
        if(reveal) {
            int x = blockSwitch.getRight() ;
            int y = blockSwitch.getBottom() ;
            int endRadius = (int) Math.hypot(rootViewAppbarMain.getWidth(), rootViewAppbarMain.getHeight());

            Animator animator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                animator = ViewAnimationUtils.createCircularReveal(colorChangeView, x, y, 0, endRadius);
                animator.setDuration(300);
                colorChangeView.setVisibility(View.VISIBLE);
                animator.start();
            }
            isOpen = true;
        }else {
            int x = blockSwitch.getRight() ;
            int y = blockSwitch.getBottom() ;

            int startRadius = Math.max(rootViewAppbarMain.getWidth(), rootViewAppbarMain.getHeight());
            int endRadius = 0;

            Animator anim;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                anim = ViewAnimationUtils.createCircularReveal(colorChangeView, x, y, startRadius, endRadius);
                anim.setDuration(300);
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

    boolean isOpen;

    private void setUpDrawerHeader(){
        TextView userName = navigationView.getHeaderView(0).findViewById(R.id.username);
        //TextView emailId = navigationView.getHeaderView(0).findViewById(R.id.email_id);
        final TextView globalCount = navigationView.getHeaderView(0).findViewById(R.id.global_count);
        ImageView imageView = navigationView.getHeaderView(0).findViewById(R.id.imageView);
        Glide.with(this)
                .load(MyApp.getUser().pic_url)
                .placeholder(R.drawable.person_blue)
                .centerCrop()
                .override(200,200)
                .into(imageView);
        userName.setText(MyApp.getUser().display_name);
        //emailId.setText(FirebaseUtil.getCurrentUser().getEmail());

        FirebaseUtil.getOverallChangeCountRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    if (dataSnapshot.getValue() != null) {
                        globalCount.setVisibility(View.VISIBLE);
                        globalCount.setText(getString(R.string.global_count
                                , UtilityFun.format((Long)dataSnapshot.getValue())));
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
        final MenuItem block_switch = menu.findItem(R.id.action_switch_block);
        blockSwitch = block_switch.getActionView().findViewById(R.id.block_switch);
        if(blockSwitch!=null){
            blockSwitch.setEnabled(true);

            if(MyApp.getUser()!=null){
                blockSwitch.setChecked(MyApp.getUser().block_status);
            }

            blockSwitch.setOnClickListener(view -> {
                if(blockSwitch.isChecked()){
                    //block user wallpaper changes
                    new MaterialDialog.Builder(ActivityMain.this)
                            .title(R.string.dialog_block_mode_title)
                            .content(R.string.dialog_block_mode_content)
                            .positiveText(R.string.dialog_block_mode_pos)
                            .negativeText(getString(R.string.cancel))
                            .onPositive((dialog, which) -> {
                                FirebaseUtil.getBlockStatusRef(FirebaseUtil.getCurrentUser().getUid()).setValue(true);
                                final String user_json = MyApp.getPref().getString(getString(R.string.pref_user_obj),"");
                                User user = new Gson().fromJson(user_json, User.class);
                                user.block_status = true;
                                MyApp.getPref().edit().putString(getString(R.string.pref_user_obj),new Gson().toJson(user)).apply();
                                MyApp.setUser(user);
                                setErrorScreen(R.string.error_block_mode);
                                statusText.setClickable(false);
                                revealBlockView(true);
                            })
                            .onNegative((dialog, which) -> blockSwitch.setChecked(false))
                            .cancelable(false)
                            .show();
                }else {
                    //unblock
                    FirebaseUtil.getBlockStatusRef(FirebaseUtil.getCurrentUser().getUid()).setValue(false);
                    final String user_json = MyApp.getPref().getString(getString(R.string.pref_user_obj),"");
                    User user = new Gson().fromJson(user_json, User.class);
                    user.block_status = false;
                    MyApp.setUser(user);
                    MyApp.getPref().edit().putString(getString(R.string.pref_user_obj),new Gson().toJson(user)).apply();
                    statusText.setClickable(true);
                    revealBlockView(false);

                    //if not restarted activity, butter knife binding was not happening for some reason in fragments
                    finish();
                    startActivity(new Intent(this, ActivityMain.class));
                }
            });
        }

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        moveToTabX();
    }

    private void moveToTabX() {
        if(getIntent().getExtras()!=null){
            String tab = getIntent().getExtras().getString("tab");
            if(tab!=null && viewPager!=null) {
                Log.d("ActivityMain", "onResume: setting tab " + tab);
                switch (tab) {
                    case "friends":
                        viewPager.setCurrentItem(0);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.ACTIONS.REFRESH_FRIEND_LIST));
                        break;

                    case "requests":
                        viewPager.setCurrentItem(1);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.ACTIONS.REFRESH_REQUESTS));
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
                new MaterialDialog.Builder(this)
                        .title(R.string.signout)
                        .content(R.string.logout_warn)
                        .positiveText(R.string.signout)
                        .negativeText(getString(R.string.cancel))
                        .onPositive((dialog, which) -> {
                            FirebaseUtil.getAuth().signOut();
                            //remove token from server
                            FirebaseUtil.getNotificationTokenRef().child(MyApp.getUser().username).removeValue();

                            //remove history
                            HistoryRepo.getInstance().nukeHistory();

                            startActivity(new Intent(ActivityMain.this, ActivityLoginSignup.class));
                            finish();
                        })
                        .show();
                break;

            case R.id.nav_history:
                startActivity(new Intent(this, ActivityHistory.class));
                break;

            case R.id.nav_rate:
                rateUs();
                break;

            /*case R.id.nav_settings:
                Toast.makeText(this, "Nothing here yet", Toast.LENGTH_SHORT).show();
                break;*/

            case R.id.nav_share:
                shareApp();
                break;

            case R.id.nav_write_me:
                feedbackEmail();
                break;

            case R.id.nav_ab_music:
                final String appPackageName = "com.bhandari.music"; // getPackageName() from Context or Activity object
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
                Toast.makeText(this, "Try out this awesome free music and lyrics fetching app", Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_instagram:
                openUrl(Uri.parse(INSTA_WEBSITE));
                break;

            case R.id.nav_about:
                startActivity(new Intent(this, ActivityAbout.class));
                break;

            case R.id.nav_faq:
                startActivity(new Intent(this, ActivityFAQ.class));
                break;

        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    private void termsOfUsage(){
        MaterialDialog dialog =  new MaterialDialog.Builder(this)
                .title("Terms of Usage")
                .customView(R.layout.dialog_terms_of_usage, true)
                .autoDismiss(false)
                .cancelable(false)
                .positiveText("I agree")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                        MyApp.getPref().edit().putBoolean(getString(R.string.pref_usage_terms),true).apply();
                        howItWorks();
                    }
                })
                .negativeText("Disagree")
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                        finish();
                    }
                }).build();

        View v = dialog.getCustomView();
        TextView text = v.findViewById(R.id.terms_of_usage_text);
        SpannableString ss = new SpannableString(getString(R.string.terms_and_usage));
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                openUrl(Uri.parse(TERMS_USAGE__WEBSITE));
            }
        };
        ss.setSpan(clickableSpan, 43, 57, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        text.setText(ss);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        text.setHighlightColor(Color.TRANSPARENT);

        dialog.show();
    }


    private void howItWorks(){
        String userName = "";
        if(MyApp.getUser()!=null) userName = MyApp.getUser().username;
        new MaterialDialog.Builder(this)
                .title(R.string.how_it_works_title)
                .content(R.string.how_it_works_content, userName)
                .positiveText(R.string.how_it_works_pos)
                .autoDismiss(false)
                .cancelable(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @OnClick(R.id.status_text)
    void refreshMainAct(){
        statusText.setText(R.string.retrying);
        loadInitialScreen();
    }

    private void openUrl(Uri parse) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, parse);
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening browser", Toast.LENGTH_SHORT).show();
        }
    }

    private void feedbackEmail() {
        String myDeviceModel = Build.MODEL;
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto",getString(R.string.au_email_id), null));
        String[] address = new String[]{getString(R.string.au_email_id)};
        emailIntent.putExtra(Intent.EXTRA_EMAIL, address);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for AB Remote Wallpaper Changer : Device " + myDeviceModel);
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello AB, \n\n");
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
        moveToTabX();
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
                        //@todo remove hardcoding 0
                        if(((FragmentFriends)adapter.get(0)).isFriend(userName)){
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdView != null) {
            mAdView.destroy();
        }
    }
}
