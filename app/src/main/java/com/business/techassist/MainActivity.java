package com.business.techassist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.business.techassist.chatbot.ChatAssistFragment;
import com.business.techassist.menucomponents.messages.messagesMenu;
import com.business.techassist.subscription.FeatureLockManager;
import com.business.techassist.utilities.FirebaseUtil;
import com.business.techassist.subscription.SubscriptionActivity;
import com.business.techassist.subscription.SubscriptionManager;
import com.business.techassist.utilities.ServiceDataInitializer;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    BottomNavigationView bottomNavigationView;
    
    // State tracking to prevent showing too many dialogs
    private boolean isFeatureCheckInProgress = false;
    private boolean isDialogShowing = false;
    private final Handler dialogHandler = new Handler();
    private Runnable pendingDialogRunnable = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        applyDefaultTheme();
        
        // Initialize subscription plans in Firestore
        SubscriptionManager.initializeSubscriptionPlans();
        
        // Initialize sample services data
        ServiceDataInitializer.initializeSampleServices();

        // Check for expired subscriptions
        checkExpiredSubscriptions();

        // Get intent extras and check for SHOW_FRAGMENT
        String showFragment = getIntent().getStringExtra("SHOW_FRAGMENT");
        
        if (savedInstanceState == null) {
            if (showFragment != null) {
                // Load the specified fragment
                if (showFragment.equals("shop")) {
                    loadShopFragment();
                } else if (showFragment.equals("activities")) {
                    loadActivitiesFragment();
                } else if (showFragment.equals("chatbot")) {
                    checkChatbotAccess();
                } else {
                    // Default to home for unknown fragment names
                    getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frameLayout, new home()).commit();
                }
            } else {
                // Default behavior - load home fragment
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, new home()).commit();
            }
        }

        bottomNavigationView.setOnItemSelectedListener(this::bottomNav);

        getFCMToken();
    }

    // Public method to load fragments from other classes
    public void loadFragment(Fragment fragment) {
        // Apply feature access control
        if (fragment instanceof ChatAssistFragment) {
            checkChatbotAccess();
            return;
        }
        
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .commit();
                
        // Update the bottom navigation selection to match the loaded fragment
        if (fragment instanceof home) {
            bottomNavigationView.setSelectedItemId(R.id.homeBtn);
            applyDefaultTheme();
        } else if (fragment instanceof shop) {
            bottomNavigationView.setSelectedItemId(R.id.shopBtn);
            applyShopTheme();
        } else if (fragment instanceof activities) {
            bottomNavigationView.setSelectedItemId(R.id.activityBtn);
            applyDefaultTheme();
        } else if (fragment instanceof ChatAssistFragment) {
            bottomNavigationView.setSelectedItemId(R.id.chatbotBtn);
            applyGeminiTheme();
        }
    }

    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                FirebaseUtil.currentUserDetails().update("fcmToken", token);
            }
        });
    }

    private boolean bottomNav(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.homeBtn) {
            loadHomeFragment();
            return true;
        } else if (itemId == R.id.shopBtn) {
            loadShopFragment();
            return true;
        } else if (itemId == R.id.activityBtn) {
            loadActivitiesFragment();
            return true;
        } else if (itemId == R.id.chatbotBtn) {
            checkChatbotAccess();
            return true;
        }
        
        return false;
    }
    
    // Helper methods to load fragments
    private void loadHomeFragment() {
        Fragment fragment = new home();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .commit();
        applyDefaultTheme();
    }
    
    private void loadShopFragment() {
        Fragment fragment = new shop();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .commit();
        applyShopTheme();
    }
    
    private void loadActivitiesFragment() {
        Fragment fragment = new activities();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .commit();
        applyDefaultTheme();
    }
    
    private void loadChatbotFragment() {
        Fragment fragment = new ChatAssistFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .commit();
        applyGeminiTheme();
    }
    
    private void checkChatbotAccess() {
        // Cancel any pending dialogs
        if (pendingDialogRunnable != null) {
            dialogHandler.removeCallbacks(pendingDialogRunnable);
            pendingDialogRunnable = null;
        }
        
        // Prevent repeated checks or overlapping checks
        if (isFeatureCheckInProgress) {
            Log.d(TAG, "Feature check already in progress, skipping duplicate check");
            return;
        }
        
        // Prevent repeated checks if already on chatbot tab
        if (bottomNavigationView.getSelectedItemId() == R.id.chatbotBtn) {
            Log.d(TAG, "Chatbot tab already selected, skipping access check");
            return;
        }
        
        // Set check in progress flag
        isFeatureCheckInProgress = true;
        
        // Check if user is on Premium or Business plan directly
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                .collection("Users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Check for Premium or Business plan
                        String userPlan = document.getString("SubscriptionPlan");
                        String userTier = document.getString("Current Tier");
                        
                        Log.d(TAG, "Direct plan check for chatbot access - Plan: " + userPlan + ", Tier: " + userTier);
                        
                        // Grant access for Premium or Business plan users
                        if (userPlan != null && (userPlan.equalsIgnoreCase("Premium") || 
                                                userPlan.equalsIgnoreCase("Business"))) {
                            Log.d(TAG, "Granting chatbot access based on Premium/Business plan");
                            isFeatureCheckInProgress = false;
                            loadChatbotFragment();
                            return;
                        }
                        
                        // Grant access for Gold or Diamond tier users
                        if (userTier != null && (userTier.equalsIgnoreCase("Gold") || 
                                                userTier.equalsIgnoreCase("Diamond") || 
                                                userTier.equalsIgnoreCase("Platinum"))) {
                            Log.d(TAG, "Granting chatbot access based on Gold/Diamond tier");
                            isFeatureCheckInProgress = false;
                            loadChatbotFragment();
                            return;
                        }
                    }
                    
                    // Fall back to regular feature check if direct check doesn't apply
                    performChatbotFeatureCheck();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user plan for chatbot access", e);
                    // Fall back to regular feature check on error
                    performChatbotFeatureCheck();
                });
        } else {
            // No user logged in, fall back to regular feature check
            performChatbotFeatureCheck();
        }
    }
    
    private void performChatbotFeatureCheck() {
        Log.d(TAG, "Performing standard chatbot feature access check");
        FeatureLockManager.checkFeatureAccessAsync(this, FeatureLockManager.FEATURE_CHATBOT, 
                (hasAccess, message) -> {
            // Reset the flag now that the check is complete
            isFeatureCheckInProgress = false;
                    
            if (hasAccess) {
                Log.d(TAG, "Chatbot access granted, loading fragment");
                loadChatbotFragment();
            } else {
                Log.d(TAG, "Chatbot access denied: " + message);
                // Show toast but don't always show dialog
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                
                // Only show dialog if we're not in a dialog loop and no dialog is currently showing
                if (!isDialogShowing && bottomNavigationView.getSelectedItemId() != R.id.homeBtn) {
                    // Use handler to post on main thread with delay to prevent window token issues
                    pendingDialogRunnable = () -> {
                        // Make sure activity is still valid
                        if (!isFinishing() && !isDestroyed()) {
                            isDialogShowing = true;
                            FeatureLockManager.showUpgradeDialog(this, 
                                FeatureLockManager.FEATURE_CHATBOT, message);
                            // Reset flag after dialog is dismissed (handled in FeatureLockManager)
                            isDialogShowing = false;
                        }
                    };
                    
                    // Post with delay to avoid window token issues
                    dialogHandler.postDelayed(pendingDialogRunnable, 300);
                    
                    // Reset to home tab instead of trying to preserve previous state
                    bottomNavigationView.setSelectedItemId(R.id.homeBtn);
                }
            }
        });
    }

    private void applyDefaultTheme() {
        bottomNavigationView.setBackgroundColor(getResources().getColor(R.color.default_navbar, getTheme()));
        bottomNavigationView.setItemTextColor(getResources().getColorStateList(R.color.default_text_color, getTheme()));
        bottomNavigationView.setItemIconTintList(getResources().getColorStateList(R.color.bottom_nav_icon_color));
    }

    private void applyShopTheme() {
//        bottomNavigationView.setBackgroundColor(getResources().getColor(R.color.lightCream, getTheme()));
//        bottomNavigationView.setItemTextColor(getResources().getColorStateList(R.color.light_cream_text, getTheme()));
//        bottomNavigationView.setItemIconTintList(getResources().getColorStateList(R.color.light_cream_icon, getTheme()));
        bottomNavigationView.setBackgroundColor(getResources().getColor(R.color.default_navbar, getTheme()));
        bottomNavigationView.setItemTextColor(getResources().getColorStateList(R.color.default_text_color, getTheme()));
        bottomNavigationView.setItemIconTintList(getResources().getColorStateList(R.color.bottom_nav_icon_color));
    }


    private void applyGeminiTheme() {
        bottomNavigationView.setBackgroundColor(getResources().getColor(R.color.gemini_dark_background, getTheme()));
        bottomNavigationView.setItemTextColor(getResources().getColorStateList(R.color.white_text_color, getTheme()));
        bottomNavigationView.setItemIconTintList(getResources().getColorStateList(R.color.bottom_nav_icon_gemini));
    }

    /**
     * Checks for expired or cancelled subscriptions and processes them
     */
    private void checkExpiredSubscriptions() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(currentUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Check for subscription expiry
                    Timestamp expiryDate = documentSnapshot.getTimestamp("SubscriptionExpiry");
                    Boolean cancelled = documentSnapshot.getBoolean("SubscriptionCancelled");
                    String currentPlan = documentSnapshot.getString("SubscriptionPlan");
                    
                    // If user has a non-basic plan
                    if (currentPlan != null && !currentPlan.equals("Basic")) {
                        boolean shouldDowngrade = false;
                        
                        // Check if subscription has expired
                        if (expiryDate != null && expiryDate.toDate().before(new Date())) {
                            shouldDowngrade = true;
                        }
                        
                        // Check if subscription was cancelled and expiry date has passed
                        if (Boolean.TRUE.equals(cancelled) && expiryDate != null && 
                                expiryDate.toDate().before(new Date())) {
                            shouldDowngrade = true;
                        }
                        
                        // Process downgrade if needed
                        if (shouldDowngrade) {
                            processExpiredSubscription(currentUser.getUid(), currentPlan);
                        }
                    } else if (currentPlan == null) {
                        // Set default plan if missing
                        FirebaseFirestore.getInstance()
                            .collection("Users")
                            .document(currentUser.getUid())
                            .update("SubscriptionPlan", FeatureLockManager.PLAN_BASIC)
                            .addOnFailureListener(e -> 
                                Log.e(TAG, "Failed to set default subscription plan", e));
                    }
                }
            });
    }
    
    /**
     * Processes an expired subscription by downgrading the user to the Basic plan
     */
    private void processExpiredSubscription(String userId, String previousPlan) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("SubscriptionPlan", "Basic");
        updates.put("SubscriptionExpiry", null);
        updates.put("SubscriptionCancelled", false);
        
        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(userId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                // Add notification for the user
                Map<String, Object> notification = new HashMap<>();
                notification.put("title", "Subscription Ended");
                notification.put("message", "Your " + previousPlan + " subscription has ended. " +
                        "You have been moved to the Basic plan.");
                notification.put("type", "subscription_ended");
                notification.put("timestamp", FieldValue.serverTimestamp());
                notification.put("isRead", false);
                
                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(userId)
                    .collection("Notifications")
                    .add(notification);
                
                Log.d("Subscription", "User downgraded to Basic plan due to expiration");
            })
            .addOnFailureListener(e -> {
                Log.e("Subscription", "Failed to process expired subscription", e);
            });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any pending dialogs
        if (pendingDialogRunnable != null) {
            dialogHandler.removeCallbacks(pendingDialogRunnable);
            pendingDialogRunnable = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Check for expired subscriptions on resume
        checkExpiredSubscriptions();
        
        // Refresh home fragment if visible (to update subscription status)
        if (getSupportFragmentManager().findFragmentById(R.id.frameLayout) instanceof home) {
            loadHomeFragment();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            // Check if this is a subscription change
            if (data.getBooleanExtra("subscription_changed", false)) {
                String newPlan = data.getStringExtra("new_plan");
                String newTier = data.getStringExtra("new_tier");
                
                Log.d(TAG, "Subscription changed - New Plan: " + newPlan + ", New Tier: " + newTier);
                
                // Force reload of the home fragment to reflect subscription changes
                loadHomeFragment();
                
                // If forcing refresh, verify user data in Firestore
                if (data.getBooleanExtra("force_refresh", false)) {
                    // Verify and fix user data if needed
                    verifyAndFixUserData(newPlan, newTier);
                }
            }
        }
    }
    
    /**
     * Verify and fix user data in Firestore if necessary
     */
    private void verifyAndFixUserData(String expectedPlan, String expectedTier) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        FirebaseFirestore.getInstance().collection("Users").document(currentUser.getUid())
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    String currentPlan = document.getString("SubscriptionPlan");
                    String currentTier = document.getString("Current Tier");
                    
                    Log.d(TAG, "Verifying user data - Current Plan: " + currentPlan + 
                          ", Current Tier: " + currentTier + 
                          ", Expected Plan: " + expectedPlan + 
                          ", Expected Tier: " + expectedTier);
                    
                    // Check if data needs fixing
                    boolean needsFix = false;
                    Map<String, Object> updates = new HashMap<>();
                    
                    if (expectedPlan != null && !expectedPlan.equals(currentPlan)) {
                        updates.put("SubscriptionPlan", expectedPlan);
                        needsFix = true;
                    }
                    
                    if (expectedTier != null && !expectedTier.equals(currentTier)) {
                        updates.put("Current Tier", expectedTier);
                        needsFix = true;
                    }
                    
                    // Apply fixes if needed
                    if (needsFix) {
                        Log.d(TAG, "Fixing inconsistent user data");
                        FirebaseFirestore.getInstance().collection("Users").document(currentUser.getUid())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "User data fixed successfully");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to fix user data", e);
                            });
                    }
                }
            });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        // Update the current intent
        setIntent(intent);
        
        // Check if we should show a specific fragment
        String showFragment = intent.getStringExtra("SHOW_FRAGMENT");
        if (showFragment != null) {
            // Load the specified fragment
            if (showFragment.equals("shop")) {
                loadShopFragment();
            } else if (showFragment.equals("activities")) {
                loadActivitiesFragment();
            } else if (showFragment.equals("chatbot")) {
                checkChatbotAccess();
            } else if (showFragment.equals("home")) {
                loadHomeFragment();
            }
        }
    }
}
