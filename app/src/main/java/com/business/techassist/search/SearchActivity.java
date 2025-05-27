package com.business.techassist.search;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.business.techassist.R;
import com.business.techassist.shopitems.DatabaseHelper;
import com.business.techassist.shopitems.Product;
import com.business.techassist.shopitems.ProductAdapter;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {

    private SearchView searchView;
    private RecyclerView resultsRecyclerView;
    private ProgressBar progressBar;
    private TextView noResultsTextView;
    private FirebaseFirestore db;
    private DatabaseHelper databaseHelper;
    private List<Product> searchResults = new ArrayList<>();
    private ProductAdapter adapter;
    private android.os.Handler handler = new android.os.Handler();
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_search);

        searchView = findViewById(R.id.searchView);
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView);
        progressBar = findViewById(R.id.searchProgressBar);
        noResultsTextView = findViewById(R.id.noResultsTextView); // make sure this exists in your layout XML

        db = FirebaseFirestore.getInstance();
        databaseHelper = new DatabaseHelper(this);

        adapter = new ProductAdapter(this, searchResults, product -> {
            Intent intent = new Intent(SearchActivity.this, com.business.techassist.shopitems.ProductDetails.class);
            intent.putExtra("productName", product.getName());
            intent.putExtra("productPrice", product.getPrice());
            intent.putExtra("productQuantity", product.getQuantity());
            intent.putExtra("descriptionTxt", product.getDescription());
            startActivity(intent);
        });

        resultsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        resultsRecyclerView.setAdapter(adapter);

        searchView.setIconified(false);
        searchView.requestFocus();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchProducts(query.trim());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                handler.removeCallbacks(searchRunnable);
                searchRunnable = () -> searchProducts(newText.trim());
                handler.postDelayed(searchRunnable, 300); // 300ms delay
                return true;
            }
        });
    }

    private void searchProducts(String keyword) {
        if (TextUtils.isEmpty(keyword)) return;
        progressBar.setVisibility(View.VISIBLE);
        noResultsTextView.setVisibility(View.GONE);
        searchResults.clear();

        db.collection("products").document("hardware").get()
                .addOnSuccessListener(hardwareSnapshot -> {
                    if (hardwareSnapshot.exists()) {
                        extractMatchingProducts(hardwareSnapshot.getData(), keyword);
                    }
                    db.collection("products").document("software").get()
                            .addOnSuccessListener(softwareSnapshot -> {
                                if (softwareSnapshot.exists()) {
                                    extractMatchingProducts(softwareSnapshot.getData(), keyword);
                                }

                                adapter.notifyDataSetChanged();
                                progressBar.setVisibility(View.GONE);

                                if (searchResults.isEmpty()) {
                                    noResultsTextView.setVisibility(View.VISIBLE);
                                } else {
                                    noResultsTextView.setVisibility(View.GONE);
                                }
                            });
                });
    }

    private void extractMatchingProducts(Map<String, Object> data, String keyword) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) entry.getValue();
            for (Map<String, Object> item : items) {
                String name = (String) item.get("name");
                if (name != null && name.toLowerCase().contains(keyword.toLowerCase())) {
                    searchResults.add(mapToProduct(item));
                }
            }
        }
    }

    private Product mapToProduct(Map<String, Object> item) {
        String name = (String) item.get("name");
        int quantity = ((Number) item.get("quantity")).intValue();
        double price = ((Number) item.get("price")).doubleValue();
        String description = (String) item.get("description");

        Bitmap imageBitmap = getProductImageBitmap(name);
        return new Product(name, quantity, price, description, imageBitmap);
    }

    private Bitmap getProductImageBitmap(String productName) {
        Cursor cursor = databaseHelper.getProductByName(productName);
        Bitmap bitmap = null;
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex("image");
            if (columnIndex != -1) {
                byte[] imageBytes = cursor.getBlob(columnIndex);
                if (imageBytes != null && imageBytes.length > 0) {
                    bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                }
            }
            cursor.close();
        }
        return bitmap;
    }
}
