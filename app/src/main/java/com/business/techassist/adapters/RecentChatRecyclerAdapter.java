package com.business.techassist.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.R;
import com.business.techassist.admin_utils.AdminModel;
import com.business.techassist.menucomponents.messages.messageActivity;
import com.business.techassist.models.ChatroomModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.business.techassist.utilities.AndroidUtil;
import com.business.techassist.utilities.FirebaseUtil;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;

public class RecentChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatroomModel, RecentChatRecyclerAdapter.ChatroomModelViewHolder> {

    private final Context context;

    public RecentChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatroomModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @NonNull
    @Override
    public ChatroomModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_chat_recycler_row, parent, false);
        return new ChatroomModelViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatroomModelViewHolder holder, int position, @NonNull ChatroomModel model) {
        FirebaseUtil.getOtherUserFromChatroom(model.getUserIDs())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        AdminModel otherUser = task.getResult().toObject(AdminModel.class);

                        holder.usernameText.setText(otherUser != null && otherUser.getName() != null
                                ? otherUser.getName()
                                : "Unknown");

                        holder.itemView.setOnClickListener(v -> {
                            Intent intent = new Intent(context, messageActivity.class);
                            AndroidUtil.passAdminDataMessages(intent, otherUser);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        });
                    }
                });

        // Start listening for real-time updates
        holder.attachListener(model.getChatroomID());
    }

    @Override
    public void onViewRecycled(@NonNull ChatroomModelViewHolder holder) {
        super.onViewRecycled(holder);
        holder.detachListener(); // Remove listener to avoid memory leaks
    }

    class ChatroomModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText, lastMessageText, lastMessageTime;
        ImageView profilePic;
        private ListenerRegistration chatroomListener;

        public ChatroomModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            lastMessageText = itemView.findViewById(R.id.last_message_text);
            lastMessageTime = itemView.findViewById(R.id.last_message_time_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
        }

        public void attachListener(String chatroomID) {
            DocumentReference chatroomRef = FirebaseUtil.getChatroomID(chatroomID);

            // Detach previous listener if any
            detachListener();

            chatroomListener = chatroomRef.addSnapshotListener((snapshot, e) -> {
                if (snapshot != null && snapshot.exists()) {
                    ChatroomModel updatedModel = snapshot.toObject(ChatroomModel.class);
                    if (updatedModel != null) {
                        boolean lastMessageSentByMe = updatedModel.getLastMessageSenderID() != null &&
                                updatedModel.getLastMessageSenderID().equals(FirebaseUtil.currentUserID());

                        lastMessageText.setText(lastMessageSentByMe
                                ? "You: " + updatedModel.getLastMessage()
                                : updatedModel.getLastMessage());

                        lastMessageTime.setText(updatedModel.getLastMessageTimestamp() != null
                                ? FirebaseUtil.timestampToString(updatedModel.getLastMessageTimestamp())
                                : "Time unavailable");
                    }
                }
            });
        }

        public void detachListener() {
            if (chatroomListener != null) {
                chatroomListener.remove();
                chatroomListener = null;
            }
        }
    }
}
