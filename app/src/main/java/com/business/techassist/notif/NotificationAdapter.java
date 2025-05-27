package com.business.techassist.notif;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.R;
import com.business.techassist.notif.Notification;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class NotificationAdapter extends FirestoreRecyclerAdapter<Notification, NotificationAdapter.NotificationViewHolder> {

    private static final String TAG = "NotificationAdapter";

    public NotificationAdapter(@NonNull FirestoreRecyclerOptions<Notification> options) {
        super(options);
        // Enable stable IDs to fix recycler view inconsistency issues
        setHasStableIds(true);
    }

    @Override
    protected void onBindViewHolder(@NonNull NotificationViewHolder holder, int position, @NonNull Notification notification) {
        try {
            Log.d(TAG, "Binding notification at position " + position);
            
            // Set position to help identify the holder
            holder.setPosition(position);
            holder.bind(notification);
        } catch (Exception e) {
            Log.e(TAG, "Error binding notification at position " + position, e);
            
            // If there's an error binding, set default values to prevent UI issues
            holder.title.setText("Notification");
            holder.message.setText("Unable to load details");
            holder.timestamp.setText("Unknown time");
            holder.card.setCardBackgroundColor(holder.itemView.getContext().getColor(android.R.color.white));
        }
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }
    
    @Override
    public void onError(@NonNull FirebaseFirestoreException e) {
        Log.e(TAG, "Error in adapter: ", e);
    }
    
    @Override
    public void onDataChanged() {
        super.onDataChanged();
        // Handle data change safely to prevent inconsistency
        Log.d(TAG, "Data changed, item count: " + getItemCount());
        notifyDataSetChanged(); // Force a complete refresh to avoid inconsistency
    }
    
    @Override
    public long getItemId(int position) {
        // Ensure we have stable IDs to prevent recycler view issues
        try {
            // Try to get a stable ID from the document ID
            return getSnapshots().getSnapshot(position).getId().hashCode();
        } catch (Exception e) {
            Log.e(TAG, "Error getting item id for position " + position, e);
            // Fallback to position-based ID if document ID not available
            return position + 1000; // Add offset to avoid conflicts with other IDs
        }
    }
    
    @Override
    public int getItemViewType(int position) {
        // Use a stable view type
        return 0; // Single view type for all items
    }
    
    @Override
    public int getItemCount() {
        try {
            return super.getItemCount();
        } catch (Exception e) {
            Log.e(TAG, "Error getting item count", e);
            return 0; // Return safe value to prevent crashes
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView message;
        private final TextView timestamp;
        private final MaterialCardView card;
        private int currentPosition = -1;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notificationTitle);
            message = itemView.findViewById(R.id.notificationMessage);
            timestamp = itemView.findViewById(R.id.notificationTime);
            card = itemView.findViewById(R.id.notificationCard);
        }
        
        public void setPosition(int position) {
            this.currentPosition = position;
        }

        public void bind(Notification notification) {
            try {
                if (notification == null) {
                    throw new IllegalArgumentException("Notification object is null");
                }
                
                title.setText(notification.getTitle() != null ? notification.getTitle() : "Notification");
                message.setText(notification.getMessage() != null ? notification.getMessage() : "");

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                
                if (notification.getTimestamp() != null) {
                    timestamp.setText(sdf.format(notification.getTimestamp().toDate()));
                } else {
                    timestamp.setText("Unknown time");
                }

                // Update visual state based on read status
                if (notification.isRead()) {
                    card.setCardBackgroundColor(itemView.getContext().getColor(R.color.background));
                } else {
                    card.setCardBackgroundColor(itemView.getContext().getColor(R.color.unread_notification));
                }
            } catch (Exception e) {
                Log.e("NotificationViewHolder", "Error binding data", e);
                // Provide fallback values if binding fails
                title.setText("Notification");
                message.setText("Unable to load details");
                timestamp.setText("Unknown time");
                card.setCardBackgroundColor(itemView.getContext().getColor(android.R.color.white));
            }
        }
    }
}