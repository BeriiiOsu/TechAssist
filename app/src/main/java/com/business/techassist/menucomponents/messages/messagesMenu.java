package com.business.techassist.menucomponents.messages;

import android.annotation.SuppressLint;
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
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.utilities.FirebaseUtil;
import com.business.techassist.R;
import com.business.techassist.admin_utils.AdminModel;
import com.business.techassist.adapters.SearchUserRecyclerAdapter;
import com.business.techassist.subscription.FeatureLockManager;
import com.business.techassist.subscription.SubscriptionActivity;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;

public class messagesMenu extends AppCompatActivity {
    private static final String TAG = "MessagesMenu";
    private SearchView messagesSearchView;
    private RecyclerView messagesRecycler;
    private SearchUserRecyclerAdapter adapter;
    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private String currentUserRole;
    private MaterialButton backSearchBtn;
    private boolean accessChecked = false;
    private TextView statusText;
    private ProgressBar progressBar;
    private ConstraintLayout loadingContainer;
    private boolean isDialogShowing = false;
    private Handler dialogHandler = new Handler();
    private Runnable pendingDialogRunnable = null;

    private void updateSearchResultsDebounced(String newText) {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        searchRunnable = () -> updateSearchResults(newText);
        searchHandler.postDelayed(searchRunnable, 300);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.menu_message_search);

        // Initialize views - make sure to use proper IDs from layout
        backSearchBtn = findViewById(R.id.backSearchBtn);
        messagesSearchView = findViewById(R.id.messagesSearchView);
        messagesRecycler = findViewById(R.id.messagesRecycler); // Ensure this matches the XML ID
        statusText = findViewById(R.id.messagesStatusText);
        progressBar = findViewById(R.id.messagesProgressBar);
        loadingContainer = findViewById(R.id.loadingContainer);
        
        if (messagesRecycler == null) {
            Log.e(TAG, "messagesRecycler view not found, layout ID mismatch");
            Toast.makeText(this, "Error initializing app", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Set up click listener for back button
        backSearchBtn.setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Check user authentication first
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in to access messages", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Show loading state
        messagesRecycler.setVisibility(View.GONE);
        messagesSearchView.setVisibility(View.GONE);
        loadingContainer.setVisibility(View.VISIBLE);
        statusText.setText("Checking subscription...");

        // Check subscription directly
        checkSubscriptionTier(currentUser.getUid());
    }
    
    /**
     * Check the user's subscription tier directly from Firestore
     */
    private void checkSubscriptionTier(String userId) {
        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                String tier = documentSnapshot.getString("SubscriptionPlan");
                if (tier == null) {
                    Log.d(TAG, "No subscription tier found, setting default Basic tier");
                    tier = FeatureLockManager.PLAN_BASIC;
                    
                    // Update the user document with default plan
                    FirebaseFirestore.getInstance()
                        .collection("Users")
                        .document(userId)
                        .update("SubscriptionPlan", FeatureLockManager.PLAN_BASIC)
                        .addOnCompleteListener(task -> {
                            // After setting default tier, check access
                            checkFeatureAccess();
                        });
                } else {
                    // Check access with the existing tier
                    Log.d(TAG, "User tier found: " + tier);
                    // Update UI before checking feature access
                    runOnUiThread(() -> {
                        statusText.setText("Checking feature access...");
                    });
                    checkFeatureAccess();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching user tier", e);
                // If we can't fetch the tier, still try checking access
                runOnUiThread(() -> {
                    statusText.setText("Checking feature access...");
                });
                checkFeatureAccess();
            });
    }
    
    /**
     * Check if the user has access to the messages feature
     */
    private void checkFeatureAccess() {
        Log.d(TAG, "Checking access to messages feature");
        
        FeatureLockManager.checkFeatureAccessAsync(this, FeatureLockManager.FEATURE_MESSAGES, 
                (hasAccess, message) -> {
            accessChecked = true;
            
            // Hide loading indicators
            runOnUiThread(() -> {
                loadingContainer.setVisibility(View.GONE);
            });
            
            if (hasAccess) {
                Log.d(TAG, "Access granted to messages feature");
                
                // Only execute this if user has access
                runOnUiThread(() -> {
                    messagesRecycler.setVisibility(View.VISIBLE);
                    messagesSearchView.setVisibility(View.VISIBLE);
                    
                    FirebaseUtil.getCurrentUserRole(role -> {
                        currentUserRole = role;
                        setupRecyclerView();
                        setupSearchView();
                    });
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
        // Apply layout manager
        messagesRecycler.setLayoutManager(new LinearLayoutManager(this));
        
        try {
            // Create options for FirestoreRecyclerAdapter
            FirestoreRecyclerOptions<AdminModel> options = new FirestoreRecyclerOptions.Builder<AdminModel>()
                    .setQuery(getUserQuery(""), AdminModel.class)
                    .build();
    
            // Initialize adapter
            adapter = new SearchUserRecyclerAdapter(options, this);
            messagesRecycler.setAdapter(adapter);
            adapter.startListening();  // Ensure it starts listening immediately
            
            // Add data observer to check for empty results
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
            
            Log.d(TAG, "RecyclerView setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
            Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Check if search results are empty and show appropriate UI
     */
    private void checkEmptyState() {
        // Find empty state view - assume added to layout
        View emptySearchView = findViewById(R.id.emptySearchView);
        if (emptySearchView == null) return;
        
        // Show empty state if adapter is empty (after search)
        if (adapter != null && adapter.getItemCount() == 0 && messagesSearchView.getQuery().length() > 0) {
            emptySearchView.setVisibility(View.VISIBLE);
            messagesRecycler.setVisibility(View.GONE);
        } else {
            emptySearchView.setVisibility(View.GONE);
            messagesRecycler.setVisibility(View.VISIBLE);
        }
    }

    private Query getUserQuery(String searchText) {
        Query query;

        try {
            if (searchText.isEmpty()) {
                // Initial load: fetch users based on role
                if ("Admin".equals(currentUserRole)) {
                    // For admins, we can use a simple query to get both admin and user roles
                    // Using only orderBy without the whereIn to avoid composite index requirements
                    query = FirebaseUtil.allUserCollectionReference()
                            .orderBy("Name", Query.Direction.ASCENDING);
                            
                    // Note: This returns all users, but since we're avoiding the composite index,
                    // we'll filter the results on the client side in the adapter if needed
                } else {
                    // For regular users, we need to find admins
                    // This is a common query pattern that should have a single field index on Role
                    query = FirebaseUtil.allUserCollectionReference()
                            .whereEqualTo("Role", "Admin");
                    
                    // We're removing the orderBy here to avoid composite index requirements
                    // The results will be in Firestore's default order
                }
            } else {
                // Search filter: fetch based on name, similar simplified approach
                if ("Admin".equals(currentUserRole)) {
                    // For admins searching, we can just search by name without role filtering
                    query = FirebaseUtil.allUserCollectionReference()
                            .orderBy("Name")
                            .startAt(searchText)
                            .endAt(searchText + "\uf8ff");
                } else {
                    // For regular users, search only among admins
                    // Here we need a composite index, but this is a common pattern
                    // that should have been created already or will prompt the user to create it
                    query = FirebaseUtil.allUserCollectionReference()
                            .whereEqualTo("Role", "Admin")
                            .orderBy("Name")
                            .startAt(searchText)
                            .endAt(searchText + "\uf8ff");
                }
            }
            
            return query;
        } catch (Exception e) {
            Log.e(TAG, "Error creating query", e);
            // Fallback to a simple query if there's an error
            return FirebaseUtil.allUserCollectionReference().limit(10);
        }
    }

    private void setupSearchView() {
        messagesSearchView.setIconified(false);
        messagesSearchView.requestFocus();
        messagesSearchView.setSubmitButtonEnabled(true);

        messagesSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                updateSearchResultsDebounced(newText);
                return true;
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateSearchResults(String newText) {
        try {
            // Update the adapter with the new search query
            FirestoreRecyclerOptions<AdminModel> options = new FirestoreRecyclerOptions.Builder<AdminModel>()
                    .setQuery(getUserQuery(newText), AdminModel.class)
                    .build();

            // Update adapter with new query
            adapter.updateOptions(options);
            adapter.notifyDataSetChanged();
            
            // Check for empty results after updating search
            checkEmptyState();
            
            Log.d(TAG, "Search query updated: " + newText);
        } catch (Exception e) {
            Log.e(TAG, "Error updating search results", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // If activity is resumed and access hasn't been checked or adapter not set up,
        // do the check again - user might have upgraded subscription
        if (!accessChecked || adapter == null) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                loadingContainer.setVisibility(View.VISIBLE);
                messagesRecycler.setVisibility(View.GONE);
                messagesSearchView.setVisibility(View.GONE);
                statusText.setText("Checking subscription...");
                checkSubscriptionTier(currentUser.getUid());
            } else {
                finish();
            }
        } else if (adapter != null) {
            adapter.notifyDataSetChanged();
            adapter.startListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (adapter != null) adapter.stopListening();
        if (pendingDialogRunnable != null) {
            dialogHandler.removeCallbacks(pendingDialogRunnable);
            pendingDialogRunnable = null;
        }
    }
}
