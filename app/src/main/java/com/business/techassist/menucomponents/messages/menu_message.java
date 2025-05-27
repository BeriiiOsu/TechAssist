package com.business.techassist.menucomponents.messages;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.utilities.FirebaseUtil;
import com.business.techassist.R;
import com.business.techassist.adapters.RecentChatRecyclerAdapter;
import com.business.techassist.models.ChatroomModel;
import com.business.techassist.subscription.FeatureLockManager;
import com.business.techassist.subscription.SubscriptionActivity;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.android.material.button.MaterialButton;

public class menu_message extends AppCompatActivity {

    private static final String TAG = "MessageMenu";
    MaterialButton searchBtn;
    RecyclerView recyler_view;
    RecentChatRecyclerAdapter adapter;
    private ProgressBar progressBar;
    private TextView statusText;
    private ConstraintLayout loadingContainer;
    private boolean isAccessChecked = false;
    private boolean isDialogShowing = false;
    private final Handler dialogHandler = new Handler();
    private Runnable pendingDialogRunnable = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.menu_message);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        searchBtn = findViewById(R.id.searchBtn);
        recyler_view = findViewById(R.id.recyler_view);
        
        // Initialize loading views
        progressBar = findViewById(R.id.messagesProgressBar);
        statusText = findViewById(R.id.messagesStatusText);
        loadingContainer = findViewById(R.id.loadingContainer);
        
        // Initially hide content and show loading
        if (loadingContainer != null) {
            loadingContainer.setVisibility(View.VISIBLE);
        }
        if (recyler_view != null) {
            recyler_view.setVisibility(View.GONE);
        }
        
        // Check if user has access to messages feature
        checkMessagesAccess();

        searchBtn.setOnClickListener(view -> {
            // Only allow search if access is granted
            if (isAccessChecked) {
                startActivity(new Intent(menu_message.this, messagesMenu.class));
            } else {
                Toast.makeText(menu_message.this, "Checking access to messages...", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Check if user has access to the messages feature based on subscription tier
     */
    private void checkMessagesAccess() {
        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in to access messages", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Show loading state
        if (statusText != null) {
            statusText.setText("Checking subscription...");
        }
        
        // Check subscription directly
        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(currentUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                String tier = documentSnapshot.getString("SubscriptionPlan");
                if (tier == null) {
                    Log.d(TAG, "No subscription tier found, setting default Basic tier");
                    tier = FeatureLockManager.PLAN_BASIC;
                    
                    // Update the user document with default plan
                    FirebaseFirestore.getInstance()
                        .collection("Users")
                        .document(currentUser.getUid())
                        .update("SubscriptionPlan", FeatureLockManager.PLAN_BASIC)
                        .addOnCompleteListener(task -> {
                            // After setting default tier, check access
                            performFeatureAccessCheck();
                        });
                } else {
                    // Update UI before checking feature access
                    if (statusText != null) {
                        statusText.setText("Checking feature access...");
                    }
                    performFeatureAccessCheck();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching user tier", e);
                if (statusText != null) {
                    statusText.setText("Checking feature access...");
                }
                performFeatureAccessCheck();
            });
    }
    
    /**
     * Check if user has access to messages feature using FeatureLockManager
     */
    private void performFeatureAccessCheck() {
        FeatureLockManager.checkFeatureAccessAsync(this, FeatureLockManager.FEATURE_MESSAGES, 
                (hasAccess, message) -> {
            isAccessChecked = true;
            
            // Hide loading indicators
            runOnUiThread(() -> {
                if (loadingContainer != null) {
                    loadingContainer.setVisibility(View.GONE);
                }
            });
            
            if (hasAccess) {
                Log.d(TAG, "Access granted to messages feature");
                
                // Only setup recycler view if user has access
                runOnUiThread(() -> {
                    if (recyler_view != null) {
                        recyler_view.setVisibility(View.VISIBLE);
                    }
                    setupRecyclerView();
                });
            } else {
                Log.d(TAG, "Access denied to messages feature: " + message);
                
                // Show toast first
                runOnUiThread(() -> {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
                
                // Cancel any pending dialogs
                if (pendingDialogRunnable != null) {
                    dialogHandler.removeCallbacks(pendingDialogRunnable);
                }
                
                // Use a handler to post dialog with a delay to avoid window issues
                pendingDialogRunnable = () -> {
                    if (!isFinishing() && !isDestroyed() && !isDialogShowing) {
                        // Set flag to prevent multiple dialogs
                        isDialogShowing = true;
                        
                        // Show upgrade dialog
                        FeatureLockManager.showUpgradeDialog(this, FeatureLockManager.FEATURE_MESSAGES, message);
                        
                        // Navigate to subscription activity
                        Intent intent = new Intent(this, SubscriptionActivity.class);
                        startActivity(intent);
                        finish();
                    }
                };
                
                // Post with delay to avoid window token issues
                dialogHandler.postDelayed(pendingDialogRunnable, 300);
            }
        });
    }
    
    private void setupRecyclerView() {
        Query query = FirebaseUtil.allChatroomCollectionReference()
                .whereArrayContains("userIDs", FirebaseUtil.currentUserID())
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatroomModel> options = new FirestoreRecyclerOptions.Builder<ChatroomModel>()
                .setQuery(query, ChatroomModel.class).build();

        adapter = new RecentChatRecyclerAdapter(options, this);
        recyler_view.setLayoutManager(new LinearLayoutManager(this));
        recyler_view.setAdapter(adapter);
        adapter.startListening();
        
        // Add a data observer to check for empty state
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkEmptyState();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkEmptyState();
            }
        });
        
        // Initial check
        checkEmptyState();
    }

    /**
     * Check if the recycler view is empty and show empty state if needed
     */
    private void checkEmptyState() {
        // Find the empty state view - assume added to layout
        View emptyStateView = findViewById(R.id.emptyStateView);
        if (emptyStateView == null) return;
        
        // Show empty state if adapter is empty (after initial load)
        if (adapter != null && adapter.getItemCount() == 0) {
            emptyStateView.setVisibility(View.VISIBLE);
            recyler_view.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            recyler_view.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(adapter!=null)
            adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(adapter!=null)
            adapter.stopListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) adapter.stopListening();
        if (pendingDialogRunnable != null) {
            dialogHandler.removeCallbacks(pendingDialogRunnable);
            pendingDialogRunnable = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        // Re-check access if needed when activity resumes
        if (!isAccessChecked) {
            if (loadingContainer != null) {
                loadingContainer.setVisibility(View.VISIBLE);
            }
            if (recyler_view != null) {
                recyler_view.setVisibility(View.GONE);
            }
            checkMessagesAccess();
        }
    }
}