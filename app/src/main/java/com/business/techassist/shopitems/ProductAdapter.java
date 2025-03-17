package com.business.techassist.shopitems;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.business.techassist.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

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
        //holder.bind(productList.get(position), listener);
        Product product = productList.get(position);
        holder.nameText.setText(product.getName());
        holder.quantityText.setText(String.valueOf(product.getQuantity()));
        holder.priceText.setText(String.format("₱%.2f", product.getPrice()));

        FirebaseFirestore.getInstance().collection("products")
                .document(product.getName()) // Assuming product name is the document ID
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        double price = documentSnapshot.getDouble("price");
                        long quantity = documentSnapshot.getLong("quantity");
                        String description = documentSnapshot.getString("description");

                        holder.priceText.setText("₱" + price);
                        holder.quantityText.setText(String.valueOf(quantity));

                        // Save data to pass when clicked
                        product.setPrice(price);
                        product.setQuantity((int) quantity);
                        product.setDescription(description);
                    }
                });

        byte[] imageBytes = dbHelper.getProductImage(product.getName());
        if (imageBytes != null && imageBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            holder.productPicture.setImageBitmap(bitmap);
            holder.productPicture.setVisibility(View.VISIBLE);
        } else {
            holder.productPicture.setVisibility(View.GONE);
        }

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
        TextView nameText, quantityText, priceText;
        ShapeableImageView productPicture;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
            quantityText = itemView.findViewById(R.id.quantityText);
            priceText = itemView.findViewById(R.id.priceText);
            productPicture = itemView.findViewById(R.id.productPicture);
        }

//        public void bind(final Product product, final OnItemClickListener listener) {
//            nameText.setText(product.getName());
//            quantityText.setText(String.valueOf(product.getQuantity()));
//            priceText.setText(String.format("₱%.2f", product.getPrice()));
//
//            if (product.getImage() != null && !product.getImage().isEmpty()) {
//                Glide.with(productPicture.getContext())
//                        .load(product.getImage())  // Load image URL
//                        .into(productPicture);
//            }
//
//            itemView.setOnClickListener(v -> {
//                listener.onItemClick(product);
//                Context context = v.getContext();
//                Intent intent = new Intent(context, ProductDetails.class);
//                intent.putExtra("productName", product.getName());
//                intent.putExtra("productPrice", product.getPrice());
//                intent.putExtra("descriptionTxt", product.getDescription());
//
//                if (product.getImage() != null) {
//                    intent.putExtra("productImageURL", product.getImage());
//                }
//
//                context.startActivity(intent);
//            });
//        }
    }

}
