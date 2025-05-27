package com.business.techassist.transactionhistory;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.business.techassist.R;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TransactionHistoryActivity extends AppCompatActivity {

    private static final String TAG = "TransactionHistory";
    
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyState;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        // Initialize views
        recyclerView = findViewById(R.id.transactionRecyclerView);
        emptyState = findViewById(R.id.emptyState);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Transaction History");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
        // Set up Firestore and current user
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        // Set up recycler view with adapter
        setupRecyclerView();
        
        // Setup pull to refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.primary, 
                R.color.secondary,
                R.color.black);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    private void setupRecyclerView() {
        // Check if user is logged in
        if (currentUser == null) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText("Please login to view your transaction history");
            return;
        }
        
        // Set up query for all transactions, sorted by date
        Query query = db.collection("Users")
                .document(currentUser.getUid())
                    .collection("Transactions")
                    .orderBy("timestamp", Query.Direction.DESCENDING);
            
        // Create FirestoreRecyclerOptions
            FirestoreRecyclerOptions<Transaction> options = new FirestoreRecyclerOptions.Builder<Transaction>()
                    .setQuery(query, Transaction.class)
                    .build();
            
        // Create and set adapter
            adapter = new TransactionAdapter(options);
        adapter.setOnTransactionClickListener((documentSnapshot, transaction) -> {
            showTransactionDetails(documentSnapshot, transaction);
        });
            
        // Configure recycler view
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
    
        // Register data observer to check for empty state
            adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
            public void onChanged() {
                super.onChanged();
                checkIfEmpty();
                }
                
                @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkIfEmpty();
                }
                
                @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkIfEmpty();
                }
            });
            
        // Start listening for changes
            adapter.startListening();
    }

    private void showTransactionDetails(DocumentSnapshot documentSnapshot, Transaction transaction) {
        // Create and show a dialog with transaction details
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_transaction_details, null);
        
        // Set up dialog views
        TextView titleView = dialogView.findViewById(R.id.transactionTitle);
        TextView amountView = dialogView.findViewById(R.id.transactionAmount);
        TextView dateView = dialogView.findViewById(R.id.transactionDate);
        TextView statusView = dialogView.findViewById(R.id.transactionStatus);
        TextView typeView = dialogView.findViewById(R.id.transactionType);
        TextView descriptionView = dialogView.findViewById(R.id.transactionDescription);
        TextView idView = dialogView.findViewById(R.id.transactionId);
        
        // Populate views with transaction data
        titleView.setText(transaction.getDescription());
        amountView.setText(String.format("₱%,.2f", transaction.getAmount()));
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        if (transaction.getTimestamp() != null) {
            dateView.setText(sdf.format(transaction.getTimestamp().toDate()));
        } else {
            dateView.setText("N/A");
        }
        
        statusView.setText(transaction.getStatus());
        typeView.setText(transaction.getType());
        descriptionView.setText(transaction.getDescription());
        idView.setText(documentSnapshot.getId());
        
        // Set status color based on status
        int bgColorRes;
        switch (transaction.getStatus().toLowerCase()) {
            case "completed":
                bgColorRes = R.color.success;
                break;
            case "pending":
                bgColorRes = R.color.warning;
                break;
            case "failed":
                bgColorRes = R.color.error;
                break;
            case "cancelled":
                bgColorRes = R.color.textSecondary;
                break;
            default:
                bgColorRes = R.color.gray;
                break;
        }
        statusView.getBackground().setTint(getColor(bgColorRes));
        
        // Show the dialog
        builder.setView(dialogView)
               .setPositiveButton("Close", null)
               .create()
               .show();
    }

    private void checkIfEmpty() {
        if (adapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText("No transactions found");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }
    
    private void refreshData() {
        Log.d(TAG, "Refreshing transactions");
        if (adapter == null) {
            setupRecyclerView();
        } else {
            adapter.stopListening();
            adapter.startListening();
        }
        
        // Make sure to always end the refreshing animation
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}