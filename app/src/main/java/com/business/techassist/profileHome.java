package com.business.techassist;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.business.techassist.UserCredentials.LoginScreen;
import com.business.techassist.admin_utils.admin;
import com.business.techassist.menucomponents.cart.cart;
import com.business.techassist.menucomponents.messages.menu_message;
import com.business.techassist.menucomponents.profileMenu;
import com.business.techassist.menucomponents.trackOrderMenu;
import com.business.techassist.subscription.SubscriptionActivity;
import com.business.techassist.subscription.FeatureLockManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class profileHome extends AppCompatActivity {

    private static final String TAG = "ProfileHome";
    private boolean isAdmin = false;
    MaterialCardView profileMenuBtn;
    MaterialButton logoutProfileBtn, cartMenuBtn, trackOrderMenuBtn, messageMenuBtn,
            settingsProfileBtn, supportProfileBtn, adminPanelBtn;
    TextView nameUser, emailUser, PP_Btn, TOS_Btn, levelExp, levelNumber, currentTierTxt;
    ImageView userPicture;
    String currentUserId = "";
    Chip totalExp;
    MaterialButton viewBenefitsBtn;
    ImageView ic_currentTier, ic_currentTierPic;
    LinearProgressIndicator xpProgress;
    String instanceCurrentTier = "";
    private static final int SUBSCRIPTION_REQUEST_CODE = 1001;
    private Dialog loadingDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_home);

        loadComponents();
        loadLoyalty();
        loadUserData();
        checkUserRoleAndSetupUI();
        setupListeners();
        loadProfileImage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SUBSCRIPTION_REQUEST_CODE && resultCode == RESULT_OK) {
            loadUserData();
            loadLoyalty();
            checkUserRoleAndSetupUI();
            loadProfileImage();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        loadUserData();
        loadLoyalty();
        checkUserRoleAndSetupUI();
        loadProfileImage();
        
        // Sync tier UI with current subscription plan
        refreshTierUI();
    }

    private void loadComponents() {
        userPicture = findViewById(R.id.userPicture);
        logoutProfileBtn = findViewById(R.id.logoutProfileBtn);
        nameUser = findViewById(R.id.nameUser);
        emailUser = findViewById(R.id.emailUser);
        PP_Btn = findViewById(R.id.PP_Btn);
        TOS_Btn = findViewById(R.id.TOS_Btn);
        profileMenuBtn = findViewById(R.id.profileMenuBtn);
        cartMenuBtn = findViewById(R.id.cartMenuBtn);
        trackOrderMenuBtn = findViewById(R.id.trackOrderMenuBtn);
        messageMenuBtn = findViewById(R.id.messageMenuBtn);
        settingsProfileBtn = findViewById(R.id.settingsProfileBtn);
        supportProfileBtn = findViewById(R.id.supportProfileBtn);
        adminPanelBtn = findViewById(R.id.adminPanelBtn);
        adminPanelBtn.setVisibility(View.GONE);

        ic_currentTier = findViewById(R.id.ic_currentTier);
        totalExp = findViewById(R.id.totalExp);
        currentTierTxt = findViewById(R.id.currentTierTxt);
        levelNumber = findViewById(R.id.levelNumber);
        levelExp = findViewById(R.id.levelExp);
        xpProgress = findViewById(R.id.xpProgress);
        ic_currentTierPic = findViewById(R.id.ic_currentTierPic);
        viewBenefitsBtn = findViewById(R.id.viewBenefitsBtn);
    }

    private void loadLoyalty() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = Objects.requireNonNull(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser())).getUid();

        db.collection("Users").document(userId)
                .get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        long currentLevel = getNumberFromDocument(documentSnapshot, "Level", 1L);
                        long currentExp = getNumberFromDocument(documentSnapshot, "Exp", 0L);
                        long totalExpValue = getNumberFromDocument(documentSnapshot, "Total Exp", 0L);
                        String currentTier = documentSnapshot.getString("Current Tier");
                        if (currentTier == null) currentTier = "Bronze";

                        // Handle multiple level ups if user gained a lot of XP
                        boolean leveledUp = false;
                        Map<String, Object> updates = new HashMap<>();

                        // Process level-ups as long as user has enough XP
                        while (true) {
                        long xpForNextLevel = calculateXpForNextLevel(currentLevel);

                            // If user has enough XP to level up
                        if (currentExp >= xpForNextLevel) {
                                leveledUp = true;
                                currentLevel++;
                                currentExp -= xpForNextLevel;
                                totalExpValue += xpForNextLevel;

                                // Determine new tier based on new level
                                currentTier = determineTier(currentLevel);
                            } else {
                                // Not enough XP for another level up, exit loop
                                break;
                            }
                        }

                        // If at least one level up occurred, update the database
                        if (leveledUp) {
                            final long finalCurrentLevel = currentLevel;
                            final long finalCurrentExp = currentExp;
                            final long finalTotalExp = totalExpValue;
                            final String finalCurrentTier = currentTier;

                            // Prepare updates for Firestore
                            updates.put("Level", currentLevel);
                            updates.put("Exp", currentExp);
                            updates.put("Total Exp", totalExpValue);
                            updates.put("Current Tier", currentTier);

                            db.collection("Users").document(userId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> updateLoyaltyUI(finalCurrentLevel, finalCurrentExp,
                                            calculateXpForNextLevel(finalCurrentLevel),
                                            finalTotalExp, finalCurrentTier))
                                    .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(profileHome.this, "Level up failed", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            // No level up, just update UI
                            updateLoyaltyUI(currentLevel, currentExp,
                                    calculateXpForNextLevel(currentLevel),
                                    totalExpValue, currentTier);
                        }
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(profileHome.this, "Failed to load loyalty data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private long getNumberFromDocument(DocumentSnapshot document, String field, long defaultValue) {
        Object value = document.get(field);
        if (value == null) return defaultValue;

        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private void updateLoyaltyUI(long level, long exp, long xpForNextLevel,
                                 long totalExp1, String tier) {
        // Safeguard against division by zero
        if (xpForNextLevel <= 0) {
            xpForNextLevel = 1;
        }

        // Calculate progress percentage safely
        int progressPercentage = (int) (((float) exp / xpForNextLevel) * 100);
        
        // Ensure progress is within valid bounds (0-100)
        progressPercentage = Math.max(0, Math.min(progressPercentage, 100));

        levelNumber.setText("Level " + level + " " + getLevelTitle(level));
        totalExp.setText(totalExp1 + " XP");
        levelExp.setText(exp + "/" + xpForNextLevel);
        
        // Update progress bar with animation for better UX
        xpProgress.setProgress(0); // Reset first
        int finalProgressPercentage = progressPercentage;
        xpProgress.postDelayed(new Runnable() {
            @Override
            public void run() {
                xpProgress.setProgress(finalProgressPercentage);
            }
        }, 100);
        
        currentTierTxt.setText("Current Tier: " + tier);

        setLevelIcon(level);
        setTierIcon(tier);
        
        // Store current tier for reference
        instanceCurrentTier = tier;
    }

    private long calculateXpForNextLevel(long currentLevel) {
        return (long) (100 * Math.pow(1.5, currentLevel - 1));
    }

    private String determineTier(long level) {
        if (level <= 5) return "Bronze";
        if (level <= 10) return "Silver";
        if (level <= 15) return "Gold";
        if (level <= 20) return "Platinum";
        return "Diamond";
    }

    private void setTierIcon(String tier) {
        if (tier == null) {
            ic_currentTier.setImageResource(R.drawable.user_icon);
            return;
        }

        // Clear any previous tint to preserve original icon colors
        ic_currentTier.setColorFilter(null);

        switch (tier) {
            case "Bronze":
                ic_currentTier.setImageResource(R.drawable.ic_bronze);
                break;
            case "Silver":
                ic_currentTier.setImageResource(R.drawable.ic_silver);
                break;
            case "Gold":
                ic_currentTier.setImageResource(R.drawable.ic_gold);
                break;
            case "Platinum":
                // Use Platinum icon for Diamond tier as requested
                ic_currentTier.setImageResource(R.drawable.ic_platinum);
                break;
            case "Diamond":
                // Use Platinum icon for Diamond tier as requested
                ic_currentTier.setImageResource(R.drawable.ic_platinum);
                break;
            default:
                ic_currentTier.setImageResource(R.drawable.user_icon);
        }
    }

    private String getLevelTitle(long level) {
        if (level <= 3) return "Newbie";
        if (level <= 7) return "Pro";
        if (level <= 10) return "Expert";
        if (level <= 15) return "Master";
        return "Legend";
    }

    private void setLevelIcon(long level) {
        if (level <= 3) {
            ic_currentTierPic.setImageResource(R.drawable.ic_newbie);
        } else if (level <= 7) {
            ic_currentTierPic.setImageResource(R.drawable.ic_pro);
        } else if (level <= 10) {
            ic_currentTierPic.setImageResource(R.drawable.ic_expert);
        } else if (level <= 15) {
            ic_currentTierPic.setImageResource(R.drawable.ic_master);
        } else {
            ic_currentTierPic.setImageResource(R.drawable.ic_legend);
        }
    }

    private void setupListeners() {
        logoutProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadingDialog("Logging out...");
                logout();
            }
        });

        PP_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPPDialog(profileHome.this, R.layout.popup_privacy_policy, R.id.privacy_policy_text, R.string.privacy_policy_text);
            }
        });

        TOS_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTOSDialog(profileHome.this, R.layout.popup_tos, R.id.terms_of_service_text, R.string.terms_of_service_text);
            }
        });

        profileMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(profileHome.this, profileMenu.class));
            }
        });

        cartMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(profileHome.this, cart.class));
            }
        });

        trackOrderMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(profileHome.this, trackOrderMenu.class));
            }
        });

        messageMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Direct access to messages without subscription check
                startActivity(new Intent(profileHome.this, menu_message.class));
            }
        });

        viewBenefitsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBenefitsDialog();
//                Intent intent = new Intent(profileHome.this, SubscriptionActivity.class);
//                startActivityForResult(intent, SUBSCRIPTION_REQUEST_CODE);
            }
        });

        adminPanelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(profileHome.this, admin.class);
                startActivity(intent);
            }
        });

        settingsProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(profileHome.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        supportProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSupportEmail();
            }
        });
    }

    public void showBenefitsDialog() {
        Dialog benefitsDialog = new Dialog(this);
        benefitsDialog.setContentView(R.layout.dialog_benefits);
        benefitsDialog.setCancelable(true);
        benefitsDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView benefitsTitle = benefitsDialog.findViewById(R.id.benefitsTitle);
        TextView benefitsContent = benefitsDialog.findViewById(R.id.benefitsContent);
        MaterialButton closeBtn = benefitsDialog.findViewById(R.id.closeBenefitsBtn);
        MaterialButton upgradeBtn = benefitsDialog.findViewById(R.id.upgradeBtn);

        // Get current user for accurate subscription plan info
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("Users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    String currentPlan = "Basic"; // Default
                    String currentTier = "Bronze"; // Default
                    
                    if (document.exists()) {
                        // Get plan from Firestore - check both possible field names
                        String plan = document.getString("SubscriptionPlan");
                        if (plan == null || plan.isEmpty()) {
                            // Try alternate field name
                            plan = document.getString("Subscriptions");
                        }
                        
                        if (plan != null && !plan.isEmpty()) {
                            currentPlan = plan;
                        }
                    }
                    
                    // Store final values for using in lambdas
                    final String finalCurrentPlan = currentPlan;
                    
                    // Get plan details from Firestore
                    FirebaseFirestore.getInstance().collection("Subscriptions").document(finalCurrentPlan)
                        .get()
                        .addOnSuccessListener(planDoc -> {
                            // Get tier from plan document
                            String tier = planDoc.getString("tier");
                            
                            // Fallback if tier not available
                            if (tier == null || tier.isEmpty()) {
                                // Map subscription plan to tier based on name
                                switch (finalCurrentPlan) {
                                    case "Basic":
                                        tier = "Bronze";
                                        break;
                                    case "Standard":
                                        tier = "Silver";
                                        break;
                                    case "Premium":
                                        tier = "Gold";
                                        break;
                                    case "Business":
                                        tier = "Diamond";
                                        break;
                                    default:
                                        tier = "Bronze"; // Default fallback
                                        break;
                                }
                            }
                            
                            final String finalTier = tier;
                            
                            // Set the dialog title based on current tier
                            benefitsTitle.setText(finalTier + " Tier Benefits");
                            
                            // Check for higher priced plans instead of using hardcoded plan names
                            checkForHigherPricedPlans(finalCurrentPlan, finalTier, upgradeBtn, benefitsDialog);
                            
                            // Get features from Firestore if available
                            List<String> featuresFromFirestore = (List<String>) planDoc.get("features");
                            
                            // Prepare tier-specific benefits based on data from Firestore
                            String tierColor;
                            switch(finalTier) {
                                case "Silver":
                                    tierColor = "#C0C0C0";
                                    break;
                                case "Gold":
                                    tierColor = "#FFD700";
                                    break;
                                case "Diamond":
                                    tierColor = "#b9f2ff";
                                    break;
                                default: // Bronze
                                    tierColor = "#CD7F32";
                                    break;
                            }
                            
                            // Start building the benefits content
                            StringBuilder contentBuilder = new StringBuilder();
                            
                            // Add standard intro content
                            contentBuilder.append("<b><font color='#2196F3'>TechAssist Benefits</font></b><br><br>")
                                         .append("<b><u>Our Services:</u></b><br>")
                                         .append("• <b>Remote Software Repairs:</b> Fix lag, viruses, malware via secure remote access<br>")
                                         .append("• <b>Shop Services:</b> Drop-off repairs at our certified service centers<br>")
                                         .append("• <b>PC Building:</b> Custom computer assembly with warranty<br><br>")
                                         .append("<b><u>Security Features:</u></b><br>")
                                         .append("• Military-grade encryption for remote sessions<br>")
                                         .append("• Real-time action logging<br>")
                                         .append("• View-only mode available<br>")
                                         .append("• One-time access codes<br><br>");
                            
                            // Add tier-specific benefits header
                            contentBuilder.append("<b><font color='")
                                         .append(tierColor)
                                         .append("'>")
                                         .append(finalTier)
                                         .append(" Tier Benefits</font></b><br>");
                            
                            // Add features from Firestore if available
                            if (featuresFromFirestore != null && !featuresFromFirestore.isEmpty()) {
                                for (String feature : featuresFromFirestore) {
                                    contentBuilder.append(feature).append("<br>");
                                }
                            } else {
                                // Fallback to default features if Firestore data unavailable
                                switch(finalTier) {
            case "Bronze":
                                        contentBuilder.append("• Limited remote support (3 fixes/month)<br>")
                                                     .append("• Community-based troubleshooting<br>")
                                                     .append("• Basic guides<br>")
                                                     .append("• Standard response time (24-48 hrs)<br>")
                                                     .append("• 5% discount<br>");
                break;
            case "Silver":
                                        contentBuilder.append("• Unlimited remote support<br>")
                                                     .append("• Priority booking<br>")
                                                     .append("• Basic diagnostics<br>")
                                                     .append("• Faster response (12-24 hrs)<br>")
                                                     .append("• 10% service discount<br>");
                break;
            case "Gold":
                                        contentBuilder.append("• On-demand support<br>")
                                                     .append("• 2 emergency calls/month<br>")
                                                     .append("• Free monthly system check<br>")
                                                     .append("• 6-hour response guarantee<br>")
                                                     .append("• Access TechAssist Chatbot<br>")
                                                     .append("• 15% service discount<br>");
                break;
            case "Diamond":
                                        contentBuilder.append("• Bulk device support<br>")
                                                     .append("• Dedicated account manager<br>")
                                                     .append("• Free hardware diagnostics<br>")
                                                     .append("• 1-hour emergency response<br>")
                                                     .append("• 24/7 priority support<br>")
                                                     .append("• 20-25% service discount<br>");
                                        break;
                                }
                            }
                            
                            contentBuilder.append("<br>");
                            
                            // Add general subscription plans section
                            contentBuilder.append("<b><u>Subscription Plans:</u></b><br>");
                            
                            // Fetch all subscription plans for display
                            FirebaseFirestore.getInstance().collection("Subscriptions")
                                .whereEqualTo("status", "active")
                                .get()
                                .addOnSuccessListener(planDocs -> {
                                    if (!planDocs.isEmpty()) {
                                        // Process each plan
                                        for (DocumentSnapshot planSnapshot : planDocs) {
                                            String planName = planSnapshot.getString("name");
                                            String planPrice = planSnapshot.getString("price");
                                            List<String> planFeatures = (List<String>) planSnapshot.get("features");
                                            
                                            if (planName != null && !planName.isEmpty()) {
                                                // Add plan header
                                                contentBuilder.append("<b>").append(planName);
                                                
                                                if (planPrice != null && !planPrice.isEmpty()) {
                                                    if ("Basic".equals(planName)) {
                                                        contentBuilder.append(" (Free)");
                                                    } else {
                                                        contentBuilder.append(" (").append(planPrice).append(")");
                                                    }
                                                }
                                                
                                                contentBuilder.append("</b><br>");
                                                
                                                // Add plan features
                                                if (planFeatures != null && !planFeatures.isEmpty()) {
                                                    for (String feature : planFeatures) {
                                                        contentBuilder.append("- ").append(feature).append("<br>");
                                                    }
                                                }
                                                
                                                contentBuilder.append("<br>");
                                            }
                                        }
                                        
                                        // Set the complete content
                                        benefitsContent.setText(Html.fromHtml(contentBuilder.toString(), Html.FROM_HTML_MODE_LEGACY));
                                        benefitsContent.setMovementMethod(new ScrollingMovementMethod());
                                    } else {
                                        // No plans found, use default content
                                        addDefaultPlansContent(contentBuilder);
                                        benefitsContent.setText(Html.fromHtml(contentBuilder.toString(), Html.FROM_HTML_MODE_LEGACY));
                                        benefitsContent.setMovementMethod(new ScrollingMovementMethod());
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    // Error fetching plans, use default content
                                    Log.e(TAG, "Error fetching subscription plans", e);
                                    addDefaultPlansContent(contentBuilder);
                                    benefitsContent.setText(Html.fromHtml(contentBuilder.toString(), Html.FROM_HTML_MODE_LEGACY));
                                    benefitsContent.setMovementMethod(new ScrollingMovementMethod());
                                });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error loading user data: " + e.getMessage());
                            
                            // Define fallback values for current plan and tier
                            final String currentPlanFallback = "Basic";
                            final String currentTierFallback = "Bronze";
                            
                            // Show error message instead of hardcoded content
                            String errorContent = "<b><font color='#FF0000'>Error Loading Data</font></b><br><br>" +
                                    "Unable to retrieve subscription data from the server. Please check your internet connection and try again.<br><br>" +
                                    "<b>Error details:</b> " + e.getMessage();
                            
                            benefitsContent.setText(Html.fromHtml(errorContent, Html.FROM_HTML_MODE_LEGACY));
                            benefitsContent.setMovementMethod(new ScrollingMovementMethod());
                            
                            // Set a basic title
                            benefitsTitle.setText("Subscription Information");

                            // Still show the upgrade button but with proper variables
                            upgradeBtn.setVisibility(View.VISIBLE);
                            upgradeBtn.setText("View Subscription Options");
                            upgradeBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    benefitsDialog.dismiss();
                                    Intent intent = new Intent(profileHome.this, SubscriptionActivity.class);
                                    intent.putExtra("CURRENT_PLAN", currentPlanFallback);
                                    intent.putExtra("CURRENT_TIER", currentTierFallback);
                                    startActivityForResult(intent, SUBSCRIPTION_REQUEST_CODE);
                                }
                            });
                        });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data: " + e.getMessage());
                    
                    // Define fallback values for current plan and tier
                    final String currentPlanFallback = "Basic";
                    final String currentTierFallback = "Bronze";
                    
                    // Show error message instead of hardcoded content
                    String errorContent = "<b><font color='#FF0000'>Error Loading Data</font></b><br><br>" +
                            "Unable to retrieve subscription data from the server. Please check your internet connection and try again.<br><br>" +
                            "<b>Error details:</b> " + e.getMessage();
                    
                    benefitsContent.setText(Html.fromHtml(errorContent, Html.FROM_HTML_MODE_LEGACY));
                    benefitsContent.setMovementMethod(new ScrollingMovementMethod());
                    
                    // Set a basic title
                    benefitsTitle.setText("Subscription Information");

                    // Still show the upgrade button but with proper variables
                    upgradeBtn.setVisibility(View.VISIBLE);
                    upgradeBtn.setText("View Subscription Options");
                    upgradeBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            benefitsDialog.dismiss();
                            Intent intent = new Intent(profileHome.this, SubscriptionActivity.class);
                            intent.putExtra("CURRENT_PLAN", currentPlanFallback);
                            intent.putExtra("CURRENT_TIER", currentTierFallback);
                            startActivityForResult(intent, SUBSCRIPTION_REQUEST_CODE);
                        }
                    });
                });
        } else {
            // Not logged in user
            upgradeBtn.setVisibility(View.VISIBLE);
            String guestText = "<b>Sign in to access TechAssist subscription benefits</b>";
            benefitsContent.setText(Html.fromHtml(guestText, Html.FROM_HTML_MODE_LEGACY));
            
            upgradeBtn.setText("Sign In");
            upgradeBtn.setOnClickListener(v -> {
                benefitsDialog.dismiss();
                // Redirect to login
                logout();
            });
        }

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                benefitsDialog.dismiss();
            }
        });
        benefitsDialog.show();
    }
    
    // Helper method to add default plans content when Firestore data is unavailable
    private void addDefaultPlansContent(StringBuilder contentBuilder) {
        contentBuilder.append("<b>Basic (Free)</b><br>")
                     .append("- Limited remote support (3 fixes/month)<br>")
                     .append("- Community-based troubleshooting<br>")
                     .append("- Basic guides<br>")
                     .append("- Standard response time (24-48 hrs)<br>")
                     .append("- 5% discount<br><br>")
                     
                     .append("<b>Standard (₱199/month)</b><br>")
                     .append("- Unlimited remote support<br>")
                     .append("- Priority booking<br>")
                     .append("- Basic diagnostics<br>")
                     .append("- Faster response (12-24 hrs)<br>")
                     .append("- 10% service discount<br><br>")
                     
                     .append("<b>Premium (₱499/month)</b><br>")
                     .append("- On-demand support<br>")
                     .append("- 2 emergency calls/month<br>")
                     .append("- Free monthly system check<br>")
                     .append("- 6-hour response guarantee<br>")
                     .append("- Access TechAssist Chatbot<br>")
                     .append("- 15% service discount<br><br>")
                     
                     .append("<b>Business Plan (Custom)</b><br>")
                     .append("- Bulk device support<br>")
                     .append("- Dedicated account manager<br>")
                     .append("- Free hardware diagnostics<br>")
                     .append("- 1-hour emergency response<br>")
                     .append("- 24/7 priority support<br>")
                     .append("- 20-25% service discount");
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
            String email = firebaseUser.getEmail();

            // Get name from Firestore first, as it might be more up-to-date
            FirebaseFirestore.getInstance()
                .collection("Users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("Name");
                        
                        if (fullName != null && !fullName.isEmpty()) {
                            // Split the name by space and take the first part as first name
                            String[] nameParts = fullName.split(" ");
                            if (nameParts.length > 0) {
                                nameUser.setText(nameParts[0]);
                            } else {
                                nameUser.setText(fullName); // In case there are no spaces
                            }
                        } else {
                            // Fall back to Google display name if Firestore name is empty
                            setNameFromGoogleAccount(firebaseUser);
                        }
                    } else {
                        // No user document, use Google name
                        setNameFromGoogleAccount(firebaseUser);
                    }
                })
                .addOnFailureListener(e -> {
                    // On failure, use Google name
                    setNameFromGoogleAccount(firebaseUser);
                });
                
            emailUser.setText(email != null ? email : "Email not found");
        } else {
            nameUser.setText("Guest");
            emailUser.setText("Not logged in");
        }
    }
    
    private void setNameFromGoogleAccount(FirebaseUser user) {
        String googleName = user.getDisplayName();
        
        if (googleName != null && !googleName.isEmpty()) {
            // Split the name by space and take the first part as first name
            String[] nameParts = googleName.split(" ");
            if (nameParts.length > 0) {
                nameUser.setText(nameParts[0]);
            } else {
                nameUser.setText(googleName); // In case there are no spaces
            }
        } else {
            nameUser.setText("User"); // Default fallback
        }
    }

    private void checkUserRoleAndSetupUI() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();

            FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(currentUserId)
                    .get()
                    .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("Role");
                            isAdmin = "Admin".equalsIgnoreCase(role);
                            adminPanelBtn.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                        } else {
                            isAdmin = false;
                            adminPanelBtn.setVisibility(View.GONE);
                            }
                        }
                    })
                    .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(profileHome.this, "Failed to load role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        adminPanelBtn.setVisibility(View.GONE);
                        }
                    });
        }
    }

    private void loadProfileImage() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null || firebaseUser.getUid() == null || firebaseUser.getUid().isEmpty()) {
            loadDefaultImage();
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseUser.getUid();

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        String imageUrl = documentSnapshot.getString("ProfilePic");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(profileHome.this)
                                    .load(imageUrl)
                                    .into(userPicture);
                        } else {
                            loadDefaultImage();
                        }
                    } else {
                        loadDefaultImage();
                        }
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    loadDefaultImage();
                        Toast.makeText(profileHome.this, "Failed to load profile image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadDefaultImage() {
        Glide.with(this)
                .load(R.drawable.user_icon)
                .into(userPicture);
    }

    private void showPPDialog(Context context, int layoutId, int textViewId, int textResId) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(layoutId);
        TextView textView = dialog.findViewById(textViewId);
        textView.setText(Html.fromHtml(getString(textResId), Html.FROM_HTML_MODE_LEGACY));
        textView.setMovementMethod(new ScrollingMovementMethod());

        ImageView closeButton = dialog.findViewById(R.id.btn_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showTOSDialog(Context context, int layoutId, int textViewId, int textResId) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(layoutId);
        TextView textView = dialog.findViewById(textViewId);
        textView.setText(Html.fromHtml(getString(textResId), Html.FROM_HTML_MODE_LEGACY));
        textView.setMovementMethod(new ScrollingMovementMethod());

        ImageView closeButton = dialog.findViewById(R.id.btn_close_tos);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showLoadingDialog(String message) {
        if (loadingDialog == null) {
            loadingDialog = new Dialog(this);
            loadingDialog.setContentView(R.layout.dialog_loading);
            loadingDialog.setCancelable(false);
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        TextView loadingText = loadingDialog.findViewById(R.id.loadingText);
        if (loadingText != null && message != null) {
            loadingText.setText(message);
        }
        
        if (!isFinishing() && !loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing() && !isFinishing()) {
            loadingDialog.dismiss();
        }
    }

    private void logout() {
        // Reference to FirebaseAuth for use in callbacks
        final FirebaseAuth auth = FirebaseAuth.getInstance();
        
        // Start with Firebase Messaging token deletion
        FirebaseMessaging.getInstance().deleteToken()
            .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                    // Proceed even if token deletion fails
                    // Get Google Sign In client
                    GoogleSignIn.getClient(profileHome.this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build())
                        .revokeAccess()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                            public void onComplete(@NonNull Task<Void> revokeTask) {
                                // Sign out from Firebase regardless of Google revoke result
                                auth.signOut();
                                
                                // Allow a brief moment for the user to see the loading dialog
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Hide loading dialog
                                        hideLoadingDialog();
                                        
                                        // Navigate to LoginScreen
                                        Intent intent = new Intent(profileHome.this, LoginScreen.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                                    }
                                }, 1000); // Show loading for at least 1 second for better UX
                            }
                        })
                        .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // If Google revoke fails, still sign out from Firebase
                                auth.signOut();
                                
                                // Hide loading dialog
                                hideLoadingDialog();
                                
                                // Navigate to LoginScreen
                                Intent intent = new Intent(profileHome.this, LoginScreen.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                                }
                        });
                }
            })
            .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // If Firebase Messaging token deletion fails, continue with logout
                    auth.signOut();
                    
                    // Hide loading dialog
                    hideLoadingDialog();
                    
                    // Navigate to LoginScreen
                    Intent intent = new Intent(profileHome.this, LoginScreen.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
            }
        });
    }

    /**
     * Comprehensively refresh all tier and subscription related UI
     */
    private void refreshTierUI() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        FirebaseFirestore.getInstance().collection("Users").document(currentUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Get subscription plan and current tier
                    String plan = documentSnapshot.getString("SubscriptionPlan");
                    
                    // Set default value if null
                    if (plan == null || plan.isEmpty()) {
                        plan = "Basic";
                    }
                    
                    final String finalPlan = plan;
                    
                    // Fetch tier info from Firestore subscription data
                    FirebaseFirestore.getInstance().collection("Subscriptions").document(finalPlan)
                        .get()
                        .addOnSuccessListener(planDoc -> {
                            if (planDoc.exists()) {
                                // Get tier directly from subscription document
                                String displayTier = planDoc.getString("tier");
                                String displayTitle = "Member";
                                
                                // Fallback if tier not found in document
                                if (displayTier == null || displayTier.isEmpty()) {
                                    // Use plan-based mapping as fallback
                                    switch (finalPlan) {
                                        case "Basic":
                                            displayTier = "Bronze";
                                            break;
                                        case "Standard":
                                            displayTier = "Silver";
                                            break;
                                        case "Premium":
                                            displayTier = "Gold";
                                            break;
                                        case "Business":
                                            displayTier = "Diamond";
                                            break;
                                        default:
                                            displayTier = "Bronze"; // Default
                                            break;
                                    }
                                }
                                
                                // Create a synchronized update for all tier-related UI elements
                                final String finalDisplayTier = displayTier;
                                final String finalTitle = displayTitle;
                                
                                // Log tier info retrieved from Firestore
                                Log.d(TAG, "Refreshed tier from Firestore: Plan=" + finalPlan + ", Tier=" + displayTier);
                                
                                // Update text and icon on UI thread
                                runOnUiThread(() -> {
                                    // Update the tier text
                                    currentTierTxt.setText("Current Tier: " + finalDisplayTier + " " + finalTitle);
                                    
                                    // Update the tier icon
                                    setTierIcon(finalDisplayTier);
                                    
                                    // Store current tier for reference
                                    instanceCurrentTier = finalDisplayTier;
                                    
                                    // Log the tier update
                                    Log.d(TAG, "Updated tier UI: " + finalPlan + " -> " + finalDisplayTier);
                                });
                            } else {
                                // Plan document doesn't exist, use manual mapping
                                fallbackTierRefresh(finalPlan);
                            }
                        })
                        .addOnFailureListener(e -> {
                            // On error, use fallback method
                            Log.e(TAG, "Error fetching subscription data: " + e.getMessage());
                            fallbackTierRefresh(finalPlan);
                        });
                } else {
                    // User document doesn't exist, use default Basic
                    fallbackTierRefresh("Basic");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error refreshing tier UI: " + e.getMessage());
                fallbackTierRefresh("Basic");
            });
    }
    
    /**
     * Fallback method for tier refresh when Firestore data is unavailable
     */
    private void fallbackTierRefresh(String plan) {
        // Map subscription plan to appropriate tier display
        String displayTier;
        String displayTitle = "Member";
        
        // Set subscription-appropriate tier display
        switch (plan) {
            case "Basic":
                displayTier = "Bronze";
                break;
            case "Standard":
                displayTier = "Silver";
                break;
            case "Premium":
                displayTier = "Gold";
                break;
            case "Business":
                displayTier = "Diamond";
                break;
            default:
                displayTier = "Bronze"; // Fallback to existing tier
                break;
        }
        
        // Create a synchronized update for all tier-related UI elements
        final String finalDisplayTier = displayTier;
        final String finalTitle = displayTitle;
        
        // Update text and icon on UI thread
        runOnUiThread(() -> {
            // Update the tier text
            currentTierTxt.setText("Current Tier: " + finalDisplayTier + " " + finalTitle);
            
            // Update the tier icon
            setTierIcon(finalDisplayTier);
            
            // Store current tier for reference
            instanceCurrentTier = finalDisplayTier;
            
            // Log the tier update
            Log.d(TAG, "Updated tier UI (fallback): " + plan + " -> " + finalDisplayTier);
        });
    }

    /**
     * Helper to extract numeric price value from price strings like "₱888/month" or "Free"
     * This method specifically handles the Philippine Peso symbol (₱) and pricing formats from Firestore
     */
    private double extractPriceValue(String priceString) {
        if (priceString == null || priceString.isEmpty()) {
            Log.d(TAG, "Price extraction: Empty or null price string, returning 0.0");
            return 0.0;
        }
        
        // Log the original price string for debugging
        Log.d(TAG, "Price extraction: Processing price string: '" + priceString + "'");
        
        // Handle "Free" case specifically 
        if (priceString.equalsIgnoreCase("Free")) {
            Log.d(TAG, "Price extraction: 'Free' price detected, returning 0.0");
            return 0.0;
        }
        
        // Handle "Custom" or "Custom Pricing" case
        if (priceString.toLowerCase().contains("custom")) {
            Log.d(TAG, "Price extraction: 'Custom' pricing detected, returning a high value (10000.0) to ensure it's treated as premium");
            return 10000.0;  // Very high value so Custom plans are treated as premium
        }
        
        // Handle case where there are no digits in the string
        if (!priceString.matches(".*\\d.*")) {
            Log.d(TAG, "Price extraction: No digits found in price string, returning 0.0");
            return 0.0;
        }
        
        try {
            // Handle the ₱ symbol and any other non-numeric characters
            // First replace the peso symbol specifically if present
            String processed = priceString.replace("₱", "");
            
            // Then extract only digits and decimal points
            String numericString = processed.replaceAll("[^0-9.]", "");
            Log.d(TAG, "Price extraction: Extracted numeric value: '" + numericString + "' from '" + priceString + "'");
            
            double value = Double.parseDouble(numericString);
            Log.d(TAG, "Price extraction: Final price value: " + value);
            return value;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Price extraction: Error parsing price '" + priceString + "' - " + e.getMessage());
            return 0.0;
        }
    }
    
    // Helper method to check if there are any plans with higher price than current plan
    private void checkForHigherPricedPlans(String currentPlan, String currentTier, MaterialButton upgradeBtn, Dialog benefitsDialog) {
        FirebaseFirestore.getInstance().collection("Subscriptions")
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener(planDocs -> {
                if (planDocs.isEmpty()) {
                    // No plans found, hide upgrade button
                    upgradeBtn.setVisibility(View.GONE);
                    return;
                }
                
                // Get current plan details to extract current price
                double currentPrice = 0.0;
                boolean foundCurrentPlan = false;
                
                // First find the current plan to get its price
                for (DocumentSnapshot planDoc : planDocs) {
                    String planName = planDoc.getString("name");
                    if (planName != null && planName.equals(currentPlan)) {
                        String priceString = planDoc.getString("price");
                        currentPrice = extractPriceValue(priceString);
                        foundCurrentPlan = true;
                        Log.d(TAG, "Found current plan '" + currentPlan + "' with price: " + priceString + " → " + currentPrice);
                        break;
                    }
                }
                
                if (!foundCurrentPlan) {
                    Log.w(TAG, "Current plan '" + currentPlan + "' not found in Firestore. Using price 0.0");
                }
                
                // Now check if there are any higher priced plans
                boolean hasHigherPricedPlan = false;
                StringBuilder availablePlansLog = new StringBuilder("Available plans for comparison:\n");
                
                for (DocumentSnapshot planDoc : planDocs) {
                    String planName = planDoc.getString("name");
                    if (planName == null) {
                        continue; // Skip plans with no name
                    }
                    
                    String priceString = planDoc.getString("price");
                    double planPrice = extractPriceValue(priceString);
                    
                    availablePlansLog.append("- ")
                        .append(planName)
                        .append(": ")
                        .append(priceString)
                        .append(" → ")
                        .append(planPrice);
                    
                    if (planName.equals(currentPlan)) {
                        availablePlansLog.append(" [CURRENT]");
                    } else if (planPrice > currentPrice) {
                        availablePlansLog.append(" [HIGHER]");
                        hasHigherPricedPlan = true;
                    } else {
                        availablePlansLog.append(" [LOWER/EQUAL]");
                    }
                    
                    availablePlansLog.append("\n");
                }
                
                // Log all plans for debugging
                Log.d(TAG, availablePlansLog.toString());
                
                // Set the visibility of the upgrade button based on whether there are higher priced plans
                if (hasHigherPricedPlan) {
                    upgradeBtn.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Higher priced plan(s) found, showing upgrade button");
                    upgradeBtn.setOnClickListener(v -> {
                        benefitsDialog.dismiss();
                        // Launch subscription activity for upgrades with current plan info
                        Intent intent = new Intent(profileHome.this, SubscriptionActivity.class);
                        intent.putExtra("CURRENT_PLAN", currentPlan);
                        intent.putExtra("CURRENT_TIER", currentTier);
                        startActivityForResult(intent, SUBSCRIPTION_REQUEST_CODE);
                    });
                } else {
                    // No higher priced plans available, hide upgrade button
                    Log.d(TAG, "No higher priced plans found, hiding upgrade button");
                    upgradeBtn.setVisibility(View.GONE);
                }
            })
            .addOnFailureListener(e -> {
                // Show error message to user
                Toast.makeText(profileHome.this, 
                    "Could not retrieve subscription plans. Please check your connection.", 
                    Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to check for higher priced plans: " + e.getMessage());
                
                // Default to showing the upgrade button with appropriate error handling
                upgradeBtn.setVisibility(View.VISIBLE);
                upgradeBtn.setText("View Subscription Options");
                upgradeBtn.setOnClickListener(v -> {
                    benefitsDialog.dismiss();
                    Intent intent = new Intent(profileHome.this, SubscriptionActivity.class);
                    intent.putExtra("CURRENT_PLAN", currentPlan);
                    intent.putExtra("CURRENT_TIER", currentTier);
                    startActivityForResult(intent, SUBSCRIPTION_REQUEST_CODE);
                });
            });
    }

    /**
     * Opens email app with pre-filled email to TechAssist support
     */
    private void sendSupportEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"business.techassistph@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "TechAssist Support Request");
        
        // Include user information in email body if logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String emailBody = "Hello TechAssist Support Team,\n\n";
        
        if (currentUser != null) {
            String name = nameUser.getText().toString();
            String email = emailUser.getText().toString();
            String userId = currentUser.getUid();
            
            emailBody += "User details:\n";
            emailBody += "Name: " + name + "\n";
            emailBody += "Email: " + email + "\n";
            emailBody += "User ID: " + userId + "\n\n";
        }
        
        emailBody += "I need assistance with the following issue:\n\n[Please describe your issue here]\n\n";
        emailBody += "Device information:\n";
        emailBody += "Device model: " + android.os.Build.MODEL + "\n";
        emailBody += "Android version: " + android.os.Build.VERSION.RELEASE + "\n";
        // Get app version from package info instead of BuildConfig
        String appVersion = "Unknown";
        try {
            appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            Log.e(TAG, "Error getting app version: " + e.getMessage());
        }
        emailBody += "App version: " + appVersion + "\n\n";
        emailBody += "Thank you,\n";
        
        if (currentUser != null) {
            emailBody += nameUser.getText().toString();
        } else {
            emailBody += "TechAssist User";
        }
        
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);
        
        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(profileHome.this, 
                "No email clients installed on your device.", 
                Toast.LENGTH_SHORT).show();
        }
    }
}
