package in.thetechguru.walle.remote.abremotewallpaperchanger;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;

import com.squareup.leakcanary.LeakCanary;

import in.thetechguru.walle.remote.abremotewallpaperchanger.model.User;

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

public class MyApp extends Application {
    private static MyApp instance;
    private static SharedPreferences pref;
    private static User user;

    @Override
    public void onCreate() {
        instance = this;
        pref = PreferenceManager.getDefaultSharedPreferences(this);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);

        /*
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath(TypeFaceHelper.getTypeFacePath())
                        .setFontAttrId(R.attr.fontPath)
                        .build());

*/
        //this stops crash reports, that's why removed
        /*Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException (Thread thread, Throwable e)
            {
                handleUncaughtException (thread, e);
            }
        });*/

        super.onCreate();
    }

    public void handleUncaughtException (Thread thread, Throwable e)
    {
        e.printStackTrace(); // not all Android versions will print the stack trace automatically
        Intent intent = new Intent ();
        intent.setAction ("com.bhandari.music.SEND_LOG"); // see step 5.
        intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK); // required when starting from Application
        startActivity (intent);
        System.exit(1); // kill off the crashed app
    }

    public static Context getContext(){
        return instance;
    }

    public static SharedPreferences getPref(){
        return pref;
    }

    public static User getUser() {
        return user;
    }

    public static void setUser(User user) {
        MyApp.user = user;
    }

}