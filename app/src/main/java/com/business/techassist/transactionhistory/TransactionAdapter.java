package com.business.techassist.transactionhistory;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.R;
import com.business.techassist.transactionhistory.Transaction;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionAdapter extends FirestoreRecyclerAdapter<Transaction, TransactionAdapter.TransactionViewHolder> {

    private static final String TAG = "TransactionAdapter";
    private OnTransactionClickListener transactionClickListener;

    // Interface for transaction item click events
    public interface OnTransactionClickListener {
        void onTransactionClick(DocumentSnapshot documentSnapshot, Transaction transaction);
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.transactionClickListener = listener;
    }

    public TransactionAdapter(@NonNull FirestoreRecyclerOptions<Transaction> options) {
        super(options);
        // Enable stable IDs to fix recycler view inconsistency issues
        setHasStableIds(true);
    }

    @Override
    protected void onBindViewHolder(@NonNull TransactionViewHolder holder, int position, @NonNull Transaction model) {
        try {
            Log.d(TAG, "Binding transaction at position " + position + ": " + model.getDescription());
            // Set holder position explicitly to maintain consistency
            holder.setPosition(position);
            holder.bind(model);
            
            // Set click listener for the entire item
            holder.itemView.setOnClickListener(v -> {
                if (transactionClickListener != null) {
                    transactionClickListener.onTransactionClick(
                        getSnapshots().getSnapshot(holder.getBindingAdapterPosition()),
                        model
                    );
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error binding transaction at position " + position, e);
            
            // If there's an error binding, set default values to prevent UI issues
            holder.description.setText("Transaction Details");
            holder.amount.setText("₱0.00");
            holder.date.setText("N/A");
            holder.status.setText("Unknown");
            
            // Set a default icon and safe styling
            holder.icon.setImageResource(R.drawable.ic_receipt);
            holder.icon.setColorFilter(null);
            holder.card.setCardBackgroundColor(holder.itemView.getContext().getColor(android.R.color.white));
        }
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
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

    static class TransactionViewHolder extends RecyclerView.ViewHolder {

        final TextView amount;
        final TextView description;
        final TextView date;
        final TextView status;
        final ImageView icon;
        final MaterialCardView card;
        private int currentPosition = -1;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            amount = itemView.findViewById(R.id.amount);
            description = itemView.findViewById(R.id.description);
            date = itemView.findViewById(R.id.date);
            status = itemView.findViewById(R.id.status);
            icon = itemView.findViewById(R.id.icon);
            card = itemView.findViewById(R.id.card);
        }
        
        public void setPosition(int position) {
            this.currentPosition = position;
        }

        public void bind(Transaction transaction) {
            try {
                // Set amount
                amount.setText(String.format("₱%,.2f", transaction.getAmount()));
                
                // Set description (with null check)
                description.setText(transaction.getDescription() != null ? transaction.getDescription() : "");

                // Format and set date
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                // Safely handle timestamp, checking for null
                if (transaction.getTimestamp() != null) {
                    try {
                        date.setText(sdf.format(transaction.getTimestamp().toDate()));
                    } catch (Exception e) {
                        Log.e("TransactionViewHolder", "Error formatting date", e);
                        date.setText("Invalid date");
                    }
                } else {
                    date.setText("N/A");
                }

                // Set status (with null check)
                status.setText(transaction.getStatus() != null ? transaction.getStatus() : "Unknown");
                
                // Apply styling
                setupStatusStyle(transaction.getStatus());
                setupIcon(transaction.getType());
            } catch (Exception e) {
                Log.e("TransactionViewHolder", "Error binding data: " + e.getMessage(), e);
                
                // Provide fallback values if binding fails
                amount.setText("₱0.00");
                description.setText("Transaction");
                date.setText("N/A");
                status.setText("Unknown");
                
                // Use default styling
                try {
                    status.setTextColor(itemView.getContext().getColor(android.R.color.white));
                    status.getBackground().setTint(itemView.getContext().getColor(R.color.gray));
                    icon.setImageResource(R.drawable.ic_receipt);
                    icon.setColorFilter(null);
                } catch (Exception ex) {
                    // Ignore styling errors as a last resort
                }
            }
        }

        private void setupStatusStyle(String statusText) {
            // Default values for Completed status
            int textColorRes = R.color.white;
            int bgColorRes = R.color.success;
            
            if (statusText == null) {
                statusText = "Unknown";
            }

            try {
                switch (statusText.toLowerCase()) {
                    case "completed":
                        textColorRes = R.color.white;
                        bgColorRes = R.color.success;
                        break;
                    case "pending":
                        textColorRes = R.color.white;
                        bgColorRes = R.color.warning;
                        break;
                    case "failed":
                        textColorRes = R.color.white;
                        bgColorRes = R.color.error;
                        break;
                    case "cancelled":
                        textColorRes = R.color.white;
                        bgColorRes = R.color.textSecondary;
                        break;
                    default:
                        // Use default colors for other status values
                        break;
                }

                status.setTextColor(itemView.getContext().getColor(textColorRes));
                status.getBackground().setTint(itemView.getContext().getColor(bgColorRes));
            } catch (Exception e) {
                Log.e("TransactionViewHolder", "Error setting status style", e);
                // Fallback to default styling
                try {
                    status.setTextColor(itemView.getContext().getColor(android.R.color.white));
                    status.getBackground().setTint(itemView.getContext().getColor(R.color.textSecondary));
                } catch (Exception ex) {
                    // Last resort fallback
                }
            }
        }

        private void setupIcon(String type) {
            int iconRes = R.drawable.ic_receipt;
            
            try {
                // Default to transparent background
                card.setCardBackgroundColor(itemView.getContext().getColor(android.R.color.white));
                
                if (type == null) {
                    type = "";
                }

                switch (type.toLowerCase()) {
                    case "subscription":
                        iconRes = R.drawable.ic_subscription;
                        break;
                    case "subscription_cancel":
                        iconRes = R.drawable.ic_subscription;
                        break;
                    case "service":
                        iconRes = R.drawable.ic_repair;
                        break;
                    case "repair":
                        iconRes = R.drawable.ic_repair;
                        break;
                    case "consultation":
                        iconRes = R.drawable.ic_consultation;
                        break;
                    case "purchase":
                        iconRes = R.drawable.ic_receipt;
                        break;
                    default:
                        iconRes = R.drawable.ic_receipt;
                        break;
                }

                icon.setImageResource(iconRes);
                
                // Don't set any tint on the icon to preserve original colors
                icon.setColorFilter(null);
            } catch (Exception e) {
                Log.e("TransactionViewHolder", "Error setting icon", e);
                // Fallback to basic icon
                try {
                    icon.setImageResource(R.drawable.ic_receipt);
                    icon.setColorFilter(null);
                } catch (Exception ex) {
                    // Last resort fallback
                }
            }
        }
    }
}