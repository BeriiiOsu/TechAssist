package com.business.techassist.menucomponents.cart;

import static android.content.ContentValues.TAG;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.R;
import com.business.techassist.UserCredentials.UserAddressPaymentManager;
import com.business.techassist.shopitems.DatabaseHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class cart extends AppCompatActivity implements CartAdapter.CartItemListener {

    private RecyclerView cartRecyclerView;
    private LinearLayout emptyStateView;
    private ConstraintLayout cartItemsContainer;
    private MaterialButton checkoutBtn, clearCartBtn;
    private TextView subtotalText, deliveryText, taxText, totalText;
    private List<CartItem> cartItems;
    private CartAdapter cartAdapter;
    private FirebaseFirestore db;
    private String userId;
    private DatabaseHelper dbHelper;

    // Constants for calculations
    private static final double TAX_RATE = 0.12; // 12% tax
    private static final double DELIVERY_FEE = 50.0; // Fixed delivery fee

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cart);
        
        // Initialize insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "guest";
        dbHelper = new DatabaseHelper(this);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        loadCartItems();
    }

    private void initializeViews() {
        // Find views in layout
        cartRecyclerView = findViewById(R.id.cartView);
        emptyStateView = findViewById(R.id.emptyState);
        cartItemsContainer = findViewById(R.id.cartItemsContainer);
        checkoutBtn = findViewById(R.id.checkoutBtn);
        clearCartBtn = findViewById(R.id.clearCartBtn);
        
        // Order summary views
        subtotalText = findViewById(R.id.totalFeeTxt);
        deliveryText = findViewById(R.id.deliveryTxt);
        taxText = findViewById(R.id.taxTxt);
        totalText = findViewById(R.id.totalTxt);
        
        // Set button click listeners
        checkoutBtn.setOnClickListener(v -> processCheckout());
        clearCartBtn.setOnClickListener(v -> showClearCartConfirmation());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        cartItems = new ArrayList<>();
        cartAdapter = new CartAdapter(this, cartItems, this);
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartRecyclerView.setAdapter(cartAdapter);
    }

    private void loadCartItems() {
        db.collection("Users").document(userId)
            .collection("Cart")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                cartItems.clear();
                
                if (queryDocumentSnapshots.isEmpty()) {
                    showEmptyState();
                    return;
                }
                
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    CartItem item = document.toObject(CartItem.class);
                    if (item != null) {
                        item.setId(document.getId());
                        
                        // Load image from local storage
                        byte[] imageBytes = dbHelper.getProductImage(item.getProductName());
                        if (imageBytes != null) {
                            item.setImage(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
                        }
                        
                        cartItems.add(item);
                    }
                }
                
                if (cartItems.isEmpty()) {
                    showEmptyState();
                } else {
                    showCartItems();
                    cartAdapter.notifyDataSetChanged();
                    updateOrderSummary();
                }
            })
            .addOnFailureListener(e -> {
                Snackbar.make(findViewById(R.id.main), "Failed to load cart: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                showEmptyState();
            });
    }

    private void showEmptyState() {
        emptyStateView.setVisibility(View.VISIBLE);
        cartItemsContainer.setVisibility(View.GONE);
        clearCartBtn.setVisibility(View.GONE);
    }

    private void showCartItems() {
        emptyStateView.setVisibility(View.GONE);
        cartItemsContainer.setVisibility(View.VISIBLE);
        clearCartBtn.setVisibility(View.VISIBLE);
    }

    private void updateOrderSummary() {
        double subtotal = 0;
        boolean containsHardware = false;
        
        for (CartItem item : cartItems) {
            subtotal += item.getTotalPrice();
            
            // Check if cart contains any hardware products
            if (item.isHardware() || "hardware".equalsIgnoreCase(item.getProductType())) {
                containsHardware = true;
            }
        }
        
        double tax = subtotal * TAX_RATE;
        
        // Apply delivery fee only if cart contains hardware products
        double delivery = (subtotal > 0 && containsHardware) ? DELIVERY_FEE : 0;
        
        double total = subtotal + tax + delivery;
        
        // Update UI
        subtotalText.setText(String.format("₱%.2f", subtotal));
        taxText.setText(String.format("₱%.2f", tax));
        deliveryText.setText(String.format("₱%.2f", delivery));
        totalText.setText(String.format("₱%.2f", total));
    }

    private void processCheckout() {
        if (cartItems.isEmpty()) {
            return;
        }
        
        // Show payment dialog with cart items
        showPaymentDialog();
    }
    
    private void showPaymentDialog() {
        // Get default address and payment method from UserAddressPaymentManager
        UserAddressPaymentManager userManager = new UserAddressPaymentManager();
        
        // Create dialog builder early
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_purchase, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        // Get references to views
        ImageView productImage = dialogView.findViewById(R.id.productImage);
        TextView productNameView = dialogView.findViewById(R.id.productName);
        TextView productPriceView = dialogView.findViewById(R.id.productPrice);
        TextView productQuantityView = dialogView.findViewById(R.id.productQuantity);
        TextView subtotalText = dialogView.findViewById(R.id.subtotalTxt);
        TextView taxText = dialogView.findViewById(R.id.taxTxt);
        TextView deliveryText = dialogView.findViewById(R.id.deliveryTxt);
        TextView totalText = dialogView.findViewById(R.id.totalTxt);
        RadioGroup paymentMethodGroup = dialogView.findViewById(R.id.paymentMethodGroup);
        RadioButton creditCardRadio = dialogView.findViewById(R.id.creditCardRadio);
        RadioButton paypalRadio = dialogView.findViewById(R.id.paypalRadio);
        RadioButton gcashRadio = dialogView.findViewById(R.id.gcashRadio);
        RadioButton codRadio = dialogView.findViewById(R.id.codRadio);
        MaterialButton confirmButton = dialogView.findViewById(R.id.confirmButton);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelButton);
        
        // Get counts and set summary info
        int itemCount = cartItems.size();
        int totalQuantity = 0;
        for (CartItem item : cartItems) {
            totalQuantity += item.getQuantity();
        }
        
        // Set summary details
        if (itemCount == 1) {
            // If only one item, show its details
            CartItem singleItem = cartItems.get(0);
            productNameView.setText(singleItem.getProductName());
            productPriceView.setText(String.format("₱%.2f", singleItem.getPrice()));
            productQuantityView.setText(String.format("Quantity: %d", singleItem.getQuantity()));
            
            // Set product image if available
            if (singleItem.getImage() != null) {
                productImage.setImageBitmap(singleItem.getImage());
            } else {
                // Use placeholder if no image
                productImage.setImageResource(R.drawable.store_icon);
            }
        } else {
            // If multiple items, show summary
            productNameView.setText(String.format("Multiple Items (%d)", itemCount));
            productPriceView.setText("Multiple prices");
            productQuantityView.setText(String.format("Total items: %d", totalQuantity));
            productImage.setImageResource(R.drawable.cart_new_icon);
        }
        
        // Calculate order summary
        double subtotal = 0;
        boolean containsHardware = false;
        
        for (CartItem item : cartItems) {
            subtotal += item.getTotalPrice();
            
            // Check if cart contains any hardware products
            if (item.isHardware() || "hardware".equalsIgnoreCase(item.getProductType())) {
                containsHardware = true;
            }
        }
        
        double tax = subtotal * TAX_RATE;
        
        // Apply delivery fee only if cart contains hardware products
        double deliveryFee = containsHardware ? DELIVERY_FEE : 0;
        
        double total = subtotal + tax + deliveryFee;
        
        // Update summary texts
        subtotalText.setText(String.format("₱%.2f", subtotal));
        taxText.setText(String.format("₱%.2f", tax));
        deliveryText.setText(String.format("₱%.2f", deliveryFee));
        totalText.setText(String.format("₱%.2f", total));
        
        // Hide COD option
        codRadio.setVisibility(View.GONE);
        
        // Initialize payment method radio buttons as disabled
        creditCardRadio.setEnabled(false);
        paypalRadio.setEnabled(false);
        gcashRadio.setEnabled(false);
        creditCardRadio.setAlpha(0.5f);
        paypalRadio.setAlpha(0.5f);
        gcashRadio.setAlpha(0.5f);
            
        // First, load all payment methods from subcollection
        userManager.getPaymentMethods(new UserAddressPaymentManager.PaymentMethodsLoadCallback() {
            @Override
            public void onPaymentMethodsLoaded(List<Map<String, Object>> paymentMethods) {
                if (paymentMethods.isEmpty()) {
                    // No payment methods available
                    Toast.makeText(cart.this, "Please add a payment method in settings", Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Variables to track payment method types
                boolean hasCreditCard = false;
                boolean hasPaypal = false;
                boolean hasGcash = false;
                boolean hasMaya = false;
                Map<String, Object> defaultPaymentMethod = null;
                
                // Check which payment methods are available and find default
                for (Map<String, Object> paymentMethod : paymentMethods) {
                    String type = (String) paymentMethod.get("type");
                    Boolean isDefault = (Boolean) paymentMethod.get("isDefault");
                    
                    if (type != null) {
                        if (type.equalsIgnoreCase("Credit Card")) {
                            hasCreditCard = true;
                            creditCardRadio.setEnabled(true);
                            creditCardRadio.setAlpha(1.0f);
                            creditCardRadio.setText("Credit Card (Saved)");
                        } else if (type.equalsIgnoreCase("PayPal")) {
                            hasPaypal = true;
                            paypalRadio.setEnabled(true);
                            paypalRadio.setAlpha(1.0f);
                            paypalRadio.setText("PayPal (Saved)");
                        } else if (type.equalsIgnoreCase("GCash")) {
                            hasGcash = true;
                            gcashRadio.setEnabled(true);
                            gcashRadio.setAlpha(1.0f);
                            gcashRadio.setText("GCash (Saved)");
                        } else if (type.equalsIgnoreCase("Maya")) {
                            hasMaya = true;
                            // Maya option not shown in this dialog
                        }
                    }
                    
                    // Store default payment method
                    if (isDefault != null && isDefault) {
                        defaultPaymentMethod = paymentMethod;
                    }
                }
                
                // Now load shipping address
                boolean finalHasCreditCard = hasCreditCard;
                boolean finalHasPaypal = hasPaypal;
                Map<String, Object> finalDefaultPaymentMethod = defaultPaymentMethod;
                boolean finalHasGcash = hasGcash;
                boolean finalHasMaya = hasMaya;
                userManager.getDefaultAddress(new UserAddressPaymentManager.AddressesLoadCallback() {
                    @Override
                    public void onAddressesLoaded(List<Map<String, Object>> addresses) {
                        // Populate address section if address is available
                        TextView addressLabelText = dialogView.findViewById(R.id.addressLabelText);
                        TextView addressLine1 = dialogView.findViewById(R.id.addressLine1);
                        TextView addressLine2 = dialogView.findViewById(R.id.addressLine2);
                        
                        if (!addresses.isEmpty()) {
                            Map<String, Object> defaultAddress = addresses.get(0);
                            
                            // Extract address components
                            String label = getStringOrEmpty(defaultAddress.get("label"));
                            String streetAddress = getStringOrEmpty(defaultAddress.get("streetAddress"));
                            String city = getStringOrEmpty(defaultAddress.get("city"));
                            String province = getStringOrEmpty(defaultAddress.get("province"));
                            String postalCode = getStringOrEmpty(defaultAddress.get("postalCode"));
                            String country = getStringOrEmpty(defaultAddress.get("country"));
                            
                            // Set address label
                            addressLabelText.setText(label);
                            
                            // Set address line 1
                            addressLine1.setText(streetAddress);
                            
                            // Build address line 2
                            StringBuilder addressLine2Text = new StringBuilder();
                            if (!city.isEmpty() && !city.equals("N/A")) {
                                addressLine2Text.append(city);
                            }
                            if (!province.isEmpty() && !province.equals("N/A")) {
                                if (addressLine2Text.length() > 0) addressLine2Text.append(", ");
                                addressLine2Text.append(province);
                            }
                            if (!postalCode.isEmpty() && !postalCode.equals("N/A")) {
                                if (addressLine2Text.length() > 0) addressLine2Text.append(", ");
                                addressLine2Text.append(postalCode);
                            }
                            if (!country.isEmpty() && !country.equals("N/A")) {
                                if (addressLine2Text.length() > 0) addressLine2Text.append(", ");
                                addressLine2Text.append(country);
                            }
                            
                            addressLine2.setText(addressLine2Text.toString());
                        } else {
                            // No address available
                            addressLabelText.setText("No Address");
                            addressLine1.setText("Please add an address in settings");
                            addressLine2.setText("");
                        }
                        
                        // No methods available
                        if (!finalHasCreditCard && !finalHasPaypal && !finalHasGcash && !finalHasMaya) {
                            Toast.makeText(cart.this, "Please add a payment method in settings", Toast.LENGTH_LONG).show();
                            return;
                        }
                        
                        // Select the default payment method
                        if (finalDefaultPaymentMethod != null) {
                            String defaultType = (String) finalDefaultPaymentMethod.get("type");
                            if (defaultType != null) {
                                if (defaultType.equalsIgnoreCase("Credit Card") && finalHasCreditCard) {
                                    creditCardRadio.setChecked(true);
                                } else if (defaultType.equalsIgnoreCase("PayPal") && finalHasPaypal) {
                                    paypalRadio.setChecked(true);
                                } else if (defaultType.equalsIgnoreCase("GCash") && finalHasGcash) {
                                    gcashRadio.setChecked(true);
                                }
                            }
                        } else {
                            // No default set, use first available
                            if (finalHasCreditCard) {
                                creditCardRadio.setChecked(true);
                            } else if (finalHasPaypal) {
                                paypalRadio.setChecked(true);
                            } else if (finalHasGcash) {
                                gcashRadio.setChecked(true);
                            }
                        }
                        
                        // Show the dialog now that payment methods and address are loaded
                        dialog.show();
                        
                        // Set button click listeners
                        confirmButton.setOnClickListener(v -> {
                            // Get selected payment method
                            int selectedId = paymentMethodGroup.getCheckedRadioButtonId();
                            String paymentMethod = "";
                            
                            if (selectedId == R.id.creditCardRadio && finalHasCreditCard) {
                                paymentMethod = "Credit Card";
                            } else if (selectedId == R.id.paypalRadio && finalHasPaypal) {
                                paymentMethod = "PayPal";
                            } else if (selectedId == R.id.gcashRadio && finalHasGcash) {
                                paymentMethod = "GCash";
                            } else {
                                // No valid payment method selected
                                Toast.makeText(cart.this, "Please select a valid payment method", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // Process payment
                            processPayment(paymentMethod, total);
                            dialog.dismiss();
                        });
                    }
                    
                    @Override
                    public void onFailure(String errorMessage) {
                        // Failed to load address, continue with payment methods
                        Toast.makeText(cart.this, "Failed to load address: " + errorMessage, Toast.LENGTH_SHORT).show();
                        
                        // Set default text for address fields
                        TextView addressLabelText = dialogView.findViewById(R.id.addressLabelText);
                        TextView addressLine1 = dialogView.findViewById(R.id.addressLine1);
                        TextView addressLine2 = dialogView.findViewById(R.id.addressLine2);
                        
                        addressLabelText.setText("No Address");
                        addressLine1.setText("Please add an address in settings");
                        addressLine2.setText("");
                        
                        // Continue with showing dialog and setting up payment methods
                        // Select the default payment method
                        if (finalDefaultPaymentMethod != null) {
                            String defaultType = (String) finalDefaultPaymentMethod.get("type");
                            if (defaultType != null) {
                                if (defaultType.equalsIgnoreCase("Credit Card") && finalHasCreditCard) {
                                    creditCardRadio.setChecked(true);
                                } else if (defaultType.equalsIgnoreCase("PayPal") && finalHasPaypal) {
                                    paypalRadio.setChecked(true);
                                } else if (defaultType.equalsIgnoreCase("GCash") && finalHasGcash) {
                                    gcashRadio.setChecked(true);
                                }
                            }
                        } else {
                            // No default set, use first available
                            if (finalHasCreditCard) {
                                creditCardRadio.setChecked(true);
                            } else if (finalHasPaypal) {
                                paypalRadio.setChecked(true);
                            } else if (finalHasGcash) {
                                gcashRadio.setChecked(true);
                            }
                        }
                        
                        // Show the dialog
                        dialog.show();
                        
                        // Set button click listeners
                        confirmButton.setOnClickListener(v -> {
                            // Get selected payment method
                            int selectedId = paymentMethodGroup.getCheckedRadioButtonId();
                            String paymentMethod = "";
                            
                            if (selectedId == R.id.creditCardRadio && finalHasCreditCard) {
                                paymentMethod = "Credit Card";
                            } else if (selectedId == R.id.paypalRadio && finalHasPaypal) {
                                paymentMethod = "PayPal";
                            } else if (selectedId == R.id.gcashRadio && finalHasGcash) {
                                paymentMethod = "GCash";
                            } else {
                                // No valid payment method selected
                                Toast.makeText(cart.this, "Please select a valid payment method", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // Process payment
                            processPayment(paymentMethod, total);
                            dialog.dismiss();
                        });
                    }
                });
            }
            
            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(cart.this, "Failed to load payment methods: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
    }
    
    private void processPayment(String paymentMethod, double amount) {
        // Ensure all items have their product type properly set
        for (CartItem item : cartItems) {
            item.inferProductType();
        }
        
        // Determine if order contains hardware products
        boolean containsHardware = false;
        for (CartItem item : cartItems) {
            if (item.isHardware() || "hardware".equalsIgnoreCase(item.getProductType())) {
                containsHardware = true;
                break;
            }
        }

        // Calculate shipping fee based on product types
        double shippingFee = containsHardware ? DELIVERY_FEE : 0;
        
        // Calculate subtotal
        double subtotal = 0;
        for (CartItem item : cartItems) {
            subtotal += item.getTotalPrice();
        }
        
        // Calculate tax
        double tax = subtotal * TAX_RATE;
        
        // Create an order in Firestore
        Map<String, Object> order = new HashMap<>();
        order.put("userId", userId);
        order.put("items", cartItems.size());
        order.put("totalAmount", amount);
        order.put("subtotal", subtotal);
        order.put("taxAmount", tax);
        order.put("shippingFee", shippingFee);
        order.put("paymentMethod", paymentMethod);
        order.put("status", "pending");
        order.put("trackingStatus", "ordered");
        order.put("orderDate", com.google.firebase.Timestamp.now());
        order.put("timestamp", com.google.firebase.Timestamp.now());
        order.put("containsHardware", containsHardware);
        
        // Get address from the subcollection (default address)
        UserAddressPaymentManager userManager = new UserAddressPaymentManager();
        userManager.getDefaultAddress(new UserAddressPaymentManager.AddressesLoadCallback() {
            @Override
            public void onAddressesLoaded(List<Map<String, Object>> addresses) {
                if (!addresses.isEmpty()) {
                    // Use the first address (which is the default one)
                    Map<String, Object> defaultAddress = addresses.get(0);
                    
                    // Create shipping address map
                    Map<String, Object> shippingAddress = new HashMap<>();
                    
                    // Extract address components
                    String label = getStringOrEmpty(defaultAddress.get("label"));
                    String streetAddress = getStringOrEmpty(defaultAddress.get("streetAddress"));
                    String city = getStringOrEmpty(defaultAddress.get("city"));
                    String province = getStringOrEmpty(defaultAddress.get("province"));
                    String postalCode = getStringOrEmpty(defaultAddress.get("postalCode"));
                    String country = getStringOrEmpty(defaultAddress.get("country"));
                    
                    // Add valid fields to the shipping address map
                    if (!streetAddress.isEmpty() && !streetAddress.equals("N/A")) {
                        shippingAddress.put("streetAddress", streetAddress);
                    }
                    if (!city.isEmpty() && !city.equals("N/A")) {
                        shippingAddress.put("city", city);
                    }
                    if (!province.isEmpty() && !province.equals("N/A")) {
                        shippingAddress.put("province", province);
                    }
                    if (!postalCode.isEmpty() && !postalCode.equals("N/A")) {
                        shippingAddress.put("postalCode", postalCode);
                    }
                    if (!country.isEmpty() && !country.equals("N/A")) {
                        shippingAddress.put("country", country);
                    }
                    if (!label.isEmpty() && !label.equals("N/A")) {
                        shippingAddress.put("label", label);
                    }
                    
                    // Add shipping address to order if we have address data
                    if (!shippingAddress.isEmpty()) {
                        order.put("shippingAddress", shippingAddress);
                        
                        // Log the shipping address
                        Log.d(TAG, "Using default shipping address: " + streetAddress + ", " + city);
                    }
                    
                    // Continue with creating the order
                    createOrder(order, paymentMethod, amount);
                } else {
                    // No address available, fallback to user document
                    fetchAddressFromUserDoc(order, paymentMethod, amount);
                }
            }
            
            @Override
            public void onFailure(String errorMessage) {
                // Error fetching address from subcollection, fallback to user document
                Log.e(TAG, "Error fetching address from subcollection: " + errorMessage);
                fetchAddressFromUserDoc(order, paymentMethod, amount);
            }
        });
    }
    
    // New method to create the order, extracted from processPayment
    private void createOrder(Map<String, Object> order, String paymentMethod, double amount) {
        db.collection("Users").document(userId)
           .collection("Orders")
           .add(order)
           .addOnSuccessListener(documentReference -> {
               String orderId = documentReference.getId();
               
               // Add the cart items to the order
               for (CartItem item : cartItems) {
                   Map<String, Object> orderItem = new HashMap<>();
                   orderItem.put("productName", item.getProductName());
                   orderItem.put("price", item.getPrice());
                   orderItem.put("quantity", item.getQuantity());
                   orderItem.put("productType", item.getProductType());
                   
                   db.collection("Users").document(userId)
                       .collection("Orders").document(orderId)
                       .collection("Items")
                       .add(orderItem);
               }
               
               // Update stock quantities for all items
               updateProductStocks();
               
               // Create transaction record
               createTransactionRecord(paymentMethod, amount);
               
               // Create notification
               createPurchaseNotification(amount);
               
               // Clear cart after successful checkout
               clearCart();
               
               // Show success message
               Snackbar.make(findViewById(R.id.main), "Order placed successfully!", Snackbar.LENGTH_LONG).show();
           })
           .addOnFailureListener(e -> {
               Snackbar.make(findViewById(R.id.main), "Failed to place order: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
           });
    }
    
    /**
     * Update product stock quantities for all cart items
     */
    private void updateProductStocks() {
        // First get all product categories
        FirebaseFirestore.getInstance().collection("products")
            .get()
            .addOnSuccessListener(categorySnapshots -> {
                // For each cart item, find and update its stock
                for (CartItem cartItem : cartItems) {
                    final String productName = cartItem.getProductName();
                    final int quantityOrdered = cartItem.getQuantity();
                    
                    // Check each category (hardware or software)
                    for (DocumentSnapshot categoryDoc : categorySnapshots.getDocuments()) {
                        String category = categoryDoc.getId();
                        updateStockInCategory(category, productName, quantityOrdered);
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating stocks: " + e.getMessage());
            });
    }
    
    /**
     * Update stock for a specific product in a category
     */
    private void updateStockInCategory(String category, String productName, int quantityToDecrease) {
        FirebaseFirestore.getInstance().collection("products").document(category)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                // Get the items array
                List<Map<String, Object>> items = (List<Map<String, Object>>) documentSnapshot.get("items");
                
                if (items != null) {
                    boolean updated = false;
                    
                    // Find the item by name and update its quantity
                    for (int i = 0; i < items.size(); i++) {
                        Map<String, Object> item = items.get(i);
                        String name = (String) item.get("name");
                        
                        if (productName.equals(name)) {
                            // Found our product, get current quantity
                            Long currentStock = (Long) item.get("quantity");
                            if (currentStock != null) {
                                // Calculate new quantity
                                long newStock = Math.max(0, currentStock - quantityToDecrease);
                                
                                // Update the quantity in the item
                                item.put("quantity", newStock);
                                items.set(i, item);
                                updated = true;
                                
                                // Log the update
                                Log.d(TAG, "Updating stock for " + productName + " from " + currentStock + " to " + newStock);
                                
                                break;
                            }
                        }
                    }
                    
                    // If we found and updated the item, save changes to Firestore
                    if (updated) {
                        // Update the document with the modified items array
                        FirebaseFirestore.getInstance().collection("products").document(category)
                            .update("items", items)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Stock updated successfully for " + productName);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating stock for " + productName, e);
                            });
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting category document: " + e.getMessage());
           });
    }
    
    private void createTransactionRecord(String paymentMethod, double amount) {
        // Create transaction record
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("userId", userId);
        transaction.put("description", "Cart Checkout");
        transaction.put("items", cartItems.size());
        transaction.put("amount", amount);
        transaction.put("paymentMethod", paymentMethod);
        transaction.put("timestamp", com.google.firebase.Timestamp.now());
        transaction.put("type", "checkout");
        transaction.put("status", "completed");
        
        // Add to transactions collection
        db.collection("Users").document(userId)
            .collection("Transactions")
            .add(transaction)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Transaction record created successfully");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to create transaction record", e);
            });
    }
    
    private void createPurchaseNotification(double amount) {
        // Create notification object
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "Order Confirmation");
        notification.put("message", "Your order of ₱" + String.format("%.2f", amount) + " has been confirmed.");
        notification.put("timestamp", com.google.firebase.Timestamp.now());
        notification.put("isRead", false);
        notification.put("type", "purchase");
        
        // Add to notifications collection
        db.collection("Users").document(userId)
            .collection("Notifications")
            .add(notification)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Notification created successfully");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to create notification", e);
            });
    }

    private void showClearCartConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Clear Cart")
            .setMessage("Are you sure you want to remove all items from your cart?")
            .setPositiveButton("Yes", (dialog, which) -> clearCart())
            .setNegativeButton("No", null)
            .show();
    }

    private void clearCart() {
        db.collection("Users").document(userId)
            .collection("Cart")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    document.getReference().delete();
                }
                
                cartItems.clear();
                cartAdapter.notifyDataSetChanged();
                showEmptyState();
                //Snackbar.make(findViewById(R.id.main), "Cart cleared", Snackbar.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> 
                Snackbar.make(findViewById(R.id.main), "Failed to clear cart: " + e.getMessage(), Snackbar.LENGTH_LONG).show()
            );
    }

    // CartAdapter.CartItemListener implementation
    @Override
    public void onQuantityChanged(int position, int newQuantity) {
        updateOrderSummary();
    }

    @Override
    public void onItemDeleted(int position) {
        if (cartItems.isEmpty()) {
            showEmptyState();
        }
    }

    @Override
    public void onCartUpdated() {
        updateOrderSummary();
    }

    // New helper method to get a String value or empty string if null
    private String getStringOrEmpty(Object value) {
        return (value != null) ? String.valueOf(value) : "";
    }
    
    /**
     * Fallback method to fetch address from user document if no address in subcollection
     */
    private void fetchAddressFromUserDoc(Map<String, Object> order, String paymentMethod, double amount) {
        // Fetch user details to get shipping address information
        db.collection("Users").document(userId)
            .get()
            .addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    // Create shipping address map
                    Map<String, Object> shippingAddress = new HashMap<>();
                    
                    // Try to get address fields from the user document
                    String streetAddress = userDoc.getString("streetAddress");
                    String city = userDoc.getString("city");
                    String province = userDoc.getString("province");
                    String postalCode = userDoc.getString("postalCode");
                    String country = userDoc.getString("country");
                    
                    // Add valid fields to the shipping address map
                    if (streetAddress != null && !streetAddress.trim().isEmpty()) {
                        shippingAddress.put("streetAddress", streetAddress);
                    }
                    if (city != null && !city.trim().isEmpty()) {
                        shippingAddress.put("city", city);
                    }
                    if (province != null && !province.trim().isEmpty()) {
                        shippingAddress.put("province", province);
                    }
                    if (postalCode != null && !postalCode.trim().isEmpty()) {
                        shippingAddress.put("postalCode", postalCode);
                    }
                    if (country != null && !country.trim().isEmpty()) {
                        shippingAddress.put("country", country);
                    }
                    
                    // Add shipping address to order if we have address data
                    if (!shippingAddress.isEmpty()) {
                        order.put("shippingAddress", shippingAddress);
                        Log.d(TAG, "Using shipping address from user document");
                    }
                }
                
                // Continue with creating the order
                createOrder(order, paymentMethod, amount);
            })
            .addOnFailureListener(e -> {
                // Continue with creating the order even if getting address fails
                Log.e(TAG, "Error fetching user details for shipping address: " + e.getMessage());
                createOrder(order, paymentMethod, amount);
            });
    }
}