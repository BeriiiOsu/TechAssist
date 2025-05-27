package com.business.techassist.shopitems;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.business.techassist.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    public List<Product> productList;
    private OnItemClickListener listener;
    private DatabaseHelper dbHelper;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public ProductAdapter(Context context, List<Product> productList, OnItemClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
        dbHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        
        // Set product name
        holder.nameText.setText(product.getName());
        
        // Set price with peso sign
        holder.priceText.setText(String.format("₱%.2f", product.getPrice()));
        
        // Set quantity/stock
        int quantity = product.getQuantity();
        holder.quantityText.setText(String.valueOf(quantity));
        
        // Handle stock indicator and out-of-stock visual elements
        if (quantity > 0) {
            // In stock
            holder.stockIndicator.setText("In Stock");
            holder.stockIndicator.setBackgroundResource(R.drawable.pill_bg);
            
            // Make sure out-of-stock elements are hidden
            if (holder.outOfStockOverlay != null) {
                holder.outOfStockOverlay.setVisibility(View.GONE);
            }
            if (holder.outOfStockBanner != null) {
                holder.outOfStockBanner.setVisibility(View.GONE);
            }
        } else {
            // Out of stock
            holder.stockIndicator.setText("Out of Stock");
            holder.stockIndicator.setBackgroundResource(R.color.gray);
            
            // Show out-of-stock elements
            if (holder.outOfStockOverlay != null) {
                holder.outOfStockOverlay.setVisibility(View.VISIBLE);
            }
            if (holder.outOfStockBanner != null) {
                holder.outOfStockBanner.setVisibility(View.VISIBLE);
            }
        }

        // Load product image if available
        byte[] imageBytes = dbHelper.getProductImage(product.getName());
        if (imageBytes != null && imageBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            holder.productPicture.setImageBitmap(bitmap);
            holder.productPicture.setVisibility(View.VISIBLE);
        } else {
            // Use a placeholder if no image is available
            holder.productPicture.setImageResource(R.drawable.store_icon);
            holder.productPicture.setVisibility(View.VISIBLE);
        }

        // Set click listener for the whole item
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, ProductDetails.class);
            intent.putExtra("productName", product.getName());
            intent.putExtra("productPrice", product.getPrice());
            intent.putExtra("productQuantity", product.getQuantity());
            intent.putExtra("descriptionTxt", product.getDescription());
            context.startActivity(intent);
        });
    }

    @SuppressLint("SetTextI18n")
    private void addToCart(Product product, int position, View view) {
        // Get the current user's ID
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        
        if (userId == null) {
            Snackbar.make(view, "Please log in to add items to cart", Snackbar.LENGTH_LONG).show();
            return;
        }
        
        // Generate a unique ID for the cart item
        String cartItemId = UUID.randomUUID().toString();
        
        // Create a map of the product details
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("id", cartItemId);
        cartItem.put("productId", product.getName());
        cartItem.put("productName", product.getName());
        cartItem.put("price", product.getPrice());
        cartItem.put("quantity", 1);
        cartItem.put("timestamp", com.google.firebase.Timestamp.now());
        
        // Add the item to the user's cart in Firestore
        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(userId)
            .collection("Cart")
            .document(cartItemId)
            .set(cartItem)
            .addOnSuccessListener(aVoid -> {
                // Update UI to show success
                Snackbar.make(view, product.getName() + " added to cart", Snackbar.LENGTH_SHORT).show();
                notifyItemChanged(position);
            })
            .addOnFailureListener(e -> {
                Snackbar.make(view, "Failed to add to cart: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void updateList(List<Product> newList) {
        productList.clear();
        productList.addAll(newList);
        notifyDataSetChanged();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, quantityText, priceText, stockIndicator, outOfStockBanner;
        ShapeableImageView productPicture;
        View outOfStockOverlay;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
            quantityText = itemView.findViewById(R.id.quantityText);
            priceText = itemView.findViewById(R.id.priceText);
            productPicture = itemView.findViewById(R.id.productPicture);
            stockIndicator = itemView.findViewById(R.id.stockIndicator);
            outOfStockOverlay = itemView.findViewById(R.id.outOfStockOverlay);
            outOfStockBanner = itemView.findViewById(R.id.outOfStockBanner);
        }
    }
}
