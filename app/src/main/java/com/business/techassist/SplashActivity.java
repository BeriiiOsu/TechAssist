package com.business.techassist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.business.techassist.utilities.FirebaseUtil;
import com.business.techassist.UserCredentials.LoginScreen;
import com.business.techassist.menucomponents.messages.messageActivity;
import com.business.techassist.models.AdminModel;
import com.business.techassist.utilities.AndroidUtil;
import com.google.firebase.FirebaseApp;

public class SplashActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        if (FirebaseApp.initializeApp(this) == null) {
            Log.e("FirebaseInit", "Firebase initialization failed!");
        } else {
            Log.d("FirebaseInit", "Firebase initialized successfully!");
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("GlobalException", "Uncaught Exception: ", throwable);
        });

        if(FirebaseUtil.isLoggedIn() && getIntent().getExtras() != null){
            String userID = getIntent().getExtras().getString("userID");
            FirebaseUtil.allUser().document(userID).get()
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            AdminModel model = task.getResult().toObject(AdminModel.class);

                            Intent mainIntent = new Intent(this, MainActivity.class);
                            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(mainIntent);

                                Intent intent = new Intent(this, messageActivity.class);
                                AndroidUtil.passAdminDataMessages(intent,model);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                        }
                    });

        }else{
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(FirebaseUtil.isLoggedIn()){
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    }else{
                        startActivity(new Intent(SplashActivity.this, LoginScreen.class));
                    }
                    finish();
                }
            }, 2000);
        }


    }
}