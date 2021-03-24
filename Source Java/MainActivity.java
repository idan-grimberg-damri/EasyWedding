package com.example.easywedding;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.example.easywedding.Utils.Utils;
import com.example.easywedding.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements GuestsFragment.OnGuestArrivingCountPass {
    //fields for view pager and tabs
    private ViewPager2 mViewPager2;
    private TabLayout mTabLayout;
    ViewPagerFragmentAdapter mAdapter; // Package access
    // true if the fragments that are bound to the view pagers are cleared
    // false otherwise.
    private boolean isDataCleared;

    private int mArrivingGuestCount;

    // DB fields
    private DatabaseReference mRootReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private String mDataId;
    private boolean mHasSharedData;
    //sign-in flow request code
    private static final int RC_SIGN_IN = 1;
    private ProgressBar mProgressBar;
    private View mTabsDivider;

    private String mSubtitleSuffix;
    // SharedPreference fields
    private SharedPreferences mSharedPref;
    private String mFeaturesSortVal;
    private String mGuestsSortVal;
    private boolean mIsSortGuestByArriving;


    /*fragments constants*/
    public static final int FRAGMENTS_COUNT = 3;
    public static final int FEATURES_POSITION = 0;
    public static final int GUESTS_POSITION = 1;
    public static final int CHAT_POSITION = 2;

    public static final String TAG = MainActivity.class.getSimpleName();

    private ValueEventListener mChangeInOwnerListener;
    private DatabaseReference mOwnerRef;
    private ViewPager2.OnPageChangeCallback mPageChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressBar = findViewById(R.id.main_progress_bar);
        mTabsDivider = findViewById(R.id.main_tabs_divider);

        isDataCleared = true;
        // AppBar
        mSubtitleSuffix = getString(R.string.arriving);
        // DB
        mRootReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();

        setSharedPrefFields();

       // if (!mHasSharedData)
       //     checkIfUSerWipedAppData();


        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // If we don't have the key to the user's data then fetch the key from the db.
                    if (TextUtils.isEmpty(mDataId)) {

                        mTabsDivider.setVisibility(View.INVISIBLE);
                        mProgressBar.setVisibility(View.VISIBLE);

                        mRootReference.child(Constants.PATH_USERS)
                                .child(user.getUid())
                                .child(Constants.PATH_DATA_ID)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            mDataId = snapshot.getValue(String.class);

                                            if (mDataId != null && !TextUtils.isEmpty(mDataId))

                                            // save the chat id in the user device. We'll listen
                                            // to the user data id in the db only once in this listener.
                                            // That happens when the user sign up for the first time
                                            // or mDataId wiped from the user device after the user uninstalled the app
                                            // or wiped his memory
                                            mSharedPref.edit().putString(Constants.FRAGMENT_DATA_ID_ARG, mDataId).apply();
                                            // user is signed in and we fetched the key to his data.
                                            // If we didn't already attached the tabs to the viewpager
                                            // then to the attachment (show to the user his data).
                                            if (isDataCleared) {
                                                handleViewPagerWithTabs();
                                                isDataCleared = false;
                                            }

                                            mProgressBar.setVisibility(View.GONE);
                                            mTabsDivider.setVisibility(View.VISIBLE);


                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(MainActivity.this, R.string.error_generic_msg, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                    // Else we already fetched the key to the user's data so show the user his data.
                    else {
                        if (isDataCleared) {
                            handleViewPagerWithTabs();
                            isDataCleared = false;
                        }
                        mProgressBar.setVisibility(View.GONE);
                        mTabsDivider.setVisibility(View.VISIBLE);
                    }

                } else {
                    // user is signed out, use FirebaseUI sign-in flow
                    createSignInIntent();
                }
            }
        };

    }

    /**
     * Set the fields that dependent on in-device memory values
     * these fields will save us queries to the db
     */
    private void setSharedPrefFields() {
        mSharedPref = getPreferences(Context.MODE_PRIVATE);
        mFeaturesSortVal = mSharedPref.getString(Constants.FEATURES_SORT_KEY, "");
        mGuestsSortVal = mSharedPref.getString(Constants.GUESTS_SORT_KEY, "");
        mIsSortGuestByArriving = mSharedPref.getBoolean(Constants.GUEST_ARRIVAL_SORT_BOOL_VAL, false);
        mDataId = mSharedPref.getString(Constants.FRAGMENT_DATA_ID_ARG, "");
       // mHasSharedData = mSharedPref.getBoolean(Constants.HAS_SHARED_DATA, false);
        mDataId = "";
    }

    /**
     *
     * @return {@link androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback}
     * that sets the subtitle of the app's action bar to the number of guests that confirmed arrival
     * when the we switch pages in the viewpager to the guests page.
     */
    private ViewPager2.OnPageChangeCallback createOnPageChangeListener() {
        return new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {

                ActionBar ab = getSupportActionBar();
                if (ab != null) {
                    if (position == Constants.GUEST_FRAGMENT_POS && 0 < mArrivingGuestCount) {
                        ab.setSubtitle(mArrivingGuestCount + " " + mSubtitleSuffix);
                    } else {
                        ab.setSubtitle("");
                    }
                }
                super.onPageSelected(position);
            }
        };
    }

    /**
     * if the user shared his data with someone then the owner record in the db
     * will contain the value "noOwner".
     * The field mHashSharedData is true if the fact above was saved s a boolean
     * in the user's device. However, if the user wiped his device data or uninstalled the app
     * and then installed it again then we need to check the db if the owner record
     * contains the special value "noOwner" and write this fact again to the user's device
     * as a boolean
     */
    private void checkIfUSerWipedAppData() {
        String uid = mFirebaseAuth.getUid();
        if (uid != null) {
            mRootReference.child(Constants.PATH_USERS)
                    .child(uid)
                    .child(Constants.PATH_OWNER)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String owner = snapshot.getValue(String.class);
                            // If the user have the special value "noOwner" for the owner field
                            // in the db then his data is shared.
                            if (owner != null && owner.equals(Constants.NO_OWNER_INDICATOR)) {
                                mHasSharedData = true;
                                mSharedPref.edit().putBoolean(Constants.HAS_SHARED_DATA, true).apply();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        }
    }



    /**
     * Listen to changes in the user's owner record in the db.
     * If this value is "noOwner" then this user is sharing his data with someone else
     * so his dataId is never change again under his account, so indicate this
     * fact by writing a boolean flag to the user device.
     * If we don't do this then we'll fetch the dataId from the db every time
     * that the user enters onCreate state in this activity.
     */
    private void ListenToChangeInOwner() {

        String uid = mFirebaseAuth.getUid();
        if (uid != null && mChangeInOwnerListener == null) {
            mOwnerRef = mRootReference.child(Constants.PATH_USERS)
                    .child(uid)
                    .child(Constants.PATH_OWNER);

            mChangeInOwnerListener = setChangeInOwnerListener();
            mOwnerRef.addValueEventListener(mChangeInOwnerListener);

        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case RC_SIGN_IN:
                if (resultCode == RESULT_OK) {
                    // IdpResponse is a container that encapsulates the result of authenticating with an Identity Provider.
                    IdpResponse response = IdpResponse.fromResultIntent(data);
                    // If this is the first time the user did sign in
                    // then create a user record for this user in the users node in the db
                    if (response != null) {
                        if (response.isNewUser()) {
                            // The user's chat is initially closed, so no chat id
                            FirebaseUser user = mFirebaseAuth.getCurrentUser();


                            User newUser = new User(user.getDisplayName(),
                                    user.getEmail(), user.getUid());


                            Map<String, Object> updatedFields = new HashMap<>();
                            updatedFields.put("/" + Constants.PATH_GROUPS + "/" +
                                    user.getUid() + "/usersCount", 1);

                            updatedFields.put("/" + Constants.PATH_USERS + "/" +
                                    user.getUid(), newUser);

                            mRootReference.updateChildren(updatedFields);

                        }

                        if (isDataCleared) {
                            handleViewPagerWithTabs();
                            isDataCleared = false;
                        }
                    }
                    // Sign in failed. If response is null the user canceled the
                    // sign-in flow using the back button. Otherwise check
                    // response.getError().getErrorCode() and handle the error.
                } else if (resultCode == RESULT_CANCELED) {
                    finish();
                }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        // start/resume listening to changes in db related data

        if (mFirebaseAuth != null)
            mFirebaseAuth.addAuthStateListener(mAuthStateListener);

        if (!mHasSharedData && mOwnerRef != null && mChangeInOwnerListener == null) {

            mChangeInOwnerListener = setChangeInOwnerListener();
            mOwnerRef.addValueEventListener(mChangeInOwnerListener);
        }
        if (mViewPager2 != null && mPageChangeListener == null){
            mPageChangeListener = createOnPageChangeListener();
            mViewPager2.registerOnPageChangeCallback(mPageChangeListener);
        }


    }



    @Override
    protected void onPause() {
        super.onPause();
       // Stop listening to changes in db related data
        if (mFirebaseAuth != null)
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);

        if (mOwnerRef != null && mChangeInOwnerListener != null) {
            mOwnerRef.removeEventListener(mChangeInOwnerListener);
            mChangeInOwnerListener = null;
        }

        if (mViewPager2 != null && mPageChangeListener != null){
            mViewPager2.unregisterOnPageChangeCallback(mPageChangeListener);
            mPageChangeListener = null;
        }

    }


    /**
     * create a sign-in flow with a  {@link Intent} when a user need to sign-up/sign-in
     */
    public void createSignInIntent() {

        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false)
                        .setLogo(R.drawable.ic_easywedding_logo)
                        .setTheme(R.style.LoginTheme)
                        .build(),
                RC_SIGN_IN);
    }

    /**
     * signs out the current user
     */
    private void signOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(MainActivity.this, R.string.signed_out_success
                                , Toast.LENGTH_SHORT).show();
                    }
                });

    }

    /**
     * set the toolbar and link the TabsLayout with the ViewPager2
     */
    private void handleViewPagerWithTabs() {

        mViewPager2 = findViewById(R.id.main_viewpager);
        mAdapter = new ViewPagerFragmentAdapter(getSupportFragmentManager(), getLifecycle(), mDataId,
                mFeaturesSortVal, mGuestsSortVal, mIsSortGuestByArriving);
        mViewPager2.setAdapter(mAdapter);

        if (mPageChangeListener == null){
            mPageChangeListener = createOnPageChangeListener();
            mViewPager2.registerOnPageChangeCallback(mPageChangeListener);
        }

        mTabLayout = findViewById(R.id.main_tabs);
        // link the TabLayout with the ViewPager
        new TabLayoutMediator(mTabLayout, mViewPager2,
                new TabLayoutMediator.TabConfigurationStrategy() {

                    @Override
                    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                        // set tab title according to its position
                        switch (position) {
                            case FEATURES_POSITION:
                                tab.setText(R.string.tab_features_title);
                                tab.setIcon(R.drawable.vector_features_24);
                                break;

                            case GUESTS_POSITION:
                                tab.setText(R.string.tab_guests_title);
                                tab.setIcon(R.drawable.vector_guests_24);
                                break;

                            case CHAT_POSITION:
                                tab.setText(R.string.tab_chat_title);
                                tab.setIcon(R.drawable.vector_chat_24);
                                break;
                        }
                    }
                }
        ).attach();



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.main_menu, menu);
        // if the condition is true then this user already shared his data
        // (he opened access or ask for access and his request was granted,
        // so he shouldn't be able to ask someone to share data with him,
        // so hide the item menu who give him the ability to request data access.
        if (mHasSharedData){
            ((MenuItem) menu.findItem(R.id.action_request_access)).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                signOut();
                removeFragments();
                // Next time the user logs in we'll create new pages for the fragments.
                isDataCleared = true;
                return true;

            case R.id.action_request_access:
                // TODO show dialog and explain. Allow enter an owner email.

                manageSharedDataAccess(true);

                return true;

            case R.id.action_grant_access:
                // TODO
                manageSharedDataAccess(false);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Ask for, or grant access to the wedding's data.
     *
     * @param isAskingForAccess true if the current user is asking someone else to give him
     *                          access to the wedding's data, false if the current user is the one
     *                          who grants the access to the wedding's data.
     */
    private void manageSharedDataAccess(final boolean isAskingForAccess) {
        ConstraintLayout emailInputLayout = (ConstraintLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_chat_invite, null);
        final TextInputLayout textInputLayout = emailInputLayout.findViewById(R.id.dialog_email_input_layout);
        textInputLayout.getEditText().addTextChangedListener(new CustomTextWatcher(textInputLayout));

        String title, message;

        final String invalidEmailErrorMessage = getString(R.string.error_chat_email_input);
        // If this user is asking for access to the wedding's data
        if (isAskingForAccess) {
            title = getString(R.string.dialog_title_request_data);
            message = getString(R.string.dialog_body_request_data);
        } else {
            title = getString(R.string.dialog_title_grant_access);
            message = getString(R.string.dialog_body_grant_access);
        }
        textInputLayout.setHelperText(message);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(title)
                .setPositiveButton(R.string.dialog_positive_confirm
                        , null)
                .setNegativeButton(R.string.dialog_negative_cancel, null)
                .setView(emailInputLayout);

        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = textInputLayout.getEditText().getText().toString();

                if (Utils.isValidEmail(email)) {
                    // If this user asks for access then the owner is the one who will grant the access.
                    // so write to the db who is the owner (write the owner's email).
                    if (isAskingForAccess)
                        writeOwnerToDB(email);
                    else // Else the current user is the one who grants access to the data
                        grantAccessToSharedData(email);

                    dialog.dismiss();
                    // Else show an error message and don't dismiss the dialog.
                } else
                    textInputLayout.setError(invalidEmailErrorMessage);
            }
        });
    }

    /**
     * Grant access to shared data. This will add the requesting user  to the goup
     * of the current user. After that they will share the data of the current user
     * @param email the email of the user who requested data acccess
     */
    private void grantAccessToSharedData(final String email) {
        Query query = mRootReference.child(Constants.PATH_USERS)
                .orderByChild(Constants.ORDER_BY_EMAIL).equalTo(email);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    final DataSnapshot otherUserSnapShot = snapshot.getChildren().iterator().next();
                    // If the other user gave access to the current user for granting him
                    // access to the shared data then the other user set the owner's email to the
                    // current user email.
                    User otherUser = otherUserSnapShot.getValue(User.class);

                    if (otherUser.getOwner() != null
                            && mFirebaseAuth.getCurrentUser().getEmail().equals(otherUser.getOwner())) {

                        updateFields(otherUserSnapShot.getKey());
                    } else {
                        Toast.makeText(MainActivity.this, "You can't grant access to " + email + ".", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(MainActivity.this, "No user with email " + email + ".", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, R.string.error_generic_msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * We get here after we grant access to our shared data.
     * This method update relevant fields in the db that required to be updated
     * after the access has granted to the user with the given key.
     *
     * @param otherKey the key of the user we're giving the access .
     */
    private void updateFields(final String otherKey) {
        // We need the value of the record dataId since it will point to the guests, features, chat and group.
        mRootReference.child(Constants.PATH_USERS)
                .child(mFirebaseAuth.getUid())
                .child("dataId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String dataId = snapshot.getValue(String.class);

                    Map<String, Object> updatedFields = new HashMap<>();
                    // Add the other user to the group
                    updatedFields.put("/" + Constants.PATH_GROUPS
                            + "/" + dataId
                            + "/" + otherKey, "owner");

                    // set the dataId record of the other user to the dataId of the user who gave the access.
                    updatedFields.put("/" + Constants.PATH_USERS
                            + "/" + otherKey
                            + "/dataId", dataId);

                    // Since The current user grant access to his data
                    // we need to indicate this and save this indication in
                    // the user's device. This will allow us to query the user's device
                    // not the db, for the data id.

                    // If this indication (mHasSharedData) is not already in the user's device
                    // then write it to the user's device and indicate it in the db
                    // with the special value "noOwner" for the owner record
                        if (!mHasSharedData) {
                            mSharedPref.edit().putBoolean(Constants.HAS_SHARED_DATA, true).apply();

                            updatedFields.put("/" + Constants.PATH_USERS
                                    + "/" + mFirebaseAuth.getUid()
                                    + "/owner", Constants.NO_OWNER_INDICATOR);

                        }
                    // Also, write to the other user (who asked for data access)
                    // that he now have shared data. We do this by writing to his owner record in the db
                    // the value "noOwner"
                    updatedFields.put("/" + Constants.PATH_USERS
                            + "/" + otherKey
                            + "/owner", Constants.NO_OWNER_INDICATOR);

                    // delete the data of the user who request the access since this dta
                    // is no longer relevant (and shouldn't be populated in the first place).
                    updatedFields.put("/" + Constants.PATH_FEATURES
                            + "/" + otherKey, null);

                    updatedFields.put("/" + Constants.PATH_GUESTS
                            + "/" + otherKey, null);

                    updatedFields.put("/" + Constants.PATH_GROUPS
                            + "/" + otherKey, null);

                    mRootReference.updateChildren(updatedFields, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if (error != null)
                                Toast.makeText(MainActivity.this, R.string.error_generic_msg, Toast.LENGTH_SHORT).show();
                            else

                                Toast.makeText(MainActivity.this, R.string.access_granted, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, R.string.error_generic_msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Write the owner's email to the db. The owner is the one who will grant the current user
     * access.
     * @param ownerEmail
     */
    private void writeOwnerToDB(String ownerEmail) {
        mRootReference.child(Constants.PATH_USERS)
                .child(mFirebaseAuth.getUid())
                .child("owner")
                .setValue(ownerEmail)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this, R.string.success_request_data, Toast.LENGTH_SHORT).show();
                        // if the user didn't share the data with someone the listen for a change in
                        // this situation , since the data id will probably change soon.
                        if (!mHasSharedData) {
                            ListenToChangeInOwner();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, R.string.error_generic_msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Inform the user that his data is about to change.
     * This will happen after this user asked for data access and then his request was granted
     */
    private void createDataIsAboutToChangeDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_access_granted)
                .setMessage(R.string.dialog_body_access_granted)
                .setNeutralButton(R.string.dialog_positive_ok, null);

        AlertDialog dialog = builder.create();

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                startActivity(new Intent(MainActivity.this, MainActivity.class));
                finish();
            }
        });

        dialog.show();

    }

    /**
     * Removing the fragments. This method gets called after a user has signed out.
     */
    private void removeFragments() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        for (Fragment fragment : getSupportFragmentManager().getFragments())
            ft.remove(fragment);

        ft.commitAllowingStateLoss();
    }


    /**
     *
     * @return {@link ValueEventListener} that listen to changes in the data ownership.
     */
    private ValueEventListener setChangeInOwnerListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String owner = snapshot.getValue(String.class);
                // If owner == "noOwner" then this user now have access to shared data
                // and his dataId in the db had changed, so fetch the the new data id
                // and save it to the user device.
                if (owner != null && owner.equals(Constants.NO_OWNER_INDICATOR)) {
                    mSharedPref.edit().putBoolean(Constants.HAS_SHARED_DATA, true).apply();
                    mRootReference.child(Constants.PATH_USERS)
                            .child(mFirebaseAuth.getUid())
                            .child(Constants.PATH_DATA_ID)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String newDataId = snapshot.getValue(String.class);
                                    if (newDataId != null){
                                        // No need to listen anymore since the ownership will change
                                        // only once.
                                        mOwnerRef.removeEventListener(this);
                                        // save the new data id value.
                                        mDataId = newDataId;
                                        mSharedPref.edit().putString(Constants.FRAGMENT_DATA_ID_ARG, mDataId).apply();
                                        createDataIsAboutToChangeDialog();
                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.v(TAG, error.toException().toString());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.v(TAG, error.toException().toString());
            }
        };
    }

    /**
     * Set the subtitle of the app's action bar to the number of guests that confirmed arrival.
     * This method gets called from {@link GuestsFragment} when the state of the guests adapter
     * changes.
     * @param guestCount the number of guests that confirmed arrival (in a direct or indirect way).
     */
    @Override
    public void onGuestArrivingCountPass(int guestCount) {
        mArrivingGuestCount = guestCount;
        ActionBar ab = getSupportActionBar();
        // Required since in some cases, the guests adapter will finish to load the data (guests)
        // after the view pager page change listener needs to update the
        // arriving guests count.
        if (mViewPager2 != null
                && mViewPager2.getCurrentItem() == Constants.GUEST_FRAGMENT_POS){
    // && TextUtils.isEmpty(ab.getSubtitle()
            ab.setSubtitle(mArrivingGuestCount + " " + mSubtitleSuffix);
        }
    }


}


























