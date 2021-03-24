package com.example.easywedding;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.easywedding.model.Feature;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.ObservableSnapshotArray;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


public class FeaturesFragment extends Fragment {
    //db fields
    private DatabaseReference mRootRef;
    private Query mFeaturesReference;
    private FirebaseAuth mAuth;
    private String mDataId;
    private Intent mIntent;
    private View mFragmentLayout;
    private FirebaseRecyclerAdapter<Feature, FeatureViewHolder> mAdapter;
    private FirebaseRecyclerOptions<Feature> mOptions;
    private String mQueryOrderByValue;
    // fab
    private FloatingActionButton mFabAddFeature;
    // list fields
    private LinearLayoutManager mLinearLayoutManager;
    private RecyclerView mRecyclerView;

    // Keys for passing data to AddFeatureActivity
    public static final String EXTRA_FEATURE = "com.example.easywedding.FEATURE";
    public static final String EXTRA_FEATURE_KEY = "com.example.easywedding.FEATURE_KEY";

    public static final int SORT_DIALOG_ALPHABETICALLY = 0;
    public static final int SORT_DIALOG_BALANCE = 1;
    public static final int SORT_DIALOG_SUPPLIER = 2;

    public static final String TAG = FeaturesFragment.class.getSimpleName();

    public FeaturesFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
    // fetch the relevant data from MainActivity
        // such as the data id of the current user and the query he used last time
        if (bundle != null) {
            if (bundle.containsKey(Constants.FRAGMENT_DATA_ID_ARG))
                mDataId = bundle.getString(Constants.FRAGMENT_DATA_ID_ARG);
            if (bundle.containsKey(Constants.FEATURES_SORT_KEY))
                mQueryOrderByValue = bundle.getString(Constants.FEATURES_SORT_KEY);
        }
        // indicate that this fragment have an options menu
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mFeaturesReference = mRootRef.child(Constants.PATH_FEATURES).child(mDataId);

        mFragmentLayout = inflater.inflate(R.layout.fragment_features, container, false);

        mFabAddFeature = mFragmentLayout.findViewById(R.id.fab_features);

        mFabAddFeature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddFeatureActivity.class);
                intent.putExtra(Constants.FRAGMENT_DATA_ID_ARG, mDataId);
                startActivity(intent);
            }
        });


        mLinearLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView = mFragmentLayout.findViewById(R.id.recyclerview_features);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setHasFixedSize(true);


        setAdapter();

        return mFragmentLayout;
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.features_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort_feature:
                showSortOptionsDialog();
                return true;

            case R.id.action_export_features:
                exportFeatures();
                return true;
            case R.id.action_delete_by_supplier:
                deleteBySupplier();
                return true;

            case R.id.action_delete_all_features:
                deleteAllFeatures();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Delete features of a specific supplier. The key is the supplier name.
     */
    private void deleteBySupplier() {
        HashSet<String> names = new HashSet<>();
        for (Feature feature : mAdapter.getSnapshots()) {
            if (!TextUtils.isEmpty(feature.getSupplierName()))
                names.add(feature.getSupplierName());
        }

        final String[] namesArray = names.toArray(new String[0]);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.delete_sup_by_name)
                .setItems(namesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String supplierToDelete = namesArray[which];
                        Map<String, Object> updatedFields = new HashMap<>();
                        ObservableSnapshotArray<Feature> features = mAdapter.getSnapshots();
                        // delete the features of the given supplier
                        for (int i = 0; i < features.size(); i++){
                            if (features.get(i).getSupplierName().equals(supplierToDelete)){
                                updatedFields.put("/" + Constants.PATH_FEATURES +
                                        "/" + mDataId +
                                        "/" + mAdapter.getRef(i).getKey(), null);
                            }
                        }
                        // Write to database the updates.
                        mRootRef.updateChildren(updatedFields, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                if (error == null) {
                                    Toast.makeText(getContext(), R.string.success_deleted_features, Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    Toast.makeText(getContext(), R.string.error_generic_msg, Toast.LENGTH_SHORT).show();
                                    Log.v(TAG, error.toException().toString());
                                }
                            }

                        });
                    }
                }).create().show();



    }

    /**
     * Delete all the features in the db. Show the user a dialog first
     */
    private void deleteAllFeatures() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.delete_all_features)
                .setMessage(R.string.confirm_delete_all_features)
                .setPositiveButton(R.string.dialog_positive_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mRootRef.child(Constants.PATH_FEATURES)
                                .child(mDataId)
                                .removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }).setNegativeButton(R.string.dialog_negative_no, null)
                .create().show();
    }

    /**
     * export all the features as plain text
     */
    private void exportFeatures() {
        StringBuilder sb = new StringBuilder(getString(R.string.tab_features_title) + ":\n\n");
        String line = "--------------------\n";
        String[] headers = getResources().getStringArray(R.array.features_export_headers);

        for (Feature feature : mAdapter.getSnapshots()){
            sb.append(line);
            // Fetch the fields of the feature into an array
            String[] featureData = feature.arrayValues();
            for (int i = 0; i < featureData.length; i++){
                sb.append(headers[i]).append(" : ").append(featureData[i]).append("\n");
            }
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, sb.toString());
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.title_export_features)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getActivity(), R.string.error_generic_msg, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show the user the possible ways that he can sort the features list
     */
    private void showSortOptionsDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(R.string.action_sort_by)
                .setItems(R.array.dialog_sort_features, new DialogInterface.OnClickListener() {

                    String choice;
                    FirebaseRecyclerOptions<Feature> newOptions;
                    Query newFeaturesReference = mRootRef.child(Constants.PATH_FEATURES)
                            .child(mDataId);

                    public void onClick(DialogInterface dialog, int which) {

                        switch (which) {
                            case SORT_DIALOG_ALPHABETICALLY:
                                newOptions = new FirebaseRecyclerOptions.Builder<Feature>()
                                        .setQuery(newFeaturesReference.orderByChild(choice = Constants.PATH_GUEST_NAME), Feature.class)
                                        .build();
                                // If we the user previously sorted by balance
                                // then the layout is reversed so we need to reverse it again

                                mLinearLayoutManager.setReverseLayout(false);
                                mLinearLayoutManager.setStackFromEnd(false);
                                break;


                            case SORT_DIALOG_BALANCE:
                                newOptions = new FirebaseRecyclerOptions.Builder<Feature>()
                                        .setQuery(newFeaturesReference.orderByChild(choice = Constants.PATH_PAYMENT_BALANCE), Feature.class)
                                        .build();
                                // Symmetric explanation to the explanation above

                                mLinearLayoutManager.setReverseLayout(true);
                                mLinearLayoutManager.setStackFromEnd(true);
                                break;

                            case  SORT_DIALOG_SUPPLIER:
                                newOptions = new FirebaseRecyclerOptions.Builder<Feature>()
                                        .setQuery(newFeaturesReference.orderByChild(choice = Constants.PATH_SUPPLIER_NAME), Feature.class)
                                        .build();
                                mLinearLayoutManager.setReverseLayout(false);
                                mLinearLayoutManager.setStackFromEnd(false);
                                break;

                        }
                        // Save the sort preference in the user's phone
                        SharedPreferences sharedPref = ((MainActivity) getActivity()).getPreferences(Context.MODE_PRIVATE);
                        sharedPref.edit().putString(Constants.FEATURES_SORT_KEY, choice).apply();
                        // If for some reason we'll quit and resume then in on start
                        // we will use the new query value
                        if (choice != null)
                            mQueryOrderByValue = choice;
                        // This will refresh the list with the sort preference.
                        if (newOptions != null) {
                            mAdapter.updateOptions(newOptions);
                            // TODO I don't think this is necessary
                            mOptions = newOptions;
                        }
                        if (newFeaturesReference != null)
                            mFeaturesReference = newFeaturesReference;
                    }

                });
        builder.create().show();
    }

    /**
     * Here we are attaching the {@link FirebaseRecyclerAdapter} to the {@link RecyclerView}
     */
    private void setAdapter() {
        // If the user had previously set a sorting preference
        if (!TextUtils.isEmpty(mQueryOrderByValue)) {
            mFeaturesReference = mFeaturesReference.orderByChild(mQueryOrderByValue);
           // If the sorting is by positive balances then we need to reverse the layout
            // since in firebase, string are sorted in ascending order.
            if (mQueryOrderByValue.equals(Constants.PATH_PAYMENT_BALANCE)) {
                mLinearLayoutManager.setReverseLayout(true);
                mLinearLayoutManager.setStackFromEnd(true);
            }
        }
        mOptions = new FirebaseRecyclerOptions.Builder<Feature>()
                .setQuery(mFeaturesReference, Feature.class)
                .build();

        mAdapter = new FirebaseRecyclerAdapter<Feature, FeaturesFragment.FeatureViewHolder>(mOptions) {
            @Override
            protected void onBindViewHolder(@NonNull FeaturesFragment.FeatureViewHolder holder, final int position, @NonNull final Feature model) {
                // handle how to populate the views in the list item according to
                // the data contained in the model.
                final String featureKey = getRef(position).getKey();

                holder.supplierName.setText(model.getSupplierName());
                holder.featureName.setText(model.getName());
                holder.paymentBalance.setText(model.getPaymentBalance());

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Pass AddFeatureActivity the model object, which relates to the current
                        // feature that the user pressed on in FeaturesFragment.
                        mIntent = new Intent(getContext(), AddFeatureActivity.class);
                        mIntent.putExtra(EXTRA_FEATURE, model);
                        mIntent.putExtra(EXTRA_FEATURE_KEY, featureKey);
                        mIntent.putExtra(Constants.FRAGMENT_DATA_ID_ARG, mDataId);
                        startActivity(mIntent);
                    }
                });

                holder.popupMenuButton.setOnClickListener(setPopupMenuClickListener(getRef(position).getKey(), model));
            }

            @NonNull
            @Override
            public FeaturesFragment.FeatureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // The ViewHolder will keep references to the views in the list item view.
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_features, parent, false);
                return new FeatureViewHolder(view);
            }

        };


        // Scroll to last item.
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int itemsCount = mAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (itemsCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mRecyclerView.setAdapter(mAdapter);
    }

    /**
     *
     * @param featureKey the key of the current {@link Feature}
     * @param model the data of the {@link Feature}
     * @return a {@link android.view.View.OnClickListener } to handle user clicks on a
     * {@link PopupMenu} that related to the current {@link Feature}
     */
    private View.OnClickListener setPopupMenuClickListener(final String featureKey, final Feature model) {

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                popupMenu.inflate(R.menu.features_popup_menu);
                // if gave permission to sms then set visible , else invisible
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    /**
                     *
                     * @param item the {@link PopupMenu} item that the user clicked on
                     * @return tre if the user clicked on an item, false otherwise
                     */
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        switch (item.getItemId()) {

                            case R.id.action_delete_feature:
                                deleteFeature(featureKey, model.getName());
                                return true;
                            case R.id.action_call_supplier:
                                callSupplier(model.getSupplierPhone());
                                return true;

                            default:
                                return false;
                        }
                    }
                });
                popupMenu.show();
            }
        };
    }

    /**
     * Call the {@link Feature's } supplier
     * @param supplierPhone the phone of the {@link Feature's } supplier
     */
    private void callSupplier(String supplierPhone) {
        // If there is no phone for this supplier then show a toast
        if (supplierPhone == null || TextUtils.isEmpty(supplierPhone)) {
            Toast.makeText(getContext(), R.string.error_no_supplier_phone, Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + supplierPhone.trim()));
            startActivity(intent);
        }
    }

    /**
     * Delete a given {@link Feature}
     * @param featureKey the key of the {@link Feature}
     * @param featureName the name of the {@link Feature}
     */
    private void deleteFeature(final String featureKey, String featureName) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(getString(R.string.dialog_delete_list_item) + " " + featureName + "?")
                .setPositiveButton(R.string.dialog_positive_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mRootRef.child(Constants.PATH_FEATURES)
                                .child(mDataId)
                                .child(featureKey)
                                .removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }).setNegativeButton(R.string.dialog_negative_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // intentionally left blank
            }
        }).create().show();
    }

    /**
     * This class represents an object that holds
     * the {@link View's} of a {@link Feature} item
     */
    public static class FeatureViewHolder extends RecyclerView.ViewHolder {

        TextView featureName, supplierName, paymentBalance;
        ImageButton popupMenuButton;

        public FeatureViewHolder(@NonNull View itemView) {
            super(itemView);

            featureName = itemView.findViewById(R.id.list_item_feature_name);
            supplierName = itemView.findViewById(R.id.list_item_supplier_name);
            paymentBalance = itemView.findViewById(R.id.list_item_balance);
            popupMenuButton = itemView.findViewById(R.id.features_item_popup);
        }
    }


    @Override
    public void onStart() {
        super.onStart();
       // Stop listen to changes in the features
        if (mAdapter != null)
            mAdapter.startListening();

    }


    @Override
    public void onStop() {
        super.onStop();
        //Stop listen to changes in the features
        if (mAdapter != null)
            mAdapter.stopListening();
    }


}