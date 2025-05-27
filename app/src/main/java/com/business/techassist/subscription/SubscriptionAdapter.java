package com.business.techassist.subscription;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * Adapter for displaying subscription plans in a RecyclerView
 */
public class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionAdapter.SubscriptionViewHolder> {
    private static final String TAG = "SubscriptionAdapter";
    private final List<SubscriptionPlan> subscriptionPlans;
    private final Context context;
    private final OnSubscriptionSelectedListener listener;
    private final String currentPlan;
    private int selectedPosition = -1;

    public interface OnSubscriptionSelectedListener {
        void onSubscriptionSelected(SubscriptionPlan plan);
    }

    public SubscriptionAdapter(Context context, List<SubscriptionPlan> subscriptionPlans, String currentPlan, OnSubscriptionSelectedListener listener) {
        this.context = context;
        this.subscriptionPlans = subscriptionPlans;
        this.listener = listener;
        this.currentPlan = currentPlan;
        
        // Mark the current plan as selected initially
        if (currentPlan != null && !currentPlan.isEmpty()) {
            for (int i = 0; i < subscriptionPlans.size(); i++) {
                if (subscriptionPlans.get(i).getName().equals(currentPlan)) {
                    selectedPosition = i;
                    break;
                }
            }
        }
    }

    @NonNull
    @Override
    public SubscriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subscription, parent, false);
        return new SubscriptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubscriptionViewHolder holder, int position) {
        SubscriptionPlan plan = subscriptionPlans.get(position);
        boolean isCurrentPlan = plan.getName().equals(currentPlan);
        boolean isActivePlan = plan.isActive();
        
        // Set basic plan information
        holder.planNameTextView.setText(plan.getName());
        holder.planPriceTextView.setText(plan.getPrice());
        
        // Get tier from plan
        String tier = plan.getTier();
        if (tier == null || tier.isEmpty()) {
            tier = plan.resolveTier();
        }
        
        // Set tier text if view exists
        if (holder.planTierTextView != null) {
            holder.planTierTextView.setText(tier + " Tier");
        }
        
        // Always set icon based on tier for consistency
        setDefaultIcon(holder, tier);
        
        // Apply color from Firestore
        applyPlanColor(holder, plan);
        
        // Format features
        StringBuilder featuresText = new StringBuilder();
        List<String> features = plan.getFeatures();
        if (features != null && !features.isEmpty()) {
            for (String feature : features) {
                featuresText.append(feature).append("\n");
            }
            if (featuresText.length() > 0) {
                featuresText.setLength(featuresText.length() - 1); // Remove last newline
            }
        } else {
            featuresText.append("Features loading...");
        }
        
        // For inactive plans, add a note about unavailability
        if (!isActivePlan) {
            featuresText.append("\n\n");
            featuresText.append("This plan is currently unavailable.");
        }
        
        holder.planFeaturesTextView.setText(featuresText.toString());
        
        // Handle "Current Plan" badge if view exists
        if (holder.currentPlanBadge != null) {
            holder.currentPlanBadge.setVisibility(isCurrentPlan ? View.VISIBLE : View.GONE);
        }
        
        // Set item selection state
        boolean isSelected = position == selectedPosition;
        holder.cardView.setCardElevation(isSelected ? 12f : 4f);
        
        // Only MaterialCardView has setStrokeWidth method
        if (holder.cardView instanceof MaterialCardView) {
            ((MaterialCardView) holder.cardView).setStrokeWidth(isSelected ? 4 : 0);
        }
        
        // Set card selection color based on plan's color
        if (isSelected) {
            setSelectedCardStroke(holder, plan);
        }
        
        // Visual indication for inactive plans
        if (!isActivePlan) {
            // Apply greyscale effect or reduced opacity
            holder.itemView.setAlpha(0.5f);
            holder.planNameTextView.setTextColor(Color.GRAY);
            holder.planPriceTextView.setTextColor(Color.GRAY);
            
            if (holder.cardView instanceof MaterialCardView) {
                // Visual indication that plan is unavailable
                ((MaterialCardView) holder.cardView).setStrokeColor(Color.LTGRAY);
            }
        }
        
        // Check if plan is selectable (must be active and appropriate tier)
        boolean isSelectable = plan.isSelectable() && isActivePlan;
        
        // Only make the item clickable if it's selectable
        if (isSelectable) {
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int oldPosition = selectedPosition;
                    selectedPosition = holder.getAdapterPosition();
                    
                    // Refresh both old and new positions
                    if (oldPosition != -1) {
                        notifyItemChanged(oldPosition);
                    }
                    notifyItemChanged(selectedPosition);
                    
                    listener.onSubscriptionSelected(plan);
                }
            });
            
            // Keep clickable appearance
            holder.itemView.setAlpha(isActivePlan ? 1.0f : 0.5f);
        } else {
            // Remove click listener and show as disabled
            holder.itemView.setOnClickListener(null);
            // If it's the current plan but inactive, still show it normally
            holder.itemView.setAlpha(isCurrentPlan ? 1.0f : 0.6f);
        }
    }
    
    /**
     * Apply color styling to plan card based on Firestore data
     */
    private void applyPlanColor(SubscriptionViewHolder holder, SubscriptionPlan plan) {
        // Get color from plan object
        String colorHex = plan.getColor();
        
        if (colorHex != null && !colorHex.isEmpty()) {
            try {
                int planColor = Color.parseColor(colorHex);
                
                // Apply color to UI elements
                holder.planIconImageView.setColorFilter(planColor);
                holder.planNameTextView.setTextColor(planColor);
                
                return;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid color format: " + colorHex, e);
                // Will fall back to default colors below
            }
        }
        
        // If color not in plan object, try to get from Firestore
        FirebaseFirestore.getInstance()
            .collection("Subscriptions")
            .document(plan.getName())
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    String docColorHex = document.getString("color");
                    if (docColorHex != null && !docColorHex.isEmpty()) {
                        try {
                            int planColor = Color.parseColor(docColorHex);
                            
                            // Store color in plan object for future use
                            plan.setColor(docColorHex);
                            
                            // Apply color to UI elements
                            holder.planIconImageView.setColorFilter(planColor);
                            holder.planNameTextView.setTextColor(planColor);
                            
                            // If this is the selected item, update selection stroke
                            if (holder.getAdapterPosition() == selectedPosition) {
                                setSelectedCardStroke(holder, plan);
                            }
                        } catch (IllegalArgumentException e) {
                            Log.e(TAG, "Invalid color format from Firestore: " + docColorHex, e);
                            applyDefaultColor(holder, plan);
                        }
                    } else {
                        applyDefaultColor(holder, plan);
                    }
                } else {
                    applyDefaultColor(holder, plan);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching plan color: " + e.getMessage());
                applyDefaultColor(holder, plan);
            });
    }
    
    /**
     * Apply default color based on tier when Firestore data is unavailable
     */
    private void applyDefaultColor(SubscriptionViewHolder holder, SubscriptionPlan plan) {
        int colorResId;
        
        // Get tier, either from plan or infer from name
        String tier = plan.getTier();
        if (tier == null || tier.isEmpty()) {
            tier = plan.resolveTier();
        }
        
        // Select color based on tier
        switch (tier) {
            case "Silver":
                colorResId = R.color.silver;
                break;
            case "Gold":
                colorResId = R.color.gold;
                break;
            case "Diamond":
                colorResId = R.color.platinum;
                break;
            default: // Bronze
                colorResId = R.color.bronze;
                break;
        }
        
        int planColor = ContextCompat.getColor(context, colorResId);
        holder.planIconImageView.setColorFilter(planColor);
        holder.planNameTextView.setTextColor(planColor);
    }
    
    /**
     * Set card stroke color for selected card
     */
    private void setSelectedCardStroke(SubscriptionViewHolder holder, SubscriptionPlan plan) {
        // Only proceed if we have a MaterialCardView
        if (!(holder.cardView instanceof MaterialCardView)) {
            return;
        }
        
        MaterialCardView materialCardView = (MaterialCardView) holder.cardView;
        int strokeColor;
        
        // Use plan color if available
        String colorHex = plan.getColor();
        if (colorHex != null && !colorHex.isEmpty()) {
            try {
                strokeColor = Color.parseColor(colorHex);
                materialCardView.setStrokeColor(strokeColor);
                return;
            } catch (IllegalArgumentException e) {
                // Fall back to default colors if parsing fails
            }
        }
        
        // Use default color based on tier
        String tier = plan.getTier();
        if (tier == null || tier.isEmpty()) {
            tier = plan.resolveTier();
        }
        
        int colorResId;
        switch (tier) {
            case "Silver":
                colorResId = R.color.silver;
                break;
            case "Gold":
                colorResId = R.color.gold;
                break;
            case "Diamond":
                colorResId = R.color.platinum;
                break;
            default: // Bronze
                colorResId = R.color.bronze;
                break;
        }
        
        strokeColor = ContextCompat.getColor(context, colorResId);
        materialCardView.setStrokeColor(strokeColor);
    }

    /**
     * Safely check if a resource ID is valid without throwing exceptions
     */
    private boolean isValidDrawableResource(Context context, int resourceId) {
        try {
            // This is a safe way to check if a drawable resource exists
            return context.getDrawable(resourceId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Set a default icon based on tier when the specified resource doesn't exist
     */
    private void setDefaultIcon(SubscriptionViewHolder holder, String tier) {
        try {
            int iconRes;
            
            // Select default icon based on tier
            switch (tier) {
                case "Silver":
                    iconRes = R.drawable.ic_silver;
                    break;
                case "Gold":
                    iconRes = R.drawable.ic_gold;
                    break;
                case "Diamond":
                    iconRes = R.drawable.ic_platinum;
                    break;
                default: // Bronze
                    iconRes = R.drawable.ic_bronze;
                    break;
            }
            
            // Apply the icon resource - should always be valid
            holder.planIconImageView.setImageResource(iconRes);
        } catch (Exception e) {
            // In case of any errors, use Android's built-in icon as last resort
            Log.e(TAG, "Error setting fallback icon", e);
            try {
                holder.planIconImageView.setImageResource(android.R.drawable.ic_menu_info_details);
            } catch (Exception ex) {
                // If even this fails, just clear the image
                holder.planIconImageView.setImageDrawable(null);
            }
        }
    }

    @Override
    public int getItemCount() {
        return subscriptionPlans.size();
    }

    static class SubscriptionViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView planNameTextView;
        TextView planPriceTextView;
        TextView planFeaturesTextView;
        TextView planTierTextView = null; // Will not look for this view
        TextView currentPlanBadge = null; // Will not look for this view
        ImageView planIconImageView;

        public SubscriptionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView; // Cast the root view which is a MaterialCardView
            planNameTextView = itemView.findViewById(R.id.planName);
            planPriceTextView = itemView.findViewById(R.id.planPrice);
            planFeaturesTextView = itemView.findViewById(R.id.planFeatures);
            planIconImageView = itemView.findViewById(R.id.planIcon);
            
            // We won't look for these optional views anymore
            // They will remain null, and we'll handle that in onBindViewHolder
        }
    }
}