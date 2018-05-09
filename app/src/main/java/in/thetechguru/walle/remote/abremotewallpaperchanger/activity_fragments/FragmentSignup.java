package in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.FirebaseUtil;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.User;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.gson.Gson;

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

@SuppressWarnings("ConstantConditions")
public class FragmentSignup  extends Fragment implements  GoogleApiClient.OnConnectionFailedListener{

    private final String TAG = getClass().getSimpleName();
    private boolean isUserCreated;

    @BindView(R.id.root_View_sign_up) View rootView;
    @BindView(R.id.email_id_input) EditText email_input;
    @BindView(R.id.username_input) EditText username_input;
    @BindView(R.id.password_input) EditText password_input;
    @BindView(R.id.display_name_input) EditText display_name_input;
    @BindView(R.id.sign_up_button) CircularProgressButton sign_up_button;
    @BindView(R.id.sign_up_google) CircularProgressButton google_sign_up_button;

    private GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 7;

    private String personPhotoUrl ;

    public FragmentSignup() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .enableAutoManage(getActivity(), this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_signup, container, false);
        ButterKnife.bind(this, layout);
        
        //force small cap to username field
        InputFilter[] editFilters = username_input.getFilters();
        InputFilter[] newFilters = new InputFilter[editFilters.length + 1];
        System.arraycopy(editFilters, 0, newFilters, 0, editFilters.length);
        newFilters[editFilters.length] = new InputFilter.AllCaps() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                return String.valueOf(source).toLowerCase();
            }
        };
        username_input.setFilters(newFilters);
        return layout;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        google_sign_up_button.dispose();
        sign_up_button.dispose();
    }

    @OnClick(R.id.sign_up_button)
    void signUp(){

        //@todo validations
        final String display_name = display_name_input.getText().toString().trim();

        if(display_name.isEmpty()){
            display_name_input.setError(getString(R.string.empty_display_name_error));
            return;
        }

        final String email = email_input.getText().toString().trim();

        if(email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            email_input.setError(getString(R.string.invalid_email_id_error));
            return;
        }

        String password = password_input.getText().toString().trim();

        if(password.isEmpty()){
            password_input.setError(getString(R.string.empty_password_error));
            return;
        }

        if(password.length() < 6){
            password_input.setError(getString(R.string.weak_password_error));
            return;
        }

        final String username = username_input.getText().toString().trim();

        if(username.isEmpty()){
            username_input.setError(getString(R.string.empty_username_error));
            return;
        }

        /*
        data structure in firebase database
        app : {
            users: {
               "some-user-uid": {
                    email: "test@test.com"
                    username: "myname"
               }
            },
            usernames: {
                "myname": "some-user-uid"
            }
        }

        firebase validity rules for unique username
        "users": {
          "$uid": {
            ".write": "auth !== null && auth.uid === $uid",
            ".read": "auth !== null && auth.provider === 'password'",
            "username": {
              ".validate": "
                !root.child('usernames').child(newData.val()).exists() ||
                root.child('usernames').child(newData.val()).val() == $uid"
            }
          }
        }
         */

        sign_up_button.startAnimation();

        final User user = new User(display_name, username, personPhotoUrl);

        if(isAdded() && getActivity()!=null) {
            if(!isUserCreated) {
                FirebaseUtil.getAuth().createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "createUserWithEmail:success");
                                    isUserCreated = true;
                                    FirebaseUser firebaseUser = FirebaseUtil.getCurrentUser();
                                    //set default user name as user id
                                    //if some error comes while creating user name and user tries to login with the account
                                    // we wont be left with account without username
                                    FirebaseUtil.getUsernameRef(firebaseUser.getUid()).setValue(firebaseUser.getUid());

                                    //now create user_name for user
                                    createUserProfile(FirebaseUtil.getCurrentUser(), user);
                                } else {
                                    sign_up_button.revertAnimation();
                                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                    try {
                                        throw task.getException();
                                    } catch(FirebaseAuthUserCollisionException e) {
                                        email_input.setError(getString(R.string.account_exists_error));
                                        //move to login screen
                                    } catch (FirebaseAuthWeakPasswordException e) {
                                        password_input.setError(getString(R.string.weak_password_error));
                                    } catch (FirebaseAuthInvalidCredentialsException e) {
                                        email_input.setError(getString(R.string.weak_email_error));
                                    } catch (FirebaseNetworkException e){
                                        Snackbar.make(rootView,  R.string.no_network_error, Snackbar.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        Snackbar.make(rootView, getString(R.string.unknown_sign_up_error), Snackbar.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
            }else {
                createUserProfile(FirebaseUtil.getCurrentUser(),user);
            }
        }

    }

    @OnClick(R.id.sign_up_google)
    void googleSignIn() {
        google_sign_up_button.startAnimation();
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void createUserProfile(final FirebaseUser firebaseUser, final User user){

        email_input.setEnabled(false);
        password_input.setEnabled(false);
        display_name_input.setEnabled(false);

        FirebaseUtil.getUsersReference()
                .child(firebaseUser.getUid())
                .setValue(user);

        FirebaseUtil.getUsernameRef(firebaseUser.getUid()).setValue(user.username, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                if(databaseError!=null){
                    sign_up_button.revertAnimation();
                    Log.d("FragmentSignup", "onComplete: " + databaseError.getDetails());
                    switch (databaseError.getCode()){
                        case DatabaseError.PERMISSION_DENIED:
                            username_input.setError(getString(R.string.username_clash_error));
                            break;

                        default:
                            Snackbar.make(rootView, "Error occurred while creating user profile " 
                                    + "[Error code:" + databaseError.getCode() + "]", Snackbar.LENGTH_SHORT).show();
                            break;
                    }
                }else {
                    FirebaseUtil.getUsernamesReference().child(user.username).setValue(firebaseUser.getUid());

                    MyApp.getPref().edit().putString(getString(R.string.pref_user_obj), new Gson().toJson(user)).apply();

                    //send token to server
                    String token = MyApp.getPref().getString(getString(R.string.notification_token),"");
                    if(user.username==null || user.username.equals("") || token.equals("")) return;
                    FirebaseUtil.getNotificationTokenRef().child(user.username).setValue(token);

                    //USER CREATION PROCESS COMPLETE
                    MyApp.setUser(user);
                    startActivity(new Intent(getActivity(), ActivityMain.class));
                    getActivity().finish();
                }
            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            google_sign_up_button.revertAnimation();
            Log.d("FragmentSignup", "onActivityResult: singin result");
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d("FragmentSignup", "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            
            GoogleSignInAccount acct = result.getSignInAccount();
            if(acct==null){
                Snackbar.make(rootView, R.string.google_signin_error,Snackbar.LENGTH_SHORT).show();
                return;
            }

            String email = acct.getEmail();
            String name = acct.getDisplayName();
            if(email!=null) {
                email_input.setText(email);
            }

            if(name!=null){
                display_name_input.setText(name);
            }

            String userName = email.substring(0, email.indexOf('@')).replace(".","_");
            if(!userName.equals("")){
                username_input.setText(userName);

                //
            }

            if(acct.getPhotoUrl()!=null) {
                personPhotoUrl = acct.getPhotoUrl().toString();
            }

            google_sign_up_button.setEnabled(false);

            //log out from google, as we only wanted email id and username
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
            }
        } else {
            // some Error or user logged out, either case, update the drawer and give user appropriate info
                if (result.getStatus().getStatusCode() == CommonStatusCodes.NETWORK_ERROR) {
                    Snackbar.make(rootView, getString(R.string.network_error), Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(rootView, getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
                }
        }
    }
}
