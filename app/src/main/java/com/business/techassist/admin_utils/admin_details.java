package com.business.techassist.admin_utils;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.business.techassist.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
//import java.util.SimpleDateFormat;
//import java.util.Timestamp;

import com.google.android.material.snackbar.Snackbar;

public class admin_details extends AppCompatActivity {

    private static final String TAG = "AdminDetails";

    ShapeableImageView img2;
    TextView nameTxt, specialTxt, experienceTxt, ratingTxt2, devicesCheckedTxt, scheduleTxt, availabilityTxt, statusTxt;
    MaterialToolbar backBtnAdmin;
    MaterialButton chatAdminBtn;

    // Firestore reference
    private FirebaseFirestore db;
    private String technicianId;
    private String technicianEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        initializeUI();
        
        // Get technician ID from intent
        Intent intent = getIntent();
        if (intent != null) {
            technicianId = intent.getStringExtra("technicianId");
            
            // If we have a technician ID, fetch data from Firestore
            if (technicianId != null && !technicianId.isEmpty()) {
                fetchTechnicianData(technicianId);
            } else {
                // Use bundled data from intent if Firestore ID is not available
                if (intent.hasExtra("name")) {
                    displayIntentData(intent);
                } else {
                    // Try to find and display first admin user
                    fetchAllAdmins();
                }
            }
        } else {
            Log.e(TAG, "Intent is null");
            displayDefaultData();
        }
    }
    
    private void initializeUI() {
        chatAdminBtn = findViewById(R.id.chatWithTechnicianBtn);
        img2 = findViewById(R.id.expertImage);
        nameTxt = findViewById(R.id.nameTxt);
        specialTxt = findViewById(R.id.specialTxt);
        experienceTxt = findViewById(R.id.experienceTxt);
        ratingTxt2 = findViewById(R.id.ratingTxt);
        devicesCheckedTxt = findViewById(R.id.devicesCheckedTxt);
        scheduleTxt = findViewById(R.id.scheduleTxt);
        availabilityTxt = findViewById(R.id.availabilityTxt);
        
        // Find status text view if it exists
        try {
            statusTxt = findViewById(R.id.statusTxt);
        } catch (Exception e) {
            Log.w(TAG, "Status TextView not found in layout", e);
            statusTxt = null;
        }
        
        // Find and set up book button
        MaterialButton bookButton = findViewById(R.id.bookButton);
        if (bookButton != null) {
            bookButton.setOnClickListener(v -> showBookingDialog());
        }

        backBtnAdmin = findViewById(R.id.toolbar);
        backBtnAdmin.setOnClickListener(view -> finish());

        // Set up chat button click listener if needed
        chatAdminBtn.setOnClickListener(v -> {
            if (technicianId != null && !technicianId.isEmpty()) {
                // Create AdminModel object with the technician's data
                AdminModel admin = new AdminModel();
                admin.setUserID(technicianId);
                admin.setName(nameTxt.getText().toString());
                
                // Add email if available
                if (technicianEmail != null) {
                    admin.setEmail(technicianEmail);
                }
                
                // Start message activity
                Intent intent = new Intent(admin_details.this, com.business.techassist.menucomponents.messages.messageActivity.class);
                com.business.techassist.utilities.AndroidUtil.passAdminDataMessages(intent, admin);
                startActivity(intent);
            } else {
                // Show error if technician ID is missing
                Log.e(TAG, "Cannot start chat - technician ID is missing");
                Toast.makeText(admin_details.this, "Cannot start chat - technician information is missing", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void fetchTechnicianData(String technicianId) {
        Log.d(TAG, "Fetching technician data for ID: " + technicianId);
        
        db.collection("Users").document(technicianId)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    Log.d(TAG, "Document exists. Checking role...");
                    String role = document.getString("Role");
                    Log.d(TAG, "User role: " + role);
                    
                    if ("Admin".equals(role)) {
                        Log.d(TAG, "User is an Admin. Displaying data.");
                        displayFirestoreData(document);
                    } else {
                        Log.w(TAG, "User exists but is not an Admin. Role: " + role);
                        // Try to display what we have from the intent
                        Intent intent = getIntent();
                        if (intent != null && intent.hasExtra("name")) {
                            displayIntentData(intent);
                        } else {
                            displayDefaultData();
                        }
                    }
                } else {
                    Log.e(TAG, "User document does not exist");
                    // Try to display what we have from the intent
                    Intent intent = getIntent();
                    if (intent != null && intent.hasExtra("name")) {
                        displayIntentData(intent);
                    } else {
                        displayDefaultData();
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching user data", e);
                // Try to display what we have from the intent
        Intent intent = getIntent();
                if (intent != null && intent.hasExtra("name")) {
                    displayIntentData(intent);
                } else {
                    displayDefaultData();
                }
            });
    }
    
    private void displayFirestoreData(DocumentSnapshot document) {
        Log.d(TAG, "Displaying Firestore data");
        
        // Name field (try both name and Name formats)
        String name = document.getString("name");
        if (name == null) {
            name = document.getString("Name"); // Try alternate capitalization
        }
        nameTxt.setText(name != null ? name : "NULL");
        Log.d(TAG, "Name: " + name);
        
        // Get email for chat functionality
        technicianEmail = document.getString("email");
        if (technicianEmail == null) {
            technicianEmail = document.getString("Email"); // Try alternate capitalization
        }
        Log.d(TAG, "Email: " + technicianEmail);
        
        // Experience field - handle as number
        Object expObj = document.get("experience");
        if (expObj == null) {
            expObj = document.get("Experience"); // Try alternate capitalization
        }
        String experience = "NULL";
        if (expObj instanceof Number) {
            experience = String.valueOf(((Number) expObj).intValue());
        }
        experienceTxt.setText(experience);
        Log.d(TAG, "Experience: " + experience);
        
        // Rating field - handle as number
        Object ratingObj = document.get("rating");
        if (ratingObj == null) {
            ratingObj = document.get("Rating"); // Try alternate capitalization
        }
        String rating = "NULL";
        if (ratingObj instanceof Number) {
            rating = String.valueOf(((Number) ratingObj).floatValue());
        }
        ratingTxt2.setText(rating);
        Log.d(TAG, "Rating: " + rating);
        
        // Completed jobs field (try different field names)
        Object jobsObj = document.get("completedJobs");
        if (jobsObj == null) {
            jobsObj = document.get("CompletedJobs"); // Try alternate capitalization
            if (jobsObj == null) {
                jobsObj = document.get("DevicesChecked"); // Try alternate field name
            }
        }
        String completedJobs = "NULL";
        if (jobsObj instanceof Number) {
            completedJobs = String.valueOf(((Number) jobsObj).intValue());
        }
        devicesCheckedTxt.setText(completedJobs);
        Log.d(TAG, "Completed Jobs: " + completedJobs);
        
        // Handle availability (separate from status)
        String availability = document.getString("availability");
        if (availability == null) {
            availability = document.getString("Availability"); // Try alternate capitalization
        }
        
        if (availability != null) {
            availabilityTxt.setText(availability);
            
            // Set color based on availability status
            int colorResId;
            switch (availability.toLowerCase()) {
                case "available":
                    colorResId = R.color.success;
                    break;
                case "unavailable":
                    colorResId = R.color.error;
                    break;
                case "busy":
                    colorResId = R.color.warning;
                    break;
                default:
                    colorResId = R.color.outline;
                    break;
            }
            availabilityTxt.setTextColor(getResources().getColor(colorResId));
        } else {
            availabilityTxt.setText("NULL");
            availabilityTxt.setTextColor(getResources().getColor(R.color.outline));
        }
        Log.d(TAG, "Availability: " + availability);
        
        // Handle status field (if statusTxt exists)
        if (statusTxt != null) {
            String status = document.getString("status");
            if (status == null) {
                status = document.getString("Status"); // Try alternate capitalization
            }
            
            if (status != null) {
                statusTxt.setText(status);
                
                // Set color based on status
                int statusColorResId;
                switch (status.toLowerCase()) {
                    case "active":
                    case "online":
                        statusColorResId = R.color.success;
                        break;
                    case "offline":
                    case "inactive":
                        statusColorResId = R.color.error;
                        break;
                    case "away":
                    case "busy":
                        statusColorResId = R.color.warning;
                        break;
                    default:
                        statusColorResId = R.color.outline;
                        break;
                }
                statusTxt.setTextColor(getResources().getColor(statusColorResId));
            } else {
                statusTxt.setText("NULL");
                statusTxt.setTextColor(getResources().getColor(R.color.outline));
            }
            Log.d(TAG, "Status: " + status);
        }
        
        // Handle specialization (array field - try both field names)
        List<String> specializations = new ArrayList<>();
        Object specializationObj = document.get("specialization");
        if (specializationObj == null) {
            specializationObj = document.get("Specialization"); // Try alternate capitalization
        }
        
        if (specializationObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> specList = (List<Object>) specializationObj;
            for (Object spec : specList) {
                if (spec instanceof String) {
                    specializations.add((String) spec);
                }
            }
        } else if (specializationObj instanceof String) {
            // Handle case where specialization is a single string
            specializations.add((String) specializationObj);
        }
        
        // Display specializations as comma-separated list
        if (!specializations.isEmpty()) {
            specialTxt.setText(TextUtils.join(", ", specializations));
        } else {
            specialTxt.setText("NULL");
        }
        Log.d(TAG, "Specializations: " + specializations);
        
        // Handle schedule (array field - try both field names)
        List<String> schedules = new ArrayList<>();
        Object scheduleObj = document.get("schedule");
        if (scheduleObj == null) {
            scheduleObj = document.get("Schedule"); // Try alternate capitalization
        }
        
        if (scheduleObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> schedList = (List<Object>) scheduleObj;
            for (Object sched : schedList) {
                if (sched instanceof String) {
                    schedules.add((String) sched);
                }
            }
        } else if (scheduleObj instanceof String) {
            // Handle case where schedule is a single string
            schedules.add((String) scheduleObj);
        }
        
        // Display schedule as bullet points
        if (!schedules.isEmpty()) {
            StringBuilder scheduleText = new StringBuilder();
            for (String schedule : schedules) {
                scheduleText.append("• ").append(schedule).append("\n");
            }
            scheduleTxt.setText(scheduleText.toString().trim());
        } else {
            scheduleTxt.setText("Not setted yet.");
        }
        Log.d(TAG, "Schedules: " + schedules);
        
        // Load profile image if available
        loadProfileImage(document);
    }
    
    private void loadProfileImage(DocumentSnapshot document) {
        // First set default image
        img2.setImageResource(R.drawable.user_icon);
        
        // Try to find profile image URL from various fields
        String imageUrl = null;
        
        // Check for profileImage field
        if (document.contains("profileImage")) {
            imageUrl = document.getString("profileImage");
        }
        
        // Check for ProfileImage field (alternate capitalization)
        if (imageUrl == null && document.contains("ProfileImage")) {
            imageUrl = document.getString("ProfileImage");
        }
        
        // Check for photoURL field
        if (imageUrl == null && document.contains("photoURL")) {
            imageUrl = document.getString("photoURL");
        }
        
        // Check for photoUrl field (alternate capitalization)
        if (imageUrl == null && document.contains("photoUrl")) {
            imageUrl = document.getString("photoUrl");
        }
        
        // If we found an image URL, load it with Glide
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Log.d(TAG, "Loading profile image from URL: " + imageUrl);
                
                // Use Glide to load image if available
                if (isGlideAvailable()) {
                    com.bumptech.glide.Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.user_icon)
                        .error(R.drawable.user_icon)
                        .into(img2);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading profile image", e);
            }
        } else {
            Log.d(TAG, "No profile image URL found");
            // Use default image already set
        }
    }
    
    private boolean isGlideAvailable() {
        try {
            Class.forName("com.bumptech.glide.Glide");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private void displayIntentData(Intent intent) {
        // This is the legacy method from the original code
        nameTxt.setText(intent.getStringExtra("name") != null ? intent.getStringExtra("name") : "NULL");
        
        // Get email for chat functionality
        technicianEmail = intent.getStringExtra("email");
        Log.d(TAG, "Email from intent: " + technicianEmail);
        
        specialTxt.setText(intent.getStringExtra("specialization") != null ? intent.getStringExtra("specialization") : "NULL");
        experienceTxt.setText(String.valueOf(intent.getIntExtra("experience", 0)));
        ratingTxt2.setText(intent.getStringExtra("rating") != null ? intent.getStringExtra("rating") : "NULL");
        devicesCheckedTxt.setText(String.valueOf(intent.getIntExtra("deviceChecked", 0)));
        scheduleTxt.setText(intent.getStringExtra("schedule") != null ? intent.getStringExtra("schedule") : "NULL");
        
        // Availability with color coding
        String availability = intent.getStringExtra("availability");
        if (availability != null) {
            availabilityTxt.setText(availability);
            
            // Set color based on availability status
            int colorResId;
            switch (availability.toLowerCase()) {
                case "available":
                    colorResId = R.color.success;
                    break;
                case "unavailable":
                    colorResId = R.color.error;
                    break;
                case "busy":
                    colorResId = R.color.warning;
                    break;
                default:
                    colorResId = R.color.outline;
                    break;
            }
            availabilityTxt.setTextColor(getResources().getColor(colorResId));
        } else {
            availabilityTxt.setText("NULL");
            availabilityTxt.setTextColor(getResources().getColor(R.color.outline));
        }
        
        // Status field (if statusTxt exists)
        if (statusTxt != null) {
            String status = intent.getStringExtra("status");
            if (status != null) {
                statusTxt.setText(status);
                
                // Set color based on status
                int statusColorResId;
                switch (status.toLowerCase()) {
                    case "active":
                    case "online":
                        statusColorResId = R.color.success;
                        break;
                    case "offline":
                    case "inactive":
                        statusColorResId = R.color.error;
                        break;
                    case "away":
                    case "busy":
                        statusColorResId = R.color.warning;
                        break;
                    default:
                        statusColorResId = R.color.outline;
                        break;
                }
                statusTxt.setTextColor(getResources().getColor(statusColorResId));
            } else {
                statusTxt.setText("NULL");
                statusTxt.setTextColor(getResources().getColor(R.color.outline));
            }
        }

        byte[] imageBytes = intent.getByteArrayExtra("image");
        if (imageBytes != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            img2.setImageBitmap(bitmap);
        } else {
            Log.e("AdminDetails", "Image is null, setting placeholder");
            img2.setImageResource(R.drawable.user_icon);
        }
    }
    
    private void displayDefaultData() {
        // Set default values when no data is available
        nameTxt.setText("NULL");
        specialTxt.setText("NULL");
        experienceTxt.setText("NULL");
        ratingTxt2.setText("NULL");
        devicesCheckedTxt.setText("NULL");
        scheduleTxt.setText("NULL");
        
        // Set availability text and color
        availabilityTxt.setText("NULL");
        availabilityTxt.setTextColor(getResources().getColor(R.color.outline));
        
        // Set status text and color (if statusTxt exists)
        if (statusTxt != null) {
            statusTxt.setText("NULL");
            statusTxt.setTextColor(getResources().getColor(R.color.outline));
        }
        
        // Set image placeholder
        img2.setImageResource(R.drawable.user_icon);
    }

    /**
     * Fetch all users with Admin role
     * This can be used by other components if needed
     */
    private void fetchAllAdmins() {
        Log.d(TAG, "Fetching all admin users");
        
        db.collection("Users")
            .whereEqualTo("Role", "Admin")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    // If we want to display a list of admins, we would process the results here
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " admin users");
                    
                    // Example: Display the first admin found if we don't have a specific ID
                    if (technicianId == null || technicianId.isEmpty()) {
                        DocumentSnapshot firstAdmin = queryDocumentSnapshots.getDocuments().get(0);
                        technicianId = firstAdmin.getId();
                        Log.d(TAG, "Displaying first admin: " + technicianId);
                        displayFirestoreData(firstAdmin);
                    }
                } else {
                    // Try alternate field name/capitalization
                    Log.d(TAG, "No admins found with Role=Admin, trying role=Admin");
                    db.collection("Users")
                        .whereEqualTo("role", "Admin")
                        .get()
                        .addOnSuccessListener(queryDocs -> {
                            if (!queryDocs.isEmpty()) {
                                Log.d(TAG, "Found " + queryDocs.size() + " admin users with role=Admin");
                                
                                // Display first admin
                                DocumentSnapshot firstAdmin = queryDocs.getDocuments().get(0);
                                technicianId = firstAdmin.getId();
                                Log.d(TAG, "Displaying first admin: " + technicianId);
                                displayFirestoreData(firstAdmin);
        } else {
                                Log.e(TAG, "No admin users found with either Role=Admin or role=Admin");
                                displayDefaultData();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error fetching admin users with role=Admin", e);
                            displayDefaultData();
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching admin users with Role=Admin", e);
                displayDefaultData();
            });
    }
    
    // Add a method to check if the document has all required fields
    private boolean hasRequiredFields(DocumentSnapshot document) {
        return document.contains("name") || 
               document.contains("specialization") || 
               document.contains("experience") || 
               document.contains("rating") || 
               document.contains("completedJobs") || 
               document.contains("availability") || 
               document.contains("schedule");
    }

    private void showBookingDialog() {
        // Create dialog builder
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Book Appointment");
        

        // Inflate the custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_book_appointment, null);
        builder.setView(dialogView);
        
        // Get references to dialog components
        TextView basePrice = dialogView.findViewById(R.id.basePrice);
        TextView discountText = dialogView.findViewById(R.id.discountText);
        TextView finalPrice = dialogView.findViewById(R.id.finalPrice);
        EditText notesEditText = dialogView.findViewById(R.id.appointmentNotes);
        Spinner serviceTypeSpinner = dialogView.findViewById(R.id.serviceTypeSpinner);
        
        // Set up date picker
        TextView dateText = dialogView.findViewById(R.id.dateText);
        MaterialButton selectDateButton = dialogView.findViewById(R.id.selectDateButton);
        
        // Set base price
        double basePriceValue = 500.00; // ₱500 per appointment
        basePrice.setText(String.format("₱%.2f", basePriceValue));
        
        // Get user's subscription tier and calculate discount
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        db.collection("Users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String subscriptionPlan = documentSnapshot.getString("SubscriptionPlan");
                    int discountPercentage = 0;
                    
                    // Calculate discount based on subscription tier
                    if (subscriptionPlan != null) {
                        switch (subscriptionPlan) {
                            case "Premium":
                                discountPercentage = 15;
                                break;
                            case "Standard":
                                discountPercentage = 10;
                                break;
                            case "Business":
                                discountPercentage = 20;
                                break;
                            default: // Basic
                                discountPercentage = 5;
                                break;
                        }
                    }
                    
                    // Calculate discounted price
                    double discount = basePriceValue * (discountPercentage / 100.0);
                    double discountedPrice = basePriceValue - discount;
                    
                    // Update UI
                    if (discountPercentage > 0) {
                        discountText.setVisibility(View.VISIBLE);
                        discountText.setText(String.format("%d%% discount applied", discountPercentage));
                        finalPrice.setText(String.format("₱%.2f", discountedPrice));
                    } else {
                        discountText.setVisibility(View.GONE);
                        finalPrice.setText(String.format("₱%.2f", basePriceValue));
                    }
                    
                    // Store the values for later use
                    dialogView.setTag(R.id.tag_discount_percentage, discountPercentage);
                    dialogView.setTag(R.id.tag_base_price, basePriceValue);
                    dialogView.setTag(R.id.tag_discounted_price, discountedPrice);
                    dialogView.setTag(R.id.tag_subscription_plan, subscriptionPlan);
                }
            });
        
        // Set up service type spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.service_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serviceTypeSpinner.setAdapter(adapter);
        
        // Set up available time slots based on technician's schedule
        RecyclerView timeSlotRecyclerView = dialogView.findViewById(R.id.timeSlotRecyclerView);
        List<String> availableTimeSlots = new ArrayList<>();
        
        // Selected date - default to tomorrow
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.add(Calendar.DAY_OF_MONTH, 1);
        dialogView.setTag(R.id.tag_selected_date, selectedDate.getTime());
        
        // Display the initially selected date
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US);
        dateText.setText(dateFormat.format(selectedDate.getTime()));
        
        // Set up date selection
        selectDateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    admin_details.this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar newDate = Calendar.getInstance();
                        newDate.set(year, month, dayOfMonth);
                        
                        // Store the selected date
                        dialogView.setTag(R.id.tag_selected_date, newDate.getTime());
                        
                        // Update the date text
                        dateText.setText(dateFormat.format(newDate.getTime()));
                        
                        // Update time slots based on day of week
                        updateAvailableTimeSlots(newDate, availableTimeSlots, timeSlotRecyclerView, dialogView);
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH));
            
            // Set min date to tomorrow
            Calendar minDate = Calendar.getInstance();
            minDate.add(Calendar.DAY_OF_MONTH, 1);
            datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
            
            // Set max date to 30 days from now
            Calendar maxDate = Calendar.getInstance();
            maxDate.add(Calendar.DAY_OF_MONTH, 30);
            datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
            
            datePickerDialog.show();
        });
        
        // Initially populate time slots based on default date (tomorrow)
        updateAvailableTimeSlots(selectedDate, availableTimeSlots, timeSlotRecyclerView, dialogView);
        
        // Create the dialog
        AlertDialog dialog = builder.create();
        
        // Add buttons
        builder.setPositiveButton("Book", null); // We'll override this below
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
        
        // Create and show the dialog
        AlertDialog alertDialog = builder.create();
        
        // Set dialog background to white
        alertDialog.setOnShowListener(dialogInterface -> {
            // Set white background
            alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
            
            Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                // Validate selections
                String selectedTimeSlot = (String) dialogView.getTag(R.id.tag_selected_time_slot);
                String notes = notesEditText.getText().toString().trim();
                String serviceType = serviceTypeSpinner.getSelectedItem().toString();
                Date date = (Date) dialogView.getTag(R.id.tag_selected_date);
                
                if (selectedTimeSlot == null) {
                    Snackbar.make(dialogView, "Please select a time slot", Snackbar.LENGTH_LONG).show();
                    return;
                }
                
                // Check if the selected time slot is a valid time slot or an error message
                if (selectedTimeSlot.startsWith("No schedule") || 
                    selectedTimeSlot.startsWith("Error") || 
                    selectedTimeSlot.equals("Loading available slots...") ||
                    selectedTimeSlot.startsWith("All slots booked") ||
                    selectedTimeSlot.equals("Technician not found") ||
                    selectedTimeSlot.equals("No schedule available")) {
                    Snackbar.make(dialogView, "Cannot book: " + selectedTimeSlot, Snackbar.LENGTH_LONG).show();
                    return;
                }
                
                if (serviceType.equals("Select service type")) {
                    Snackbar.make(dialogView, "Please select a service type", Snackbar.LENGTH_LONG).show();
                    return;
                }
                
                // Proceed with booking
                bookAppointment(
                    userId,
                    technicianId,
                    serviceType,
                    notes,
                    date,
                    selectedTimeSlot,
                    (Integer) dialogView.getTag(R.id.tag_discount_percentage),
                    (Double) dialogView.getTag(R.id.tag_base_price),
                    (Double) dialogView.getTag(R.id.tag_discounted_price),
                    (String) dialogView.getTag(R.id.tag_subscription_plan)
                );
                
                alertDialog.dismiss();
            });
        });
        
        alertDialog.show();
    }
    
    private void updateAvailableTimeSlots(Calendar date, List<String> availableTimeSlots, RecyclerView recyclerView, View dialogView) {
        // Clear existing slots
        availableTimeSlots.clear();
        
        // Get day of week for the selected date
        String dayOfWeek = new SimpleDateFormat("EEEE", Locale.getDefault()).format(date.getTime());
        Log.d(TAG, "Looking for schedule for day: " + dayOfWeek);
        
        // Get technician's schedule from Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").document(technicianId)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    // Try to get schedule as an array
                    List<String> scheduleList = new ArrayList<>();
                    
                    // Check if schedule is an array field
                    Object scheduleObj = document.get("schedule");
                    if (scheduleObj == null) {
                        scheduleObj = document.get("Schedule"); // Try alternate capitalization
                    }
                    
                    if (scheduleObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> schedList = (List<Object>) scheduleObj;
                        for (Object sched : schedList) {
                            if (sched instanceof String) {
                                scheduleList.add((String) sched);
                            }
                        }
                    }
                    
                    Log.d(TAG, "Technician schedule from Firestore: " + scheduleList);
                    
                    // Find the schedule for the selected day
                    String timeRange = null;
                    for (String scheduleItem : scheduleList) {
                        Log.d(TAG, "Checking schedule entry: " + scheduleItem);
                        
                        // Check if this schedule entry is for the selected day (case-insensitive)
                        if (scheduleItem.toLowerCase().startsWith(dayOfWeek.toLowerCase())) {
                            // Extract time range (e.g., "Monday 9AM-5PM" -> "9AM-5PM")
                            timeRange = scheduleItem.substring(dayOfWeek.length()).trim();
                            Log.d(TAG, "Found matching schedule for " + dayOfWeek + ": " + scheduleItem + " -> time range: " + timeRange);
                            break;
                        }
                    }
                    
                    // If found, generate time slots
                    if (timeRange != null && !timeRange.isEmpty()) {
                        generateTimeSlots(timeRange, availableTimeSlots);
                        Log.d(TAG, "Generated time slots: " + availableTimeSlots);
                        setupTimeSlotRecyclerView(recyclerView, availableTimeSlots, dialogView);
                    } else {
                        // No schedule found for this day
                        Log.d(TAG, "No schedule found for " + dayOfWeek);
                        availableTimeSlots.add("No schedule for " + dayOfWeek);
                        setupTimeSlotRecyclerView(recyclerView, availableTimeSlots, dialogView);
                        
                        // Show a Snackbar message
                        Snackbar.make(dialogView, "No schedule available for " + dayOfWeek, Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    // Document doesn't exist
                    availableTimeSlots.add("Technician not found");
                    setupTimeSlotRecyclerView(recyclerView, availableTimeSlots, dialogView);
                    
                    // Show a Snackbar message
                    Snackbar.make(dialogView, "Technician profile not found", Snackbar.LENGTH_LONG).show();
                }
            })
            .addOnFailureListener(e -> {
                // Error fetching schedule
                Log.e(TAG, "Error fetching technician schedule", e);
                availableTimeSlots.add("Error loading schedule");
                setupTimeSlotRecyclerView(recyclerView, availableTimeSlots, dialogView);
                
                // Show a Snackbar message
                Snackbar.make(dialogView, "Error loading schedule: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            });
    }
    
    private void setupTimeSlotRecyclerView(RecyclerView recyclerView, List<String> timeSlots, View dialogView) {
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        TimeSlotAdapter timeSlotAdapter = new TimeSlotAdapter(timeSlots, selectedSlot -> {
            // Store the selected time slot
            dialogView.setTag(R.id.tag_selected_time_slot, selectedSlot);
        });
        recyclerView.setAdapter(timeSlotAdapter);
    }
    
    private void generateTimeSlots(String timeRange, List<String> slots) {
        // Parse time range like "9AM-5PM" into hourly slots
        try {
            Log.d(TAG, "Generating time slots from time range: " + timeRange);
            
            // Split the range
            String[] parts = timeRange.split("-");
            if (parts.length != 2) {
                Log.e(TAG, "Invalid time range format: " + timeRange);
                return;
            }
            
            String startTimeStr = parts[0].trim();
            String endTimeStr = parts[1].trim();
            
            Log.d(TAG, "Parsed start time: " + startTimeStr + ", end time: " + endTimeStr);
            
            // Parse start and end times
            SimpleDateFormat format = new SimpleDateFormat("hha", Locale.US);
            Date startTime;
            Date endTime;
            
            try {
                startTime = format.parse(startTimeStr);
                endTime = format.parse(endTimeStr);
                
                if (startTime == null || endTime == null) {
                    Log.e(TAG, "Failed to parse time strings: " + startTimeStr + ", " + endTimeStr);
                    return;
                }
                
                Log.d(TAG, "Parsed times: start=" + startTime + ", end=" + endTime);
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing time strings: " + startTimeStr + ", " + endTimeStr, e);
                
                // Try alternative parsing using our own method
                int startHour = parseHour(startTimeStr);
                int endHour = parseHour(endTimeStr);
                
                // Create calendar instances for these hours
                Calendar startCal = Calendar.getInstance();
                startCal.set(Calendar.HOUR_OF_DAY, startHour);
                startCal.set(Calendar.MINUTE, 0);
                
                Calendar endCal = Calendar.getInstance();
                endCal.set(Calendar.HOUR_OF_DAY, endHour);
                endCal.set(Calendar.MINUTE, 0);
                
                startTime = startCal.getTime();
                endTime = endCal.getTime();
                
                Log.d(TAG, "Used alternative parsing: start=" + startTime + ", end=" + endTime);
            }
            
            // Generate hourly slots
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startTime);
            
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.setTime(endTime);
            
            // Format for display
            SimpleDateFormat displayFormat = new SimpleDateFormat("h:mm a", Locale.US);
            
            while (calendar.getTime().before(endTime)) {
                String slotStart = displayFormat.format(calendar.getTime());
                
                calendar.add(Calendar.HOUR_OF_DAY, 1);
                if (calendar.getTime().after(endTime)) break;
                
                String slotEnd = displayFormat.format(calendar.getTime());
                String slot = slotStart + " - " + slotEnd;
                slots.add(slot);
                Log.d(TAG, "Added slot: " + slot);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing time range: " + timeRange, e);
        }
    }
    
    /**
     * Parses hour from time string (e.g., "9AM", "5PM")
     * @param timeStr Time string
     * @return Hour in 24-hour format
     */
    private int parseHour(String timeStr) {
        try {
            int hour;
            String upperTimeStr = timeStr.toUpperCase();
            boolean isPM = upperTimeStr.endsWith("PM");
            
            // Remove AM/PM suffix
            String hourStr = upperTimeStr.replace("AM", "").replace("PM", "").trim();
            
            // Parse hour
            hour = Integer.parseInt(hourStr);
            
            // Convert to 24-hour format if PM
            if (isPM && hour < 12) {
                hour += 12;
            } else if (!isPM && hour == 12) {
                // 12AM is 0 in 24-hour format
                hour = 0;
            }
            
            return hour;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing hour from: " + timeStr, e);
            // Default to 9AM if parsing fails
            return 9;
        }
    }
    
    private void bookAppointment(String userId, String technicianId, String serviceType, 
                               String notes, Date date, String timeSlot, 
                               int discountPercentage, double basePrice, double discountedPrice,
                               String subscriptionPlan) {
        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Booking appointment...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Create appointment data
        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("userId", userId);
        appointmentData.put("technicianId", technicianId);
        appointmentData.put("serviceType", serviceType);
        appointmentData.put("notes", notes);
        appointmentData.put("appointmentDate", new Timestamp(date));
        appointmentData.put("timeSlot", timeSlot);
        appointmentData.put("status", "Pending");
        appointmentData.put("basePrice", basePrice);
        appointmentData.put("discountedPrice", discountedPrice);
        appointmentData.put("discountPercentage", discountPercentage);
        appointmentData.put("createdAt", Timestamp.now());
        appointmentData.put("userSubscriptionTier", mapSubscriptionPlanToTier(subscriptionPlan));
        
        // Save to Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Appointments")
            .add(appointmentData)
            .addOnSuccessListener(documentReference -> {
                // Add notification for the technician
                addAppointmentNotification(technicianId, serviceType, date, timeSlot);
                
                progressDialog.dismiss();
                
                // Show success message
                new AlertDialog.Builder(admin_details.this)
                    .setTitle("Appointment Booked")
                    .setMessage("Your appointment has been successfully booked!")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Snackbar.make(findViewById(R.id.main), "Error booking appointment: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            });
    }
    
    private void addAppointmentNotification(String technicianId, String serviceType, Date date, String timeSlot) {
        Map<String, Object> notificationData = new HashMap<>();
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.US);
        String formattedDate = dateFormat.format(date);
        
        notificationData.put("title", "New Appointment");
        notificationData.put("body", "You have a new " + serviceType + " appointment on " + formattedDate + " at " + timeSlot);
        notificationData.put("timestamp", Timestamp.now());
        notificationData.put("type", "appointment");
        notificationData.put("read", false);
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").document(technicianId)
            .collection("Notifications")
            .add(notificationData)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Notification added for technician: " + technicianId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding notification", e);
            });
    }
    
    private String mapSubscriptionPlanToTier(String subscriptionPlan) {
        if (subscriptionPlan == null) return "Bronze";
        
        switch (subscriptionPlan) {
            case "Premium": return "Gold";
            case "Standard": return "Silver";
            case "Business": return "Diamond";
            default: return "Bronze";
        }
    }
    
    // Time slot adapter for the booking dialog
    private static class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {
        private final List<String> timeSlots;
        private final OnTimeSlotSelectedListener listener;
        private int selectedPosition = -1;
        
        public interface OnTimeSlotSelectedListener {
            void onTimeSlotSelected(String timeSlot);
        }
        
        public TimeSlotAdapter(List<String> timeSlots, OnTimeSlotSelectedListener listener) {
            this.timeSlots = timeSlots;
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_time_slot, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String timeSlot = timeSlots.get(position);
            holder.timeSlotText.setText(timeSlot);
            
            // Check if this is a bookable time slot or an error/info message
            boolean isBookable = isBookableTimeSlot(timeSlot);
            
            // Set selected state
            if (position == selectedPosition) {
                holder.itemView.setBackgroundResource(R.color.primary);
                holder.timeSlotText.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            } else {
                holder.itemView.setBackgroundResource(android.R.color.white);
                holder.timeSlotText.setTextColor(holder.itemView.getContext().getResources().getColor(
                    isBookable ? R.color.black : R.color.outline));
            }
            
            // Set click listener only for bookable slots
            if (isBookable) {
                holder.itemView.setOnClickListener(v -> {
                    int previousSelected = selectedPosition;
                    selectedPosition = holder.getAdapterPosition();
                    
                    // Update UI
                    notifyItemChanged(previousSelected);
                    notifyItemChanged(selectedPosition);
                    
                    // Notify listener
                    if (listener != null) {
                        listener.onTimeSlotSelected(timeSlot);
                    }
                });
            } else {
                // Disable click for non-bookable slots
                holder.itemView.setOnClickListener(null);
                holder.itemView.setClickable(false);
                // Apply a disabled style
                holder.itemView.setAlpha(0.7f);
            }
        }
        
        @Override
        public int getItemCount() {
            return timeSlots.size();
        }
        
        /**
         * Check if a time slot is bookable or just an information/error message
         */
        private boolean isBookableTimeSlot(String timeSlot) {
            return !(timeSlot.startsWith("No schedule") || 
                    timeSlot.startsWith("Error") || 
                    timeSlot.equals("Loading available slots...") ||
                    timeSlot.startsWith("All slots booked") ||
                    timeSlot.equals("Technician not found") ||
                    timeSlot.equals("No schedule available"));
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView timeSlotText;
            
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                timeSlotText = itemView.findViewById(R.id.timeSlotText);
            }
        }
    }
}
