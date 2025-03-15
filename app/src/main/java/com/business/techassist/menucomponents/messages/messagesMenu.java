package com.business.techassist.menucomponents.messages;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.utilities.FirebaseUtil;
import com.business.techassist.R;
import com.business.techassist.models.AdminModel;
import com.business.techassist.adapters.SearchUserRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

public class messagesMenu extends AppCompatActivity {
    private SearchView messagesSearchView;
    private RecyclerView messagesRecycler;
    private SearchUserRecyclerAdapter adapter;
    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private String currentUserRole;
    ImageView backSearchBtn;

    private void updateSearchResultsDebounced(String newText) {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        searchRunnable = () -> updateSearchResults(newText);
        searchHandler.postDelayed(searchRunnable, 300);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.menu_message_search);

        backSearchBtn = findViewById(R.id.backSearchBtn);
        backSearchBtn.setOnClickListener(v -> finish());


        setupUI();

        FirebaseUtil.getCurrentUserRole(role -> {
            currentUserRole = role;
            setupRecyclerView();
            setupSearchView();
        });
    }

    private void setupUI() {
        messagesSearchView = findViewById(R.id.messagesSearchView);
        messagesRecycler = findViewById(R.id.messagesRecycler);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupRecyclerView() {
        messagesRecycler.setLayoutManager(new LinearLayoutManager(this));

        FirestoreRecyclerOptions<AdminModel> options = new FirestoreRecyclerOptions.Builder<AdminModel>()
                .setQuery(getUserQuery(""), AdminModel.class)
                .build();

        adapter = new SearchUserRecyclerAdapter(options, this);
        messagesRecycler.setAdapter(adapter);
        adapter.startListening();  // Ensure it starts listening immediately
    }


    private Query getUserQuery(String searchText) {
        Query query;

        if (searchText.isEmpty()) {
            // Initial load: fetch based on role
            if ("Admin".equals(currentUserRole)) {
                query = FirebaseUtil.allUser()
                        .whereIn("Role", Arrays.asList("Admin", "User"))
                        .orderBy("Name", Query.Direction.ASCENDING);
            } else {
                query = FirebaseUtil.allUser()
                        .whereEqualTo("Role", "Admin")
                        .orderBy("Name", Query.Direction.ASCENDING);
            }
        } else {
            // Search filter: fetch based on name
            if ("Admin".equals(currentUserRole)) {
                query = FirebaseUtil.allUser()
                        .whereIn("Role", Arrays.asList("Admin", "User")) // Ensure role filtering
                        .orderBy("Name")
                        .startAt(searchText)
                        .endAt(searchText + "\uf8ff");
            } else {
                query = FirebaseUtil.allUser()
                        .whereEqualTo("Role", "Admin")
                        .orderBy("Name")
                        .startAt(searchText)
                        .endAt(searchText + "\uf8ff");
            }
        }

        return query;
    }




    private void setupSearchView() {
        messagesSearchView.setIconified(false);
        messagesSearchView.requestFocus();
        messagesSearchView.setSubmitButtonEnabled(true);

        messagesSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                updateSearchResultsDebounced(newText);
                return true;
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateSearchResults(String newText) {
        Query query = getUserQuery(newText);

        FirestoreRecyclerOptions<AdminModel> options = new FirestoreRecyclerOptions.Builder<AdminModel>()
                .setQuery(query, AdminModel.class)
                .build();

        adapter.updateOptions(options);
        adapter.startListening();

        messagesRecycler.getRecycledViewPool().clear();
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            adapter.startListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) adapter.stopListening();
    }
}
