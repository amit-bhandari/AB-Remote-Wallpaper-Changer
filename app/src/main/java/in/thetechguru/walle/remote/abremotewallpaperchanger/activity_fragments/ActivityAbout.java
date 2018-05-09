package in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;

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

public class ActivityAbout extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        Toolbar toolbar = findViewById(R.id.toolbar_);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_privacy_policy:
                new MaterialDialog.Builder(this)
                        .title(R.string.title_privacy_policy)
                        .content(R.string.dialog_privacy_policy_content)
                        .positiveText(R.string.dialog_privacy_policy_pos)
                        .show();
                break;

            case R.id.action_terms_of_usage:
                openUrl(Uri.parse(ActivityMain.TERMS_USAGE__WEBSITE));
                break;

            case R.id.action_how_does_it_work:
                howItWorks();
                break;

            case R.id.action_lib:
                new LibsBuilder()
                        .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                        .withActivityTitle("Libraries")
                        .start(this);
                break;

            case android.R.id.home:
                overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
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

    private void openUrl(Uri parse) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, parse);
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening browser", Toast.LENGTH_SHORT).show();
        }
    }
}
