package com.business.techassist.subscription;

import java.util.List;

/**
 * Model class representing a subscription plan from Firestore.
 */
public class SubscriptionPlan {
    private String name;
    private String price;
    private List<String> features;
    private int iconRes;
    private boolean selectable = true; // Default to true
    private String status = "active"; // Default status is active
    private String color; // Color hex code from Firestore
    private String tier; // Bronze, Silver, Gold, Diamond
    private int xpReward; // XP reward for subscribing to this plan

    /**
     * Basic constructor with minimal required fields
     */
    public SubscriptionPlan(String name, String price, List<String> features, int iconRes) {
        this.name = name;
        this.price = price;
        this.features = features;
        this.iconRes = iconRes;
    }
    
    /**
     * Constructor with status field
     */
    public SubscriptionPlan(String name, String price, List<String> features, int iconRes, String status) {
        this.name = name;
        this.price = price;
        this.features = features;
        this.iconRes = iconRes;
        this.status = status;
    }
    
    /**
     * Constructor with status and color fields
     */
    public SubscriptionPlan(String name, String price, List<String> features, int iconRes, String status, String color) {
        this.name = name;
        this.price = price;
        this.features = features;
        this.iconRes = iconRes;
        this.status = status;
        this.color = color;
    }
    
    /**
     * Full constructor with all fields from Firestore
     */
    public SubscriptionPlan(String name, String price, List<String> features, int iconRes, 
                            String status, String color, String tier, int xpReward) {
        this.name = name;
        this.price = price;
        this.features = features;
        this.iconRes = iconRes;
        this.status = status;
        this.color = color;
        this.tier = tier;
        this.xpReward = xpReward;
    }

    // Getters
    public String getName() { return name; }
    public String getPrice() { return price; }
    public List<String> getFeatures() { return features; }
    public int getIconRes() { return iconRes; }
    public boolean isSelectable() { return selectable; }
    public String getStatus() { return status; }
    public boolean isActive() { return "active".equalsIgnoreCase(status); }
    public String getColor() { return color; }
    public String getTier() { return tier; }
    public int getXpReward() { return xpReward; }
    
    // Setters
    public void setSelectable(boolean selectable) { this.selectable = selectable; }
    public void setStatus(String status) { this.status = status; }
    public void setColor(String color) { this.color = color; }
    public void setTier(String tier) { this.tier = tier; }
    public void setXpReward(int xpReward) { this.xpReward = xpReward; }
    
    /**
     * Maps a plan name to its corresponding tier if tier is not already set
     * @return The tier for this plan
     */
    public String resolveTier() {
        if (tier != null && !tier.isEmpty()) {
            return tier;
        }
        
        // Default mapping if tier is not set
        switch (name) {
            case "Standard":
                return "Silver";
            case "Premium":
                return "Gold";
            case "Business":
                return "Diamond";
            default: // Basic
                return "Bronze";
        }
    }
}