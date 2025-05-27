package com.business.techassist.shopitems;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.DocumentSnapshot;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.business.techassist.R;
import com.business.techassist.UserCredentials.UserAddressPaymentManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProductDetails extends AppCompatActivity {
    private TextView productName, priceTxt, descriptionTxt, stockDetails, quantityText;
    private ImageView detailsPicProduct;
    private DatabaseHelper dbHelper;
    private FloatingActionButton backProductBtn;
    private ImageButton increaseBtn, decreaseBtn;
    private MaterialButton addToCartBtn, buyNowBtn;
    private int currentQuantity = 1;
    private int maxStock = 0;
    private String productNameStr;
    private double productPrice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        initializeViews();
        setupIntentData();
        setupClickListeners();
    }
    
    private void initializeViews() {
        productName = findViewById(R.id.productName);
        priceTxt = findViewById(R.id.priceTxt);
        descriptionTxt = findViewById(R.id.descriptionTxt);
        backProductBtn = findViewById(R.id.backProductBtn);
        detailsPicProduct = findViewById(R.id.detailsPicProduct);
        stockDetails = findViewById(R.id.stockDetails);
        
        // Quantity selector views
        quantityText = findViewById(R.id.quantityText);
        increaseBtn = findViewById(R.id.increaseBtn);
        decreaseBtn = findViewById(R.id.decreaseBtn);
        addToCartBtn = findViewById(R.id.addToCartBtn);
        buyNowBtn = findViewById(R.id.buyNowBtn);
        
        dbHelper = new DatabaseHelper(this);
    }
    
    private void setupIntentData() {
        Intent intent = getIntent();

        if (intent != null) {
            productNameStr = intent.getStringExtra("productName");
            productPrice = intent.getDoubleExtra("productPrice", 0);
            maxStock = intent.getIntExtra("productQuantity", 0);
            String description = intent.getStringExtra("descriptionTxt");

            if (productNameStr != null) productName.setText(productNameStr);
            priceTxt.setText(String.format("₱%.2f", productPrice));
            if (description != null) descriptionTxt.setText(description);
            
            // Update stock display
            String stockStatus = maxStock > 0 ? "In Stock" : "Out of Stock";
            stockDetails.setText(stockStatus + ": " + maxStock);

            byte[] imageBytes = dbHelper.getProductImage(productNameStr);
            if (imageBytes != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                detailsPicProduct.setImageBitmap(bitmap);
            }
            
            // Initialize quantity (disabled if out of stock)
            updateQuantityDisplay();
            if (maxStock <= 0) {
                disableQuantityControls();
            }
        }
    }
    
    private void setupClickListeners() {
        backProductBtn.setOnClickListener(v -> finish());
        
        // Quantity increase button
        increaseBtn.setOnClickListener(v -> {
            if (currentQuantity < maxStock) {
                currentQuantity++;
                updateQuantityDisplay();
            } else {
                Toast.makeText(this, "Maximum stock reached", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Quantity decrease button
        decreaseBtn.setOnClickListener(v -> {
            if (currentQuantity > 1) {
                currentQuantity--;
                updateQuantityDisplay();
            }
        });
        
        // Add to cart button
        addToCartBtn.setOnClickListener(v -> {
            if (maxStock > 0) {
                addToCart();
            } else {
                Toast.makeText(this, "Sorry, this item is out of stock", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Buy now button
        buyNowBtn.setOnClickListener(v -> {
            if (maxStock > 0) {
                buyNow();
            } else {
                Toast.makeText(this, "Sorry, this item is out of stock", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateQuantityDisplay() {
        quantityText.setText(String.valueOf(currentQuantity));
    }
    
    private void disableQuantityControls() {
        increaseBtn.setEnabled(false);
        decreaseBtn.setEnabled(false);
        addToCartBtn.setEnabled(false);
        buyNowBtn.setEnabled(false);
        addToCartBtn.setText("Out of Stock");
        buyNowBtn.setText("Out of Stock");
        currentQuantity = 0;
        updateQuantityDisplay();
    }
    
    private void addToCart() {
        // Get the current user ID
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Generate cart item ID
        String cartItemId = UUID.randomUUID().toString();
        
        // Create cart item
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("id", cartItemId);
        cartItem.put("productId", productNameStr);
        cartItem.put("productName", productName.getText().toString());
        cartItem.put("price", productPrice);
        cartItem.put("quantity", currentQuantity);
        cartItem.put("timestamp", com.google.firebase.Timestamp.now());
        
        // Add to Firestore
        FirebaseFirestore.getInstance().collection("Users")
            .document(userId)
            .collection("Cart")
            .document(cartItemId)
            .set(cartItem)
            .addOnSuccessListener(aVoid -> {
                Snackbar.make(findViewById(R.id.main), "Added to cart", Snackbar.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Snackbar.make(findViewById(R.id.main), "Failed to add to cart", Snackbar.LENGTH_LONG).show();
            });
    }
    
    private void buyNow() {
        // Show payment dialog
        showPaymentDialog();
    }
    
    private void showPaymentDialog() {
        // Use UserAddressPaymentManager to get payment methods from subcollection
        UserAddressPaymentManager userManager = new UserAddressPaymentManager();
        
        // Create dialog builder early
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        // Inflate the custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_purchase, null);
        builder.setView(dialogView);
        
        // Create dialog early but don't show it yet
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
        RadioButton mayaRadio = dialogView.findViewById(R.id.mayaRadio);
        RadioButton codRadio = dialogView.findViewById(R.id.codRadio);
        MaterialButton confirmButton = dialogView.findViewById(R.id.confirmButton);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelButton);
        
        // Set product details
        productNameView.setText(productNameStr);
        productPriceView.setText(String.format("₱%.2f", productPrice));
        productQuantityView.setText(String.format("Quantity: %d", currentQuantity));
        
        // Set product image if available
        byte[] imageBytes = dbHelper.getProductImage(productNameStr);
        if (imageBytes != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            productImage.setImageBitmap(bitmap);
        } else {
            // Use placeholder if no image
            productImage.setImageResource(R.drawable.store_icon);
        }
        
        // Calculate order summary
        double subtotal = productPrice * currentQuantity;
        double tax = subtotal * 0.12; // 12% tax
        double deliveryFee = 50.0; // Fixed delivery fee
        double total = subtotal + tax + deliveryFee;
        
        // Update summary texts
        subtotalText.setText(String.format("₱%.2f", subtotal));
        taxText.setText(String.format("₱%.2f", tax));
        deliveryText.setText(String.format("₱%.2f", deliveryFee));
        totalText.setText(String.format("₱%.2f", total));
        
        // Configure payment methods
        // Hide COD option
        codRadio.setVisibility(View.GONE);
        
        // Initialize payment method radio buttons as disabled
        creditCardRadio.setEnabled(false);
        paypalRadio.setEnabled(false);
        gcashRadio.setEnabled(false);
        mayaRadio.setEnabled(false);
        creditCardRadio.setAlpha(0.5f);
        paypalRadio.setAlpha(0.5f);
        gcashRadio.setAlpha(0.5f);
        mayaRadio.setAlpha(0.5f);
        
        // First, load all payment methods from subcollection
        userManager.getPaymentMethods(new UserAddressPaymentManager.PaymentMethodsLoadCallback() {
            @Override
            public void onPaymentMethodsLoaded(List<Map<String, Object>> paymentMethods) {
                if (paymentMethods.isEmpty()) {
                    // No payment methods available
                    Toast.makeText(ProductDetails.this, "Please add a payment method in settings", Toast.LENGTH_LONG).show();
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
                            mayaRadio.setEnabled(true);
                            mayaRadio.setAlpha(1.0f);
                            mayaRadio.setText("Maya (Saved)");
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
                boolean finalHasGcash = hasGcash;
                boolean finalHasMaya = hasMaya;
                Map<String, Object> finalDefaultPaymentMethod = defaultPaymentMethod;
                
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
                            Toast.makeText(ProductDetails.this, "Please add a payment method in settings", Toast.LENGTH_LONG).show();
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
                                } else if (defaultType.equalsIgnoreCase("Maya") && finalHasMaya) {
                                    mayaRadio.setChecked(true);
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
                            } else if (finalHasMaya) {
                                mayaRadio.setChecked(true);
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
                            } else if (selectedId == R.id.mayaRadio && finalHasMaya) {
                                paymentMethod = "Maya";
                            } else {
                                // No valid payment method selected
                                Toast.makeText(ProductDetails.this, "Please select a valid payment method", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // Process the direct purchase with the selected payment method
                            processDirectPurchase(paymentMethod);
                            dialog.dismiss();
                        });
                    }
                    
                    @Override
                    public void onFailure(String errorMessage) {
                        // Failed to load address, continue with payment methods
                        Toast.makeText(ProductDetails.this, "Failed to load address: " + errorMessage, Toast.LENGTH_SHORT).show();
                        
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
                                } else if (defaultType.equalsIgnoreCase("Maya") && finalHasMaya) {
                                    mayaRadio.setChecked(true);
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
                            } else if (finalHasMaya) {
                                mayaRadio.setChecked(true);
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
                            } else if (selectedId == R.id.mayaRadio && finalHasMaya) {
                                paymentMethod = "Maya";
                            } else {
                                // No valid payment method selected
                                Toast.makeText(ProductDetails.this, "Please select a valid payment method", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // Process the direct purchase with the selected payment method
                            processDirectPurchase(paymentMethod);
                            dialog.dismiss();
                        });
                    }
                });
            }
            
            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(ProductDetails.this, "Failed to load payment methods: " + errorMessage, Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
    }
    
    // Helper method to handle null strings
    private String getStringOrEmpty(Object value) {
        return (value != null) ? String.valueOf(value) : "";
    }
    
    private void processDirectPurchase(String paymentMethod) {
        // Get current user ID
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Save the selected payment method for future use
        saveSelectedPaymentMethod(paymentMethod);
        
        // Calculate the total amount
        double subtotal = productPrice * currentQuantity;
        double tax = subtotal * 0.12; // 12% tax
        double deliveryFee = 50.0; // Fixed delivery fee
        double totalAmount = subtotal + tax + deliveryFee;
        
        // Create an order in Firestore
        Map<String, Object> order = new HashMap<>();
        order.put("userId", userId);
        order.put("items", 1); // Single item in direct purchase
        order.put("amount", totalAmount);
        order.put("paymentMethod", paymentMethod);
        order.put("status", "completed");
        order.put("timestamp", com.google.firebase.Timestamp.now());
        
        // Get shipping address from UserAddressPaymentManager
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
                    if (!label.isEmpty() && !label.equals("N/A")) {
                        shippingAddress.put("label", label);
                    }
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
                    
                    // Add shipping address to order if we have address data
                    if (!shippingAddress.isEmpty()) {
                        order.put("shippingAddress", shippingAddress);
                        
                        // Log the shipping address
                        Log.d("ProductDetails", "Using default shipping address: " + streetAddress + ", " + city);
                    }
                    
                    // Continue with creating the order
                    createOrder(order, userId, paymentMethod, totalAmount);
                } else {
                    // No address available, fallback to user document
                    fetchAddressFromUserDoc(order, userId, paymentMethod, totalAmount);
                }
            }
            
            @Override
            public void onFailure(String errorMessage) {
                // Error fetching address from subcollection, fallback to user document
                Log.e("ProductDetails", "Error fetching address from subcollection: " + errorMessage);
                fetchAddressFromUserDoc(order, userId, paymentMethod, totalAmount);
            }
        });
    }
    
    /**
     * Fallback method to fetch address from user document if no address in subcollection
     */
    private void fetchAddressFromUserDoc(Map<String, Object> order, String userId, String paymentMethod, double totalAmount) {
        // Fetch user details to get shipping address information
        FirebaseFirestore.getInstance().collection("Users").document(userId)
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
                        Log.d("ProductDetails", "Using shipping address from user document");
                    }
                }
                
                // Continue with creating the order
                createOrder(order, userId, paymentMethod, totalAmount);
            })
            .addOnFailureListener(e -> {
                // Continue with creating the order even if getting address fails
                Log.e("ProductDetails", "Error fetching user details for shipping address: " + e.getMessage());
                createOrder(order, userId, paymentMethod, totalAmount);
            });
    }
    
    /**
     * Create the order in Firestore
     */
    private void createOrder(Map<String, Object> order, String userId, String paymentMethod, double totalAmount) {
        FirebaseFirestore.getInstance().collection("Users").document(userId)
           .collection("Orders")
           .add(order)
           .addOnSuccessListener(documentReference -> {
               String orderId = documentReference.getId();
               
               // Add the product to the order
               Map<String, Object> orderItem = new HashMap<>();
               orderItem.put("productName", productNameStr);
               orderItem.put("price", productPrice);
               orderItem.put("quantity", currentQuantity);
               
               FirebaseFirestore.getInstance().collection("Users").document(userId)
                   .collection("Orders").document(orderId)
                   .collection("Items")
                   .add(orderItem);
               
               // Update stock quantity
               updateProductStock();
               
               // Complete the transaction
               completeTransaction(paymentMethod, totalAmount);
           })
           .addOnFailureListener(e -> {
               Snackbar.make(findViewById(R.id.main), "Failed to place order: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
           });
    }
    
    /**
     * Save the selected payment method to Firestore
     */
    private void saveSelectedPaymentMethod(String paymentMethod) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("Users").document(userId)
            .update("PaymentType", paymentMethod)
            .addOnSuccessListener(aVoid -> Log.d("ProductDetails", "Payment method saved: " + paymentMethod))
            .addOnFailureListener(e -> Log.e("ProductDetails", "Error saving payment method", e));
    }
    
    private void completeTransaction(String paymentMethod, double amount) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Create transaction record
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("userId", userId);
        transaction.put("description", "Direct Purchase: " + productNameStr);
        transaction.put("amount", amount);
        transaction.put("quantity", currentQuantity);
        transaction.put("paymentMethod", paymentMethod);
        transaction.put("timestamp", com.google.firebase.Timestamp.now());
        transaction.put("type", "purchase");
        transaction.put("status", "completed");
        
        // Add to transactions collection
        FirebaseFirestore.getInstance().collection("Users")
            .document(userId)
            .collection("Transactions")
            .add(transaction)
            .addOnSuccessListener(documentReference -> {
                // Create notification
                createPurchaseNotification(productNameStr, amount);
                
                // Show success message
                Snackbar.make(findViewById(R.id.main), "Purchase successful!", Snackbar.LENGTH_LONG).show();
                
                // Optionally navigate back or to home
                finish();
            })
            .addOnFailureListener(e -> {
                Snackbar.make(findViewById(R.id.main), "Transaction failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            });
    }
    
    private void createPurchaseNotification(String productName, double amount) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Create notification object
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "Purchase Successful");
        notification.put("message", "You purchased " + productName + " for ₱" + String.format("%.2f", amount));
        notification.put("timestamp", com.google.firebase.Timestamp.now());
        notification.put("isRead", false);
        notification.put("type", "purchase");
        
        // Add to notifications collection
        FirebaseFirestore.getInstance().collection("Users")
            .document(userId)
            .collection("Notifications")
            .add(notification)
            .addOnSuccessListener(documentReference -> {
                Log.d("ProductDetails", "Notification created successfully");
            })
            .addOnFailureListener(e -> {
                Log.e("ProductDetails", "Failed to create notification", e);
            });
    }
    
    /**
     * Update product stock quantity after successful order
     */
    private void updateProductStock() {
        // First, determine product category (hardware or software)
        FirebaseFirestore.getInstance().collection("products")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot categoryDoc : queryDocumentSnapshots.getDocuments()) {
                    String category = categoryDoc.getId(); // 'hardware' or 'software'
                    
                    // Check if this category contains our product
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
                                    
                                    if (productNameStr.equals(name)) {
                                        // Found our product, get current quantity
                                        Long currentStock = (Long) item.get("quantity");
                                        if (currentStock != null) {
                                            // Calculate new quantity
                                            long newStock = Math.max(0, currentStock - currentQuantity);
                                            
                                            // Update the quantity in the item
                                            item.put("quantity", newStock);
                                            items.set(i, item);
                                            updated = true;
                                            
                                            // Update the local UI to reflect the change
                                            maxStock = (int) newStock;
                                            String stockStatus = maxStock > 0 ? "In Stock" : "Out of Stock";
                                            stockDetails.setText(stockStatus + ": " + maxStock);
                                            
                                            if (maxStock <= 0) {
                                                disableQuantityControls();
                                            }
                                            
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
                                            Log.d("ProductDetails", "Stock updated successfully");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("ProductDetails", "Error updating stock", e);
                                        });
                                }
                            }
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e("ProductDetails", "Error finding product categories", e);
            });
    }
}
