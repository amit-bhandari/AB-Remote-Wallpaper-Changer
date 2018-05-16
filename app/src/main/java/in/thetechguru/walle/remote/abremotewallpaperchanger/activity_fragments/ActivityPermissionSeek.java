package in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import in.thetechguru.walle.remote.abremotewallpaperchanger.BuildConfig;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.preferences.Preferences;

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

public class ActivityPermissionSeek extends AppCompatActivity {
    final private int MY_PERMISSIONS_REQUEST = 0;
    private static String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }

        if(!hasPermissions(this, PERMISSIONS)) {
            try {
                permissionDetailsDialog();
            }catch (Exception e){
                RequestPermission();
            }
        }else {
            startActivity(new Intent(this, ActivityMain.class));
            //finish();
        }

        if(BuildConfig.DEBUG){
            Preferences.setAdsRemoved(this, true);
        }
    }

    private void permissionDetailsDialog(){
        new MaterialDialog.Builder(this)
                .title(R.string.permission_grant_title)
                .content(R.string.permission_grant_content)
                .positiveText(R.string.permission_grant_pos)
                .negativeText(getString(R.string.cancel))
                .cancelable(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        RequestPermission();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Toast.makeText(ActivityPermissionSeek.this, "Sorry, but app needs storage permission to work properly!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .show();
    }

    private void RequestPermission(){
        // Here, thisActivity is the current activity
        ActivityCompat.requestPermissions(this,
                PERMISSIONS,
                MY_PERMISSIONS_REQUEST);

    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {

                if(grantResults.length==0){
                    return;
                }
                // If request is cancelled, the result arrays are empty.
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //finish();
                    startActivity(new Intent(this, ActivityMain.class));
                    }
                }
                break;
        }
    }

}
