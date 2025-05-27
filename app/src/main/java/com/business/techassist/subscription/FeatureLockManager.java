package com.business.techassist.subscription;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.business.techassist.subscription.SubscriptionActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.Source;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manager class that handles feature access control based on user subscription plans and admin status
 */
public class FeatureLockManager {

    private static final String TAG = "FeatureLockManager";
    private static final String COLLECTION_SUBSCRIPTIONS = "Subscriptions";
    private static final String COLLECTION_USERS = "Users";
    
    // Feature constants
    public static final String FEATURE_MESSAGES = "messages";
    public static final String FEATURE_CHATBOT = "chatbot";
    public static final String FEATURE_REMOTE_SUPPORT = "remote_support";
    public static final String FEATURE_EMERGENCY_SUPPORT = "emergency_support";
    public static final String FEATURE_HARDWARE_DISCOUNTS = "hardware_discounts";
    public static final String FEATURE_DEDICATED_SUPPORT = "dedicated_support";
    
    // Subscription plan constants
    public static final String PLAN_BASIC = "Basic";
    public static final String PLAN_STANDARD = "Standard";
    public static final String PLAN_PREMIUM = "Premium";
    public static final String PLAN_BUSINESS = "Business";
    
    // Dialog tracking to prevent too many dialogs
    private static WeakReference<AlertDialog> activeDialogRef = new WeakReference<>(null);
    private static boolean isDialogShowing = false;
    
    /**
     * Asynchronously checks if a user has access to a specific feature
     * 
     * @param context Application context
     * @param feature The feature to check access for
     * @param callback Callback with the access result
     */
    public static void checkFeatureAccessAsync(Context context, String feature, FeatureAccessCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User is not logged in
            if (callback != null) {
                callback.onResult(false, "You need to be logged in to access this feature");
            }
            return;
        }
        
        Log.d(TAG, "Checking access for feature: " + feature + " (User: " + currentUser.getUid() + ")");
        
        // First try from cache for faster response
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(currentUser.getUid())
            .get(Source.CACHE)
            .addOnSuccessListener(cachedDoc -> {
                if (cachedDoc.exists()) {
                    // Process from cache
                    processUserDocument(context, cachedDoc, feature, callback);
                } else {
                    // If not in cache, get from server
                    getFromServer(context, currentUser.getUid(), feature, callback);
                }
            })
            .addOnFailureListener(e -> {
                // Cache failed, get from server
                getFromServer(context, currentUser.getUid(), feature, callback);
            });
    }
    
    /**
     * Get user document from server when cache fails
     */
    private static void getFromServer(Context context, String userId, String feature, FeatureAccessCallback callback) {
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    processUserDocument(context, document, feature, callback);
                } else {
                    // No user document - deny access
                    Log.w(TAG, "User document not found, denying access");
                    if (callback != null) {
                        callback.onResult(false, "User profile not found");
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking feature access", e);
                // In case of error, deny access
                if (callback != null) {
                    callback.onResult(false, "Error checking access: " + e.getMessage());
                }
            });
    }
    
    /**
     * Process a user document for feature access checking
     */
    private static void processUserDocument(Context context, DocumentSnapshot document, String feature, FeatureAccessCallback callback) {
        // If the SubscriptionPlan field is missing, ensure we set a default
        String plan = document.getString("SubscriptionPlan");
        if (plan == null || plan.isEmpty()) {
            // Try alternate field
            plan = document.getString("Subscriptions");
            if (plan == null || plan.isEmpty()) {
                Log.w(TAG, "User has null SubscriptionPlan field, setting Basic as default");
                // Add subscription field if missing
                FirebaseFirestore.getInstance()
                    .collection(COLLECTION_USERS)
                    .document(document.getId())
                    .update("SubscriptionPlan", PLAN_BASIC)
                    .addOnSuccessListener(aVoid -> {
                        // Re-check with the updated document
                        boolean accessWithDefault = isBasicFeature(feature);
                        String message = getUpgradeMessage(feature);
                        Log.d(TAG, "Set default plan to Basic, access for " + feature + ": " + accessWithDefault);
                        
                        if (callback != null) {
                            callback.onResult(accessWithDefault, message);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update missing subscription plan", e);
                        if (callback != null) {
                            callback.onResult(false, "Error updating subscription data");
                        }
                    });
                return;
            }
        }
        
        // Check if user is admin - admins have access to everything
        Boolean isAdmin = document.getBoolean("isAdmin");
        if (isAdmin != null && isAdmin) {
            Log.d(TAG, "Admin access granted for feature: " + feature);
            if (callback != null) {
                callback.onResult(true, "Admin access granted");
            }
            return;
        }
        
        // Check role field (legacy or alternate field)
        String role = document.getString("Role");
        if (role != null && "Admin".equalsIgnoreCase(role)) {
            Log.d(TAG, "Admin role access granted for feature: " + feature);
            if (callback != null) {
                callback.onResult(true, "Admin access granted");
            }
            return;
        }
        
        // Log the user's plan for debugging
        Log.d(TAG, "User subscription plan: " + plan + " for feature: " + feature);
        
        // Add a direct check for Premium plan and chatbot feature
        if (FEATURE_CHATBOT.equals(feature) && PLAN_PREMIUM.equalsIgnoreCase(plan.trim())) {
            Log.d(TAG, "Premium plan direct access check - Granting access to chatbot");
            if (callback != null) {
                callback.onResult(true, "Access granted for Premium plan");
            }
            return;
        }
        
        // Check tier from user document directly
        String tier = document.getString("Current Tier");
        
        // Log the user's tier for debugging
        Log.d(TAG, "User tier: " + tier + " for feature: " + feature);
        
        // Add a direct check for Gold tier and chatbot feature
        if (FEATURE_CHATBOT.equals(feature) && tier != null && "Gold".equalsIgnoreCase(tier.trim())) {
            Log.d(TAG, "Gold tier direct access check - Granting access to chatbot");
            if (callback != null) {
                callback.onResult(true, "Access granted for Gold tier");
            }
            return;
        }
        
        // Use tier to check access if available
        if (tier != null && !tier.isEmpty()) {
            // For fast response, use tier-based check
            boolean hasAccess = hasAccessBasedOnTier(tier, feature);
            String message = hasAccess ? "Access granted" : getUpgradeMessage(feature);
            
            Log.d(TAG, "Tier-based check for " + feature + " with tier " + tier + ": " + hasAccess);
            
            if (callback != null) {
                callback.onResult(hasAccess, message);
            }
            return;
        }
        
        // Get feature access from Firestore subscription document for more detailed control
        getAccessFromSubscriptionDoc(context, plan, feature, callback);
    }
    
    /**
     * Get access information from the subscription document in Firestore
     */
    private static void getAccessFromSubscriptionDoc(Context context, String plan, String feature, FeatureAccessCallback callback) {
                FirebaseFirestore.getInstance()
            .collection(COLLECTION_SUBSCRIPTIONS)
                    .document(plan)
            .get()
            .addOnSuccessListener(planDoc -> {
            if (planDoc.exists()) {
                // Check if this plan has allowedFeatures field
                List<String> allowedFeatures = (List<String>) planDoc.get("allowedFeatures");
                if (allowedFeatures != null && !allowedFeatures.isEmpty()) {
                    // Check if the requested feature is in the allowed list
                    boolean hasAccess = allowedFeatures.contains(feature);
                        String message = hasAccess ? "Access granted" : getUpgradeMessage(feature);
                        
                        Log.d(TAG, "Feature access from allowedFeatures for " + feature + " with plan " + plan + ": " + hasAccess);
                        
                        if (callback != null) {
                            callback.onResult(hasAccess, message);
                        }
                        return;
                }
                
                // Alternatively, check if there's a direct boolean field for this feature
                Boolean featureEnabled = planDoc.getBoolean("feature_" + feature);
                if (featureEnabled != null) {
                        String message = featureEnabled ? "Access granted" : getUpgradeMessage(feature);
                        
                    Log.d(TAG, "Direct feature flag for " + feature + " with plan " + plan + ": " + featureEnabled);
                        
                        if (callback != null) {
                            callback.onResult(featureEnabled, message);
                        }
                        return;
                }
                
                // If there's no direct feature information, infer from tier
                String tier = planDoc.getString("tier");
                    if (tier != null && !tier.isEmpty()) {
                    // Use tier-based rules to determine access
                    boolean hasAccess = hasAccessBasedOnTier(tier, feature);
                        String message = hasAccess ? "Access granted" : getUpgradeMessage(feature);
                        
                    Log.d(TAG, "Tier-based access for " + feature + " with tier " + tier + ": " + hasAccess);
                        
                        if (callback != null) {
                            callback.onResult(hasAccess, message);
                        }
                        return;
                    }
                }
                
                // If everything else fails, use basic plan-based rules
                boolean hasAccess = hasPlanBasedAccess(plan, feature);
                String message = hasAccess ? "Access granted" : getUpgradeMessage(feature);
                
                Log.d(TAG, "Fallback plan-based access for " + feature + " with plan " + plan + ": " + hasAccess);
                
                if (callback != null) {
                    callback.onResult(hasAccess, message);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking subscription document", e);
                
                // Fallback to basic plan-based rules
                boolean hasAccess = hasPlanBasedAccess(plan, feature);
                String message = hasAccess ? "Access granted" : getUpgradeMessage(feature);
                
                if (callback != null) {
                    callback.onResult(hasAccess, message);
                }
            });
    }
    
    /**
     * Check if the feature is available for Basic plan users
     */
    private static boolean isBasicFeature(String feature) {
        // These features are available even on the Basic plan
        return FEATURE_MESSAGES.equals(feature);
    }
    
    /**
     * Determine if a user has access to a feature based on their plan (fallback method)
     */
    private static boolean hasPlanBasedAccess(String plan, String feature) {
        // Normalize plan name to handle case sensitivity issues
        String normalizedPlan = plan != null ? plan.trim() : "";
        
        // Basic plan only has access to messages
        if (PLAN_BASIC.equalsIgnoreCase(normalizedPlan)) {
            return isBasicFeature(feature);
        }
        
        // Standard plan adds remote support and hardware discounts
        if (PLAN_STANDARD.equalsIgnoreCase(normalizedPlan)) {
            return isBasicFeature(feature) || 
                  FEATURE_REMOTE_SUPPORT.equals(feature) || 
                  FEATURE_HARDWARE_DISCOUNTS.equals(feature);
        }
        
        // Premium plan adds chatbot and emergency support
        if (PLAN_PREMIUM.equalsIgnoreCase(normalizedPlan)) {
            Log.d(TAG, "Premium plan access check for feature: " + feature);
            return isBasicFeature(feature) || 
                  FEATURE_REMOTE_SUPPORT.equals(feature) || 
                  FEATURE_HARDWARE_DISCOUNTS.equals(feature) ||
                  FEATURE_CHATBOT.equals(feature) ||
                  FEATURE_EMERGENCY_SUPPORT.equals(feature);
        }
        
        // Business plan has access to everything
        if (PLAN_BUSINESS.equalsIgnoreCase(normalizedPlan)) {
            return true;
        }
        
        // Unknown plan, default to Basic features
        return isBasicFeature(feature);
    }
    
    /**
     * Determine if a user has access to a feature based on their tier
     */
    private static boolean hasAccessBasedOnTier(String tier, String feature) {
        if (tier == null) {
            return isBasicFeature(feature);
        }
        
        // Normalize tier to handle case sensitivity issues
        String normalizedTier = tier.trim();
        
        // Allow for alternate spellings/capitalization
        boolean isBronze = normalizedTier.equalsIgnoreCase("Bronze");
        boolean isSilver = normalizedTier.equalsIgnoreCase("Silver");
        boolean isGold = normalizedTier.equalsIgnoreCase("Gold");
        boolean isDiamond = normalizedTier.equalsIgnoreCase("Diamond") || normalizedTier.equalsIgnoreCase("Platinum");
        
        // Log tier access check
        Log.d(TAG, "Tier access check for " + feature + " with tier: " + normalizedTier);
        
        if (isBronze) {
            // Bronze tier (Basic plan) only has access to messages
            return isBasicFeature(feature);
        }
        
        if (isSilver) {
            // Silver tier (Standard plan) adds remote support and hardware discounts
            return isBasicFeature(feature) || 
                   FEATURE_REMOTE_SUPPORT.equals(feature) || 
                   FEATURE_HARDWARE_DISCOUNTS.equals(feature);
        }
        
        if (isGold) {
            // Gold tier (Premium plan) adds chatbot and emergency support
            boolean hasAccess = isBasicFeature(feature) || 
                   FEATURE_REMOTE_SUPPORT.equals(feature) || 
                   FEATURE_HARDWARE_DISCOUNTS.equals(feature) ||
                   FEATURE_CHATBOT.equals(feature) ||
                   FEATURE_EMERGENCY_SUPPORT.equals(feature);
            
            // Log specific result for chatbot access
            if (FEATURE_CHATBOT.equals(feature)) {
                Log.d(TAG, "Gold tier (Premium) chatbot access result: " + hasAccess);
            }
            
            return hasAccess;
        }
        
        if (isDiamond) {
            // Diamond/Platinum tier (Business plan) has access to everything
            return true;
        }
        
        // Unknown tier, default to basic features
        return isBasicFeature(feature);
    }
    
    /**
     * Synchronous check for feature access (for UI state decisions)
     * This method only checks the local cache - may not reflect current state
     * 
     * @param context Application context
     * @param feature The feature to check access for
     * @return true if user has access, false otherwise
     */
    public static boolean hasFeatureAccess(Context context, String feature) {
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                return false; // Not logged in
            }
            
            // Quick check from cache - may not be up to date
            DocumentSnapshot userDoc = Tasks.await(
                FirebaseFirestore.getInstance()
                    .collection(COLLECTION_USERS)
                    .document(currentUser.getUid())
                    .get(Source.CACHE),
                200, TimeUnit.MILLISECONDS
            );
            
            if (userDoc.exists()) {
                // Admin access
                Boolean isAdmin = userDoc.getBoolean("isAdmin");
                if (isAdmin != null && isAdmin) {
                    return true;
                }
                
                // Check role field (legacy/alternate admin field)
                String role = userDoc.getString("Role");
                if (role != null && "Admin".equalsIgnoreCase(role)) {
                    return true;
                }
                
                // Get tier directly
                String tier = userDoc.getString("Current Tier");
                if (tier != null && !tier.isEmpty()) {
                    return hasAccessBasedOnTier(tier, feature);
                }
                
                // Get plan
                String plan = userDoc.getString("SubscriptionPlan");
                if (plan == null || plan.isEmpty()) {
                    // Try alternate field
                    plan = userDoc.getString("Subscriptions");
                    if (plan == null || plan.isEmpty()) {
                        plan = PLAN_BASIC;
                    }
                }
                
                return hasPlanBasedAccess(plan, feature);
            }
            
            // Default to basic features if user doc not in cache
            return isBasicFeature(feature);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in hasFeatureAccess", e);
            return isBasicFeature(feature); // Default to basic features on error
        }
    }
    
    /**
     * Check access and show upgrade dialog if needed
     * 
     * @param context Application context
     * @param feature The feature to check access for
     * @param onAccessGranted Callback to run if access is granted
     */
    public static void checkAndShowDialogIfNeeded(Context context, String feature, Runnable onAccessGranted) {
        checkFeatureAccessAsync(context, feature, (hasAccess, message) -> {
        if (hasAccess) {
                // Access granted, run callback
                if (onAccessGranted != null) {
                    onAccessGranted.run();
                }
        } else {
                // Access denied, show dialog
                showUpgradeDialog(context, feature, message);
            }
        });
    }
    
    /**
     * Show a dialog prompting the user to upgrade to access a feature
     * 
     * @param context Application context
     * @param feature The feature being accessed
     * @param message Custom message to explain the access restriction
     */
    public static void showUpgradeDialog(Context context, String feature, String message) {
        // Don't show if another dialog is already showing
        if (isDialogShowing) {
            AlertDialog existingDialog = activeDialogRef.get();
            if (existingDialog != null && existingDialog.isShowing()) {
                // Dialog already showing, don't show another
            return;
            }
        }
        
        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Subscription Required");
        
        String dialogMessage = message;
        if (dialogMessage == null || dialogMessage.isEmpty()) {
            dialogMessage = getUpgradeMessage(feature);
        }
        
        builder.setMessage(dialogMessage);
        
        // Add view plans button
        builder.setPositiveButton("View Plans", (dialog, which) -> {
            dialog.dismiss();
            isDialogShowing = false;
            
                    Intent intent = new Intent(context, SubscriptionActivity.class);
            intent.putExtra("feature", feature);
                    context.startActivity(intent);
        });
        
        // Add cancel button
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
            isDialogShowing = false;
        });
        
        // Show dialog
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Keep track of dialog
        activeDialogRef = new WeakReference<>(dialog);
        isDialogShowing = true;
    }
    
    /**
     * Get a message explaining why a feature requires an upgrade
     * 
     * @param feature The feature being accessed
     * @return A message explaining access requirements
     */
    private static String getUpgradeMessage(String feature) {
        switch (feature) {
            case FEATURE_MESSAGES:
                return "Direct messaging requires a subscription. Please upgrade to access this feature.";
                
            case FEATURE_CHATBOT:
                return "The TechAssist Chatbot is available in Premium and Business plans. Please upgrade to access this feature.";
                
            case FEATURE_REMOTE_SUPPORT:
                return "Remote support services require a Standard or higher subscription. Please upgrade to access this feature.";
                
            case FEATURE_EMERGENCY_SUPPORT:
                return "Emergency support services are available in Premium and Business plans. Please upgrade to access this feature.";
                
            case FEATURE_HARDWARE_DISCOUNTS:
                return "Hardware discounts require a Standard or higher subscription. Please upgrade to access this feature.";
                
            case FEATURE_DEDICATED_SUPPORT:
                return "Dedicated support agents are available in the Business plan. Please upgrade to access this feature.";
                
            default:
                return "This premium feature requires a subscription. Please upgrade your plan to access it.";
        }
    }
    
    /**
     * Callback interface for feature access check results
     */
    public interface FeatureAccessCallback {
        void onResult(boolean hasAccess, String message);
    }
}