package in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.FirebaseUtil;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.UtilityFun;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.User;

/**
 * Created by AB on 2017-12-24.
 */

public class FragmentLogin extends Fragment {

    private final String TAG = getClass().getSimpleName();
    @BindView(R.id.root_view_login) View rootView;
    @BindView(R.id.email_id_input) EditText email_input;
    @BindView(R.id.password_input) EditText password_input;
    @BindView(R.id.login_button) CircularProgressButton login_button;
    @BindView(R.id.forgot_password_textview) TextView forgotPassword;


    public FragmentLogin() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_login, container, false);
        ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        login_button.dispose();
    }

    @OnClick(R.id.forgot_password_textview)
    void forgotPassword(){
        if(!UtilityFun.isConnectedToInternet()){
            Snackbar.make(rootView, R.string.no_network_error, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if(isAdded() && getActivity()!=null) {
            new MaterialDialog.Builder(getActivity())
                    .title(R.string.password_reset_title)
                    .content(R.string.password_reset_content)
                    .inputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                    .autoDismiss(false)
                    .input(getString(R.string.email_id_hint), "", new MaterialDialog.InputCallback() {
                        @SuppressWarnings("ConstantConditions")
                        @Override
                        public void onInput(@NonNull final MaterialDialog dialog, CharSequence input) {

                            String email = input.toString().trim();
                            if(email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                                dialog.getInputEditText().setError(getString(R.string.invalid_email_id_error));
                                return;
                            }

                            // Do something
                            FirebaseUtil.getAuth().sendPasswordResetEmail(email)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "Check your email for password reset instruction.");
                                                Snackbar.make(rootView, R.string.password_reset_email_sent, Snackbar.LENGTH_SHORT).show();
                                                dialog.dismiss();
                                            }else {
                                                try {
                                                    throw task.getException();
                                                } catch (FirebaseAuthInvalidUserException e){
                                                    dialog.getInputEditText().setError(getString(R.string.invalid_user_error));
                                                } catch (FirebaseNetworkException e){
                                                    dialog.getInputEditText().setError(getString(R.string.no_network_error));
                                                } catch (Exception e) {
                                                    dialog.getInputEditText().setError(getString(R.string.unknown_error));
                                                }
                                            }
                                        }
                                    });
                        }
                    }).show();
        }
    }

    @OnClick(R.id.login_button)
    void login(){
        String email = email_input.getText().toString().trim();

        if(email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            email_input.setError(getString(R.string.invalid_email_id_error));
            return;
        }

        final String password = password_input.getText().toString().trim();

        if(password.isEmpty()){
            password_input.setError(getString(R.string.empty_password_error));
            return;
        }

        login_button.startAnimation();

        if(isAdded() && getActivity()!=null) {
            FirebaseUtil.getAuth().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                        @SuppressWarnings("ConstantConditions")
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser firebaseUser = task.getResult().getUser();
                                //get username
                                if(firebaseUser!=null) {
                                    DatabaseReference db = FirebaseUtil.getUsersReference().child(firebaseUser.getUid());
                                    db.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            User user = dataSnapshot.getValue(User.class);
                                            MyApp.getPref().edit().putString(getString(R.string.pref_user_obj), new Gson().toJson(user)).apply();

                                            //send token to server
                                            String token = MyApp.getPref().getString(getString(R.string.notification_token),"");
                                            if(user.username==null || user.username.equals("") || token.equals("")) return;
                                            FirebaseUtil.getNotificationTokenRef().child(user.username).setValue(token);

                                            startActivity(new Intent(getActivity(), ActivityMain.class));
                                            getActivity().finish();
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            login_button.revertAnimation();
                                            Snackbar.make(rootView,  R.string.unknown_login_error, Snackbar.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                            } else {
                                // If sign in fails, display a message to the user.
                                login_button.revertAnimation();
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                Snackbar.make(rootView,  R.string.login_failed_error, Snackbar.LENGTH_SHORT).show();
                                try {
                                    throw task.getException();
                                } catch(FirebaseAuthInvalidUserException e) {
                                    email_input.setError(getString(R.string.invalid_user_error));
                                } catch (FirebaseAuthInvalidCredentialsException e) {
                                    password_input.setError(getString(R.string.invalid_password_error));
                                } catch (FirebaseNetworkException e){
                                    Snackbar.make(rootView,  R.string.no_network_error, Snackbar.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Snackbar.make(rootView,  R.string.unknown_login_error, Snackbar.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
        }
    }
}
