package com.example.easywedding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.easywedding.Utils.Utils;
import com.example.easywedding.model.Feature;
import com.example.easywedding.model.Guest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AddGuestActivity extends AppCompatActivity {
    // Max joiners for a guest
    private static final int MAX_JOINERS = 10;
    // REQUEST CODE to the intent that connect us from this activity to
    // the contacts activity
    private static final int RC_FROM_ADD_GUEST_TO_CONTACTS = 1;
    // the state of the input fields
    // if we in edit mode (edit existing guest) then use this state
    // to figure what fields are needed to update in the db
    private static final int STATE_SIZE = 5;
    //layouts
    private TextInputLayout mLayoutName;
    private TextInputLayout mLayoutPhone;
    private TextInputLayout mLayoutEmail;
    private TextInputLayout mLayoutPriority;
    // guests phones
    private ArrayList<String> mPhones;
    // form fields
    private CheckBox mArrivingCheckBox;
    private TextInputLayout mLayoutJoiners;

    private LinearLayout mDummyLayout;

    private MaterialButton mAddFromContactsButton;
    // for the priority
    private ArrayAdapter<String> mDropdownMenuAdapter;

    private Intent mIntent;
    private boolean initialArrivingState;
    // see explanation above
    private boolean[] changeInState;
    private static final int NAME = 0;
    private static final int PHONE = 1;
    private static final int PRIORITY = 2;
    private static final int JOINERS = 3;
    private static final int EMAIL = 4;

    private DatabaseReference mRootRef;
    private FirebaseAuth mAuth;
    private String mDataId;

    // Map priority from string value to int value
    private static final Map<String, Integer> priorityMap = new HashMap<>();

    // Identifier for the permission request
    private static final int READ_CONTACTS_PERMISSIONS_REQUEST = 1;
    public static final String TAG = AddGuestActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_guest);

        getPermissionToReadUserContacts();

        mDummyLayout = findViewById(R.id.dummy_layout_guests);

        changeInState = new boolean[STATE_SIZE];


        setFields();

        takeFocusFromInputFields();

        setTextWatchers();

        handleRTLLayout();

        setPriorityMap();

        mAddFromContactsButton = findViewById(R.id.button_add_contact);

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();


        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayHomeAsUpEnabled(true);
        // This is  the action we will take if the user start the contacts activity
        mAddFromContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddGuestActivity.this, ContactsActivity.class);

                intent.putExtra(Constants.FRAGMENT_DATA_ID_ARG, mDataId);
                // pass to the contacts activity the phones of the guests
                if (mPhones != null)
                    intent.putStringArrayListExtra(Constants.EXTRA_PHONE_LIST, mPhones);

                startActivityForResult(intent, RC_FROM_ADD_GUEST_TO_CONTACTS);
            }
        });


        // If the user is adding a new guest (not editing existing one)
        // then set the title accordingly and set the default priority value.
        if (!mIntent.hasExtra(GuestsFragment.EXTRA_GUEST)) {
            setTitle(R.string.title_add_guest);

            ((MaterialAutoCompleteTextView) mLayoutPriority.getEditText()).setText(mDropdownMenuAdapter.getItem(0), false);

            // Else the user is editing an existing feature, so prepare
            // the feature data and set the title to the feature's name.
        } else {
            setForEditExistingGuest(mIntent);
        }

        setEndIconsInvisibleOnStartup();

    }



    /**
     * Editing an existing guest using the intent we got from GuestsFragment.
     *
     * @param intent the intent we get the guest data from.
     */
    private void setForEditExistingGuest(Intent intent) {
        Guest guest = (Guest) intent.getParcelableExtra(GuestsFragment.EXTRA_GUEST);

        if (guest != null) {
            setTitle(guest.getName());

            mLayoutPhone.getEditText().setText(guest.getPhoneNumber());
            mLayoutJoiners.getEditText().setText(guest.getJoiners());
            // this value will be null (guest.getEmail);
            //  mLayoutEmail.getEditText().setText(guest.getEmail());
            mLayoutName.getEditText().setText(guest.getName());
            ((MaterialAutoCompleteTextView) mLayoutPriority.getEditText()).setText(mDropdownMenuAdapter.getItem(guest.getPriority())
                    , false);
            mArrivingCheckBox.setChecked(guest.isArrive());
            initialArrivingState = guest.isArrive();


        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // If the user got back here from contacts activity after he saved contacts
        // then we want the user to get to the guests fragment.
        if (requestCode == RC_FROM_ADD_GUEST_TO_CONTACTS && resultCode == RESULT_OK) {
           // This condition will be true if the user saved guest via
            // the contacts activity. In this case we'll want to
            // return to the main activity exactly to the GuestFragment.
            if (data == null) {
                onBackPressed();
            }
            // Else the user pressed on the home button in the ContactsActivity
            // so restore the data needed for this activity.
            else{
                if (data.hasExtra(Constants.FRAGMENT_DATA_ID_ARG))
                    mDataId =  data.getStringExtra(Constants.FRAGMENT_DATA_ID_ARG);

                if (data.hasExtra(Constants.EXTRA_PHONE_LIST))
                    mPhones = data.getStringArrayListExtra(Constants.EXTRA_PHONE_LIST);
            }
        }

    }

    /**
     * Set the mapping from string priority values to int priority values
     */
    private void setPriorityMap() {
        String[] priorities = getResources().getStringArray(R.array.guest_dropdown_menu_values);
        for (int i = 0; i < priorities.length; i++)
            priorityMap.put(priorities[i], i);
    }

    /**
     * Handle locale sensitive input fields
     */
    private void handleRTLLayout() {
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {

            // start writing the phone number from the right (in a RTL layout)
            // I guess android change this automatically to LTR so the alignment of the text
            // is to the end, which is the start of the RTL layout
            mLayoutPhone.getEditText().setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        }
    }

    /**
     * Listen to change in text in the text input fields
     */
    private void setTextWatchers() {
        mLayoutJoiners.getEditText().addTextChangedListener(new CustomTextWatcher(mLayoutJoiners, changeInState, JOINERS));
        mLayoutName.getEditText().addTextChangedListener(new CustomTextWatcher(mLayoutName, changeInState, NAME));
        mLayoutEmail.getEditText().addTextChangedListener(new CustomTextWatcher(mLayoutEmail, changeInState, EMAIL));
        mLayoutPhone.getEditText().addTextChangedListener(new CustomTextWatcher(mLayoutPhone, changeInState, PHONE));
        mLayoutPriority.getEditText().addTextChangedListener(new CustomTextWatcher(mLayoutPriority, changeInState, PRIORITY));


    }

    /**
     * Set the end icons "clear text" to invisible when the user first start this activity
     */
    private void setEndIconsInvisibleOnStartup() {
        mLayoutJoiners.setEndIconVisible(false);
        mLayoutPhone.setEndIconVisible(false);
        mLayoutEmail.setEndIconVisible(false);
        mLayoutName.setEndIconVisible(false);

    }

    /**
     * Initialize this activity attributes
     */
    private void setFields() {
        mLayoutEmail = findViewById(R.id.text_layout_guest_email);
        mLayoutName = findViewById(R.id.layout_guest_name);
        mLayoutPhone = findViewById(R.id.layout_guest_phone);
        mLayoutPriority = findViewById(R.id.layout_guest_priority);
        mArrivingCheckBox = findViewById(R.id.guest_checkbox_arrive);
        mLayoutJoiners = findViewById(R.id.layout_guest_joiners);
        mIntent = getIntent();

       if (mIntent.hasExtra(Constants.FRAGMENT_DATA_ID_ARG))
            mDataId = mIntent.getStringExtra(Constants.FRAGMENT_DATA_ID_ARG);

        if (mIntent.hasExtra(Constants.EXTRA_PHONE_LIST))
            mPhones = mIntent.getStringArrayListExtra(Constants.EXTRA_PHONE_LIST);

        mDropdownMenuAdapter = new ArrayAdapter<>(
                this
                , R.layout.guest_dropdown_menu_item
                , getResources().getStringArray(R.array.guest_dropdown_menu_values));

        ((MaterialAutoCompleteTextView) mLayoutPriority.getEditText()).setAdapter(mDropdownMenuAdapter);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_guest_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Imitate onBackPresses behavior, which is getting back
                // to GuestsFragment
                onBackPressed();
                return true;

            case R.id.action_save_one_guest:

                mDummyLayout.requestFocus();
                // Id the form fields are valid then write to the db
                if (isDataValid()) {

                    Utils.hideKeyboardInActivity(this);

                    mDummyLayout.requestFocus();

                    updateDatabase(mIntent.hasExtra(GuestsFragment.EXTRA_GUEST_KEY));

                    onBackPressed();
                }

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }


    private void takeFocusFromInputFields() {
        mDummyLayout.setFocusable(true);
        mDummyLayout.setFocusableInTouchMode(true);
        mDummyLayout.requestFocus();
    }

    /**
     * Validate the input fields data
     * @return true if the input fields of the guest form are valid, false otherwise.
     */
    private boolean isDataValid() {
        String phone =  mLayoutPhone.getEditText().getText().toString().trim().replaceAll("-", "");
        // If the current guest already in the user guests then return false
        if (Utils.isDuplicateGuest(phone, mPhones)) {
            Toast.makeText(this, R.string.dup_guest, Toast.LENGTH_SHORT).show();
            return false;
        }

        String errorMessage = getString(R.string.error_required_field_empty);
        // email validation
        if (!TextUtils.isEmpty(mLayoutEmail.getEditText().getText()) &&
                !Utils.isValidEmail(mLayoutEmail.getEditText().getText())) {
            mLayoutEmail.setError(getString(R.string.error_chat_email_input));
            mLayoutEmail.requestFocus();
            return false;
        }
        // joiners validation
        if (!TextUtils.isEmpty(mLayoutJoiners.getEditText().getText())) {
            try {
                int joiners = Integer.parseInt(mLayoutJoiners.getEditText().getText().toString());
                if (MAX_JOINERS < joiners) {
                    mLayoutJoiners.setError(getString(R.string.error_guest_joiners));
                    mLayoutJoiners.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                mLayoutJoiners.setError(getString(R.string.error_generic_msg));
                return false;
            }
        }
        // name and phone validation
        return Utils.isValidRequiredField(mLayoutName, errorMessage)
                && Utils.isValidRequiredField(mLayoutPhone, errorMessage);

    }


    /**
     * write the guest to the db.
     * @param isExistingGuest true if the guest is an existing guest
     *                        and the user update this guest, false if this
     *                        guest is a new guest.
     */
    private void updateDatabase(boolean isExistingGuest) {
        String name = mLayoutName.getEditText().getText().toString().trim();
        String phone = mLayoutPhone.getEditText().getText().toString();
        String email = mLayoutEmail.getEditText().getText().toString().trim();
        Integer priority = priorityMap.get(mLayoutPriority.getEditText().getText().toString());
        String joiners = mLayoutJoiners.getEditText().getText().toString();
        Boolean isArriving = mArrivingCheckBox.isChecked();


        // If the user is in edit mode
        if (isExistingGuest) {
            updateExistingGuest(name, phone, email, priority, joiners, isArriving);
        }
        // Else create a new guest record for this guest in the db.
        else {
            mRootRef.child(Constants.PATH_GUESTS)
                    .child(mDataId)
                    .push().setValue(new Guest(name, phone, priority, email, isArriving, false, joiners, false))
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(AddGuestActivity.this, R.string.guest_saved_success, Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(AddGuestActivity.this, R.string.error_saving_record, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            });
        }

    }

    /**
     * update existing guest fields
     * @param name guest name
     * @param phone guest phone
     * @param email guest email
     * @param priority guest priority
     * @param joiners guest joiners
     * @param isArriving true if the guest arrive to the wedding, false otherwise
     */
    private void updateExistingGuest(String name, String phone, String email, Integer priority, String joiners, Boolean isArriving) {
        String guestKey = mIntent.getStringExtra(GuestsFragment.EXTRA_GUEST_KEY);

        DatabaseReference editedGuestRef = mRootRef
                .child(Constants.PATH_GUESTS)
                .child(mDataId)
                .child(guestKey);

        Map<String, Object> updatedFields = new HashMap<>();
        // update only fields that was changed (indicated by changeInState boolean array)
        for (int i = 0; i < STATE_SIZE; i++) {
            if (changeInState[i]) {
                switch (i) {
                    case NAME:
                        updatedFields.put("/" + Constants.PATH_GUEST_NAME, name);
                        break;
                    case PRIORITY:
                        updatedFields.put("/" + Constants.PATH_GUEST_PRIORITY, priority);
                        break;
                    case JOINERS:
                        updatedFields.put("/" + Constants.PATH_GUEST_JOINERS, joiners);
                        break;
                    case EMAIL:
                        updatedFields.put("/" + Constants.PATH_GUEST_EMAIL, email);
                        break;
                    case PHONE:
                        updatedFields.put("/" + Constants.PATH_GUEST_PHONE, phone);
                        break;
                }
            }
        }
        // If the state of the arrival checkbox changed then update this value in the db
        if (initialArrivingState != isArriving)
            updatedFields.put("/" + Constants.PATH_GUEST_ARRIVE, isArriving);

        editedGuestRef.updateChildren(updatedFields).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(AddGuestActivity.this, R.string.guest_saved_success, Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(AddGuestActivity.this, R.string.error_saving_record, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getPermissionToReadUserContacts() {
        List<String> permissions = new ArrayList<>();
        // 1) Use the support library version ContextCompat.checkSelfPermission(...) to avoid
        // checking the build version since Context.checkSelfPermission(...) is only available
        // in Marshmallow
        // 2) Always check for permission (even if permission has already been granted)
        // since the user can revoke permissions at any time through Settings
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_CONTACTS);

            // The permission is NOT already granted.
            // Check if the user has been asked about this permission already and denied
            // it. If so, we want to give more explanation about why the permission is needed.
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_CONTACTS)) {
                // Show our own UI to explain to the user why we need to read the contacts
                // before actually requesting the permission and showing the default UI
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS);
        }

        // Fire off an async request to actually get the permission
        // This will show the standard permission request dialog UI
        if (0 < permissions.size())
            requestPermissions(permissions.toArray(new String[permissions.size()]),
                    READ_CONTACTS_PERMISSIONS_REQUEST);
    }

    // Callback with the request from calling requestPermissions(...)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Make sure it's our original READ_CONTACTS request
        if (requestCode == READ_CONTACTS_PERMISSIONS_REQUEST) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.READ_CONTACTS)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
                        mAddFromContactsButton.setVisibility(View.VISIBLE);
                    else
                        mAddFromContactsButton.setVisibility(View.GONE);
                } else if (permissions[i].equals(Manifest.permission.SEND_SMS)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
                        // TODO
                        Toast.makeText(AddGuestActivity.this, "SMS permission accepted", Toast.LENGTH_SHORT).show();
                    else
                        // TODO
                        Toast.makeText(AddGuestActivity.this, "SMS permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


}
