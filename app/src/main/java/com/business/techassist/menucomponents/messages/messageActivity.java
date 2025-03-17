package com.business.techassist.menucomponents.messages;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.utilities.FirebaseUtil;
import com.business.techassist.R;
import com.business.techassist.models.AdminModel;
import com.business.techassist.adapters.ChatRecyclerAdapter;
import com.business.techassist.models.ChatMessageModel;
import com.business.techassist.models.ChatroomModel;
import com.business.techassist.utilities.AndroidUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

public class messageActivity extends AppCompatActivity {

    AdminModel otherAdmin;
    String chatroomID;
    ChatroomModel chatroomModel;
    ChatRecyclerAdapter chatRecyclerAdapter;

    ImageButton back_btn, message_send_btn;
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

        otherAdmin = AndroidUtil.getAdminDataMessages(getIntent());
        chatroomID = FirebaseUtil.getChatroomIDUser(FirebaseUtil.currentUserID(), otherAdmin.getUserID());

        back_btn = findViewById(R.id.back_btn);
        message_send_btn = findViewById(R.id.message_send_btn);
        other_username = findViewById(R.id.other_username);
        chat_recycler_view = findViewById(R.id.chat_recycler_view);
        chat_message_input = findViewById(R.id.chat_message_input);

        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        other_username.setText(otherAdmin.getName());

        message_send_btn.setOnClickListener(v ->{
            String message = chat_message_input.getText().toString();
            if(message.isEmpty()){
                return;
            }
            sendMessageAdmin(message);
        });

        getOrCreateChatroomModel();
        setUpChatRecyclerView();

        chat_message_input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                chat_recycler_view.postDelayed(() -> chat_recycler_view.smoothScrollToPosition(0), 200);
            }
        });

        chat_message_input.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });



    }

    private void setUpChatRecyclerView() {
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
        chat_message_input.setText("");

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
                    .addOnFailureListener(e -> Toast.makeText(getApplicationContext(),
                            "Failed to create chatroom.", Toast.LENGTH_SHORT).show());
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
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getApplicationContext(), "Message failed to send.", Toast.LENGTH_SHORT).show();
                });
    }



    private void getOrCreateChatroomModel() {
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