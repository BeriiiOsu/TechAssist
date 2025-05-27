// admin.java
package com.business.techassist.admin_utils;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.BookedService;
import com.business.techassist.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.Timestamp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Calendar;
import java.util.TreeMap;

public class admin extends AppCompatActivity {
    // Product UI Components
    private EditText etProductName, etProductDescription, etProductPrice, etEmail, etPassword;
    private Button btnUploadImage, btnAddProduct;
    private Bitmap selectedImageBitmap;

    // Technician UI Components
    private EditText etTechnicianName, etSpecialization, etDevicesChecked, etExperience;
    private Button btnAddTechnician, btnUploadTechImage;
    private ImageView imgTechnician;
    private Bitmap technicianImageBitmap;
    
    // Service Management Components
    private RecyclerView servicesRecyclerView;
    private ServiceAdapter serviceAdapter;
    private LinearLayout serviceManagementView;
    private ScrollView productManagementView, technicianManagementView;
    private FloatingActionButton btnUpdateStatus;

    // Request Codes
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_TECH_IMAGE_REQUEST = 2;

    // Firebase References
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private AdminDatabase adminDatabase;
    
    // Tab Layout
    private TabLayout tabLayout;

    // Order Management Components
    private RecyclerView ordersRecyclerView;
    private LinearLayout orderManagementView;
    private ListenerRegistration orderListener;

    // Appointment Management Components
    private RecyclerView appointmentsRecyclerView;
    private TextView emptyAppointmentsText;
    private LinearLayout appointmentsManagementView;
    private List<AppointmentModel> appointmentsList;
    private AppointmentAdapter appointmentAdapter;
    private ListenerRegistration appointmentsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        adminDatabase = new AdminDatabase(this);

        initializeUIComponents();
        setupFirebase();
        setupTabLayout();
        setupButtonListeners();
        setupServicesRecyclerView();
        setupOrdersRecyclerView();
        
        // Initialize appointment list
        appointmentsList = new ArrayList<>();
        
        // Setup appointments recycler view
        setupAppointmentsRecyclerView();

        // Set click listener for status update button
        btnUpdateStatus.setOnClickListener(v -> {
            startActivity(new Intent(admin.this, AdminStatusActivity.class));
        });
    }
    
    private void setupTabLayout() {
        tabLayout = findViewById(R.id.tabLayout);
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0: // Service Management
                        serviceManagementView.setVisibility(View.VISIBLE);
                        productManagementView.setVisibility(View.GONE);
                        technicianManagementView.setVisibility(View.GONE);
                        orderManagementView.setVisibility(View.GONE);
                        appointmentsManagementView.setVisibility(View.GONE);
                        break;
                    case 1: // Order Management
                        serviceManagementView.setVisibility(View.GONE);
                        productManagementView.setVisibility(View.GONE);
                        technicianManagementView.setVisibility(View.GONE);
                        orderManagementView.setVisibility(View.VISIBLE);
                        appointmentsManagementView.setVisibility(View.GONE);
                        break;
                    case 2: // Appointments Management
                        serviceManagementView.setVisibility(View.GONE);
                        productManagementView.setVisibility(View.GONE);
                        technicianManagementView.setVisibility(View.GONE);
                        orderManagementView.setVisibility(View.GONE);
                        appointmentsManagementView.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        // Make sure we have exactly 3 tabs (Services, Orders, and Appointments)
        while (tabLayout.getTabCount() > 3) {
            tabLayout.removeTabAt(tabLayout.getTabCount() - 1);
        }
        
        // Add missing tabs if needed
        if (tabLayout.getTabCount() < 3) {
            if (tabLayout.getTabCount() < 2) {
                tabLayout.addTab(tabLayout.newTab().setText("Orders"));
            }
            if (tabLayout.getTabCount() < 3) {
                tabLayout.addTab(tabLayout.newTab().setText("Appointments"));
            }
        }
    }

    private void initializeUIComponents() {
        // Service Management Components
        serviceManagementView = findViewById(R.id.serviceManagementView);
        servicesRecyclerView = findViewById(R.id.servicesRecyclerView);
        
        // Tab Views
        productManagementView = findViewById(R.id.productManagementView);
        technicianManagementView = findViewById(R.id.technicianManagementView);
        orderManagementView = findViewById(R.id.orderManagementView);
        appointmentsManagementView = findViewById(R.id.appointmentsManagementView);
        
        // Appointment Management Components
        appointmentsRecyclerView = findViewById(R.id.appointmentsRecyclerView);
        emptyAppointmentsText = findViewById(R.id.emptyAppointmentsText);
        
        // Product Components - keep references but don't use
        etProductName = findViewById(R.id.etProductName);
        etProductDescription = findViewById(R.id.etProductDescription);
        etProductPrice = findViewById(R.id.etProductPrice);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnAddProduct = findViewById(R.id.btnAddProduct);

        // Technician Components - keep references but don't use
        etTechnicianName = findViewById(R.id.etTechnicianName);
        etSpecialization = findViewById(R.id.etSpecialization);
        etDevicesChecked = findViewById(R.id.etDevicesChecked);
        etExperience = findViewById(R.id.etExperience);
        btnAddTechnician = findViewById(R.id.btnAddTechnician);
        imgTechnician = findViewById(R.id.imgTechnician);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnUploadTechImage = findViewById(R.id.btnUploadTechImage);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        
        // Order Management Components
        ordersRecyclerView = findViewById(R.id.ordersRecyclerView);
        
        // Set default visibility - only show Services tab initially
        serviceManagementView.setVisibility(View.VISIBLE);
        productManagementView.setVisibility(View.GONE);
        technicianManagementView.setVisibility(View.GONE);
        orderManagementView.setVisibility(View.GONE);
        appointmentsManagementView.setVisibility(View.GONE);
        
        // Hide Product and Technician management views completely
        productManagementView.setVisibility(View.GONE);
        technicianManagementView.setVisibility(View.GONE);
    }

    private void setupFirebase() {
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }
    
    private void setupServicesRecyclerView() {
        try {
            // Use a simpler query that doesn't require a composite index
            // Instead of using collectionGroup, query the current user's services directly
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Query query = firestore.collection("Users")
                    .document(currentUserId)
                    .collection("BookedServices")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50); // Limit to avoid loading too many services
            
            FirestoreRecyclerOptions<BookedService> options = new FirestoreRecyclerOptions.Builder<BookedService>()
                    .setQuery(query, BookedService.class)
                    .build();
            
            serviceAdapter = new ServiceAdapter(options);
            
            // Enable stable IDs to fix RecyclerView inconsistency issues
            serviceAdapter.setHasStableIds(true);
            
            // Use a new LinearLayoutManager to avoid recycling issues
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            servicesRecyclerView.setLayoutManager(layoutManager);
            
            // Disable animations to prevent glitches during data updates
            servicesRecyclerView.setItemAnimator(null);
            
            // Set the empty view text
            TextView emptyView = findViewById(R.id.emptyServiceText);
            
            // Register an observer to show/hide the empty view based on adapter item count
            serviceAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    updateEmptyState(serviceAdapter.getItemCount(), servicesRecyclerView, emptyView);
                }
            });
            
            // Set the adapter
            servicesRecyclerView.setAdapter(serviceAdapter);
            
            // Start listening
            serviceAdapter.startListening();
        } catch (Exception e) {
            Log.e("Admin", "Error setting up services recycler view: " + e.getMessage());
            Toast.makeText(this, "Error setting up services list", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupButtonListeners() {
        // Product and Technician buttons are no longer needed
        // Keep only status update button
        btnUpdateStatus.setOnClickListener(v -> {
            startActivity(new Intent(admin.this, AdminStatusActivity.class));
        });
    }

    private void openImagePicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) return;

        Uri imageUri = data.getData();
        try {
            if (requestCode == PICK_IMAGE_REQUEST) {
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                Toast.makeText(this, "Product image selected", Toast.LENGTH_SHORT).show();
            } else if (requestCode == PICK_TECH_IMAGE_REQUEST) {
                technicianImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imgTechnician.setImageBitmap(technicianImageBitmap);
                Toast.makeText(this, "Technician image selected", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    private void addProduct() {
        String name = etProductName.getText().toString().trim();
        String desc = etProductDescription.getText().toString().trim();
        String price = etProductPrice.getText().toString().trim();

        if (!validateProductInput(name, desc, price)) return;

        uploadImageAndAddProduct(name, desc, price);
    }

    private boolean validateProductInput(String name, String desc, String price) {
        if (name.isEmpty() || desc.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Please fill all product fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedImageBitmap == null) {
            Toast.makeText(this, "Please select a product image", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void uploadImageAndAddProduct(String name, String desc, String price) {
        String filename = UUID.randomUUID().toString() + ".webp";
        StorageReference imageRef = storage.getReference().child("product_images/" + filename);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedImageBitmap.compress(Bitmap.CompressFormat.WEBP, 80, baos);
        byte[] imageData = baos.toByteArray();

        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnSuccessListener(taskSnapshot ->
                imageRef.getDownloadUrl().addOnSuccessListener(uri ->
                                addProductToFirestore(name, desc, price, uri.toString())
                        )
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                        ));
    }

    private void addProductToFirestore(String name, String desc, String price, String imageUrl) {
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("description", desc);
        product.put("price", price);
        product.put("imageUrl", imageUrl);

        firestore.collection("products").document(name)
                .set(product)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Product added successfully!", Toast.LENGTH_SHORT).show();
                    clearProductFields();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show()
                );
    }

    private void clearProductFields() {
        etProductName.getText().clear();
        etProductDescription.getText().clear();
        etProductPrice.getText().clear();
        selectedImageBitmap = null;
    }

    private void addTechnician() {
        String name = etTechnicianName.getText().toString().trim();
        String specialization = etSpecialization.getText().toString().trim();
        String devices = etDevicesChecked.getText().toString().trim();
        String experience = etExperience.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateTechnicianInput(name, specialization, devices, experience, email, password)) return;

        saveTechnicianData(name, specialization, devices, experience, email, password);
    }

    private boolean validateTechnicianInput(String name, String specialization, String devices,
                                            String experience, String email, String password) {
        if (name.isEmpty() || specialization.isEmpty() || devices.isEmpty() ||
                experience.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all technician fields", Toast.LENGTH_SHORT).show();
            return false;
        }
//        if (technicianImageBitmap == null) {
//            Toast.makeText(this, "Please select a technician image", Toast.LENGTH_SHORT).show();
//            return false;
//        }
        return true;
    }

    private void saveTechnicianData(String name, String specialization,
                                    String devices, String experience,
                                    String email, String password) {
        // Convert image to byte[]
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        technicianImageBitmap.compress(Bitmap.CompressFormat.WEBP, 80, baos);
        byte[] imageBytes = baos.toByteArray();

        createAdmin(name, email, password, specialization, experience, imageBytes, devices);
    }

    private void createAdmin(String name, String email, String password,
                             String specialization, String experience,
                             byte[] imageBytes, String devices) {
        FirebaseAuthHelper authHelper = new FirebaseAuthHelper();
        authHelper.registerUser(name, email, password, "Admin", specialization, experience,
                new FirebaseAuthHelper.OnRegistrationCompleteListener() {
                    @Override
                    public void onSuccess(FirebaseUser user) throws IOException {
                        // Save to local SQLite database
                        boolean isSavedLocally = adminDatabase.insertAdmin(
                                user.getUid(),  // adminID
                                name,
                                "0",            // default rating
                                specialization,
                                Integer.parseInt(experience),
                                imageBytes,
                                "Available",    // default availability
                                "Online",       // default status
                                0               // default completedJobs
                        );

                        if (isSavedLocally) {
                            Toast.makeText(admin.this, "Admin created successfully!", Toast.LENGTH_SHORT).show();
                            clearTechnicianFields();
                        } else {
                            Toast.makeText(admin.this, "Failed to save admin locally", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(admin.this, "Failed to create admin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void clearTechnicianFields() {
        etTechnicianName.getText().clear();
        etSpecialization.getText().clear();
        etDevicesChecked.getText().clear();
        etExperience.getText().clear();
        etEmail.getText().clear();
        etPassword.getText().clear();
        imgTechnician.setImageResource(android.R.color.transparent);
        technicianImageBitmap = null;
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        if (serviceAdapter != null) {
            serviceAdapter.startListening();
        }
        setupAppointmentsRecyclerView();
        loadAppointments();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (serviceAdapter != null) {
            serviceAdapter.stopListening();
        }
        
        if (appointmentsListener != null) {
            appointmentsListener.remove();
        }
        
        if (orderListener != null) {
            orderListener.remove();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        refreshServicesList();
        refreshOrdersList();
    }

    /**
     * Refreshes the services list by requerying Firestore
     */
    private void refreshServicesList() {
        if (serviceAdapter != null) {
            try {
                // Use a simpler query that doesn't require a composite index
                // Instead of using collectionGroup, query the current user's services directly
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                Query query = firestore.collection("Users")
                        .document(currentUserId)
                        .collection("BookedServices")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(50); // Limit to avoid loading too many services
                
                // Create new options with the updated query
                FirestoreRecyclerOptions<BookedService> newOptions = 
                        new FirestoreRecyclerOptions.Builder<BookedService>()
                                .setQuery(query, BookedService.class)
                                .build();
                
                // Update the adapter with the new options
                serviceAdapter.stopListening();
                serviceAdapter.updateOptions(newOptions);
                serviceAdapter.startListening();
                
                Log.d("Admin", "Services list refreshed");
            } catch (Exception e) {
                Log.e("Admin", "Error refreshing services list: " + e.getMessage());
                Toast.makeText(this, "Error refreshing services", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    // Inner class for Service Adapter
    private class ServiceAdapter extends FirestoreRecyclerAdapter<BookedService, ServiceAdapter.ServiceViewHolder> {
        
        public ServiceAdapter(@NonNull FirestoreRecyclerOptions<BookedService> options) {
            super(options);
        }
        
        @Override
        public long getItemId(int position) {
            // Return a stable ID for each item to fix the inconsistency issue
            try {
                DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);
                // Use document ID as a stable identifier
                return snapshot.getId().hashCode();
            } catch (Exception e) {
                // Fallback to position as ID if there's an error
                return position;
            }
        }
        
        @Override
        protected void onBindViewHolder(@NonNull ServiceViewHolder holder, int position, @NonNull BookedService model) {
            try {
                // Skip completed services
                if (model.getStatus() != null && model.getStatus().equalsIgnoreCase("completed")) {
                    holder.itemView.setVisibility(View.GONE);
                    return;
                } else {
                    holder.itemView.setVisibility(View.VISIBLE);
                }
                
                DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);
                holder.bind(model, snapshot);
            } catch (Exception e) {
                Log.e("ServiceAdapter", "Error binding service: " + e.getMessage());
                // Handle gracefully
                holder.serviceName.setText("Error loading service");
                holder.serviceType.setText("Try again later");
                holder.serviceDate.setText("");
                holder.clientId.setText("");
                holder.completeButton.setEnabled(false);
            }
        }
        
        @Override
        public void onDataChanged() {
            super.onDataChanged();
            
            // Log current item count for debugging
            android.util.Log.d("ServiceAdapter", "Data changed. Item count: " + getItemCount());
            
            // Check if we need to show the empty view
            TextView emptyView = findViewById(R.id.emptyServiceText);
            if (emptyView != null) {
                boolean isEmpty = getItemCount() == 0;
                emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                servicesRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                
                // Log visibility state
                android.util.Log.d("ServiceAdapter", "Empty view visible: " + (isEmpty ? "yes" : "no"));
            }
        }
        
        @NonNull
        @Override
        public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_service, parent, false);
            return new ServiceViewHolder(view);
        }
        
        class ServiceViewHolder extends RecyclerView.ViewHolder {
            private final TextView serviceName;
            private final TextView serviceType;
            private final TextView serviceDate;
            private final TextView clientId;
            private final Button completeButton;
            
            public ServiceViewHolder(@NonNull View itemView) {
                super(itemView);
                serviceName = itemView.findViewById(R.id.serviceName);
                serviceType = itemView.findViewById(R.id.serviceType);
                serviceDate = itemView.findViewById(R.id.serviceDate);
                clientId = itemView.findViewById(R.id.clientId);
                completeButton = itemView.findViewById(R.id.completeButton);
            }
            
            public void bind(BookedService service, DocumentSnapshot snapshot) {
                serviceName.setText(service.getName());
                serviceType.setText(service.getType());
                
                // Format timestamp
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                
                // Safely handle the timestamp (which might be null in some cases)
                Timestamp timestamp = service.getTimestamp();
                if (timestamp != null) {
                    try {
                        Date date = timestamp.toDate();
                        serviceDate.setText(sdf.format(date));
                    } catch (Exception e) {
                        // Log exception but don't crash
                        android.util.Log.e("ServiceAdapter", "Error formatting timestamp", e);
                        serviceDate.setText("Invalid date format");
                    }
                } else {
                    serviceDate.setText("No date available");
                }
                
                // Display client ID (shortened)
                String userIdDisplay = service.getUserId();
                if (userIdDisplay != null && userIdDisplay.length() > 8) {
                    userIdDisplay = userIdDisplay.substring(0, 8) + "...";
                }
                clientId.setText("Client: " + userIdDisplay);
                
                // Ensure transactionId is set from the document
                if (service.getTransactionId() == null && snapshot.contains("transactionId")) {
                    service.setTransactionId(snapshot.getString("transactionId"));
                }
                
                // Set up Complete button action
                completeButton.setOnClickListener(v -> markServiceAsComplete(service, snapshot));
            }
            
            private void markServiceAsComplete(BookedService service, DocumentSnapshot snapshot) {
                // Show a loading dialog
                ProgressDialog progressDialog = new ProgressDialog(admin.this);
                progressDialog.setMessage("Marking service as complete...");
                progressDialog.setCancelable(false);
                progressDialog.show();
                
                try {
                    // Update the status in Firestore
                    snapshot.getReference().update("status", "completed")
                        .addOnSuccessListener(aVoid -> {
                            // Update the local model
                            service.setStatus("completed");
                            
                            // Update the transaction status if needed
                            String transactionId = service.getTransactionId();
                            if (transactionId != null && !transactionId.isEmpty()) {
                                updateTransactionStatus(service);
                            }
                            
                            // Dismiss the dialog
                            progressDialog.dismiss();
                            
                            // Show a success message
                            Toast.makeText(admin.this, "Service marked as complete", Toast.LENGTH_SHORT).show();
                            
                            // Refresh the list to update UI
                            refreshServicesList();
                        })
                        .addOnFailureListener(e -> {
                            // Dismiss the dialog
                            progressDialog.dismiss();
                            
                            // Show an error message
                            Toast.makeText(admin.this, "Failed to mark service as complete: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                            Log.e("Admin", "Error marking service as complete: " + e.getMessage());
                        });
                } catch (Exception e) {
                    // Dismiss dialog
                    progressDialog.dismiss();
                    
                    // Log and show error
                    Log.e("Admin", "Exception marking service as complete: " + e.getMessage());
                    Toast.makeText(admin.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            
            private void updateTransactionStatus(BookedService service) {
                try {
                    if (service.getUserId() == null || service.getName() == null) return;
                    
                    // Query for the matching transaction in the user's transactions
                    firestore.collection("Users")
                            .document(service.getUserId())
                            .collection("Transactions")
                            .whereEqualTo("description", "Service: " + service.getName())
                            .whereEqualTo("type", "service")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                                    doc.getReference().update("status", "Completed");
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Admin", "Error updating transaction status: " + e.getMessage());
                            });
                } catch (Exception e) {
                    Log.e("Admin", "Exception in updateTransactionStatus: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Initialize the orders recycler view to show orders grouped by user
     */
    private void setupOrdersRecyclerView() {
        try {
            // Set up RecyclerView
            ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            
            // Set empty state text
            TextView emptyOrdersText = findViewById(R.id.emptyOrdersText);
            
            // Load all orders grouped by user
            loadOrdersByUser();
            
        } catch (Exception e) {
            // Handle initialization errors
            Log.e("Admin", "Error setting up orders recycler view: " + e.getMessage());
            Toast.makeText(this, "Error loading orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Load all orders and group them by user
     */
    private void loadOrdersByUser() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading orders...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Create a map to group orders by user ID
        Map<String, List<Map<String, Object>>> ordersByUser = new TreeMap<>();
        List<UserOrdersEntry> entries = new ArrayList<>();
        
        // Use get() instead of real-time listener to avoid index issues
        firestore.collectionGroup("Orders")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView emptyOrdersText = findViewById(R.id.emptyOrdersText);
                        if (emptyOrdersText != null) {
                            emptyOrdersText.setVisibility(View.VISIBLE);
                            ordersRecyclerView.setVisibility(View.GONE);
                        }
                        return;
                    }
                    
                    // Process each order document
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Map<String, Object> orderData = doc.getData();
                        String path = doc.getReference().getPath();
                        
                        if (orderData != null) {
                            // Add document ID and path to the data
                            orderData.put("id", doc.getId());
                            orderData.put("path", path);
                            
                            // Extract timestamp for client-side sorting
                            Object timestampObj = orderData.get("timestamp");
                            if (timestampObj instanceof Timestamp) {
                                orderData.put("timestampMillis", ((Timestamp) timestampObj).toDate().getTime());
                            }
                            
                            // Get user ID for grouping
                            String userId = (String) orderData.get("userId");
                            if (userId != null) {
                                // If this user exists in the map, add to their list, otherwise create a new list
                                if (!ordersByUser.containsKey(userId)) {
                                    ordersByUser.put(userId, new ArrayList<>());
                                }
                                ordersByUser.get(userId).add(orderData);
                            }
                        }
                    }
                    
                    // Once we have all orders grouped by user, fetch user details for each user
                    for (String userId : ordersByUser.keySet()) {
                        List<Map<String, Object>> userOrders = ordersByUser.get(userId);
                        
                        // Sort orders by timestamp (descending)
                        Collections.sort(userOrders, (a, b) -> {
                            Long timeA = a.containsKey("timestampMillis") ? (Long) a.get("timestampMillis") : 0L;
                            Long timeB = b.containsKey("timestampMillis") ? (Long) b.get("timestampMillis") : 0L;
                            return timeB.compareTo(timeA); // Descending order
                        });
                        
                        fetchUserDetailsForOrders(userId, userOrders, entries);
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    
                    Log.e(TAG, "Error fetching orders by user", e);
                    Toast.makeText(admin.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    // Show the empty state with error message
                    TextView emptyOrdersText = findViewById(R.id.emptyOrdersText);
                    if (emptyOrdersText != null) {
                        emptyOrdersText.setText("Error loading orders: " + e.getMessage());
                        emptyOrdersText.setVisibility(View.VISIBLE);
                        ordersRecyclerView.setVisibility(View.GONE);
                    }
                });
    }
    
    /**
     * Fetch user details and build the entries list for the adapter
     */
    private void fetchUserDetailsForOrders(String userId, List<Map<String, Object>> userOrders, 
                                         List<UserOrdersEntry> entries) {
        firestore.collection("Users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    String userName = "Unknown User";
                    String userEmail = "No email";
                    
                    if (userDoc.exists()) {
                        // Try to get name from various fields
                        String firstName = userDoc.getString("firstName");
                        String lastName = userDoc.getString("lastName");
                        String name = userDoc.getString("Name");
                        String displayName = userDoc.getString("displayName");
                        
                        if (firstName != null || lastName != null) {
                            userName = (firstName != null ? firstName : "") + 
                                      (lastName != null ? " " + lastName : "");
                        } else if (name != null) {
                            userName = name;
                        } else if (displayName != null) {
                            userName = displayName;
                        }
                        
                        // Try to get email
                        String email = userDoc.getString("email");
                        if (email == null) {
                            email = userDoc.getString("Email");
                        }
                        if (email != null) {
                            userEmail = email;
                        }
                    }
                    
                    // Add entry with user details and their orders
                    UserOrdersEntry entry = new UserOrdersEntry(userId, userName, userEmail, userOrders);
                    entries.add(entry);
                    
                    // Update adapter
                    UserOrdersAdapter adapter = new UserOrdersAdapter(entries, admin.this);
                    ordersRecyclerView.setAdapter(adapter);
                    
                    // Show recycler view and hide empty text
                    TextView emptyOrdersText = findViewById(R.id.emptyOrdersText);
                    if (emptyOrdersText != null) {
                        emptyOrdersText.setVisibility(View.GONE);
                        ordersRecyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user details", e);
                    
                    // Still add the entry with unknown user
                    UserOrdersEntry entry = new UserOrdersEntry(userId, "Unknown User", "Error loading email", userOrders);
                    entries.add(entry);
                    
                    // Update adapter
                    UserOrdersAdapter adapter = new UserOrdersAdapter(entries, admin.this);
                    ordersRecyclerView.setAdapter(adapter);
                    
                    // Show recycler view and hide empty text
                    TextView emptyOrdersText = findViewById(R.id.emptyOrdersText);
                    if (emptyOrdersText != null) {
                        emptyOrdersText.setVisibility(View.GONE);
                        ordersRecyclerView.setVisibility(View.VISIBLE);
                    }
                });
    }
    
    /**
     * Refreshes the orders list by reloading all data
     */
    private void refreshOrdersList() {
        // Reload all orders grouped by user
        loadOrdersByUser();
    }

    /**
     * Show full order details in a dialog
     */
    void showOrderDetails(String orderPath) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Showing order details for path: " + orderPath);
        db.document(orderPath).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> orderData = documentSnapshot.getData();
                if (orderData == null) {
                    Toast.makeText(admin.this, "Failed to load order data (null map).", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Order data map is null for path: " + orderPath);
                    return;
                }
                Log.d(TAG, "OrderData for dialog: " + orderData.toString()); // Log all order data

                AlertDialog.Builder builder = new AlertDialog.Builder(admin.this);
                LayoutInflater inflater = LayoutInflater.from(admin.this);
                View dialogView = inflater.inflate(R.layout.dialog_admin_order_details, null);
                builder.setView(dialogView);

                // --- Populate Order Info ---
                TextView dialogOrderNumber = dialogView.findViewById(R.id.dialogOrderNumber);
                TextView dialogOrderDate = dialogView.findViewById(R.id.dialogOrderDate);
                TextView dialogOrderStatus = dialogView.findViewById(R.id.dialogOrderStatus);
                TextView dialogOrderEstimatedDelivery = dialogView.findViewById(R.id.dialogOrderEstimatedDelivery);
                
                // Status update controls
                AutoCompleteTextView statusSpinner = dialogView.findViewById(R.id.dialogOrderStatusSpinner);
                // Set up the adapter for the dropdown
                ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(
                        admin.this, R.array.admin_order_status_options, android.R.layout.simple_dropdown_item_1line);
                statusSpinner.setAdapter(statusAdapter);
                Button updateStatusBtn = dialogView.findViewById(R.id.dialogUpdateStatusBtn);
                Button setDeliveryDateBtn = dialogView.findViewById(R.id.dialogSetDeliveryDateBtn);
                
                // Payment summary TextViews - ensure these IDs exist in dialog_admin_order_details.xml
                TextView dialogOrderSubtotal = dialogView.findViewById(R.id.dialogOrderSubtotal);
                TextView dialogOrderTax = dialogView.findViewById(R.id.dialogOrderTax);
                TextView dialogOrderShippingFee = dialogView.findViewById(R.id.dialogOrderShippingFee);
                TextView dialogOrderTotal = dialogView.findViewById(R.id.dialogOrderTotal);

                // Use subcollection document ID as order number
                String orderNumber = "UNKNOWN";
                if (orderPath != null && !orderPath.isEmpty()) {
                    String[] pathSegments = orderPath.split("/");
                    if (pathSegments.length >= 4) {
                        orderNumber = pathSegments[pathSegments.length - 1];
                    }
                }
                dialogOrderNumber.setText("Order #: " + orderNumber);

                // Date (match order list logic)
                Object timestampObj = orderData.get("timestamp");
                if (timestampObj == null) {
                    timestampObj = orderData.get("orderDate");
                }
                String formattedDate = null;
                if (timestampObj instanceof com.google.firebase.Timestamp) {
                    com.google.firebase.Timestamp ts = (com.google.firebase.Timestamp) timestampObj;
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault());
                    formattedDate = sdf.format(ts.toDate());
                } else if (timestampObj instanceof Long) {
                    java.util.Date date = new java.util.Date((Long) timestampObj);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault());
                    formattedDate = sdf.format(date);
                }
                if (formattedDate != null) {
                    dialogOrderDate.setText("Date: " + formattedDate);
                } else {
                    dialogOrderDate.setText("Date: -");
                }

                // Get current status
                String currentStatus = (String) orderData.getOrDefault("trackingStatus", "ordered");
                dialogOrderStatus.setText("Status: " + currentStatus);
                
                // Set spinner to current status
                statusSpinner.setText(statusAdapter.getItem(getStatusPosition(currentStatus)).toString(), false);

                // Disable controls if status is already delivered or cancelled
                if ("delivered".equals(currentStatus.toLowerCase()) || "cancelled".equals(currentStatus.toLowerCase())) {
                    // Hide status update controls
                    TextView updateStatusLabel = dialogView.findViewById(R.id.updateStatusLabel);
                    updateStatusLabel.setVisibility(View.GONE);
                    
                    // Hide status dropdown
                    TextInputLayout statusInputLayout = dialogView.findViewById(R.id.statusInputLayout);
                    statusInputLayout.setVisibility(View.GONE);
                    
                    // Hide buttons container
                    LinearLayout buttonsContainer = dialogView.findViewById(R.id.statusButtonsContainer);
                    buttonsContainer.setVisibility(View.GONE);
                    
                    // Disable spinner
                    statusSpinner.setEnabled(false);
                    
                    // Add message to inform admin that order is completed or cancelled
                    TextView deliveredMessage = dialogView.findViewById(R.id.dialogDeliveredMessage);
                    if (deliveredMessage != null) {
                        if ("delivered".equals(currentStatus.toLowerCase())) {
                            deliveredMessage.setText("This order has been delivered. No further status updates are possible.");
                            deliveredMessage.setTextColor(getResources().getColor(R.color.success));
                        } else {
                            deliveredMessage.setText("This order has been cancelled. No further status updates are possible.");
                            deliveredMessage.setTextColor(getResources().getColor(R.color.error));
                        }
                        deliveredMessage.setVisibility(View.VISIBLE);
                    }
                }

                // Set up delivery date display
                Object estDeliveryObj = orderData.get("estimatedDelivery");
                if (estDeliveryObj instanceof Timestamp) {
                    Timestamp estDeliveryTimestamp = (Timestamp) estDeliveryObj;
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    dialogOrderEstimatedDelivery.setText("Est. Delivery: " + sdf.format(estDeliveryTimestamp.toDate()));
                } else if (estDeliveryObj != null) {
                    dialogOrderEstimatedDelivery.setText("Est. Delivery: To be determined");
                }

                // Get stored shipping fee and tax amount
                double shippingFee = 0.0;
                if (orderData.get("shippingFee") instanceof Number) {
                    shippingFee = ((Number) orderData.get("shippingFee")).doubleValue();
                }
                dialogOrderShippingFee.setText(String.format(Locale.getDefault(), "Shipping: ₱%.2f", shippingFee));

                double taxAmount = 0.0;
                if (orderData.get("taxAmount") instanceof Number) {
                    taxAmount = ((Number) orderData.get("taxAmount")).doubleValue();
                }
                dialogOrderTax.setText(String.format(Locale.getDefault(), "Tax: ₱%.2f", taxAmount));

                Object totalAmountObj = orderData.get("totalAmount");
                if (totalAmountObj instanceof Number) {
                    dialogOrderTotal.setText(String.format(Locale.getDefault(), "Total: ₱%.2f", ((Number) totalAmountObj).doubleValue()));
                } else {
                    dialogOrderTotal.setText("Total: N/A");
                }

                // --- Populate Customer Info ---
                TextView dialogCustomerName = dialogView.findViewById(R.id.dialogCustomerName);
                TextView dialogCustomerEmail = dialogView.findViewById(R.id.dialogCustomerEmail);
                TextView dialogCustomerStreetAddress = dialogView.findViewById(R.id.dialogCustomerStreetAddress);
                TextView dialogCustomerCity = dialogView.findViewById(R.id.dialogCustomerCity);
                TextView dialogCustomerProvince = dialogView.findViewById(R.id.dialogCustomerProvince);
                TextView dialogCustomerPostalCode = dialogView.findViewById(R.id.dialogCustomerPostalCode);
                TextView dialogCustomerCountry = dialogView.findViewById(R.id.dialogCustomerCountry);

                String userId = (String) orderData.get("userId");
                if (userId != null && !userId.isEmpty()) {
                    db.collection("Users").document(userId).get().addOnSuccessListener(userDoc -> {
                        String fullName = "";
                        String firstName = userDoc.getString("firstName");
                        String lastName = userDoc.getString("lastName");
                        if (firstName != null || lastName != null) {
                            if (firstName != null) fullName += firstName;
                            if (lastName != null) {
                                if (!fullName.isEmpty()) fullName += " ";
                                fullName += lastName;
                            }
                        }
                        if (fullName.isEmpty()) {
                            String name = userDoc.getString("Name");
                            if (name != null && !name.isEmpty()) {
                                fullName = name;
                            } else {
                                String displayName = userDoc.getString("displayName");
                                if (displayName != null && !displayName.isEmpty()) {
                                    fullName = displayName;
                                }
                            }
                        }
                        dialogCustomerName.setText("Name: " + (!fullName.isEmpty() ? fullName : "N/A"));

                        String email = userDoc.getString("email");
                        if (email == null || email.isEmpty()) {
                            email = userDoc.getString("Email");
                        }
                        dialogCustomerEmail.setText("Email: " + (email != null && !email.isEmpty() ? email : "N/A"));
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching user details for dialog", e);
                        dialogCustomerName.setText("Name: Error");
                        dialogCustomerEmail.setText("Email: Error");
                    });
                } else {
                    dialogCustomerName.setText("Name: N/A");
                    dialogCustomerEmail.setText("Email: N/A");
                }

                // Populate detailed address
                Object shippingAddressObj = orderData.get("shippingAddress");
                Log.d(TAG, "ShippingAddress object from orderData: " + shippingAddressObj);

                if (shippingAddressObj instanceof Map) {
                    Map<String, Object> shippingAddress = (Map<String, Object>) shippingAddressObj;
                    Log.d(TAG, "ShippingAddress map content: " + shippingAddress.toString());
                    dialogCustomerStreetAddress.setText("Street: " + shippingAddress.getOrDefault("streetAddress", "N/A").toString());
                    dialogCustomerCity.setText("City: " + shippingAddress.getOrDefault("city", "N/A").toString());
                    dialogCustomerProvince.setText("Province: " + shippingAddress.getOrDefault("province", "N/A").toString());
                    dialogCustomerPostalCode.setText("Postal Code: " + shippingAddress.getOrDefault("postalCode", "N/A").toString());
                    dialogCustomerCountry.setText("Country: " + shippingAddress.getOrDefault("country", "N/A").toString());
                } else {
                    Log.e(TAG, "shippingAddress is not a Map or is null. Actual type: " + (shippingAddressObj != null ? shippingAddressObj.getClass().getName() : "null"));
                    dialogCustomerStreetAddress.setText("Street: N/A (Data Error)");
                    dialogCustomerCity.setText("City: N/A (Data Error)");
                    dialogCustomerProvince.setText("Province: N/A (Data Error)");
                    dialogCustomerPostalCode.setText("Postal Code: N/A (Data Error)");
                    dialogCustomerCountry.setText("Country: N/A (Data Error)");
                }

                // --- Populate Items ---
                RecyclerView dialogOrderItemsRecyclerView = dialogView.findViewById(R.id.dialogOrderItemsRecyclerView);
                TextView dialogNoItemsText = dialogView.findViewById(R.id.dialogNoItemsText);

                List<Map<String, Object>> itemsList = new ArrayList<>();
                AdminDialogOrderItemsAdapter itemsAdapter = new AdminDialogOrderItemsAdapter(itemsList);
                dialogOrderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(admin.this));
                dialogOrderItemsRecyclerView.setAdapter(itemsAdapter);

                // Create the AlertDialog
                AlertDialog dialog = new AlertDialog.Builder(admin.this)
                    .setView(dialogView)
                    .create();
                
                // Set up the status update button click listener
                updateStatusBtn.setOnClickListener(v -> {
                    // Find the position of the selected item in the adapter
                    String selectedStatus = statusSpinner.getText().toString();
                    int position = -1;
                    for (int i = 0; i < statusAdapter.getCount(); i++) {
                        if (selectedStatus.equals(statusAdapter.getItem(i).toString())) {
                            position = i;
                            break;
                        }
                    }
                    
                    if (position >= 0) {
                        String newStatus = getStatusFromPosition(position);
                        if (!newStatus.equals(currentStatus)) {
                            updateOrderStatus(orderPath, newStatus);
                            dialogOrderStatus.setText("Status: " + newStatus);
                            Toast.makeText(admin.this, "Order status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                            
                            // If status is delivered, refresh the orders list
                            if ("delivered".equals(newStatus)) {
                                refreshOrdersList();
                            }
                        }
                    }
                });
                
                // Set up the delivery date button click listener
                setDeliveryDateBtn.setOnClickListener(v -> {
                    // Create a calendar instance to get current date
                    final Calendar calendar = Calendar.getInstance();
                    int year = calendar.get(Calendar.YEAR);
                    int month = calendar.get(Calendar.MONTH);
                    int day = calendar.get(Calendar.DAY_OF_MONTH);

                    // Create a date picker dialog
                    android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                        admin.this,
                        (view, selectedYear, selectedMonth, selectedDay) -> {
                            // Create a calendar with selected date
                            Calendar selectedCalendar = Calendar.getInstance();
                            selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                            
                            // Set time to noon (12:00 PM)
                            selectedCalendar.set(Calendar.HOUR_OF_DAY, 12);
                            selectedCalendar.set(Calendar.MINUTE, 0);
                            selectedCalendar.set(Calendar.SECOND, 0);
                            
                            // Convert to Timestamp
                            Date selectedDate = selectedCalendar.getTime();
                            Timestamp deliveryTimestamp = new Timestamp(selectedDate);
                            
                            // Save to Firestore and update UI
                            updateDeliveryDate(orderPath, deliveryTimestamp);
                            
                            // Update the display in the dialog
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                            dialogOrderEstimatedDelivery.setText("Est. Delivery: " + sdf.format(selectedDate));
                        },
                        year, month, day);

                    // Set min date to current date
                    datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
                    
                    // Show dialog
                    datePickerDialog.show();
                });

                // Load items for this order
                firestore.document(orderPath).collection("Items").get().addOnSuccessListener(itemsSnapshot -> {
                    if (itemsSnapshot.isEmpty()) {
                        dialogNoItemsText.setVisibility(View.VISIBLE);
                        dialogOrderItemsRecyclerView.setVisibility(View.GONE);
                    } else {
                        dialogNoItemsText.setVisibility(View.GONE);
                        dialogOrderItemsRecyclerView.setVisibility(View.VISIBLE);
                        for (DocumentSnapshot itemDoc : itemsSnapshot.getDocuments()) {
                            itemsList.add(itemDoc.getData());
                        }
                        itemsAdapter.notifyDataSetChanged();
                        
                        // Calculate subtotal from items to display
                        double calculatedSubtotal = 0;
                        for(Map<String, Object> item : itemsList) {
                            double itemPrice = 0;
                            int itemQuantity = 0;
                            if (item.get("price") instanceof Number) {
                                itemPrice = ((Number) item.get("price")).doubleValue();
                            }
                            if (item.get("quantity") instanceof Number) {
                                itemQuantity = ((Number) item.get("quantity")).intValue();
                            }
                            calculatedSubtotal += itemPrice * itemQuantity;
                        }
                        dialogOrderSubtotal.setText(String.format(Locale.getDefault(), "Subtotal: ₱%.2f", calculatedSubtotal));
                    }
                    
                    // Set up the toolbar navigation icon (close button)
                    MaterialToolbar toolbar = dialogView.findViewById(R.id.dialogToolbar);
                    if (toolbar != null) {
                        toolbar.setNavigationOnClickListener(v -> {
                            dialog.dismiss();
                        });
                    }
                    
                    // Show the dialog
                    dialog.show();

                }).addOnFailureListener(e -> {
                    dialogNoItemsText.setText("Error loading items: " + e.getMessage());
                    dialogNoItemsText.setVisibility(View.VISIBLE);
                    dialogOrderItemsRecyclerView.setVisibility(View.GONE);
                    Toast.makeText(admin.this, "Error loading order items: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    
                    // Set up the toolbar for error dialog
                    MaterialToolbar errorToolbar = dialogView.findViewById(R.id.dialogToolbar);
                    if (errorToolbar != null) {
                        errorToolbar.setNavigationOnClickListener(v -> {
                            dialog.dismiss();
                        });
                    }
                    
                    // Show the dialog even if items failed to load
                    dialog.show();
                });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(admin.this, "Error loading order details: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
    
    /**
     * Convert tracking status string to spinner position
     * This matches the admin_order_status_options array in order_arrays.xml
     */
    private int getStatusPosition(String status) {
        switch (status.toLowerCase()) {
            case "ordered": return 0;
            case "processing": return 1;
            case "shipped": return 2;
            case "delivered": return 3;
            case "cancelled": return 4;
            default: return 0;
        }
    }
    
    /**
     * Convert spinner position to tracking status string
     * This matches the admin_order_status_options array in order_arrays.xml
     */
    private String getStatusFromPosition(int position) {
        switch (position) {
            case 0: return "ordered";
            case 1: return "processing";
            case 2: return "shipped";
            case 3: return "delivered";
            case 4: return "cancelled";
            default: return "ordered";
        }
    }
    
    /**
     * Update order tracking status in Firestore
     */
    private void updateOrderStatus(String path, String newStatus) {
        try {
            // First check if the order is already delivered or cancelled
            firestore.document(path)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String currentStatus = documentSnapshot.getString("trackingStatus");
                            
                            // If already delivered or cancelled, show message and do nothing
                            if ("delivered".equals(currentStatus)) {
                                Toast.makeText(admin.this, "Order is already delivered and cannot be modified.", 
                                        Toast.LENGTH_SHORT).show();
                                return;
                            } else if ("cancelled".equals(currentStatus)) {
                                Toast.makeText(admin.this, "Order is already cancelled and cannot be modified.", 
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // If not delivered or cancelled, proceed with update
                            firestore.document(path)
                                    .update("trackingStatus", newStatus)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Admin", "Order status updated to " + newStatus);
                                        
                                        // If status is "delivered", also update the order status field
                                        if ("delivered".equals(newStatus)) {
                                            firestore.document(path).update("status", "completed");
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(admin.this, "Failed to update order status: " + e.getMessage(), 
                                                Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(admin.this, "Failed to check order status: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e("Admin", "Exception updating order: " + e.getMessage());
            Toast.makeText(admin.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Update delivery date in Firestore
     */
    private void updateDeliveryDate(String orderPath, Timestamp deliveryDate) {
        try {
            // First check if the order is already delivered or cancelled
            firestore.document(orderPath)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String currentStatus = documentSnapshot.getString("trackingStatus");
                            
                            // If already delivered or cancelled, show message and do nothing
                            if ("delivered".equals(currentStatus)) {
                                Toast.makeText(admin.this, "Order is already delivered and cannot be modified.", 
                                        Toast.LENGTH_SHORT).show();
                                return;
                            } else if ("cancelled".equals(currentStatus)) {
                                Toast.makeText(admin.this, "Order is already cancelled and cannot be modified.", 
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // If not delivered or cancelled, proceed with update
                            firestore.document(orderPath)
                                .update("estimatedDelivery", deliveryDate)
                                .addOnSuccessListener(aVoid -> {
                                    // Format date for display
                                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                                    String formattedDate = sdf.format(deliveryDate.toDate());
                                    
                                    Toast.makeText(admin.this, "Delivery date set to " + formattedDate, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Admin", "Error updating delivery date: " + e.getMessage());
                                    Toast.makeText(admin.this, "Failed to set delivery date: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(admin.this, "Failed to check order status: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e("Admin", "Exception setting delivery date: " + e.getMessage());
            Toast.makeText(admin.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Set up appointments recycler view
    private void setupAppointmentsRecyclerView() {
        // Initialize appointmentsList if null
        if (appointmentsList == null) {
            appointmentsList = new ArrayList<>();
        }
        
        // Create adapter if it doesn't exist yet
        if (appointmentAdapter == null) {
            appointmentAdapter = new AppointmentAdapter(this, appointmentsList, new AppointmentAdapter.OnAppointmentActionListener() {
                @Override
                public void onContactUser(AppointmentModel appointment) {
                    contactUser(appointment);
                }

                @Override
                public void onMarkComplete(AppointmentModel appointment, int position) {
                    markAppointmentComplete(appointment, position);
                }
            });
            
            // Set layout manager and adapter for the recycler view
            appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            appointmentsRecyclerView.setAdapter(appointmentAdapter);
        }
        
        // Load appointments from Firestore
        loadAppointments();
    }
    
    // Handle contacting a user
    private void contactUser(AppointmentModel appointment) {
        // Get user data and start message activity
        firestore.collection("Users").document(appointment.getUserId())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Create user model for messaging
                    AdminModel user = new AdminModel();
                    user.setUserID(appointment.getUserId());
                    user.setName(appointment.getUserName());
                    user.setEmail(appointment.getUserEmail());
                    
                    // Start message activity
                    Intent intent = new Intent(admin.this, com.business.techassist.menucomponents.messages.messageActivity.class);
                    com.business.techassist.utilities.AndroidUtil.passUserDataMessages(intent, user);
                    startActivity(intent);
                } else {
                    Toast.makeText(admin.this, "User not found", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(admin.this, "Error contacting user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    // Handle marking an appointment as complete
    private void markAppointmentComplete(AppointmentModel appointment, int position) {
        // Show confirmation dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Complete Appointment");
        builder.setMessage("Are you sure you want to mark this appointment as completed?");
        builder.setPositiveButton("Complete", (dialog, which) -> {
            // Show progress dialog
            ProgressDialog progressDialog = new ProgressDialog(admin.this);
            progressDialog.setMessage("Updating appointment status...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            
            // Update appointment status in Firestore
            firestore.collection("Appointments").document(appointment.getId())
                .update("status", "Completed")
                .addOnSuccessListener(aVoid -> {
                    // Increment completed jobs counter for the technician
                    incrementCompletedJobs(appointment.getTechnicianId());
                    
                    // Add transaction to user's transaction history
                    addTransactionForAppointment(appointment);
                    
                    // Update local appointment
                    appointment.setStatus("Completed");
                    appointmentAdapter.notifyItemChanged(position);
                    
                    // Add notification for the user
                    addCompletionNotification(appointment);
                    
                    progressDialog.dismiss();
                    
                    // Show success message with Toast instead of Snackbar to avoid view parent issues
                    Toast.makeText(admin.this, "Appointment marked as completed", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(admin.this, "Error updating appointment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    // Increment the completedJobs field for a technician
    private void incrementCompletedJobs(String technicianId) {
        // Make sure we're not trying to increment completedJobs on a null or empty technician ID
        if (technicianId == null || technicianId.isEmpty()) {
            Log.e(TAG, "Cannot increment completedJobs: technician ID is null or empty");
            return;
        }

        try {
            // Use a transaction to safely increment the completedJobs field
            firestore.runTransaction(transaction -> {
                DocumentReference technicianRef = firestore.collection("Users").document(technicianId);
                DocumentSnapshot snapshot = transaction.get(technicianRef);
                
                if (snapshot.exists()) {
                    // Check if the completedJobs field exists
                    if (snapshot.contains("completedJobs")) {
                        // Get the current value (default to 0 if it can't be converted to Number)
                        long currentValue = 0;
                        Object completedJobsObj = snapshot.get("completedJobs");
                        if (completedJobsObj instanceof Number) {
                            currentValue = ((Number) completedJobsObj).longValue();
                        }
                        
                        // Increment by 1
                        transaction.update(technicianRef, "completedJobs", currentValue + 1);
                    } else {
                        // Field doesn't exist, create with value 1
                        transaction.update(technicianRef, "completedJobs", 1);
                    }
                } else {
                    Log.e(TAG, "Cannot increment completedJobs: technician document doesn't exist");
                }
                
                // Return result value (not used but required by transaction)
                return null;
            }).addOnSuccessListener(result -> {
                Log.d(TAG, "Successfully updated completedJobs for technician: " + technicianId);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to update completedJobs: " + e.getMessage());
                
                // Fallback method in case transaction fails
                fallbackIncrementCompletedJobs(technicianId);
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in incrementCompletedJobs: " + e.getMessage());
            
            // Try fallback method
            fallbackIncrementCompletedJobs(technicianId);
        }
    }
    
    // Fallback method to increment completedJobs if transaction fails
    private void fallbackIncrementCompletedJobs(String technicianId) {
        try {
            // Directly update using FieldValue.increment(1)
            firestore.collection("Users").document(technicianId)
                .update("completedJobs", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully incremented completed jobs using fallback method");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in fallback increment method: " + e.getMessage());
                    
                    // Last resort: try to set the value directly after getting current value
                    getAndSetCompletedJobs(technicianId);
                });
        } catch (Exception e) {
            Log.e(TAG, "Exception in fallback method: " + e.getMessage());
            
            // Try last resort method
            getAndSetCompletedJobs(technicianId);
        }
    }
    
    // Last resort method: get current value and set new value
    private void getAndSetCompletedJobs(String technicianId) {
        firestore.collection("Users").document(technicianId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Get current value, default to 0 if not found or not a number
                    long currentValue = 0;
                    Object completedJobsObj = documentSnapshot.get("completedJobs");
                    if (completedJobsObj instanceof Number) {
                        currentValue = ((Number) completedJobsObj).longValue();
                    }
                    
                    // Increment and set the new value
                    firestore.collection("Users").document(technicianId)
                        .update("completedJobs", currentValue + 1)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Successfully set completed jobs using last resort method");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to set completed jobs value: " + e.getMessage());
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get document for last resort method: " + e.getMessage());
            });
    }

    // Add a transaction entry for completed appointment
    private void addTransactionForAppointment(AppointmentModel appointment) {
        try {
            // Create transaction data
            Map<String, Object> transactionData = new HashMap<>();
            transactionData.put("amount", appointment.getServicePrice());
            transactionData.put("description", "Appointment: " + appointment.getServiceType());
            transactionData.put("status", "Completed");
            transactionData.put("type", "appointment");
            transactionData.put("timestamp", FieldValue.serverTimestamp());
            
            // Add transaction to user's transaction history
            firestore.collection("Users")
                .document(appointment.getUserId())
                .collection("Transactions")
                .add(transactionData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Transaction added for appointment: " + appointment.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding transaction for appointment", e);
                });
        } catch (Exception e) {
            Log.e(TAG, "Exception in addTransactionForAppointment: " + e.getMessage());
        }
    }

    // Add a notification for the user when their appointment is completed
    private void addCompletionNotification(AppointmentModel appointment) {
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", "Appointment Completed");
        notificationData.put("body", "Your " + appointment.getServiceType() + " appointment has been marked as completed");
        notificationData.put("timestamp", FieldValue.serverTimestamp());
        notificationData.put("type", "appointment");
        notificationData.put("read", false);
        
        firestore.collection("Users").document(appointment.getUserId())
            .collection("Notifications")
            .add(notificationData)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Notification added for user: " + appointment.getUserId());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding notification", e);
            });
    }
    
    // Load appointments from Firestore
    private void loadAppointments() {
        appointmentsList.clear();
        
        if (appointmentsListener != null) {
            appointmentsListener.remove();
        }
        
        // Get the current user ID (technician)
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Query appointments where technicianId matches the current user
        appointmentsListener = firestore.collection("Appointments")
            .whereEqualTo("technicianId", currentUserId)
            .addSnapshotListener((queryDocumentSnapshots, e) -> {
                if (e != null) {
                    Log.e(TAG, "Error loading appointments", e);
                    return;
                }
                
                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    appointmentsList.clear();
                    
                    // Sort by tier priority and date
                    List<DocumentSnapshot> sortedDocs = new ArrayList<>(queryDocumentSnapshots.getDocuments());
                    Collections.sort(sortedDocs, (doc1, doc2) -> {
                        // First sort by tier/subscription level (Diamond > Gold > Silver > Bronze/Basic)
                        String tier1 = doc1.getString("userSubscriptionTier");
                        String tier2 = doc2.getString("userSubscriptionTier");
                        
                        int tier1Rank = getTierRank(tier1);
                        int tier2Rank = getTierRank(tier2);
                        
                        if (tier1Rank != tier2Rank) {
                            return tier2Rank - tier1Rank; // Higher tier first
                        }
                        
                        // Then sort by appointment date
                        Timestamp date1 = doc1.getTimestamp("appointmentDate");
                        Timestamp date2 = doc2.getTimestamp("appointmentDate");
                        
                        if (date1 != null && date2 != null) {
                            return date1.compareTo(date2);
                        } else if (date1 != null) {
                            return -1;
                        } else if (date2 != null) {
                            return 1;
                        }
                        
                        return 0;
                    });
                    
                    for (DocumentSnapshot document : sortedDocs) {
                        // Create appointment from document
                        AppointmentModel appointment = document.toObject(AppointmentModel.class);
                        if (appointment != null) {
                            appointment.setId(document.getId());
                            
                            // Get user details
                            String userId = document.getString("userId");
                            if (userId != null) {
                                fetchUserDetails(userId, appointment);
                            }
                            
                            appointmentsList.add(appointment);
                        }
                    }
                    
                    // Show empty view if needed
                    if (appointmentsList.isEmpty()) {
                        emptyAppointmentsText.setVisibility(View.VISIBLE);
                        appointmentsRecyclerView.setVisibility(View.GONE);
                    } else {
                        emptyAppointmentsText.setVisibility(View.GONE);
                        appointmentsRecyclerView.setVisibility(View.VISIBLE);
                    }
                    
                    // Update adapter
                    appointmentAdapter.notifyDataSetChanged();
                } else {
                    // No appointments found
                    appointmentsList.clear();
                    emptyAppointmentsText.setVisibility(View.VISIBLE);
                    appointmentsRecyclerView.setVisibility(View.GONE);
                    
                    if (appointmentAdapter != null) {
                        appointmentAdapter.notifyDataSetChanged();
                    }
                }
            });
    }
    
    // Fetch user details for an appointment
    private void fetchUserDetails(String userId, AppointmentModel appointment) {
        firestore.collection("Users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Get user name
                    String name = documentSnapshot.getString("name");
                    if (name == null) {
                        name = documentSnapshot.getString("Name");
                    }
                    appointment.setUserName(name);
                    
                    // Get user email
                    String email = documentSnapshot.getString("email");
                    if (email == null) {
                        email = documentSnapshot.getString("Email");
                    }
                    appointment.setUserEmail(email);
                    
                    // Get user profile image
                    String profileImage = documentSnapshot.getString("profileImage");
                    if (profileImage == null) {
                        profileImage = documentSnapshot.getString("photoURL");
                    }
                    appointment.setUserProfileImage(profileImage);
                    
                    // Get user subscription plan
                    String subscriptionPlan = documentSnapshot.getString("SubscriptionPlan");
                    if (subscriptionPlan != null) {
                        // Map subscription plan to tier
                        String tier;
                        switch (subscriptionPlan) {
                            case "Premium":
                                tier = "Gold";
                                break;
                            case "Standard":
                                tier = "Silver";
                                break;
                            case "Business":
                                tier = "Diamond";
                                break;
                            default:
                                tier = "Bronze";
                                break;
                        }
                        appointment.setUserSubscriptionTier(tier);
                    } else {
                        appointment.setUserSubscriptionTier("Bronze");
                    }
                    
                    // Update the adapter
                    if (appointmentAdapter != null) {
                        appointmentAdapter.notifyDataSetChanged();
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching user details", e);
            });
    }
    
    // Get numeric rank for subscription tier
    private int getTierRank(String tier) {
        if (tier == null) return 0;
        
        switch (tier) {
            case "Diamond": return 4;
            case "Gold": return 3;
            case "Silver": return 2;
            case "Bronze": return 1;
            default: return 0;
        }
    }

    // Helper method to update empty state visibility
    private void updateEmptyState(int itemCount, RecyclerView recyclerView, TextView emptyView) {
        if (itemCount == 0) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}

/**
 * Data class to hold user information and their orders
 */
class UserOrdersEntry {
    private final String userId;
    private final String userName;
    private final String userEmail;
    private final List<Map<String, Object>> orders;
    private boolean expanded = false;
    
    public UserOrdersEntry(String userId, String userName, String userEmail, List<Map<String, Object>> orders) {
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.orders = orders;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public List<Map<String, Object>> getOrders() {
        return orders;
    }
    
    public boolean isExpanded() {
        return expanded;
    }
    
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
    
    public void toggleExpanded() {
        this.expanded = !this.expanded;
    }
}

/**
 * Adapter to display users with their orders
 */
class UserOrdersAdapter extends RecyclerView.Adapter<UserOrdersAdapter.ViewHolder> {
    private final List<UserOrdersEntry> entries;
    private final admin adminActivity;
    
    public UserOrdersAdapter(List<UserOrdersEntry> entries, admin adminActivity) {
        this.entries = entries;
        this.adminActivity = adminActivity;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_orders, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserOrdersEntry entry = entries.get(position);
        
        // Set user info
        holder.userName.setText(entry.getUserName());
        holder.userEmail.setText(entry.getUserEmail());
        
        // Show/hide orders based on expansion state
        if (entry.isExpanded()) {
            holder.ordersContainer.setVisibility(View.VISIBLE);
            holder.expandIcon.setImageResource(R.drawable.ic_expand_less_24);
        } else {
            holder.ordersContainer.setVisibility(View.GONE);
            holder.expandIcon.setImageResource(R.drawable.ic_expand_more_24);
        }
        
        // Set click listener for expansion
        holder.headerContainer.setOnClickListener(v -> {
            entry.toggleExpanded();
            notifyItemChanged(position);
        });
        
        // Clear previous orders
        holder.ordersLayout.removeAllViews();
        
        // Add each order
        for (Map<String, Object> order : entry.getOrders()) {
            View orderView = LayoutInflater.from(holder.itemView.getContext())
                    .inflate(R.layout.item_order_in_user, holder.ordersLayout, false);
            
            TextView orderNumber = orderView.findViewById(R.id.orderNumber);
            TextView orderDate = orderView.findViewById(R.id.orderDate);
            TextView orderAmount = orderView.findViewById(R.id.orderAmount);
            TextView orderStatus = orderView.findViewById(R.id.orderStatus);
            Button btnDetails = orderView.findViewById(R.id.btnOrderDetails);
            
            // Set order data
            String path = (String) order.get("path");
            String id = (String) order.get("id");
            orderNumber.setText("Order #" + id);
            
            // Order date
            Object timestampObj = order.get("timestamp");
            if (timestampObj instanceof Timestamp) {
                Timestamp timestamp = (Timestamp) timestampObj;
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                orderDate.setText(sdf.format(timestamp.toDate()));
            } else {
                orderDate.setText("Unknown date");
            }
            
            // Order amount
            Object totalObj = order.get("totalAmount");
            if (totalObj instanceof Number) {
                double total = ((Number) totalObj).doubleValue();
                orderAmount.setText(String.format(Locale.getDefault(), "₱%.2f", total));
            } else {
                orderAmount.setText("₱0.00");
            }
            
            // Order status
            String status = (String) order.getOrDefault("trackingStatus", "ordered");
            orderStatus.setText(status);
            
            // Set status color based on tracking status
            int statusColor;
            switch (status.toLowerCase()) {
                case "processing":
                    statusColor = holder.itemView.getContext().getResources().getColor(R.color.warning);
                    break;
                case "shipped":
                    statusColor = holder.itemView.getContext().getResources().getColor(R.color.primary);
                    break;
                case "delivered":
                    statusColor = holder.itemView.getContext().getResources().getColor(R.color.success);
                    break;
                default: // ordered
                    statusColor = holder.itemView.getContext().getResources().getColor(R.color.secondary);
                    break;
            }
            orderStatus.setTextColor(statusColor);
            
            // Set click listener for details button
            final String orderPath = path; // Create a final variable for the lambda
            btnDetails.setOnClickListener(v -> {
                if (orderPath != null) {
                    adminActivity.showOrderDetails(orderPath);
                }
            });
            
            // Add this order view to container
            holder.ordersLayout.addView(orderView);
        }
    }
    
    @Override
    public int getItemCount() {
        return entries.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userEmail;
        ImageView expandIcon;
        LinearLayout headerContainer, ordersContainer;
        LinearLayout ordersLayout;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            userEmail = itemView.findViewById(R.id.userEmail);
            expandIcon = itemView.findViewById(R.id.expandIcon);
            headerContainer = itemView.findViewById(R.id.headerContainer);
            ordersContainer = itemView.findViewById(R.id.ordersContainer);
            ordersLayout = itemView.findViewById(R.id.ordersLayout);
        }
    }
}

class AdminDialogOrderItemsAdapter extends RecyclerView.Adapter<AdminDialogOrderItemsAdapter.ViewHolder> {
    private List<Map<String, Object>> items;

    public AdminDialogOrderItemsAdapter(List<Map<String, Object>> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_product_dialog, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = items.get(position);
        holder.productName.setText(item.get("productName") != null ? item.get("productName").toString() : "N/A");
        Object quantityObj = item.get("quantity");
        if (quantityObj instanceof Number) {
             holder.quantity.setText("x" + ((Number) quantityObj).intValue());
        } else if (quantityObj != null) {
             holder.quantity.setText("x" + quantityObj.toString());
        } else {
             holder.quantity.setText("x0");
        }
       
        Object priceObj = item.get("price");
        double priceVal = 0.0;
        if (priceObj instanceof Number) {
            priceVal = ((Number)priceObj).doubleValue();
        }
        holder.price.setText("₱" + String.format(Locale.US, "%.2f", priceVal));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView productName, quantity, price;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.dialogItemProductName);
            quantity = itemView.findViewById(R.id.dialogItemQuantity);
            price = itemView.findViewById(R.id.dialogItemPrice);
        }
    }
}