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
import com.squareup.haha.perflib.Main;

import in.thetechguru.walle.remote.abremotewallpaperchanger.R;

/**
 * Created by abami on 04-Feb-18.
 */

public class ActivityPermissionSeek extends AppCompatActivity {
    final private int MY_PERMISSIONS_REQUEST = 0;
    private static String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!hasPermissions(this, PERMISSIONS)) {
            try {
                permissionDetailsDialog();
            }catch (Exception e){
                RequestPermission();
            }
        }else {
            startActivity(new Intent(this, ActivityMain.class));
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
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        startActivity(new Intent(this, ActivityMain.class));
                    }
                }
                break;
        }
    }

}
