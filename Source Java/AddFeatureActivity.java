package com.example.easywedding;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.ConfigurationCompat;


import android.content.Intent;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.easywedding.Utils.Utils;

import com.example.easywedding.model.Feature;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.math.BigDecimal;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Currency;

import java.util.Locale;


public class AddFeatureActivity extends AppCompatActivity {
    // edit text layouts
    private TextInputLayout mLayoutFeatureName;
    private TextInputLayout mLayoutPaymentBalance;
    private TextInputLayout mLayoutAdvancePayment;
    private TextInputLayout mLayoutSupplierEmail;
    private TextInputLayout mLayoutSupplierName;
    private TextInputLayout mLayoutSupplierPhone;
    private TextInputLayout mLayoutQuantity;
    private TextInputLayout mLayoutFreeText;
    // edit texts
    private EditText mSupplierNameEditText;
    private EditText mSupplierPhoneEditText;
    private EditText mSupplierEmailEditText;
    private EditText mQuantityEditText;
    private EditText mFreeTextEditText;
    private EditText mFeatureNameEditText;
    private EditText mPaymentBalanceEditText;
    private EditText mAdvancePaymentEditText;
    // For the currency of the advance payment and payment balance
    private Locale mLocal;
    private String mCurrencySymbol;

    private Intent mIntent;

    // For handling focus and soft keyboard events
    // when we start and finish the activity.
    private LinearLayout mDummyLayout;
    // db
    private DatabaseReference mRootRef;
    private FirebaseAuth mFirebaseAuth;
    private String mDataId;
    // Starting index to fetch the payments fields values (without the currency symbol).
    private final static int SKIPPED_CURRENCY_SYMBOL_INDEX = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_features);

        // Enable the up button.
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayHomeAsUpEnabled(true);


        setFields();

        handleLocaleSensitiveInputFields();
        // If the user is adding a new feature (not editing existing one)
        // then set the title accordingly.
        if (!mIntent.hasExtra(FeaturesFragment.EXTRA_FEATURE)) {
            setTitle(R.string.title_add_feature);
            // Else the user is editing an existing feature, so prepare
            // the feature data and set the title to the feature's name.
        } else {
            setForEditExistingFeature(mIntent);
        }

        setTextWatchers();

        takeFocusFromInputFields();

        setEndIconsInvisibleOnStartup();


    }

    /**
     * Set the end icons "clear text" to invisible when the user first
     * enter to this activity.
     */
    private void setEndIconsInvisibleOnStartup() {
        mLayoutPaymentBalance.setEndIconVisible(false);
        mLayoutAdvancePayment.setEndIconVisible(false);
        mLayoutSupplierPhone.setEndIconVisible(false);
        mLayoutSupplierName.setEndIconVisible(false);
        mLayoutSupplierEmail.setEndIconVisible(false);
        mLayoutQuantity.setEndIconVisible(false);
        mLayoutFreeText.setEndIconVisible(false);
    }

    /**
     * The user is editing an existing feature so set the input fields with the relevant information.
     *
     * @param intent the intent we got from FeaturesFragment.
     */
    private void setForEditExistingFeature(Intent intent) {
        Feature feature = (Feature) intent.getParcelableExtra(FeaturesFragment.EXTRA_FEATURE);
        if (feature != null) {
            setTitle(feature.getName());
            mFeatureNameEditText.setText(feature.getName());
            mQuantityEditText.setText(feature.getQuantity());
            mFreeTextEditText.setText(feature.getFreeText());
            mSupplierNameEditText.setText(feature.getSupplierName());
            mSupplierEmailEditText.setText(feature.getSupplierEmail());
            mSupplierPhoneEditText.setText(feature.getSupplierPhone());
            // Don't output the locale currency symbol (since TextInputLayout will do this.
            if (!TextUtils.isEmpty(feature.getPaymentBalance()))
                mPaymentBalanceEditText.setText(feature.getPaymentBalance().substring(SKIPPED_CURRENCY_SYMBOL_INDEX));
            if (!TextUtils.isEmpty(feature.getAdvancePayment()))
                mAdvancePaymentEditText.setText(feature.getAdvancePayment().substring(SKIPPED_CURRENCY_SYMBOL_INDEX));

        }

    }

    /**
     * listen to changes in the text of these {@link EditText}
     */
    private void setTextWatchers() {
        mFeatureNameEditText.addTextChangedListener(new CustomTextWatcher(mLayoutFeatureName));
        mSupplierEmailEditText.addTextChangedListener(new CustomTextWatcher(mLayoutSupplierEmail));
        mSupplierNameEditText.addTextChangedListener(new CustomTextWatcher(mLayoutSupplierName));
        mPaymentBalanceEditText.addTextChangedListener(new NumberTextWatcher(mPaymentBalanceEditText));
        mAdvancePaymentEditText.addTextChangedListener(new NumberTextWatcher(mAdvancePaymentEditText));
    }

    /**
     * When the user first enter to this activity, this dummy view will
     * take the focus from the {@link EditText} fields
     */
    private void takeFocusFromInputFields() {
        mDummyLayout.setFocusable(true);
        mDummyLayout.setFocusableInTouchMode(true);
        mDummyLayout.requestFocus();
    }

    /**
     * handle the case where the user's layout is RTL
     * and there exists input fields that force the user write from left to right.
     * For example, phone number and email input field.
     *
     */
    private void handleLocaleSensitiveInputFields() {
        // If the layout direction is from right to left then change the text direction
        // to be from left to right since the user enters numbers
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            mLayoutPaymentBalance.setTextDirection(TextInputLayout.TEXT_DIRECTION_LTR);
            mLayoutAdvancePayment.setTextDirection(TextInputLayout.TEXT_DIRECTION_LTR);
            // Show the locale currency symbol from the left of the input
            // in a RTL layout (and not from the start as usual).
            mLayoutPaymentBalance.setSuffixText(" " + mCurrencySymbol);
            mLayoutAdvancePayment.setSuffixText(" " + mCurrencySymbol);
            // start writing the phone number from the right (in a RTL layout)
            // I guess android change this automatically to LTR so the alignment of the text
            // is to the end, which is the start of the RTL layout
            mSupplierPhoneEditText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

        } else {
            mLayoutPaymentBalance.setPrefixText(" " + mCurrencySymbol);
            mLayoutAdvancePayment.setPrefixText(" " + mCurrencySymbol);
        }

    }

    /**
     * Initialize this activity attributes
     */
    private void setFields() {
        mLayoutFeatureName = findViewById(R.id.text_layout_feature_name);
        mLayoutSupplierName = findViewById(R.id.text_layout_supplier_name);
        mLayoutSupplierEmail = findViewById(R.id.text_layout_email);
        mSupplierEmailEditText = mLayoutSupplierEmail.getEditText();
        mLayoutPaymentBalance = findViewById(R.id.text_layout_payment_balance);
        mLayoutAdvancePayment = findViewById(R.id.text_layout_advance_payment);
        mLayoutSupplierPhone = findViewById(R.id.text_layout_phone_number);
        mSupplierPhoneEditText = mLayoutSupplierPhone.getEditText();
        mLocal = ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0);
        mCurrencySymbol = Currency.getInstance(mLocal).getSymbol();
        mLocal = ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0);
        mCurrencySymbol = Currency.getInstance(mLocal).getSymbol();
        mDummyLayout = findViewById(R.id.dummy_layout);
        mFeatureNameEditText = mLayoutFeatureName.getEditText();
        mSupplierEmailEditText = mLayoutSupplierEmail.getEditText();
        mSupplierNameEditText = mLayoutSupplierName.getEditText();
        mPaymentBalanceEditText = mLayoutPaymentBalance.getEditText();
        mAdvancePaymentEditText = mLayoutAdvancePayment.getEditText();
        mLayoutQuantity = findViewById(R.id.layout_feature_amount);
        mLayoutFreeText = findViewById(R.id.text_layout_feature_free_text);
        mQuantityEditText = mLayoutQuantity.getEditText();
        mFreeTextEditText = mLayoutFreeText.getEditText();

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mIntent = getIntent();

        if (mIntent.hasExtra(Constants.FRAGMENT_DATA_ID_ARG))
            mDataId = mIntent.getStringExtra(Constants.FRAGMENT_DATA_ID_ARG);
    }

    /**
     *
     * @param menu the menu of this activity
     * @return true indicates that this method succeeds inflating the menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.features_activity_menu, menu);
        return true;
    }

    /**
     *
     * @param item the current menu item that the user clicked on
     * @return true if the behavior was as expected
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save_feature:
                // Release focus from text layouts.
                mDummyLayout.requestFocus();
                // If the data that the user entered is valid then
                // close the soft keyboard and release focus from all the text layout.
                if (isDataValid()) {

                    Utils.hideKeyboardInActivity(this);

                    mDummyLayout.requestFocus();
                    // This will write to the db
                    updateDatabase(mIntent.hasExtra(FeaturesFragment.EXTRA_FEATURE_KEY));

                    Toast.makeText(this, R.string.success_feature_saved, Toast.LENGTH_SHORT).show();
                    // Go back to the last fragment (on the back stack))
                    onBackPressed();
                }

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Write the feature to the db
     * @param isEditExistingFeature true if the user edit an existing feature, false otherwise
     */
    private void updateDatabase(boolean isEditExistingFeature) {
        String featureName = mFeatureNameEditText.getText().toString();
        String paymentBalance = mPaymentBalanceEditText.getText().toString();
        String advancePayment = mAdvancePaymentEditText.getText().toString();
        String quantity = mQuantityEditText.getText().toString();
        String freeText = mFreeTextEditText.getText().toString();
        String supplierEmail = mSupplierEmailEditText.getText().toString();
        String supplierName = mSupplierNameEditText.getText().toString();
        String supplierPhone = mSupplierPhoneEditText.getText().toString();

        // Concat the local currency symbol with the payments
        if (!TextUtils.isEmpty(paymentBalance) && !paymentBalance.equals("0"))
            paymentBalance = mCurrencySymbol + paymentBalance;
        else
            paymentBalance = "";

        if (!TextUtils.isEmpty(advancePayment))
            advancePayment = mCurrencySymbol + advancePayment;


        Feature feature = new Feature(
                featureName,
                freeText,
                advancePayment,
                paymentBalance,
                quantity,
                supplierName,
                supplierEmail,
                supplierPhone);

        String key;

        // If we in edit mode then we will fetch the key of the current user
        // override the existing values of this feature in the db
        if (isEditExistingFeature) {
            key = mIntent.getStringExtra(FeaturesFragment.EXTRA_FEATURE_KEY);
            // Else this is a new feature so "push" a new key to the db and use this
            // as the new feature's key
        }
        else {
            key = mRootRef.child(Constants.PATH_FEATURES)
                    .child(mDataId)
                    .push().getKey();
        }
        // Write the new feature record to the db
        mRootRef.child(Constants.PATH_FEATURES)
                .child(mDataId)
                .child(key)
                .setValue(feature);

    }

    /**
     * Data validation to the feature form
     * @return true if the data is valid , false otherwise
     */
    private boolean isDataValid() {
        String featureName = mFeatureNameEditText.getText().toString();
        String supplierEmail = mSupplierEmailEditText.getText().toString();

        // Feature name field can't be empty, therefore if this field is empty show an error.
        if (TextUtils.isEmpty(featureName)) {
            mLayoutFeatureName.setError(getString(R.string.error_required_field_empty));
            mLayoutFeatureName.requestFocus();

            return false;
        }

        // If the email string is not empty and
        // pattern doesn't match a valid email pattern
        // then show an error.
        if (!TextUtils.isEmpty(supplierEmail) && !Utils.isValidEmail(supplierEmail)) {
            mLayoutSupplierEmail.setError(getString(R.string.error_chat_email_input));
            mLayoutSupplierEmail.requestFocus();

            return false;
        }
        // If the user provided a phone or an email for the supplier
        // then he must provide a name for the supplier
        if ((!TextUtils.isEmpty(mSupplierPhoneEditText.getText())
                || !TextUtils.isEmpty(mSupplierEmailEditText.getText()))
                && TextUtils.isEmpty(mSupplierNameEditText.getText())) {
            mLayoutSupplierName.setError(getString(R.string.error_supplier_name));
            mLayoutSupplierName.requestFocus();

            return false;

        } else {
            mLayoutSupplierName.setError("");
        }

        return true;

    }


    /**
     * Format payment fields, adding thousands separators and performing other
     * validations
     *
     */

    private class NumberTextWatcher implements TextWatcher {

        private DecimalFormat df;
        private DecimalFormat dfnd;
        private boolean hasFractionalPart;

        private EditText et;
        // Format pattern
        public NumberTextWatcher(EditText et) {
            df = new DecimalFormat("#,###.##");
            df.setDecimalSeparatorAlwaysShown(true);
            dfnd = new DecimalFormat("#,###");
            this.et = et;
            hasFractionalPart = false;
        }

        @SuppressWarnings("unused")
        private static final String TAG = "NumberTextWatcher";

        @Override
        public void afterTextChanged(Editable s) {
            et.removeTextChangedListener(this);

            try {
                int inilen, endlen;
                inilen = et.getText().length();

                String v = s.toString().replace(String.valueOf(df.getDecimalFormatSymbols().getGroupingSeparator()), "");
                Number n = df.parse(v);
                int cp = et.getSelectionStart();
                if (hasFractionalPart) {
                    et.setText(df.format(n));
                } else {
                    et.setText(dfnd.format(n));
                }
                endlen = et.getText().length();
                int sel = (cp + (endlen - inilen));
                if (sel > 0 && sel <= et.getText().length()) {
                    et.setSelection(sel);
                } else {
                    // place cursor at the end?
                    et.setSelection(et.getText().length() - 1);
                }
            } catch (NumberFormatException | ParseException nfe) {
                // We'll get here if the edittext's text is empty and that's ok.
                if (!TextUtils.isEmpty(et.getText())) {
                    nfe.printStackTrace();
                    Toast.makeText(AddFeatureActivity.this, "An error occurred, check your input", Toast.LENGTH_LONG).show();
                }

            }

            et.addTextChangedListener(this);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.toString().contains(String.valueOf(df.getDecimalFormatSymbols().getDecimalSeparator()))) {
                hasFractionalPart = true;
            } else {
                hasFractionalPart = false;
            }
        }

    }
}
