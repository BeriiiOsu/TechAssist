package com.business.techassist;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.business.techassist.chatbot.ChatAssistFragment;
import com.business.techassist.utilities.FirebaseUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;

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

        applyDefaultTheme();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, new home()).commit();
        }

        bottomNavigationView.setOnItemSelectedListener(this::bottomNav);

        getFCMToken();
    }

    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                FirebaseUtil.currentUserDetails().update("fcmToken", token);
            }
        });
    }

    private boolean bottomNav(MenuItem item) {
        Fragment selectedFragment = null;

        if (item.getItemId() == R.id.homeBtn) {
            selectedFragment = new home();
            applyDefaultTheme();
        } else if (item.getItemId() == R.id.shopBtn) {
            selectedFragment = new shop();
            applyShopTheme();
        } else if (item.getItemId() == R.id.activityBtn) {
            selectedFragment = new activities();
            applyDefaultTheme();
        } else if (item.getItemId() == R.id.chatbotBtn) {
            selectedFragment = new ChatAssistFragment();
            applyGeminiTheme();
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, selectedFragment)
                    .commit();
        }
        return true;
    }

    private void applyDefaultTheme() {
        bottomNavigationView.setBackgroundColor(getResources().getColor(R.color.default_navbar, getTheme()));
        bottomNavigationView.setItemTextColor(getResources().getColorStateList(R.color.default_text_color, getTheme()));
        bottomNavigationView.setItemIconTintList(getResources().getColorStateList(R.color.bottom_nav_icon_color));
    }

    private void applyShopTheme() {
        bottomNavigationView.setBackgroundColor(getResources().getColor(R.color.lightCream, getTheme()));
        bottomNavigationView.setItemTextColor(getResources().getColorStateList(R.color.light_cream_text, getTheme()));
        bottomNavigationView.setItemIconTintList(getResources().getColorStateList(R.color.light_cream_icon, getTheme()));
    }


    private void applyGeminiTheme() {
        bottomNavigationView.setBackgroundColor(getResources().getColor(R.color.gemini_dark_background, getTheme()));
        bottomNavigationView.setItemTextColor(getResources().getColorStateList(R.color.white_text_color, getTheme()));
        bottomNavigationView.setItemIconTintList(getResources().getColorStateList(R.color.bottom_nav_icon_gemini));
    }

}
