package com.business.techassist.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.utilities.FirebaseUtil;
import com.business.techassist.R;
import com.business.techassist.models.AdminModel;
import com.business.techassist.menucomponents.messages.messageActivity;
import com.business.techassist.utilities.AndroidUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class SearchUserRecyclerAdapter extends FirestoreRecyclerAdapter<AdminModel, SearchUserRecyclerAdapter.UserHolder> {

    private Context context;

    public SearchUserRecyclerAdapter(@NonNull FirestoreRecyclerOptions<AdminModel> options, Context context) {
        super(options);
        this.context = context;
        setHasStableIds(false);
    }

    @Override
    protected void onBindViewHolder(@NonNull UserHolder holder, int position, @NonNull AdminModel model) {
        if (position == RecyclerView.NO_POSITION) return;

        try {
            holder.emailRow.setText(model.getEmail());
            holder.nameRow.setText(model.getName());

            if (model.getUserID() != null && model.getUserID().equals(FirebaseUtil.currentUserID())) {
                holder.nameRow.setText(model.getName() + " (Me)");
            }

            holder.itemView.setOnClickListener(view -> {
                Intent intent = new Intent(context, messageActivity.class);
                AndroidUtil.passAdminDataMessages(intent, model);
                context.startActivity(intent);
            });

        } catch (Exception e) {
            Toast.makeText(context, "Error Search: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < getSnapshots().size()) {
            return getSnapshots().getSnapshot(position).getId().hashCode();
        } else {
            return RecyclerView.NO_ID;
        }
    }



    @NonNull
    @Override
    public UserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_recycler_user_row, parent, false);
        return new UserHolder(view);
    }

    public void updateOptions(FirestoreRecyclerOptions<AdminModel> newOptions) {
        super.updateOptions(newOptions);
        notifyDataSetChanged();
    }




    static class UserHolder extends RecyclerView.ViewHolder {
        TextView nameRow;
        TextView emailRow;
        ImageView profile_pic_image_view;

        public UserHolder(@NonNull View itemView) {
            super(itemView);
            nameRow = itemView.findViewById(R.id.nameRow);
            emailRow = itemView.findViewById(R.id.emailRow);
            profile_pic_image_view = itemView.findViewById(R.id.profile_pic_image_view);
        }
    }
}
