package com.business.techassist.menucomponents;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.business.techassist.R;
import com.business.techassist.profileHome;
import com.business.techassist.shop;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class trackOrderMenu extends AppCompatActivity implements OrderAdapter.OnOrderClickListener {
    private static final String TAG = "TrackOrderMenu";
    
    // UI elements
    private MaterialToolbar toolbar;
    private TextInputLayout orderSearchLayout;
    private TextInputEditText orderSearchInput;
    private RecyclerView ordersRecyclerView;
    private View emptyOrdersState;
    private MaterialButton shopNowBtn;
    private MaterialCardView exampleOrderCard;
    private ExtendedFloatingActionButton refreshOrdersFab;
    private MaterialButton viewDetailsBtn;

    // Data
    private List<Order> masterOrderList; // Holds all orders, items populate asynchronously
    private OrderAdapter orderAdapter;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration ordersListener;
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.menu_track_order);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        // Initialize data
        masterOrderList = new ArrayList<>(); // Initialize master list
        
        // Initialize UI components
        initializeUI();
        
        // Set up click listeners
        setupClickListeners();
        
        // Setup window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Setup recycler view
        setupRecyclerView();
        
        // Fetch orders with real-time updates
        setupOrdersListener();
    }
    
    private void initializeUI() {
        // Initialize toolbar and navigation
        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
        // Initialize search UI
        orderSearchLayout = findViewById(R.id.orderSearchLayout);
        orderSearchInput = findViewById(R.id.orderSearchInput);
        
        // Initialize empty state and recycler view
        emptyOrdersState = findViewById(R.id.emptyOrdersState);
        ordersRecyclerView = findViewById(R.id.ordersRecyclerView);
        
        // Initialize buttons
        shopNowBtn = findViewById(R.id.shopNowBtn);
        refreshOrdersFab = findViewById(R.id.refreshOrdersFab);
        
        // Initialize example order card
        exampleOrderCard = findViewById(R.id.exampleOrderCard);
        viewDetailsBtn = findViewById(R.id.viewDetailsBtn);
        
        // Setup search text watcher
        orderSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase();
                applySearchFilterAndUpdateAdapter();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }
    
    private void setupRecyclerView() {
        // Setup recycler view with adapter
        orderAdapter = new OrderAdapter(this, masterOrderList, this);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ordersRecyclerView.setAdapter(orderAdapter);
    }
    
    private void setupClickListeners() {
        // Setup shop now button to navigate to shop
        shopNowBtn.setOnClickListener(v -> {
            // Navigate to MainActivity and request shop fragment to be shown
            Intent intent = new Intent(trackOrderMenu.this, com.business.techassist.MainActivity.class);
            intent.putExtra("SHOW_FRAGMENT", "shop");
            startActivity(intent);
            finish();
        });
        
        // Setup refresh orders FAB
        refreshOrdersFab.setOnClickListener(v -> {
            Snackbar.make(findViewById(R.id.main), "Refreshing orders...", Snackbar.LENGTH_SHORT).show();
            refreshOrdersFab.setEnabled(false);
            
            // Clear search query
            if (orderSearchInput.getText() != null && orderSearchInput.getText().length() > 0) {
                orderSearchInput.setText("");
            }
            
            // Re-setup listener
            if (ordersListener != null) {
                ordersListener.remove();
            }
            setupOrdersListener();
            
            // Re-enable button after a delay
            refreshOrdersFab.postDelayed(() -> refreshOrdersFab.setEnabled(true), 1000);
        });
        
        // Setup view details button
        viewDetailsBtn.setOnClickListener(v -> {
            // Show an example order details experience
            Snackbar.make(findViewById(R.id.main), "Example order details - In a real order, you would see your items and status", Snackbar.LENGTH_LONG).show();
        });
    }
    
    private void setupOrdersListener() {
        if (currentUser == null) {
            showEmptyState("Please sign in to view your orders");
            return;
        }
        
        String userId = currentUser.getUid();
        
        Query query = db.collection("Users")
                .document(userId)
                .collection("Orders")
                .orderBy("timestamp", Query.Direction.DESCENDING);
        
        ordersListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed:", e);
                Snackbar.make(findViewById(R.id.main), "Failed to load orders: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                showEmptyState("Could not load orders. Please try again."); 
                return;
            }
            
            if (snapshots == null) {
                Log.d(TAG, "Snapshots object is null");
                showEmptyState("You don't have any pending orders to track");
                updateAdapterWithProcessedOrders(Collections.emptyList()); // Update with empty list
                return;
            }

            List<Order> newOrdersFromSnapshot = new ArrayList<>();
            for (DocumentSnapshot document : snapshots.getDocuments()) {
                Order order = Order.fromFirestore(document.getId(), document.getData());
                if (order.getTrackingStatus() != null && 
                    order.getTrackingStatus().equalsIgnoreCase("delivered")) {
                    continue; // Skip delivered orders
                }
                newOrdersFromSnapshot.add(order);
            }
            
            // Update master list carefully, preserving existing item data if possible
            // or simply replace and re-fetch items for simplicity here
            masterOrderList.clear();
            masterOrderList.addAll(newOrdersFromSnapshot);

            if (masterOrderList.isEmpty()) {
                showEmptyState("You don't have any pending orders to track");
                updateAdapterWithProcessedOrders(Collections.emptyList());
                return;
            }
            
            // Initially display orders; items will be fetched asynchronously.
            // The search filter will be re-applied as items load.
            applySearchFilterAndUpdateAdapter(); 

            // Fetch items for all orders in the new snapshot
            for (Order order : masterOrderList) {
                 // Check if items are already loaded or being loaded to avoid re-fetching unnecessarily if not desired
                if (order.getItems() == null || order.getItems().isEmpty()) { 
                    fetchOrderItems(order);
                }
            }
        });
    }
    
    private void fetchOrderItems(Order order) {
        if (currentUser == null || order == null) return;
        
        String orderId = order.getId();
        if (orderId == null) {
            Log.e(TAG, "Order ID is null, cannot fetch items.");
            return;
        }

        db.collection("Users")
                .document(currentUser.getUid())
                .collection("Orders")
                .document(orderId)
                .collection("Items")
                .get()
                .addOnSuccessListener(itemsSnapshot -> {
                    if (order.getItems() == null) { // Ensure items list is initialized
                        order.setItems(new ArrayList<>());
                    } else {
                        order.getItems().clear(); // Clear previous items if re-fetching
                    }

                    if (itemsSnapshot != null && !itemsSnapshot.isEmpty()) {
                        for (DocumentSnapshot itemDoc : itemsSnapshot.getDocuments()) {
                            String productName = itemDoc.getString("productName");
                            double price = itemDoc.contains("price") && itemDoc.get("price") instanceof Number ? 
                                           itemDoc.getDouble("price") : 0.0;
                            long quantityLong = itemDoc.contains("quantity") && itemDoc.get("quantity") instanceof Number ? 
                                              itemDoc.getLong("quantity") : 0L;
                            
                            order.addItem(new Order.OrderItem(productName, price, (int) quantityLong));
                        }
                    }
                    // Items for this specific order are now updated in the masterOrderList.
                    // Re-apply search and update the adapter.
                    applySearchFilterAndUpdateAdapter();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching items for order " + orderId + ": " + e.getMessage());
                    // Optionally, still update the adapter so other orders are shown
                    applySearchFilterAndUpdateAdapter(); 
                });
    }
    
    // Renamed from filterOrders for clarity and to reflect its new role
    private void applySearchFilterAndUpdateAdapter() {
        List<Order> filteredList = new ArrayList<>();
        String currentSearchQuery = orderSearchInput.getText().toString().toLowerCase().trim();

        if (currentSearchQuery.isEmpty()) {
            filteredList.addAll(masterOrderList);
        } else {
            for (Order order : masterOrderList) {
                boolean matches = false;
                if (order.getOrderNumber() != null && order.getOrderNumber().toLowerCase().contains(currentSearchQuery)) {
                    matches = true;
                }
                // Check items only if not already matched by order number
                if (!matches && order.getItems() != null) { 
                    for (Order.OrderItem item : order.getItems()) {
                        if (item.getProductName() != null && item.getProductName().toLowerCase().contains(currentSearchQuery)) {
                            matches = true;
                            break;
                        }
                    }
                }
                // Also check if status matches the search query
                if (!matches && order.getTrackingStatus() != null && order.getTrackingStatus().toLowerCase().contains(currentSearchQuery)) {
                    matches = true;
                }

                if (matches) {
                    filteredList.add(order);
                }
            }
        }
        updateAdapterWithProcessedOrders(filteredList);
    }
    
    // Renamed from updateOrdersList for clarity
    private void updateAdapterWithProcessedOrders(List<Order> ordersToDisplay) {
        if (ordersToDisplay.isEmpty()) {
            if (orderSearchInput.getText().toString().trim().isEmpty()) {
                 showEmptyState("You don't have any pending orders to track");
            } else {
                 showEmptyState("No orders match your search: '" + orderSearchInput.getText().toString().trim() + "'");
            }
        } else {
            showOrders();
        }
        // OrderAdapter should handle this list (e.g., using DiffUtil or notifyDataSetChanged)
        if (orderAdapter != null) {
            orderAdapter.updateOrders(ordersToDisplay); 
        }
    }
    
    private void showEmptyState(String message) {
        ordersRecyclerView.setVisibility(View.GONE);
        emptyOrdersState.setVisibility(View.VISIBLE);
        
        // Update empty state message if a TextView is available
        TextView emptyStateMessage = emptyOrdersState.findViewById(R.id.emptyStateText);
        if (emptyStateMessage != null) {
            emptyStateMessage.setText(message);
        }
    }
    
    private void showOrders() {
        emptyOrdersState.setVisibility(View.GONE);
        ordersRecyclerView.setVisibility(View.VISIBLE);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listener
        if (ordersListener != null) {
            ordersListener.remove();
        }
    }

    // OrderAdapter.OnOrderClickListener implementation
    @Override
    public void onOrderClick(Order order) {
        if (order == null || order.getId() == null) {
            Log.e(TAG, "Cannot view order details: order or order ID is null");
            Snackbar.make(findViewById(R.id.main), "Cannot view order details: Invalid order data", Snackbar.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Viewing order details for order ID: " + order.getId());
        
        // Start OrderDetailsActivity with the order ID
        Intent intent = new Intent(this, OrderDetailsActivity.class);
        intent.putExtra("ORDER_ID", order.getId());
        startActivity(intent);
    }

    @Override
    public void onViewDetailsClick(Order order) {
        if (order == null || order.getId() == null) {
            Log.e(TAG, "Cannot view order details: order or order ID is null");
            Snackbar.make(findViewById(R.id.main), "Cannot view order details: Invalid order data", Snackbar.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "View details button clicked for order ID: " + order.getId());
        
        // Start OrderDetailsActivity with the order ID
        Intent intent = new Intent(this, OrderDetailsActivity.class);
        intent.putExtra("ORDER_ID", order.getId());
        startActivity(intent);
    }
}