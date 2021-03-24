package com.example.easywedding.Utils;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.easywedding.R;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Utils {
/*
    public static String parseDate(String inputDateString, DateFormat inputDateFormat, DateFormat outputDateFormat) {
        if (inputDateString == null)
            return null;

        Date date = null;
        String outputDateString = null;
        try {
            date = inputDateFormat.parse(inputDateString);
            if (date != null)
                outputDateString = outputDateFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return outputDateString;
    }

 */

    /**
     * Hide soft keyboard in activity.
     * @param activity
     */
    public static void hideKeyboardInActivity(AppCompatActivity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    /**
     * @param email a string the user entered
     * @return true if the string is a valid email
     */
    public static boolean isValidEmail(CharSequence email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidRequiredField(@NonNull  TextInputLayout textInputLayout, String errorMessage){
        if (TextUtils.isEmpty(textInputLayout.getEditText().getText())) {
            textInputLayout.setError(errorMessage);
            textInputLayout.requestFocus();

            return false;
        }
        return true;
    }

    /**
     * @param phone the guest's phone.
     * @param  mPhones a list of guests phone numbers.
     * @return true if this guest already in the guests list, false otherwise.
     */
    public static boolean isDuplicateGuest(String phone, ArrayList<String> mPhones) {

            return mPhones != null && mPhones.contains(phone);

        }

    }

