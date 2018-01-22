package in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.FirebaseUtil;

/**
 * Created by abami on 1/17/2018.
 */

public class FragmentProfile extends Fragment {

    @BindView(R.id.username)
    TextView username;

    @BindView(R.id.email_id)
    TextView emailid;

    @BindView(R.id.display_name)
    TextView display_name;

    @BindView(R.id.image_view_profile)
    ImageView profile_photo;

    public FragmentProfile(){}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_profile, container, false);
        ButterKnife.bind(this, layout);

        if(FirebaseUtil.getCurrentUser()!=null && MyApp.getUser().username!=null){
            display_name.setText(MyApp.getUser().display_name);
            username.setText( MyApp.getUser().username);
            emailid.setText(FirebaseUtil.getCurrentUser().getEmail());
            Glide.with(this)
                    .load(MyApp.getUser().pic_url)
                    .placeholder(R.drawable.person_blue)
                    .into(profile_photo);
        }

        return layout;
    }
}
