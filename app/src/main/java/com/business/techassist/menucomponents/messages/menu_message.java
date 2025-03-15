package com.business.techassist.menucomponents.messages;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.utilities.FirebaseUtil;
import com.business.techassist.R;
import com.business.techassist.adapters.RecentChatRecyclerAdapter;
import com.business.techassist.models.ChatroomModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

public class menu_message extends AppCompatActivity {

    ImageView searchBtn;
    RecyclerView recyler_view;
    RecentChatRecyclerAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.menu_message);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        searchBtn = findViewById(R.id.searchBtn);

        searchBtn.setOnClickListener(view -> startActivity(new Intent(menu_message.this, messagesMenu.class)));

        recyler_view = findViewById(R.id.recyler_view);
        setupRecyclerView();
    }
    private void setupRecyclerView() {
        Query query = FirebaseUtil.allChatroomCollectionReference()
                .whereArrayContains("userIDs",FirebaseUtil.currentUserID())
                .orderBy("lastMessageTimestamp",Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatroomModel> options = new FirestoreRecyclerOptions.Builder<ChatroomModel>()
                .setQuery(query,ChatroomModel.class).build();

        adapter = new RecentChatRecyclerAdapter(options, this);
        recyler_view.setLayoutManager(new LinearLayoutManager(this));
        recyler_view.setAdapter(adapter);
        adapter.startListening();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(adapter!=null)
            adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(adapter!=null)
            adapter.stopListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) adapter.stopListening();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(adapter!=null)
            adapter.notifyDataSetChanged();
    }
}