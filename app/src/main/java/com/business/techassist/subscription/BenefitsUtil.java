package com.business.techassist.subscription;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.business.techassist.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class for displaying subscription benefits to users
 */
public class BenefitsUtil {
    private static final String TAG = "BenefitsUtil";
    private static final String COLLECTION_SUBSCRIPTIONS = "Subscriptions";
    private static final String COLLECTION_USERS = "Users";

    /**
     * Show a dialog with the benefits for the user's current tier
     * 
     * @param activity The activity context
     * @param currentTier The user's current tier (Bronze, Silver, Gold, Diamond)
     */
    public static void showCurrentBenefits(Activity activity, String currentTier) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Your Current Benefits");

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_current_benefits, null);
        builder.setView(dialogView);

        TextView currentTierText = dialogView.findViewById(R.id.currentTierText);
        TextView currentBenefitsText = dialogView.findViewById(R.id.currentBenefitsText);
        MaterialButton viewUpgradesButton = dialogView.findViewById(R.id.viewUpgradesButton);
        MaterialButton closeButton = dialogView.findViewById(R.id.closeButton);

        currentTierText.setText("Current Tier: " + currentTier);

        // Load benefits from Firestore
        loadBenefitsFromFirestore(activity, currentTier, currentBenefitsText);

        // Disable upgrade button for highest tier
        if ("Diamond".equalsIgnoreCase(currentTier)) {
            viewUpgradesButton.setVisibility(View.GONE);

            // Adjust close button to take full width
            closeButton.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
        } else {
            viewUpgradesButton.setOnClickListener(v -> {
                // Get current user plan
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    FirebaseFirestore.getInstance()
                        .collection(COLLECTION_USERS)
                        .document(currentUser.getUid())
                        .get()
                        .addOnSuccessListener(doc -> {
                            Intent intent = new Intent(activity, SubscriptionActivity.class);
                            // Add current plan info if available
                            if (doc.exists()) {
                                String plan = doc.getString("SubscriptionPlan");
                                if (plan != null && !plan.isEmpty()) {
                                    intent.putExtra("CURRENT_PLAN", plan);
                                }
                                intent.putExtra("CURRENT_TIER", currentTier);
                            }
                            activity.startActivity(intent);
                        })
                        .addOnFailureListener(e -> {
                            // Just launch without extras on failure
                            activity.startActivity(new Intent(activity, SubscriptionActivity.class));
                        });
                } else {
                    // Not logged in
                activity.startActivity(new Intent(activity, SubscriptionActivity.class));
                }
            });
        }

        AlertDialog dialog = builder.create();

        closeButton.setOnClickListener(v -> dialog.dismiss());
        
        // Show dialog when all views are set up
        dialog.show();
    }
    
    /**
     * Load benefits content from Firestore based on tier
     */
    private static void loadBenefitsFromFirestore(Activity activity, String tier, TextView benefitsTextView) {
        if (activity == null || benefitsTextView == null) return;
        
        // Show loading state
        benefitsTextView.setText("Loading benefits...");
        
        // Display benefits using either plan or tier information
            String planName = getPlanNameFromTier(tier);
            
                    FirebaseFirestore.getInstance()
            .collection(COLLECTION_SUBSCRIPTIONS)
                        .document(planName)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    // Get data from Firestore
                    List<String> features = (List<String>) document.get("features");
                    String color = document.getString("color");
                    
                    // Format benefits content
                    String benefitsContent = formatBenefitsContent(tier, features, color);
                    
                    // Set the formatted content
                    if (activity != null && !activity.isFinishing()) {
                        benefitsTextView.setText(Html.fromHtml(benefitsContent, Html.FROM_HTML_MODE_LEGACY));
                    }
                } else {
                    // Fallback to hardcoded benefits
                    String fallbackContent = getDefaultBenefitsForTier(tier);
                    
                    if (activity != null && !activity.isFinishing()) {
                        benefitsTextView.setText(Html.fromHtml(fallbackContent, Html.FROM_HTML_MODE_LEGACY));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading benefits from Firestore", e);
                
                // Use fallback content on error
                String fallbackContent = getDefaultBenefitsForTier(tier);
                
                if (activity != null && !activity.isFinishing()) {
                    benefitsTextView.setText(Html.fromHtml(fallbackContent, Html.FROM_HTML_MODE_LEGACY));
                }
            });
    }
    
    /**
     * Format benefits content as HTML based on Firestore data
     */
    private static String formatBenefitsContent(String tier, List<String> features, String color) {
        // Use default color if not provided
        if (color == null || color.isEmpty()) {
            color = getDefaultColorForTier(tier);
        }
        
                    StringBuilder benefitsBuilder = new StringBuilder();
                    
        // Add tier header with color
                    benefitsBuilder.append("<b><font color='")
                     .append(color)
                                 .append("'>")
                                 .append(tier)
                                 .append(" Tier Benefits</font></b><br>");
                    
        // Add features
        if (features != null && !features.isEmpty()) {
                    for (String feature : features) {
                benefitsBuilder.append(feature).append("<br>");
            }
        } else {
            // Fallback for missing features
            benefitsBuilder.append("• Benefits information not available<br>");
        }
        
        benefitsBuilder.append("<br>");
        
        return benefitsBuilder.toString();
    }
    
    /**
     * Get default benefits HTML content for a tier
     */
    private static String getDefaultBenefitsForTier(String tier) {
        switch(tier) {
            case "Bronze":
                return  "<b><font color='#CD7F32'>Bronze Tier Benefits</font></b><br>" +
                        "• Limited remote support (3 fixes/month)<br>" +
                        "• Community-based troubleshooting<br>" +
                        "• Basic guides<br>" +
                        "• Standard response time (24-48 hrs)<br>" +
                        "• 5% discount<br><br>";
                        
            case "Silver":
                return  "<b><font color='#C0C0C0'>Silver Tier Benefits</font></b><br>" +
                        "• Unlimited remote support<br>" +
                        "• Priority booking<br>" +
                        "• Basic diagnostics<br>" +
                        "• Faster response (12-24 hrs)<br>" +
                        "• 10% service discount<br><br>";
                        
            case "Gold":
                return  "<b><font color='#FFD700'>Gold Tier Benefits</font></b><br>" +
                        "• On-demand support<br>" +
                        "• 2 emergency calls/month<br>" +
                        "• Free monthly system check<br>" +
                        "• 6-hour response guarantee<br>" +
                        "• Access TechAssist Chatbot<br>" +
                        "• 15% service discount<br><br>";
                        
            case "Diamond":
                return  "<b><font color='#b9f2ff'>Diamond Tier Benefits</font></b><br>" +
                        "• Bulk device support<br>" +
                        "• Dedicated account manager<br>" +
                        "• Free hardware diagnostics<br>" +
                        "• 1-hour emergency response<br>" +
                        "• 24/7 priority support<br>" +
                        "• 20-25% service discount<br><br>";
                        
            default:
                return  "<b>Basic Benefits:</b><br>" +
                        "• Limited remote support (3 fixes/month)<br>" +
                        "• Community-based troubleshooting<br>" +
                        "• Basic guides<br>" +
                        "• Standard response time (24-48 hrs)<br>" +
                        "• 5% discount<br><br>";
        }
    }
    
    /**
     * Map a tier to its corresponding subscription plan name
     */
    private static String getPlanNameFromTier(String tier) {
        switch (tier) {
            case "Bronze": return "Basic";
            case "Silver": return "Standard";
            case "Gold": return "Premium";
            case "Diamond": return "Business";
            default: return "Basic"; // Default to Basic for unknown tiers
        }
    }
    
    /**
     * Get default color hex code for a tier
     */
    private static String getDefaultColorForTier(String tier) {
        switch(tier) {
            case "Bronze": return "#CD7F32";
            case "Silver": return "#C0C0C0";
            case "Gold": return "#FFD700";
            case "Diamond": return "#b9f2ff";
            case "Platinum": return "#b9f2ff";
            default: return "#808080"; // Default gray for unknown tiers
        }
    }

    /**
     * Show benefits for a user based on their Firestore data
     * 
     * @param activity The activity context
     * @param userId The user's Firebase ID
     */
    public static void showBenefitsFromFirestore(Activity activity, String userId) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        
        AtomicReference<String> tierRef = new AtomicReference<>("Bronze"); // Default
        
        // First try to get from cache for quick response
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(userId)
            .get(Source.CACHE)
            .addOnSuccessListener(cachedDoc -> {
                if (cachedDoc.exists()) {
                    processBenefitsFromUserDoc(activity, cachedDoc, tierRef);
                } else {
                    // If not in cache, get from server
                    getFromServer(activity, userId, tierRef);
                }
            })
            .addOnFailureListener(e -> {
                // Cache failed, get from server
                getFromServer(activity, userId, tierRef);
            });
    }
    
    /**
     * Get user document from server when cache fails
     */
    private static void getFromServer(Activity activity, String userId, AtomicReference<String> tierRef) {
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                    processBenefitsFromUserDoc(activity, userDoc, tierRef);
                } else {
                    // No user document, use default Bronze tier
                    showCurrentBenefits(activity, tierRef.get());
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user document: " + e.getMessage());
                showCurrentBenefits(activity, tierRef.get());
            });
    }
    
    /**
     * Process user document to get tier information and show benefits
     */
    private static void processBenefitsFromUserDoc(Activity activity, DocumentSnapshot userDoc, AtomicReference<String> tierRef) {
                        // Try to get the tier directly
                        String tier = userDoc.getString("Current Tier");
                        
                        // If tier is not available, try to get subscription plan and map to tier
                        if (tier == null || tier.isEmpty()) {
            Log.d(TAG, "No tier found, checking subscription plan");
                            String plan = userDoc.getString("SubscriptionPlan");
            
            if (plan == null || plan.isEmpty()) {
                plan = userDoc.getString("Subscriptions"); // Try alternate field
            }
                            
                            if (plan != null && !plan.isEmpty()) {
                // Try to get tier from subscription plan document
                String finalPlan = plan;
                String finalPlan1 = plan;
                                FirebaseFirestore.getInstance()
                    .collection(COLLECTION_SUBSCRIPTIONS)
                                    .document(plan)
                                    .get()
                                    .addOnSuccessListener(planDoc -> {
                                        if (planDoc.exists()) {
                                            String planTier = planDoc.getString("tier");
                                            
                                            if (planTier != null && !planTier.isEmpty()) {
                                Log.d(TAG, "Found tier from plan: " + planTier);
                                tierRef.set(planTier);
                                                showCurrentBenefits(activity, planTier);
                                            } else {
                                                // Map subscription plan to tier manually
                                String mappedTier = mapPlanToTier(finalPlan);
                                Log.d(TAG, "Mapped plan to tier: " + finalPlan + " -> " + mappedTier);
                                tierRef.set(mappedTier);
                                                showCurrentBenefits(activity, mappedTier);
                                            }
                                        } else {
                                            // Fallback mapping if plan document doesn't exist
                            String fallbackTier = mapPlanToTier(finalPlan);
                            Log.d(TAG, "Fallback mapping: " + finalPlan + " -> " + fallbackTier);
                            tierRef.set(fallbackTier);
                                            showCurrentBenefits(activity, fallbackTier);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting plan tier: " + e.getMessage());
                        String fallbackTier = mapPlanToTier(finalPlan1);
                        tierRef.set(fallbackTier);
                                        showCurrentBenefits(activity, fallbackTier);
                                    });
                            } else {
                                // No plan found, default to Bronze
                Log.d(TAG, "No subscription plan found, defaulting to Bronze");
                                showCurrentBenefits(activity, "Bronze");
                            }
                        } else {
                            // Use tier directly from user document
            Log.d(TAG, "Using tier from user document: " + tier);
            tierRef.set(tier);
                            showCurrentBenefits(activity, tier);
                        }
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