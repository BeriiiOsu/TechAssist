package com.business.techassist;

import android.content.Intent;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Rect;

import com.business.techassist.menucomponents.cart.cart;
import com.business.techassist.menucomponents.trackOrderMenu;
import com.business.techassist.search.SearchActivity;
import com.business.techassist.shopitems.DatabaseHelper;
import com.business.techassist.shopitems.Product;
import com.business.techassist.shopitems.ProductAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class shop extends Fragment {

    private View searchBarCard;
    private Chip allBtn, softwareBtn, hardwareBtn;
    private TextView seeAllPopular;
    private MaterialButton shopNowPromo;
    private RecyclerView popularView, shopView, searchResultsView;
    private ProgressBar progressBarPopular, progressBarShop;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private FirebaseFirestore db;
    private DatabaseHelper databaseHelper;
    
    // Add these fields to store the listener registrations
    private ListenerRegistration hardwareListener;
    private ListenerRegistration softwareListener;
    private ListenerRegistration categoryListener;

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
        setupClickListeners();

        fetchProducts("software", popularView, progressBarPopular);
        fetchAllProducts(shopView, progressBarShop);

        return view;
    }

    private void initializeViews(View view) {
        // Initialize search bar
        searchBarCard = view.findViewById(R.id.searchBarCard);
        
        // Initialize category buttons (chips)
        allBtn = view.findViewById(R.id.allBtn);
        softwareBtn = view.findViewById(R.id.softwareBtn);
        hardwareBtn = view.findViewById(R.id.hardwareBtn);
        
        // Initialize recycler views
        popularView = view.findViewById(R.id.popularView);
        shopView = view.findViewById(R.id.shopView);
        searchResultsView = view.findViewById(R.id.searchResultsView);
        
        // Initialize progress bars
        progressBarPopular = view.findViewById(R.id.progressBarPopular);
        progressBarShop = view.findViewById(R.id.progressBarShop);
        
        // Initialize other UI elements
        seeAllPopular = view.findViewById(R.id.seeAllPopular);
        shopNowPromo = view.findViewById(R.id.shopNowPromo);
        
        // Initialize cart and track order buttons
        View cartShopBtn = view.findViewById(R.id.cartShopBtn);
        View trackShopBtn = view.findViewById(R.id.trackShopBtn);
        
        // Set click listeners for cart and track order buttons
        cartShopBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), cart.class);
            startActivity(intent);
        });
        
        trackShopBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), trackOrderMenu.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerViews() {
        // Setup popular products with horizontal scrolling
        popularView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        
        // Add horizontal spacing for popular view
        int horizontalSpacing = getResources().getDimensionPixelSize(R.dimen.padding_small);
        popularView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.right = horizontalSpacing;
                // Add left spacing only for the first item
                if (parent.getChildAdapterPosition(view) == 0) {
                    outRect.left = horizontalSpacing;
                }
            }
        });
        
        // Force exactly 2 columns for the shop grid
        int spanCount = 2;
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
        shopView.setLayoutManager(layoutManager);
        
        // Add item decoration for grid spacing
        int gridSpacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        shopView.addItemDecoration(new GridItemDecoration(gridSpacing, spanCount));
        
        productAdapter = new ProductAdapter(getContext(), productList, product -> {
            // Handle product click
            Toast.makeText(getContext(), "Selected: " + product.getName(), Toast.LENGTH_SHORT).show();
        });
        shopView.setAdapter(productAdapter);
    }

    /**
     * Calculate the appropriate span count based on screen width
     * This ensures the grid looks good on different screen sizes
     */
    private int calculateSpanCount() {
        // Get the display metrics
        android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        
        // Get the spacing and card width
        float spacingDp = getResources().getDimension(R.dimen.grid_spacing) / displayMetrics.density;
        float cardWidthDp = 160; // Updated card width
        
        // Product card width + margin on both sides
        float itemWidthWithSpacing = cardWidthDp + (2 * spacingDp);
        
        // Calculate how many columns can fit
        int spanCount = Math.max(2, (int)((screenWidthDp - 32) / itemWidthWithSpacing));
        
        return spanCount;
    }

    private void setupClickListeners() {
        // Category filter chips
        allBtn.setOnClickListener(v -> {
            toggleChipSelection(allBtn);
            fetchAllProducts(shopView, progressBarShop);
        });

        softwareBtn.setOnClickListener(v -> {
            toggleChipSelection(softwareBtn);
            fetchProducts("software", shopView, progressBarShop);
        });

        hardwareBtn.setOnClickListener(v -> {
            toggleChipSelection(hardwareBtn);
            fetchProducts("hardware", shopView, progressBarShop);
        });

        // Search bar
        searchBarCard.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SearchActivity.class);
            startActivity(intent);
        });
        
        // See all popular products
        seeAllPopular.setOnClickListener(v -> {
            allBtn.performClick();
            Toast.makeText(getContext(), "Showing all products", Toast.LENGTH_SHORT).show();
        });
        
        // Promo button
        shopNowPromo.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Summer Sale: 20% off on all products!", Toast.LENGTH_SHORT).show();
        });

        // Set initial selection
        toggleChipSelection(allBtn);
    }
    
    private void toggleChipSelection(Chip selectedChip) {
        // Reset all chips
        allBtn.setChecked(false);
        softwareBtn.setChecked(false);
        hardwareBtn.setChecked(false);
        
        // Set selected chip
        selectedChip.setChecked(true);
    }

    private void fetchProducts(String category, RecyclerView recyclerView, ProgressBar progressBar) {
        progressBar.setVisibility(View.VISIBLE);
        
        // Clean up any previous listener
        if (categoryListener != null) {
            categoryListener.remove();
        }
        
        categoryListener = db.collection("products").document(category)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Error fetching products", error);
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
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
                });
    }

    private void fetchAllProducts(RecyclerView recyclerView, ProgressBar progressBar) {
        progressBar.setVisibility(View.VISIBLE);
        
        // Use a counter to track when both listeners have fired
        final int[] pendingResponses = {2}; // We expect 2 responses: hardware and software
        final List<Product> allProducts = new ArrayList<>();
        
        // First, clear any existing listeners to avoid duplicates
        if (hardwareListener != null) {
            hardwareListener.remove();
        }
        if (softwareListener != null) {
            softwareListener.remove();
        }

        // Register the new hardware listener
        hardwareListener = db.collection("products").document("hardware")
            .addSnapshotListener((hardwareSnapshot, hardwareError) -> {
                if (hardwareError != null) {
                    Log.e("Firestore", "Error fetching hardware products", hardwareError);
                    pendingResponses[0]--;
                    if (pendingResponses[0] <= 0) {
                        updateRecyclerView(recyclerView, allProducts);
                        progressBar.setVisibility(View.GONE);
                    }
                    return;
                }

                // Clear existing hardware products from the list
                // This requires identifying products by category, which is not in our current model
                // For now, we'll rebuild the full list each time
                
                if (hardwareSnapshot != null && hardwareSnapshot.exists()) {
                    extractProducts(hardwareSnapshot.getData(), allProducts);
                }
                
                pendingResponses[0]--;
                if (pendingResponses[0] <= 0) {
                    updateRecyclerView(recyclerView, allProducts);
                    progressBar.setVisibility(View.GONE);
                }
            });

        // Register the new software listener
        softwareListener = db.collection("products").document("software")
            .addSnapshotListener((softwareSnapshot, softwareError) -> {
                if (softwareError != null) {
                    Log.e("Firestore", "Error fetching software products", softwareError);
                    pendingResponses[0]--;
                    if (pendingResponses[0] <= 0) {
                        updateRecyclerView(recyclerView, allProducts);
                        progressBar.setVisibility(View.GONE);
                    }
                    return;
                }

                if (softwareSnapshot != null && softwareSnapshot.exists()) {
                    extractProducts(softwareSnapshot.getData(), allProducts);
                }
                
                pendingResponses[0]--;
                if (pendingResponses[0] <= 0) {
                    updateRecyclerView(recyclerView, allProducts);
                    progressBar.setVisibility(View.GONE);
                }
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
        // Check if fragment is attached to avoid IllegalStateException
        if (!isAdded() || getContext() == null) {
            Log.d("Shop", "Fragment not attached to context. Ignoring updateRecyclerView call.");
            return;
        }
        
        ProductAdapter adapter = new ProductAdapter(getContext(), products, product -> {
            // Handle product click
            Toast.makeText(getContext(), "Selected: " + product.getName(), Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);
        
        // If updating the shopView, reapply the grid spacing decoration
        if (recyclerView.getId() == R.id.shopView) {
            // Clear any existing item decorations to avoid accumulating them
            while (recyclerView.getItemDecorationCount() > 0) {
                recyclerView.removeItemDecorationAt(0);
            }
            
            // Fixed 2-column grid
            int spanCount = 2;
            GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
            recyclerView.setLayoutManager(layoutManager);
            
            // Reapply decoration with current span count
            int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
            recyclerView.addItemDecoration(new GridItemDecoration(spacingInPixels, spanCount));
        } 
        // For popular view, ensure horizontal spacing is consistent
        else if (recyclerView.getId() == R.id.popularView) {
            // Clear existing decorations
            while (recyclerView.getItemDecorationCount() > 0) {
                recyclerView.removeItemDecorationAt(0);
            }
            
            // Add horizontal spacing
            int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.padding_small);
            recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                    outRect.right = spacingInPixels;
                    // Add left margin only for the first item
                    if (parent.getChildAdapterPosition(view) == 0) {
                        outRect.left = spacingInPixels;
                    }
                }
            });
        }
        
        adapter.notifyDataSetChanged();
    }

    /**
     * A simpler grid item decoration that applies equal spacing around all items
     * Optimized for a 2-column grid
     */
    public class GridItemDecoration extends RecyclerView.ItemDecoration {
        private int spacing;

        public GridItemDecoration(int spacing, int spanCount) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            
            // Apply reduced spacing for wider items
            if (position % 2 == 0) {
                // First column
                outRect.left = spacing / 2;
                outRect.right = spacing / 4;
            } else {
                // Second column
                outRect.left = spacing / 4;
                outRect.right = spacing / 2;
            }
            
            // Top spacing for all items in first row, and bottom spacing for all items
            if (position < 2) {
                outRect.top = spacing / 2;
            }
            outRect.bottom = spacing / 2;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        
        // Remove all snapshot listeners when the fragment is detached
        if (hardwareListener != null) {
            hardwareListener.remove();
            hardwareListener = null;
        }
        if (softwareListener != null) {
            softwareListener.remove();
            softwareListener = null;
        }
        if (categoryListener != null) {
            categoryListener.remove();
            categoryListener = null;
        }
        
        // Clean up other resources if needed
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}
