package com.business.techassist.subscription;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SubscriptionActivity extends AppCompatActivity implements SubscriptionAdapter.OnSubscriptionSelectedListener {

    private RecyclerView subscriptionRecyclerView;
    private MaterialButton backButton;
    private SubscriptionAdapter adapter;
    private List<SubscriptionPlan> subscriptionPlans = new ArrayList<>();
    private String currentPlan = "Basic"; // Default to Basic
    private final int PLAN_BASIC = 0;
    private final int PLAN_STANDARD = 1; 
    private final int PLAN_PREMIUM = 2;
    private final int PLAN_BUSINESS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        // Get current plan if provided
        if (getIntent().hasExtra("CURRENT_PLAN")) {
            currentPlan = getIntent().getStringExtra("CURRENT_PLAN");
        }

        initializeViews();
        setupSubscriptionPlans();
        setupRecyclerView();
        setupListeners();
        
        // Check if we received a selected plan from PlanDetailsActivity
        if (getIntent().hasExtra("selected_plan")) {
            String selectedPlan = getIntent().getStringExtra("selected_plan");
            for (SubscriptionPlan plan : subscriptionPlans) {
                if (plan.getName().equals(selectedPlan)) {
                    // Check if this is a downgrade
                    if (isPlanDowngrade(plan.getName())) {
                        Toast.makeText(this, "You cannot downgrade from " + currentPlan + " to " + plan.getName(), 
                                Toast.LENGTH_LONG).show();
                    } else {
                        showConfirmationDialog(plan);
                    }
                    break;
                }
            }
        }
    }

    private void initializeViews() {
        subscriptionRecyclerView = findViewById(R.id.subscriptionRecyclerView);
        backButton = findViewById(R.id.backButton);
    }

    private void showConfirmationDialog(SubscriptionPlan plan) {
        // Check if plan is active
        if (!plan.isActive()) {
            Toast.makeText(this, "The " + plan.getName() + " plan is currently unavailable.", Toast.LENGTH_LONG).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Subscription");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_subscription, null);
        builder.setView(dialogView);

        TextView planName = dialogView.findViewById(R.id.confirmPlanName);
        TextView planPrice = dialogView.findViewById(R.id.confirmPlanPrice);
        TextView planFeatures = dialogView.findViewById(R.id.confirmPlanFeatures);
        ImageView planIcon = dialogView.findViewById(R.id.confirmPlanIcon);
        MaterialButton confirmButton = dialogView.findViewById(R.id.confirmButton);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelButton);
        
        // Get payment method radio group
        RadioGroup paymentMethodGroup = dialogView.findViewById(R.id.paymentMethodGroup);

        planName.setText(plan.getName());
        planPrice.setText(plan.getPrice());
        
        // Convert features list to a displayable string
        StringBuilder featuresBuilder = new StringBuilder();
        List<String> featuresList = plan.getFeatures();
        if (featuresList != null && !featuresList.isEmpty()) {
            for (String feature : featuresList) {
                featuresBuilder.append(feature).append("\n");
            }
            // Remove the last newline
            if (featuresBuilder.length() > 0) {
                featuresBuilder.setLength(featuresBuilder.length() - 1);
            }
            planFeatures.setText(featuresBuilder.toString());
        } else {
            planFeatures.setText("No features available");
        }
        
        // Set consistent icon based on plan name
        switch (plan.getName()) {
            case "Standard":
                planIcon.setImageResource(R.drawable.ic_silver);
                break;
            case "Premium":
                planIcon.setImageResource(R.drawable.ic_gold);
                break;
            case "Business":
                planIcon.setImageResource(R.drawable.ic_platinum);
                break;
            default: // Basic or fallback
                planIcon.setImageResource(R.drawable.ic_bronze);
                break;
        }
        
        AlertDialog dialog = builder.create();
        
        // Apply color from Firestore if available in the plan object
        String colorHex = plan.getColor();
        if (colorHex != null && !colorHex.isEmpty()) {
            try {
                int planColor = Color.parseColor(colorHex);
                // Apply color to icon and buttons
                planIcon.setColorFilter(planColor);
                confirmButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(planColor));
                confirmButton.setTextColor(Color.WHITE);
                cancelButton.setTextColor(planColor);
                cancelButton.setStrokeColor(android.content.res.ColorStateList.valueOf(planColor));
            } catch (IllegalArgumentException e) {
                Log.e("SubscriptionActivity", "Invalid color format: " + colorHex, e);
                // Fallback to default styling
                applyDefaultButtonStyling(confirmButton, cancelButton, planIcon, plan.getName());
            }
        } else {
            // If color not available in the plan object, fetch it from Firestore
            FirebaseFirestore.getInstance().collection("Subscriptions").document(plan.getName())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String docColorHex = document.getString("color");
                        if (docColorHex != null && !docColorHex.isEmpty()) {
                            try {
                                int planColor = Color.parseColor(docColorHex);
                                // Store color in plan object for future use
                                plan.setColor(docColorHex);
                                // Apply color to icon and buttons
                                planIcon.setColorFilter(planColor);
                                confirmButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(planColor));
                                confirmButton.setTextColor(Color.WHITE);
                                cancelButton.setTextColor(planColor);
                                cancelButton.setStrokeColor(android.content.res.ColorStateList.valueOf(planColor));
                            } catch (IllegalArgumentException e) {
                                Log.e("SubscriptionActivity", "Invalid color format: " + docColorHex, e);
                                // Fallback to default styling
                                applyDefaultButtonStyling(confirmButton, cancelButton, planIcon, plan.getName());
                            }
                        } else {
                            // No color in Firestore, use defaults
                            applyDefaultButtonStyling(confirmButton, cancelButton, planIcon, plan.getName());
                        }
                    } else {
                        // Document doesn't exist, use defaults
                        applyDefaultButtonStyling(confirmButton, cancelButton, planIcon, plan.getName());
                    }
                })
                .addOnFailureListener(e -> {
                    // Error fetching from Firestore, use defaults
                    Log.e("SubscriptionActivity", "Error fetching plan color: " + e.getMessage());
                    applyDefaultButtonStyling(confirmButton, cancelButton, planIcon, plan.getName());
                });
        }
        
        dialog.show();

        confirmButton.setOnClickListener(v -> {
            // Get selected payment method
            int selectedId = paymentMethodGroup.getCheckedRadioButtonId();
            String paymentMethod = "Credit Card";
            
            if (selectedId == R.id.paypalRadio) {
                paymentMethod = "PayPal";
            } else if (selectedId == R.id.gcashRadio) {
                paymentMethod = "GCash";
            } else if (selectedId == R.id.mayaRadio) {
                paymentMethod = "Maya";
            }
            
            // Pass the selected payment method to processSubscription
            processSubscription(plan, paymentMethod);
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
    }
    
    /**
     * Apply default button styling based on plan name when Firestore color is not available
     */
    private void applyDefaultButtonStyling(MaterialButton confirmButton, MaterialButton cancelButton, 
                                         ImageView planIcon, String planName) {
        int color;
        
        switch (planName) {
            case "Standard":
                color = ContextCompat.getColor(this, R.color.silver);
                break;
            case "Premium":
                color = ContextCompat.getColor(this, R.color.gold);
                break;
            case "Business":
                color = ContextCompat.getColor(this, R.color.platinum);
                break;
            default: // Basic
                color = ContextCompat.getColor(this, R.color.bronze);
                break;
        }
        
        // Apply to UI elements
        planIcon.setColorFilter(color);
        confirmButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        confirmButton.setTextColor(Color.WHITE);
        cancelButton.setTextColor(color);
        cancelButton.setStrokeColor(android.content.res.ColorStateList.valueOf(color));
    }

    private void processSubscription(SubscriptionPlan plan, String paymentMethod) {
        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing your subscription...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to subscribe", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Add one month to current date for subscription end date
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1);

        Map<String, Object> subscriptionData = new HashMap<>();
        subscriptionData.put("plan", plan.getName());
        subscriptionData.put("startDate", new Timestamp(Calendar.getInstance().getTime()));
        subscriptionData.put("endDate", new Timestamp(calendar.getTime()));
        subscriptionData.put("status", "Active");
        subscriptionData.put("paymentMethod", paymentMethod);
        subscriptionData.put("price", plan.getPrice());
        subscriptionData.put("autoRenew", true);
        
        // Add 4 random digits as the last 4 digits of credit card
        if (paymentMethod.equals("Credit Card")) {
            // Generate random 4 digits
            int last4 = (int) (Math.random() * 9000) + 1000;
            subscriptionData.put("cardLast4", String.valueOf(last4));
        }

        // Get current user data to update
        db.collection("Users").document(userId)
            .get(Source.SERVER)  // Use server data to avoid caching issues
            .addOnSuccessListener(documentSnapshot -> {
                Map<String, Object> userData = new HashMap<>();
                userData.put("SubscriptionPlan", plan.getName());
                userData.put("SubscriptionStatus", "Active");
                userData.put("PaymentType", paymentMethod);
                
                // If credit card, store the last 4 digits
                if (paymentMethod.equals("Credit Card") && subscriptionData.containsKey("cardLast4")) {
                    userData.put("CardLast4", subscriptionData.get("cardLast4"));
                }

                // Update user document
                db.collection("Users").document(userId)
                    .update(userData)
                    .addOnSuccessListener(aVoid -> {
                        // Store subscription history in a subcollection
                        db.collection("Users").document(userId)
                            .collection("Subscriptions")
                            .add(subscriptionData)
                            .addOnSuccessListener(documentReference -> {
                                progressDialog.dismiss();
                                
                                // Add notification for the subscription
                                Map<String, Object> notificationData = new HashMap<>();
                                notificationData.put("title", "Subscription Activated");
                                notificationData.put("body", "Your " + plan.getName() + " subscription is now active");
                                notificationData.put("timestamp", FieldValue.serverTimestamp());
                                notificationData.put("type", "subscription");
                                notificationData.put("read", false);

                                db.collection("Users").document(userId)
                                    .collection("Notifications")
                                    .add(notificationData)
                                    .addOnSuccessListener(notifRef -> {
                                        // Send push notification
                                        sendPushNotification(userId, plan.getName());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("SubscriptionActivity", "Error adding notification", e);
                                    });
                                
                                // Show success dialog
                                showSuccessDialog(plan, "Your subscription has been successfully activated!");
                                
                                // Set result to OK to indicate changes
                                setResult(RESULT_OK);
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(SubscriptionActivity.this, "Error recording subscription history", Toast.LENGTH_SHORT).show();
                                Log.e("SubscriptionActivity", "Error recording subscription", e);
                            });
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(SubscriptionActivity.this, "Error updating user data", Toast.LENGTH_SHORT).show();
                        Log.e("SubscriptionActivity", "Error updating user data", e);
                    });
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(SubscriptionActivity.this, "Error getting user data", Toast.LENGTH_SHORT).show();
                Log.e("SubscriptionActivity", "Error getting user data", e);
            });
    }
    
    // Delegate to the new method that includes payment method
    private void processSubscription(SubscriptionPlan plan) {
        processSubscription(plan, "Credit Card"); // Default to Credit Card
    }

    private void showSuccessDialog(SubscriptionPlan plan, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Subscription Successful");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_subscription_success, null);
        builder.setView(dialogView);

        TextView successMessage = dialogView.findViewById(R.id.successMessage);
        MaterialButton doneButton = dialogView.findViewById(R.id.doneButton);

        successMessage.setText(message);

        AlertDialog dialog = builder.create();
        dialog.show();

        doneButton.setOnClickListener(v -> {
            dialog.dismiss();
            setResult(RESULT_OK);
            finish();
        });
    }

    private void setupSubscriptionPlans() {
        // Clear any existing plans
        subscriptionPlans.clear();
        
        SubscriptionManager manager = new SubscriptionManager(this);
        manager.loadSubscriptionPlans(new SubscriptionManager.SubscriptionPlansCallback() {
            @Override
            public void onPlansLoaded(List<SubscriptionPlan> plans) {
                if (plans != null && !plans.isEmpty()) {
                    subscriptionPlans.addAll(plans);
                    
                    // Sort plans by tier rank (if needed)
                    sortPlansByTier();
                    
                    // Check for selectability based on current plan
                    markSelectablePlans();
                    
                    // Update the adapter
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    // Show error to user if no plans are available
                    Toast.makeText(SubscriptionActivity.this, 
                            "Unable to load subscription plans. Please try again later.", 
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    /**
     * Sort plans by tier rank from lowest to highest using a simpler approach
     */
    private void sortPlansByTier() {
        // Simple hard-coded ordering for standard plans - safer than using rankings
        subscriptionPlans.sort((plan1, plan2) -> {
            return getSimplePlanOrder(plan1.getName()) - getSimplePlanOrder(plan2.getName());
        });
    }
    
    /**
     * Get a simple order value for standard plans without complex Firestore lookups
     */
    private int getSimplePlanOrder(String planName) {
        // Simple, reliable ordering for standard plans
        switch (planName) {
            case "Basic": return 0;
            case "Standard": return 1;
            case "Premium": return 2;
            case "Business": return 3;
            // For any custom plans, place them in the middle by default
            default: return 2;
        }
    }
    
    /**
     * Mark which plans are selectable based on current plan
     */
    private void markSelectablePlans() {
        int currentRank = getSimplePlanOrder(currentPlan);
        
        for (SubscriptionPlan plan : subscriptionPlans) {
            // If plan is lower than current, it's not selectable (no downgrades)
            int planRank = getSimplePlanOrder(plan.getName());
            
            // Set selectable if:
            // 1. The plan is higher rank than current (upgrade), OR
            // 2. It's the same plan (can renew current plan)
            boolean isSelectable = planRank >= currentRank && plan.isActive();
            plan.setSelectable(isSelectable);
        }
    }
    
    private boolean isPlanDowngrade(String selectedPlan) {
        return getSimplePlanOrder(selectedPlan) < getSimplePlanOrder(currentPlan);
    }

    private void sendPushNotification(String userId, String planName) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("to", "/topics/" + userId); // Or use FCM tokens
            payload.put("priority", "high");

            JSONObject notification = new JSONObject();
            notification.put("title", "Subscription Activated");
            notification.put("body", "You've successfully subscribed to " + planName);

            payload.put("notification", notification);

            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, payload.toString());

            Request request = new Request.Builder()
                    .url("https://fcm.googleapis.com/fcm/send")
                    .post(body)
                    .addHeader("Authorization", "key=YOUR_SERVER_KEY")
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("FCM", "Notification failed", e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    Log.d("FCM", "Notification sent");
                }
            });
        } catch (Exception e) {
            Log.e("FCM", "Error creating notification", e);
        }
    }

    private void setupRecyclerView() {
        adapter = new SubscriptionAdapter(this, subscriptionPlans, currentPlan, this);
        subscriptionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        subscriptionRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    public void onSubscriptionSelected(SubscriptionPlan plan) {
        // First check if the plan is active
        if (!plan.isActive()) {
            Toast.makeText(this, 
                "The " + plan.getName() + " plan is currently unavailable.", 
                Toast.LENGTH_LONG).show();
            return;
        }
        
        // Then check if this would be a downgrade from current plan
        if (isPlanDowngrade(plan.getName())) {
            Toast.makeText(this, 
                "You cannot downgrade from " + currentPlan + " to " + plan.getName(), 
                Toast.LENGTH_LONG).show();
            return;
        }
        
        // Proceed with subscription if it's an upgrade or same level
        Intent intent = new Intent(this, PlanDetailsActivity.class);
        intent.putExtra("plan_name", plan.getName());
        
        // Pass only the plan name - other details will be fetched from Firestore in PlanDetailsActivity
        startActivity(intent);
    }
}
