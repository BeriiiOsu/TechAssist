package com.business.techassist.menucomponents.messages;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.business.techassist.utilities.FirebaseUtil;
import com.business.techassist.R;
import com.business.techassist.admin_utils.AdminModel;
import com.business.techassist.adapters.ChatRecyclerAdapter;
import com.business.techassist.models.ChatMessageModel;
import com.business.techassist.models.ChatroomModel;
import com.business.techassist.utilities.AndroidUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import com.google.firebase.firestore.DocumentSnapshot;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import com.google.android.material.snackbar.Snackbar;

public class messageActivity extends AppCompatActivity {

    AdminModel otherAdmin;
    String chatroomID;
    ChatroomModel chatroomModel;
    ChatRecyclerAdapter chatRecyclerAdapter;

    ImageButton back_btn;
    MaterialButton message_send_btn;
    TextView other_username;
    RecyclerView chat_recycler_view;
    EditText chat_message_input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get the admin data from intent
        otherAdmin = AndroidUtil.getAdminDataMessages(getIntent());
        
        // Validate the admin data before proceeding
        if (otherAdmin == null || otherAdmin.getUserID() == null) {
            // Handle the error - show message and finish activity
            Log.e("messageActivity", "Invalid user data received: " + (otherAdmin == null ? "null user" : "null userID"));
            Toast.makeText(this, "Invalid user data. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Log successful user data loading
        Log.d("messageActivity", "Successfully loaded user data: " + otherAdmin.getUserID() + 
              " - " + otherAdmin.getName());
        
        // Generate chatroom ID
        chatroomID = FirebaseUtil.getChatroomIDUser(FirebaseUtil.currentUserID(), otherAdmin.getUserID());
        
        // Initialize UI
        initializeViews();
        
        // Setup click listeners
        setupClickListeners();
        
        // Load chat data
        getOrCreateChatroomModel();
        setUpChatRecyclerView();
    }

    private void initializeViews() {
        back_btn = findViewById(R.id.back_btn);
        message_send_btn = findViewById(R.id.message_send_btn);
        other_username = findViewById(R.id.other_username);
        chat_recycler_view = findViewById(R.id.chat_recycler_view);
        chat_message_input = findViewById(R.id.chat_message_input);
        
        // Set username safely
        other_username.setText(otherAdmin.getName() != null ? otherAdmin.getName() : "User");
        
        // Focus handling for chat input
        chat_message_input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                chat_recycler_view.postDelayed(() -> {
                    if (chat_recycler_view.getAdapter() != null && 
                        chat_recycler_view.getAdapter().getItemCount() > 0) {
                        chat_recycler_view.smoothScrollToPosition(0);
                    }
                }, 200);
            }
        });

        chat_message_input.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
    }

    private void setupClickListeners() {
        // Back button click listener
        back_btn.setOnClickListener(view -> finish());
        
        // Send message button click listener
        message_send_btn.setOnClickListener(v -> {
            String message = chat_message_input.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessageAdmin(message);
            }
        });
    }

    private void setUpChatRecyclerView() {
        // Safety check for chatroomID
        if (chatroomID == null) {
            Log.e("messageActivity", "Cannot set up recycler view - chatroomID is null");
            return;
        }
        
        Query query = FirebaseUtil.getChatroomMessage(chatroomID)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class)
                .build();

        chatRecyclerAdapter = new ChatRecyclerAdapter(options, getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        chat_recycler_view.setLayoutManager(manager);
        chat_recycler_view.setAdapter(chatRecyclerAdapter);

        // Disable item animations to prevent inconsistent state errors
        chat_recycler_view.setItemAnimator(null);

        chatRecyclerAdapter.startListening();

        chatRecyclerAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                chat_recycler_view.smoothScrollToPosition(0);
            }
        });
    }


    private void sendMessageAdmin(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        
        // Clear input field
        chat_message_input.setText("");
        
        // Safety check for required data
        if (otherAdmin == null || otherAdmin.getUserID() == null || chatroomID == null) {
            Log.e("messageActivity", "Cannot send message - missing user data. " +
                  "otherAdmin: " + (otherAdmin == null ? "null" : "valid") + 
                  ", userID: " + (otherAdmin != null && otherAdmin.getUserID() != null ? "valid" : "null") + 
                  ", chatroomID: " + (chatroomID != null ? "valid" : "null"));
            
            Snackbar.make(findViewById(android.R.id.content), 
                         "Cannot send message - Invalid user data. Please try again.", 
                         Snackbar.LENGTH_LONG)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.error))
                    .setTextColor(ContextCompat.getColor(this, R.color.white))
                    .show();
                    
            // Close activity after a delay if critical data is missing
            new Handler().postDelayed(this::finish, 2000);
            return;
        }

        // Prepare the message model
        ChatMessageModel chatMessageModel = new ChatMessageModel(
                message, FirebaseUtil.currentUserID(), Timestamp.now()
        );

        // Check if chatroomModel exists, else create it
        if (chatroomModel == null) {
            chatroomModel = new ChatroomModel(
                    chatroomID,
                    Arrays.asList(FirebaseUtil.currentUserID(), otherAdmin.getUserID()),
                    Timestamp.now(),
                    message
            );

            // Create chatroom and then send the message
            FirebaseUtil.getChatroomID(chatroomID).set(chatroomModel)
                    .addOnSuccessListener(aVoid -> sendChatMessage(chatMessageModel))
                    .addOnFailureListener(e -> {
                        Log.e("messageActivity", "Failed to create chatroom: " + e.getMessage());
                        Snackbar.make(findViewById(android.R.id.content), 
                                    "Failed to create chat room", 
                                    Snackbar.LENGTH_SHORT)
                                .setBackgroundTint(ContextCompat.getColor(this, R.color.error))
                                .setTextColor(ContextCompat.getColor(this, R.color.white))
                                .show();
                    });
        } else {
            // Chatroom already exists, send message directly
            sendChatMessage(chatMessageModel);
        }
    }

    private void sendChatMessage(ChatMessageModel chatMessageModel) {
        FirebaseUtil.getChatroomMessage(chatroomID).add(chatMessageModel)
                .addOnSuccessListener(documentReference -> {
                    // Update chatroom with last message
                    chatroomModel.setLastMessageTimestamp(Timestamp.now());
                    chatroomModel.setLastMessageSenderID(FirebaseUtil.currentUserID());
                    chatroomModel.setLastMessage(chatMessageModel.getMessage());
                    FirebaseUtil.getChatroomID(chatroomID).set(chatroomModel);
                    
                    Log.d("messageActivity", "Message sent successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("messageActivity", "Failed to send message: " + e.getMessage());
                    Snackbar.make(findViewById(android.R.id.content), 
                                "Message failed to send", 
                                Snackbar.LENGTH_SHORT)
                            .setBackgroundTint(ContextCompat.getColor(this, R.color.error))
                            .setTextColor(ContextCompat.getColor(this, R.color.white))
                            .show();
                });
    }



    private void getOrCreateChatroomModel() {
        // Safety check for chatroomID
        if (chatroomID == null) {
            Log.e("messageActivity", "Cannot get chatroom - chatroomID is null");
            return;
        }
        
        FirebaseUtil.getChatroomID(chatroomID).get().addOnCompleteListener(v -> {
            if (v.isSuccessful() && v.getResult() != null && v.getResult().exists()) {
                chatroomModel = v.getResult().toObject(ChatroomModel.class);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (chatRecyclerAdapter != null) {
            chatRecyclerAdapter.startListening();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (chatRecyclerAdapter != null) {
            chatRecyclerAdapter.notifyDataSetChanged();
            chatRecyclerAdapter.startListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatRecyclerAdapter != null) chatRecyclerAdapter.stopListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (chatRecyclerAdapter != null) {
            chatRecyclerAdapter.stopListening();
        }
    }

}