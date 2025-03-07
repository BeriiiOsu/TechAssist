package com.business.techassist;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        if(savedInstanceState == null){
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, new home()).commit();
        }

        bottomNavigationView.setOnItemSelectedListener(this::bottomNav);


    }

    private boolean bottomNav(MenuItem item){
        Fragment selectedFragment = null;

        if(item.getItemId() == R.id.homeBtn){
            selectedFragment = new home();
        } else if(item.getItemId() == R.id.shopBtn) {
            selectedFragment = new shop();
        }else if(item.getItemId() == R.id.menuBtn){
            selectedFragment = new menu();
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, selectedFragment)
                    .commit();
        }
        return true;
    }
}