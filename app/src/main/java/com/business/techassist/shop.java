package com.business.techassist;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.business.techassist.shopitems.DatabaseHelper;
import com.business.techassist.shopitems.Product;
import com.business.techassist.shopitems.ProductAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class shop extends Fragment {

    private SearchView searchShop;
    private TextView allBtn, softwareBtn, hardwareBtn;
    private RecyclerView popularView, shopView, searchResultsView;
    private ProgressBar progressBarPopular, progressBarShop;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private FirebaseFirestore db;
    private DatabaseHelper databaseHelper;

    public shop() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        databaseHelper = new DatabaseHelper(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop, container, false);

        initializeViews(view);
        setupRecyclerViews();
        setupButtons();
        setupSearchView();

        fetchProducts("software", popularView, progressBarPopular);
        fetchAllProducts(shopView, progressBarShop);

        return view;
    }

    private void initializeViews(View view) {
        searchShop = view.findViewById(R.id.searchShop);
        allBtn = view.findViewById(R.id.allBtn);
        softwareBtn = view.findViewById(R.id.softwareBtn);
        hardwareBtn = view.findViewById(R.id.hardwareBtn);
        popularView = view.findViewById(R.id.popularView);
        shopView = view.findViewById(R.id.shopView);
        progressBarPopular = view.findViewById(R.id.progressBarPopular);
        progressBarShop = view.findViewById(R.id.progressBarShop);
        searchResultsView = view.findViewById(R.id.searchResultsView);
    }

    private void setupRecyclerViews() {
        popularView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        shopView.setLayoutManager(layoutManager);
        productAdapter = new ProductAdapter(getContext(), productList, product -> {});
        shopView.setAdapter(productAdapter);
    }

    private void setupButtons() {
        allBtn.setOnClickListener(v -> {
            updateButtonStyles(allBtn);
            fetchAllProducts(shopView, progressBarShop);
        });

        softwareBtn.setOnClickListener(v -> {
            updateButtonStyles(softwareBtn);
            fetchProducts("software", shopView, progressBarShop);
        });

        hardwareBtn.setOnClickListener(v -> {
            updateButtonStyles(hardwareBtn);
            fetchProducts("hardware", shopView, progressBarShop);
        });

        updateButtonStyles(allBtn);
    }

    private void fetchProducts(String category, RecyclerView recyclerView, ProgressBar progressBar) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("products").document(category).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<Product> productList = new ArrayList<>();
                        for (Map.Entry<String, Object> entry : documentSnapshot.getData().entrySet()) {
                            List<Map<String, Object>> items = (List<Map<String, Object>>) entry.getValue();
                            for (Map<String, Object> item : items) {
                                productList.add(mapToProduct(item));
                            }
                        }
                        updateRecyclerView(recyclerView, productList);
                    } else {
                        Log.e("Firestore", "No data found in category: " + category);
                    }
                    progressBar.setVisibility(View.GONE);
                }).addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching products", e);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void fetchAllProducts(RecyclerView recyclerView, ProgressBar progressBar) {
        progressBar.setVisibility(View.VISIBLE);
        List<Product> allProducts = new ArrayList<>();

        db.collection("products").document("hardware").get()
                .addOnSuccessListener(hardwareSnapshot -> {
                    if (hardwareSnapshot.exists()) {
                        extractProducts(hardwareSnapshot.getData(), allProducts);
                    }

                    db.collection("products").document("software").get()
                            .addOnSuccessListener(softwareSnapshot -> {
                                if (softwareSnapshot.exists()) {
                                    extractProducts(softwareSnapshot.getData(), allProducts);
                                }
                                updateRecyclerView(recyclerView, allProducts);
                                progressBar.setVisibility(View.GONE);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Error fetching software products", e);
                                progressBar.setVisibility(View.GONE);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching hardware products", e);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void extractProducts(Map<String, Object> data, List<Product> productList) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) entry.getValue();
            for (Map<String, Object> item : items) {
                productList.add(mapToProduct(item));
            }
        }
    }

    private Product mapToProduct(Map<String, Object> item) {
        String name = (String) item.get("name");
        int quantity = ((Number) item.get("quantity")).intValue();
        double price = ((Number) item.get("price")).doubleValue();
        String description = (String) item.get("description");

        String imagePath = getProductImagePath(name);
        Bitmap imageBitmap = null;
        if (imagePath != null) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                imageBitmap = BitmapFactory.decodeFile(imagePath);
            } else {
                Log.e("ImageLoad", "Image file does not exist: " + imagePath);
            }
        } else {
            Log.e("ImageLoad", "Image path is null for product: " + name);
        }

        return new Product(name, quantity, price, description, imageBitmap);
    }


    private String getProductImagePath(String productName) {
        Cursor cursor = databaseHelper.getProductByName(productName);
        String imagePath = null;
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex("image_path");
            if (columnIndex != -1) {
                imagePath = cursor.getString(columnIndex);
            }
            cursor.close();
        }
        return imagePath;
    }

    private void updateRecyclerView(RecyclerView recyclerView, List<Product> products) {
        ProductAdapter adapter = new ProductAdapter(getContext(), products, product -> {});
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void setupSearchView() {
        searchResultsView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultsView.setAdapter(new ProductAdapter(getContext(), new ArrayList<>(), product -> {}));
        searchResultsView.setVisibility(View.GONE);

        searchShop.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchResultsView.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                List<Product> filteredList = new ArrayList<>();
                for (Product product : productList) {
                    if (product.getName().toLowerCase().contains(newText.toLowerCase())) {
                        filteredList.add(product);
                    }
                }
                updateRecyclerView(searchResultsView, filteredList);
                searchResultsView.setVisibility(filteredList.isEmpty() ? View.GONE : View.VISIBLE);
                return true;
            }
        });
    }

    private void updateButtonStyles(TextView selectedButton) {
        allBtn.setSelected(false);
        softwareBtn.setSelected(false);
        hardwareBtn.setSelected(false);

        allBtn.setTextColor(getResources().getColor(R.color.black));
        softwareBtn.setTextColor(getResources().getColor(R.color.black));
        hardwareBtn.setTextColor(getResources().getColor(R.color.black));

        selectedButton.setSelected(true);
        selectedButton.setTextColor(getResources().getColor(R.color.white));
    }
}
