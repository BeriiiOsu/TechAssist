package com.business.techassist.subscription;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for subscription-related operations
 */
public class SubscriptionUtils {
    private static final String TAG = "SubscriptionUtils";
    private static final String COLLECTION_USERS = "Users";
    private static final String COLLECTION_SUBSCRIPTIONS = "Subscriptions";

    /**
     * Check if a user's subscription is active based on their document
     * 
     * @param userDoc The user's Firestore document
     * @return true if subscription is active, false otherwise
     */
    public static boolean isSubscriptionActive(DocumentSnapshot userDoc) {
        if (userDoc == null || !userDoc.exists()) return false;

        Timestamp expiryDate = userDoc.getTimestamp("SubscriptionExpiry");
        String plan = userDoc.getString("SubscriptionPlan");
        
        // Try alternate field if primary field is empty
        if (plan == null || plan.isEmpty()) {
            plan = userDoc.getString("Subscriptions");
        }

        // Basic (free) plan is always "active" but with limited features
        if (plan == null || plan.isEmpty() || plan.equals("Basic")) return true;

        // Check if subscription has expired
        return expiryDate != null && expiryDate.toDate().after(new Date());
    }

    /**
     * Check if a user has access to a feature based on tier and subscription status
     * 
     * @param userDoc The user's Firestore document
     * @param feature The feature to check access for
     * @return true if the user has access to the feature
     */
    public static boolean hasFeatureAccess(DocumentSnapshot userDoc, String feature) {
        if (userDoc == null || !userDoc.exists()) return false;
        
        // Get tier from user document or infer from plan
        String tier = userDoc.getString("Current Tier");
        if (tier == null || tier.isEmpty()) {
            // Try to get plan and derive tier
            String plan = userDoc.getString("SubscriptionPlan");
            if (plan == null || plan.isEmpty()) {
                plan = userDoc.getString("Subscriptions");
            }
            
            // Default to Bronze/Basic if no plan found
            if (plan == null || plan.isEmpty()) {
                tier = "Bronze";
            } else {
                tier = mapPlanToTier(plan);
            }
        }
        
        boolean isActive = isSubscriptionActive(userDoc);
        
        // Check if user is admin (admins have access to all features)
        Boolean isAdmin = userDoc.getBoolean("isAdmin");
        if (isAdmin != null && isAdmin) return true;
        
        // Handle different features based on tier
        switch (feature) {
            case "messages":
                // All tiers have access to messages
                return true;
            case "remote_support":
                // Silver or higher tier needed
                return !tier.equals("Bronze") && isActive;
            case "chatbot":
                // Gold or higher tier needed
                return (tier.equals("Gold") || tier.equals("Diamond")) && isActive;
            case "emergency_support":
                // Gold or higher tier needed
                return (tier.equals("Gold") || tier.equals("Diamond")) && isActive;
            case "hardware_discounts":
                // Silver or higher tier needed
                return !tier.equals("Bronze") && isActive;
            case "dedicated_support":
                // Diamond tier only
                return tier.equals("Diamond") && isActive;
            default:
                // For unknown features, allow only for Diamond tier
                return tier.equals("Diamond") && isActive;
        }
    }

    /**
     * Check if a user's subscription has expired and downgrade if needed
     * 
     * @param activity The activity context
     * @param userId The user's Firebase ID
     */
    public static void checkSubscriptionExpiry(Activity activity, String userId) {
        if (activity == null || userId == null || userId.isEmpty()) {
            Log.e(TAG, "Invalid parameters for checkSubscriptionExpiry");
            return;
        }
        
        // First try to get from cache for faster response
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(userId)
            .get(Source.CACHE)
            .addOnSuccessListener(cachedDoc -> {
                if (cachedDoc.exists()) {
                    processExpiryCheck(activity, userId, cachedDoc);
                } else {
                    // If not in cache, get from server
                    getFromServer(activity, userId);
                }
            })
            .addOnFailureListener(e -> {
                // Cache failed, get from server
                getFromServer(activity, userId);
            });
    }
    
    /**
     * Get user document from server when cache fails
     */
    private static void getFromServer(Activity activity, String userId) {
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    processExpiryCheck(activity, userId, document);
                } else {
                    // Create default user document with Basic plan
                    createDefaultUserDocument(activity, userId);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking expiry", e);
            });
    }
    
    /**
     * Process subscription expiry check for a user document
     */
    private static void processExpiryCheck(Activity activity, String userId, DocumentSnapshot document) {
        Timestamp expiryDate = document.getTimestamp("SubscriptionExpiry");
        
        // Get current plan, checking both possible field names
        String currentPlan = document.getString("SubscriptionPlan");
        if (currentPlan == null || currentPlan.isEmpty()) {
            currentPlan = document.getString("Subscriptions");
        }
        
        // Get current tier, will be used to check Diamond tier (which doesn't expire)
        String currentTier = document.getString("Current Tier");
        
        // Handle missing plan data
        if (currentPlan == null || currentPlan.isEmpty()) {
            // Set default Basic plan if none exists
            setDefaultPlan(activity, userId);
            return;
        }
        
        // Handle Diamond tier exemption (never expires)
        if ("Diamond".equals(currentTier)) {
            Log.d(TAG, "Diamond tier remains active");
            return;
        }
        
        // Handle paid plan expiry
        if (!currentPlan.equals("Basic") && expiryDate != null && expiryDate.toDate().before(new Date())) {
            // Subscription expired, downgrade to Basic/Bronze
            Map<String, Object> updates = new HashMap<>();
            updates.put("SubscriptionPlan", "Basic");
            updates.put("Current Tier", "Bronze");
            updates.put("SubscriptionExpiry", null);
            
            FirebaseFirestore.getInstance()
                .collection(COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Downgraded to Bronze due to expiry");
                    showExpiryDialog(activity);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error downgrading expired subscription", e);
                });
        }
    }
    
    /**
     * Create a default user document with Basic plan
     */
    private static void createDefaultUserDocument(Activity activity, String userId) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("SubscriptionPlan", "Basic");
        userData.put("Current Tier", "Bronze");
        userData.put("Exp", 0);
        userData.put("Total Exp", 0);
        userData.put("SubscriptionExpiry", null);
        
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(userId)
            .set(userData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Created default user document");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating user document", e);
            });
    }
    
    /**
     * Set a user's plan to Basic (default)
     */
    private static void setDefaultPlan(Activity activity, String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("SubscriptionPlan", "Basic");
        updates.put("Current Tier", "Bronze");
        updates.put("SubscriptionExpiry", null);
        
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(userId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Set default Basic plan");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error setting default plan", e);
            });
    }
    
    /**
     * Show dialog when subscription has expired
     */
    private static void showExpiryDialog(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        
        new AlertDialog.Builder(activity)
            .setTitle("Subscription Expired")
            .setMessage("Your premium subscription has expired. You've been downgraded to Bronze tier.")
            .setPositiveButton("OK", null)
            .setNeutralButton("Renew", (d, w) -> {
                activity.startActivity(new Intent(activity, SubscriptionActivity.class));
            })
            .show();
    }
    
    /**
     * Map a plan name to its corresponding tier
     */
    private static String mapPlanToTier(String plan) {
        switch (plan) {
            case "Standard": return "Silver";
            case "Premium": return "Gold";
            case "Business": return "Diamond";
            default: return "Bronze"; // Basic or unknown
        }
    }
}