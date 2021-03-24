package com.example.easywedding;



import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.InputFilter;

import android.text.TextUtils;
import android.text.TextWatcher;

import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;

import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;



import com.example.easywedding.model.Message;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;


public class ChatFragment extends Fragment {

    // guest list fields
    private RecyclerView mRecyclerView;
    private EditText mMessageEditText;
    private Button mSendMessageButton;
    private View mFragmentLayout;
    private LinearLayoutManager mLinearLayoutManager;
    // DB
    private DatabaseReference mRootRef;
    private FirebaseUser mFirebaseUser;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUserGroupChatReference;
    private FirebaseRecyclerAdapter<Message, MessageViewHolder> mAdapter;

    // Storage
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatFileAttachStorageRef;
    // Group data id (features, guests, chat and group)
    private String mDataId;
    // File chooser
    private ImageButton mAttachFile;

    // default length limit of text in an EditText object
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    // File chooser request code
    private static final int RC_PHOTO_PICKER = 7;

    public static final String TAG = ChatFragment.class.getSimpleName();

  //  private final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  //  private final SimpleDateFormat chatDateFormat = new SimpleDateFormat("dd-MM-yyy"
  //          , Locale.getDefault());
    private final SimpleDateFormat timeChatFormat = new SimpleDateFormat("HH:mm",
            Locale.getDefault());


    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        // Fetch from the main activity the data id of the current user
        if (bundle != null && bundle.containsKey(Constants.FRAGMENT_DATA_ID_ARG)) {
            mDataId = bundle.getString(Constants.FRAGMENT_DATA_ID_ARG);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        mFragmentLayout = inflater.inflate(R.layout.fragment_chat, container, false);

        mRecyclerView = mFragmentLayout.findViewById(R.id.recyclerview_chat);
        mMessageEditText = mFragmentLayout.findViewById(R.id.message_edittext);
        mSendMessageButton = mFragmentLayout.findViewById(R.id.send_message_button);

        mRootRef = FirebaseDatabase.getInstance().getReference();

        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mChatFileAttachStorageRef = mFirebaseStorage.getReference().child(mDataId);

        mAttachFile = mFragmentLayout.findViewById(R.id.chat_attach);
        setFileAttachListener();
        // here we'll set the adapter to the recycler view
        listenToMessagesInChat();
        // handle chat message and message deployment
        handleMessageEditText();
        handleMessageDeploy();


        return mFragmentLayout;
    }

    /**
     * Set the Image chooser intent.
     */
    private void setFileAttachListener() {
        mAttachFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });
    }

    /**
     * We stop the listening to the db here
     */
    @Override
    public void onStart() {
        super.onStart();

        if (mAdapter != null)
            mAdapter.startListening();

    }

    /**
     * Listen to new messages in the chat
     */
    private void listenToMessagesInChat() {
        // Get a reference to the user's chat.
        mUserGroupChatReference = mRootRef.child(Constants.PATH_CHATS)
                .child(mDataId);
        // here we set the chat db reference and give the POJO class for the message
        FirebaseRecyclerOptions<Message> options = new FirebaseRecyclerOptions.Builder<Message>()
                .setQuery(mUserGroupChatReference, Message.class)
                .build();

        mAdapter = new FirebaseRecyclerAdapter<Message, MessageViewHolder>(options) {
            /**
             * // Fetch the list item UI elements value from the model
             * @param holder holds the list item {@link View's}
             * @param position the position of the current item
             * @param model the values that will populated in the holder's {@link View's}
             */
            @Override
            protected void onBindViewHolder(@NonNull MessageViewHolder holder, int position, @NonNull Message model) {
                // Fetch the list item UI elements value from the model

                String text = model.getText();
                // A file, not text
                if (text == null){
                    holder.text.setVisibility(View.GONE);
                    holder.photoImageView.setVisibility(View.VISIBLE);
                    Picasso.get().load(model.getPhotoUrl()).into(holder.photoImageView);
                    holder.photoImageView.setOnClickListener(createPhotoClickListener(holder.photoImageView, model.getPhotoUrl()));
                }
                // A plain text message
                else{
                    holder.photoImageView.setVisibility(View.GONE);
                    holder.text.setVisibility(View.VISIBLE);
                    holder.text.setText(text);
                }

                // Parse the time
                Long timeMillis = model.getTime();
                if (timeMillis != null) {
                    Date date = new Date(timeMillis);
                    holder.time.setText(timeChatFormat.format(date));
                }

                String currentMessageSenderId = model.getSenderId();
                // Set the color of the message background.
                // the sender will see his messages in light blue .
                // the sender will see other messages in white
                if (currentMessageSenderId != null) {
                    // If this message belong to the sender  the se the sender background
                    if (currentMessageSenderId.equals(mFirebaseUser.getUid())) {
                        holder.name.setVisibility(View.GONE);
                        holder.itemView.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.sender_message_background));
                        // Else this message doesn't belong to the current user so display the name of the sender
                        // No need to display the current user name if he sent the message.
                    } else {
                        holder.name.setVisibility(View.VISIBLE);
                        holder.name.setText(model.getName());
                        holder.itemView.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.others_message_background));
                    }
                }

            }

            @NonNull
            @Override
            public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // The ViewHolder will keep references to the views in the list item view.
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_chat, parent, false);
                return new MessageViewHolder(view);
            }

        };
        mLinearLayoutManager = new LinearLayoutManager(getContext());
        mLinearLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        registerDataObserver();

        mAdapter.startListening();

    }

    /**
     * set a listener to alick event. When the user click on an image,  a dialog with the full photo that the user clicked on .
     * @param img the image the user clicked on
     * @param photoUrl the Url of the image in the cloud storage
     * @return the listener
     */
    private View.OnClickListener createPhotoClickListener(ImageView img, final String photoUrl) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageView imageView = (ImageView)LayoutInflater.from(getContext()).inflate(R.layout.full_photo_dialog, null);
                Picasso.get().load(photoUrl).into(imageView);
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
                builder.setView(imageView)
                        .setNeutralButton(R.string.dialog_dismiss, null)
                        .create()
                        .requestWindowFeature(Window.FEATURE_NO_TITLE);
                builder.show();

            }
        };
    }

    /**
     * This will scroll to the last item that was inserted
     */
    private void registerDataObserver() {

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
    }

    /**
     * Stop listen to chat messages here
     */
    @Override
    public void onStop() {
        super.onStop();

        if (mAdapter != null)
            mAdapter.stopListening();


    }

    /**
     * Represents an object that holds the {@link View's} of a specific {@link Message} list item
     */
    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        TextView name, time, text;
        ImageView photoImageView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.name_textview);
            time = itemView.findViewById(R.id.time_textview);
            text = itemView.findViewById(R.id.message_textview);
            photoImageView = itemView.findViewById(R.id.chat_photo_message);
        }
    }

    /**
     * Handle message deployment, Write the message to the db with the current time
     */
    private void handleMessageDeploy() {
        // Click on the send button sends a message and clears the EditText
        mSendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the current date and time with the default local of the user
                // TODO use Local and parsing
                Calendar calendar = Calendar.getInstance();
                Date currentDate = new Date();
                DateFormat dateFormat = SimpleDateFormat.getDateInstance();
                String sDate = dateFormat.format(calendar.getTime());
                dateFormat = SimpleDateFormat.getTimeInstance();
                String sTime = dateFormat.format(calendar.getTime());

                String messageBody = mMessageEditText.getText().toString();

                if (!TextUtils.isEmpty(messageBody) && messageBody.startsWith("\n")) {
                    // Trim starting whitespaces
                    messageBody = new StringBuilder(messageBody)
                            .reverse().toString().trim();
                    messageBody = new StringBuilder(messageBody).reverse().toString();
                }
                // Create a Message POJO for serialization
                Message message = new Message(
                        messageBody,
                        mFirebaseUser.getDisplayName(),
                        System.currentTimeMillis(),
                        mFirebaseUser.getUid(), null);


                // Add a new message record to the user's chat
                mUserGroupChatReference.push().setValue(message);
                // Clear input box
                mMessageEditText.setText("");
            }
        });
    }

    /**
     * Handle change in text . Enable the send button if the {@link EditText}
     * is not empty, else disable the button
     */
    private void handleMessageEditText() {
        mMessageEditText.setEnabled(true);
        mSendMessageButton.setEnabled(false);
        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // if the current text is not empty then allow the user to
                // send the message
                if (0 < s.toString().trim().length())
                    mSendMessageButton.setEnabled(true);
                else
                    mSendMessageButton.setEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // If the user pick a photo
        if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK){

            // The photo will get back here as Uri.
            if (data != null) {
                Uri selectedFileUri = data.getData();
                // If the Uri is of the form // content://local_files//foo/xyz
                // Then the the last path segment will be xyz.
                final StorageReference fileRef = mChatFileAttachStorageRef.child(selectedFileUri.getLastPathSegment());
                // Send the file to the storage
                UploadTask uploadTask = fileRef.putFile(selectedFileUri);
                uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        // Continue with the task to get the download URL
                        return fileRef.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        // If the task was successful then get the download Uri
                        // and write the new message to the db
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();

                            Message message = new Message(null,
                                    mFirebaseUser.getDisplayName(),
                                    System.currentTimeMillis(),
                                    mFirebaseUser.getUid(),
                                    downloadUri.toString());

                            mUserGroupChatReference.push().setValue(message);
                        } else {
                            Toast.makeText(getContext(), R.string.error_generic_msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }
}