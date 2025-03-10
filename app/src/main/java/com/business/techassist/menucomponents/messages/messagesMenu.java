    package com.business.techassist.menucomponents.messages;

    import android.annotation.SuppressLint;
    import android.os.Bundle;
    import android.os.Handler;
    import android.util.Log;
    import android.widget.ImageView;
    import android.widget.Toast;

    import androidx.activity.EdgeToEdge;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.appcompat.widget.SearchView;
    import androidx.core.graphics.Insets;
    import androidx.core.view.ViewCompat;
    import androidx.core.view.WindowInsetsCompat;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.business.techassist.Database.FirebaseUtil;
    import com.business.techassist.R;
    import com.business.techassist.UserCredentials.AdminModel;
    import com.business.techassist.adapters.SearchUserRecyclerAdapter;
    import com.firebase.ui.firestore.FirestoreRecyclerOptions;
    import com.google.firebase.firestore.Query;

    public class messagesMenu extends AppCompatActivity {
        private SearchView messagesSearchView;
        private RecyclerView messagesRecycler;
        private SearchUserRecyclerAdapter adapter;
        private final Handler searchHandler = new Handler();
        private Runnable searchRunnable;
        ImageView backSearchBtn;

        private void updateSearchResultsDebounced(String newText) {
            if (searchRunnable != null) {
                searchHandler.removeCallbacks(searchRunnable);
            }

            searchRunnable = () -> updateSearchResults(newText);
            searchHandler.postDelayed(searchRunnable, 300);  // 300ms debounce
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.menu_message_search);


            backSearchBtn = findViewById(R.id.backSearchBtn);

            backSearchBtn.setOnClickListener(v -> finish());

            setupUI();
            setupRecyclerView();
            setupSearchView();
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
            Query query = FirebaseUtil.allUser().whereEqualTo("Role", "Admin");

            FirestoreRecyclerOptions<AdminModel> options = new FirestoreRecyclerOptions.Builder<AdminModel>()
                    .setQuery(query, AdminModel.class)
                    .build();

            adapter = new SearchUserRecyclerAdapter(options, this);
            adapter.setHasStableIds(false);
            messagesRecycler.setItemAnimator(null);
            messagesRecycler.setAdapter(adapter);
        }


        private void setupSearchView() {
            messagesSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    updateSearchResultsDebounced(newText);  // ✅ Use debounced method
                    return true;
                }
            });
        }

        @SuppressLint("NotifyDataSetChanged")
        private void updateSearchResults(String newText) {
            Query query;

            if (newText.isEmpty()) {
                query = FirebaseUtil.allUser().whereEqualTo("Role", "Admin");
            } else {
                query = FirebaseUtil.allUser()
                        .orderBy("Name")
                        .startAt(newText)
                        .endAt(newText + "\uf8ff");
            }

            FirestoreRecyclerOptions<AdminModel> options = new FirestoreRecyclerOptions.Builder<AdminModel>()
                    .setQuery(query, AdminModel.class)
                    .build();

            // Update adapter options to refresh the data
            adapter.updateOptions(options);

            // Clear the RecyclerView's pool and refresh
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
