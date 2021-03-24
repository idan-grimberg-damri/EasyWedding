package com.example.easywedding;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
Implementation of view pager adapter that uses a Fragment to manage each page.
This class also handles saving and restoring of fragment's state.
 */
public class ViewPagerFragmentAdapter extends FragmentStateAdapter {

    private String mDataId, mSortQueryFeatures, mSortQueryGuests;
    private boolean mIsSortGuestByArriving;

    public ViewPagerFragmentAdapter(@NonNull FragmentManager fragmentManager
            , @NonNull Lifecycle lifecycle, String mDataId,
                                    String sortQueryFeatures, String sortQueryGuests, boolean isSortGuestByArriving){
        super(fragmentManager, lifecycle);

        this.mDataId = mDataId;
        this.mSortQueryFeatures = sortQueryFeatures;
        this.mSortQueryGuests = sortQueryGuests;
        this.mIsSortGuestByArriving= isSortGuestByArriving;

    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.FRAGMENT_DATA_ID_ARG, mDataId);
        bundle.putString(Constants.FEATURES_SORT_KEY, mSortQueryFeatures);
        bundle.putString(Constants.GUESTS_SORT_KEY, mSortQueryGuests);
        bundle.putBoolean(Constants.GUEST_ARRIVAL_SORT_BOOL_VAL, mIsSortGuestByArriving);
        // Create the fragment at position "position" and set arguments (pass data to the fragment)
        switch (position){
            case MainActivity.FEATURES_POSITION:
                FeaturesFragment featuresFragment = new FeaturesFragment();
                featuresFragment.setArguments(bundle);
                return featuresFragment;

            case MainActivity.GUESTS_POSITION:
                GuestsFragment guestsFragment = new GuestsFragment();
                guestsFragment.setArguments(bundle);
                return guestsFragment;

            case MainActivity.CHAT_POSITION:
                ChatFragment chatFragment =  new ChatFragment();
                chatFragment.setArguments(bundle);
                return chatFragment;

            default:
                return new Fragment();
        }
    }

    @Override
    public int getItemCount() {
        return MainActivity.FRAGMENTS_COUNT;
    }

}
