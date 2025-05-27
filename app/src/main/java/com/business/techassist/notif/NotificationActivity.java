package com.business.techassist.notif;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.business.techassist.R;
import com.business.techassist.notif.NotificationAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyStateView;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Initialize views
        recyclerView = findViewById(R.id.notificationRecyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        
        // Get current user
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        // Set up empty state view if available
        emptyStateView = findViewById(R.id.emptyState);

        // Set up RecyclerView
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null); // Disable animations to prevent glitches
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        setupRecyclerView();
        setupSwipeRefresh();
    }

    private void setupRecyclerView() {
        if (currentUser == null) {
            showEmptyState("Please sign in to view notifications");
            return;
        }
        
        String userId = currentUser.getUid();
        Log.d(TAG, "Setting up RecyclerView for user: " + userId);
        
        try {
            // Clear any existing adapter
            if (adapter != null) {
                adapter.stopListening();
                recyclerView.setAdapter(null);
            }
            
            Query query = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(userId)
                    .collection("Notifications")
                    .orderBy("timestamp", Query.Direction.DESCENDING);

            FirestoreRecyclerOptions<Notification> options = new FirestoreRecyclerOptions.Builder<Notification>()
                    .setQuery(query, Notification.class)
                    .build();

            adapter = new NotificationAdapter(options);
            recyclerView.setAdapter(adapter);
            
            // Register adapter data observer to handle empty state
            adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    Log.d(TAG, "Items inserted: " + itemCount);
                    checkEmptyState();
                    swipeRefresh.setRefreshing(false);
                }
                
                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    checkEmptyState();
                }
                
                @Override
                public void onChanged() {
                    checkEmptyState();
                    swipeRefresh.setRefreshing(false);
                }
            });
            
            // Start listening
            adapter.startListening();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
            showEmptyState("Error loading notifications");
            swipeRefresh.setRefreshing(false);
        }
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            Log.d(TAG, "Refreshing notifications");
            setupRecyclerView();
        });
    }
    
    private void checkEmptyState() {
        if (emptyStateView == null) return;
        
        if (adapter == null || adapter.getItemCount() == 0) {
            showEmptyState("No notifications found");
        } else {
            hideEmptyState();
        }
    }
    
    private void showEmptyState(String message) {
        if (emptyStateView != null) {
            recyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
            emptyStateView.setText(message);
        }
    }
    
    private void hideEmptyState() {
        if (emptyStateView != null) {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}
