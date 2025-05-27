package com.business.techassist;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.business.techassist.admin_utils.AdminAdapter;
import com.business.techassist.menucomponents.messages.menu_message;
import com.business.techassist.admin_utils.SQL_AdminModel;
import com.business.techassist.notif.NotificationActivity;
import com.business.techassist.remote.RemoteSupportActivity;
import com.business.techassist.subscription.BenefitsUtil;
import com.business.techassist.subscription.SubscriptionActivity;
import com.business.techassist.subscription.SubscriptionUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.business.techassist.subscription.FeatureLockManager;

import android.widget.ArrayAdapter;

public class home extends Fragment {

    RecyclerView itView;
    TextView planFeatures, userPlan, userName;
    MaterialButton upgradeBtn, notifBtn, messageBtn, profileHomeBtn;
    MaterialAutoCompleteTextView sortDropdown;
    TextInputLayout sortDropdownLayout;
    ProgressBar progressBarCat,progressBarIT;
    // Quick Action Cards
    MaterialCardView remoteSupportCard, plansCard, storeCard, accountCard;

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    
    // Track current tier
    private String instanceCurrentTier = "";

    // Track if we need a refresh
    private boolean needsRefresh = false;

    // Track the current sort option
    private int currentSortOption = 0; // 0: None, 1: Rating, 2: Experience, 3: Jobs
    
    // Firestore listener for real-time updates
    private com.google.firebase.firestore.ListenerRegistration adminListener;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // Current list of admins
    private List<SQL_AdminModel> currentAdminList = new ArrayList<>();
    private AdminAdapter adminAdapter;

    public home() {
        // Required empty public constructor
    }

    public static home newInstance(String param1, String param2) {
        home fragment = new home();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            SubscriptionUtils.checkSubscriptionExpiry(getActivity(), user.getUid());
        }

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        itView = view.findViewById(R.id.itView);
        planFeatures = view.findViewById(R.id.planFeatures);
        userPlan = view.findViewById(R.id.userPlan);
        upgradeBtn = view.findViewById(R.id.upgradeBtn);
        notifBtn = view.findViewById(R.id.notifBtn);
//        progressBarIT = view.findViewById(R.id.progressBarIT);
        userName = view.findViewById(R.id.userName);
        messageBtn = view.findViewById(R.id.messageBtn);
        profileHomeBtn = view.findViewById(R.id.profileHomeBtn);
        // Get sort dropdown views
        sortDropdown = view.findViewById(R.id.sortDropdown);
        sortDropdownLayout = view.findViewById(R.id.sortDropdownLayout);
        
        // Find quick action cards
        setupQuickActionCards(view);

       // progressBarIT.setVisibility(View.VISIBLE);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Set up the sort dropdown
        setupSortDropdown();

        messageBtn.setOnClickListener(v ->{
            // Direct access to messages without subscription check
            startActivity(new Intent(getActivity(), menu_message.class));
        });

        profileHomeBtn.setOnClickListener( v ->{
            startActivity(new Intent(getActivity(), profileHome.class));
        });

        upgradeBtn.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                
                // Get current subscription plan directly from Firestore
                FirebaseFirestore.getInstance().collection("Users").document(userId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists() && isAdded() && getActivity() != null) {
                            // Check both possible field names for subscription plan
                            String currentPlan = document.getString("SubscriptionPlan");
                            
                            // Check alternate field name if primary field is empty
                            if (currentPlan == null || currentPlan.isEmpty()) {
                                currentPlan = document.getString("Subscriptions");
                            }
                            
                            // Default to Basic if not found
                            if (currentPlan == null || currentPlan.isEmpty()) {
                                currentPlan = "Basic";
                            }
                            
                            final String finalPlan = currentPlan;
                            
                            // Get tier directly from Firestore subscription document
                            FirebaseFirestore.getInstance().collection("Subscriptions").document(finalPlan)
                                .get()
                                .addOnSuccessListener(planDoc -> {
                                    String currentTier = null;
                                    
                                    if (planDoc.exists()) {
                                        // Get tier from the subscription document
                                        currentTier = planDoc.getString("tier");
                                    }
                                    
                                    // Fallback if tier not found in document
                                    if (currentTier == null || currentTier.isEmpty()) {
                                        // Map subscription plan to appropriate tier for display
                                        switch (finalPlan) {
                                            case "Standard":
                                                currentTier = "Silver";
                                                break;
                                            case "Premium":
                                                currentTier = "Gold";
                                                break;
                                            case "Business":
                                                currentTier = "Diamond";
                                                break;
                                            default: // Basic
                                                currentTier = "Bronze";
                                                break;
                                        }
                                    }
                                    
                                    // Log the mapping for debugging
                                    Log.d("TechAssist", "Mapping plan to tier: " + finalPlan + " -> " + currentTier);
                                    
                                    // Show the subscription activity with the correct tier based on subscription
                                    Intent intent = new Intent(getActivity(), SubscriptionActivity.class);
                                    intent.putExtra("CURRENT_PLAN", finalPlan);
                                    intent.putExtra("CURRENT_TIER", currentTier);
                                    startActivity(intent);
                                })
                                .addOnFailureListener(e -> {
                                    // Fallback to manual mapping on error
                                    Log.e("TechAssist", "Error fetching subscription data: " + e.getMessage());
                                    
                                    // Map subscription plan to appropriate tier for display (fallback)
                                    String fallbackTier;
                                    switch (finalPlan) {
                                        case "Standard":
                                            fallbackTier = "Silver";
                                            break;
                                        case "Premium":
                                            fallbackTier = "Gold";
                                            break;
                                        case "Business":
                                            fallbackTier = "Diamond";
                                            break;
                                        default: // Basic
                                            fallbackTier = "Bronze";
                                            break;
                                    }
                                    
                                    // Launch with fallback values
                                    Intent intent = new Intent(getActivity(), SubscriptionActivity.class);
                                    intent.putExtra("CURRENT_PLAN", finalPlan);
                                    intent.putExtra("CURRENT_TIER", fallbackTier);
                                    startActivity(intent);
                                });
                        } else {
                            // Document doesn't exist or fragment is detached
                            if (isAdded() && getActivity() != null) {
                                startActivity(new Intent(getActivity(), SubscriptionActivity.class));
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // On failure, just go to subscription activity
                        if (isAdded() && getActivity() != null) {
                            Log.e("TechAssist", "Error fetching subscription data: " + e.getMessage());
                            startActivity(new Intent(getActivity(), SubscriptionActivity.class));
                        }
                    });
            } else {
                // User not logged in
                if (isAdded() && getActivity() != null) {
                    startActivity(new Intent(getActivity(), SubscriptionActivity.class));
                }
            }
        });

        notifBtn.setOnClickListener(v ->{
            startActivity(new Intent(getActivity(), NotificationActivity.class));
        });

        // Set up the technician experts cards with real-time updates
        setupRealtimeAdminUpdates();
        
        // Also fetch user name and plan
        fetchUserName();
        fetchUserPlan();

        itView.setNestedScrollingEnabled(false);
        return view;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Remove Firestore listener when view is destroyed
        if (adminListener != null) {
            adminListener.remove();
            adminListener = null;
        }
    }
    
    private void setupQuickActionCards(View view) {
        try {
            // Find the grid layout
            GridLayout gridLayout = view.findViewById(R.id.action_grid_layout);
            if (gridLayout == null) {
                Log.e("TechAssist", "Grid layout not found");
                return;
            }
            
            // Check if the grid has enough children
            int childCount = gridLayout.getChildCount();
            if (childCount < 4) {
                Log.e("TechAssist", "Grid layout has insufficient children: " + childCount);
                return;
            }
            
            // Set up Remote Support card (first card)
            View firstChild = gridLayout.getChildAt(0);
            if (firstChild instanceof MaterialCardView) {
                remoteSupportCard = (MaterialCardView) firstChild;
                remoteSupportCard.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), RemoteSupportActivity.class);
                    startActivity(intent);
                });
            } else {
                Log.e("TechAssist", "First child is not a MaterialCardView: " + firstChild.getClass().getSimpleName());
            }
            
            // Set up Plans card (second card)
            View secondChild = gridLayout.getChildAt(1);
            if (secondChild instanceof MaterialCardView) {
                plansCard = (MaterialCardView) secondChild;
                plansCard.setOnClickListener(v -> {
                    // Get user's current plan to show details of that plan first
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        FirebaseFirestore.getInstance().collection("Users").document(currentUser.getUid())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (!isAdded() || getActivity() == null) return; // Check if fragment is still attached
                                
                                String currentPlan = documentSnapshot.getString("SubscriptionPlan");
                                // Default to Premium if no plan is found
                                if (currentPlan == null || currentPlan.isEmpty()) {
                                    currentPlan = "Premium";
                                }
                                
                                Intent intent = new Intent(getActivity(), 
                                        com.business.techassist.subscription.PlanDetailsActivity.class);
                                intent.putExtra("plan_name", currentPlan);
                                startActivity(intent);
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded() || getActivity() == null) return; // Check if fragment is still attached
                                
                                // In case of error, just show the standard subscription page
                                Intent intent = new Intent(getActivity(), 
                                        com.business.techassist.subscription.SubscriptionActivity.class);
                                startActivity(intent);
                            });
                    } else {
                        // If not logged in, go to standard subscription page
                        Intent intent = new Intent(getActivity(), 
                                com.business.techassist.subscription.SubscriptionActivity.class);
                        startActivity(intent);
                    }
                });
            } else {
                Log.e("TechAssist", "Second child is not a MaterialCardView: " + secondChild.getClass().getSimpleName());
            }
            
            // Set up Store card (third card)
            View thirdChild = gridLayout.getChildAt(2);
            if (thirdChild instanceof MaterialCardView) {
                storeCard = (MaterialCardView) thirdChild;
                storeCard.setOnClickListener(v -> {
                    // Navigate to the shop fragment
                    if (isAdded() && getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.loadFragment(new shop());
                    }
                });
            } else {
                Log.e("TechAssist", "Third child is not a MaterialCardView: " + thirdChild.getClass().getSimpleName());
            }
            
            // Set up Account card (fourth card)
            View fourthChild = gridLayout.getChildAt(3);
            if (fourthChild instanceof MaterialCardView) {
                accountCard = (MaterialCardView) fourthChild;
                accountCard.setOnClickListener(v -> {
                    if (isAdded() && getActivity() != null) {
                        Intent intent = new Intent(getActivity(), profileHome.class);
                        startActivity(intent);
                    }
                });
            } else {
                Log.e("TechAssist", "Fourth child is not a MaterialCardView: " + fourthChild.getClass().getSimpleName());
            }
        } catch (Exception e) {
            Log.e("TechAssist", "Error setting up quick action cards", e);
        }
    }

    private void fetchUserName() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();

            db.collection("Users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("Name");
                            String firstName = (fullName != null && !fullName.isEmpty()) ? fullName.split(" ")[0] : "User";
                            userName.setText(getGreeting() + " " + firstName + "!");
                        } else {
                            userName.setText(getGreeting() + " User!");
                        }
                    })
                    .addOnFailureListener(e -> {
                        userName.setText(getGreeting() + " User!");
                        e.printStackTrace();
                    });
        } else {
            userName.setText(getGreeting() + " Guest!");
        }
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            return "Good Morning,";
        } else if (hour >= 12 && hour < 18) {
            return "Good Afternoon,";
        } else {
            return "Good Evening,";
        }
    }

    private void setUpITExperts() {
        new Thread(() -> {
            try {
                Context context = getActivity();
                if (context == null) return;
                
                // Create a new list to hold admins
                List<SQL_AdminModel> adminList = new ArrayList<>();
                
                // Reference to Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                
                // Query for all users with Role = Admin
                db.collection("Users")
                    .whereEqualTo("Role", "Admin")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Log.d("TechAssist", "Found " + task.getResult().size() + " admins in Firestore");
                            
                            for (DocumentSnapshot document : task.getResult().getDocuments()) {
                                try {
                                    String adminID = document.getId();
                                    Log.d("TechAssist", "Processing admin ID: " + adminID);
                                    
                                    // Get name (try both formats)
                                    String name = document.getString("name");
                                    if (name == null) name = document.getString("name");
                                    if (name == null) name = "Admin";
                                    
                                    // Get rating
                                    String ratings = "5"; // default
                                    if (document.contains("rating")) {
                                        ratings = String.valueOf(document.get("rating"));
                                    } else if (document.contains("rating")) {
                                        ratings = String.valueOf(document.get("rating"));
                                    }
                                    
                                    // Get specialization
                                    String specialized = "IT Support"; // default
                                    Object specObj = document.get("specialization");
                                    if (specObj == null) specObj = document.get("specialization");
                                    
                                    if (specObj instanceof java.util.List) {
                                        List<Object> specList = (List<Object>) specObj;
                                        if (!specList.isEmpty()) {
                                            specialized = String.valueOf(specList.get(0));
                                        }
                                    } else if (specObj != null) {
                                        specialized = String.valueOf(specObj);
                                    }
                                    
                                    // Get experience
                                    int yearsExp = 3; // default
                                    Object expObj = document.get("experience");
                                    if (expObj == null) expObj = document.get("experience");
                                    if (expObj instanceof Number) {
                                        yearsExp = ((Number) expObj).intValue();
                                    }
                                    
                                    // Get availability
                                    String availability = "available"; // default
                                    if (document.contains("availability")) {
                                        availability = document.getString("availability");
                                    } else if (document.contains("availability")) {
                                        availability = document.getString("availability");
                                    }
                                    
                                    // Get status
                                    String status = "Online"; // default
                                    if (document.contains("status")) {
                                        status = document.getString("status");
                                    } else if (document.contains("status")) {
                                        status = document.getString("status");
                                    }
                                    
                                    // Get completedJobs count (for sorting)
                                    int completedJobs = 0;
                                    if (document.contains("completedJobs")) {
                                        Object jobsObj = document.get("completedJobs");
                                        if (jobsObj instanceof Number) {
                                            completedJobs = ((Number) jobsObj).intValue();
                                        }
                                    }
                                    
                                    // Create admin model
                                    SQL_AdminModel admin = new SQL_AdminModel();
                                    admin.setAdminID(adminID);
                                    admin.setName(name);
                                    admin.setRatings(ratings);
                                    admin.setSpecialized(specialized);
                                    admin.setYearsExp(yearsExp);
                                    admin.setAvailability(availability);
                                    admin.setStatus(status);
                                    admin.setCompletedJobs(completedJobs); // Set completed jobs
                                    
                                    // Try to get profile image URL from document
                                    String imageUrl = null;
                                    if (document.contains("profileImage")) {
                                        imageUrl = document.getString("profileImage");
                                    } else if (document.contains("ProfileImage")) {
                                        imageUrl = document.getString("ProfileImage");
                                    } else if (document.contains("photoURL")) {
                                        imageUrl = document.getString("photoURL");
                                    } else if (document.contains("photoUrl")) {
                                        imageUrl = document.getString("photoUrl");
                                    }
                                    
                                    // Set default image for now
                                    admin.setImage(getBytesFromDrawable(R.drawable.user_icon));
                                    
                                    // Add to list
                                    adminList.add(admin);
                                    Log.d("TechAssist", "Added admin: " + name + 
                                        ", availability: " + availability + 
                                        ", status: " + status +
                                        ", completedJobs: " + completedJobs);
                                } catch (Exception e) {
                                    Log.e("TechAssist", "Error processing admin document", e);
                                }
                            }
                            
                            // Update UI on main thread
                            Activity activity = getActivity();
                            if (activity != null && !activity.isFinishing()) {
                                activity.runOnUiThread(() -> {
                                    try {
                                        // Show all admins regardless of availability
                                        Log.d("TechAssist", "Showing all " + adminList.size() + " admins regardless of availability");
                                        
                                        // Set adapter with all admins
                    AdminAdapter adapter = new AdminAdapter(adminList, context);
                                        LinearLayoutManager layoutManager = new LinearLayoutManager(
                                            context, LinearLayoutManager.HORIZONTAL, false);
                                        itView.setLayoutManager(layoutManager);
                    itView.setAdapter(adapter);
                                        // Add this line to ensure horizontal scrolling works
                                        itView.setHasFixedSize(false);
                                        itView.setNestedScrollingEnabled(false);
                                        Log.d("TechAssist", "Set adapter with " + adminList.size() + " admins from Firestore");
                                        
                                        // Store the admin list for sorting
                                        currentAdminList = new ArrayList<>(adminList);
                                        adminAdapter = adapter;
                                    } catch (Exception e) {
                                        Log.e("TechAssist", "Error setting adapter", e);
                                        
                                        // If there's an error, just set an empty adapter
                                        itView.setAdapter(new AdminAdapter(new ArrayList<>(), context));
                                    }
                                });
                            }
                        } else {
                            // Second query with alternate capitalization
                            db.collection("Users")
                                .whereEqualTo("Role", "Admin")
                                .get()
                                .addOnCompleteListener(alternateTask -> {
                                    if (alternateTask.isSuccessful() && alternateTask.getResult() != null && 
                                        !alternateTask.getResult().isEmpty()) {
                                        
                                        Log.d("TechAssist", "Found " + alternateTask.getResult().size() + 
                                            " admins in Firestore with alternate capitalization");
                                        
                                        // Process the results (similar to above)
                                        List<SQL_AdminModel> altAdminList = new ArrayList<>();
                                        
                                        for (DocumentSnapshot document : alternateTask.getResult().getDocuments()) {
                                            try {
                                                String adminID = document.getId();
                                                Log.d("TechAssist", "Processing admin ID: " + adminID);
                                                
                                                // Get basic info (similar to above)
                                                String name = document.getString("name");
                                                if (name == null) name = document.getString("Name");
                                                if (name == null) name = "Admin";
                                                
                                                String ratings = "5"; // default
                                                if (document.contains("rating")) {
                                                    ratings = String.valueOf(document.get("rating"));
                                                } else if (document.contains("Rating")) {
                                                    ratings = String.valueOf(document.get("Rating"));
                                                }
                                                
                                                String specialized = "IT Support"; // default
                                                Object specObj = document.get("specialization");
                                                if (specObj == null) specObj = document.get("Specialization");
                                                
                                                if (specObj instanceof java.util.List) {
                                                    List<Object> specList = (List<Object>) specObj;
                                                    if (!specList.isEmpty()) {
                                                        specialized = String.valueOf(specList.get(0));
                                                    }
                                                } else if (specObj != null) {
                                                    specialized = String.valueOf(specObj);
                                                }
                                                
                                                int yearsExp = 3; // default
                                                Object expObj = document.get("experience");
                                                if (expObj == null) expObj = document.get("Experience");
                                                if (expObj instanceof Number) {
                                                    yearsExp = ((Number) expObj).intValue();
                                                }
                                                
                                                String availability = "Available"; // default
                                                if (document.contains("availability")) {
                                                    availability = document.getString("availability");
                                                } else if (document.contains("Availability")) {
                                                    availability = document.getString("Availability");
                                                }
                                                
                                                String status = "Online"; // default
                                                if (document.contains("status")) {
                                                    status = document.getString("status");
                                                } else if (document.contains("Status")) {
                                                    status = document.getString("Status");
                                                }
                                                
                                                // Get completedJobs count (for sorting)
                                                int completedJobs = 0;
                                                if (document.contains("completedJobs")) {
                                                    Object jobsObj = document.get("completedJobs");
                                                    if (jobsObj instanceof Number) {
                                                        completedJobs = ((Number) jobsObj).intValue();
                                                    }
                                                } else if (document.contains("CompletedJobs")) {
                                                    Object jobsObj = document.get("CompletedJobs");
                                                    if (jobsObj instanceof Number) {
                                                        completedJobs = ((Number) jobsObj).intValue();
                                                    }
                                                }
                                                
                                                SQL_AdminModel admin = new SQL_AdminModel();
                                                admin.setAdminID(adminID);
                                                admin.setName(name);
                                                admin.setRatings(ratings);
                                                admin.setSpecialized(specialized);
                                                admin.setYearsExp(yearsExp);
                                                admin.setAvailability(availability);
                                                admin.setStatus(status);
                                                admin.setCompletedJobs(completedJobs); // Set completed jobs
                                                
                                                // Set default image
                                                admin.setImage(getBytesFromDrawable(R.drawable.user_icon));
                                                
                                                altAdminList.add(admin);
            } catch (Exception e) {
                                                Log.e("TechAssist", "Error processing admin with alternate capitalization", e);
                                            }
                                        }
                                        
                                        Activity activity = getActivity();
                                        if (activity != null && !activity.isFinishing()) {
                                            activity.runOnUiThread(() -> {
                                                try {
                                                    // Show all admins regardless of availability
                                                    Log.d("TechAssist", "Showing all " + altAdminList.size() + " admins (alt capitalization)");
                                                    
                                                    // Set adapter with all admins
                                                    AdminAdapter adapter = new AdminAdapter(altAdminList, context);
                                                    LinearLayoutManager layoutManager = new LinearLayoutManager(
                                                        context, LinearLayoutManager.HORIZONTAL, false);
                                                    itView.setLayoutManager(layoutManager);
                                                    itView.setAdapter(adapter);
                                                    // Add this line to ensure horizontal scrolling works
                                                    itView.setHasFixedSize(false);
                                                    itView.setNestedScrollingEnabled(false);
                                                    Log.d("TechAssist", "Set adapter with " + altAdminList.size() + 
                                                        " admins from Firestore (alternate capitalization)");
                                                    
                                                    // Store the admin list for sorting
                                                    currentAdminList = new ArrayList<>(altAdminList);
                                                    adminAdapter = adapter;
                                                } catch (Exception e) {
                                                    Log.e("TechAssist", "Error setting adapter for alternate capitalization", e);
                                                    
                                                    // If there's an error, just set an empty adapter
                                                    itView.setAdapter(new AdminAdapter(new ArrayList<>(), context));
                                                }
                                            });
                                        }
                                    } else {
                                        // Both queries failed, show empty state
                                        Activity activity = getActivity();
                                        if (activity != null && !activity.isFinishing()) {
                                            activity.runOnUiThread(() -> {
                                                Log.d("TechAssist", "No admins found in Firestore");
                                                itView.setAdapter(new AdminAdapter(new ArrayList<>(), context));
                                            });
                                        }
                                    }
                                });
                        }
                    });
            } catch (Exception e) {
                Log.e("TechAssist", "Error in setUpITExperts", e);
                Context context = getActivity();
                if (context != null) {
                    Activity activity = getActivity();
                    if (activity != null && !activity.isFinishing()) {
                        activity.runOnUiThread(() -> {
                            // On error, show empty list
                            itView.setAdapter(new AdminAdapter(new ArrayList<>(), context));
                        });
                    }
                }
            }
        }).start();
    }

    private byte[] getBytesFromDrawable(int drawableId) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), drawableId);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
        return stream.toByteArray();
    }

    private void fetchUserPlan() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("Users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Check both possible field names for subscription plan
                        String plan = documentSnapshot.getString("SubscriptionPlan");
                        
                        // Check alternate field name if primary is empty
                        if (plan == null || plan.isEmpty()) {
                            plan = documentSnapshot.getString("Subscriptions");
                        }
                        
                        // Set default if null or empty
                        if (plan == null || plan.isEmpty()) {
                            plan = "Basic";
                            // Update the user document with the default Basic plan
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("SubscriptionPlan", "Basic");
                            updates.put("Current Tier", "Bronze");
                            
                            db.collection("Users").document(currentUser.getUid())
                                .update(updates)
                                .addOnFailureListener(e -> Log.e("TechAssist", "Error setting default plan", e));
                        }
                        
                        final String finalPlan = plan;
                        
                        // Fetch plan details from Firestore
                        db.collection("Subscriptions").document(finalPlan)
                            .get()
                            .addOnSuccessListener(planDoc -> {
                                if (planDoc.exists()) {
                                    // Get tier from the subscription document
                                    String tier = planDoc.getString("tier");
                                    if (tier == null || tier.isEmpty()) {
                                        // Fallback mapping if tier not found
                                        switch (finalPlan) {
                                            case "Standard":
                                                tier = "Silver";
                                                break;
                                            case "Premium":
                                                tier = "Gold";
                                                break;
                                            case "Business":
                                                tier = "Diamond";
                                                break;
                                            default: // Basic
                                                tier = "Bronze";
                                                break;
                                        }
                                    }
                                    
                                    // Get features array
                                    List<String> featuresList = (List<String>) planDoc.get("features");
                                    
                                    // Update UI with the plan and corresponding tier
                                    updatePlanUI(finalPlan, tier, featuresList);
                                    
                                    // Log the subscription details
                                    Log.d("TechAssist", "User plan: " + finalPlan + " (Tier: " + tier + ")");
                                    if (featuresList != null) {
                                        Log.d("TechAssist", "Features count: " + featuresList.size());
                                    }
                                } else {
                                    // Plan document doesn't exist, use fallback mapping
                                    String defaultTier;
                                    switch (finalPlan) {
                                        case "Standard":
                                            defaultTier = "Silver";
                                            break;
                                        case "Premium":
                                            defaultTier = "Gold";
                                            break;
                                        case "Business":
                                            defaultTier = "Diamond";
                                            break;
                                        default: // Basic
                                            defaultTier = "Bronze";
                                            break;
                                    }
                                    // Use default features list
                                    updatePlanUI(finalPlan, defaultTier, null);
                                }
                            })
                            .addOnFailureListener(e -> {
                                // Use fallback mapping on error
                                String fallbackTier;
                                switch (finalPlan) {
                                    case "Standard":
                                        fallbackTier = "Silver";
                                        break;
                                    case "Premium":
                                        fallbackTier = "Gold";
                                        break;
                                    case "Business":
                                        fallbackTier = "Diamond";
                                        break;
                                    default: // Basic
                                        fallbackTier = "Bronze";
                                        break;
                                }
                                updatePlanUI(finalPlan, fallbackTier, null);
                                Log.e("TechAssist", "Error fetching plan details", e);
                            });
                    } else {
                        // No user document exists, create one with Basic plan
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("SubscriptionPlan", "Basic");
                        userData.put("Current Tier", "Bronze");
                        userData.put("Exp", 0);
                        userData.put("Total Exp", 0);
                        
                        db.collection("Users").document(currentUser.getUid())
                            .set(userData, SetOptions.merge())
                            .addOnFailureListener(e -> Log.e("TechAssist", "Error creating user document", e));
                        
                        // Default to Basic/Bronze with null features (will use defaults)
                        updatePlanUI("Basic", "Bronze", null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("home", "Error fetching user plan", e);
                    // Default to Basic/Bronze if error
                    updatePlanUI("Basic", "Bronze", null);
                });
    }

    private void updatePlanUI(String plan, String tier, List<String> featuresList) {
        if (!isAdded() || getActivity() == null || userPlan == null || planFeatures == null) {
            // Fragment is not attached to activity or views are null
            Log.e("TechAssist", "Cannot update plan UI - fragment detached or views are null");
            return;
        }
        
        // Store current tier for reference
        instanceCurrentTier = tier;
        
        // Set plan text
        userPlan.setText(plan + " (" + tier + ")");

        // Get plan color from Firestore
        FirebaseFirestore.getInstance().collection("Subscriptions").document(plan)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists() && isAdded() && getActivity() != null) {
                    // Get color from Firestore
                    String colorHex = document.getString("color");
        int textColor;
        int bgColor;
                    
                    if (colorHex != null && !colorHex.isEmpty()) {
                        try {
                            // Use color from Firestore
                            textColor = Color.parseColor(colorHex);
                            
                            // Use black background for all tiers
                            bgColor = Color.BLACK;
                        } catch (IllegalArgumentException e) {
                            // Fallback to tier-based colors if parsing fails
                            Log.e("TechAssist", "Invalid color format: " + colorHex, e);
                            textColor = getTierColor(tier);
                            bgColor = Color.BLACK;
                        }
                    } else {
                        // Fallback to tier-based colors if no color in Firestore
                        textColor = getTierColor(tier);
                        bgColor = Color.BLACK;
                    }
                    
                    // Apply styling
                    userPlan.setTextColor(textColor);
                    userPlan.setBackgroundColor(bgColor);
                    userPlan.setPadding(16, 8, 16, 8);
                    userPlan.setBackgroundResource(R.drawable.tier_badge);
                    
                    // Build features string from Firestore data
                    displayFeatures(featuresList);
                } else {
                    // Fallback to tier-based styling
                    applyDefaultStyling(tier, featuresList);
                }
            })
            .addOnFailureListener(e -> {
                // Fallback to tier-based styling on error
                Log.e("TechAssist", "Error fetching plan details: " + e.getMessage());
                applyDefaultStyling(tier, featuresList);
            });
    }
    
    // Helper method for displaying features
    private void displayFeatures(List<String> featuresList) {
        if (featuresList != null && !featuresList.isEmpty()) {
            // Use features from Firestore
            StringBuilder featuresBuilder = new StringBuilder();
            for (String feature : featuresList) {
                featuresBuilder.append(feature).append("\n");
            }
            // Remove the last newline
            if (featuresBuilder.length() > 0) {
                featuresBuilder.setLength(featuresBuilder.length() - 1);
            }
            planFeatures.setText(featuresBuilder.toString());
        } else {
            // Fallback message
            planFeatures.setText("Plan features not available");
        }
        
        // Show/hide upgrade button based on plan
        if (upgradeBtn != null) {
            upgradeBtn.setVisibility("Business".equalsIgnoreCase(instanceCurrentTier) ? View.GONE : View.VISIBLE);
        }
    }
    
    // Helper method to apply default styling based on tier
    private void applyDefaultStyling(String tier, List<String> featuresList) {
        // Store current tier for reference
        instanceCurrentTier = tier;
        
        int textColor = getTierColor(tier);
        int bgColor = getTierBgColor(tier);

        // Apply styling
        userPlan.setTextColor(textColor);
        userPlan.setBackgroundColor(bgColor);
        userPlan.setPadding(16, 8, 16, 8);
        userPlan.setBackgroundResource(R.drawable.tier_badge);

        // Display features
        displayFeatures(featuresList);
    }
    
    // Helper to get color based on tier
    private int getTierColor(String tier) {
        if (tier == null || getActivity() == null) {
            return Color.BLACK; // Default
        }
        
        switch (tier) {
            case "Silver":
                return getResources().getColor(R.color.silver, getActivity().getTheme());
            case "Gold":
                return getResources().getColor(R.color.gold, getActivity().getTheme());
            case "Diamond":
                return getResources().getColor(R.color.platinum, getActivity().getTheme());
            default: // Bronze
                return getResources().getColor(R.color.bronze, getActivity().getTheme());
        }
    }
    
    // Helper to get background color based on tier
    private int getTierBgColor(String tier) {
        if (tier == null || getActivity() == null) {
            return Color.BLACK; // Default
        }
        
        // Return black color for all tiers
        return Color.BLACK;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Refresh data when returning to this fragment
        if (isAdded() && getActivity() != null) {
            fetchUserName();
            fetchUserPlan();
            
            // Optional: Only refresh UI components that might have changed
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                SubscriptionUtils.checkSubscriptionExpiry(getActivity(), user.getUid());
            }
            
            // Re-apply the current sort option to maintain sort order
            applySortOption(currentSortOption);
        }
    }

    // Add these sorting methods
    
    private void sortAdminsByRating() {
        if (currentAdminList.isEmpty()) return;
        
        Log.d("TechAssist", "Sorting admins by rating");
        
        List<SQL_AdminModel> sortedList = new ArrayList<>(currentAdminList);
        Collections.sort(sortedList, (a, b) -> {
            try {
                float ratingA = Float.parseFloat(a.getRatings().replace(" ★", ""));
                float ratingB = Float.parseFloat(b.getRatings().replace(" ★", ""));
                // Descending order (highest first)
                return Float.compare(ratingB, ratingA);
            } catch (Exception e) {
                return 0;
            }
        });
        
        // Update adapter with sorted list
        updateAdapterWithList(sortedList);
    }
    
    private void sortAdminsByExperience() {
        if (currentAdminList.isEmpty()) return;
        
        Log.d("TechAssist", "Sorting admins by experience");
        
        List<SQL_AdminModel> sortedList = new ArrayList<>(currentAdminList);
        Collections.sort(sortedList, (a, b) -> {
            // Descending order (most experience first)
            return Integer.compare(b.getYearsExp(), a.getYearsExp());
        });
        
        // Update adapter with sorted list
        updateAdapterWithList(sortedList);
    }
    
    private void sortAdminsByCompletedJobs() {
        if (currentAdminList.isEmpty()) return;
        
        Log.d("TechAssist", "Sorting admins by completed jobs");
        
        List<SQL_AdminModel> sortedList = new ArrayList<>(currentAdminList);
        Collections.sort(sortedList, (a, b) -> {
            // Descending order (most completed jobs first)
            return Integer.compare(b.getCompletedJobs(), a.getCompletedJobs());
        });
        
        // Update adapter with sorted list
        updateAdapterWithList(sortedList);
    }
    
    private void updateAdapterWithList(List<SQL_AdminModel> adminList) {
        Context context = getActivity();
        if (context == null) return;
        
        adminAdapter = new AdminAdapter(adminList, context);
        itView.setAdapter(adminAdapter);
        
        // Log the sorted order
        for (int i = 0; i < Math.min(adminList.size(), 5); i++) {
            SQL_AdminModel admin = adminList.get(i);
            Log.d("TechAssist", "Sorted position " + i + ": " + admin.getName() + 
                    ", rating: " + admin.getRatings() + 
                    ", experience: " + admin.getYearsExp() + " years");
        }
    }

    private void setupSortDropdown() {
        // Create an array of sorting options with shorter names
        String[] sortOptions = {"None", "By Rating", "By Experience", "By Jobs"};
        
        // Create array adapter with custom layout for the dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(),
                R.layout.item_sort_dropdown,
                R.id.text1,
                sortOptions
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(R.id.text1);
                
                // All items have black text in main view
                textView.setTextColor(getResources().getColor(R.color.black, null));
                
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = view.findViewById(R.id.text1);
                
                // All dropdown items have black text 
                textView.setTextColor(getResources().getColor(R.color.black, null));
                
                // Force white background for dropdown item
                textView.setBackgroundResource(R.drawable.white_dropdown_background);
                
                return view;
            }
        };
        
        // Set the adapter to the dropdown
        sortDropdown.setAdapter(adapter);
        
        // Set initial selection based on saved sort option
        sortDropdown.setText(sortOptions[currentSortOption], false);
        
        // Ensure dropdown popup has white background
        sortDropdown.setDropDownBackgroundResource(android.R.color.white);
        
        // Set item click listener
        sortDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedOption = (String) parent.getItemAtPosition(position);
            
            // Update dropdown text to show the selected option
            sortDropdown.setText(selectedOption, false);
            
            // Save the selected sort option
            currentSortOption = position;
            
            // Apply the selected sort option
            applySortOption(currentSortOption);
        });
    }

    /**
     * Sets up real-time updates for technician/admin data from Firestore
     */
    private void setupRealtimeAdminUpdates() {
        // Remove any existing listener
        if (adminListener != null) {
            adminListener.remove();
        }
        
        // Set up a new listener
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        adminListener = firestore.collection("Users")
            .whereEqualTo("Role", "Admin")
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e("TechAssist", "Listen failed for admin updates", error);
                    return;
                }
                
                if (snapshots != null && !snapshots.isEmpty()) {
                    Log.d("TechAssist", "Real-time update received for admins");
                    processAdminDocuments(snapshots.getDocuments());
                }
            });
    }
    
    /**
     * Process admin documents from Firestore and update the UI accordingly
     */
    private void processAdminDocuments(List<DocumentSnapshot> documents) {
        Context context = getActivity();
        if (context == null) return;
        
        // Create a new list to hold admins
        List<SQL_AdminModel> adminList = new ArrayList<>();
        
        for (DocumentSnapshot document : documents) {
            try {
                String adminID = document.getId();
                
                // Get name (try both formats)
                String name = document.getString("name");
                if (name == null) name = document.getString("Name");
                if (name == null) name = "Admin";
                
                // Get rating
                String ratings = "5"; // default
                if (document.contains("rating")) {
                    ratings = String.valueOf(document.get("rating"));
                } else if (document.contains("Rating")) {
                    ratings = String.valueOf(document.get("Rating"));
                }
                
                // Get specialization
                String specialized = "IT Support"; // default
                Object specObj = document.get("specialization");
                if (specObj == null) specObj = document.get("Specialization");
                
                if (specObj instanceof java.util.List) {
                    List<Object> specList = (List<Object>) specObj;
                    if (!specList.isEmpty()) {
                        specialized = String.valueOf(specList.get(0));
                    }
                } else if (specObj != null) {
                    specialized = String.valueOf(specObj);
                }
                
                // Get experience
                int yearsExp = 3; // default
                Object expObj = document.get("experience");
                if (expObj == null) expObj = document.get("Experience");
                if (expObj instanceof Number) {
                    yearsExp = ((Number) expObj).intValue();
                }
                
                // Get availability
                String availability = "Available"; // default
                if (document.contains("availability")) {
                    availability = document.getString("availability");
                } else if (document.contains("Availability")) {
                    availability = document.getString("Availability");
                }
                
                // Get status
                String status = "Online"; // default
                if (document.contains("status")) {
                    status = document.getString("status");
                } else if (document.contains("Status")) {
                    status = document.getString("Status");
                }
                
                // Get completedJobs count (for sorting)
                int completedJobs = 0;
                if (document.contains("completedJobs")) {
                    Object jobsObj = document.get("completedJobs");
                    if (jobsObj instanceof Number) {
                        completedJobs = ((Number) jobsObj).intValue();
                    }
                } else if (document.contains("CompletedJobs")) {
                    Object jobsObj = document.get("CompletedJobs");
                    if (jobsObj instanceof Number) {
                        completedJobs = ((Number) jobsObj).intValue();
                    }
                }
                
                // Create admin model
                SQL_AdminModel admin = new SQL_AdminModel();
                admin.setAdminID(adminID);
                admin.setName(name);
                admin.setRatings(ratings);
                admin.setSpecialized(specialized);
                admin.setYearsExp(yearsExp);
                admin.setAvailability(availability);
                admin.setStatus(status);
                admin.setCompletedJobs(completedJobs);
                
                // Set default image for now
                admin.setImage(getBytesFromDrawable(R.drawable.user_icon));
                
                // Add to list
                adminList.add(admin);
                Log.d("TechAssist", "Updated admin (real-time): " + name + 
                    ", availability: " + availability + 
                    ", status: " + status);
            } catch (Exception e) {
                Log.e("TechAssist", "Error processing admin document in real-time update", e);
            }
        }
        
        // Update the UI on the main thread
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(() -> {
                // Store the full list
                currentAdminList = new ArrayList<>(adminList);
                
                // Apply the current sort option
                applySortOption(currentSortOption);
            });
        }
    }
    
    /**
     * Apply the current sort option to the admin list
     */
    private void applySortOption(int sortOption) {
        switch (sortOption) {
            case 1: // By Rating
                sortAdminsByRating();
                break;
            case 2: // By Experience
                sortAdminsByExperience();
                break;
            case 3: // By Jobs
                sortAdminsByCompletedJobs();
                break;
            default: // None/Default
                // Use the original order
                updateAdapterWithList(new ArrayList<>(currentAdminList));
                break;
        }
    }

}