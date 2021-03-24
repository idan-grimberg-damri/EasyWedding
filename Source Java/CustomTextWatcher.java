package com.example.easywedding;

import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.material.textfield.TextInputLayout;

/**
 * Represent an object that listen to change in text in {@link android.widget.EditText} fields
 * in the {@link com.example.easywedding.model.Guest} and {@link com.example.easywedding.model.Feature} forms
 */
public class CustomTextWatcher implements TextWatcher {

    private TextInputLayout mTextInputLayout;
    // Array of booleans, each index represents a truth value for
    // the change in some guest input field in the guest form
    private boolean[] changeInState;
    // The index were the changed happened
    private int mChangedDataIndex;

    public CustomTextWatcher(TextInputLayout textInputLayout,boolean[] changeInState, int mChangedDataIndex){
        this.mTextInputLayout = textInputLayout;
        this.changeInState = changeInState;
        this.mChangedDataIndex = mChangedDataIndex;
    }

    public CustomTextWatcher(TextInputLayout textInputLayout){
        this.mTextInputLayout = textInputLayout;

    }

    /**
     * set the error in the {@link TextInputLayout} to the empty string when the user
     * start to write
     * @param s
     * @param start
     * @param count
     * @param after
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        mTextInputLayout.setError("");
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    /**
     * If the user wrote in a {@link android.widget.EditText} field
     * of an existing guest then we mark the state in the boolean array as changed
     * @param s
     */
    @Override
    public void afterTextChanged(Editable s) {
        if (changeInState != null)
            changeInState[mChangedDataIndex] = true;
    }
}
