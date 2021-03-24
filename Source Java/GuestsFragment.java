package com.example.easywedding;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.easywedding.model.Guest;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class GuestsFragment extends Fragment {


    private View mFragmentLayout;
    private FloatingActionButton mFabAddGuest;

    private Intent mIntent;

    private DatabaseReference mRootRef;
    private FirebaseAuth mAuth;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseRecyclerAdapter<Guest, GuestsFragment.GuestViewHolder> mAdapter;
    private FirebaseRecyclerOptions<Guest> mOptions;
    private String mDataId;
    private String guestKeyActivityResult;
    private String mQueryOrderByValue;
    private boolean mIsSortByArriving;

    private String mSubtitle;

    public static final String EXTRA_GUEST = "com.example.easywedding.GUEST";
    public static final String EXTRA_GUEST_KEY = "com.example.easywedding.GUEST_KEY";
    public static final int RC_SHARED_CONFIRM = 2;

    public static final int SORT_DIALOG_ALPHABETICALLY = 0;
    public static final int SORT_DIALOG_PRIORITY = 1;
    public static final int SORT_DIALOG_ARRIVING = 2;
    public static final int SORT_DIALOG_NOT_ARRIVING = 3;

    public static final String TAG = GuestsFragment.class.getSimpleName();

    private Query mGuestsReference;
    private OnGuestArrivingCountPass mArrivingCountPasser;


    public GuestsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        // fetch relevant data from the main activity.
        // such as the data id of the user, his last sort choice and the direction of the sort
        // for the arriving guests field
        if (bundle != null && bundle.containsKey(Constants.FRAGMENT_DATA_ID_ARG)) {
            mDataId = bundle.getString(Constants.FRAGMENT_DATA_ID_ARG);
        }

        if (bundle.containsKey(Constants.GUESTS_SORT_KEY))
            mQueryOrderByValue = bundle.getString(Constants.GUESTS_SORT_KEY);

        if (bundle.containsKey(Constants.GUEST_ARRIVAL_SORT_BOOL_VAL))
            mIsSortByArriving = bundle.getBoolean(Constants.GUEST_ARRIVAL_SORT_BOOL_VAL);

        mSubtitle = getString(R.string.arriving);

        // this fragment has an options menu
        setHasOptionsMenu(true);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        mRootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mGuestsReference = mRootRef.child(Constants.PATH_GUESTS).child(mDataId);

        mFragmentLayout = inflater.inflate(R.layout.fragment_guests, container, false);

        mFabAddGuest = mFragmentLayout.findViewById(R.id.fab_guests);
        mFabAddGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddGuestActivity.class);
                intent.putExtra(Constants.FRAGMENT_DATA_ID_ARG, mDataId)
                        .putStringArrayListExtra(Constants.EXTRA_PHONE_LIST, getPhones());
                startActivity(intent);
            }
        });

        mLinearLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView = mFragmentLayout.findViewById(R.id.recyclerview_guests);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setHasFixedSize(true);

        setAdapter();

        return mFragmentLayout;
    }

    private ArrayList<String> getPhones() {
        ArrayList<String> phones = new ArrayList<>();
        for (Guest guest : mAdapter.getSnapshots()) {
            if (guest.getPhoneNumber() != null && !TextUtils.isEmpty(guest.getPhoneNumber()))
                phones.add(guest.getPhoneNumber().replaceAll("-", ""));
        }
        return phones;
    }

    /**
     * Attach the {@link FirebaseRecyclerAdapter} to the {@link RecyclerView}
     */
    private void setAdapter() {
        // If the user had previously set a sorting preference
        if (!TextUtils.isEmpty(mQueryOrderByValue)) {
            mGuestsReference = mGuestsReference.orderByChild(mQueryOrderByValue);
            if ((mQueryOrderByValue.equals(Constants.PATH_GUEST_ARRIVE)
                    && mIsSortByArriving) || mQueryOrderByValue.equals(Constants.PATH_GUEST_PRIORITY)) {
                mLinearLayoutManager.setReverseLayout(true);
                mLinearLayoutManager.setStackFromEnd(true);
            }
        }
        mOptions = new FirebaseRecyclerOptions.Builder<Guest>()
                .setQuery(mGuestsReference, Guest.class)
                .build();

        mAdapter = new FirebaseRecyclerAdapter<Guest, GuestsFragment.GuestViewHolder>(mOptions) {
            /**
             * When the guest list is changed then count the number of arriving guest.
             * and pass this information to the {@link MainActivity} via the {@link OnGuestArrivingCountPass} interface
             */
            @Override
            public void onDataChanged() {
                super.onDataChanged();

                mArrivingCountPasser.onGuestArrivingCountPass(countArriving());

            }

            /**
             *
             * @param holder holds the {@link View's} of the {@link Guest} item list
             * @param position the position of the {@link Guest} list item
             * @param model the data of the {@link Guest} is contained in this model variable
             */
            @Override
            protected void onBindViewHolder(@NonNull GuestsFragment.GuestViewHolder holder, final int position, @NonNull final Guest model) {

                final String guestKey = getRef(position).getKey();

                String name = model.getName();

                boolean isInvitationSent = model.isInvited();
                boolean isArriving = model.isArrive();

                if (name != null)
                    holder.guestName.setText(name);
                // if we already sent an invite to this guest
                // then set the relevant text message in the guest list item
                if (isInvitationSent) {
                    holder.invited.setVisibility(View.VISIBLE);
                    holder.invited.setText(R.string.confirmation_was_sent_text);
                } else {
                    holder.invited.setVisibility(View.GONE);
                }

                // if the guest is arriving then color the relevant icon with blue, else with gray
                if (isArriving)
                    holder.arrivingImageIndicator.setImageResource(R.drawable.vector_guest_check_outlined_24);

                else
                    holder.arrivingImageIndicator.setImageResource(R.drawable.ic_baseline_check_circle_outline_24);


                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Pass to AddGuestActivity the model object, which relates to the current
                        // guest that the user pressed on in GuestFragment.
                        mIntent = new Intent(getContext(), AddGuestActivity.class);
                        mIntent.putExtra(EXTRA_GUEST, model);
                        mIntent.putExtra(EXTRA_GUEST_KEY, guestKey);
                        mIntent.putExtra(Constants.FRAGMENT_DATA_ID_ARG, mDataId);
                        startActivity(mIntent);
                    }
                });

                holder.popupMenuButton.setOnClickListener(setPopupMenuClickListener(getRef(position).getKey(), model));


            }

            @NonNull
            @Override
            public GuestsFragment.GuestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // The ViewHolder will keep references to the views in the list item view.
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_guest, parent, false);
                return new GuestsFragment.GuestViewHolder(view);
            }

        };


        // Scroll to last item
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
     * @return the number of guests that arrive to the wedding
     */
    private int countArriving() {
        int count = 0;
        for (Guest guest : mAdapter.getSnapshots())
            if (guest.isArrive()) {
                count++;
                if (!TextUtils.isEmpty(guest.getJoiners())){
                    try{
                       int joiners = Integer.parseInt(guest.getJoiners());
                       count += joiners;
                    }catch (NumberFormatException e){
                        Log.v(TAG, e.toString());
                    }
                }
            }

        return count;
    }

    /**
     *
     * @param guestKey th ekey of the {@link Guest}
     * @param model containint the {@link Guest} data
     * @return an {@link android.view.View.OnClickListener}
     * When the user clicks on the {@link PopupMenu} icon in the {@link Guest} list item
     * then show a {@link PopupMenu}
     */
    private View.OnClickListener setPopupMenuClickListener(final String guestKey, final Guest model) {

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                popupMenu.inflate(R.menu.guests_popup_menu);
                // if gave permission to sms then set visible , else invisible
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_send_confirm_sms:
                                sendSMS(model.getPhoneNumber(),
                                        buildArriveConfirmationUrl(guestKey).toString(),
                                        guestKey, false);
                                return true;

                            case R.id.action_share_confirm_text:
                                guestKeyActivityResult = guestKey;
                                startFileChooser(guestKey);

                                return true;

                            case R.id.action_delete_item:
                                deleteGuest(guestKey, model.getName());

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
     *  let the user choose an app that can handle plain text input, for sharing his
     *  arrival confirmation link.
     * @param guestKey the key of the guests we want to invite
     */
    private void startFileChooser(String guestKey) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT,
                        getString(R.string.share_confirm_message_header) +
                                "\n" +
                                buildArriveConfirmationUrl(guestKey).toString());
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.title_share_confirm_text)), RC_SHARED_CONFIRM);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getActivity(), R.string.error_generic_msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteGuest(final String guestKey, String guestName) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(getString(R.string.dialog_delete_list_item) + " " + guestName + "?")
                .setPositiveButton(R.string.dialog_positive_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mRootRef.child(Constants.PATH_GUESTS)
                                .child(mDataId)
                                .child(guestKey)
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


    public static class GuestViewHolder extends RecyclerView.ViewHolder {

        TextView guestName, invited;
        ImageView arrivingImageIndicator;
        ImageButton popupMenuButton;


        public GuestViewHolder(@NonNull View itemView) {
            super(itemView);

            guestName = itemView.findViewById(R.id.list_item_guest_name);
            invited = itemView.findViewById(R.id.list_item_invite_text);
            arrivingImageIndicator = itemView.findViewById(R.id.list_item_arriving_image);
            popupMenuButton = itemView.findViewById(R.id.features_item_popup);

        }
    }



    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.guests_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send_confirmations:
                createMultiArrivalConfirmationDialog();
                return true;

            case R.id.action_sort__guest:
                showSortOptionsDialog();
                return true;

            case R.id.action_export_guests:
                exportGuests();
                return true;

            case R.id.action_delete_all_guests:
                deleteAllGuests();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }


    }

    /**
     * Show the available sort options in the {@link Guest} list
     */
    private void showSortOptionsDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(R.string.action_sort_by)
                .setItems(R.array.dialog_sort_guests, new DialogInterface.OnClickListener() {

                    String choice;
                    FirebaseRecyclerOptions<Guest> newOptions;
                    Query newGuestsReference = mRootRef.child(Constants.PATH_GUESTS)
                            .child(mDataId);

                    public void onClick(DialogInterface dialog, int which) {

                        switch (which) {
                            case SORT_DIALOG_ALPHABETICALLY:
                                newOptions = new FirebaseRecyclerOptions.Builder<Guest>()
                                        .setQuery(newGuestsReference.orderByChild(choice = Constants.PATH_GUEST_NAME), Guest.class)
                                        .build();

                                mLinearLayoutManager.setReverseLayout(false);
                                mLinearLayoutManager.setStackFromEnd(false);
                                break;


                            case SORT_DIALOG_PRIORITY:
                                newOptions = new FirebaseRecyclerOptions.Builder<Guest>()
                                        .setQuery(newGuestsReference.orderByChild(choice = Constants.PATH_GUEST_PRIORITY), Guest.class)
                                        .build();

                                mLinearLayoutManager.setReverseLayout(true);
                                mLinearLayoutManager.setStackFromEnd(true);
                                break;

                            case SORT_DIALOG_ARRIVING:

                                mIsSortByArriving = true;

                                newOptions = new FirebaseRecyclerOptions.Builder<Guest>()
                                        .setQuery(newGuestsReference.orderByChild(choice = Constants.PATH_GUEST_ARRIVE), Guest.class)
                                        .build();

                                ((MainActivity) getActivity()).getPreferences(Context.MODE_PRIVATE)
                                        .edit()
                                        .putBoolean(Constants.GUEST_ARRIVAL_SORT_BOOL_VAL, true)
                                        .apply();

                                mLinearLayoutManager.setReverseLayout(true);
                                mLinearLayoutManager.setStackFromEnd(true);

                                break;

                            case SORT_DIALOG_NOT_ARRIVING:
                                mIsSortByArriving = false;

                                newOptions = new FirebaseRecyclerOptions.Builder<Guest>()
                                        .setQuery(newGuestsReference.orderByChild(choice = Constants.PATH_GUEST_ARRIVE), Guest.class)
                                        .build();

                                ((MainActivity) getActivity()).getPreferences(Context.MODE_PRIVATE)
                                        .edit()
                                        .putBoolean(Constants.GUEST_ARRIVAL_SORT_BOOL_VAL, false)
                                        .apply();

                                mLinearLayoutManager.setReverseLayout(false);
                                mLinearLayoutManager.setStackFromEnd(false);

                                break;
                        }
                        // Save the sort preference in the user's phone
                        SharedPreferences sharedPref = ((MainActivity) getActivity()).getPreferences(Context.MODE_PRIVATE);
                        sharedPref.edit().putString(Constants.GUESTS_SORT_KEY, choice).apply();
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
                        if (newGuestsReference != null)
                            mGuestsReference = newGuestsReference;
                    }

                });
        builder.create().show();
    }

    /**
     * export all the guests as plain text
     */
    private void exportGuests() {
        StringBuilder sb = new StringBuilder(getString(R.string.tab_guests_title) + ":\n\n");
        String line = "--------------------\n";
        Map<Boolean, String> boolMap = new HashMap<>();
        boolMap.put(true, getString(R.string.dialog_positive_yes));
        boolMap.put(false, getString(R.string.dialog_negative_no));
        String name = getString(R.string.guest_name);
        String phoneNumber = getString(R.string.phone_number);
        String invited = getString(R.string.invitet);
        String arrive = getString(R.string.arrive);

        for (Guest guest : mAdapter.getSnapshots()) {
            sb.append(line);
            // Fetch the fields of the feature into an array
            sb.append(name).append(" : ").append(guest.getName()).append("\n")
                    .append(phoneNumber).append(" : ").append(guest.getPhoneNumber()).append("\n")
                    .append(invited).append(" : ").append(boolMap.get(guest.isInvited())).append("\n")
                    .append(arrive).append(" : ").append(boolMap.get(guest.isArrive())).append("\n");
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, sb.toString());
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.title_export_guests)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getActivity(), R.string.error_generic_msg, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Delete all the {@link Guest's} in the db
     */
    private void deleteAllGuests() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.delete_all_guests)
                .setMessage(R.string.confirm_delete_all_guests)
                .setPositiveButton(R.string.dialog_positive_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mRootRef.child(Constants.PATH_GUESTS)
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
     * Create arrival confirmation links {@link MaterialAlertDialogBuilder}
     * that alert the user that he's about to send  SMS message to all the guests
     * that were not invited to the wedding yet
     */
    private void createMultiArrivalConfirmationDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.dialog_title_send_multi_sms)
                .setMessage(R.string.dialog_body_send_multi_sms)
                .setPositiveButton(R.string.dialog_positive_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendMultiArrivalConfirmationsBySms();
                    }
                }).setNegativeButton(R.string.dialog_negative_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // intentionally left blank
            }
        }).create().show();
    }

    /**
     * send SMS message to all the guests
     *      * that were not invited to the wedding yet
     */
    private void sendMultiArrivalConfirmationsBySms() {
        //TODO handle this message header
        String messageHeader = getString(R.string.share_confirm_message_header);

        StringBuilder sb;

        for (int i = 0; i < mAdapter.getItemCount(); i++) {
            // Get the current guest in the guest list.
            Guest currGuest = mAdapter.getItem(i);
            // Get the key of the guest record in the db.
            String key = mAdapter.getRef(i).getKey();
            // If the guests doesn't exists and he hasn't been invited yet
            // and hasn't been confirmed arrival yet then invite him
            if (!(key == null || currGuest.isSubmitted() || currGuest.isArrive() || currGuest.isInvited())) {
                // The query param of the guest record in the db.
                sb = buildArriveConfirmationUrl(key);
                // Try to send SMS to the guest via his phone number.
                try {
                    sendSMS(currGuest.getPhoneNumber(), sb.toString(), key, true);

                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    /**
     * @return a stringBuilder that represents the fixed prefix url
     * that will be sent for arrival confirmation.
     */
    private StringBuilder buildArriveConfirmationUrl(String guestKey) {

        Uri.Builder builder = new Uri.Builder();
        builder.scheme(Constants.SCHEME)
                .authority(Constants.AUTHORITY)
                .appendPath(Constants.MAIN_PATH)
                .appendQueryParameter(Constants.USER_QUERY, mDataId)
                .appendQueryParameter(Constants.GUEST_QUERY, guestKey);

        return new StringBuilder(builder.build().toString());
    }


    @Override
    public void onStart() {
        super.onStart();
    //start listen to change in the guests
        if (mAdapter != null)
            mAdapter.startListening();


    }


    @Override
    public void onPause() {
        super.onPause();
        // Hide the number of arriving guests from the actionbar's subtitle.
        ((AppCompatActivity) getActivity()).getSupportActionBar()
                .setSubtitle("");
    }

    @Override
    public void onStop() {
        super.onStop();
       // stop listening to change in guests
        if (mAdapter != null)
            mAdapter.stopListening();

    }

    /**
     * send an SMS message to a given phone number and update the db that an invitation has been sent
     * to this phone number.
     *
     * @param phoneNumber the destination
     * @param message     SMS body
     * @param guestKey    the key to the guest record in the db
     */
    private void sendSMS(String phoneNumber, String message, final String guestKey, final boolean isMultiDestination) {
        final Context context = getActivity();
        if (context == null)
            return;

        String SENT = "SMS_SENT";

        PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
                new Intent(SENT), 0);

        // For knowing when the SMS has been sent
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                // If the SMS has been sent then write to the db that and invitation has been sent
                if (getResultCode() == Activity.RESULT_OK) {
                    mRootRef.child(Constants.PATH_GUESTS).child(mDataId)
                            .child(guestKey)
                            .child(Constants.PATH_GUEST_INVITED)
                            .setValue(true);
                    // If the sms is designated only to one guest show a toast for success.
                    if (!isMultiDestination)
                        Toast.makeText(context, R.string.success_sms_sent, Toast.LENGTH_SHORT).show();
                }
            }
        }, new IntentFilter(SENT));


        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // If we tried to share the confirmation link then ask the user if he eventually shared the link
        // ir not. IF he shared then write to the db that we sent the user an invite.
        if (requestCode == RC_SHARED_CONFIRM) {
            new MaterialAlertDialogBuilder(getContext()).setMessage(R.string.confirm_user_sent_invitation)
                    .setPositiveButton(R.string.dialog_positive_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // guestKeyActivityResult is the key of the guest item the user
                            // tried to share the link with.
                            if (guestKeyActivityResult != null) {
                                mRootRef.child(Constants.PATH_GUESTS)
                                        .child(mDataId)
                                        .child(guestKeyActivityResult)
                                        .child(Constants.PATH_GUEST_INVITED)
                                        .setValue(true);
                            }
                            guestKeyActivityResult = null;

                        }
                    }).setNegativeButton(R.string.dialog_negative_no, null)
                    .create()
                    .show();
        }

    }

    /**
     * When this fragment is attached to {@link MainActivity} then
     * assign to {@link OnGuestArrivingCountPass} the context (which implements the interface).
     * @param context the {@link MainActivity}
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

         mArrivingCountPasser = (OnGuestArrivingCountPass)context;
    }

    /**
     * This interface was created to enable safe communication with {@link MainActivity}.
     * When the data in the adapter is changed we update the number of arriving guests and their joiners.
     * This value will be passes to {@link MainActivity}.
     */
    public interface OnGuestArrivingCountPass {
        /**
         *
         * @param guestCount the number of arriving guests and their joiners.
         */
        void onGuestArrivingCountPass(int guestCount);
    }
}