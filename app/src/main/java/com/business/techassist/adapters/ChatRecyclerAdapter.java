package com.business.techassist.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.Database.FirebaseUtil;
import com.business.techassist.R;
import com.business.techassist.models.ChatMessageModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatHolder> {
    Context context;
    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatHolder holder, int position, @NonNull ChatMessageModel model) {
        String formattedTime = "";
        if (model.getTimestamp() != null) {
            formattedTime = android.text.format.DateFormat.format("MMMM dd yyyy 'AT' hh:mm a", model.getTimestamp().toDate()).toString();
        }

        if (model.getSenderId().equals(FirebaseUtil.currentUserID())) {
            // Hide left chat layout
            holder.left_chat_layout.setVisibility(View.GONE);
            holder.leftTime.setVisibility(View.GONE);

            // Show right chat layout
            holder.right_chat_layout.setVisibility(View.VISIBLE);
            holder.right_chat_textview.setVisibility(View.VISIBLE);
            holder.rightTime.setVisibility(View.VISIBLE);
            holder.right_chat_textview.setText(model.getMessage());
            holder.rightTime.setText(formattedTime);

        } else {
            // Hide right chat layout
            holder.right_chat_layout.setVisibility(View.GONE);
            holder.rightTime.setVisibility(View.GONE);

            // Show left chat layout
            holder.left_chat_layout.setVisibility(View.VISIBLE);
            holder.left_chat_textview.setVisibility(View.VISIBLE);
            holder.leftTime.setVisibility(View.VISIBLE);
            holder.left_chat_textview.setText(model.getMessage());
            holder.leftTime.setText(formattedTime);
        }
    }



    @NonNull
    @Override
    public ChatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatHolder(view);
    }

    class ChatHolder extends RecyclerView.ViewHolder{

        LinearLayout left_chat_layout, right_chat_layout;
        TextView left_chat_textview, right_chat_textview, rightTime, leftTime;

        public ChatHolder(@NonNull View itemView) {
            super(itemView);

            left_chat_layout = itemView.findViewById(R.id.left_chat_layout);
            right_chat_layout = itemView.findViewById(R.id.right_chat_layout);
            left_chat_textview = itemView.findViewById(R.id.left_chat_textview);
            right_chat_textview = itemView.findViewById(R.id.right_chat_textview);
            rightTime = itemView.findViewById(R.id.rightTime);
            leftTime = itemView.findViewById(R.id.leftTime);

        }
    }
}
